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
    void shouldDiscardEnergyFromDefender() {
        defender.addAttachedEnergy(PokemonType.WATER);
        defender.addAttachedEnergy(PokemonType.WATER);
        final AttackContext ctx = buildCtx("discard_opponent_energy:1");
        resolver.apply(ctx);
        assertEquals(1, defender.getAttachedEnergies().size());
    }

    @Test
    void shouldDiscardEnergyFromDefenderOnHeads() {
        defender.addAttachedEnergy(PokemonType.WATER);
        final AttackContext ctx = buildCtxWithCoinFlipper("coin_flip_discard_opponent_energy:1", () -> true);
        resolver.apply(ctx);
        assertTrue(defender.getAttachedEnergies().isEmpty());
    }

    @Test
    void shouldNotDiscardEnergyFromDefenderOnTails() {
        defender.addAttachedEnergy(PokemonType.WATER);
        final AttackContext ctx = buildCtxWithCoinFlipper("coin_flip_discard_opponent_energy:1", () -> false);
        resolver.apply(ctx);
        assertEquals(1, defender.getAttachedEnergies().size());
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

    @Test
    void shouldResolveStokeOnHeadsAndAttachUpTo3BasicEnergies() {
        assertEquals(AttackEffectType.STOKE, resolver.resolveType("stoke"));

        final PlayerRuntime attackerRuntime = mock(PlayerRuntime.class);
        final ar.edu.utn.frc.tup.piii.engine.model.Deck deck = mock(ar.edu.utn.frc.tup.piii.engine.model.Deck.class);
        org.mockito.Mockito.when(attackerRuntime.getDeck()).thenReturn(deck);

        final ar.edu.utn.frc.tup.piii.engine.model.EnergyCard fire1 = new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("fire-1", "Fire Energy", PokemonType.FIRE, true);
        final ar.edu.utn.frc.tup.piii.engine.model.EnergyCard fire2 = new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("fire-2", "Fire Energy", PokemonType.FIRE, true);

        org.mockito.Mockito.when(deck.searchAndRemove(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(3)))
                .thenReturn(List.of(fire1, fire2));

        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class), () -> true) // Heads
                .effectText("stoke")
                .attackerRuntime(attackerRuntime)
                .build();

        resolver.apply(ctx);

        assertEquals(2, attacker.getAttachedEnergies().size());
        org.mockito.Mockito.verify(deck).shuffle();
    }

    @Test
    void shouldNotResolveStokeOnTails() {
        final PlayerRuntime attackerRuntime = mock(PlayerRuntime.class);
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class), () -> false) // Tails
                .effectText("stoke")
                .attackerRuntime(attackerRuntime)
                .build();

        resolver.apply(ctx);

        assertTrue(attacker.getAttachedEnergies().isEmpty());
        org.mockito.Mockito.verifyNoInteractions(attackerRuntime);
    }

    @Test
    void shouldResolveCombustionBlastAndDisableIt() {
        assertEquals(AttackEffectType.COMBUSTION_BLAST, resolver.resolveType("combustion_blast"));

        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class), () -> true)
                .effectText("combustion_blast")
                .build();

        resolver.apply(ctx);

        assertEquals("Combustion Blast", attackerSM.getSelfDisabledAttackName());
        assertTrue(attackerSM.isSelfDisabledAttackSetThisTurn());
    }

    @Test
    void shouldResolveCoinFlipPreventDamage60OrLessOnHeads() {
        assertEquals(AttackEffectType.COIN_FLIP_PREVENT_DAMAGE_60_OR_LESS, resolver.resolveType("coin_flip_prevent_damage_60_or_less"));

        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class), () -> true) // Heads
                .effectText("coin_flip_prevent_damage_60_or_less")
                .build();

        resolver.apply(ctx);

        assertTrue(attackerSM.isDamagePreventedIf60OrLessNextTurn());
    }

    @Test
    void shouldNotResolveCoinFlipPreventDamage60OrLessOnTails() {
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class), () -> false) // Tails
                .effectText("coin_flip_prevent_damage_60_or_less")
                .build();

        resolver.apply(ctx);

        assertFalse(attackerSM.isDamagePreventedIf60OrLessNextTurn());
    }

    @Test
    void shouldResolveCallForFamilyCorrectly() {
        assertEquals(AttackEffectType.CALL_FOR_FAMILY, resolver.resolveType("call_for_family:1"));

        final PlayerRuntime attackerRuntime = mock(PlayerRuntime.class);
        final Bench bench = mock(Bench.class);
        final ar.edu.utn.frc.tup.piii.engine.model.Deck deck = mock(ar.edu.utn.frc.tup.piii.engine.model.Deck.class);
        final ar.edu.utn.frc.tup.piii.engine.session.MatchSession session = mock(ar.edu.utn.frc.tup.piii.engine.session.MatchSession.class);

        // Build a basic Pokemon to be found and placed on bench
        final ar.edu.utn.frc.tup.piii.engine.model.PokemonCard basicCard = mock(ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.class);
        org.mockito.Mockito.when(basicCard.getEvolutionStage()).thenReturn(ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.BASIC);

        org.mockito.Mockito.when(attackerRuntime.getBench()).thenReturn(bench);
        org.mockito.Mockito.when(attackerRuntime.getDeck()).thenReturn(deck);
        org.mockito.Mockito.when(bench.getAll()).thenReturn(List.of()); // 0 pokemon on bench, 1 free space
        // First call finds the basic card, second call finds nothing (loop termination)
        org.mockito.Mockito.when(deck.searchAndRemove(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1)))
                .thenReturn(List.of(basicCard))
                .thenReturn(List.of());

        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class), () -> true)
                .effectText("call_for_family:1")
                .attackerRuntime(attackerRuntime)
                .matchSession(session)
                .build();

        resolver.apply(ctx);

        // Should NOT create a pending selection request - placement is automatic
        org.mockito.Mockito.verify(session, org.mockito.Mockito.never()).setPendingSelectionRequest(org.mockito.ArgumentMatchers.any());
        // Should place the basic pokemon on the bench
        org.mockito.Mockito.verify(bench).place(org.mockito.ArgumentMatchers.any());
        // Should shuffle the deck afterwards
        org.mockito.Mockito.verify(deck).shuffle();
    }

    @Test
    void shouldResolveQuiverDanceCorrectly() {
        assertEquals(AttackEffectType.QUIVER_DANCE, resolver.resolveType("quiver_dance"));

        final PlayerRuntime attackerRuntime = mock(PlayerRuntime.class);
        final ar.edu.utn.frc.tup.piii.engine.model.Deck deck = mock(ar.edu.utn.frc.tup.piii.engine.model.Deck.class);
        final ar.edu.utn.frc.tup.piii.engine.session.MatchSession session = mock(ar.edu.utn.frc.tup.piii.engine.session.MatchSession.class);
        final ar.edu.utn.frc.tup.piii.engine.manager.TurnManager turnManager = mock(ar.edu.utn.frc.tup.piii.engine.manager.TurnManager.class);

        // Create a basic energy card to be found in the deck
        final ar.edu.utn.frc.tup.piii.engine.model.EnergyCard basicEnergy = mock(ar.edu.utn.frc.tup.piii.engine.model.EnergyCard.class);
        org.mockito.Mockito.when(basicEnergy.isBasic()).thenReturn(true);

        org.mockito.Mockito.when(attackerRuntime.getDeck()).thenReturn(deck);
        org.mockito.Mockito.when(deck.getCards()).thenReturn(List.of(basicEnergy));
        org.mockito.Mockito.when(session.getTurnManager()).thenReturn(turnManager);

        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class), () -> true)
                .effectText("quiver_dance")
                .attackerRuntime(attackerRuntime)
                .matchSession(session)
                .build();

        resolver.apply(ctx);

        org.mockito.Mockito.verify(session).setPendingSelectionRequest(org.mockito.ArgumentMatchers.argThat(req ->
                req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.QUIVER_DANCE
                && req.maxSelections() == 1
                && req.source() == ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK
        ));
        org.mockito.Mockito.verify(turnManager).interruptMainPhase();
    }

    @Test
    void shouldResolveHealSelfAndSleepCorrectly() {
        assertEquals(AttackEffectType.HEAL_SELF_AND_SLEEP, resolver.resolveType("heal_and_sleep:60"));

        final PlayerRuntime attackerRuntime = mock(PlayerRuntime.class);
        final StatusEffectManager statusEffectManager = mock(StatusEffectManager.class);
        org.mockito.Mockito.when(attackerRuntime.getStatusEffectManager()).thenReturn(statusEffectManager);

        attacker.addDamageCounters(8); // 80 damage

        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class), () -> true)
                .effectText("heal_and_sleep:60")
                .attackerRuntime(attackerRuntime)
                .build();

        resolver.apply(ctx);

        assertEquals(2, attacker.getDamageCounters()); // Healed 60 (6 counters) -> 2 counters left
        org.mockito.Mockito.verify(statusEffectManager).apply(StatusEffectType.DORMIDO);
    }

    @Test
    void shouldResolveDiscardDeckSelfCorrectly() {
        assertEquals(AttackEffectType.DISCARD_DECK_SELF, resolver.resolveType("discard_deck_self:5"));

        final PlayerRuntime attackerRuntime = mock(PlayerRuntime.class);
        final ar.edu.utn.frc.tup.piii.engine.model.Deck deck = mock(ar.edu.utn.frc.tup.piii.engine.model.Deck.class);
        final ar.edu.utn.frc.tup.piii.engine.model.DiscardPile discardPile = mock(ar.edu.utn.frc.tup.piii.engine.model.DiscardPile.class);

        org.mockito.Mockito.when(attackerRuntime.getDeck()).thenReturn(deck);
        org.mockito.Mockito.when(attackerRuntime.getDiscardPile()).thenReturn(discardPile);
        final List<ar.edu.utn.frc.tup.piii.engine.model.Card> topCards = List.of(mock(ar.edu.utn.frc.tup.piii.engine.model.Card.class));
        org.mockito.Mockito.when(deck.drawMultiple(5)).thenReturn(topCards);

        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class), () -> true)
                .effectText("discard_deck_self:5")
                .attackerRuntime(attackerRuntime)
                .build();

        resolver.apply(ctx);

        org.mockito.Mockito.verify(deck).drawMultiple(5);
        org.mockito.Mockito.verify(discardPile).addAll(topCards);
    }

    @Test
    void shouldResolveCoinFlipDiscardEnergyOnTails() {
        assertEquals(AttackEffectType.COIN_FLIP_DISCARD_ENERGY, resolver.resolveType("coin_flip_discard_energy:1"));

        // Setup attacker with an energy
        final ar.edu.utn.frc.tup.piii.engine.model.EnergyCard energyCard = new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("fake_id", "Fake Energy", PokemonType.FIRE, true);
        attacker.attachEnergy(energyCard);

        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class), () -> false) // Tails
                .effectText("coin_flip_discard_energy:1")
                .build();

        resolver.apply(ctx);

        assertTrue(attacker.getAttachedEnergyCards().isEmpty());
    }

    @Test
    void shouldNotResolveCoinFlipDiscardEnergyOnHeads() {
        // Setup attacker with an energy
        final ar.edu.utn.frc.tup.piii.engine.model.EnergyCard energyCard = new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("fake_id", "Fake Energy", PokemonType.FIRE, true);
        attacker.attachEnergy(energyCard);

        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class), () -> true) // Heads
                .effectText("coin_flip_discard_energy:1")
                .build();

        resolver.apply(ctx);

        assertEquals(1, attacker.getAttachedEnergyCards().size());
    }

    @Test
    void shouldResolveCoinFlipsUntilTailsDiscardOpponentEnergyWithHeads() {
        assertEquals(AttackEffectType.COIN_FLIPS_UNTIL_TAILS_DISCARD_OPPONENT_ENERGY, resolver.resolveType("coin_flips_until_tails_discard_opponent_energy"));

        // Setup defender with 3 energies
        defender.attachEnergy(new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("e1", "Energy 1", PokemonType.GRASS, true));
        defender.attachEnergy(new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("e2", "Energy 2", PokemonType.GRASS, true));
        defender.attachEnergy(new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("e3", "Energy 3", PokemonType.GRASS, true));

        // Set coin flipper to flip: true, true, false (2 heads, then tails)
        final java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class),
                () -> {
                    int step = count.getAndIncrement();
                    return step < 2; // heads, heads, tails
                })
                .effectText("coin_flips_until_tails_discard_opponent_energy")
                .build();

        resolver.apply(ctx);

        assertEquals(1, defender.getAttachedEnergyCards().size());
    }

    @Test
    void shouldResolveCoinFlipsUntilTailsDiscardOpponentEnergyWithTailsFirst() {
        // Setup defender with 3 energies
        defender.attachEnergy(new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("e1", "Energy 1", PokemonType.GRASS, true));
        defender.attachEnergy(new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("e2", "Energy 2", PokemonType.GRASS, true));
        defender.attachEnergy(new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("e3", "Energy 3", PokemonType.GRASS, true));

        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class),
                () -> false) // tails first
                .effectText("coin_flips_until_tails_discard_opponent_energy")
                .build();

        resolver.apply(ctx);

        assertEquals(3, defender.getAttachedEnergyCards().size());
    }

    @Test
    void shouldResolveSmokescreenType() {
        assertEquals(AttackEffectType.SMOKESCREEN, resolver.resolveType("smokescreen"));

        final AttackContext ctx = buildCtx("smokescreen");
        resolver.apply(ctx);

        assertTrue(defenderSM.has(StatusEffectType.PRECISION_BAJA));
    }

    @Test
    void shouldResolveCoinFlipSelfDisableOnTails() {
        assertEquals(AttackEffectType.COIN_FLIP_SELF_DISABLE, resolver.resolveType("coin_flip_self_disable"));

        final AttackContext ctx = buildCtxWithCoinFlipper("coin_flip_self_disable", () -> false); // Tails
        resolver.apply(ctx);

        assertTrue(attackerSM.isSelfDisabledNextTurn());
        assertTrue(attackerSM.isSelfDisabledNextTurnSetThisTurn());
    }
    @Test
    void shouldNotResolveCoinFlipSelfDisableOnHeads() {
        final AttackContext ctx = buildCtxWithCoinFlipper("coin_flip_self_disable", () -> true); // Heads
        resolver.apply(ctx);

        assertFalse(attackerSM.isSelfDisabledNextTurn());
        assertFalse(attackerSM.isSelfDisabledNextTurnSetThisTurn());
    }

    @Test
    void shouldResolvePreventDamage20() {
        assertEquals(AttackEffectType.PREVENT_DAMAGE_20, resolver.resolveType("prevent_damage_20"));

        final AttackContext ctx = buildCtx("prevent_damage_20");
        resolver.apply(ctx);

        assertTrue(attackerSM.isDamageReducedBy20NextTurn());
    }

    @Test
    void shouldResolveDiscardStadium() {
        assertEquals(AttackEffectType.DISCARD_STADIUM, resolver.resolveType("discard_stadium"));

        ar.edu.utn.frc.tup.piii.engine.model.TrainerCard stadium = mock(ar.edu.utn.frc.tup.piii.engine.model.TrainerCard.class);
        ar.edu.utn.frc.tup.piii.engine.session.MatchBoard board = new ar.edu.utn.frc.tup.piii.engine.session.MatchBoard(
                List.of(mock(ar.edu.utn.frc.tup.piii.engine.session.PlayerState.class), mock(ar.edu.utn.frc.tup.piii.engine.session.PlayerState.class))
        );
        board.replaceStadium(stadium);
        board.setActiveStadiumOwnerIndex(1); // Player 1 owned it

        ar.edu.utn.frc.tup.piii.engine.session.MatchSession session = mock(ar.edu.utn.frc.tup.piii.engine.session.MatchSession.class);
        org.mockito.Mockito.when(session.getBoard()).thenReturn(board);

        PlayerRuntime player1 = mock(PlayerRuntime.class);
        ar.edu.utn.frc.tup.piii.engine.model.DiscardPile discardPile = new ar.edu.utn.frc.tup.piii.engine.model.DiscardPile();
        org.mockito.Mockito.when(player1.getDiscardPile()).thenReturn(discardPile);
        org.mockito.Mockito.when(session.getPlayerRuntime(1)).thenReturn(player1);

        final AttackContext ctx = new AttackContext.Builder(attacker, defender, BASIC_ATTACK,
                attackerSM, defenderSM,
                mock(KnockoutHandler.class), () -> true)
                .effectText("discard_stadium")
                .matchSession(session)
                .build();

        resolver.apply(ctx);

        // Board stadium should be removed
        org.junit.jupiter.api.Assertions.assertNull(board.getActiveStadium());
        org.junit.jupiter.api.Assertions.assertEquals(-1, board.getActiveStadiumOwnerIndex());
        // Should be discarded to Player 1's discard pile
        assertEquals(1, discardPile.getCards().size());
        org.junit.jupiter.api.Assertions.assertSame(stadium, discardPile.getCards().get(0));
    }
}
