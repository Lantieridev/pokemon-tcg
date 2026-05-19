package ar.edu.utn.frc.tup.piii.engine.listener;

import ar.edu.utn.frc.tup.piii.engine.model.VictoryResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for VictoryHandler functional interface. FR-008.
 */
class VictoryHandlerTest {

    @Test
    void shouldCompileWhenLambdaIsAssignedToVictoryHandlerWhenResultIsNoVictory() {
        VictoryHandler handler = result -> { };

        assertNotNull(handler);
    }

    @Test
    void shouldInvokeLambdaWhenOnVictoryIsCalledWithPrizeVictory() {
        VictoryResult[] captured = new VictoryResult[1];
        VictoryHandler handler = result -> captured[0] = result;
        VictoryResult prize = new VictoryResult.PrizeVictory(0);

        handler.onVictory(prize);

        assertNotNull(captured[0]);
    }
}
