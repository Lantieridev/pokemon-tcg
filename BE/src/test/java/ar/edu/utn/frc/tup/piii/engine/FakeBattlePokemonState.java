package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;

/**
 * Test helper — in-memory implementation of BattlePokemonState.
 * Mutable damage counters; all other fields are final and set via constructor.
 */
public class FakeBattlePokemonState implements BattlePokemonState {

    private int damageCounters;
    private final int maxHp;
    private final PokemonType type;
    private final PokemonType weaknessType;
    private final PokemonType resistanceType;
    private final boolean ex;

    public FakeBattlePokemonState(final int maxHp, final PokemonType type,
                                  final PokemonType weaknessType,
                                  final PokemonType resistanceType,
                                  final boolean ex) {
        this.maxHp = maxHp;
        this.type = type;
        this.weaknessType = weaknessType;
        this.resistanceType = resistanceType;
        this.ex = ex;
    }

    @Override
    public int getDamageCounters() {
        return damageCounters;
    }

    @Override
    public void addDamageCounters(final int amount) {
        damageCounters += amount;
    }

    @Override
    public int getMaxHp() {
        return maxHp;
    }

    @Override
    public PokemonType getPokemonType() {
        return type;
    }

    @Override
    public PokemonType getWeaknessType() {
        return weaknessType;
    }

    @Override
    public PokemonType getResistanceType() {
        return resistanceType;
    }

    @Override
    public boolean isEx() {
        return ex;
    }
}
