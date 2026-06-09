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
}
