package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the AttackModifierResult sealed type hierarchy. FR-004.
 */
class AttackModifierResultTest {

    @Test
    void shouldConstructProceedRecord() {
        AttackModifierResult result = new AttackModifierResult.Proceed();
        assertNotNull(result);
        assertTrue(result instanceof AttackModifierResult);
    }

    @Test
    void shouldConstructConfusionFailedRecordWithDamageCounters() {
        AttackModifierResult.ConfusionFailed result = new AttackModifierResult.ConfusionFailed(3);
        assertEquals(3, result.selfDamageCounters());
    }

    @Test
    void shouldDistinguishProceedFromConfusionFailed() {
        AttackModifierResult proceed = new AttackModifierResult.Proceed();
        AttackModifierResult confusionFailed = new AttackModifierResult.ConfusionFailed(3);
        assertFalse(proceed instanceof AttackModifierResult.ConfusionFailed);
        assertTrue(confusionFailed instanceof AttackModifierResult.ConfusionFailed);
    }
}
