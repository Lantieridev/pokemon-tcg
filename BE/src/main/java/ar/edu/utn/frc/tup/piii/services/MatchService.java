package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.GameStateResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.PlayerPerspectiveMapper;
import ar.edu.utn.frc.tup.piii.engine.exception.InvalidActionException;
import ar.edu.utn.frc.tup.piii.engine.manager.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.model.Action;
import ar.edu.utn.frc.tup.piii.engine.model.ValidationResult;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState;
import ar.edu.utn.frc.tup.piii.services.persistence.GameStatePersistence;
import ar.edu.utn.frc.tup.piii.services.persistence.GameStateSnapshot;
import ar.edu.utn.frc.tup.piii.services.PenaltyService;
import ar.edu.utn.frc.tup.piii.services.ProfileService;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerState;
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
 *   <li>Validate Action (via RuleValidator).</li>
 *   <li>Apply action to board (future: engine pipeline).</li>
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
    private final RuleValidator ruleValidator;
    private final GameStatePersistence persistence;
    private final PlayerPerspectiveMapper perspectiveMapper;
    private final SimpMessagingTemplate messaging;
    private final ScheduledExecutorService abandonmentScheduler;
    private final long abandonTimeoutSeconds;
    private final PenaltyService penaltyService;
    private final ProfileService profileService;
    private final UserRepository userRepository;

    // Track turns of each player in a match in-memory
    private final java.util.Map<String, String> lastActorInMatch = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, java.util.Map<String, Integer>> playerTurnsInMatch = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Constructs a MatchService with all required collaborators.
     */
    public MatchService(final MatchSessionRegistry registry,
                        final GameFacade facade,
                        final RuleValidator ruleValidator,
                        final GameStatePersistence persistence,
                        final PlayerPerspectiveMapper perspectiveMapper,
                        final SimpMessagingTemplate messaging,
                        final ScheduledExecutorService abandonmentScheduler,
                        final PenaltyService penaltyService,
                        final ProfileService profileService,
                        final UserRepository userRepository,
                        @Value("${match.abandon.timeout-seconds:60}") final long abandonTimeoutSeconds) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.facade = Objects.requireNonNull(facade, "facade must not be null");
        this.ruleValidator = Objects.requireNonNull(ruleValidator, "ruleValidator must not be null");
        this.persistence = Objects.requireNonNull(persistence, "persistence must not be null");
        this.perspectiveMapper = Objects.requireNonNull(perspectiveMapper, "perspectiveMapper must not be null");
        this.messaging = Objects.requireNonNull(messaging, "messaging must not be null");
        this.abandonmentScheduler = Objects.requireNonNull(abandonmentScheduler,
                "abandonmentScheduler must not be null");
        this.penaltyService = Objects.requireNonNull(penaltyService, "penaltyService must not be null");
        this.profileService = Objects.requireNonNull(profileService, "profileService must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.abandonTimeoutSeconds = abandonTimeoutSeconds;
    }

    /**
     * Processes a player action: validates, applies, persists (inside lock), then broadcasts (outside lock).
     *
     * @param matchId  the match to act on (never null)
     * @param playerId the acting player (never null)
     * @param dto      the action to perform (never null)
     * @throws InvalidActionException if the action is not legal in the current game state
     * @throws IllegalArgumentException if the match or player is not found
     */
    public void processAction(final String matchId, final String playerId, final ActionRequestDTO dto) {
        final MatchSession session = registry.find(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));

        final ReentrantLock lock = session.getLock();
        lock.lock();
        try {
            final int playerIndex = session.indexOf(playerId);
            final Action action = facade.toEngineAction(session.getBoard(), playerIndex, dto);
            final ValidationResult result = ruleValidator.validate(action);

            if (result instanceof ValidationResult.Invalid invalid) {
                throw new InvalidActionException(invalid.reason());
            }

            facade.apply(session, action);

            // Track action-specific stats
            if (action instanceof ar.edu.utn.frc.tup.piii.engine.model.DeclareAttackAction attackAction) {
                profileService.trackDamageDealt(playerId, attackAction.attack().baseDamage());
            } else if (action instanceof ar.edu.utn.frc.tup.piii.engine.model.PlayTrainerAction) {
                profileService.trackTrainerCardPlayed(playerId);
            }

            // Turn Tracking
            final String lastActor = lastActorInMatch.get(matchId);
            if (lastActor == null) {
                lastActorInMatch.put(matchId, playerId);
                final java.util.Map<String, Integer> turns = playerTurnsInMatch.computeIfAbsent(matchId, k -> new java.util.concurrent.ConcurrentHashMap<>());
                turns.put(playerId, 1);
            } else if (!lastActor.equals(playerId)) {
                lastActorInMatch.put(matchId, playerId);
                final java.util.Map<String, Integer> turns = playerTurnsInMatch.computeIfAbsent(matchId, k -> new java.util.concurrent.ConcurrentHashMap<>());
                final int currentTurns = turns.getOrDefault(playerId, 0);
                turns.put(playerId, currentTurns + 1);
            }

            final GameStateSnapshot snapshot = new GameStateSnapshot(
                    matchId, FIRST_ROUND, session.getPlayerIds());
            persistence.save(snapshot);

            if (session.getState() == MatchSessionState.FINISHED) {
                final String winnerId = determineWinner(session);
                final int winnerIndex = session.indexOf(winnerId);
                final int loserIndex = 1 - winnerIndex;
                final MatchBoard board = session.getBoard();

                final int loserPrizesAtEnd = board.getRemainingPrizes(loserIndex);
                final boolean isPerfectWin = (loserPrizesAtEnd == 6);
                final boolean isComebackWin = (loserPrizesAtEnd == 1);

                // Legitimate match finish, counts for mute decrement for all penalized players in the match
                for (final String participantId : session.getPlayerIds()) {
                    final boolean won = participantId.equals(winnerId);
                    final int kos = won ? (6 - board.getRemainingPrizes(loserIndex)) : (6 - board.getRemainingPrizes(winnerIndex));
                    userRepository.findByUsername(participantId).ifPresent(user -> {
                        profileService.awardXpAndCheckAchievements(user.getId(), won, won && isPerfectWin, won && isComebackWin, kos);
                    });
                    penaltyService.registerMatchFinished(participantId, true);
                }
                // Clean up tracking
                lastActorInMatch.remove(matchId);
                playerTurnsInMatch.remove(matchId);
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

    private void abandonMatch(final String matchId, final String forfeitingPlayerId) {
        registry.find(matchId).ifPresent(session -> {
            final ReentrantLock lock = session.getLock();
            lock.lock();
            try {
                session.finish();
                persistence.save(new GameStateSnapshot(matchId, FIRST_ROUND, session.getPlayerIds()));

                // Process penalties on match finish
                final java.util.Map<String, Integer> turnsMap = playerTurnsInMatch.getOrDefault(matchId, java.util.Collections.emptyMap());
                boolean completedLegitimately;

                // For each player, evaluate if it is a legitimate match completion to decrement mute and award XP
                for (final String participantId : session.getPlayerIds()) {
                    final boolean won = !participantId.equals(forfeitingPlayerId);
                    userRepository.findByUsername(participantId).ifPresent(user -> {
                        profileService.awardXpAndCheckAchievements(user.getId(), won, false, false, 0);
                    });

                    if (participantId.equals(forfeitingPlayerId)) {
                        // The penalized user who forfeited does NOT get a decrement
                        completedLegitimately = false;
                    } else {
                        // The user who did not forfeit gets a decrement ONLY if both players had at least 5 turns
                        final int turnsA = turnsMap.getOrDefault(session.getPlayerIdA(), 0);
                        final int turnsB = turnsMap.getOrDefault(session.getPlayerIdB(), 0);
                        completedLegitimately = (turnsA >= 5 && turnsB >= 5);
                    }
                    penaltyService.registerMatchFinished(participantId, completedLegitimately);
                }

                // Clean up tracking
                lastActorInMatch.remove(matchId);
                playerTurnsInMatch.remove(matchId);

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

    private String determineWinner(final MatchSession session) {
        final MatchBoard board = session.getBoard();
        final String playerA = session.getPlayerIdA();
        final String playerB = session.getPlayerIdB();

        // 1. Condición de Premios (Prize cards)
        if (board.getRemainingPrizes(0) == 0) {
            return playerA;
        }
        if (board.getRemainingPrizes(1) == 0) {
            return playerB;
        }

        // 2. Condición de Bancarrota de Pokémon (Active + Bench)
        final boolean hasActiveA = board.getActivePokemon(0) != null;
        final boolean hasBenchA = !board.getBenchedPokemon(0).isEmpty();
        final boolean hasActiveB = board.getActivePokemon(1) != null;
        final boolean hasBenchB = !board.getBenchedPokemon(1).isEmpty();

        if (!hasActiveA && !hasBenchA) {
            return playerB;
        }
        if (!hasActiveB && !hasBenchB) {
            return playerA;
        }

        // 3. Condición de Deck out
        if (board.getDeckSize(0) == 0) {
            return playerB;
        }
        if (board.getDeckSize(1) == 0) {
            return playerA;
        }

        // Fallback
        return playerA;
    }
}
