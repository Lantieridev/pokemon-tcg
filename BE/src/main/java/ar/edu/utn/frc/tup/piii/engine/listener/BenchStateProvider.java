package ar.edu.utn.frc.tup.piii.engine.listener;

import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;

import java.util.List;

/**
 * Provides bench state for a player (excludes the active slot).
 * A return value of {@code 0} for getBenchSize means the player has no Pokémon on the bench.
 * FR-008.
 */
public interface BenchStateProvider {

    /**
     * Returns the number of non-fainted Pokémon on the given player's bench.
     * The active Pokémon slot is not counted.
     *
     * @param playerIndex zero-based player index
     * @return bench size (>= 0)
     */
    int getBenchSize(int playerIndex);

    /**
     * Returns all Pokémon currently on the given player's bench.
     *
     * @param playerIndex zero-based player index
     * @return non-null, possibly empty list
     */
    List<BattlePokemonState> getBenchedPokemon(int playerIndex);
}
