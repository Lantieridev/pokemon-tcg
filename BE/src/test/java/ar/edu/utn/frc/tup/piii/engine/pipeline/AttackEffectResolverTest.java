package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AttackEffectResolverTest {

    private static final Attack BASIC_ATTACK = new Attack("Tackle", 10, List.of(PokemonType.COLORLESS));

    private AttackEffectResolver resolver;
    private FakeBattlePokemonState attacker;
    private FakeBattlePokemonState defender;
    private StatusEffectManager attackerSM;
    private StatusEffectManager defenderSM;

    @BeforeEach
    void setUp() {
        resolver = new AttackEffectResolver();
        attacker = new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false);
        defender = new FakeBattlePokemonState(100, PokemonType.WATER, null, null, false);
        attackerSM = new StatusEffectManager(() -> true);
        defenderSM = new StatusEffectManager(() -> true);
    }

    // --- resolveType ---

    @Test
    void shouldReturnNoneForBlankEffectText() {
        assertEquals(AttackEffectType.NONE, resolver.resolveType(""));
    }

    @Test
    void shouldReturnNoneForNullEffectText() {
        assertEquals(AttackEffectType.NONE, resolver.resolveType(null));
    }

    @Test
    void shouldReturnNoneForUnknownEffectText() {
        assertEquals(AttackEffectType.NONE, resolver.resolveType("unknown_effect"));
    }

    @Test
    void shouldResolvePoisonType() {
        assertEquals(AttackEffectType.APPLY_POISON, resolver.resolveType("poison"));
    }

    @Test
    void shouldResolveBurnType() {
        assertEquals(AttackEffectType.APPLY_BURN, resolver.resolveType("burn"));
    }

    @Test
    void shouldResolveParalysisType() {
        assertEquals(AttackEffectType.APPLY_PARALYSIS, resolver.resolveType("paralysis"));
    }

    @Test
    void shouldResolveSleepType() {
        assertEquals(AttackEffectType.APPLY_SLEEP, resolver.resolveType("sleep"));
    }

    @Test
    void shouldResolveConfusionType() {
        assertEquals(AttackEffectType.APPLY_CONFUSION, resolver.resolveType("confusion"));
    }

    @Test
    void shouldResolveHealSelfType() {
        assertEquals(AttackEffectType.HEAL_SELF, resolver.resolveType("heal:30"));
    }

    @Test
    void shouldResolveSelfDamageType() {
        assertEquals(AttackEffectType.SELF_DAMAGE, resolver.resolveType("self_damage:10"));
    }

    @Test
    void shouldResolveDiscardEnergyType() {
        assertEquals(AttackEffectType.DISCARD_ENERGY, resolver.resolveType("discard_energy:1"));
    }

    @Test
    void shouldResolveCoinFlipExtraType() {
        assertEquals(AttackEffectType.COIN_FLIP_EXTRA_DAMAGE, resolver.resolveType("coin_flip_extra:20"));
    }

    // --- extractAmount ---

    @Test
    void shouldExtractAmountFromEffectText() {
        assertEquals(30, resolver.extractAmount("heal:30"));
        assertEquals(20, resolver.extractAmount("coin_flip_extra:20"));
        assertEquals(1, resolver.extractAmount("discard_energy:1"));
    }

    @Test
    void shouldExtractZeroWhenNoColonInText() {
        assertEquals(0, resolver.extractAmount("poison"));
    }

    @Test
    void shouldExtractZeroForNullText() {
        assertEquals(0, resolver.extractAmount(null));
    }

    // --- apply: status effects ---

    @Test
    void shouldApplyPoisonToDefender() {
        final AttackContext ctx = buildCtx("poison");
        resolver.apply(ctx);
        assertTrue(defenderSM.has(StatusEffectType.ENVENENADO));
    }

    @Test
    void shouldApplyBurnToDefender() {
        final AttackContext ctx = buildCtx("burn");
        resolver.apply(ctx);
        assertTrue(defenderSM.has(StatusEffectType.QUEMADO));
    }

    @Test
    void shouldApplyParalysisToDefender() {
        final AttackContext ctx = buildCtx("paralysis");
        resolver.apply(ctx);
        assertTrue(defenderSM.has(StatusEffectType.PARALIZADO));
    }

    @Test
    void shouldApplySleepToDefender() {
        final AttackContext ctx = buildCtx("sleep");
        resolver.apply(ctx);
        assertTrue(defenderSM.has(StatusEffectType.DORMIDO));
    }

    @Test
    void shouldApplyConfusionToDefender() {
        final AttackContext ctx = buildCtx("confusion");
        resolver.apply(ctx);
        assertTrue(defenderSM.has(StatusEffectType.CONFUNDIDO));
    }

    // --- apply: attacker effects ---

    @Test
    void shouldHealAttacker() {
        attacker.addDamageCounters(5); // 50 HP of damage
        final AttackContext ctx = buildCtx("heal:30");
        resolver.apply(ctx);
        assertEquals(2, attacker.getDamageCounters()); // 5 - 3 = 2 counters remain
    }

    @Test
    void shouldNotHealBelowZeroCounters() {
        attacker.addDamageCounters(1);
        final AttackContext ctx = buildCtx("heal:50");
        resolver.apply(ctx);
        assertEquals(0, attacker.getDamageCounters());
    }

    @Test
    void shouldApplySelfDamageAsCounters() {
        final AttackContext ctx = buildCtx("self_damage:10");
        resolver.apply(ctx);
        assertEquals(1, attacker.getDamageCounters()); // 10 HP = 1 counter
    }

    @Test
    void shouldDiscardEnergyFromAttacker() {
        attacker.addAttachedEnergy(PokemonType.FIRE);
        attacker.addAttachedEnergy(PokemonType.FIRE);
        final AttackContext ctx = buildCtx("discard_energy:1");
        resolver.apply(ctx);
        assertEquals(1, attacker.getAttachedEnergies().size());
    }

    @Test
    void shouldBeNoOpForCoinFlipExtraType() {
        final int defenderCountersBefore = defender.getDamageCounters();
        final AttackContext ctx = buildCtx("coin_flip_extra:20");
        resolver.apply(ctx);
        assertEquals(defenderCountersBefore, defender.getDamageCounters());
        assertFalse(defenderSM.has(StatusEffectType.ENVENENADO));
    }

    @Test
    void shouldBeNoOpForNoneType() {
        final int defenderCountersBefore = defender.getDamageCounters();
        final AttackContext ctx = buildCtx("");
        resolver.apply(ctx);
        assertEquals(defenderCountersBefore, defender.getDamageCounters());
    }

    // --- helpers ---

    private AttackContext buildCtx(final String effectText) {
        return new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class), () -> true)
                .effectText(effectText)
                .build();
    }
}
