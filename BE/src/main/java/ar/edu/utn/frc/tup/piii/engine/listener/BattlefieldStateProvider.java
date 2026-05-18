package ar.edu.utn.frc.tup.piii.engine.listener;

import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;

/**
 * Provides the current active Pokémon for each player on the battlefield.
 * Used by KnockoutManager to query player states during phase transition events. FR-013.
 */
public interface BattlefieldStateProvider {

    /**
     * Returns the active Pokémon for the given player, or {@code null} if the player
     * has no active Pokémon (e.g. the slot is empty between KO and replacement).
     *
     * @param playerIndex zero-based index of the player
     * @return active Pokémon state, or null
     */
    BattlePokemonState getActivePokemon(int playerIndex);
}
