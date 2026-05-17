package ar.edu.utn.frc.tup.piii.engine.statuseffect;

import ar.edu.utn.frc.tup.piii.engine.model.ActivePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for BurnedEffect (QUEMADO). FR-007.
 */
class BurnedEffectTest {

    private BurnedEffect effect;
    private ActivePokemonState state;
    private CoinFlipper coin;

    @BeforeEach
    void setUp() {
        effect = new BurnedEffect();
        state = Mockito.mock(ActivePokemonState.class);
        coin = Mockito.mock(CoinFlipper.class);
    }

    @Test
    void shouldAddTwoDamageCountersWhenBurnCoinIsTails() {
        when(coin.flip()).thenReturn(false);
        effect.processBetweenTurns(state, coin);
        verify(state).addDamageCounters(2);
    }

    @Test
    void shouldNotAddDamageCountersWhenBurnCoinIsHeads() {
        when(coin.flip()).thenReturn(true);
        effect.processBetweenTurns(state, coin);
        verify(state, never()).addDamageCounters(Mockito.anyInt());
    }

    @Test
    void shouldFlipCoinExactlyOnceWhenProcessingBurnBetweenTurns() {
        when(coin.flip()).thenReturn(true);
        effect.processBetweenTurns(state, coin);
        verify(coin, times(1)).flip();
    }

    @Test
    void shouldNotBlockAttackOrRetreatWhenBurned() {
        assertFalse(effect.blocksAttack());
        assertFalse(effect.blocksRetreat());
    }
}
