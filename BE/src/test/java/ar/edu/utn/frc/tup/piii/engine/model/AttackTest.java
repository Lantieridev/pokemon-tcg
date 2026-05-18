package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for Attack record. FR-003.
 */
class AttackTest {

    private static final String VALID_NAME = "Ember";
    private static final int VALID_DAMAGE = 30;
    private static final int ZERO_DAMAGE = 0;

    @Test
    void shouldStoreNameAndBaseDamageWhenConstructedWithValidArguments() {
        Attack attack = new Attack(VALID_NAME, VALID_DAMAGE);

        assertEquals(VALID_NAME, attack.name());
        assertEquals(VALID_DAMAGE, attack.baseDamage());
    }

    @Test
    void shouldAllowZeroBaseDamageWhenConstructed() {
        Attack attack = new Attack(VALID_NAME, ZERO_DAMAGE);

        assertEquals(ZERO_DAMAGE, attack.baseDamage());
    }

    @Test
    void shouldThrowWhenNameIsNullWhenConstructed() {
        assertThrows(IllegalArgumentException.class, () -> new Attack(null, VALID_DAMAGE));
    }

    @Test
    void shouldThrowWhenNameIsBlankWhenConstructed() {
        assertThrows(IllegalArgumentException.class, () -> new Attack("   ", VALID_DAMAGE));
    }

    @Test
    void shouldThrowWhenBaseDamageIsNegativeWhenConstructed() {
        assertThrows(IllegalArgumentException.class, () -> new Attack(VALID_NAME, -1));
    }
}
