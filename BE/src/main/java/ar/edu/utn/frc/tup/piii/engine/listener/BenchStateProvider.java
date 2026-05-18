package ar.edu.utn.frc.tup.piii.engine.listener;

/**
 * Provides bench-size state for a player (excludes the active slot).
 * A return value of {@code 0} means the player has no Pokémon on the bench.
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
}
