package ar.edu.utn.frc.tup.piii.engine.statuseffect;

import ar.edu.utn.frc.tup.piii.engine.model.ActivePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for ParalyzedEffect (PARALIZADO). FR-009.
 */
class ParalyzedEffectTest {

    private ParalyzedEffect effect;
    private ActivePokemonState state;
    private CoinFlipper coin;

    @BeforeEach
    void setUp() {
        effect = new ParalyzedEffect();
        state = Mockito.mock(ActivePokemonState.class);
        coin = Mockito.mock(CoinFlipper.class);
    }

    @Test
    void shouldAlwaysRemoveParalysisAfterBetweenTurnsProcessing() {
        assertTrue(effect.processBetweenTurns(state, coin));
    }

    @Test
    void shouldNotFlipCoinWhenProcessingParalysisBetweenTurns() {
        effect.processBetweenTurns(state, coin);
        verify(coin, never()).flip();
    }

    @Test
    void shouldBlockAttackAndRetreatWhenParalyzed() {
        assertTrue(effect.blocksAttack());
        assertTrue(effect.blocksRetreat());
    }
}
