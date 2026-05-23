package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.GameStateResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.PlayerPerspectiveMapper;
import ar.edu.utn.frc.tup.piii.engine.exception.InvalidActionException;
import ar.edu.utn.frc.tup.piii.engine.manager.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.manager.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.model.Action;
import ar.edu.utn.frc.tup.piii.dtos.ActionType;
import ar.edu.utn.frc.tup.piii.engine.model.DeclareAttackAction;
import ar.edu.utn.frc.tup.piii.engine.model.EndTurnAction;
import ar.edu.utn.frc.tup.piii.engine.model.PromoteActiveAction;
import ar.edu.utn.frc.tup.piii.engine.model.ValidationResult;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState;
import ar.edu.utn.frc.tup.piii.services.persistence.GameStatePersistence;
import ar.edu.utn.frc.tup.piii.services.persistence.GameStateSnapshot;
import ar.edu.utn.frc.tup.piii.services.PenaltyService;
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
    private final PenaltyService penaltyService;

    /**
     * Constructs a MatchService with all required collaborators.
     *
     * @param registry              holds all active sessions (never null)
     * @param facade                translates DTOs to engine actions (never null)
     * @param persistence           persists state snapshots (never null)
     * @param perspectiveMapper     builds per-player response DTOs (never null)
     * @param messaging             sends WebSocket messages (never null)
     * @param abandonmentScheduler  schedules disconnect timeout tasks (never null)
     * @param penaltyService        manages turn penalties (never null)
     * @param abandonTimeoutSeconds seconds before a disconnected player forfeits
     */
    public MatchService(final MatchSessionRegistry registry,
                        final GameFacade facade,
                        final GameStatePersistence persistence,
                        final PlayerPerspectiveMapper perspectiveMapper,
                        final SimpMessagingTemplate messaging,
                        final ScheduledExecutorService abandonmentScheduler,
                        final PenaltyService penaltyService,
                        @Value("${match.abandon.timeout-seconds:60}") final long abandonTimeoutSeconds) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.facade = Objects.requireNonNull(facade, "facade must not be null");
        this.persistence = Objects.requireNonNull(persistence, "persistence must not be null");
        this.perspectiveMapper = Objects.requireNonNull(perspectiveMapper, "perspectiveMapper must not be null");
        this.messaging = Objects.requireNonNull(messaging, "messaging must not be null");
        this.abandonmentScheduler = Objects.requireNonNull(abandonmentScheduler,
                "abandonmentScheduler must not be null");
        this.penaltyService = Objects.requireNonNull(penaltyService, "penaltyService must not be null");
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

            boolean isAuthorized = false;

            if (session.isAwaitingPromotion()) {
                // Enforce promotion-gate: while a KO replacement is pending, only the
                // promoting player may act, and only with PROMOTE_ACTIVE (XY1 Rulebook §2).
                if (dto.type() != ActionType.PROMOTE_ACTIVE) {
                    throw new InvalidActionException("must_promote_before_continuing");
                }
                if (session.getPromotingPlayerIndex() != playerIndex) {
                    throw new InvalidActionException("not_your_promotion");
                }
                isAuthorized = true;
            } else {
                if (playerIndex == session.getTurnManager().activePlayerIndex()) {
                    isAuthorized = true;
                }
            }

            if (!isAuthorized) {
                throw new InvalidActionException("not_your_turn");
            }

            session.setActivePlayerIndex(playerIndex);

            final Action action = facade.toEngineAction(session, playerIndex, dto);

            // Use the per-session RuleValidator if available
            final RuleValidator validator = resolveValidator(session);
            final ValidationResult result = validator.validate(action);

            if (result instanceof ValidationResult.Invalid invalid) {
                throw new InvalidActionException(invalid.reason());
            }

            final TurnManager turnManager = session.getTurnManager();

            // Apply action and manage TurnManager phase transitions
            applyWithPhaseTransitions(session, action, turnManager);

            final GameStateSnapshot snapshot = new GameStateSnapshot(
                    matchId, FIRST_ROUND, session.getPlayerIds());
            persistence.save(snapshot);
            persistence.saveMatch(session);
            String resultDetail = String.format("Executed action %s with cardId=%s, targetId=%s", dto.type(), dto.cardId(), dto.targetId());
            persistence.logAction(matchId, 0, playerId, dto.type().name(), resultDetail);

            if (session.getState() == MatchSessionState.FINISHED) {
                // Legitimate match finish, counts for mute decrement for all penalized players in the match
                for (final String participantId : session.getPlayerIds()) {
                    penaltyService.registerMatchFinished(participantId, true);
                }
            }
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
     * correct phase transitions.
     *
     * <p>KO-promotion pause: after an attack resolves (or after between-turns status effects),
     * if the defending player's Active slot is empty AND they have a non-empty bench, the flow
     * PAUSES and {@link MatchSession#setAwaitingPromotion(int)} is set. Phase progression
     * (processBetweenTurns + endBetweenTurns) only continues once the promotion arrives.
     * See {@link ActionType#PROMOTE_ACTIVE}.</p>
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
                // PhaseExited(AttackPhase) fires here → KnockoutManager checks for KOs
                turnManager.endAttack();
                // If defender's active was just KO'd and bench has Pokémon, pause
                if (checkForPendingPromotion(session)) {
                    return; // between-turns will run once PROMOTE_ACTIVE is received
                }
                processBetweenTurns(session, turnManager);
                turnManager.endBetweenTurns();
            }
            case EndTurnAction ignored -> {
                turnManager.passTurn();
                processBetweenTurns(session, turnManager);
                turnManager.endBetweenTurns();
            }
            case PromoteActiveAction ignored -> {
                // Apply the promotion; session.clearAwaitingPromotion() is called after
                facade.apply(session, action, turnManager);
                session.clearAwaitingPromotion();
                // Resume the deferred between-turns phase that was paused for this promotion
                processBetweenTurns(session, turnManager);
                turnManager.endBetweenTurns();
            }
            default -> facade.apply(session, action, turnManager);
        }
    }

    /**
     * Checks whether any player's Active Pokémon slot is empty and their bench is non-empty,
     * indicating that a mandatory KO-replacement promotion is required before play continues.
     * When detected, sets the promotion-pending state on the session.
     *
     * @param session the current match session
     * @return {@code true} if promotion is now pending (caller should pause phase progression)
     */
    private boolean checkForPendingPromotion(final MatchSession session) {
        for (int i = 0; i < 2; i++) {
            final var runtime = session.getPlayerRuntime(i);
            if (runtime.getActivePokemon() == null && !runtime.getBench().getAll().isEmpty()) {
                session.setAwaitingPromotion(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Runs between-turns status effects for both players' Active Pokémon.
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
                // Determine the winner (the other player)
                String winnerUsername = null;
                if (session.getPlayerIdA() != null && session.getPlayerIdB() != null) {
                    winnerUsername = session.getPlayerIdA().equals(forfeitingPlayerId)
                            ? session.getPlayerIdB()
                            : session.getPlayerIdA();
                }
                if (winnerUsername != null) {
                    session.setWinnerId(winnerUsername);
                }

                persistence.save(new GameStateSnapshot(matchId, FIRST_ROUND, session.getPlayerIds()));
                persistence.saveMatch(session);

                if (winnerUsername != null) {
                    persistence.declareWinner(matchId, winnerUsername);
                }

                persistence.logAction(matchId, 0, forfeitingPlayerId, "ABANDON", "Player abandoned the match");


                // Process penalties on match finish
                boolean completedLegitimately;
                
                final int turnsA = session.getTurnManager() != null ? session.getTurnManager().getTurnCount(0) : 0;
                final int turnsB = session.getTurnManager() != null ? session.getTurnManager().getTurnCount(1) : 0;

                // For each player, evaluate if it is a legitimate match completion to decrement mute
                for (final String participantId : session.getPlayerIds()) {
                    if (participantId.equals(forfeitingPlayerId)) {
                        // The penalized user who forfeited does NOT get a decrement
                        completedLegitimately = false;
                    } else {
                        // The user who did not forfeit gets a decrement ONLY if both players had at least 5 turns
                        completedLegitimately = (turnsA >= 5 && turnsB >= 5);
                    }
                    penaltyService.registerMatchFinished(participantId, completedLegitimately);
                }
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
