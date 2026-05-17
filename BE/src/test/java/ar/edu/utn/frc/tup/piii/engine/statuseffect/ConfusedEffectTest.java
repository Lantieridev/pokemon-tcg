package ar.edu.utn.frc.tup.piii.engine.statuseffect;

import ar.edu.utn.frc.tup.piii.engine.model.ActivePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for ConfusedEffect (CONFUNDIDO). FR-010.
 */
class ConfusedEffectTest {

    private ConfusedEffect effect;
    private ActivePokemonState state;
    private CoinFlipper coin;

    @BeforeEach
    void setUp() {
        effect = new ConfusedEffect();
        state = Mockito.mock(ActivePokemonState.class);
        coin = Mockito.mock(CoinFlipper.class);
    }

    @Test
    void shouldNotBlockAttackWhenConfused() {
        assertFalse(effect.blocksAttack());
    }

    @Test
    void shouldNotBlockRetreatWhenConfused() {
        assertFalse(effect.blocksRetreat());
    }

    @Test
    void shouldBeNoOpBetweenTurnsWhenConfused() {
        boolean result = effect.processBetweenTurns(state, coin);
        assertFalse(result);
        verify(coin, never()).flip();
        verify(state, never()).addDamageCounters(Mockito.anyInt());
    }
}
