package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for DamageResult record. FR-006.
 */
class DamageResultTest {

    private static final int DAMAGE_35 = 35;
    private static final int COUNTERS_FOR_35 = 3;
    private static final int ZERO = 0;

    @Test
    void shouldComputeCountersAsIntegerDivisionByTenWhenFinalDamageIs35() {
        DamageResult result = new DamageResult(DAMAGE_35, COUNTERS_FOR_35);

        assertEquals(DAMAGE_35, result.finalDamage());
        assertEquals(COUNTERS_FOR_35, result.damageCountersToPlace());
    }

    @Test
    void shouldYieldZeroCountersWhenFinalDamageIsZero() {
        DamageResult result = new DamageResult(ZERO, ZERO);

        assertEquals(ZERO, result.finalDamage());
        assertEquals(ZERO, result.damageCountersToPlace());
    }

    @Test
    void shouldThrowWhenFinalDamageIsNegativeWhenConstructed() {
        assertThrows(IllegalArgumentException.class, () -> new DamageResult(-1, 0));
    }

    @Test
    void shouldThrowWhenCountersDontMatchInvariantWhenConstructed() {
        // finalDamage=35 / 10 == 3, but counters=4 violates invariant
        assertThrows(IllegalArgumentException.class, () -> new DamageResult(DAMAGE_35, 4));
    }
}
