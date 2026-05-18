package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for BetweenTurnsPhase. FR-001.
 */
class BetweenTurnsPhaseTest {

    @Test
    void shouldReturnCorrectNameForBetweenTurnsPhase() {
        assertEquals("BETWEEN_TURNS", new BetweenTurnsPhase().name());
    }
}
