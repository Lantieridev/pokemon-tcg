package ar.edu.utn.frc.tup.piii.engine.status;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.manager.EvolveExecutor;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests that EvolveExecutor clears all status effects after evolution.
 * Per XY1 rules, evolving removes all special conditions. FR-010.
 */
class EvolveExecutorTest {

    private static final int HP = 100;

    private StatusEffectManager statusEffectManager;
    private EvolveExecutor evolveExecutor;

    @BeforeEach
    void setUp() {
        final CoinFlipper coinFlipper = () -> true;
        statusEffectManager = new StatusEffectManager(coinFlipper);
        evolveExecutor = new EvolveExecutor(statusEffectManager);
    }

    @Test
    void shouldClearAllStatusEffectsAfterEvolveActionIsApplied() {
        statusEffectManager.apply(StatusEffectType.ENVENENADO);

        final FakeBattlePokemonState pokemon =
                new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);

        evolveExecutor.executeEvolve(pokemon);

        assertFalse(statusEffectManager.has(StatusEffectType.ENVENENADO),
                "ENVENENADO status must be cleared after evolution");
    }
}
