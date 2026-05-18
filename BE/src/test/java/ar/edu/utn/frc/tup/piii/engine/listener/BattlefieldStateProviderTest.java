package ar.edu.utn.frc.tup.piii.engine.listener;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.FakeBattlefieldStateProvider;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for the BattlefieldStateProvider interface (via FakeBattlefieldStateProvider). FR-013.
 */
class BattlefieldStateProviderTest {

    private static final int MAX_HP = 100;
    private static final int PLAYER_0 = 0;
    private static final int PLAYER_1 = 1;

    private BattlePokemonState player0Pokemon;
    private BattlePokemonState player1Pokemon;
    private BattlefieldStateProvider provider;

    @BeforeEach
    void setUp() {
        player0Pokemon = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, false);
        player1Pokemon = new FakeBattlePokemonState(MAX_HP, PokemonType.WATER, null, null, false);
        provider = new FakeBattlefieldStateProvider(player0Pokemon, player1Pokemon);
    }

    @Test
    void shouldReturnPlayer0PokemonWhenPlayerIndexIsZero() {
        assertSame(player0Pokemon, provider.getActivePokemon(PLAYER_0));
    }

    @Test
    void shouldReturnPlayer1PokemonWhenPlayerIndexIsOne() {
        assertSame(player1Pokemon, provider.getActivePokemon(PLAYER_1));
    }
}
