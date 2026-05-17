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
 * Tests for PoisonedEffect (ENVENENADO). FR-006.
 */
class PoisonedEffectTest {

    private PoisonedEffect effect;
    private ActivePokemonState state;
    private CoinFlipper coin;

    @BeforeEach
    void setUp() {
        effect = new PoisonedEffect();
        state = Mockito.mock(ActivePokemonState.class);
        coin = Mockito.mock(CoinFlipper.class);
    }

    @Test
    void shouldAddOneDamageCounterWhenProcessingPoisonBetweenTurns() {
        effect.processBetweenTurns(state, coin);
        verify(state).addDamageCounters(1);
    }

    @Test
    void shouldNotBlockAttackWhenPoisoned() {
        assertFalse(effect.blocksAttack());
    }

    @Test
    void shouldNotBlockRetreatWhenPoisoned() {
        assertFalse(effect.blocksRetreat());
    }

    @Test
    void shouldNotFlipCoinWhenProcessingPoisonBetweenTurns() {
        effect.processBetweenTurns(state, coin);
        verify(coin, never()).flip();
    }
}
