package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Tests for VictoryResult sealed type. FR-006.
 */
class VictoryResultTest {

    @Test
    void shouldReturnWinnerIndexWhenPrizeVictoryIsCreated() {
        VictoryResult result = new VictoryResult.PrizeVictory(0);

        assertEquals(0, ((VictoryResult.PrizeVictory) result).winnerPlayerIndex());
    }

    @Test
    void shouldReturnWinnerIndexWhenBenchOutVictoryIsCreated() {
        VictoryResult result = new VictoryResult.BenchOutVictory(1);

        assertEquals(1, ((VictoryResult.BenchOutVictory) result).winnerPlayerIndex());
    }

    @Test
    void shouldReturnWinnerIndexWhenDeckOutVictoryIsCreated() {
        VictoryResult result = new VictoryResult.DeckOutVictory(0);

        assertEquals(0, ((VictoryResult.DeckOutVictory) result).winnerPlayerIndex());
    }

    @Test
    void shouldHaveNoFieldsWhenNoVictoryIsCreated() {
        VictoryResult result = new VictoryResult.NoVictory();

        assertInstanceOf(VictoryResult.class, result);
    }
}
