package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Minimal interface representing the mutable state of the active Pokémon
 * that status effects may read or modify. FR-003.
 */
public interface ActivePokemonState {

    /**
     * Returns the current number of damage counters on this Pokémon.
     *
     * @return damage counter count
     */
    int getDamageCounters();

    /**
     * Adds the given number of damage counters to this Pokémon.
     *
     * @param amount number of counters to add (must be positive)
     */
    void addDamageCounters(int amount);
}
