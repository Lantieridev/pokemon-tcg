package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import ar.edu.utn.frc.tup.piii.engine.model.Bench;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
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

    // --- apply: bench_damage (EX attacks) ---

    @Test
    void shouldResolveBenchDamageType() {
        assertEquals(AttackEffectType.BENCH_DAMAGE, resolver.resolveType("bench_damage:20"));
    }

    @Test
    void shouldApplyDamageToEachBenchedPokemon() {
        final FakeBattlePokemonState benched1 =
                new FakeBattlePokemonState(60, PokemonType.WATER, null, null, false);
        final FakeBattlePokemonState benched2 =
                new FakeBattlePokemonState(60, PokemonType.WATER, null, null, false);

        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class), () -> true)
                .effectText("bench_damage:20")
                .defenderBench(List.of(benched1, benched2))
                .build();

        resolver.apply(ctx);

        // 20 damage = 2 counters each
        assertEquals(2, benched1.getDamageCounters(), "benched1 should receive 2 counters (20 damage)");
        assertEquals(2, benched2.getDamageCounters(), "benched2 should receive 2 counters (20 damage)");
    }

    @Test
    void shouldBeNoOpForBenchDamageWhenNoBenchedPokemon() {
        // Empty bench — should not throw
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class), () -> true)
                .effectText("bench_damage:20")
                .defenderBench(List.of())
                .build();

        resolver.apply(ctx); // must not throw

        assertEquals(0, defender.getDamageCounters(), "active defender should not be affected by bench_damage");
    }

    @Test
    void shouldResolveMoveEnergyTypeAndBeNoOp() {
        assertEquals(AttackEffectType.MOVE_ENERGY, resolver.resolveType("move_energy"));
        final AttackContext ctx = buildCtx("move_energy");
        resolver.apply(ctx); // FR-TODO: no-op, must not throw
    }

    @Test
    void shouldResolveForceSwitchTypeAndExecuteSwitch() {
        assertEquals(AttackEffectType.FORCE_SWITCH, resolver.resolveType("force_switch"));
        
        final PlayerRuntime attackerRuntime = mock(PlayerRuntime.class);
        final PlayerRuntime defenderRuntime = mock(PlayerRuntime.class);
        
        final Bench attackerBench = mock(Bench.class);
        final Bench defenderBench = mock(Bench.class);
        
        final BattlePokemonState activeAttacker = mock(BattlePokemonState.class);
        final BattlePokemonState activeDefender = mock(BattlePokemonState.class);
        
        final BattlePokemonState benchAttacker = mock(BattlePokemonState.class);
        final BattlePokemonState benchDefender = mock(BattlePokemonState.class);
        
        final StatusEffectManager attackerSM = mock(StatusEffectManager.class);
        final StatusEffectManager defenderSM = mock(StatusEffectManager.class);
        
        org.mockito.Mockito.when(attackerRuntime.getBench()).thenReturn(attackerBench);
        org.mockito.Mockito.when(attackerBench.getAll()).thenReturn(List.of(benchAttacker));
        org.mockito.Mockito.when(attackerRuntime.getActivePokemon()).thenReturn(activeAttacker);
        org.mockito.Mockito.when(attackerBench.promote(0)).thenReturn(benchAttacker);
        org.mockito.Mockito.when(attackerRuntime.getStatusEffectManager()).thenReturn(attackerSM);
        
        org.mockito.Mockito.when(defenderRuntime.getBench()).thenReturn(defenderBench);
        org.mockito.Mockito.when(defenderBench.getAll()).thenReturn(List.of(benchDefender));
        org.mockito.Mockito.when(defenderRuntime.getActivePokemon()).thenReturn(activeDefender);
        org.mockito.Mockito.when(defenderBench.promote(0)).thenReturn(benchDefender);
        org.mockito.Mockito.when(defenderRuntime.getStatusEffectManager()).thenReturn(defenderSM);
        
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                mock(StatusEffectManager.class), mock(StatusEffectManager.class),
                mock(KnockoutHandler.class), () -> true)
                .effectText("force_switch")
                .attackerRuntime(attackerRuntime)
                .defenderRuntime(defenderRuntime)
                .build();
                
        resolver.apply(ctx);
        
        org.mockito.Mockito.verify(attackerRuntime).setActivePokemon(benchAttacker);
        org.mockito.Mockito.verify(attackerBench).place(activeAttacker);
        org.mockito.Mockito.verify(attackerSM).clearAll();
        org.mockito.Mockito.verify(attackerRuntime).recordPokemonEntered(activeAttacker);
        
        org.mockito.Mockito.verify(defenderRuntime).setActivePokemon(benchDefender);
        org.mockito.Mockito.verify(defenderBench).place(activeDefender);
        org.mockito.Mockito.verify(defenderSM).clearAll();
        org.mockito.Mockito.verify(defenderRuntime).recordPokemonEntered(activeDefender);
    }

    // --- apply: coin-flip status effects ---

    @Test
    void shouldResolveCoinFlipPoisonType() {
        assertEquals(AttackEffectType.COIN_FLIP_POISON, resolver.resolveType("coin_flip_poison"));
    }

    @Test
    void shouldResolveCoinFlipBurnType() {
        assertEquals(AttackEffectType.COIN_FLIP_BURN, resolver.resolveType("coin_flip_burn"));
    }

    @Test
    void shouldResolveCoinFlipParalysisType() {
        assertEquals(AttackEffectType.COIN_FLIP_PARALYSIS, resolver.resolveType("coin_flip_paralysis"));
    }

    @Test
    void shouldResolveCoinFlipSleepType() {
        assertEquals(AttackEffectType.COIN_FLIP_SLEEP, resolver.resolveType("coin_flip_sleep"));
    }

    @Test
    void shouldResolveCoinFlipConfusionType() {
        assertEquals(AttackEffectType.COIN_FLIP_CONFUSION, resolver.resolveType("coin_flip_confusion"));
    }

    @Test
    void shouldApplyPoisonWhenCoinFlipIsHeads() {
        final AttackContext ctx = buildCtxWithCoinFlipper("coin_flip_poison", () -> true);
        resolver.apply(ctx);
        assertTrue(defenderSM.has(StatusEffectType.ENVENENADO));
    }

    @Test
    void shouldNotApplyPoisonWhenCoinFlipIsTails() {
        final AttackContext ctx = buildCtxWithCoinFlipper("coin_flip_poison", () -> false);
        resolver.apply(ctx);
        assertFalse(defenderSM.has(StatusEffectType.ENVENENADO));
    }

    @Test
    void shouldApplyBurnWhenCoinFlipIsHeads() {
        final AttackContext ctx = buildCtxWithCoinFlipper("coin_flip_burn", () -> true);
        resolver.apply(ctx);
        assertTrue(defenderSM.has(StatusEffectType.QUEMADO));
    }

    @Test
    void shouldNotApplyBurnWhenCoinFlipIsTails() {
        final AttackContext ctx = buildCtxWithCoinFlipper("coin_flip_burn", () -> false);
        resolver.apply(ctx);
        assertFalse(defenderSM.has(StatusEffectType.QUEMADO));
    }

    @Test
    void shouldApplyParalysisWhenCoinFlipIsHeads() {
        final AttackContext ctx = buildCtxWithCoinFlipper("coin_flip_paralysis", () -> true);
        resolver.apply(ctx);
        assertTrue(defenderSM.has(StatusEffectType.PARALIZADO));
    }

    @Test
    void shouldNotApplyParalysisWhenCoinFlipIsTails() {
        final AttackContext ctx = buildCtxWithCoinFlipper("coin_flip_paralysis", () -> false);
        resolver.apply(ctx);
        assertFalse(defenderSM.has(StatusEffectType.PARALIZADO));
    }

    @Test
    void shouldApplySleepWhenCoinFlipIsHeads() {
        final AttackContext ctx = buildCtxWithCoinFlipper("coin_flip_sleep", () -> true);
        resolver.apply(ctx);
        assertTrue(defenderSM.has(StatusEffectType.DORMIDO));
    }

    @Test
    void shouldNotApplySleepWhenCoinFlipIsTails() {
        final AttackContext ctx = buildCtxWithCoinFlipper("coin_flip_sleep", () -> false);
        resolver.apply(ctx);
        assertFalse(defenderSM.has(StatusEffectType.DORMIDO));
    }

    @Test
    void shouldApplyConfusionWhenCoinFlipIsHeads() {
        final AttackContext ctx = buildCtxWithCoinFlipper("coin_flip_confusion", () -> true);
        resolver.apply(ctx);
        assertTrue(defenderSM.has(StatusEffectType.CONFUNDIDO));
    }

    @Test
    void shouldNotApplyConfusionWhenCoinFlipIsTails() {
        final AttackContext ctx = buildCtxWithCoinFlipper("coin_flip_confusion", () -> false);
        resolver.apply(ctx);
        assertFalse(defenderSM.has(StatusEffectType.CONFUNDIDO));
    }

    // --- helpers ---

    private AttackContext buildCtx(final String effectText) {
        return new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class), () -> true)
                .effectText(effectText)
                .build();
    }

    private AttackContext buildCtxWithCoinFlipper(final String effectText,
                                                  final ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper coinFlipper) {
        return new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class), coinFlipper)
                .effectText(effectText)
                .build();
    }

    @Test
    void shouldResolveCoinFlipSwitchSelfOnHeads() {
        assertEquals(AttackEffectType.COIN_FLIP_SWITCH_SELF, resolver.resolveType("coin_flip_switch_self"));
        
        final PlayerRuntime attackerRuntime = mock(PlayerRuntime.class);
        final Bench attackerBench = mock(Bench.class);
        final BattlePokemonState activeAttacker = mock(BattlePokemonState.class);
        final BattlePokemonState benchAttacker = mock(BattlePokemonState.class);
        final StatusEffectManager attackerSM = mock(StatusEffectManager.class);
        
        org.mockito.Mockito.when(attackerRuntime.getBench()).thenReturn(attackerBench);
        org.mockito.Mockito.when(attackerBench.getAll()).thenReturn(List.of(benchAttacker));
        org.mockito.Mockito.when(attackerRuntime.getActivePokemon()).thenReturn(activeAttacker);
        org.mockito.Mockito.when(attackerBench.promote(0)).thenReturn(benchAttacker);
        org.mockito.Mockito.when(attackerRuntime.getStatusEffectManager()).thenReturn(attackerSM);
        
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                mock(StatusEffectManager.class), mock(StatusEffectManager.class),
                mock(KnockoutHandler.class), () -> true) // Heads
                .effectText("coin_flip_switch_self")
                .attackerRuntime(attackerRuntime)
                .build();
                
        resolver.apply(ctx);
        
        org.mockito.Mockito.verify(attackerRuntime).setActivePokemon(benchAttacker);
        org.mockito.Mockito.verify(attackerBench).place(activeAttacker);
    }

    @Test
    void shouldNotSwitchSelfOnTails() {
        final PlayerRuntime attackerRuntime = mock(PlayerRuntime.class);
        
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                mock(StatusEffectManager.class), mock(StatusEffectManager.class),
                mock(KnockoutHandler.class), () -> false) // Tails
                .effectText("coin_flip_switch_self")
                .attackerRuntime(attackerRuntime)
                .build();
                
        resolver.apply(ctx);
        
        org.mockito.Mockito.verifyNoInteractions(attackerRuntime);
    }

    @Test
    void shouldResolveHealAnyCorrectly() {
        assertEquals(AttackEffectType.HEAL_ANY, resolver.resolveType("heal_any:20"));
        
        final PlayerRuntime attackerRuntime = mock(PlayerRuntime.class);
        final Bench attackerBench = mock(Bench.class);
        final BattlePokemonState activeAttacker = mock(BattlePokemonState.class);
        final BattlePokemonState benched1 = mock(BattlePokemonState.class);
        final BattlePokemonState benched2 = mock(BattlePokemonState.class);
        
        org.mockito.Mockito.when(attackerRuntime.getActivePokemon()).thenReturn(activeAttacker);
        org.mockito.Mockito.when(attackerRuntime.getBench()).thenReturn(attackerBench);
        org.mockito.Mockito.when(attackerBench.getAll()).thenReturn(List.of(benched1, benched2));
        
        org.mockito.Mockito.when(activeAttacker.getDamageCounters()).thenReturn(2);
        org.mockito.Mockito.when(benched1.getDamageCounters()).thenReturn(4); // Most damaged
        org.mockito.Mockito.when(benched2.getDamageCounters()).thenReturn(1);
        
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                mock(StatusEffectManager.class), mock(StatusEffectManager.class),
                mock(KnockoutHandler.class), () -> true)
                .effectText("heal_any:20")
                .attackerRuntime(attackerRuntime)
                .build();
                
        resolver.apply(ctx);
        
        org.mockito.Mockito.verify(benched1).heal(20);
        org.mockito.Mockito.verify(activeAttacker, org.mockito.Mockito.never()).heal(20);
        org.mockito.Mockito.verify(benched2, org.mockito.Mockito.never()).heal(20);
    }

    @Test
    void shouldResolveHealBenchCorrectly() {
        assertEquals(AttackEffectType.HEAL_BENCH, resolver.resolveType("heal_bench:60"));
        
        final PlayerRuntime attackerRuntime = mock(PlayerRuntime.class);
        final Bench attackerBench = mock(Bench.class);
        final BattlePokemonState activeAttacker = mock(BattlePokemonState.class);
        final BattlePokemonState benched1 = mock(BattlePokemonState.class);
        final BattlePokemonState benched2 = mock(BattlePokemonState.class);
        
        org.mockito.Mockito.when(attackerRuntime.getActivePokemon()).thenReturn(activeAttacker);
        org.mockito.Mockito.when(attackerRuntime.getBench()).thenReturn(attackerBench);
        org.mockito.Mockito.when(attackerBench.getAll()).thenReturn(List.of(benched1, benched2));
        
        org.mockito.Mockito.when(benched1.getDamageCounters()).thenReturn(1);
        org.mockito.Mockito.when(benched2.getDamageCounters()).thenReturn(5); // Most damaged on bench
        
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                mock(StatusEffectManager.class), mock(StatusEffectManager.class),
                mock(KnockoutHandler.class), () -> true)
                .effectText("heal_bench:60")
                .attackerRuntime(attackerRuntime)
                .build();
                
        resolver.apply(ctx);
        
        org.mockito.Mockito.verify(benched2).heal(60);
        org.mockito.Mockito.verify(activeAttacker, org.mockito.Mockito.never()).heal(60);
        org.mockito.Mockito.verify(benched1, org.mockito.Mockito.never()).heal(60);
    }

    @Test
    void shouldResolveHealAllCorrectly() {
        assertEquals(AttackEffectType.HEAL_ALL, resolver.resolveType("heal_all:10"));
        
        final PlayerRuntime attackerRuntime = mock(PlayerRuntime.class);
        final Bench attackerBench = mock(Bench.class);
        final BattlePokemonState activeAttacker = mock(BattlePokemonState.class);
        final BattlePokemonState benched1 = mock(BattlePokemonState.class);
        final BattlePokemonState benched2 = mock(BattlePokemonState.class);
        
        org.mockito.Mockito.when(attackerRuntime.getActivePokemon()).thenReturn(activeAttacker);
        org.mockito.Mockito.when(attackerRuntime.getBench()).thenReturn(attackerBench);
        org.mockito.Mockito.when(attackerBench.getAll()).thenReturn(List.of(benched1, benched2));
        
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                mock(StatusEffectManager.class), mock(StatusEffectManager.class),
                mock(KnockoutHandler.class), () -> true)
                .effectText("heal_all:10")
                .attackerRuntime(attackerRuntime)
                .build();
                
        resolver.apply(ctx);
        
        org.mockito.Mockito.verify(activeAttacker).heal(10);
        org.mockito.Mockito.verify(benched1).heal(10);
        org.mockito.Mockito.verify(benched2).heal(10);
    }
}
