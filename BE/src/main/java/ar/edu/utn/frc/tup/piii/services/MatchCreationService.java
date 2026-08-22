package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.engine.infra.RandomCoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.listener.VictoryHandler;
import ar.edu.utn.frc.tup.piii.engine.manager.AutoSetupStrategy;
import ar.edu.utn.frc.tup.piii.engine.manager.DrawPhaseExecutor;
import ar.edu.utn.frc.tup.piii.engine.manager.KnockoutManager;
import ar.edu.utn.frc.tup.piii.engine.manager.KnockoutResolutionHandler;
import ar.edu.utn.frc.tup.piii.engine.manager.PlayerSetupSlot;
import ar.edu.utn.frc.tup.piii.engine.manager.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.manager.SetupManager;
import ar.edu.utn.frc.tup.piii.engine.manager.SetupResult;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.manager.TurnInPlayTracker;
import ar.edu.utn.frc.tup.piii.engine.manager.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.manager.VictoryConditionChecker;
import ar.edu.utn.frc.tup.piii.engine.model.Bench;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.Deck;
import ar.edu.utn.frc.tup.piii.engine.model.DiscardPile;
import ar.edu.utn.frc.tup.piii.engine.model.Hand;
import ar.edu.utn.frc.tup.piii.engine.model.VictoryResult;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerState;
import ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse;
import ar.edu.utn.frc.tup.piii.dtos.GameStateResponseDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Orchestrates the creation of a new match: runs the Setup Phase, wires all engine
 * components together, registers the session, and fires the first turn.
 *
 * <p>Lock contract: the session lock is NOT held during creation — the session is not
 * registered until all components are fully initialised.</p>
 */
@Service
public final class MatchCreationService {

    private static final String MATCH_TOPIC_BASE = "/topic/match/";
    private static final String PLAYER_SUB_PATH = "/player/";

    private final MatchSessionRegistry registry;
    private final SimpMessagingTemplate messaging;
    private final ChatService chatService;
    private final PlayerPerspectiveMapper perspectiveMapper;
    private final BotDecisionService botDecisionService;

    /**
     * @param registry          stores active sessions (never null)
     * @param messaging         used to broadcast the initial game state after setup (never null)
     * @param chatService       used to broadcast setup events like mulligans
     * @param perspectiveMapper maps sessions to player-specific views
     */
    public MatchCreationService(final MatchSessionRegistry registry,
                                 final SimpMessagingTemplate messaging,
                                 final ChatService chatService,
                                 final PlayerPerspectiveMapper perspectiveMapper,
                                 @org.springframework.context.annotation.Lazy final BotDecisionService botDecisionService) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.messaging = Objects.requireNonNull(messaging, "messaging must not be null");
        this.chatService = Objects.requireNonNull(chatService, "chatService must not be null");
        this.perspectiveMapper = Objects.requireNonNull(perspectiveMapper, "perspectiveMapper must not be null");
        this.botDecisionService = Objects.requireNonNull(botDecisionService, "botDecisionService must not be null");
    }

    /**
     * Creates a new match between two players.
     *
     * @param playerAId ID of player A (never null)
     * @param playerBId ID of player B (never null)
     * @param deckACards cards composing player A's 60-card deck (never null)
     * @param deckBCards cards composing player B's 60-card deck (never null)
     * @return the newly created match's unique identifier
     */
    public String createMatch(final String playerAId,
                               final String playerBId,
                               final List<Card> deckACards,
                               final List<Card> deckBCards,
                               final boolean isRanked) {
        Objects.requireNonNull(playerAId, "playerAId must not be null");
        Objects.requireNonNull(playerBId, "playerBId must not be null");
        Objects.requireNonNull(deckACards, "deckACards must not be null");
        Objects.requireNonNull(deckBCards, "deckBCards must not be null");

        final String matchId = UUID.randomUUID().toString();
        final PlayerComponents p0 = new PlayerComponents(deckACards);
        final PlayerComponents p1 = new PlayerComponents(deckBCards);
        final CoinFlipper coinFlipper = new RandomCoinFlipper();
        p0.sem = new StatusEffectManager(coinFlipper);
        p1.sem = new StatusEffectManager(coinFlipper);

        final PlayerSetupSlot slot0 = new PlayerSetupSlot(p0.deck, p0.hand, p0.bench);
        final PlayerSetupSlot slot1 = new PlayerSetupSlot(p1.deck, p1.hand, p1.bench);
        final SetupResult setupResult = runSetupPhase(matchId, playerAId, playerBId, slot0, slot1, coinFlipper);
        final int firstPlayerIndex = setupResult.firstPlayerIndex();

        final List<PlayerRuntime> runtimes = buildRuntimes(p0, p1, slot0, slot1);
        final MatchBoard board = buildBoard(p0, p1, slot0, slot1, runtimes);

        final MatchSession session = new MatchSession(
                matchId, List.of(playerAId, playerBId), board, runtimes, isRanked);
        session.setCoinFlipper(coinFlipper);

        final TurnManager turnManager = wireEngine(matchId, session, board, runtimes, firstPlayerIndex,
                List.of(p0.sem, p1.sem));

        registry.register(session);
        turnManager.startTurn(firstPlayerIndex);
        triggerBotTurnIfNeeded(session, firstPlayerIndex, matchId);

        return matchId;
    }

    /** Mutable per-player scratch state threaded through {@link #createMatch} phases. */
    private static final class PlayerComponents {
        private final Deck deck;
        private final Hand hand = new Hand();
        private final Bench bench = new Bench();
        private final DiscardPile discardPile = new DiscardPile();
        private StatusEffectManager sem;

        PlayerComponents(final List<Card> deckCards) {
            this.deck = new Deck(new ArrayList<>(deckCards));
        }
    }

    private SetupResult runSetupPhase(final String matchId, final String playerAId, final String playerBId,
            final PlayerSetupSlot slot0, final PlayerSetupSlot slot1, final CoinFlipper coinFlipper) {
        final SetupManager setupManager = new SetupManager(coinFlipper);
        final SetupResult setupResult = setupManager.executeWithoutPlacement(slot0, slot1);

        final String firstPlayerName = setupResult.firstPlayerIndex() == 0 ? playerAId : playerBId;
        broadcastSystemMessage(matchId, "Lanzamiento de moneda: " + firstPlayerName
                + " ganó el coin flip y empieza la partida.");
        broadcastMulliganIfAny(matchId, playerAId, setupResult.mulligansP0().size());
        broadcastMulliganIfAny(matchId, playerBId, setupResult.mulligansP1().size());
        return setupResult;
    }

    private void broadcastMulliganIfAny(final String matchId, final String playerId, final int mulliganCount) {
        if (mulliganCount > 0) {
            broadcastSystemMessage(matchId, playerId + " declaró " + mulliganCount
                    + " Mulligan(s) por no tener Pokémon Básicos en su mano.");
        }
    }

    private void broadcastSystemMessage(final String matchId, final String message) {
        chatService.addMessage(matchId, ChatMessageResponse.builder()
                .sender("SISTEMA")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build());
    }

    private List<PlayerRuntime> buildRuntimes(final PlayerComponents p0, final PlayerComponents p1,
            final PlayerSetupSlot slot0, final PlayerSetupSlot slot1) {
        final PlayerRuntime runtime0 = new PlayerRuntime(
                p0.deck, p0.hand, p0.bench, p0.discardPile, p0.sem, null, slot0.getPrizes());
        final PlayerRuntime runtime1 = new PlayerRuntime(
                p1.deck, p1.hand, p1.bench, p1.discardPile, p1.sem, null, slot1.getPrizes());
        final List<PlayerRuntime> runtimes = List.of(runtime0, runtime1);
        p0.sem.setPlayerRuntime(runtime0);
        p1.sem.setPlayerRuntime(runtime1);
        return runtimes;
    }

    private MatchBoard buildBoard(final PlayerComponents p0, final PlayerComponents p1,
            final PlayerSetupSlot slot0, final PlayerSetupSlot slot1, final List<PlayerRuntime> runtimes) {
        final PlayerState ps0 = new PlayerState(
                null, p0.bench.getAll(),
                p0.hand.getCards().stream().map(Card::getCardId).toList(),
                List.of(), p0.deck.size(), slot0.getPrizes().size(), Map.of());
        final PlayerState ps1 = new PlayerState(
                null, p1.bench.getAll(),
                p1.hand.getCards().stream().map(Card::getCardId).toList(),
                List.of(), p1.deck.size(), slot1.getPrizes().size(), Map.of());
        final MatchBoard board = new MatchBoard(List.of(ps0, ps1));
        board.bindRuntimes(runtimes);
        return board;
    }

    private TurnManager wireEngine(final String matchId, final MatchSession session, final MatchBoard board,
            final List<PlayerRuntime> runtimes, final int firstPlayerIndex,
            final List<StatusEffectManager> statusManagers) {
        final TurnManager turnManager = new TurnManager();
        turnManager.setStartingPlayer(firstPlayerIndex);

        final VictoryHandler victoryHandler = result -> handleVictory(matchId, session, result);
        final VictoryConditionChecker vcc = new VictoryConditionChecker(board, board, board, board, victoryHandler);
        final KnockoutResolutionHandler koResolution = new KnockoutResolutionHandler(runtimes, turnManager, vcc);
        final KnockoutManager koManager = new KnockoutManager(board, board, koResolution);
        final DrawPhaseExecutor drawExec = new DrawPhaseExecutor(runtimes, turnManager, victoryHandler);
        final TurnInPlayTracker turnInPlayTracker = new TurnInPlayTracker(runtimes);

        turnManager.registerListener(drawExec);
        turnManager.registerListener(koManager);
        turnManager.registerListener(vcc);
        turnManager.registerListener(turnInPlayTracker);

        final RuleValidator ruleValidator = new RuleValidator(turnManager, statusManagers, board, board, board, board);

        session.setKnockoutHandler(koResolution);
        session.setTurnManager(turnManager);
        session.setRuleValidator(ruleValidator);
        session.setVictoryConditionChecker(vcc);
        session.setup();
        session.start();
        return turnManager;
    }

    private void triggerBotTurnIfNeeded(final MatchSession session, final int firstPlayerIndex, final String matchId) {
        final String activePlayerId = session.getPlayerIds().get(firstPlayerIndex);
        if (activePlayerId == null || !activePlayerId.startsWith("Bot-")) {
            return;
        }
        new Thread(() -> {
            try {
                // Esperar 6 segundos para que el frontend termine la animación de la moneda
                Thread.sleep(6000);
                botDecisionService.evaluateAndPlay(matchId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Handles a victory result. For a normal win (Prize, BenchOut, DeckOut) the session
     * is finished and removed. For {@link VictoryResult.SuddenDeath} the session is reset
     * to a 1-prize-per-player sudden-death state and the match continues (Rulebook §6).
     *
     * @param matchId   the match identifier
     * @param session   the session to finish or reset
     * @param result    the victory result from the engine
     */
    private void handleVictory(final String matchId,
                                final MatchSession session,
                                final VictoryResult result) {
        final String topicA = MATCH_TOPIC_BASE + matchId + PLAYER_SUB_PATH + session.getPlayerIdA();
        final String topicB = MATCH_TOPIC_BASE + matchId + PLAYER_SUB_PATH + session.getPlayerIdB();

        if (result instanceof VictoryResult.SuddenDeath) {
            // Rulebook §6: both players start a new game with 1 Prize card each.
            session.resetForSuddenDeath();
            final GameStateResponseDTO viewA = perspectiveMapper.toResponse(session, 0);
            final GameStateResponseDTO viewB = perspectiveMapper.toResponse(session, 1);
            messaging.convertAndSend(topicA, viewA);
            messaging.convertAndSend(topicB, viewB);
            // Session stays in registry — match continues in sudden-death mode.
            return;
        }

        if (result instanceof VictoryResult.PrizeVictory prize) {
            session.setWinnerId(session.getPlayerIds().get(prize.winnerPlayerIndex()));
            session.setVictoryReason("PRIZE_CARDS");
        } else if (result instanceof VictoryResult.BenchOutVictory bench) {
            session.setWinnerId(session.getPlayerIds().get(bench.winnerPlayerIndex()));
            session.setVictoryReason("NO_BENCH_POKEMON");
        } else if (result instanceof VictoryResult.DeckOutVictory deck) {
            session.setWinnerId(session.getPlayerIds().get(deck.winnerPlayerIndex()));
            session.setVictoryReason("DECK_OUT");
        }

        session.finish();
        final GameStateResponseDTO viewA = perspectiveMapper.toResponse(session, 0);
        final GameStateResponseDTO viewB = perspectiveMapper.toResponse(session, 1);
        messaging.convertAndSend(topicA, viewA);
        messaging.convertAndSend(topicB, viewB);
        registry.remove(matchId);
    }
}
