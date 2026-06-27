package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.GameStateResponseDTO;
import ar.edu.utn.frc.tup.piii.services.PlayerPerspectiveMapper;
import ar.edu.utn.frc.tup.piii.engine.exception.InvalidActionException;
import ar.edu.utn.frc.tup.piii.engine.manager.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.manager.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.model.Action;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.dtos.ActionType;
import ar.edu.utn.frc.tup.piii.engine.model.DeclareAttackAction;
import ar.edu.utn.frc.tup.piii.engine.model.EndTurnAction;
import ar.edu.utn.frc.tup.piii.engine.model.PromoteActiveAction;
import ar.edu.utn.frc.tup.piii.engine.model.ValidationResult;
import ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState;
import ar.edu.utn.frc.tup.piii.services.persistence.GameStatePersistence;
import ar.edu.utn.frc.tup.piii.services.persistence.GameStateSnapshot;
import ar.edu.utn.frc.tup.piii.services.PenaltyService;
import ar.edu.utn.frc.tup.piii.services.ProfileService;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.persistence.entity.Tier;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
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
public class MatchService {

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
    private final ProfileService profileService;
    private final UserRepository userRepository;
    private final BotDecisionService botDecisionService;
    private final MmrCalculationService mmrCalculationService;
    private final CampaignService campaignService;

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
     * @param profileService        manages user profiles and XP (never null)
     * @param userRepository        repository for User entities (never null)
     * @param botDecisionService    service for handling bot turns
     * @param abandonTimeoutSeconds seconds before a disconnected player forfeits
     */
    public MatchService(final MatchSessionRegistry registry,
                        final GameFacade facade,
                        final GameStatePersistence persistence,
                        final PlayerPerspectiveMapper perspectiveMapper,
                        final SimpMessagingTemplate messaging,
                        final ScheduledExecutorService abandonmentScheduler,
                        final PenaltyService penaltyService,
                        final ProfileService profileService,
                        final UserRepository userRepository,
                        @Lazy final BotDecisionService botDecisionService,
                        final MmrCalculationService pMmrCalculationService,
                        @Lazy final CampaignService campaignService,
                        @Value("${match.abandon.timeout-seconds:60}") final long abandonTimeoutSeconds) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.facade = Objects.requireNonNull(facade, "facade must not be null");
        this.persistence = Objects.requireNonNull(persistence, "persistence must not be null");
        this.perspectiveMapper = Objects.requireNonNull(perspectiveMapper, "perspectiveMapper must not be null");
        this.messaging = Objects.requireNonNull(messaging, "messaging must not be null");
        this.abandonmentScheduler = Objects.requireNonNull(abandonmentScheduler,
                "abandonmentScheduler must not be null");
        this.penaltyService = Objects.requireNonNull(penaltyService, "penaltyService must not be null");
        this.profileService = Objects.requireNonNull(profileService, "profileService must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.botDecisionService = botDecisionService;
        this.mmrCalculationService = Objects.requireNonNull(pMmrCalculationService, "mmrCalculationService must not be null");
        this.campaignService = Objects.requireNonNull(campaignService, "campaignService must not be null");
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
            if (session.getState() == MatchSessionState.FINISHED) {
                throw new InvalidActionException("match_already_finished");
            }
            session.clearLastCoinFlips();
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
                boolean isOpponentChoosing = false;
                if (session.getPendingSelectionRequest() != null
                        && (session.getPendingSelectionRequest().sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FLASH_CLAW
                        || session.getPendingSelectionRequest().sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.PUSH_DOWN)) {
                    isOpponentChoosing = true;
                }

                if (isOpponentChoosing) {
                    if (playerIndex == 1 - session.getTurnManager().activePlayerIndex()) {
                        isAuthorized = true;
                    }
                } else {
                    if (playerIndex == session.getTurnManager().activePlayerIndex()) {
                        isAuthorized = true;
                    } else if (dto.type() == ActionType.PLACE_BASIC_POKEMON && session.getPlayerRuntime(playerIndex).getActivePokemon() == null) {
                        isAuthorized = true;
                    }
                }
            }

            if (!isAuthorized) {
                throw new InvalidActionException("not_your_turn");
            }

            session.setActivePlayerIndex(playerIndex);

            final Action action = facade.toEngineAction(session, playerIndex, dto);

            // Use the per-session RuleValidator if available
            final RuleValidator validator = resolveValidator(session);
            final ValidationResult result = validator.validate(action, playerIndex);

            if (result instanceof ValidationResult.Invalid invalid) {
                throw new InvalidActionException(invalid.reason());
            }

            final TurnManager turnManager = session.getTurnManager();

            // Track action-specific stats
            if (action instanceof ar.edu.utn.frc.tup.piii.engine.model.DeclareAttackAction attackAction) {
                profileService.trackDamageDealt(playerId, attackAction.attack().baseDamage());
            } else if (action instanceof ar.edu.utn.frc.tup.piii.engine.model.PlayTrainerAction) {
                profileService.trackTrainerCardPlayed(playerId);
            }

            // Apply action and manage TurnManager phase transitions
            applyWithPhaseTransitions(session, action, turnManager);

            session.incrementVersion();

            final int turnNumber = getCurrentTurnNumber(session);
            final GameStateSnapshot snapshot = new GameStateSnapshot(
                    matchId, turnNumber, session.getPlayerIds());
            persistence.save(snapshot);
            persistence.saveMatch(session);
            String resultDetail = String.format("Executed action %s with cardId=%s, targetId=%s", dto.type(), dto.cardId(), dto.targetId());
            persistence.logAction(matchId, turnNumber, playerId, dto.type().name(), resultDetail);

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
                    userRepository.findFirstByUsername(participantId).ifPresent(user -> {
                        profileService.awardXpAndCheckAchievements(user.getId(), won, won && isPerfectWin, won && isComebackWin, kos);
                        final int xpGained = won ? 50 : 25;
                        final int coinsGained = won ? 50 : 10;
                        if (participantId.equals(session.getPlayerIdA())) {
                            session.setXpGainedA(xpGained);
                            session.setCoinsGainedA(coinsGained);
                        } else {
                            session.setXpGainedB(xpGained);
                            session.setCoinsGainedB(coinsGained);
                        }
                    });
                    penaltyService.registerMatchFinished(participantId, true);
                }
                
                // Handle MMR updates if ranked
                if (session.isRanked()) {
                    updateMmr(session, winnerId, loserIndex == 0 ? session.getPlayerIdA() : session.getPlayerIdB());
                }

                // Handle campaign progress
                handleCampaignCompletion(session);
            }
        } finally {
            lock.unlock();
        }

        broadcastState(matchId, session);

        // Check for bot turn or bot promotion
        final MatchSession currentSession = registry.find(matchId).orElse(null);
        if (currentSession != null) {
            boolean triggerBot = false;
            if (currentSession.getTurnManager() != null) {
                int activeIndex = currentSession.getTurnManager().activePlayerIndex();
                if (activeIndex >= 0 && activeIndex < currentSession.getPlayerIds().size()) {
                    String activeId = currentSession.getPlayerIds().get(activeIndex);
                    if (activeId != null && activeId.startsWith("Bot-")) {
                        triggerBot = true;
                    }
                }
            }
            if (currentSession.isAwaitingPromotion()) {
                int promotingIndex = currentSession.getPromotingPlayerIndex();
                if (promotingIndex >= 0 && promotingIndex < currentSession.getPlayerIds().size()) {
                    String promotingId = currentSession.getPlayerIds().get(promotingIndex);
                    if (promotingId != null && promotingId.startsWith("Bot-")) {
                        triggerBot = true;
                    }
                }
            }
            if (triggerBot) {
                botDecisionService.evaluateAndPlay(matchId);
            }
        }
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

    /**
     * Explicitly surrenders a match.
     */
    public void surrenderMatch(final String matchId, final String playerId) {
        abandonMatch(matchId, playerId);
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
                if (session.getPendingSelectionRequest() != null) {
                    return; // pause turn advancement for Clairvoyant Eye / Stoke
                }
                // PhaseExited(AttackPhase) fires here → KnockoutManager checks for KOs
                turnManager.endAttack();
                if (session.getState() == ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState.FINISHED) {
                    return;
                }
                // If defender's active was just KO'd and bench has Pokémon, pause
                if (checkForPendingPromotion(session)) {
                    return; // between-turns will run once PROMOTE_ACTIVE is received
                }
                processBetweenTurns(session, turnManager);
                session.setBetweenTurnsProcessed(true);
                if (resolveBetweenTurnsKnockouts(session)) {
                    if (checkForPendingPromotion(session)) {
                        return;
                    }
                }
                turnManager.endBetweenTurns();
                if (session.getState() == ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState.FINISHED) {
                    return;
                }
                session.setBetweenTurnsProcessed(false);
            }
            case SelectCardsAction selectCards -> {
                final boolean isAttackSelection = session.getPendingSelectionRequest() != null
                        && (session.getPendingSelectionRequest().sourceEffect() == TrainerEffectId.CLAIRVOYANT_EYE
                        || session.getPendingSelectionRequest().sourceEffect() == TrainerEffectId.QUIVER_DANCE
                        || session.getPendingSelectionRequest().sourceEffect() == TrainerEffectId.FLASH_CLAW
                        || session.getPendingSelectionRequest().sourceEffect() == TrainerEffectId.ROCK_RUSH
                        || session.getPendingSelectionRequest().sourceEffect() == TrainerEffectId.BRILLIANT_SEARCH
                        || session.getPendingSelectionRequest().sourceEffect() == TrainerEffectId.BURIED_TREASURE_HUNT
                        || session.getPendingSelectionRequest().sourceEffect() == TrainerEffectId.DUAL_BULLET
                        || session.getPendingSelectionRequest().sourceEffect() == TrainerEffectId.PAIN_PELLETS
                        || session.getPendingSelectionRequest().sourceEffect() == TrainerEffectId.BENCH_DAMAGE_ONE
                        || session.getPendingSelectionRequest().sourceEffect() == TrainerEffectId.CURSED_DROP
                        || session.getPendingSelectionRequest().sourceEffect() == TrainerEffectId.EAR_INFLUENCE
                        || session.getPendingSelectionRequest().sourceEffect() == TrainerEffectId.RESCUE
                        || session.getPendingSelectionRequest().sourceEffect() == TrainerEffectId.FANG_SNIPE
                        || session.getPendingSelectionRequest().sourceEffect() == TrainerEffectId.REVIVAL
                        || session.getPendingSelectionRequest().sourceEffect() == TrainerEffectId.PUSH_DOWN
                        || session.getPendingSelectionRequest().sourceEffect() == TrainerEffectId.BOUNCE
                        || session.getPendingSelectionRequest().sourceEffect() == TrainerEffectId.PARABOLIC_CHARGE);

                facade.apply(session, action, turnManager);

                if (isAttackSelection) {
                    turnManager.endAttack();
                    if (checkForPendingPromotion(session)) {
                        return;
                    }
                    processBetweenTurns(session, turnManager);
                    session.setBetweenTurnsProcessed(true);
                    if (resolveBetweenTurnsKnockouts(session)) {
                        if (checkForPendingPromotion(session)) {
                            return;
                        }
                    }
                    turnManager.endBetweenTurns();
                    session.setBetweenTurnsProcessed(false);
                } else {
                    if (session.getVictoryConditionChecker() != null) {
                        session.getVictoryConditionChecker().checkFieldVictory();
                    }
                    if (session.getState() != ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState.FINISHED) {
                        checkForPendingPromotion(session);
                    }
                }
            }
            case EndTurnAction ignored -> {
                turnManager.passTurn();
                processBetweenTurns(session, turnManager);
                session.setBetweenTurnsProcessed(true);
                if (resolveBetweenTurnsKnockouts(session)) {
                    if (checkForPendingPromotion(session)) {
                        return;
                    }
                }
                turnManager.endBetweenTurns();
                if (session.getState() == ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState.FINISHED) {
                    return;
                }
                session.setBetweenTurnsProcessed(false);
            }
            case PromoteActiveAction ignored -> {
                final boolean wasAwaiting = session.isAwaitingPromotion();
                facade.apply(session, action, turnManager);
                if (wasAwaiting) {
                    session.clearAwaitingPromotion();
                    if (checkForPendingPromotion(session)) {
                        return;
                    }
                    if (turnManager.currentPhase() instanceof ar.edu.utn.frc.tup.piii.engine.model.BetweenTurnsPhase) {
                        if (!session.isBetweenTurnsProcessed()) {
                            // Resume the deferred between-turns phase that was paused for this promotion
                            processBetweenTurns(session, turnManager);
                            session.setBetweenTurnsProcessed(true);
                            if (resolveBetweenTurnsKnockouts(session)) {
                                if (checkForPendingPromotion(session)) {
                                    return;
                                }
                            }
                        }
                        if (session.getState() == ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState.FINISHED) {
                            return;
                        }
                        turnManager.endBetweenTurns();
                        session.setBetweenTurnsProcessed(false);
                    } else if (turnManager.currentPhase() instanceof ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase) {
                        turnManager.resumeMainPhase();
                    }
                }
            }
            default -> {
                facade.apply(session, action, turnManager);
                if (session.getVictoryConditionChecker() != null) {
                    session.getVictoryConditionChecker().checkFieldVictory();
                }
                if (session.getState() != ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState.FINISHED) {
                    checkForPendingPromotion(session);
                }
            }
        }

        if (session.getState() != ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState.FINISHED && session.isMegaEvolvedThisTurn()) {
            session.setMegaEvolvedThisTurn(false);
            turnManager.passTurn();
            processBetweenTurns(session, turnManager);
            session.setBetweenTurnsProcessed(true);
            if (resolveBetweenTurnsKnockouts(session)) {
                if (checkForPendingPromotion(session)) {
                    return;
                }
            }
            turnManager.endBetweenTurns();
            session.setBetweenTurnsProcessed(false);
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

    private boolean resolveBetweenTurnsKnockouts(final MatchSession session) {
        boolean anyKnockout = false;
        for (int i = 0; i < 2; i++) {
            final var runtime = session.getPlayerRuntime(i);
            final var active = runtime.getActivePokemon();
            if (active != null && isKnockedOut(active)) {
                session.getKnockoutHandler().onKnockout(active, active.isEx() ? 2 : 1);
                anyKnockout = true;
            }
            for (final var benched : List.copyOf(runtime.getBench().getAll())) {
                if (isKnockedOut(benched)) {
                    session.getKnockoutHandler().onKnockout(benched, benched.isEx() ? 2 : 1);
                    anyKnockout = true;
                }
            }
        }
        return anyKnockout;
    }

    private boolean isKnockedOut(final BattlePokemonState state) {
        return state.getDamageCounters() * 10 >= state.getMaxHp();
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
                sem.processBetweenTurns(session.getPlayerRuntime(i).getActivePokemon(), i == turnManager.activePlayerIndex());
                if (i == turnManager.activePlayerIndex()) {
                    sem.setDisabledAttackName(null);
                    if (sem.isSelfDisabledAttackSetThisTurn()) {
                        sem.setSelfDisabledAttackSetThisTurn(false);
                    } else {
                        sem.setSelfDisabledAttackName(null);
                    }
                    if (sem.isSelfDisabledNextTurnSetThisTurn()) {
                        sem.setSelfDisabledNextTurnSetThisTurn(false);
                    } else {
                        sem.setSelfDisabledNextTurn(false);
                    }
                    
                    if (sem.isRetreatBlockedNextTurnSetThisTurn()) {
                        sem.setRetreatBlockedNextTurnSetThisTurn(false);
                    } else {
                        sem.setRetreatBlockedNextTurn(false);
                    }

                    if (sem.isExcitingShakeActiveNextTurnSetThisTurn()) {
                        sem.setExcitingShakeActiveNextTurnSetThisTurn(false);
                    } else {
                        sem.setExcitingShakeActiveNextTurn(false);
                    }

                    if (sem.isStrongGustUsedLastTurnSetThisTurn()) {
                        sem.setStrongGustUsedLastTurnSetThisTurn(false);
                    } else {
                        sem.setStrongGustUsedLastTurn(false);
                    }

                    session.getPlayerRuntime(i).setKnockedOutLastTurn(false);
                } else {
                    sem.setDamagePreventedNextTurn(false);
                    sem.setDamagePreventedIf60OrLessNextTurn(false);
                    sem.setDamageReducedBy20NextTurn(false);
                }
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
                if (session.getState() == MatchSessionState.FINISHED) {
                    return;
                }
                session.finish();
                session.incrementVersion();
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
                session.setVictoryReason("ABANDON");

                final int turnNumber = getCurrentTurnNumber(session);
                persistence.save(new GameStateSnapshot(matchId, turnNumber, session.getPlayerIds()));
                persistence.saveMatch(session);

                if (winnerUsername != null) {
                    persistence.declareWinner(matchId, winnerUsername);
                }

                persistence.logAction(matchId, turnNumber, forfeitingPlayerId, "ABANDON", "Player abandoned the match");


                // Process penalties on match finish
                boolean completedLegitimately;
                
                final int turnsA = session.getTurnManager() != null ? session.getTurnManager().getTurnCount(0) : 0;
                final int turnsB = session.getTurnManager() != null ? session.getTurnManager().getTurnCount(1) : 0;

                // For each player, evaluate if it is a legitimate match completion to decrement mute and award XP
                for (final String participantId : session.getPlayerIds()) {
                    final boolean won = !participantId.equals(forfeitingPlayerId);
                    
                    if (participantId.equals(forfeitingPlayerId)) {
                        // The penalized user who forfeited does NOT get a decrement
                        completedLegitimately = false;
                    } else {
                        // The user who did not forfeit gets a decrement ONLY if both players had at least 15 turns
                        completedLegitimately = (turnsA >= 15 && turnsB >= 15);
                    }
                    
                    final boolean finalCompletedLegitimately = completedLegitimately;

                    userRepository.findFirstByUsername(participantId).ifPresent(user -> {
                        // To prevent farming, the forfeiting player gets NO rewards.
                        if (won) {
                            profileService.awardXpAndCheckAchievements(user.getId(), won, false, false, 0);
                            if (participantId.equals(session.getPlayerIdA())) {
                                session.setXpGainedA(50);
                                session.setCoinsGainedA(50);
                            } else {
                                session.setXpGainedB(50);
                                session.setCoinsGainedB(50);
                            }
                        } else {
                            if (participantId.equals(session.getPlayerIdA())) {
                                session.setXpGainedA(0);
                                session.setCoinsGainedA(0);
                            } else {
                                session.setXpGainedB(0);
                                session.setCoinsGainedB(0);
                            }
                        }
                    });

                    penaltyService.registerMatchFinished(participantId, finalCompletedLegitimately);
                }

                // Apply ranked abandonment penalties
                if (session.isRanked()) {
                    // Standard forfeit MMR loss (winner gets MMR, forfeiting player loses MMR)
                    if (winnerUsername != null) {
                        updateMmr(session, winnerUsername, forfeitingPlayerId);
                    }
                    // Apply 15 minute ranked ban for abandoning
                    penaltyService.applyRankedBan(forfeitingPlayerId, 15);
                }

                // Handle campaign progress
                handleCampaignCompletion(session);
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

    private void updateMmr(MatchSession session, String winnerId, String loserId) {
        ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity winner = userRepository.findFirstByUsername(winnerId).orElse(null);
        ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity loser = userRepository.findFirstByUsername(loserId).orElse(null);

        if (winner != null && loser != null) {
            int winnerMmr = winner.getMmr() != null ? winner.getMmr() : 1000;
            int loserMmr = loser.getMmr() != null ? loser.getMmr() : 1000;

            int winnerRankedMatches = winner.getRankedMatchesPlayed() != null ? winner.getRankedMatchesPlayed() : 0;
            int loserRankedMatches = loser.getRankedMatchesPlayed() != null ? loser.getRankedMatchesPlayed() : 0;

            Tier winnerTierBefore = Tier.fromMmrAndMatches(winnerMmr, winnerRankedMatches);
            Tier loserTierBefore = Tier.fromMmrAndMatches(loserMmr, loserRankedMatches);

            int newWinnerMmr = mmrCalculationService.calculateNewMmr(winnerMmr, loserMmr, true, winnerRankedMatches);
            int newLoserMmr = mmrCalculationService.calculateNewMmr(loserMmr, winnerMmr, false, loserRankedMatches);

            winner.setMmr(newWinnerMmr);
            winner.setRankedMatchesPlayed(winnerRankedMatches + 1);
            loser.setMmr(newLoserMmr);
            loser.setRankedMatchesPlayed(loserRankedMatches + 1);

            userRepository.save(winner);
            userRepository.save(loser);

            Tier winnerTierAfter = Tier.fromMmrAndMatches(newWinnerMmr, winnerRankedMatches + 1);
            Tier loserTierAfter = Tier.fromMmrAndMatches(newLoserMmr, loserRankedMatches + 1);

            boolean winnerRankedUp = winnerTierAfter.isHigherThan(winnerTierBefore);
            boolean loserRankedUp = loserTierAfter.isHigherThan(loserTierBefore);

            if (winnerId.equals(session.getPlayerIdA())) {
                session.setMmrChangeA(newWinnerMmr - winnerMmr);
                session.setMmrChangeB(newLoserMmr - loserMmr);
                session.setCurrentMmrA(newWinnerMmr);
                session.setCurrentMmrB(newLoserMmr);
                session.setCurrentTierA(winnerTierAfter.getName());
                session.setCurrentTierB(loserTierAfter.getName());
                session.setRankUpTriggeredA(winnerRankedUp);
                session.setRankUpTriggeredB(loserRankedUp);
            } else {
                session.setMmrChangeB(newWinnerMmr - winnerMmr);
                session.setMmrChangeA(newLoserMmr - loserMmr);
                session.setCurrentMmrB(newWinnerMmr);
                session.setCurrentMmrA(newLoserMmr);
                session.setCurrentTierB(winnerTierAfter.getName());
                session.setCurrentTierA(loserTierAfter.getName());
                session.setRankUpTriggeredB(winnerRankedUp);
                session.setRankUpTriggeredA(loserRankedUp);
            }
        }
    }

    private int getCurrentTurnNumber(final MatchSession session) {
        if (session.getTurnManager() == null) {
            return 0;
        }
        return session.getTurnManager().getTurnCount(0) + session.getTurnManager().getTurnCount(1);
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

    private void handleCampaignCompletion(final MatchSession session) {
        if (session.getState() != MatchSessionState.FINISHED) {
            return;
        }
        final String winnerId = session.getWinnerId();
        if (winnerId == null) {
            return;
        }

        final String playerA = session.getPlayerIdA();
        final String playerB = session.getPlayerIdB();

        for (final CampaignService.CampaignNodeInfo node : CampaignService.NODES) {
            if (node.botId().equals(playerA) || node.botId().equals(playerB)) {
                final String humanPlayer = node.botId().equals(playerA) ? playerB : playerA;
                if (winnerId.equals(humanPlayer)) {
                    campaignService.completeNode(humanPlayer, node.id(), session.getMatchId());
                }
                break;
            }
        }
    }
}
