package ar.edu.utn.frc.tup.piii.engine.statuseffect;

import ar.edu.utn.frc.tup.piii.engine.model.ActivePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for AsleepEffect (DORMIDO). FR-008.
 */
class AsleepEffectTest {

    private AsleepEffect effect;
    private ActivePokemonState state;
    private CoinFlipper coin;

    @BeforeEach
    void setUp() {
        effect = new AsleepEffect();
        state = Mockito.mock(ActivePokemonState.class);
        coin = Mockito.mock(CoinFlipper.class);
    }

    @Test
    void shouldWakeUpWhenSleepCoinIsHeads() {
        when(coin.flip()).thenReturn(true);
        assertTrue(effect.processBetweenTurns(state, coin));
    }

    @Test
    void shouldStayAsleepWhenSleepCoinIsTails() {
        when(coin.flip()).thenReturn(false);
        assertFalse(effect.processBetweenTurns(state, coin));
    }

    @Test
    void shouldBlockAttackWhenAsleep() {
        assertTrue(effect.blocksAttack());
    }

    @Test
    void shouldBlockRetreatWhenAsleep() {
        assertTrue(effect.blocksRetreat());
    }

    @Test
    void shouldNotAddDamageCountersWhenProcessingSleepBetweenTurns() {
        when(coin.flip()).thenReturn(true);
        effect.processBetweenTurns(state, coin);
        verify(state, never()).addDamageCounters(Mockito.anyInt());
    }
}
