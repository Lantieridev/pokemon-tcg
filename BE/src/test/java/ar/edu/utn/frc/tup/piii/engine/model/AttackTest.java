package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import java.util.List;

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
        Attack attack = new Attack(VALID_NAME, VALID_DAMAGE, List.of());

        assertEquals(VALID_NAME, attack.name());
        assertEquals(VALID_DAMAGE, attack.baseDamage());
    }

    @Test
    void shouldAllowZeroBaseDamageWhenConstructed() {
        Attack attack = new Attack(VALID_NAME, ZERO_DAMAGE, List.of());

        assertEquals(ZERO_DAMAGE, attack.baseDamage());
    }

    @Test
    void shouldThrowWhenNameIsNullWhenConstructed() {
        assertThrows(IllegalArgumentException.class, () -> new Attack(null, VALID_DAMAGE, List.of()));
    }

    @Test
    void shouldThrowWhenNameIsBlankWhenConstructed() {
        assertThrows(IllegalArgumentException.class, () -> new Attack("   ", VALID_DAMAGE, List.of()));
    }

    @Test
    void shouldThrowWhenBaseDamageIsNegativeWhenConstructed() {
        assertThrows(IllegalArgumentException.class, () -> new Attack(VALID_NAME, -1, List.of()));
    }

    @Test
    void shouldThrowWhenRequiredEnergiesIsNullWhenConstructed() {
        assertThrows(IllegalArgumentException.class, () -> new Attack(VALID_NAME, VALID_DAMAGE, null));
    }

    @Test
    void shouldReturnEmptyRequiredEnergiesWhenConstructedWithEmptyList() {
        Attack attack = new Attack(VALID_NAME, VALID_DAMAGE, List.of());

        assertEquals(0, attack.requiredEnergies().size());
    }

    @Test
    void shouldReturnRequiredEnergiesWhenConstructedWithEnergyList() {
        Attack attack = new Attack(VALID_NAME, VALID_DAMAGE, List.of(PokemonType.FIRE, PokemonType.COLORLESS));

        assertEquals(2, attack.requiredEnergies().size());
        assertEquals(PokemonType.FIRE, attack.requiredEnergies().get(0));
        assertEquals(PokemonType.COLORLESS, attack.requiredEnergies().get(1));
    }
}
