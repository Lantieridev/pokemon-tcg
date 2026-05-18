package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.listener.BattlefieldStateProvider;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;

/**
 * Test helper — in-memory implementation of BattlefieldStateProvider.
 * Holds two player slots; null is a valid value (no active Pokémon).
 */
public class FakeBattlefieldStateProvider implements BattlefieldStateProvider {

    private final BattlePokemonState[] actives;

    public FakeBattlefieldStateProvider(final BattlePokemonState player0, final BattlePokemonState player1) {
        this.actives = new BattlePokemonState[]{player0, player1};
    }

    @Override
    public BattlePokemonState getActivePokemon(final int playerIndex) {
        return actives[playerIndex];
    }
}
