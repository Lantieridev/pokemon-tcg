package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.manager.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonToolEffectId;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link PokemonToolStep} — verifies that Muscle Band and Hard Charm
 * are applied at the correct position in the damage pipeline (XY1 rulebook §3).
 *
 * <ul>
 *   <li>Muscle Band adds +20 to base damage BEFORE Weakness x2.</li>
 *   <li>Hard Charm subtracts −20 AFTER Weakness/Resistance, with a minimum of 0.</li>
 * </ul>
 */
class PokemonToolStepTest {

    private static final int ATTACKER_HP = 200;
    private static final int DEFENDER_HP = 200;

    private FakeBattlePokemonState attacker;
    private FakeBattlePokemonState defender;
    private StatusEffectManager attackerSM;
    private StatusEffectManager defenderSM;
    private KnockoutHandler knockoutHandler;

    @BeforeEach
    void setUp() {
        // FIRE attacker with no weakness or resistance by default
        attacker = new FakeBattlePokemonState(ATTACKER_HP, PokemonType.FIRE, null, null, false);
        // WATER defender — no weakness (test controls weakness explicitly)
        defender = new FakeBattlePokemonState(DEFENDER_HP, PokemonType.WATER, null, null, false);
        attackerSM = new StatusEffectManager(() -> true);
        defenderSM = new StatusEffectManager(() -> true);
        knockoutHandler = mock(KnockoutHandler.class);

        attacker.addAttachedEnergy(PokemonType.FIRE);
    }

    // -----------------------------------------------------------------------
    // No tool — no modifier
    // -----------------------------------------------------------------------

    @Test
    void shouldNotModifyDamageWhenNoToolsAreAttached() {
        final Attack attack = new Attack("Ember", 30, List.of(PokemonType.FIRE));

        buildPipelineWithToolStep().execute(buildCtx(attack));

        // 30 / 10 = 3 counters
        assertEquals(3, defender.getDamageCounters());
    }

    // -----------------------------------------------------------------------
    // Muscle Band — attacker modifier (+20 before Weakness)
    // -----------------------------------------------------------------------

    @Test
    void shouldAddTwentyDamageWhenAttackerHasMuscleBand() {
        // Muscle Band: base 30 + 20 = 50 → 5 counters (no weakness)
        attacker.attachTool(muscleBand());
        final Attack attack = new Attack("Ember", 30, List.of(PokemonType.FIRE));

        buildPipelineWithToolStep().execute(buildCtx(attack));

        assertEquals(5, defender.getDamageCounters()); // (30 + 20) / 10
    }

    @Test
    void shouldApplyMuscleBandBeforeWeaknessMultiplier() {
        // Defender is weak to FIRE: (base + band) × 2
        // 30 + 20 = 50; 50 × 2 = 100 → 10 counters
        final FakeBattlePokemonState weakDefender =
                new FakeBattlePokemonState(DEFENDER_HP, PokemonType.WATER, PokemonType.FIRE, null, false);
        attacker.attachTool(muscleBand());
        final Attack attack = new Attack("Ember", 30, List.of(PokemonType.FIRE));

        buildPipelineWithToolStep().execute(buildCtxWith(attack, attacker, weakDefender));

        assertEquals(10, weakDefender.getDamageCounters()); // (30 + 20) * 2 / 10
    }

    @Test
    void shouldNotApplyMuscleBandWhenDefenderHasIt() {
        // Muscle Band on defender is ignored (it's an attacker-side tool)
        defender.attachTool(muscleBand());
        final Attack attack = new Attack("Ember", 30, List.of(PokemonType.FIRE));

        buildPipelineWithToolStep().execute(buildCtx(attack));

        assertEquals(3, defender.getDamageCounters()); // still 30 / 10
    }

    // -----------------------------------------------------------------------
    // Hard Charm — defender modifier (−20 after Weakness, minimum 0)
    // -----------------------------------------------------------------------

    @Test
    void shouldReduceTwentyDamageWhenDefenderHasHardCharm() {
        // base 30 → reduced to 20 → 2 counters
        defender.attachTool(hardCharm());
        final Attack attack = new Attack("Ember", 30, List.of(PokemonType.FIRE));

        buildPipelineWithToolStep().execute(buildCtx(attack));

        assertEquals(2, defender.getDamageCounters());
    }

    @Test
    void shouldApplyHardCharmAfterWeaknessMultiplier() {
        // Defender weak to FIRE: base 30 × 2 = 60; reduced to 20 → 2 counters
        final FakeBattlePokemonState weakDefender =
                new FakeBattlePokemonState(DEFENDER_HP, PokemonType.WATER, PokemonType.FIRE, null, false);
        weakDefender.attachTool(hardCharm());
        final Attack attack = new Attack("Ember", 30, List.of(PokemonType.FIRE));

        buildPipelineWithToolStep().execute(buildCtxWith(attack, attacker, weakDefender));

        assertEquals(2, weakDefender.getDamageCounters());
    }

    @Test
    void shouldLimitHardCharmDamageToExactlyTwentyForLowDamage() {
        // base 10 → set to 20 → 2 counters
        defender.attachTool(hardCharm());
        final Attack attack = new Attack("Tackle", 10, List.of(PokemonType.COLORLESS));
        attacker.addAttachedEnergy(PokemonType.COLORLESS);

        buildPipelineWithToolStep().execute(buildCtx(attack));

        assertEquals(2, defender.getDamageCounters());
    }

    @Test
    void shouldNotApplyHardCharmWhenAttackerHasIt() {
        // Hard Charm on attacker is ignored (it's a defender-side tool)
        attacker.attachTool(hardCharm());
        final Attack attack = new Attack("Ember", 30, List.of(PokemonType.FIRE));

        buildPipelineWithToolStep().execute(buildCtx(attack));

        assertEquals(3, defender.getDamageCounters()); // still 30 / 10
    }

    // -----------------------------------------------------------------------
    // Both tools active simultaneously
    // -----------------------------------------------------------------------

    @Test
    void shouldApplyBothMuscleBandAndHardCharmTogether() {
        // Attacker has Muscle Band, defender has Hard Charm (no weakness):
        // base 40 + 20 (band) = 60; set to 20 (charm) → 2 counters
        attacker.attachTool(muscleBand());
        defender.attachTool(hardCharm());
        final Attack attack = new Attack("Fire Blast", 40, List.of(PokemonType.FIRE));

        buildPipelineWithToolStep().execute(buildCtx(attack));

        assertEquals(2, defender.getDamageCounters());
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private AttackPipeline buildPipelineWithToolStep() {
        return new AttackPipeline(List.of(
                new ValidationStep(),
                new PreDamageEffectsStep(),
                new PokemonToolStep(new TrainerEffectResolver()),
                new DamageCalculationStep(new DamageCalculator()),
                new DamageApplicationStep(),
                new PostDamageEffectsStep(new AttackEffectResolver()),
                new KnockoutCheckStep()
        ));
    }

    private AttackContext buildCtx(final Attack attack) {
        return buildCtxWith(attack, attacker, defender);
    }

    private AttackContext buildCtxWith(final Attack attack,
                                        final FakeBattlePokemonState atk,
                                        final FakeBattlePokemonState def) {
        return new AttackContext.Builder(atk, def, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .effectText("")
                .build();
    }

    private static TrainerCard muscleBand() {
        return new TrainerCard.Builder("xy1-121", "Muscle Band", TrainerType.POKEMON_TOOL)
                .toolEffectId(PokemonToolEffectId.MUSCLE_BAND)
                .build();
    }

    private static TrainerCard hardCharm() {
        return new TrainerCard.Builder("xy1-119", "Hard Charm", TrainerType.POKEMON_TOOL)
                .toolEffectId(PokemonToolEffectId.HARD_CHARM)
                .build();
    }
}
