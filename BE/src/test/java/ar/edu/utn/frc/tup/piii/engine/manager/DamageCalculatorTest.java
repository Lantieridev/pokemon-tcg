package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.DamageContext;
import ar.edu.utn.frc.tup.piii.engine.model.DamageResult;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for DamageCalculator §3 formula. FR-007, FR-015.
 */
class DamageCalculatorTest {

    private static final int BASE_HP = 100;
    private static final int BASE_DAMAGE_40 = 40;
    private static final int BASE_DAMAGE_30 = 30;
    private static final int BASE_DAMAGE_60 = 60;
    private static final int BASE_DAMAGE_50 = 50;
    private static final int BASE_DAMAGE_10 = 10;
    private static final int BASE_DAMAGE_35 = 35;
    private static final int ADD_TEN = 10;
    private static final int SUBTRACT_TEN = -10;

    private DamageCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DamageCalculator();
    }

    // ─── Baseline ────────────────────────────────────────────────────────────

    @Test
    void shouldReturnBaseDamageAsCountersWhenNoModifiersNoWeaknessNoResistance() {
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(
                BASE_HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState defender = new FakeBattlePokemonState(
                BASE_HP, PokemonType.WATER, null, null, false);
        DamageContext ctx = new DamageContext(
                attacker, defender, new Attack("Ember", BASE_DAMAGE_40, List.of()), List.of(), List.of());

        DamageResult result = calculator.calculate(ctx);

        assertEquals(BASE_DAMAGE_40, result.finalDamage());
        assertEquals(4, result.damageCountersToPlace());
    }

    // ─── Attacker modifiers ──────────────────────────────────────────────────

    @Test
    void shouldApplyAttackerModifiersBeforeWeaknessCheckWhenModifiersPresent() {
        // base=30, mod=+10 → 40; weakness ×2 → 80
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(
                BASE_HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState defender = new FakeBattlePokemonState(
                BASE_HP, PokemonType.GRASS, PokemonType.FIRE, null, false);
        DamageContext ctx = new DamageContext(
                attacker, defender,
                new Attack("Ember", BASE_DAMAGE_30, List.of()),
                List.of(dmg -> dmg + ADD_TEN),
                List.of());

        DamageResult result = calculator.calculate(ctx);

        assertEquals(80, result.finalDamage());
        assertEquals(8, result.damageCountersToPlace());
    }

    // ─── Weakness ────────────────────────────────────────────────────────────

    @Test
    void shouldApplyWeaknessMultiplierWhenAttackerTypeMatchesDefenderWeaknessType() {
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(
                BASE_HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState defender = new FakeBattlePokemonState(
                BASE_HP, PokemonType.GRASS, PokemonType.FIRE, null, false);
        DamageContext ctx = new DamageContext(
                attacker, defender, new Attack("Flamethrower", BASE_DAMAGE_60, List.of()), List.of(), List.of());

        DamageResult result = calculator.calculate(ctx);

        assertEquals(120, result.finalDamage());
        assertEquals(12, result.damageCountersToPlace());
    }

    @Test
    void shouldNotApplyWeaknessWhenAttackerTypeDoesNotMatchDefenderWeaknessType() {
        // FIRE attack vs WATER weakness — no multiplier
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(
                BASE_HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState defender = new FakeBattlePokemonState(
                BASE_HP, PokemonType.WATER, PokemonType.LIGHTNING, null, false);
        DamageContext ctx = new DamageContext(
                attacker, defender, new Attack("Flamethrower", BASE_DAMAGE_60, List.of()), List.of(), List.of());

        DamageResult result = calculator.calculate(ctx);

        assertEquals(BASE_DAMAGE_60, result.finalDamage());
    }

    @Test
    void shouldNotApplyWeaknessWhenDefenderHasNoWeaknessType() {
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(
                BASE_HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState defender = new FakeBattlePokemonState(
                BASE_HP, PokemonType.DRAGON, null, null, false);
        DamageContext ctx = new DamageContext(
                attacker, defender, new Attack("Ember", BASE_DAMAGE_60, List.of()), List.of(), List.of());

        DamageResult result = calculator.calculate(ctx);

        assertEquals(BASE_DAMAGE_60, result.finalDamage());
    }

    // ─── Resistance ──────────────────────────────────────────────────────────

    @Test
    void shouldApplyResistanceReductionWhenAttackerTypeMatchesDefenderResistanceType() {
        // base=50, resistance -20 → 30
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(
                BASE_HP, PokemonType.WATER, null, null, false);
        FakeBattlePokemonState defender = new FakeBattlePokemonState(
                BASE_HP, PokemonType.FIRE, null, PokemonType.WATER, false);
        DamageContext ctx = new DamageContext(
                attacker, defender, new Attack("Bubble", BASE_DAMAGE_50, List.of()), List.of(), List.of());

        DamageResult result = calculator.calculate(ctx);

        assertEquals(30, result.finalDamage());
        assertEquals(3, result.damageCountersToPlace());
    }

    @Test
    void shouldNotApplyResistanceWhenAttackerTypeDoesNotMatchDefenderResistanceType() {
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(
                BASE_HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState defender = new FakeBattlePokemonState(
                BASE_HP, PokemonType.GRASS, null, PokemonType.WATER, false);
        DamageContext ctx = new DamageContext(
                attacker, defender, new Attack("Ember", BASE_DAMAGE_50, List.of()), List.of(), List.of());

        DamageResult result = calculator.calculate(ctx);

        assertEquals(BASE_DAMAGE_50, result.finalDamage());
    }

    @Test
    void shouldNotApplyResistanceWhenDefenderHasNoResistanceType() {
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(
                BASE_HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState defender = new FakeBattlePokemonState(
                BASE_HP, PokemonType.GRASS, null, null, false);
        DamageContext ctx = new DamageContext(
                attacker, defender, new Attack("Ember", BASE_DAMAGE_50, List.of()), List.of(), List.of());

        DamageResult result = calculator.calculate(ctx);

        assertEquals(BASE_DAMAGE_50, result.finalDamage());
    }

    // ─── Floor ───────────────────────────────────────────────────────────────

    @Test
    void shouldFloorFinalDamageToZeroWhenResistanceDrivesItNegative() {
        // base=10, resistance matches (-20) → -10 → floored to 0
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(
                BASE_HP, PokemonType.WATER, null, null, false);
        FakeBattlePokemonState defender = new FakeBattlePokemonState(
                BASE_HP, PokemonType.FIRE, null, PokemonType.WATER, false);
        DamageContext ctx = new DamageContext(
                attacker, defender, new Attack("Bubble", BASE_DAMAGE_10, List.of()), List.of(), List.of());

        DamageResult result = calculator.calculate(ctx);

        assertEquals(0, result.finalDamage());
        assertEquals(0, result.damageCountersToPlace());
    }

    // ─── Defender modifiers ───────────────────────────────────────────────────

    @Test
    void shouldApplyDefenderModifiersAfterResistanceWhenModifiersPresent() {
        // base=50, no weakness, resistance -20 → 30, defMod -10 → 20
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(
                BASE_HP, PokemonType.WATER, null, null, false);
        FakeBattlePokemonState defender = new FakeBattlePokemonState(
                BASE_HP, PokemonType.FIRE, null, PokemonType.WATER, false);
        DamageContext ctx = new DamageContext(
                attacker, defender,
                new Attack("Bubble", BASE_DAMAGE_50, List.of()),
                List.of(),
                List.of(dmg -> dmg + SUBTRACT_TEN));

        DamageResult result = calculator.calculate(ctx);

        assertEquals(20, result.finalDamage());
        assertEquals(2, result.damageCountersToPlace());
    }

    // ─── Full pipeline ────────────────────────────────────────────────────────

    @Test
    void shouldApplyFullPipelineWithWeaknessAndResistanceAndModifiersWhenAllStepsAreActive() {
        // base=60, attMod=+10 → 70, ×2=140, -20=120, defMod=-10 → 110; 110/10=11 counters
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(
                BASE_HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState defender = new FakeBattlePokemonState(
                BASE_HP, PokemonType.GRASS, PokemonType.FIRE, PokemonType.FIRE, false);
        DamageContext ctx = new DamageContext(
                attacker, defender,
                new Attack("Flamethrower", BASE_DAMAGE_60, List.of()),
                List.of(dmg -> dmg + ADD_TEN),
                List.of(dmg -> dmg + SUBTRACT_TEN));

        DamageResult result = calculator.calculate(ctx);

        assertEquals(110, result.finalDamage());
        assertEquals(11, result.damageCountersToPlace());
    }

    // ─── Integer division ─────────────────────────────────────────────────────

    @Test
    void shouldConvertFinalDamageToCountersUsingIntegerDivisionWhenDamageIs35() {
        // base=35 with no weakness/resistance/modifiers → 35/10 = 3 counters
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(
                BASE_HP, PokemonType.FIGHTING, null, null, false);
        FakeBattlePokemonState defender = new FakeBattlePokemonState(
                BASE_HP, PokemonType.COLORLESS, null, null, false);
        DamageContext ctx = new DamageContext(
                attacker, defender, new Attack("Punch", BASE_DAMAGE_35, List.of()), List.of(), List.of());

        DamageResult result = calculator.calculate(ctx);

        assertEquals(BASE_DAMAGE_35, result.finalDamage());
        assertEquals(3, result.damageCountersToPlace());
    }

    // ─── Null guard ───────────────────────────────────────────────────────────

    @Test
    void shouldThrowNullPointerExceptionWhenContextIsNull() {
        assertThrows(NullPointerException.class, () -> calculator.calculate(null));
    }

    // ─── Shadow Circle: weakness suppression ─────────────────────────────────

    @Test
    void shouldNotApplyWeaknessWhenWeaknessIsSuppressed() {
        // FIRE attacker vs WATER defender that has FIRE weakness — normally ×2
        // With weakness suppressed, damage should stay at base (no ×2)
        final FakeBattlePokemonState attacker = new FakeBattlePokemonState(
                BASE_HP, PokemonType.FIRE, null, null, false);
        final FakeBattlePokemonState defender = new FakeBattlePokemonState(
                BASE_HP, PokemonType.WATER, PokemonType.FIRE, null, false);
        final DamageContext ctx = new DamageContext(
                attacker, defender, new Attack("Flamethrower", BASE_DAMAGE_60, List.of()), List.of(), List.of());

        final DamageResult withWeakness    = calculator.calculate(ctx, false);
        final DamageResult withSuppression = calculator.calculate(ctx, true);

        assertEquals(120, withWeakness.finalDamage(),    "Normal: 60 × 2 = 120");
        assertEquals(60,  withSuppression.finalDamage(), "Suppressed: no ×2 → 60");
    }

    @Test
    void shouldApplyWeaknessNormallyWhenSuppressionIsFalse() {
        final FakeBattlePokemonState attacker = new FakeBattlePokemonState(
                BASE_HP, PokemonType.WATER, null, null, false);
        final FakeBattlePokemonState defender = new FakeBattlePokemonState(
                BASE_HP, PokemonType.FIRE, PokemonType.WATER, null, false);
        final DamageContext ctx = new DamageContext(
                attacker, defender, new Attack("Water Gun", BASE_DAMAGE_30, List.of()), List.of(), List.of());

        final DamageResult result = calculator.calculate(ctx, false);

        assertEquals(60, result.finalDamage(), "suppressWeakness=false: 30 × 2 = 60");
    }
}
