package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for DrawPhase. FR-001.
 */
class DrawPhaseTest {

    @Test
    void shouldReturnCorrectNameForDrawPhase() {
        assertEquals("DRAW", new DrawPhase().name());
    }
}
