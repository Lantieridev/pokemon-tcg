package ar.edu.utn.frc.tup.piii.engine.victory;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlefieldStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.FakeBenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakeDeckStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakePrizeStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.listener.PhaseEvent;
import ar.edu.utn.frc.tup.piii.engine.manager.VictoryConditionChecker;
import ar.edu.utn.frc.tup.piii.engine.model.BetweenTurnsPhase;
import ar.edu.utn.frc.tup.piii.engine.model.DrawPhase;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.VictoryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that prize attribution is based on the OWNER of the KO'd Pokémon,
 * not on activePlayerIndex. FR-017.
 *
 * <p>Scenario: during BetweenTurnsPhase, Player 0's Pokémon is KO'd by poison
 * that Player 1 inflicted. The prize should go to Player 1 (the opponent of
 * the KO'd Pokémon's owner), NOT to Player 0 (activePlayerIndex).</p>
 */
class KOFromPoisonPrizesTest {

    private static final int PLAYER_0 = 0;
    private static final int PLAYER_1 = 1;
    private static final int PRIZES_ZERO = 0;
    private static final int PRIZES_TWO = 2;
    private static final int BENCH_NOT_EMPTY = 1;
    private static final int MAX_HP = 100;

    private FakePrizeStateProvider prizeProvider;
    private FakeDeckStateProvider deckProvider;
    private FakeBenchStateProvider benchProvider;
    private FakeBattlefieldStateProvider battlefieldProvider;
    private List<VictoryResult> captured;
    private VictoryConditionChecker checker;

    @BeforeEach
    void setUp() {
        prizeProvider = new FakePrizeStateProvider();
        deckProvider = new FakeDeckStateProvider();
        benchProvider = new FakeBenchStateProvider();
        battlefieldProvider = new FakeBattlefieldStateProvider(null, null);
        captured = new ArrayList<>();
        checker = new VictoryConditionChecker(
                prizeProvider, deckProvider, benchProvider, battlefieldProvider,
                result -> captured.add(result));
    }

    @Test
    void shouldAwardPrizesToOpponentWhenKoFromPoisonDuringBetweenTurns() {
        // Setup: Player 1's Pokémon is active (activePlayerIndex=1 from TurnStarted)
        // Player 0's Pokémon was poisoned during Player 1's attack last turn.
        // Now in BetweenTurns (still player 1's turn cycle), Player 0's Pokémon gets KO'd.
        // Player 1 should receive the prizes.
        // Player 0 has 2 prizes left (not 0), Player 1 has 0 prizes left (they win).
        prizeProvider.set(PLAYER_0, PRIZES_TWO);  // Player 0's prizes remaining: still has cards
        prizeProvider.set(PLAYER_1, PRIZES_ZERO); // Player 1 took last prize from this KO
        benchProvider.set(PLAYER_0, BENCH_NOT_EMPTY);
        benchProvider.set(PLAYER_1, BENCH_NOT_EMPTY);

        // Player 1 is active (their turn cycle: they attacked, now in BetweenTurns)
        checker.on(new PhaseEvent.TurnStarted(PLAYER_1, new DrawPhase()));
        checker.on(new PhaseEvent.PhaseEntered(PLAYER_1, new BetweenTurnsPhase()));

        // Player 0's Pokémon dies from poison during BetweenTurns
        // The knocked owner is player 0 — prizes go to player 1
        FakeBattlePokemonState knockedPlayer0Pokemon = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        checker.onKnockout(knockedPlayer0Pokemon, 1);

        assertEquals(1, captured.size(), "Exactly one VictoryResult should be fired");
        assertInstanceOf(VictoryResult.PrizeVictory.class, captured.get(0));
        assertEquals(PLAYER_1,
                ((VictoryResult.PrizeVictory) captured.get(0)).winnerPlayerIndex(),
                "Player 1 should win (they are the opponent of the KO'd Pokémon's owner)");
    }

    @Test
    void shouldNotFireVictoryWhenBetweenTurnsKoDoesNotDepletePrizes() {
        // Player 0's Pokémon is KO'd during Player 1's BetweenTurns,
        // but Player 1 still has prizes left — no victory yet
        prizeProvider.set(PLAYER_0, PRIZES_TWO);
        prizeProvider.set(PLAYER_1, PRIZES_TWO); // still has prizes
        benchProvider.set(PLAYER_0, BENCH_NOT_EMPTY);
        benchProvider.set(PLAYER_1, BENCH_NOT_EMPTY);

        checker.on(new PhaseEvent.TurnStarted(PLAYER_1, new DrawPhase()));

        FakeBattlePokemonState knocked = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        checker.onKnockout(knocked, 1);

        assertTrue(captured.isEmpty(), "No victory should fire when prizes remain");
    }
}
