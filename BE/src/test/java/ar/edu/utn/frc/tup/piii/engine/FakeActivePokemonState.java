package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.model.ActivePokemonState;

/**
 * Test helper — simple in-memory implementation of ActivePokemonState.
 * Accumulates damage counters without any domain rules.
 */
public class FakeActivePokemonState implements ActivePokemonState {

    private int damageCounters = 0;

    @Override
    public int getDamageCounters() {
        return damageCounters;
    }

    @Override
    public void addDamageCounters(final int amount) {
        this.damageCounters += amount;
    }
}
