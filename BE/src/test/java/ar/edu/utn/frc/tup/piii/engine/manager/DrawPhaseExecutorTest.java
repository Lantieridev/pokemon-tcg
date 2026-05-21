package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Bench;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.Deck;
import ar.edu.utn.frc.tup.piii.engine.model.DiscardPile;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.Hand;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.VictoryResult;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DrawPhaseExecutorTest {

    private static final int STARTING_PLAYER = 0;
    private static final int OTHER_PLAYER = 1;

    private TurnManager turnManager;
    private List<PlayerRuntime> runtimes;
    private List<VictoryResult> capturedVictories;

    private PlayerRuntime runtimeFor(final int playerIndex) {
        return runtimes.get(playerIndex);
    }

    @BeforeEach
    void setUp() {
        turnManager = new TurnManager();
        turnManager.setStartingPlayer(STARTING_PLAYER);
        capturedVictories = new ArrayList<>();

        runtimes = List.of(
                buildRuntime(5),
                buildRuntime(5)
        );
    }

    @Test
    void shouldSkipDrawForStartingPlayerOnFirstTurn() {
        registerExecutor(runtimes, turnManager);

        turnManager.startTurn(STARTING_PLAYER);

        assertEquals(0, runtimeFor(STARTING_PLAYER).getHand().size(),
                "Starting player must not draw on their first turn");
        assertEquals(5, runtimeFor(STARTING_PLAYER).getDeck().size());
    }

    @Test
    void shouldDrawCardForOtherPlayerOnFirstTurn() {
        registerExecutor(runtimes, turnManager);

        turnManager.startTurn(OTHER_PLAYER);

        assertEquals(1, runtimeFor(OTHER_PLAYER).getHand().size(),
                "Non-starting player draws 1 on their first turn");
        assertEquals(4, runtimeFor(OTHER_PLAYER).getDeck().size());
    }

    @Test
    void shouldDrawCardForStartingPlayerOnSecondTurn() {
        registerExecutor(runtimes, turnManager);

        // Player 0 (starting) first turn — skips draw
        turnManager.startTurn(STARTING_PLAYER);
        assertEquals(0, runtimeFor(STARTING_PLAYER).getHand().size());
        turnManager.endDraw();
        turnManager.passTurn();
        // endBetweenTurns auto-starts Player 1's turn → player 1 draws
        turnManager.endBetweenTurns();
        assertEquals(1, runtimeFor(OTHER_PLAYER).getHand().size());

        // Advance player 1's turn
        turnManager.endDraw();
        turnManager.passTurn();
        // endBetweenTurns auto-starts Player 0's second turn → player 0 draws
        turnManager.endBetweenTurns();

        assertEquals(1, runtimeFor(STARTING_PLAYER).getHand().size(),
                "Starting player draws 1 on their second turn");
    }

    @Test
    void shouldFireDeckOutVictoryWhenDeckIsEmpty() {
        final PlayerRuntime emptyPlayer0 = buildRuntime(1);
        emptyPlayer0.getDeck().draw();  // drain deck to 0

        final List<PlayerRuntime> runtimesWithEmptyDeck = List.of(emptyPlayer0, buildRuntime(5));

        // Configure: player 1 is the starting player, so player 0 draws normally
        final TurnManager tm = new TurnManager();
        tm.setStartingPlayer(OTHER_PLAYER);
        registerExecutorFor(runtimesWithEmptyDeck, tm);

        // Player 0 starts their turn — deck is empty → deck-out
        tm.startTurn(STARTING_PLAYER);

        assertEquals(1, capturedVictories.size());
        assertInstanceOf(VictoryResult.DeckOutVictory.class, capturedVictories.get(0));
        assertEquals(OTHER_PLAYER,
                ((VictoryResult.DeckOutVictory) capturedVictories.get(0)).winnerPlayerIndex());
    }

    @Test
    void shouldDrawMultipleCardsAcrossConsecutiveTurns() {
        registerExecutor(runtimes, turnManager);

        // Player 1 first turn — draws
        turnManager.startTurn(OTHER_PLAYER);
        assertEquals(1, runtimeFor(OTHER_PLAYER).getHand().size());

        turnManager.endDraw();
        turnManager.passTurn();
        // endBetweenTurns starts player 0's first turn → player 0 skips draw
        turnManager.endBetweenTurns();
        assertEquals(0, runtimeFor(STARTING_PLAYER).getHand().size());

        turnManager.endDraw();
        turnManager.passTurn();
        // endBetweenTurns starts player 1's second turn → player 1 draws again
        turnManager.endBetweenTurns();

        assertEquals(2, runtimeFor(OTHER_PLAYER).getHand().size(),
                "Player 1 should have drawn on both their turns");
    }

    // --- helpers ---

    private void registerExecutor(final List<PlayerRuntime> pr, final TurnManager tm) {
        tm.registerListener(new DrawPhaseExecutor(pr, tm, result -> capturedVictories.add(result)));
    }

    private void registerExecutorFor(final List<PlayerRuntime> pr, final TurnManager tm) {
        tm.registerListener(new DrawPhaseExecutor(pr, tm, result -> capturedVictories.add(result)));
    }

    private static PlayerRuntime buildRuntime(final int deckSize) {
        final List<Card> cards = new ArrayList<>();
        for (int i = 0; i < deckSize; i++) {
            cards.add(new EnergyCard("e-" + i, "Fire Energy", PokemonType.FIRE, true));
        }
        final Deck deck = new Deck(cards);
        final Hand hand = new Hand();
        final Bench bench = new Bench();
        final DiscardPile discard = new DiscardPile();
        final StatusEffectManager sem = new StatusEffectManager(() -> true);
        final FakeBattlePokemonState active =
                new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false);
        return new PlayerRuntime(deck, hand, bench, discard, sem, active);
    }
}
