package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.exception.PokemonAsleepException;
import ar.edu.utn.frc.tup.piii.engine.exception.PokemonParalyzedException;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.manager.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AttackPipelineTest {

    private static final int DEFENDER_HP = 100;
    private static final int ATTACKER_HP = 100;

    private FakeBattlePokemonState attacker;
    private FakeBattlePokemonState defender;
    private StatusEffectManager attackerSM;
    private StatusEffectManager defenderSM;
    private KnockoutHandler knockoutHandler;
    private AttackPipeline pipeline;

    @BeforeEach
    void setUp() {
        attacker = new FakeBattlePokemonState(ATTACKER_HP, PokemonType.FIRE, null, null, false);
        defender = new FakeBattlePokemonState(DEFENDER_HP, PokemonType.WATER, null, null, false);
        attackerSM = new StatusEffectManager(() -> true); // always heads
        defenderSM = new StatusEffectManager(() -> true);
        knockoutHandler = mock(KnockoutHandler.class);
        pipeline = buildDefaultPipeline(() -> true);
    }

    // --- Standard attack ---

    @Test
    void shouldDealDamageOnStandardAttack() {
        final Attack attack = new Attack("Ember", 30, List.of(PokemonType.FIRE));
        attacker.addAttachedEnergy(PokemonType.FIRE);

        final AttackContext ctx = buildCtx(attack, "");
        pipeline.execute(ctx);

        assertEquals(3, defender.getDamageCounters()); // 30 / 10 = 3 counters
        assertFalse(ctx.isAttackBlocked());
    }

    @Test
    void shouldNotMarkAttackBlockedOnSuccess() {
        final Attack attack = new Attack("Tackle", 10, List.of(PokemonType.COLORLESS));
        attacker.addAttachedEnergy(PokemonType.FIRE); // COLORLESS accepts any type

        final AttackContext ctx = buildCtx(attack, "");
        pipeline.execute(ctx);

        assertFalse(ctx.isAttackBlocked());
    }

    // --- Energy validation ---

    @Test
    void shouldBlockAttackWhenEnergyInsufficient() {
        final Attack attack = new Attack("Flamethrower", 60, List.of(PokemonType.FIRE, PokemonType.FIRE));
        attacker.addAttachedEnergy(PokemonType.FIRE); // only 1, needs 2

        final AttackContext ctx = buildCtx(attack, "");
        pipeline.execute(ctx);

        assertTrue(ctx.isAttackBlocked());
        assertEquals(0, defender.getDamageCounters());
    }

    @Test
    void shouldAllowColorlessToBeFilledByAnyType() {
        final Attack attack = new Attack("Quick Attack", 20, List.of(PokemonType.FIRE, PokemonType.COLORLESS));
        attacker.addAttachedEnergy(PokemonType.FIRE);
        attacker.addAttachedEnergy(PokemonType.WATER); // WATER satisfies COLORLESS

        final AttackContext ctx = buildCtx(attack, "");
        pipeline.execute(ctx);

        assertFalse(ctx.isAttackBlocked());
        assertEquals(2, defender.getDamageCounters()); // 20 / 10 = 2
    }

    @Test
    void shouldAllowFreeAttackWithNoEnergyRequired() {
        final Attack attack = new Attack("Splash", 0, List.of()); // free
        final AttackContext ctx = buildCtx(attack, "");
        pipeline.execute(ctx);

        assertFalse(ctx.isAttackBlocked());
    }

    // --- Status effect blocking ---

    @Test
    void shouldPropagateAsleepExceptionWhenAttackerIsAsleep() {
        attackerSM.apply(StatusEffectType.DORMIDO);
        final Attack attack = new Attack("Ember", 30, List.of());
        final AttackContext ctx = buildCtx(attack, "");

        assertThrows(PokemonAsleepException.class, () -> pipeline.execute(ctx));
    }

    @Test
    void shouldPropagateParalyzedExceptionWhenAttackerIsParalyzed() {
        attackerSM.apply(StatusEffectType.PARALIZADO);
        final Attack attack = new Attack("Ember", 30, List.of());
        final AttackContext ctx = buildCtx(attack, "");

        assertThrows(PokemonParalyzedException.class, () -> pipeline.execute(ctx));
    }

    @Test
    void shouldBlockAttackOnConfusionWhenCoinIsTails() {
        // confusion coin is resolved by the StatusEffectManager's own flipper, not the context's
        final StatusEffectManager tailsAttackerSM = new StatusEffectManager(() -> false);
        tailsAttackerSM.apply(StatusEffectType.CONFUNDIDO);
        final Attack attack = new Attack("Ember", 30, List.of());
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, attack,
                tailsAttackerSM, defenderSM, knockoutHandler, () -> false)
                .build();

        pipeline.execute(ctx);

        assertTrue(ctx.isAttackBlocked());
        assertEquals(0, defender.getDamageCounters());
        assertEquals(3, attacker.getDamageCounters()); // self-damage from confusion
    }

    @Test
    void shouldProceedAttackOnConfusionWhenCoinIsHeads() {
        attackerSM.apply(StatusEffectType.CONFUNDIDO);
        // attackerSM already uses () -> true (always heads), so proceed
        final Attack attack = new Attack("Ember", 30, List.of());
        final AttackContext ctx = buildCtx(attack, "");

        pipeline.execute(ctx);

        assertFalse(ctx.isAttackBlocked());
        assertEquals(3, defender.getDamageCounters());
    }

    // --- Post-damage effects ---

    @Test
    void shouldApplyPoisonEffectToDefender() {
        final Attack attack = new Attack("Poison Sting", 10, List.of());
        final AttackContext ctx = buildCtx(attack, "poison");
        pipeline.execute(ctx);

        assertTrue(defenderSM.has(StatusEffectType.ENVENENADO));
    }

    @Test
    void shouldApplyBurnEffectToDefender() {
        final Attack attack = new Attack("Flamethrower", 60, List.of());
        final AttackContext ctx = buildCtx(attack, "burn");
        pipeline.execute(ctx);

        assertTrue(defenderSM.has(StatusEffectType.QUEMADO));
    }

    @Test
    void shouldHealAttackerOnHealEffect() {
        attacker.addDamageCounters(5); // 50 HP damage
        final Attack attack = new Attack("Recover", 0, List.of());
        final AttackContext ctx = buildCtx(attack, "heal:30");
        pipeline.execute(ctx);

        assertEquals(2, attacker.getDamageCounters()); // healed 3 counters
    }

    @Test
    void shouldApplySelfDamageEffect() {
        final Attack attack = new Attack("Recoil", 30, List.of());
        final AttackContext ctx = buildCtx(attack, "self_damage:10");
        pipeline.execute(ctx);

        assertEquals(1, attacker.getDamageCounters()); // 10 HP = 1 counter to self
    }

    @Test
    void shouldDiscardEnergyOnDiscardEffect() {
        attacker.addAttachedEnergy(PokemonType.FIRE);
        attacker.addAttachedEnergy(PokemonType.FIRE);
        final Attack attack = new Attack("Flamethrower", 60, List.of());
        final AttackContext ctx = buildCtx(attack, "discard_energy:1");
        pipeline.execute(ctx);

        assertEquals(1, attacker.getAttachedEnergies().size());
    }

    // --- Coin-flip extra damage ---

    @Test
    void shouldAddExtraDamageWhenCoinFlipExtraLandsHeads() {
        // pipeline built with always-heads coinFlipper
        final Attack attack = new Attack("Flare", 20, List.of());
        final AttackContext ctx = buildCtx(attack, "coin_flip_extra:20");
        pipeline.execute(ctx);

        // base 20 + extra 20 = 40, no weakness → 4 counters
        assertEquals(4, defender.getDamageCounters());
    }

    @Test
    void shouldNotAddExtraDamageWhenCoinFlipExtraLandsTails() {
        final AttackPipeline tailsPipeline = buildDefaultPipeline(() -> false);
        final Attack attack = new Attack("Flare", 20, List.of());
        final AttackContext ctx = buildCtxWithPipelineFlip(attack, "coin_flip_extra:20", () -> false);

        tailsPipeline.execute(ctx);

        assertEquals(2, defender.getDamageCounters()); // only base 20 → 2 counters
    }

    // --- Knockout detection ---

    @Test
    void shouldCallKnockoutHandlerWhenDefenderFaints() {
        defender.addDamageCounters(9); // 90 damage → 10 HP left
        final Attack attack = new Attack("Final Strike", 10, List.of()); // +10 → KO
        final AttackContext ctx = buildCtx(attack, "");
        pipeline.execute(ctx);

        verify(knockoutHandler).onKnockout(defender, 1);
    }

    @Test
    void shouldGiveTwoPrizesForEXKnockout() {
        final FakeBattlePokemonState exDefender =
                new FakeBattlePokemonState(100, PokemonType.WATER, null, null, true);
        exDefender.addDamageCounters(9); // 90 damage
        final Attack attack = new Attack("Final Strike", 10, List.of());
        final AttackContext ctx = new AttackContext.Builder(attacker, exDefender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .build();

        pipeline.execute(ctx);

        final ArgumentCaptor<Integer> prizesCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(knockoutHandler).onKnockout(
                org.mockito.ArgumentMatchers.eq(exDefender), prizesCaptor.capture());
        assertEquals(2, prizesCaptor.getValue());
    }

    @Test
    void shouldNotCallKnockoutHandlerWhenDefenderSurvives() {
        final Attack attack = new Attack("Tackle", 10, List.of());
        final AttackContext ctx = buildCtx(attack, "");
        pipeline.execute(ctx);

        verify(knockoutHandler, never()).onKnockout(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    // --- Weakness interaction (integration) ---

    @Test
    void shouldDoubleBaseDamageOnWeakness() {
        // defender is FIRE type, attacker is WATER type
        final FakeBattlePokemonState fireDefender =
                new FakeBattlePokemonState(100, PokemonType.FIRE, PokemonType.WATER, null, false);
        final FakeBattlePokemonState waterAttacker =
                new FakeBattlePokemonState(100, PokemonType.WATER, null, null, false);
        final Attack attack = new Attack("Water Gun", 30, List.of());

        final AttackContext ctx = new AttackContext.Builder(waterAttacker, fireDefender, attack,
                new StatusEffectManager(() -> true), defenderSM, knockoutHandler, () -> true)
                .build();
        pipeline.execute(ctx);

        assertEquals(6, fireDefender.getDamageCounters()); // 30 * 2 = 60 / 10 = 6
    }

    @Test
    void shouldSuppressResistanceWhenMagneticStormIsActive() {
        // defender is GRASS type, resistant to FIRE, attacker is FIRE type
        final FakeBattlePokemonState grassDefender =
                new FakeBattlePokemonState(100, PokemonType.GRASS, null, PokemonType.FIRE, false);
        final ar.edu.utn.frc.tup.piii.engine.model.TrainerCard magneticStorm =
                new ar.edu.utn.frc.tup.piii.engine.model.TrainerCard.Builder("xy2-91", "Magnetic Storm", ar.edu.utn.frc.tup.piii.engine.model.TrainerType.STADIUM)
                .build();
        final Attack attack = new Attack("Ember", 30, List.of());

        final AttackContext ctx = new AttackContext.Builder(attacker, grassDefender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .stadiumProvider(() -> magneticStorm)
                .build();
        pipeline.execute(ctx);

        // Without Magnetic Storm: 30 - 20 (resistance) = 10 (1 counter)
        // With Magnetic Storm: 30 - 0 = 30 (3 counters)
        assertEquals(3, grassDefender.getDamageCounters());
    }

    @Test
    void shouldSuppressResistanceWhenIgnoreResistanceEffectTextIsActive() {
        final FakeBattlePokemonState grassDefender =
                new FakeBattlePokemonState(100, PokemonType.GRASS, null, PokemonType.FIRE, false);
        final Attack attack = new Attack("Smash Uppercut", 30, List.of());

        final AttackContext ctx = new AttackContext.Builder(attacker, grassDefender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .effectText("ignore_resistance")
                .build();
        pipeline.execute(ctx);

        assertEquals(3, grassDefender.getDamageCounters());
    }

    // --- helpers ---

    private AttackContext buildCtx(final Attack attack, final String effectText) {
        return new AttackContext.Builder(attacker, defender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .effectText(effectText)
                .build();
    }

    private AttackContext buildCtxWithPipelineFlip(
            final Attack attack, final String effectText, final ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper flipper) {
        return new AttackContext.Builder(attacker, defender, attack,
                attackerSM, defenderSM, knockoutHandler, flipper)
                .effectText(effectText)
                .build();
    }

    private AttackPipeline buildDefaultPipeline(
            final ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper flipper) {
        return new AttackPipeline(List.of(
                new ValidationStep(),
                new PreDamageEffectsStep(),
                new PokemonToolStep(new TrainerEffectResolver()),
                new StadiumEffectStep(new TrainerEffectResolver()),
                new AttackCancellationStep(),
                new DamageCalculationStep(new DamageCalculator()),
                new DamageApplicationStep(),
                new PostDamageEffectsStep(new AttackEffectResolver()),
                new KnockoutCheckStep()
        ));
    }
}
