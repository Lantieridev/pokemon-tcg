package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.GameStateResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.PlayerPerspectiveMapper;
import ar.edu.utn.frc.tup.piii.engine.exception.InvalidActionException;
import ar.edu.utn.frc.tup.piii.engine.manager.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.manager.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.model.Action;
import ar.edu.utn.frc.tup.piii.engine.model.DeclareAttackAction;
import ar.edu.utn.frc.tup.piii.engine.model.EndTurnAction;
import ar.edu.utn.frc.tup.piii.engine.model.ValidationResult;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState;
import ar.edu.utn.frc.tup.piii.services.persistence.GameStatePersistence;
import ar.edu.utn.frc.tup.piii.services.persistence.GameStateSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Orchestrates match actions: validates, applies, persists, and broadcasts state.
 *
 * <p>Lock contract (ADR-5):
 * <ol>
 *   <li>Acquire session lock.</li>
 *   <li>Authorize player.</li>
 *   <li>Translate DTO → engine Action (via GameFacade).</li>
 *   <li>Validate Action (via RuleValidator bound to the session).</li>
 *   <li>Apply action and advance TurnManager phase.</li>
 *   <li>Persist snapshot — INSIDE lock.</li>
 *   <li>Release lock.</li>
 *   <li>Broadcast state to both players — OUTSIDE lock.</li>
 * </ol>
 * </p>
 */
@Service
public final class MatchService {

    private static final int FIRST_ROUND = 0;
    private static final String MATCH_TOPIC_BASE = "/topic/match/";
    private static final String PLAYER_SUB_PATH = "/player/";

    private final MatchSessionRegistry registry;
    private final GameFacade facade;
    private final GameStatePersistence persistence;
    private final PlayerPerspectiveMapper perspectiveMapper;
    private final SimpMessagingTemplate messaging;
    private final ScheduledExecutorService abandonmentScheduler;
    private final long abandonTimeoutSeconds;

    /**
     * Constructs a MatchService with all required collaborators.
     *
     * @param registry              holds all active sessions (never null)
     * @param facade                translates DTOs to engine actions (never null)
     * @param persistence           persists state snapshots (never null)
     * @param perspectiveMapper     builds per-player response DTOs (never null)
     * @param messaging             sends WebSocket messages (never null)
     * @param abandonmentScheduler  schedules disconnect timeout tasks (never null)
     * @param abandonTimeoutSeconds seconds before a disconnected player forfeits
     */
    public MatchService(final MatchSessionRegistry registry,
                        final GameFacade facade,
                        final GameStatePersistence persistence,
                        final PlayerPerspectiveMapper perspectiveMapper,
                        final SimpMessagingTemplate messaging,
                        final ScheduledExecutorService abandonmentScheduler,
                        @Value("${match.abandon.timeout-seconds:60}") final long abandonTimeoutSeconds) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.facade = Objects.requireNonNull(facade, "facade must not be null");
        this.persistence = Objects.requireNonNull(persistence, "persistence must not be null");
        this.perspectiveMapper = Objects.requireNonNull(perspectiveMapper, "perspectiveMapper must not be null");
        this.messaging = Objects.requireNonNull(messaging, "messaging must not be null");
        this.abandonmentScheduler = Objects.requireNonNull(abandonmentScheduler,
                "abandonmentScheduler must not be null");
        this.abandonTimeoutSeconds = abandonTimeoutSeconds;
    }

    /**
     * Processes a player action: validates, applies, advances the turn phase, persists
     * (inside lock), then broadcasts (outside lock).
     *
     * @param matchId  the match to act on (never null)
     * @param playerId the acting player (never null)
     * @param dto      the action to perform (never null)
     * @throws InvalidActionException   if the action is not legal in the current game state
     * @throws IllegalArgumentException if the match or player is not found
     */
    public void processAction(final String matchId, final String playerId, final ActionRequestDTO dto) {
        final MatchSession session = registry.find(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));

        final ReentrantLock lock = session.getLock();
        lock.lock();
        try {
            final int playerIndex = session.indexOf(playerId);
            final Action action = facade.toEngineAction(session, playerIndex, dto);

            // Use the per-session RuleValidator if available
            final RuleValidator validator = resolveValidator(session);
            final ValidationResult result = validator.validate(action);

            if (result instanceof ValidationResult.Invalid invalid) {
                throw new InvalidActionException(invalid.reason());
            }

            session.setActivePlayerIndex(playerIndex);
            final TurnManager turnManager = session.getTurnManager();

            // Apply action and manage TurnManager phase transitions
            applyWithPhaseTransitions(session, action, turnManager);

            final GameStateSnapshot snapshot = new GameStateSnapshot(
                    matchId, FIRST_ROUND, session.getPlayerIds());
            persistence.save(snapshot);
        } finally {
            lock.unlock();
        }

        broadcastState(matchId, session);
    }

    /**
     * Called when a player's WebSocket connection drops.
     * Schedules an abandonment timer — if the player does not reconnect within
     * {@code abandonTimeoutSeconds}, the match is forfeited.
     *
     * @param matchId  the match identifier (never null)
     * @param playerId the disconnecting player (never null)
     */
    public void onPlayerDisconnect(final String matchId, final String playerId) {
        registry.find(matchId).ifPresent(session -> {
            if (session.getState() != MatchSessionState.ACTIVE) {
                return;
            }
            final Runnable task = () -> abandonMatch(matchId, playerId);
            final ScheduledFuture<?> future = abandonmentScheduler.schedule(
                    task, abandonTimeoutSeconds, TimeUnit.SECONDS);
            final ReentrantLock lock = session.getLock();
            lock.lock();
            try {
                session.setDisconnectTimeout(playerId, future);
            } finally {
                lock.unlock();
            }
        });
    }

    /**
     * Called when a player's WebSocket connection is restored.
     * Atomically cancels any pending abandonment timer.
     *
     * @param matchId  the match identifier (never null)
     * @param playerId the reconnecting player (never null)
     */
    public void onPlayerReconnect(final String matchId, final String playerId) {
        registry.find(matchId).ifPresent(session ->
                session.cancelDisconnectTimeout(playerId));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Applies the action and, when a TurnManager is bound to the session, drives the
     * correct phase transitions (DECLARE_ATTACK → attack + between-turns; END_TURN → between-turns).
     *
     * @param session     the current match session
     * @param action      the validated engine action
     * @param turnManager the turn manager bound to this session, or null for legacy sessions
     */
    private void applyWithPhaseTransitions(final MatchSession session,
                                            final Action action,
                                            final TurnManager turnManager) {
        if (turnManager == null) {
            facade.apply(session, action);
            return;
        }

        switch (action) {
            case DeclareAttackAction ignored -> {
                turnManager.declareAttack();
                facade.apply(session, action, turnManager);
                turnManager.endAttack();
                processBetweenTurns(session, turnManager);
                turnManager.endBetweenTurns();
            }
            case EndTurnAction ignored -> {
                turnManager.passTurn();
                processBetweenTurns(session, turnManager);
                turnManager.endBetweenTurns();
            }
            default -> facade.apply(session, action, turnManager);
        }
    }

    /**
     * Runs between-turns status effects for the active player's Pokémon.
     * Must be called AFTER entering BetweenTurnsPhase and BEFORE calling
     * {@link TurnManager#endBetweenTurns()}.
     *
     * @param session     the current match session
     * @param turnManager the active turn manager
     */
    private void processBetweenTurns(final MatchSession session, final TurnManager turnManager) {
        for (int i = 0; i < 2; i++) {
            if (session.getPlayerRuntime(i).getActivePokemon() != null) {
                final StatusEffectManager sem =
                        session.getPlayerRuntime(i).getStatusEffectManager();
                sem.processBetweenTurns(session.getPlayerRuntime(i).getActivePokemon());
            }
        }
    }

    /**
     * Resolves the RuleValidator to use for the given session.
     * Prefers the per-session validator (set during match creation) and returns it if present.
     * This ensures multi-match correctness — each match validates against its own TurnManager
     * and StatusEffectManagers.
     *
     * @param session the current match session
     * @return the validator to use (never null)
     * @throws IllegalStateException if no validator is bound to the session
     */
    private RuleValidator resolveValidator(final MatchSession session) {
        final RuleValidator sessionValidator = session.getRuleValidator();
        if (sessionValidator != null) {
            return sessionValidator;
        }
        throw new IllegalStateException(
                "No RuleValidator bound to session " + session.getMatchId()
                        + " — ensure MatchCreationService.createMatch() was used to initialize the session");
    }

    private void abandonMatch(final String matchId, final String forfeitingPlayerId) {
        registry.find(matchId).ifPresent(session -> {
            final ReentrantLock lock = session.getLock();
            lock.lock();
            try {
                session.finish();
                persistence.save(new GameStateSnapshot(matchId, FIRST_ROUND, session.getPlayerIds()));
            } finally {
                lock.unlock();
            }
            broadcastState(matchId, session);
            registry.remove(matchId);
        });
    }

    private void broadcastState(final String matchId, final MatchSession session) {
        final GameStateResponseDTO viewA = perspectiveMapper.toResponse(session, 0);
        final GameStateResponseDTO viewB = perspectiveMapper.toResponse(session, 1);
        messaging.convertAndSend(
                MATCH_TOPIC_BASE + matchId + PLAYER_SUB_PATH + session.getPlayerIdA(), viewA);
        messaging.convertAndSend(
                MATCH_TOPIC_BASE + matchId + PLAYER_SUB_PATH + session.getPlayerIdB(), viewB);
    }
}
