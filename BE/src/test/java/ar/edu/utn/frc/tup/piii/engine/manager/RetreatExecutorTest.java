package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.RetreatAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that RetreatExecutor discards energies equal to the retreat cost.
 * FR-011.
 */
class RetreatExecutorTest {

    private static final int HP = 100;

    private StatusEffectManager statusEffectManager;
    private RetreatExecutor retreatExecutor;

    @BeforeEach
    void setUp() {
        final CoinFlipper coinFlipper = () -> true;
        statusEffectManager = new StatusEffectManager(coinFlipper);
        retreatExecutor = new RetreatExecutor(statusEffectManager);
    }

    @Test
    void shouldDiscardEnergiesEqualToRetreatCostWhenRetreating() {
        final FakeBattlePokemonState active =
                new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        active.setRetreatCost(2);
        active.addAttachedEnergy(PokemonType.FIRE);
        active.addAttachedEnergy(PokemonType.WATER);
        active.addAttachedEnergy(PokemonType.GRASS);

        retreatExecutor.executeRetreat(new RetreatAction(active));

        // 3 energies - 2 retreat cost = 1 remaining
        assertEquals(1, active.getAttachedEnergies().size(),
                "After retreating with cost 2, active should have 1 energy remaining (3 - 2)");
    }

    @Test
    void shouldDiscardNoEnergiesWhenRetreatCostIsZero() {
        final FakeBattlePokemonState active =
                new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        active.setRetreatCost(0);
        active.addAttachedEnergy(PokemonType.FIRE);
        active.addAttachedEnergy(PokemonType.WATER);

        retreatExecutor.executeRetreat(new RetreatAction(active));

        // retreat cost 0 → no energies removed
        assertEquals(2, active.getAttachedEnergies().size(),
                "With retreat cost 0, energies should remain unchanged");
    }
}
