package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for AttackPhase. FR-001.
 */
class AttackPhaseTest {

    @Test
    void shouldReturnCorrectNameForAttackPhase() {
        assertEquals("ATTACK", new AttackPhase().name());
    }
}
