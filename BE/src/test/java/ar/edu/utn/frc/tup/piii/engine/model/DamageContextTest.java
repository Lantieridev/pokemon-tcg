package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for DamageContext record. FR-005.
 */
class DamageContextTest {

    private static final int HP = 80;
    private static final int BASE_DAMAGE = 40;
    private static final String ATTACK_NAME = "Bubble";

    private FakeBattlePokemonState attacker() {
        return new FakeBattlePokemonState(HP, PokemonType.WATER, null, null, false);
    }

    private FakeBattlePokemonState defender() {
        return new FakeBattlePokemonState(HP, PokemonType.FIRE, PokemonType.WATER, null, false);
    }

    private Attack attack() {
        return new Attack(ATTACK_NAME, BASE_DAMAGE, List.of());
    }

    @Test
    void shouldBeValidWithEmptyModifierListsWhenConstructed() {
        DamageContext ctx = new DamageContext(
                attacker(), defender(), attack(), List.of(), List.of());

        assertNotNull(ctx);
    }

    @Test
    void shouldThrowWhenAttackerIsNullWhenConstructed() {
        assertThrows(Exception.class, () ->
                new DamageContext(null, defender(), attack(), List.of(), List.of()));
    }

    @Test
    void shouldThrowWhenDefenderIsNullWhenConstructed() {
        assertThrows(Exception.class, () ->
                new DamageContext(attacker(), null, attack(), List.of(), List.of()));
    }

    @Test
    void shouldThrowWhenAttackIsNullWhenConstructed() {
        assertThrows(Exception.class, () ->
                new DamageContext(attacker(), defender(), null, List.of(), List.of()));
    }

    @Test
    void shouldThrowWhenAttackerModifiersIsNullWhenConstructed() {
        assertThrows(Exception.class, () ->
                new DamageContext(attacker(), defender(), attack(), null, List.of()));
    }

    @Test
    void shouldThrowWhenDefenderModifiersIsNullWhenConstructed() {
        assertThrows(Exception.class, () ->
                new DamageContext(attacker(), defender(), attack(), List.of(), null));
    }

    @Test
    void shouldReturnImmutableModifierListsWhenQueried() {
        DamageContext ctx = new DamageContext(
                attacker(), defender(), attack(), List.of(), List.of());

        assertThrows(UnsupportedOperationException.class,
                () -> ctx.attackerModifiers().add(dmg -> dmg + 10));
    }
}
