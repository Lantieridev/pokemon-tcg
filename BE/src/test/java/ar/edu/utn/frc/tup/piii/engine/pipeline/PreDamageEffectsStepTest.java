package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.manager.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.Ability;
import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PreDamageEffectsStepTest {

    private FakeBattlePokemonState attacker;
    private FakeBattlePokemonState defender;
    private AttackContext ctx;
    private PreDamageEffectsStep step;

    @BeforeEach
    void setUp() {
        step = new PreDamageEffectsStep();
        attacker = new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false);
        attacker.setCardId("test-attacker");
        
        defender = new FakeBattlePokemonState(100, PokemonType.WATER, null, null, false);
        defender.setCardId("test-defender");

        ctx = new AttackContext.Builder(attacker, defender, new Attack("Tackle", 20, List.of()), mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class), () -> true).build();
    }

    @Test
    void testProcess_withSafeguardAndExAttacker_blocksAttack() {
        // Arrange
        Ability safeguard = new Ability("Safeguard", "Prevent all effects of attacks...", AbilityEffectId.SAFEGUARD);
        defender.setAbilities(List.of(safeguard));
        
        // Make attacker EX
        attacker = new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, true);
        attacker.setCardId("test-attacker-ex");
        ctx = new AttackContext.Builder(attacker, defender, new Attack("Tackle", 20, List.of()), mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class), () -> true).build();

        // Act
        step.process(ctx, () -> {});

        // Assert
        assertTrue(ctx.isAttackBlocked());
    }

    @Test
    void testProcess_withSafeguardAndNonExAttacker_doesNotBlockAttack() {
        // Arrange
        Ability safeguard = new Ability("Safeguard", "Prevent all effects of attacks...", AbilityEffectId.SAFEGUARD);
        defender.setAbilities(List.of(safeguard));
        
        // Attacker is NOT EX (from setUp)
        
        // Act
        step.process(ctx, () -> {});

        // Assert
        assertFalse(ctx.isAttackBlocked());
    }

    @Test
    void testProcess_withoutSafeguardAndExAttacker_doesNotBlockAttack() {
        // Arrange
        attacker = new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, true);
        attacker.setCardId("test-attacker-ex");
        ctx = new AttackContext.Builder(attacker, defender, new Attack("Tackle", 20, List.of()), mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class), () -> true).build();
        
        // Defender has no abilities
        
        // Act
        step.process(ctx, () -> {});

        // Assert
        assertFalse(ctx.isAttackBlocked());
    }

    @Test
    void testProcess_coinFlipFail_heads_doesNotBlockAttack() {
        ctx = new AttackContext.Builder(attacker, defender, new Attack("Hyper Fang", 40, List.of()), mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class), () -> true)
                .effectText("coin_flip_fail")
                .build();
        step.process(ctx, () -> {});
        assertFalse(ctx.isAttackBlocked());
    }

    @Test
    void testProcess_coinFlipFail_tails_blocksAttack() {
        ctx = new AttackContext.Builder(attacker, defender, new Attack("Hyper Fang", 40, List.of()), mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class), () -> false)
                .effectText("coin_flip_fail")
                .build();
        step.process(ctx, () -> {});
        assertTrue(ctx.isAttackBlocked());
    }

    @Test
    void testProcess_withDamagePreventedNextTurn_addsModifierToZero() {
        StatusEffectManager defenderSem = mock(StatusEffectManager.class);
        org.mockito.Mockito.when(defenderSem.isDamagePreventedNextTurn()).thenReturn(true);

        ctx = new AttackContext.Builder(attacker, defender, new Attack("Tackle", 20, List.of()), mock(StatusEffectManager.class), defenderSem, mock(KnockoutHandler.class), () -> true).build();

        step.process(ctx, () -> {});

        assertFalse(ctx.getDefenderModifiers().isEmpty());
        int finalDamage = ctx.getDefenderModifiers().get(0).apply(50);
        org.junit.jupiter.api.Assertions.assertEquals(0, finalDamage);
    }

    @Test
    void testProcess_withDamagePreventedIf60OrLessNextTurn_blocksDamageLessThan60() {
        StatusEffectManager defenderSem = mock(StatusEffectManager.class);
        org.mockito.Mockito.when(defenderSem.isDamagePreventedIf60OrLessNextTurn()).thenReturn(true);

        ctx = new AttackContext.Builder(attacker, defender, new Attack("Tackle", 20, List.of()), mock(StatusEffectManager.class), defenderSem, mock(KnockoutHandler.class), () -> true).build();

        step.process(ctx, () -> {});

        assertFalse(ctx.getDefenderModifiers().isEmpty());
        // Under or equal to 60 damage should be reduced to 0
        org.junit.jupiter.api.Assertions.assertEquals(0, ctx.getDefenderModifiers().get(0).apply(50));
        org.junit.jupiter.api.Assertions.assertEquals(0, ctx.getDefenderModifiers().get(0).apply(60));
        // Over 60 damage should NOT be reduced
        org.junit.jupiter.api.Assertions.assertEquals(70, ctx.getDefenderModifiers().get(0).apply(70));
    }

    @Test
    void testProcess_coinFlipsUntilTailsExtra_heads_addsExtraDamage() {
        // Set coin flipper to flip: true, true, false (2 heads, then tails)
        final java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);
        ctx = new AttackContext.Builder(attacker, defender, new Attack("Wham Bam Punch", 100, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class),
                () -> {
                    int step = count.getAndIncrement();
                    return step < 2; // heads, heads, tails
                })
                .effectText("coin_flips_until_tails_extra:30")
                .build();

        step.process(ctx, () -> {});

        assertFalse(ctx.getAttackerModifiers().isEmpty());
        // Initial damage is 100. Modifiers add 2 * 30 = 60. Final damage should be 160.
        org.junit.jupiter.api.Assertions.assertEquals(160, ctx.getAttackerModifiers().get(0).apply(100));
    }

    @Test
    void testProcess_coinFlipsUntilTailsExtra_tailsFirst_noExtraDamage() {
        ctx = new AttackContext.Builder(attacker, defender, new Attack("Wham Bam Punch", 100, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class),
                () -> false) // Tails on first flip
                .effectText("coin_flips_until_tails_extra:30")
                .build();

        step.process(ctx, () -> {});

        assertFalse(ctx.getAttackerModifiers().isEmpty());
        // Initial damage is 100. 0 heads. Final damage should be 100.
        org.junit.jupiter.api.Assertions.assertEquals(100, ctx.getAttackerModifiers().get(0).apply(100));
    }

    @Test
    void testProcess_powerfulFriends_withStage2OnBench_addsDamage() {
        final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime attackerRuntime = mock(ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime.class);
        final ar.edu.utn.frc.tup.piii.engine.model.Bench bench = new ar.edu.utn.frc.tup.piii.engine.model.Bench();
        final FakeBattlePokemonState benchedStage2 = new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false);
        benchedStage2.setEvolutionStage(ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.STAGE_2);
        bench.place(benchedStage2);
        org.mockito.Mockito.when(attackerRuntime.getBench()).thenReturn(bench);

        ctx = new AttackContext.Builder(attacker, defender, new Attack("Powerful Friends", 10, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class),
                () -> true)
                .effectText("powerful_friends:70")
                .attackerRuntime(attackerRuntime)
                .build();

        step.process(ctx, () -> {});

        assertFalse(ctx.getAttackerModifiers().isEmpty());
        // Base damage 10. Modifiers add 70. Final damage should be 80.
        org.junit.jupiter.api.Assertions.assertEquals(80, ctx.getAttackerModifiers().get(0).apply(10));
    }

    @Test
    void testProcess_powerfulFriends_withoutStage2OnBench_noExtraDamage() {
        final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime attackerRuntime = mock(ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime.class);
        final ar.edu.utn.frc.tup.piii.engine.model.Bench bench = new ar.edu.utn.frc.tup.piii.engine.model.Bench();
        final FakeBattlePokemonState benchedStage1 = new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false);
        benchedStage1.setEvolutionStage(ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.STAGE_1);
        bench.place(benchedStage1);
        org.mockito.Mockito.when(attackerRuntime.getBench()).thenReturn(bench);

        ctx = new AttackContext.Builder(attacker, defender, new Attack("Powerful Friends", 10, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class),
                () -> true)
                .effectText("powerful_friends:70")
                .attackerRuntime(attackerRuntime)
                .build();

        step.process(ctx, () -> {});

        assertTrue(ctx.getAttackerModifiers().isEmpty());
    }

    @Test
    void testProcess_damagePerEnergyType_addsExtraDamage() {
        // Setup attacker with 2 Fairy energy cards
        attacker.attachEnergy(new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("e1", "Fairy Energy 1", PokemonType.FAIRY, true));
        attacker.attachEnergy(new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("e2", "Fairy Energy 2", PokemonType.FAIRY, true));

        ctx = new AttackContext.Builder(attacker, defender, new Attack("Wonder Blast", 40, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class),
                () -> true)
                .effectText("damage_per_energy_type:fairy:20")
                .build();

        step.process(ctx, () -> {});

        assertFalse(ctx.getAttackerModifiers().isEmpty());
        // Base damage 40. Modifiers add 2 * 20 = 40. Final damage should be 80.
        org.junit.jupiter.api.Assertions.assertEquals(80, ctx.getAttackerModifiers().get(0).apply(40));
    }

    @Test
    void testProcess_damageIfTargetDamaged_withDamagedDefender_addsDamage() {
        defender.setDamageCounters(3); // Defender has 30 damage

        ctx = new AttackContext.Builder(attacker, defender, new Attack("Claw Rend", 60, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class),
                () -> true)
                .effectText("damage_if_target_damaged:30")
                .build();

        step.process(ctx, () -> {});

        assertFalse(ctx.getAttackerModifiers().isEmpty());
        // Base damage 60. Modifiers add 30. Final damage should be 90.
        org.junit.jupiter.api.Assertions.assertEquals(90, ctx.getAttackerModifiers().get(0).apply(60));
    }

    @Test
    void testProcess_damageIfTargetDamaged_withUndamagedDefender_noExtraDamage() {
        defender.setDamageCounters(0); // Defender is healthy

        ctx = new AttackContext.Builder(attacker, defender, new Attack("Claw Rend", 60, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class),
                () -> true)
                .effectText("damage_if_target_damaged:30")
                .build();

        step.process(ctx, () -> {});

        assertTrue(ctx.getAttackerModifiers().isEmpty());
    }

    @Test
    void testProcess_damageMinusPerCounter_subtractsDamage() {
        attacker.setDamageCounters(3); // Attacker has 30 damage

        ctx = new AttackContext.Builder(attacker, defender, new Attack("Big Tusk", 100, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class),
                () -> true)
                .effectText("damage_minus_per_counter:10")
                .build();

        step.process(ctx, () -> {});

        assertFalse(ctx.getAttackerModifiers().isEmpty());
        // Base damage 100. Resta 3 * 10 = 30. Daño final 70.
        org.junit.jupiter.api.Assertions.assertEquals(70, ctx.getAttackerModifiers().get(0).apply(100));
    }

    @Test
    void testProcess_damageMinusPerCounter_clampsToZero() {
        attacker.setDamageCounters(12); // Attacker has 120 damage

        ctx = new AttackContext.Builder(attacker, defender, new Attack("Big Tusk", 100, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class),
                () -> true)
                .effectText("damage_minus_per_counter:10")
                .build();

        step.process(ctx, () -> {});

        assertFalse(ctx.getAttackerModifiers().isEmpty());
        // Base damage 100. Resta 12 * 10 = 120. Daño final clamp a 0.
        org.junit.jupiter.api.Assertions.assertEquals(0, ctx.getAttackerModifiers().get(0).apply(100));
    }

    @Test
    void testProcess_revengeDamage_withKnockoutLastTurn_addsDamage() {
        final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime attackerRuntime = mock(ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime.class);
        org.mockito.Mockito.when(attackerRuntime.isKnockedOutLastTurn()).thenReturn(true);

        ctx = new AttackContext.Builder(attacker, defender, new Attack("Revenge", 20, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class),
                () -> true)
                .effectText("revenge_damage:70")
                .attackerRuntime(attackerRuntime)
                .build();

        step.process(ctx, () -> {});

        assertFalse(ctx.getAttackerModifiers().isEmpty());
        // Base damage 20. Modifiers add 70. Final damage should be 90.
        org.junit.jupiter.api.Assertions.assertEquals(90, ctx.getAttackerModifiers().get(0).apply(20));
    }

    @Test
    void testProcess_revengeDamage_withoutKnockoutLastTurn_noExtraDamage() {
        final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime attackerRuntime = mock(ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime.class);
        org.mockito.Mockito.when(attackerRuntime.isKnockedOutLastTurn()).thenReturn(false);

        ctx = new AttackContext.Builder(attacker, defender, new Attack("Revenge", 20, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class),
                () -> true)
                .effectText("revenge_damage:70")
                .attackerRuntime(attackerRuntime)
                .build();

        step.process(ctx, () -> {});

        assertTrue(ctx.getAttackerModifiers().isEmpty());
    }

    @Test
    void testProcess_damagePerOpponentPrize_calculatesCorrectDamage() {
        final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime defenderRuntime = mock(ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime.class);
        org.mockito.Mockito.when(defenderRuntime.getStartingPrizeCount()).thenReturn(6);
        org.mockito.Mockito.when(defenderRuntime.getPrizeCount()).thenReturn(4); // 2 prizes taken by opponent

        ctx = new AttackContext.Builder(attacker, defender, new Attack("Electricounter", 0, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class),
                () -> true)
                .effectText("damage_per_opponent_prize:40")
                .defenderRuntime(defenderRuntime)
                .build();

        step.process(ctx, () -> {});

        assertFalse(ctx.getAttackerModifiers().isEmpty());
        // Base damage 0. Modifiers set total to 2 * 40 = 80.
        org.junit.jupiter.api.Assertions.assertEquals(80, ctx.getAttackerModifiers().get(0).apply(0));
    }

    @Test
    void testProcess_withDamageReducedBy20NextTurn_reducesIncomingDamage() {
        StatusEffectManager defenderSem = mock(StatusEffectManager.class);
        org.mockito.Mockito.when(defenderSem.isDamageReducedBy20NextTurn()).thenReturn(true);

        ctx = new AttackContext.Builder(attacker, defender, new Attack("Tackle", 20, List.of()), mock(StatusEffectManager.class), defenderSem, mock(KnockoutHandler.class), () -> true).build();

        step.process(ctx, () -> {});

        assertFalse(ctx.getDefenderModifiers().isEmpty());
        // 50 damage reduced by 20 -> 30
        org.junit.jupiter.api.Assertions.assertEquals(30, ctx.getDefenderModifiers().get(0).apply(50));
        // 10 damage reduced by 20 -> 0 (min 0)
        org.junit.jupiter.api.Assertions.assertEquals(0, ctx.getDefenderModifiers().get(0).apply(10));
    }

    @Test
    void testProcess_withDiscardOpponentTool_discardsDefenderTool() {
        ar.edu.utn.frc.tup.piii.engine.model.TrainerCard tool = mock(ar.edu.utn.frc.tup.piii.engine.model.TrainerCard.class);
        defender.attachTool(tool);

        ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime defenderRuntime = mock(ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime.class);
        ar.edu.utn.frc.tup.piii.engine.model.DiscardPile discardPile = new ar.edu.utn.frc.tup.piii.engine.model.DiscardPile();
        org.mockito.Mockito.when(defenderRuntime.getDiscardPile()).thenReturn(discardPile);

        ctx = new AttackContext.Builder(attacker, defender, new Attack("Peck Off", 10, List.of()), mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class), () -> true)
                .effectText("discard_opponent_tool")
                .defenderRuntime(defenderRuntime)
                .build();

        step.process(ctx, () -> {});

        assertFalse(defender.hasToolAttached());
        org.junit.jupiter.api.Assertions.assertEquals(1, discardPile.getCards().size());
        org.junit.jupiter.api.Assertions.assertSame(tool, discardPile.getCards().get(0));
    }

    @Test
    void testProcess_rockRush_firstRun_withFightingEnergies_setsSelectionRequestAndZeroDamage() {
        final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime attackerRuntime = mock(ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime.class);
        final ar.edu.utn.frc.tup.piii.engine.model.Hand hand = new ar.edu.utn.frc.tup.piii.engine.model.Hand();
        hand.addCard(new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("e1", "Fighting Energy", PokemonType.FIGHTING, true));
        org.mockito.Mockito.when(attackerRuntime.getHand()).thenReturn(hand);

        final ar.edu.utn.frc.tup.piii.engine.session.MatchSession session = mock(ar.edu.utn.frc.tup.piii.engine.session.MatchSession.class);
        final ar.edu.utn.frc.tup.piii.engine.manager.TurnManager turnManager = mock(ar.edu.utn.frc.tup.piii.engine.manager.TurnManager.class);
        org.mockito.Mockito.when(session.getTurnManager()).thenReturn(turnManager);

        ctx = new AttackContext.Builder(attacker, defender, new Attack("Rock Rush", 0, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class),
                () -> true)
                .effectText("discard_hand_energy_multiply_damage:fighting:30")
                .attackerRuntime(attackerRuntime)
                .matchSession(session)
                .build();

        step.process(ctx, () -> {});

        org.mockito.Mockito.verify(session).setPendingSelectionRequest(org.mockito.ArgumentMatchers.argThat(req ->
                req.sourceEffect() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.ROCK_RUSH
                && req.maxSelections() == 1
                && req.source() == ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND
        ));
        org.mockito.Mockito.verify(turnManager).interruptMainPhase();
        assertFalse(ctx.getDefenderModifiers().isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(0, ctx.getDefenderModifiers().get(0).apply(30));
    }

    @Test
    void testProcess_rockRush_firstRun_withoutFightingEnergies_doesZeroDamageAndNoRequest() {
        final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime attackerRuntime = mock(ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime.class);
        final ar.edu.utn.frc.tup.piii.engine.model.Hand hand = new ar.edu.utn.frc.tup.piii.engine.model.Hand();
        hand.addCard(new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("e1", "Fire Energy", PokemonType.FIRE, true));
        org.mockito.Mockito.when(attackerRuntime.getHand()).thenReturn(hand);

        final ar.edu.utn.frc.tup.piii.engine.session.MatchSession session = mock(ar.edu.utn.frc.tup.piii.engine.session.MatchSession.class);

        ctx = new AttackContext.Builder(attacker, defender, new Attack("Rock Rush", 0, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class),
                () -> true)
                .effectText("discard_hand_energy_multiply_damage:fighting:30")
                .attackerRuntime(attackerRuntime)
                .matchSession(session)
                .build();

        step.process(ctx, () -> {});

        org.mockito.Mockito.verify(session, org.mockito.Mockito.never()).setPendingSelectionRequest(org.mockito.ArgumentMatchers.any());
        assertFalse(ctx.getAttackerModifiers().isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(0, ctx.getAttackerModifiers().get(0).apply(30));
    }

    @Test
    void testProcess_rockRush_secondRun_calculatesCorrectDamage() {
        ctx = new AttackContext.Builder(attacker, defender, new Attack("Rock Rush", 0, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class),
                () -> true)
                .effectText("discard_hand_energy_multiply_damage:fighting:30")
                .build();

        ctx.setRockRushResolved(true);
        ctx.setRockRushDiscardCount(2);

        step.process(ctx, () -> {});

        assertFalse(ctx.getAttackerModifiers().isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(60, ctx.getAttackerModifiers().get(0).apply(30));
    }

    @Test
    void testProcess_damageAllOpponents_setsBaseDamage() {
        ctx = new AttackContext.Builder(attacker, defender, new Attack("Petal Blizzard", 0, List.of()),
                mock(StatusEffectManager.class), mock(StatusEffectManager.class), mock(KnockoutHandler.class),
                () -> true)
                .effectText("damage_all_opponents:20")
                .build();

        step.process(ctx, () -> {});

        assertFalse(ctx.getAttackerModifiers().isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(20, ctx.getAttackerModifiers().get(0).apply(0));
    }
}

