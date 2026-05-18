package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for DamageModifier functional interface. FR-004.
 */
class DamageModifierTest {

    private static final int BASE_DAMAGE = 30;
    private static final int ADD_TEN = 10;
    private static final int EXPECTED_AFTER_ADD = 40;
    private static final int MULTIPLIER = 2;
    private static final int EXPECTED_AFTER_COMPOSE = 80;

    @Test
    void shouldApplyLambdaModifierToCurrentDamageWhenInvoked() {
        DamageModifier modifier = dmg -> dmg + ADD_TEN;

        int result = modifier.apply(BASE_DAMAGE);

        assertEquals(EXPECTED_AFTER_ADD, result);
    }

    @Test
    void shouldComposeTwoModifiersInOrderWhenFoldedOverList() {
        List<DamageModifier> modifiers = List.of(
                dmg -> dmg + ADD_TEN,
                dmg -> dmg * MULTIPLIER
        );

        int dmg = BASE_DAMAGE;
        for (DamageModifier modifier : modifiers) {
            dmg = modifier.apply(dmg);
        }

        assertEquals(EXPECTED_AFTER_COMPOSE, dmg);
    }
}
