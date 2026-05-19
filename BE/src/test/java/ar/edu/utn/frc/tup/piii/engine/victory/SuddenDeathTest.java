package ar.edu.utn.frc.tup.piii.engine.victory;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlefieldStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.FakeBenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakeDeckStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakePrizeStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.PhaseEvent;
import ar.edu.utn.frc.tup.piii.engine.manager.VictoryConditionChecker;
import ar.edu.utn.frc.tup.piii.engine.model.DrawPhase;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.VictoryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Tests for the Sudden Death victory condition (FR-020).
 * Both players simultaneously meeting a victory condition results in SuddenDeath,
 * NOT two separate win events.
 */
class SuddenDeathTest {

    private static final int PLAYER_0 = 0;
    private static final int PLAYER_1 = 1;
    private static final int PRIZES_ZERO = 0;
    private static final int BENCH_NOT_EMPTY = 1;
    private static final int PRIZES_TO_TAKE = 1;
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
    void shouldEmitSuddenDeathWhenBothPlayersSimultaneouslyMeetVictoryCondition() {
        // Both players have 0 prize cards — either one would normally win independently
        prizeProvider.set(PLAYER_0, PRIZES_ZERO);
        prizeProvider.set(PLAYER_1, PRIZES_ZERO);
        benchProvider.set(PLAYER_0, BENCH_NOT_EMPTY);
        benchProvider.set(PLAYER_1, BENCH_NOT_EMPTY);

        // Player 0 is active
        checker.on(new PhaseEvent.TurnStarted(PLAYER_0, new DrawPhase()));

        FakeBattlePokemonState knocked = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        // KO is called — both players have 0 prizes; should trigger SuddenDeath
        checker.onKnockout(knocked, PRIZES_TO_TAKE);

        assertEquals(1, captured.size(), "Exactly one VictoryResult should be emitted");
        assertInstanceOf(VictoryResult.SuddenDeath.class, captured.get(0),
                "VictoryResult should be SuddenDeath when both players simultaneously meet victory condition");
    }
}
