package ar.edu.utn.frc.tup.piii.engine.status;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.manager.RetreatExecutor;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.RetreatAction;
import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests that retreating a Pokémon clears all status effects from the StatusEffectManager.
 * FR-011: When a Pokémon retreats, all special conditions are cleared.
 */
class StatusEffectClearOnRetreatTest {

    private static final int HP = 100;

    private StatusEffectManager statusEffectManager;
    private RetreatExecutor retreatExecutor;

    @BeforeEach
    void setUp() {
        CoinFlipper coinFlipper = () -> true;
        statusEffectManager = new StatusEffectManager(coinFlipper);
        retreatExecutor = new RetreatExecutor(statusEffectManager);
    }

    @Test
    void shouldClearAllStatusEffectsAfterRetreatActionIsApplied() {
        FakeBattlePokemonState active = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        active.setRetreatCost(0);

        // Apply multiple status effects to the active Pokémon
        statusEffectManager.apply(StatusEffectType.ENVENENADO);
        statusEffectManager.apply(StatusEffectType.QUEMADO);

        RetreatAction action = new RetreatAction(active);

        // Execute the retreat — this must clear all status effects
        retreatExecutor.executeRetreat(action);

        assertFalse(statusEffectManager.has(StatusEffectType.ENVENENADO),
                "ENVENENADO should be cleared after retreat");
        assertFalse(statusEffectManager.has(StatusEffectType.QUEMADO),
                "QUEMADO should be cleared after retreat");
    }
}
