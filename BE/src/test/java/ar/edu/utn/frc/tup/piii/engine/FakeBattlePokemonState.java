package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;

import java.util.ArrayList;
import java.util.List;

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
    private int retreatCost = 0;
    private final List<PokemonType> attachedEnergies = new ArrayList<>();
    private boolean toolAttached = false;

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

    public void setRetreatCost(final int cost) {
        this.retreatCost = cost;
    }

    public void addAttachedEnergy(final PokemonType pokemonType) {
        attachedEnergies.add(pokemonType);
    }

    @Override
    public int getRetreatCost() {
        return retreatCost;
    }

    @Override
    public List<PokemonType> getAttachedEnergies() {
        return List.copyOf(attachedEnergies);
    }

    public void setToolAttached(final boolean attached) {
        this.toolAttached = attached;
    }

    @Override
    public boolean hasToolAttached() {
        return toolAttached;
    }
}
