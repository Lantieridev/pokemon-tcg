package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.FakeBenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakePokemonTurnInPlayProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.PokemonTurnInPlayProvider;
import ar.edu.utn.frc.tup.piii.engine.model.Action;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.AttachEnergyAction;
import ar.edu.utn.frc.tup.piii.engine.model.DeclareAttackAction;
import ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage;
import ar.edu.utn.frc.tup.piii.engine.model.EvolveAction;
import ar.edu.utn.frc.tup.piii.engine.model.PlayTrainerAction;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.RetreatAction;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;
import ar.edu.utn.frc.tup.piii.engine.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Tests for RuleValidator. FR-009 through FR-015.
 */
class RuleValidatorTest {

    private static final int HP = 100;

    private TurnManager turnManager;
    private StatusEffectManager statusEffectManager;
    private FakePokemonTurnInPlayProvider turnInPlayProvider;
    private FakeBenchStateProvider benchProvider;
    private RuleValidator validator;

    @BeforeEach
    void setUp() {
        turnManager = Mockito.mock(TurnManager.class);
        statusEffectManager = Mockito.mock(StatusEffectManager.class);
        turnInPlayProvider = new FakePokemonTurnInPlayProvider();
        benchProvider = new FakeBenchStateProvider();
        validator = new RuleValidator(turnManager, statusEffectManager, turnInPlayProvider, benchProvider);
    }

    // ─── Constructor null-guards ───────────────────────────────────────────────

    @Test
    void shouldThrowNullPointerExceptionWhenTurnManagerIsNull() {
        assertThrows(NullPointerException.class,
                () -> new RuleValidator(null, statusEffectManager, turnInPlayProvider, benchProvider));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenStatusEffectManagerIsNull() {
        assertThrows(NullPointerException.class,
                () -> new RuleValidator(turnManager, null, turnInPlayProvider, benchProvider));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenTurnInPlayProviderIsNull() {
        assertThrows(NullPointerException.class,
                () -> new RuleValidator(turnManager, statusEffectManager, null, benchProvider));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenBenchStateProviderIsNull() {
        assertThrows(NullPointerException.class,
                () -> new RuleValidator(turnManager, statusEffectManager, turnInPlayProvider, null));
    }

    @Test
    void shouldNotThrowWhenAllDependenciesAreValid() {
        assertDoesNotThrow(() -> new RuleValidator(turnManager, statusEffectManager, turnInPlayProvider, benchProvider));
    }

    // ─── EvolveAction ─────────────────────────────────────────────────────────

    @Test
    void shouldReturnInvalidWhenEvolveActionIsCalledOnPlayerFirstTurn() {
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState evolution = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        turnInPlayProvider.set(target, 5);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        when(turnManager.isFirstTurnOfPlayer(0)).thenReturn(true);

        ValidationResult result = validator.validate(new EvolveAction(target, evolution));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "cannot_evolve_first_turn");
    }

    @Test
    void shouldReturnInvalidWhenTargetPokemonEnteredPlayThisTurn() {
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState evolution = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        target.setEvolutionStage(EvolutionStage.BASIC);
        evolution.setEvolutionStage(EvolutionStage.STAGE_1);
        evolution.setEvolvesFrom("Charmander");
        target.setName("Charmander");
        turnInPlayProvider.set(target, 0);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        when(turnManager.isFirstTurnOfPlayer(0)).thenReturn(false);

        ValidationResult result = validator.validate(new EvolveAction(target, evolution));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "pokemon_entered_this_turn");
    }

    @Test
    void shouldReturnValidWhenEvolvingBasicToStageOne() {
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState evolution = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        target.setName("Charmander");
        target.setEvolutionStage(EvolutionStage.BASIC);
        evolution.setEvolutionStage(EvolutionStage.STAGE_1);
        evolution.setEvolvesFrom("Charmander");
        turnInPlayProvider.set(target, 1);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        when(turnManager.isFirstTurnOfPlayer(0)).thenReturn(false);

        ValidationResult result = validator.validate(new EvolveAction(target, evolution));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnValidWhenTargetHasBeenInPlayAtLeastOneTurn() {
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState evolution = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        target.setName("Charmander");
        target.setEvolutionStage(EvolutionStage.BASIC);
        evolution.setEvolutionStage(EvolutionStage.STAGE_1);
        evolution.setEvolvesFrom("Charmander");
        turnInPlayProvider.set(target, 1);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        when(turnManager.isFirstTurnOfPlayer(0)).thenReturn(false);

        ValidationResult result = validator.validate(new EvolveAction(target, evolution));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnFirstTurnInvalidWhenBothFirstTurnAndEnteredThisTurnAreTrue() {
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState evolution = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        turnInPlayProvider.set(target, 0);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        when(turnManager.isFirstTurnOfPlayer(0)).thenReturn(true);

        ValidationResult result = validator.validate(new EvolveAction(target, evolution));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "cannot_evolve_first_turn");
    }

    // ─── EvolveAction — evolution chain validation (BLOCKER-6) ──────────────

    @Test
    void shouldReturnInvalidWhenEvolvingBasicToStageTwo() {
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState evolution = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        target.setEvolutionStage(EvolutionStage.BASIC);
        evolution.setEvolutionStage(EvolutionStage.STAGE_2);
        turnInPlayProvider.set(target, 2);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        when(turnManager.isFirstTurnOfPlayer(0)).thenReturn(false);

        ValidationResult result = validator.validate(new EvolveAction(target, evolution));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "invalid_evolution_stage");
    }

    @Test
    void shouldReturnInvalidWhenEvolvingStageOneToBasic() {
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState evolution = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        target.setEvolutionStage(EvolutionStage.STAGE_1);
        evolution.setEvolutionStage(EvolutionStage.BASIC);
        turnInPlayProvider.set(target, 2);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        when(turnManager.isFirstTurnOfPlayer(0)).thenReturn(false);

        ValidationResult result = validator.validate(new EvolveAction(target, evolution));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "invalid_evolution_stage");
    }

    @Test
    void shouldReturnInvalidWhenEvolutionCardEvolvesFromDifferentSpecies() {
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState evolution = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        target.setName("Charmander");
        target.setEvolutionStage(EvolutionStage.BASIC);
        evolution.setEvolutionStage(EvolutionStage.STAGE_1);
        evolution.setEvolvesFrom("Squirtle");
        turnInPlayProvider.set(target, 2);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        when(turnManager.isFirstTurnOfPlayer(0)).thenReturn(false);

        ValidationResult result = validator.validate(new EvolveAction(target, evolution));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "wrong_evolution_target");
    }

    // ─── RetreatAction ────────────────────────────────────────────────────────

    @Test
    void shouldReturnInvalidWhenBenchIsEmptyDuringRetreat() {
        FakeBattlePokemonState active = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        benchProvider.set(0, 0);
        when(statusEffectManager.canRetreat()).thenReturn(true);
        when(turnManager.activePlayerIndex()).thenReturn(0);

        ValidationResult result = validator.validate(new RetreatAction(active));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "empty_bench_for_retreat");
    }

    @Test
    void shouldReturnInvalidWhenRetreatIsBlockedByStatus() {
        FakeBattlePokemonState active = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        when(statusEffectManager.canRetreat()).thenReturn(false);

        ValidationResult result = validator.validate(new RetreatAction(active));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "retreat_blocked_by_status");
    }

    @Test
    void shouldReturnInvalidWhenRetreatAlreadyUsedThisTurn() {
        FakeBattlePokemonState active = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        mainPhase.recordRetreatUsed();
        when(statusEffectManager.canRetreat()).thenReturn(true);
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        ValidationResult result = validator.validate(new RetreatAction(active));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "retreat_already_used");
    }

    @Test
    void shouldReturnInvalidWhenAttachedEnergiesAreLessThanRetreatCost() {
        FakeBattlePokemonState active = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        active.setRetreatCost(2);
        active.addAttachedEnergy(PokemonType.FIRE);
        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        when(statusEffectManager.canRetreat()).thenReturn(true);
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        ValidationResult result = validator.validate(new RetreatAction(active));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "insufficient_energy_for_retreat");
    }

    @Test
    void shouldReturnValidWhenAttachedEnergiesEqualRetreatCost() {
        FakeBattlePokemonState active = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        active.setRetreatCost(2);
        active.addAttachedEnergy(PokemonType.FIRE);
        active.addAttachedEnergy(PokemonType.WATER);
        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        when(statusEffectManager.canRetreat()).thenReturn(true);
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        ValidationResult result = validator.validate(new RetreatAction(active));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnValidWhenAttachedEnergiesExceedRetreatCost() {
        FakeBattlePokemonState active = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        active.setRetreatCost(1);
        active.addAttachedEnergy(PokemonType.FIRE);
        active.addAttachedEnergy(PokemonType.WATER);
        active.addAttachedEnergy(PokemonType.GRASS);
        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        when(statusEffectManager.canRetreat()).thenReturn(true);
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        ValidationResult result = validator.validate(new RetreatAction(active));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnValidWhenRetreatCostIsZeroAndNoEnergyAttached() {
        FakeBattlePokemonState active = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        active.setRetreatCost(0);
        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        when(statusEffectManager.canRetreat()).thenReturn(true);
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        ValidationResult result = validator.validate(new RetreatAction(active));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    // ─── PlayTrainerAction ────────────────────────────────────────────────────

    @Test
    void shouldReturnInvalidWhenSupporterAlreadyPlayedThisTurn() {
        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        mainPhase.recordSupporterPlayed();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        ValidationResult result = validator.validate(new PlayTrainerAction(TrainerType.SUPPORTER));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "supporter_already_played");
    }

    @Test
    void shouldReturnInvalidWhenStadiumAlreadyPlayedThisTurn() {
        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        mainPhase.recordStadiumPlayed();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        ValidationResult result = validator.validate(new PlayTrainerAction(TrainerType.STADIUM));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "stadium_already_played");
    }

    @Test
    void shouldReturnValidWhenItemIsPlayed() {
        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        ValidationResult result = validator.validate(new PlayTrainerAction(TrainerType.ITEM));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnValidWhenFirstSupporterIsPlayedThisTurn() {
        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        ValidationResult result = validator.validate(new PlayTrainerAction(TrainerType.SUPPORTER));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnInvalidWhenPokemonToolTargetIsNull() {
        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        ValidationResult result = validator.validate(new PlayTrainerAction(TrainerType.POKEMON_TOOL, null));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "pokemon_tool_requires_target");
    }

    // ─── AttachEnergyAction ───────────────────────────────────────────────────

    @Test
    void shouldReturnValidWhenNoEnergyAttachedThisTurn() {
        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        ValidationResult result = validator.validate(new AttachEnergyAction(PokemonType.FIRE));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnInvalidWhenEnergyAlreadyAttachedThisTurn() {
        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        mainPhase.recordEnergyAttached();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        ValidationResult result = validator.validate(new AttachEnergyAction(PokemonType.WATER));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "energy_already_attached");
    }

    // ─── DeclareAttackAction ──────────────────────────────────────────────────

    @Test
    void shouldReturnInvalidWhenAttackIsBlockedByStatus() {
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        Attack attack = new Attack("Ember", 30, List.of(PokemonType.FIRE));
        when(statusEffectManager.canAttack()).thenReturn(false);

        ValidationResult result = validator.validate(new DeclareAttackAction(attacker, attack));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "attack_blocked_by_status");
    }

    @Test
    void shouldReturnInvalidWhenTypedEnergyRequirementIsUnmet() {
        // required: [FIRE, FIRE, COLORLESS], attached: [FIRE, WATER, WATER]
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        attacker.addAttachedEnergy(PokemonType.FIRE);
        attacker.addAttachedEnergy(PokemonType.WATER);
        attacker.addAttachedEnergy(PokemonType.WATER);
        Attack attack = new Attack("Flamethrower", 60, List.of(PokemonType.FIRE, PokemonType.FIRE, PokemonType.COLORLESS));
        when(statusEffectManager.canAttack()).thenReturn(true);

        ValidationResult result = validator.validate(new DeclareAttackAction(attacker, attack));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "insufficient_energy_for_attack");
    }

    @Test
    void shouldReturnValidWhenColorlessIsSatisfiedByRemainingEnergy() {
        // required: [FIRE, COLORLESS], attached: [FIRE, WATER]
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        attacker.addAttachedEnergy(PokemonType.FIRE);
        attacker.addAttachedEnergy(PokemonType.WATER);
        Attack attack = new Attack("Ember", 30, List.of(PokemonType.FIRE, PokemonType.COLORLESS));
        when(statusEffectManager.canAttack()).thenReturn(true);

        ValidationResult result = validator.validate(new DeclareAttackAction(attacker, attack));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnValidWhenAttackerHasSurplusEnergy() {
        // required: [FIRE], attached: [FIRE, FIRE, WATER]
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        attacker.addAttachedEnergy(PokemonType.FIRE);
        attacker.addAttachedEnergy(PokemonType.FIRE);
        attacker.addAttachedEnergy(PokemonType.WATER);
        Attack attack = new Attack("Ember", 30, List.of(PokemonType.FIRE));
        when(statusEffectManager.canAttack()).thenReturn(true);

        ValidationResult result = validator.validate(new DeclareAttackAction(attacker, attack));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnValidWhenAttackRequiresNoEnergy() {
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(HP, PokemonType.COLORLESS, null, null, false);
        Attack attack = new Attack("Splash", 0, List.of());
        when(statusEffectManager.canAttack()).thenReturn(true);

        ValidationResult result = validator.validate(new DeclareAttackAction(attacker, attack));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private void assertInvalidReason(final ValidationResult result, final String expectedReason) {
        org.junit.jupiter.api.Assertions.assertEquals(expectedReason,
                ((ValidationResult.Invalid) result).reason());
    }
}
