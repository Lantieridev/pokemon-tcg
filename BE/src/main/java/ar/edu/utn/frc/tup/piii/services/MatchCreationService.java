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
import ar.edu.utn.frc.tup.piii.services.ChatService;
import ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

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

    /**
     * @param registry    stores active sessions (never null)
     * @param messaging   used to broadcast the initial game state after setup (never null)
     * @param chatService used to broadcast setup events like mulligans
     */
    public MatchCreationService(final MatchSessionRegistry registry,
                                 final SimpMessagingTemplate messaging,
                                 final ChatService chatService) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.messaging = Objects.requireNonNull(messaging, "messaging must not be null");
        this.chatService = Objects.requireNonNull(chatService, "chatService must not be null");
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
                               final List<Card> deckBCards) {
        Objects.requireNonNull(playerAId, "playerAId must not be null");
        Objects.requireNonNull(playerBId, "playerBId must not be null");
        Objects.requireNonNull(deckACards, "deckACards must not be null");
        Objects.requireNonNull(deckBCards, "deckBCards must not be null");

        final String matchId = UUID.randomUUID().toString();

        // --- Build mutable components for each player ---
        final Deck deck0 = new Deck(new ArrayList<>(deckACards));
        final Deck deck1 = new Deck(new ArrayList<>(deckBCards));
        final Hand hand0 = new Hand();
        final Hand hand1 = new Hand();
        final Bench bench0 = new Bench();
        final Bench bench1 = new Bench();
        final DiscardPile dp0 = new DiscardPile();
        final DiscardPile dp1 = new DiscardPile();
        final CoinFlipper coinFlipper = new RandomCoinFlipper();
        final StatusEffectManager sem0 = new StatusEffectManager(coinFlipper);
        final StatusEffectManager sem1 = new StatusEffectManager(coinFlipper);

        // --- Setup Phase ---
        final SetupManager setupManager = new SetupManager(coinFlipper);
        final PlayerSetupSlot slot0 = new PlayerSetupSlot(deck0, hand0, bench0);
        final PlayerSetupSlot slot1 = new PlayerSetupSlot(deck1, hand1, bench1);
        final AutoSetupStrategy strategy = new AutoSetupStrategy();
        final SetupResult setupResult = setupManager.execute(slot0, strategy, slot1, strategy);

        // --- Broadcast Mulligans ---
        for (final List<Card> handCards : setupResult.mulligansP0()) {
            final String cardNames = handCards.stream().map(Card::getName).collect(Collectors.joining(", "));
            chatService.addMessage(matchId, new ChatMessageResponse(
                    "Sistema", "Mulligan de " + playerAId + ": " + cardNames, LocalDateTime.now()));
        }
        for (final List<Card> handCards : setupResult.mulligansP1()) {
            final String cardNames = handCards.stream().map(Card::getName).collect(Collectors.joining(", "));
            chatService.addMessage(matchId, new ChatMessageResponse(
                    "Sistema", "Mulligan de " + playerBId + ": " + cardNames, LocalDateTime.now()));
        }

        // --- Build PlayerRuntimes with prize piles from setup ---
        final PlayerRuntime runtime0 = new PlayerRuntime(
                deck0, hand0, bench0, dp0, sem0,
                slot0.getActivePokemon(), new ArrayList<>(slot0.getPrizes()));
        final PlayerRuntime runtime1 = new PlayerRuntime(
                deck1, hand1, bench1, dp1, sem1,
                slot1.getActivePokemon(), new ArrayList<>(slot1.getPrizes()));
        final List<PlayerRuntime> runtimes = List.of(runtime0, runtime1);
        
        sem0.setPlayerRuntime(runtime0);
        sem1.setPlayerRuntime(runtime1);

        // --- Register initial Pokémon in turnsInPlay (active + bench, turnsInPlay = 0) ---
        if (slot0.getActivePokemon() != null) {
            runtime0.recordPokemonEntered(slot0.getActivePokemon());
        }
        bench0.getAll().forEach(runtime0::recordPokemonEntered);
        if (slot1.getActivePokemon() != null) {
            runtime1.recordPokemonEntered(slot1.getActivePokemon());
        }
        bench1.getAll().forEach(runtime1::recordPokemonEntered);

        // --- Build MatchBoard (immutable snapshot fields only) ---
        final PlayerState ps0 = new PlayerState(
                slot0.getActivePokemon(), bench0.getAll(),
                List.of(), slot0.getActivePokemon() != null ? slot0.getActivePokemon().getAttacks() : List.of(),
                deck0.size(), slot0.getPrizes().size(), Map.of());
        final PlayerState ps1 = new PlayerState(
                slot1.getActivePokemon(), bench1.getAll(),
                List.of(), slot1.getActivePokemon() != null ? slot1.getActivePokemon().getAttacks() : List.of(),
                deck1.size(), slot1.getPrizes().size(), Map.of());
        final MatchBoard board = new MatchBoard(List.of(ps0, ps1));
        board.bindRuntimes(runtimes);

        // --- Create MatchSession (needed for VictoryHandler broadcast) ---
        final MatchSession session = new MatchSession(
                matchId, List.of(playerAId, playerBId), board, runtimes);
        session.setCoinFlipper(coinFlipper);

        // --- Wire TurnManager and all PhaseListeners ---
        final TurnManager turnManager = new TurnManager();
        turnManager.setStartingPlayer(setupResult.firstPlayerIndex());

        final VictoryHandler victoryHandler =
                result -> handleVictory(matchId, session, result);

        final VictoryConditionChecker vcc = new VictoryConditionChecker(
                board, board, board, board, victoryHandler);

        final KnockoutResolutionHandler koResolution =
                new KnockoutResolutionHandler(runtimes, turnManager, vcc);

        final KnockoutManager koManager =
                new KnockoutManager(board, board, koResolution);

        final DrawPhaseExecutor drawExec =
                new DrawPhaseExecutor(runtimes, turnManager, victoryHandler);

        final TurnInPlayTracker turnInPlayTracker = new TurnInPlayTracker(runtimes);

        turnManager.registerListener(drawExec);
        turnManager.registerListener(koManager);
        turnManager.registerListener(vcc);
        turnManager.registerListener(turnInPlayTracker);

        // --- Wire RuleValidator ---
        final RuleValidator ruleValidator = new RuleValidator(
                turnManager,
                List.of(sem0, sem1),
                board,
                board,
                board,
                board);

        // --- Attach engine to session ---
        session.setKnockoutHandler(koResolution);
        session.setTurnManager(turnManager);
        session.setRuleValidator(ruleValidator);
        session.setup();
        session.start();

        // --- Register and kick off first turn ---
        registry.register(session);
        turnManager.startTurn(setupResult.firstPlayerIndex());

        return matchId;
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
            messaging.convertAndSend(topicA, result);
            messaging.convertAndSend(topicB, result);
            // Session stays in registry — match continues in sudden-death mode.
            return;
        }

        if (result instanceof VictoryResult.PrizeVictory prize) {
            session.setWinnerId(session.getPlayerIds().get(prize.winnerPlayerIndex()));
        } else if (result instanceof VictoryResult.BenchOutVictory bench) {
            session.setWinnerId(session.getPlayerIds().get(bench.winnerPlayerIndex()));
        } else if (result instanceof VictoryResult.DeckOutVictory deck) {
            session.setWinnerId(session.getPlayerIds().get(deck.winnerPlayerIndex()));
        }

        session.finish();
        messaging.convertAndSend(topicA, result);
        messaging.convertAndSend(topicB, result);
        registry.remove(matchId);
    }
}
