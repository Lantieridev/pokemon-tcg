package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for BattlePokemonState interface via FakeBattlePokemonState. FR-002.
 */
class BattlePokemonStateTest {

    private static final int BASE_HP = 100;
    private static final int COUNTER_AMOUNT = 5;
    private static final int DOUBLE_COUNTER_AMOUNT = 10;

    @Test
    void shouldReturnNullWeaknessTypeWhenNoWeaknessConfigured() {
        FakeBattlePokemonState state = new FakeBattlePokemonState(
                BASE_HP, PokemonType.FIRE, null, PokemonType.WATER, false);

        assertNull(state.getWeaknessType());
    }

    @Test
    void shouldReturnNullResistanceTypeWhenNoResistanceConfigured() {
        FakeBattlePokemonState state = new FakeBattlePokemonState(
                BASE_HP, PokemonType.FIRE, PokemonType.WATER, null, false);

        assertNull(state.getResistanceType());
    }

    @Test
    void shouldAccumulateDamageCountersWhenAddDamageCountersIsCalled() {
        FakeBattlePokemonState state = new FakeBattlePokemonState(
                BASE_HP, PokemonType.FIRE, null, null, false);

        state.addDamageCounters(COUNTER_AMOUNT);
        state.addDamageCounters(COUNTER_AMOUNT);

        assertEquals(DOUBLE_COUNTER_AMOUNT, state.getDamageCounters());
    }

    @Test
    void shouldReturnMaxHpWhenQueried() {
        FakeBattlePokemonState state = new FakeBattlePokemonState(
                BASE_HP, PokemonType.WATER, null, null, false);

        assertEquals(BASE_HP, state.getMaxHp());
    }

    @Test
    void shouldReportIsExTrueWhenConfiguredAsEx() {
        FakeBattlePokemonState state = new FakeBattlePokemonState(
                BASE_HP, PokemonType.PSYCHIC, null, null, true);

        assertTrue(state.isEx());
    }
}
