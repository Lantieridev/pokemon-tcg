package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.GameStateResponseDTO;
import ar.edu.utn.frc.tup.piii.services.PlayerPerspectiveMapper;
import ar.edu.utn.frc.tup.piii.services.ChatService;
import ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse;
import java.time.LocalDateTime;
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
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.CouplingBetweenObjects", "PMD.GodClass",
        "PMD.TooManyMethods", "PMD.CyclomaticComplexity"})
// Match orchestrator by design (ADR-5): the single place that coordinates validation, the
// engine facade, persistence, rewards, penalties, and broadcast for every action in a match —
// high coupling and method count are the intended shape of that role, not a design smell to
// fix by splitting the orchestrator. Every individual method-level complexity metric in this
// class has been resolved via real extraction; none is flagged individually.
public class MatchService {

    private static final int FIRST_ROUND = 0;
    private static final String MATCH_TOPIC_BASE = "/topic/match/";
    private static final String PLAYER_SUB_PATH = "/player/";
    private static final int MAX_MISSED_TURNS_BEFORE_ABANDON = 2;

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
    private final MatchRewardService matchRewardService;
    private final ChatService chatService;

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
     * @param matchRewardService    resolves match winners and applies MMR/campaign rewards (never null)
     * @param abandonTimeoutSeconds seconds before a disconnected player forfeits
     */
    @SuppressWarnings("PMD.ExcessiveParameterList")
    // Standard Spring constructor-injection with every collaborator this orchestrator's ADR-5
    // flow genuinely needs (registry, facade, persistence, messaging, three reward/penalty
    // services, etc.) — bundling them into an artificial "config object" would just move the
    // same coupling behind an extra layer without reducing it.
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
                        final MatchRewardService matchRewardService,
                        final ChatService chatService,
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
        this.matchRewardService = Objects.requireNonNull(matchRewardService, "matchRewardService must not be null");
        this.chatService = Objects.requireNonNull(chatService, "chatService must not be null");
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
            final int playerIndex = authorizePlayer(session, playerId, dto);
            executeAction(session, matchId, playerId, dto, playerIndex);
            updateTurnTimers(session);
        } finally {
            lock.unlock();
        }

        broadcastState(matchId, session);
        triggerBotTurnIfNeeded(matchId);
    }

    /**
     * Confirms the match is ACTIVE and the acting player is allowed to submit this action
     * right now — their own turn, a pending KO-replacement promotion, or a pending
     * interactive selection they're allowed to answer (ADR-5 step 2).
     *
     * @return the acting player's index (0 or 1)
     * @throws IllegalStateException  if the match is not ACTIVE
     * @throws InvalidActionException if the player is not authorized to act right now
     */
    private int authorizePlayer(final MatchSession session, final String playerId, final ActionRequestDTO dto) {
        if (session.getState() != MatchSessionState.ACTIVE) {
            throw new IllegalStateException("Match is not active");
        }
        session.clearLastCoinFlips();
        final int playerIndex = session.indexOf(playerId);

        if (session.getTurnManager() != null && playerIndex == session.getTurnManager().activePlayerIndex()) {
            session.resetMissedTurns(playerId);
        }

        if (session.isAwaitingPromotion()) {
            authorizePendingPromotion(session, dto, playerIndex);
        } else if (!isAuthorizedForNormalPlay(session, dto, playerIndex)) {
            throw new InvalidActionException("not_your_turn");
        }

        session.setActivePlayerIndex(playerIndex);
        return playerIndex;
    }

    /**
     * Enforces the promotion-gate: while a KO replacement is pending, only the promoting
     * player may act, and only with PROMOTE_ACTIVE (XY1 Rulebook §2).
     */
    private void authorizePendingPromotion(final MatchSession session, final ActionRequestDTO dto,
            final int playerIndex) {
        if (dto.type() != ActionType.PROMOTE_ACTIVE) {
            throw new InvalidActionException("must_promote_before_continuing");
        }
        if (session.getPromotingPlayerIndex() != playerIndex) {
            throw new InvalidActionException("not_your_promotion");
        }
    }

    private boolean isAuthorizedForNormalPlay(final MatchSession session, final ActionRequestDTO dto,
            final int playerIndex) {
        if (isOpponentChoosingSelection(session)) {
            return playerIndex == 1 - session.getTurnManager().activePlayerIndex();
        }
        return playerIndex == session.getTurnManager().activePlayerIndex()
                || (dto.type() == ActionType.PLACE_BASIC_POKEMON
                        && session.getPlayerRuntime(playerIndex).getActivePokemon() == null);
    }

    /**
     * True when a pending selection (e.g. Flash Claw target choice, Push Down bench pick) must
     * be answered by the OPPONENT of the active player, rather than the active player themselves.
     */
    private boolean isOpponentChoosingSelection(final MatchSession session) {
        return session.getPendingSelectionRequest() != null
                && (session.getPendingSelectionRequest().sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FLASH_CLAW
                || session.getPendingSelectionRequest().sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.PUSH_DOWN);
    }

    /**
     * Translates the DTO to an engine Action, validates it, applies it (advancing
     * TurnManager phases), persists the result, and handles match-finish reward bookkeeping
     * if the match ended (ADR-5 steps 3-6). Runs entirely while the caller holds the
     * session lock.
     */
    private void executeAction(final MatchSession session, final String matchId, final String playerId,
                                final ActionRequestDTO dto, final int playerIndex) {
        final Action action = facade.toEngineAction(session, playerIndex, dto);

        final ValidationResult result = resolveValidator(session).validate(action, playerIndex);
        if (result instanceof ValidationResult.Invalid invalid) {
            throw new InvalidActionException(invalid.reason());
        }

        final String cardName = lookupCardNameInHand(session, playerIndex, dto.cardId());
        trackActionSpecificStats(action, playerId);

        applyWithPhaseTransitions(session, action, session.getTurnManager());

        logTrainerCardUsage(action, matchId, playerId, cardName);
        logCoinFlips(session, action, dto, matchId, cardName);
        persistActionResult(session, matchId, playerId, dto);

        if (session.getState() == MatchSessionState.FINISHED) {
            handleMatchFinish(session);
        }
    }

    private String lookupCardNameInHand(final MatchSession session, final int playerIndex, final String cardId) {
        if (cardId == null) {
            return null;
        }
        return session.getPlayerRuntime(playerIndex).getHand().getCards().stream()
                .filter(c -> c.getCardId().equals(cardId))
                .map(ar.edu.utn.frc.tup.piii.engine.model.Card::getName)
                .findFirst().orElse(null);
    }

    private void trackActionSpecificStats(final Action action, final String playerId) {
        if (action instanceof ar.edu.utn.frc.tup.piii.engine.model.DeclareAttackAction attackAction) {
            profileService.trackDamageDealt(playerId, attackAction.attack().baseDamage());
        } else if (action instanceof ar.edu.utn.frc.tup.piii.engine.model.PlayTrainerAction) {
            profileService.trackTrainerCardPlayed(playerId);
        }
    }

    private void logTrainerCardUsage(final Action action, final String matchId, final String playerId,
            final String cardName) {
        if (action instanceof ar.edu.utn.frc.tup.piii.engine.model.PlayTrainerAction) {
            sendSystemMessage(matchId, playerId + " jugó la carta de Entrenador: " + trainerCardLabel(cardName));
        }
    }

    private String trainerCardLabel(final String cardName) {
        return cardName != null ? cardName : "Entrenador";
    }

    private void logCoinFlips(final MatchSession session, final Action action, final ActionRequestDTO dto,
            final String matchId, final String cardName) {
        final java.util.List<Boolean> flips = session.getLastCoinFlips();
        if (flips.isEmpty()) {
            return;
        }
        final StringBuilder sb = new StringBuilder("Lanzamiento de moneda");
        sb.append(describeCoinFlipContext(action, dto, cardName)).append(": ");
        for (int i = 0; i < flips.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(flips.get(i) ? "CARA" : "SECA");
        }
        sendSystemMessage(matchId, sb.toString());
    }

    private String describeCoinFlipContext(final Action action, final ActionRequestDTO dto, final String cardName) {
        if (action instanceof ar.edu.utn.frc.tup.piii.engine.model.DeclareAttackAction declareAttackAction) {
            return " para el ataque '" + declareAttackAction.attack().name() + "'";
        }
        if (action instanceof ar.edu.utn.frc.tup.piii.engine.model.PlayTrainerAction) {
            return " para la carta de Entrenador '" + trainerCardLabel(cardName) + "'";
        }
        if (dto.type() == ActionType.END_TURN) {
            return " para chequeo de estado";
        }
        return "";
    }

    private void persistActionResult(final MatchSession session, final String matchId, final String playerId,
            final ActionRequestDTO dto) {
        session.incrementVersion();
        final int turnNumber = getCurrentTurnNumber(session);
        persistence.save(new GameStateSnapshot(matchId, turnNumber, session.getPlayerIds()));
        persistence.saveMatch(session);
        final String resultDetail = String.format(
                "Executed action %s with cardId=%s, targetId=%s", dto.type(), dto.cardId(), dto.targetId());
        persistence.logAction(matchId, turnNumber, playerId, dto.type().name(), resultDetail);
    }

    /**
     * Awards XP/coins/MMR/campaign progress to both participants when a match finishes as
     * the direct result of an in-match action. (Abandon/surrender endings are handled
     * separately in {@link #abandonMatch}.)
     */
    private void handleMatchFinish(final MatchSession session) {
        final String winnerId = matchRewardService.determineWinner(session);
        final int winnerIndex = session.indexOf(winnerId);
        final int loserIndex = 1 - winnerIndex;
        final int loserPrizesAtEnd = session.getBoard().getRemainingPrizes(loserIndex);
        final boolean isPerfectWin = loserPrizesAtEnd == 6;
        final boolean isComebackWin = loserPrizesAtEnd == 1;

        // Legitimate match finish, counts for mute decrement for all penalized players in the match
        for (final String participantId : session.getPlayerIds()) {
            awardMatchFinishRewards(session, participantId, winnerId, isPerfectWin, isComebackWin);
            penaltyService.registerMatchFinished(participantId, true);
        }

        if (session.isRanked()) {
            matchRewardService.updateMmr(session, winnerId, loserIndex == 0 ? session.getPlayerIdA() : session.getPlayerIdB());
        }
        matchRewardService.handleCampaignCompletion(session);
    }

    private void awardMatchFinishRewards(final MatchSession session, final String participantId,
            final String winnerId, final boolean isPerfectWin, final boolean isComebackWin) {
        final boolean won = participantId.equals(winnerId);
        final MatchStatsSummary stats = summarizeParticipantStats(session, participantId);

        userRepository.findFirstByUsername(participantId).ifPresent(user -> {
            profileService.awardXpAndCheckAchievements(user.getId(), won, won && isPerfectWin, won && isComebackWin,
                    stats.kos());
            profileService.trackDamageDealt(participantId, stats.damage());
            setGainedXpAndCoins(session, participantId, won ? 50 : 25, won ? 50 : 10);
        });
    }

    /** Summed damage-dealt / KO counters for one participant, or zeros if runtimes aren't bound. */
    private record MatchStatsSummary(int damage, int kos) {
    }

    private MatchStatsSummary summarizeParticipantStats(final MatchSession session, final String participantId) {
        if (!session.hasPlayerRuntimes()) {
            return new MatchStatsSummary(0, 0);
        }
        final int participantIndex = session.indexOf(participantId);
        final ar.edu.utn.frc.tup.piii.engine.session.MatchStatisticsTracker tracker =
                session.getPlayerRuntime(participantIndex).getStatisticsTracker();
        final int damage = tracker.getPokemonDamageDealt().values().stream().mapToInt(Integer::intValue).sum();
        final int kos = tracker.getPokemonKOsMade().values().stream().mapToInt(Integer::intValue).sum();
        return new MatchStatsSummary(damage, kos);
    }

    private void setGainedXpAndCoins(final MatchSession session, final String participantId, final int xp,
            final int coins) {
        if (participantId.equals(session.getPlayerIdA())) {
            session.setXpGainedA(xp);
            session.setCoinsGainedA(coins);
        } else {
            session.setXpGainedB(xp);
            session.setCoinsGainedB(coins);
        }
    }

    /**
     * Checks whether a bot needs to act next (their turn, or a pending KO promotion) and
     * triggers async evaluation if so. Runs AFTER the session lock is released.
     */
    private static final String BOT_ID_PREFIX = "Bot-";

    private void triggerBotTurnIfNeeded(final String matchId) {
        final MatchSession currentSession = registry.find(matchId).orElse(null);
        if (currentSession == null) {
            return;
        }
        if (isBotAtIndex(currentSession, activePlayerIndexOrInvalid(currentSession))
                || (currentSession.isAwaitingPromotion()
                        && isBotAtIndex(currentSession, currentSession.getPromotingPlayerIndex()))) {
            botDecisionService.evaluateAndPlay(matchId);
        }
    }

    private int activePlayerIndexOrInvalid(final MatchSession session) {
        return session.getTurnManager() != null ? session.getTurnManager().activePlayerIndex() : -1;
    }

    private boolean isBotAtIndex(final MatchSession session, final int index) {
        if (index < 0 || index >= session.getPlayerIds().size()) {
            return false;
        }
        final String playerId = session.getPlayerIds().get(index);
        return playerId != null && playerId.startsWith(BOT_ID_PREFIX);
    }

    /**
     * Called when a player's WebSocket connection drops.
     * We no longer schedule an immediate abandonment timer here.
     * The Turn Timer will naturally expire and handle their inactivity.
     *
     * @param matchId  the match identifier (never null)
     * @param playerId the disconnecting player (never null)
     */
    public void onPlayerDisconnect(final String matchId, final String playerId) {
        // We do not stop the turn timer, but we might want to flag the session.
    }

    /**
     * Called when a player's WebSocket connection is restored.
     *
     * @param matchId  the match identifier (never null)
     * @param playerId the reconnecting player (never null)
     */
    public void onPlayerReconnect(final String matchId, final String playerId) {
        // Intentionally left as a no-op. Turn timers handle AFK.
    }

    /**
     * Starts the initial turn timers for the match.
     * Called by MatchCreationService once the match is set up.
     */
    public void startTurnTimers(final String matchId) {
        registry.find(matchId).ifPresent(session -> {
            session.getLock().lock();
            try {
                updateTurnTimers(session);
            } finally {
                session.getLock().unlock();
            }
        });
    }

    private String getExpectedActor(final MatchSession session) {
        if (session.isAwaitingPromotion()) {
            for (int i = 0; i < 2; i++) {
                final var runtime = session.getPlayerRuntime(i);
                if (runtime.getActivePokemon() == null && !runtime.getBench().getAll().isEmpty()) {
                    return session.getPlayerIds().get(i);
                }
            }
        }
        final int activeIdx = session.getTurnManager() != null ? session.getTurnManager().activePlayerIndex() : -1;
        if (activeIdx != -1) {
            return session.getPlayerIds().get(activeIdx);
        }
        return null;
    }

    private void updateTurnTimers(final MatchSession session) {
        if (session.getState() != MatchSessionState.ACTIVE) {
            session.cancelTurnTimeout(session.getPlayerIdA());
            session.cancelTurnTimeout(session.getPlayerIdB());
            return;
        }

        final String expectedActor = getExpectedActor(session);
        for (final String pId : session.getPlayerIds()) {
            if (pId == null) continue;
            if (pId.equals(expectedActor)) {
                if (session.getTimeoutFuture(pId) == null) {
                    final Runnable task = () -> handleTurnTimeout(session.getMatchId(), pId);
                    final ScheduledFuture<?> future = abandonmentScheduler.schedule(
                            task, abandonTimeoutSeconds, TimeUnit.SECONDS);
                    session.setTurnTimeout(pId, future);
                }
            } else {
                session.cancelTurnTimeout(pId);
            }
        }
    }

    private void handleTurnTimeout(final String matchId, final String playerId) {
        final MatchSession session = registry.find(matchId).orElse(null);
        if (session == null || session.getState() != MatchSessionState.ACTIVE) return;

        boolean shouldAbandon = false;
        boolean shouldEndTurn = false;

        session.getLock().lock();
        try {
            final String expectedActor = getExpectedActor(session);
            if (!playerId.equals(expectedActor)) {
                return; // Stale timeout
            }

            session.cancelTurnTimeout(playerId); // Clear it
            session.incrementMissedTurns(playerId);

            if (session.isAwaitingPromotion()) {
                // Must abandon because a promotion cannot be skipped
                shouldAbandon = true;
            } else {
                if (session.getMissedTurns(playerId) >= MAX_MISSED_TURNS_BEFORE_ABANDON) {
                    shouldAbandon = true;
                } else {
                    shouldEndTurn = true;
                }
            }
        } finally {
            session.getLock().unlock();
        }

        if (shouldAbandon) {
            sendSystemMessage(matchId, "El jugador fue expulsado por inactividad.");
            abandonMatch(matchId, playerId);
        } else if (shouldEndTurn) {
            forceEndTurnOrAbandon(matchId, playerId, session);
        }
    }

    // PMD AvoidCatchingGenericException: the injected END_TURN action can fail with any of
    // several unrelated exception types depending on session/turn state at the moment the
    // timeout fires (a race with a real player action, an already-finished match, etc.) — the
    // whole point of this fallback is "any failure here means abandon", so catching narrower
    // types would just leave some failure modes unhandled without changing the actual behavior.
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void forceEndTurnOrAbandon(final String matchId, final String playerId, final MatchSession session) {
        sendSystemMessage(matchId, "El jugador se quedó sin tiempo. Turno omitido.");
        try {
            processAction(matchId, playerId, new ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO(
                    ar.edu.utn.frc.tup.piii.dtos.ActionType.END_TURN, null, null, null, null, null, null, null, null, null));

            // processAction automatically resets missedTurns to 0 (assuming a valid player action).
            // Since this was a system-injected timeout, we must restore the missed turn count.
            session.getLock().lock();
            try {
                session.incrementMissedTurns(playerId);
            } finally {
                session.getLock().unlock();
            }
        } catch (final Exception e) {
            abandonMatch(matchId, playerId);
        }
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
        if (dispatchPhaseTransition(session, action, turnManager)) {
            return; // the handler already paused turn advancement; skip the mega-evolution check too
        }
        checkMegaEvolutionExtraTurn(session, turnManager);
    }

    /** @return {@code true} if the caller should return immediately (matches every original inline "return;"). */
    private boolean dispatchPhaseTransition(final MatchSession session, final Action action,
            final TurnManager turnManager) {
        return switch (action) {
            case DeclareAttackAction ignored -> handleDeclareAttackPhase(session, action, turnManager);
            case SelectCardsAction ignored -> handleSelectCardsPhase(session, action, turnManager);
            case EndTurnAction ignored -> handleEndTurnPhase(session, turnManager);
            case PromoteActiveAction ignored -> handlePromoteActivePhase(session, action, turnManager);
            default -> handleDefaultPhase(session, action, turnManager);
        };
    }

    private boolean handleDeclareAttackPhase(final MatchSession session, final Action action,
            final TurnManager turnManager) {
        turnManager.declareAttack();
        facade.apply(session, action, turnManager);
        if (session.getPendingSelectionRequest() != null) {
            return true; // pause turn advancement for Clairvoyant Eye / Stoke
        }
        // PhaseExited(AttackPhase) fires here → KnockoutManager checks for KOs
        turnManager.endAttack();
        if (session.getState() == ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState.FINISHED) {
            return true;
        }
        // If defender's active was just KO'd and bench has Pokémon, pause (between-turns will
        // run once PROMOTE_ACTIVE is received) — otherwise run the between-turns sequence.
        return checkForPendingPromotion(session)
                || runBetweenTurnsSequenceCheckFinishedAfterEnd(session, turnManager);
    }

    // Effects whose interactive card selection resolves an attack still in progress — once
    // answered, the attack phase must be formally ended and between-turns processing resumed,
    // unlike a selection answered outside of combat (e.g. a Trainer card's own effect).
    private static final java.util.Set<TrainerEffectId> ATTACK_IN_PROGRESS_SELECTION_EFFECTS = java.util.Set.of(
            TrainerEffectId.CLAIRVOYANT_EYE, TrainerEffectId.QUIVER_DANCE, TrainerEffectId.FLASH_CLAW,
            TrainerEffectId.ROCK_RUSH, TrainerEffectId.BRILLIANT_SEARCH, TrainerEffectId.BURIED_TREASURE_HUNT,
            TrainerEffectId.DUAL_BULLET, TrainerEffectId.PAIN_PELLETS, TrainerEffectId.BENCH_DAMAGE_ONE,
            TrainerEffectId.CURSED_DROP, TrainerEffectId.EAR_INFLUENCE, TrainerEffectId.RESCUE,
            TrainerEffectId.FANG_SNIPE, TrainerEffectId.REVIVAL, TrainerEffectId.PUSH_DOWN,
            TrainerEffectId.BOUNCE, TrainerEffectId.PARABOLIC_CHARGE);

    private boolean handleSelectCardsPhase(final MatchSession session, final Action action,
            final TurnManager turnManager) {
        final boolean isAttackSelection = session.getPendingSelectionRequest() != null
                && ATTACK_IN_PROGRESS_SELECTION_EFFECTS.contains(session.getPendingSelectionRequest().sourceEffect());

        facade.apply(session, action, turnManager);

        if (isAttackSelection) {
            turnManager.endAttack();
            return checkForPendingPromotion(session)
                    || runBetweenTurnsSequenceNoFinishedCheck(session, turnManager);
        }
        if (session.getVictoryConditionChecker() != null) {
            session.getVictoryConditionChecker().checkFieldVictory();
        }
        if (session.getState() != ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState.FINISHED) {
            checkForPendingPromotion(session);
        }
        return false;
    }

    private boolean handleEndTurnPhase(final MatchSession session, final TurnManager turnManager) {
        turnManager.passTurn();
        return runBetweenTurnsSequenceCheckFinishedAfterEnd(session, turnManager);
    }

    private boolean handleDefaultPhase(final MatchSession session, final Action action, final TurnManager turnManager) {
        facade.apply(session, action, turnManager);
        if (session.getVictoryConditionChecker() != null) {
            session.getVictoryConditionChecker().checkFieldVictory();
        }
        if (session.getState() != ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState.FINISHED) {
            checkForPendingPromotion(session);
        }
        return false;
    }

    private boolean handlePromoteActivePhase(final MatchSession session, final Action action,
            final TurnManager turnManager) {
        final boolean wasAwaiting = session.isAwaitingPromotion();
        facade.apply(session, action, turnManager);
        if (!wasAwaiting) {
            return false;
        }
        session.clearAwaitingPromotion();
        if (checkForPendingPromotion(session)) {
            return true;
        }
        if (turnManager.currentPhase() instanceof ar.edu.utn.frc.tup.piii.engine.model.BetweenTurnsPhase) {
            return resumeDeferredBetweenTurnsForPromotion(session, turnManager);
        }
        if (turnManager.currentPhase() instanceof ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase) {
            turnManager.resumeMainPhase();
        }
        return false;
    }

    private void checkMegaEvolutionExtraTurn(final MatchSession session, final TurnManager turnManager) {
        if (session.getState() != ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState.FINISHED
                && session.isMegaEvolvedThisTurn()) {
            session.setMegaEvolvedThisTurn(false);
            turnManager.passTurn();
            runBetweenTurnsSequenceNoFinishedCheck(session, turnManager);
        }
    }

    /**
     * Resumes a between-turns phase that was deferred while a KO-promotion was pending: runs
     * the status-effect/knockout sequence only if it wasn't already processed before the pause,
     * then always checks for FINISHED and ends the phase (matching the original inline logic's
     * "always run the tail, only run the head conditionally" shape).
     *
     * @return {@code true} if the caller should return early (a new promotion became pending,
     *         or the match finished)
     */
    private boolean resumeDeferredBetweenTurnsForPromotion(final MatchSession session, final TurnManager turnManager) {
        if (!session.isBetweenTurnsProcessed()) {
            processBetweenTurns(session, turnManager);
            session.setBetweenTurnsProcessed(true);
            if (resolveBetweenTurnsKnockouts(session) && checkForPendingPromotion(session)) {
                return true;
            }
        }
        if (session.getState() == ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState.FINISHED) {
            return true;
        }
        turnManager.endBetweenTurns();
        session.setBetweenTurnsProcessed(false);
        return false;
    }

    /**
     * Runs the shared "process between-turns status effects, resolve KOs, check for a new
     * pending promotion" sequence, checking for a FINISHED match AFTER ending the between-turns
     * phase. Used where the original flow always calls {@code endBetweenTurns()} before the
     * FINISHED check, regardless of outcome.
     *
     * @return {@code true} if the caller should return early
     */
    private boolean runBetweenTurnsSequenceCheckFinishedAfterEnd(final MatchSession session,
            final TurnManager turnManager) {
        processBetweenTurns(session, turnManager);
        session.setBetweenTurnsProcessed(true);
        if (resolveBetweenTurnsKnockouts(session) && checkForPendingPromotion(session)) {
            return true;
        }
        turnManager.endBetweenTurns();
        if (session.getState() == ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState.FINISHED) {
            return true;
        }
        session.setBetweenTurnsProcessed(false);
        return false;
    }

    /**
     * Same sequence as {@link #runBetweenTurnsSequenceCheckFinishedAfterEnd}, but without a
     * FINISHED check around {@code endBetweenTurns()} — matches call sites where the caller
     * handles the FINISHED/victory check itself afterward.
     *
     * @return {@code true} if the caller should return early (a new promotion became pending)
     */
    private boolean runBetweenTurnsSequenceNoFinishedCheck(final MatchSession session, final TurnManager turnManager) {
        processBetweenTurns(session, turnManager);
        session.setBetweenTurnsProcessed(true);
        if (resolveBetweenTurnsKnockouts(session) && checkForPendingPromotion(session)) {
            return true;
        }
        turnManager.endBetweenTurns();
        session.setBetweenTurnsProcessed(false);
        return false;
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
            anyKnockout |= resolveKnockoutsForRuntime(session, session.getPlayerRuntime(i));
        }
        return anyKnockout;
    }

    private boolean resolveKnockoutsForRuntime(final MatchSession session,
            final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime) {
        boolean anyKnockout = false;
        final BattlePokemonState active = runtime.getActivePokemon();
        if (active != null && isKnockedOut(active)) {
            knockOut(session, active);
            anyKnockout = true;
        }
        for (final BattlePokemonState benched : List.copyOf(runtime.getBench().getAll())) {
            if (isKnockedOut(benched)) {
                knockOut(session, benched);
                anyKnockout = true;
            }
        }
        return anyKnockout;
    }

    private void knockOut(final MatchSession session, final BattlePokemonState pokemon) {
        session.getKnockoutHandler().onKnockout(pokemon, pokemon.isEx() ? 2 : 1);
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
            final var runtime = session.getPlayerRuntime(i);
            if (runtime.getActivePokemon() == null) {
                continue;
            }
            final StatusEffectManager sem = runtime.getStatusEffectManager();
            sem.processBetweenTurns(runtime.getActivePokemon(), i == turnManager.activePlayerIndex());
            if (i == turnManager.activePlayerIndex()) {
                resetActivePlayerTurnFlags(sem, runtime);
            } else {
                sem.setDamagePreventedNextTurn(false);
                sem.setDamagePreventedIf60OrLessNextTurn(false);
                sem.setDamageReducedBy20NextTurn(false);
            }
        }
    }

    /**
     * Each of these status flags has a same-turn "just set" marker so it survives the turn it
     * was set on and only expires the turn AFTER: if the marker is set, clear the marker instead
     * of the flag (giving it one more turn); otherwise the flag has already lived its extra turn
     * and gets cleared for real.
     */
    private void resetActivePlayerTurnFlags(final StatusEffectManager sem,
            final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime) {
        sem.setDisabledAttackName(null);
        expireOrDelayOneTurn(sem.isSelfDisabledAttackSetThisTurn(),
                () -> sem.setSelfDisabledAttackSetThisTurn(false), () -> sem.setSelfDisabledAttackName(null));
        expireOrDelayOneTurn(sem.isSelfDisabledNextTurnSetThisTurn(),
                () -> sem.setSelfDisabledNextTurnSetThisTurn(false), () -> sem.setSelfDisabledNextTurn(false));
        expireOrDelayOneTurn(sem.isRetreatBlockedNextTurnSetThisTurn(),
                () -> sem.setRetreatBlockedNextTurnSetThisTurn(false), () -> sem.setRetreatBlockedNextTurn(false));
        expireOrDelayOneTurn(sem.isExcitingShakeActiveNextTurnSetThisTurn(),
                () -> sem.setExcitingShakeActiveNextTurnSetThisTurn(false), () -> sem.setExcitingShakeActiveNextTurn(false));
        expireOrDelayOneTurn(sem.isStrongGustUsedLastTurnSetThisTurn(),
                () -> sem.setStrongGustUsedLastTurnSetThisTurn(false), () -> sem.setStrongGustUsedLastTurn(false));
        runtime.setKnockedOutLastTurn(false);
    }

    private void expireOrDelayOneTurn(final boolean wasSetThisTurn, final Runnable clearSetThisTurnMarker,
            final Runnable clearFlag) {
        if (wasSetThisTurn) {
            clearSetThisTurnMarker.run();
        } else {
            clearFlag.run();
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
                final String winnerUsername = finishAbandonedSession(session, forfeitingPlayerId);
                persistAbandon(session, matchId, forfeitingPlayerId, winnerUsername);
                applyAbandonPenaltiesAndRewards(session, forfeitingPlayerId, winnerUsername);
            } finally {
                lock.unlock();
            }
            broadcastState(matchId, session);
            registry.remove(matchId);
        });
    }

    /**
     * Transitions the session to FINISHED and determines the winner (the other player),
     * marking the victory reason as ABANDON.
     *
     * @return the winning player's username, or {@code null} if playerIdA/playerIdB aren't
     *         both set (legacy/incomplete session)
     */
    private String finishAbandonedSession(final MatchSession session, final String forfeitingPlayerId) {
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
        return winnerUsername;
    }

    private void persistAbandon(final MatchSession session, final String matchId,
                                 final String forfeitingPlayerId, final String winnerUsername) {
        final int turnNumber = getCurrentTurnNumber(session);
        persistence.save(new GameStateSnapshot(matchId, turnNumber, session.getPlayerIds()));
        persistence.saveMatch(session);

        if (winnerUsername != null) {
            persistence.declareWinner(matchId, winnerUsername);
        }

        persistence.logAction(matchId, turnNumber, forfeitingPlayerId, "ABANDON", "Player abandoned the match");
    }

    /**
     * Applies per-participant XP/coin rewards (the forfeiter gets none, to prevent farming),
     * penalty bookkeeping, ranked MMR/ban penalties, and campaign progress after an abandon.
     */
    private static final int ABANDON_MIN_LEGITIMATE_TURNS = 15;
    private static final int ABANDON_RANKED_BAN_MINUTES = 15;

    private void applyAbandonPenaltiesAndRewards(final MatchSession session, final String forfeitingPlayerId,
                                                  final String winnerUsername) {
        final boolean bothPlayedEnoughTurns = bothPlayersReachedMinimumTurns(session);

        // For each player, evaluate if it is a legitimate match completion to decrement mute and award XP
        for (final String participantId : session.getPlayerIds()) {
            final boolean won = !participantId.equals(forfeitingPlayerId);
            // The forfeiter never gets a mute decrement; the other player only gets one if both
            // players had at least ABANDON_MIN_LEGITIMATE_TURNS turns (not an instant-quit game).
            final boolean completedLegitimately = won && bothPlayedEnoughTurns;

            userRepository.findFirstByUsername(participantId)
                    .ifPresent(user -> awardAbandonRewards(session, participantId, won, user.getId()));
            penaltyService.registerMatchFinished(participantId, completedLegitimately);
        }

        if (session.isRanked()) {
            if (winnerUsername != null) {
                matchRewardService.updateMmr(session, winnerUsername, forfeitingPlayerId);
            }
            penaltyService.applyRankedBan(forfeitingPlayerId, ABANDON_RANKED_BAN_MINUTES);
        }
        matchRewardService.handleCampaignCompletion(session);
    }

    private boolean bothPlayersReachedMinimumTurns(final MatchSession session) {
        final int turnsA = session.getTurnManager() != null ? session.getTurnManager().getTurnCount(0) : 0;
        final int turnsB = session.getTurnManager() != null ? session.getTurnManager().getTurnCount(1) : 0;
        return turnsA >= ABANDON_MIN_LEGITIMATE_TURNS && turnsB >= ABANDON_MIN_LEGITIMATE_TURNS;
    }

    /** To prevent farming, the forfeiting player (won == false) gets NO XP/coin rewards. */
    private void awardAbandonRewards(final MatchSession session, final String participantId, final boolean won,
            final Long userId) {
        if (!won) {
            setGainedXpAndCoins(session, participantId, 0, 0);
            return;
        }
        final MatchStatsSummary stats = summarizeParticipantStats(session, participantId);
        profileService.awardXpAndCheckAchievements(userId, true, false, false, stats.kos());
        profileService.trackDamageDealt(participantId, stats.damage());
        setGainedXpAndCoins(session, participantId, 50, 50);
    }

    private void broadcastState(final String matchId, final MatchSession session) {
        final GameStateResponseDTO viewA = perspectiveMapper.toResponse(session, 0);
        final GameStateResponseDTO viewB = perspectiveMapper.toResponse(session, 1);
        messaging.convertAndSend(
                MATCH_TOPIC_BASE + matchId + PLAYER_SUB_PATH + session.getPlayerIdA(), viewA);
        messaging.convertAndSend(
                MATCH_TOPIC_BASE + matchId + PLAYER_SUB_PATH + session.getPlayerIdB(), viewB);
    }

    private int getCurrentTurnNumber(final MatchSession session) {
        if (session.getTurnManager() == null) {
            return 0;
        }
        return session.getTurnManager().getTurnCount(0) + session.getTurnManager().getTurnCount(1);
    }

    private void sendSystemMessage(final String matchId, final String message) {
        final ChatMessageResponse response = ChatMessageResponse.builder()
                .sender("SISTEMA")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        chatService.addMessage(matchId, response);
        messaging.convertAndSend("/topic/chat/" + matchId, response);
    }
}
