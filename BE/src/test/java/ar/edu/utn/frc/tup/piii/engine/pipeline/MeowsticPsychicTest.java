package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.manager.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class MeowsticPsychicTest {

    private FakeBattlePokemonState attacker;
    private FakeBattlePokemonState defender;
    private StatusEffectManager attackerSM;
    private StatusEffectManager defenderSM;
    private KnockoutHandler knockoutHandler;
    private AttackPipeline pipeline;

    @BeforeEach
    void setUp() {
        attacker = new FakeBattlePokemonState(100, PokemonType.PSYCHIC, null, null, false);
        defender = new FakeBattlePokemonState(100, PokemonType.COLORLESS, null, null, false);
        attackerSM = new StatusEffectManager(() -> true);
        defenderSM = new StatusEffectManager(() -> true);
        knockoutHandler = mock(KnockoutHandler.class);
        pipeline = new AttackPipeline(List.of(
                new ValidationStep(),
                new PreDamageEffectsStep(),
                new DamageCalculationStep(new DamageCalculator()),
                new DamageApplicationStep()
        ));
    }

    @Test
    void shouldDealBaseDamageWhenDefenderHasNoEnergy() {
        // Attack: 10 base damage + 10 more for each energy attached to opponent
        final Attack attack = new Attack("Psychic", 10, List.of(PokemonType.PSYCHIC));
        attacker.addAttachedEnergy(PokemonType.PSYCHIC);

        final AttackContext ctx = new AttackContext.Builder(attacker, defender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .effectText("damage_per_opponent_all_energy:10")
                .build();

        pipeline.execute(ctx);

        // 10 base damage -> 1 counter
        assertEquals(1, defender.getDamageCounters());
    }

    @Test
    void shouldDealExtraDamagePerEnergyAttachedToDefender() {
        final Attack attack = new Attack("Psychic", 10, List.of(PokemonType.PSYCHIC));
        attacker.addAttachedEnergy(PokemonType.PSYCHIC);

        // Defender has 2 energies attached
        defender.addAttachedEnergy(PokemonType.FIRE);
        defender.addAttachedEnergy(PokemonType.WATER);

        final AttackContext ctx = new AttackContext.Builder(attacker, defender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .effectText("damage_per_opponent_all_energy:10")
                .build();

        pipeline.execute(ctx);

        // 10 base + 2 * 10 = 30 damage -> 3 counters
        assertEquals(3, defender.getDamageCounters());
    }
}
