package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.FakeBenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakeHandStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakePokemonTurnInPlayProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.PokemonTurnInPlayProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.BattlefieldStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;
import ar.edu.utn.frc.tup.piii.engine.model.Action;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.AttachEnergyAction;
import ar.edu.utn.frc.tup.piii.engine.model.DeclareAttackAction;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage;
import ar.edu.utn.frc.tup.piii.engine.model.EvolveAction;
import ar.edu.utn.frc.tup.piii.engine.model.PlaceBasicPokemonAction;
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
    private FakeHandStateProvider handProvider;
    private RuleValidator validator;

    @BeforeEach
    void setUp() {
        turnManager = Mockito.mock(TurnManager.class);
        statusEffectManager = Mockito.mock(StatusEffectManager.class);
        turnInPlayProvider = new FakePokemonTurnInPlayProvider();
        benchProvider = new FakeBenchStateProvider();
        handProvider = new FakeHandStateProvider();
        validator = new RuleValidator(turnManager, statusEffectManager, turnInPlayProvider, benchProvider, handProvider);
    }

    // ─── Constructor null-guards ───────────────────────────────────────────────

    @Test
    void shouldThrowNullPointerExceptionWhenTurnManagerIsNull() {
        assertThrows(NullPointerException.class,
                () -> new RuleValidator(null, statusEffectManager, turnInPlayProvider, benchProvider, handProvider));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenStatusEffectManagerIsNull() {
        assertThrows(NullPointerException.class,
                () -> new RuleValidator(turnManager, (StatusEffectManager) null, turnInPlayProvider, benchProvider, handProvider));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenTurnInPlayProviderIsNull() {
        assertThrows(NullPointerException.class,
                () -> new RuleValidator(turnManager, statusEffectManager, null, benchProvider, handProvider));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenBenchStateProviderIsNull() {
        assertThrows(NullPointerException.class,
                () -> new RuleValidator(turnManager, statusEffectManager, turnInPlayProvider, null, handProvider));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenHandStateProviderIsNull() {
        assertThrows(NullPointerException.class,
                () -> new RuleValidator(turnManager, statusEffectManager, turnInPlayProvider, benchProvider, (HandStateProvider) null));
    }

    @Test
    void shouldNotThrowWhenAllDependenciesAreValid() {
        assertDoesNotThrow(() -> new RuleValidator(turnManager, statusEffectManager, turnInPlayProvider, benchProvider, handProvider));
    }

    // ─── EvolveAction ─────────────────────────────────────────────────────────

    @Test
    void shouldReturnInvalidWhenEvolveActionIsCalledOnPlayerFirstTurn() {
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        ar.edu.utn.frc.tup.piii.engine.model.PokemonCard evolution = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder(
            "evo-id", "Evo", HP, ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIRE)
            .evolutionStage(EvolutionStage.BASIC).build();
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
        ar.edu.utn.frc.tup.piii.engine.model.PokemonCard evolution = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder(
            "evo-id", "Evo", HP, ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIRE)
            .evolutionStage(EvolutionStage.STAGE_1).evolvesFrom("Charmander").build();
        target.setName("Charmander");
        turnInPlayProvider.set(target, 0);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        when(turnManager.isFirstTurnOfPlayer(0)).thenReturn(false);

        ValidationResult result = validator.validate(new EvolveAction(target, evolution));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "pokemon_entered_this_turn");
    }

    @Test
    void shouldReturnValidWhenTargetPokemonHasAdaptiveEvolutionOnFirstTurnOrTurnEntered() {
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        target.setAbilities(List.of(new ar.edu.utn.frc.tup.piii.engine.model.Ability(
            "Adaptive Evolution", "", ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.ADAPTIVE_EVOLUTION
        )));
        ar.edu.utn.frc.tup.piii.engine.model.PokemonCard evolution = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder(
            "evo-id", "Evo", HP, ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIRE)
            .evolutionStage(EvolutionStage.STAGE_1).evolvesFrom("Caterpie").build();
        target.setName("Caterpie");
        
        turnInPlayProvider.set(target, 0);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        when(turnManager.isFirstTurnOfPlayer(0)).thenReturn(true);
        
        ValidationResult result1 = validator.validate(new EvolveAction(target, evolution));
        assertInstanceOf(ValidationResult.Valid.class, result1);

        when(turnManager.isFirstTurnOfPlayer(0)).thenReturn(false);
        ValidationResult result2 = validator.validate(new EvolveAction(target, evolution));
        assertInstanceOf(ValidationResult.Valid.class, result2);
    }

    @Test
    void shouldReturnValidWhenEvolvingBasicToStageOne() {
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        ar.edu.utn.frc.tup.piii.engine.model.PokemonCard evolution = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder(
            "evo-id", "Evo", HP, ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIRE)
            .evolutionStage(EvolutionStage.STAGE_1).evolvesFrom("Charmander").build();
        target.setName("Charmander");
        turnInPlayProvider.set(target, 1);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        when(turnManager.isFirstTurnOfPlayer(0)).thenReturn(false);

        ValidationResult result = validator.validate(new EvolveAction(target, evolution));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnValidWhenTargetHasBeenInPlayAtLeastOneTurn() {
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        ar.edu.utn.frc.tup.piii.engine.model.PokemonCard evolution = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder(
            "evo-id", "Evo", HP, ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIRE)
            .evolutionStage(EvolutionStage.STAGE_1).evolvesFrom("Charmander").build();
        target.setName("Charmander");
        turnInPlayProvider.set(target, 1);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        when(turnManager.isFirstTurnOfPlayer(0)).thenReturn(false);

        ValidationResult result = validator.validate(new EvolveAction(target, evolution));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnFirstTurnInvalidWhenBothFirstTurnAndEnteredThisTurnAreTrue() {
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        ar.edu.utn.frc.tup.piii.engine.model.PokemonCard evolution = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder(
            "evo-id", "Evo", HP, ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIRE)
            .evolutionStage(EvolutionStage.BASIC).build();
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
        ar.edu.utn.frc.tup.piii.engine.model.PokemonCard evolution = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder(
            "evo-id", "Evo", HP, ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIRE)
            .evolutionStage(EvolutionStage.STAGE_2).build();
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
        ar.edu.utn.frc.tup.piii.engine.model.PokemonCard evolution = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder(
            "evo-id", "Evo", HP, ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIRE)
            .evolutionStage(EvolutionStage.BASIC).build();
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
        ar.edu.utn.frc.tup.piii.engine.model.PokemonCard evolution = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder(
            "evo-id", "Evo", HP, ar.edu.utn.frc.tup.piii.engine.model.PokemonType.FIRE)
            .evolutionStage(EvolutionStage.STAGE_1).evolvesFrom("Squirtle").build();
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

        ValidationResult result = validator.validate(new RetreatAction(active, 0, java.util.List.of(0, 1)));

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

        ValidationResult result = validator.validate(new RetreatAction(active, 0, java.util.List.of(0)));

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

        ValidationResult result = validator.validate(new RetreatAction(active, 0, java.util.Collections.emptyList()));

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

        ValidationResult result = validator.validate(new AttachEnergyAction(new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false), PokemonType.FIRE));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnInvalidWhenEnergyAlreadyAttachedThisTurn() {
        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        mainPhase.recordEnergyAttached();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        ValidationResult result = validator.validate(new AttachEnergyAction(new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false), PokemonType.WATER));

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
    void shouldReturnInvalidWhenAttackIsDisabledByEffect() {
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        Attack attack = new Attack("Ember", 30, List.of(PokemonType.FIRE));
        when(statusEffectManager.canAttack()).thenReturn(true);
        when(statusEffectManager.getDisabledAttackName()).thenReturn("Ember");

        ValidationResult result = validator.validate(new DeclareAttackAction(attacker, attack));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "attack_disabled_by_effect");
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

    // ─── RetreatAction: Fairy Garden (xy1-117) ────────────────────────────────

    @Test
    void shouldAllowFreeRetreatWhenFairyGardenActiveAndPokemonHasFairyEnergy() {
        final TrainerCard fairyGarden = new TrainerCard.Builder("xy1-117", "Fairy Garden", TrainerType.STADIUM)
                .build();
        final RuleValidator fairyValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, benchProvider, handProvider,
                () -> fairyGarden);

        final FakeBattlePokemonState active = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        active.setRetreatCost(2); // normally costs 2, but Fairy Garden makes it free
        active.addAttachedEnergy(PokemonType.FAIRY); // triggers Fairy Garden

        final ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        when(statusEffectManager.canRetreat()).thenReturn(true);
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        benchProvider.set(0, 1);

        // With Fairy Garden, retreat is free — pass empty energy index list
        final ValidationResult result = fairyValidator.validate(
                new RetreatAction(active, 0, java.util.Collections.emptyList()));

        assertInstanceOf(ValidationResult.Valid.class, result,
                "Fairy Garden must allow free retreat when Pokémon has FAIRY energy");
    }

    @Test
    void shouldAllowFreeRetreatWhenFairyGardenActiveAndPokemonHasRainbowEnergy() {
        final TrainerCard fairyGarden = new TrainerCard.Builder("xy1-117", "Fairy Garden", TrainerType.STADIUM)
                .build();
        final RuleValidator fairyValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, benchProvider, handProvider,
                () -> fairyGarden);

        final FakeBattlePokemonState active = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        active.setRetreatCost(2); // normally costs 2
        EnergyCard rainbow = new EnergyCard("rainbow-1", "Rainbow Energy", PokemonType.COLORLESS, false, 1, true);
        active.attachEnergy(rainbow); // triggers Fairy Garden because Rainbow provides all types including Fairy

        final ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        when(statusEffectManager.canRetreat()).thenReturn(true);
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        benchProvider.set(0, 1);

        final ValidationResult result = fairyValidator.validate(
                new RetreatAction(active, 0, java.util.Collections.emptyList()));

        assertInstanceOf(ValidationResult.Valid.class, result,
                "Fairy Garden must allow free retreat when Pokémon has Rainbow Energy");
    }

    @Test
    void shouldNotReduceRetreatCostWhenFairyGardenActiveButNoPokemonHasFairyEnergy() {
        final TrainerCard fairyGarden = new TrainerCard.Builder("xy1-117", "Fairy Garden", TrainerType.STADIUM)
                .build();
        final RuleValidator fairyValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, benchProvider, handProvider,
                () -> fairyGarden);

        final FakeBattlePokemonState active = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        active.setRetreatCost(2);
        active.addAttachedEnergy(PokemonType.FIRE); // No Fairy energy → Fairy Garden doesn't apply

        final ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        when(statusEffectManager.canRetreat()).thenReturn(true);
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        benchProvider.set(0, 1);

        // Only 1 energy but cost is 2 → should be invalid even with Fairy Garden
        final ValidationResult result = fairyValidator.validate(
                new RetreatAction(active, 0, java.util.List.of(0)));

        assertInstanceOf(ValidationResult.Invalid.class, result,
                "Fairy Garden must NOT reduce retreat cost without FAIRY energy");
        assertInvalidReason(result, "insufficient_energy_for_retreat");
    }

    @Test
    void shouldNotReduceRetreatCostWhenNoStadiumIsActive() {
        // Default validator has null stadiumProvider
        final FakeBattlePokemonState active = new FakeBattlePokemonState(HP, PokemonType.FAIRY, null, null, false);
        active.setRetreatCost(1);
        active.addAttachedEnergy(PokemonType.FAIRY);

        final ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        when(statusEffectManager.canRetreat()).thenReturn(true);
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        benchProvider.set(0, 1);

        // No stadium → must discard 1 energy normally
        final ValidationResult result = validator.validate(
                new RetreatAction(active, 0, java.util.List.of(0)));

        assertInstanceOf(ValidationResult.Valid.class, result,
                "Without Fairy Garden, retreat follows normal cost (1 energy)");
    }

    // ─── PlaceBasicPokemonAction ──────────────────────────────────────────────

    @Test
    void shouldReturnInvalidWhenCardNotInHand() {
        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase = new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);
        when(turnManager.activePlayerIndex()).thenReturn(0);

        ValidationResult result = validator.validate(new PlaceBasicPokemonAction("missing-card-id"));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "card_not_in_hand");
    }

    @Test
    void shouldReturnInvalidWhenCardIsNotBasicPokemon() {
        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase = new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);
        when(turnManager.activePlayerIndex()).thenReturn(0);

        ar.edu.utn.frc.tup.piii.engine.model.PokemonCard stage1 = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder(
                "charmeleon-id", "Charmeleon", 90, PokemonType.FIRE)
                .evolutionStage(EvolutionStage.STAGE_1).build();
        handProvider.addCard(0, stage1);

        ValidationResult result = validator.validate(new PlaceBasicPokemonAction("charmeleon-id"));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "card_not_basic_pokemon");
    }

    @Test
    void shouldReturnInvalidWhenBenchIsFull() {
        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase = new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);
        when(turnManager.activePlayerIndex()).thenReturn(0);

        ar.edu.utn.frc.tup.piii.engine.model.PokemonCard basic = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder(
                "charmander-id", "Charmander", 60, PokemonType.FIRE)
                .evolutionStage(EvolutionStage.BASIC).build();
        handProvider.addCard(0, basic);

        // Max bench is typically 5
        benchProvider.set(0, 5);

        ValidationResult result = validator.validate(new PlaceBasicPokemonAction("charmander-id"));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "bench_full");
    }

    @Test
    void shouldReturnValidWhenPlacingBasicPokemonWithBenchSpace() {
        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase = new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);
        when(turnManager.activePlayerIndex()).thenReturn(0);

        ar.edu.utn.frc.tup.piii.engine.model.PokemonCard basic = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder(
                "charmander-id", "Charmander", 60, PokemonType.FIRE)
                .evolutionStage(EvolutionStage.BASIC).build();
        handProvider.addCard(0, basic);

        benchProvider.set(0, 4);

        ValidationResult result = validator.validate(new PlaceBasicPokemonAction("charmander-id"));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    // ─── Special Energy — Rainbow & Double Colorless ────────────────────────────

    @Test
    void shouldReturnValidWhenRainbowEnergySatisfiesTypedRequirement() {
        // required: [FIRE], attached: [Rainbow Energy (COLORLESS, provides all types)]
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        EnergyCard rainbow = new EnergyCard("rainbow-1", "Rainbow Energy",
                PokemonType.COLORLESS, false, 1, true);
        attacker.attachEnergy(rainbow);
        Attack attack = new Attack("Ember", 30, List.of(PokemonType.FIRE));
        when(statusEffectManager.canAttack()).thenReturn(true);

        ValidationResult result = validator.validate(new DeclareAttackAction(attacker, attack));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnValidWhenDoubleColorlessEnergySatisfies2ColorlessRequirements() {
        // required: [COLORLESS, COLORLESS], attached: [DCE (2 units COLORLESS)]
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(HP, PokemonType.COLORLESS, null, null, false);
        EnergyCard dce = new EnergyCard("dce-1", "Double Colorless Energy",
                PokemonType.COLORLESS, false, 2, false);
        attacker.attachEnergy(dce);
        Attack attack = new Attack("Tackle", 20, List.of(PokemonType.COLORLESS, PokemonType.COLORLESS));
        when(statusEffectManager.canAttack()).thenReturn(true);

        ValidationResult result = validator.validate(new DeclareAttackAction(attacker, attack));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldPreferExactMatchOverRainbowForTypedRequirement() {
        // required: [FIRE, COLORLESS], attached: [Fire Energy, Rainbow Energy]
        // Fire should satisfy FIRE; Rainbow should satisfy COLORLESS.
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        attacker.addAttachedEnergy(PokemonType.FIRE);
        EnergyCard rainbow = new EnergyCard("rainbow-1", "Rainbow Energy",
                PokemonType.COLORLESS, false, 1, true);
        attacker.attachEnergy(rainbow);
        Attack attack = new Attack("Flamethrower", 40, List.of(PokemonType.FIRE, PokemonType.COLORLESS));
        when(statusEffectManager.canAttack()).thenReturn(true);

        ValidationResult result = validator.validate(new DeclareAttackAction(attacker, attack));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnInvalidWhenRainbowEnergyAloneCannotSatisfyMultipleTypedRequirements() {
        // required: [FIRE, FIRE], attached: [Rainbow Energy (1 unit)]
        FakeBattlePokemonState attacker = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        EnergyCard rainbow = new EnergyCard("rainbow-1", "Rainbow Energy",
                PokemonType.COLORLESS, false, 1, true);
        attacker.attachEnergy(rainbow);
        Attack attack = new Attack("Flamethrower", 60, List.of(PokemonType.FIRE, PokemonType.FIRE));
        when(statusEffectManager.canAttack()).thenReturn(true);

        ValidationResult result = validator.validate(new DeclareAttackAction(attacker, attack));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "insufficient_energy_for_attack");
    }

    // ─── Trainer Effects Validation Tests ─────────────────────────────────────

    @Test
    void shouldReturnInvalidWhenSuperPotionTargetIsNull() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        ValidationResult result = validator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "potion-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.SUPER_POTION));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_pokemon_required");
    }

    @Test
    void shouldReturnInvalidWhenSuperPotionTargetHasNoDamage() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        target.setDamageCounters(0);
        ValidationResult result = validator.validate(new PlayTrainerAction(
                TrainerType.ITEM, target, "potion-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.SUPER_POTION));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_has_no_damage");
    }

    @Test
    void shouldReturnInvalidWhenSuperPotionTargetHasNoEnergy() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        target.setDamageCounters(2);
        // no energies attached
        ValidationResult result = validator.validate(new PlayTrainerAction(
                TrainerType.ITEM, target, "potion-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.SUPER_POTION));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_has_no_energy");
    }

    @Test
    void shouldReturnValidWhenSuperPotionIsValid() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        target.setDamageCounters(2);
        target.addAttachedEnergy(PokemonType.FIRE);
        ValidationResult result = validator.validate(new PlayTrainerAction(
                TrainerType.ITEM, target, "potion-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.SUPER_POTION));
        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnInvalidWhenCassiusTargetIsNull() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        ValidationResult result = validator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, null, "cassius-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.CASSIUS));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_pokemon_required");
    }

    @Test
    void shouldReturnInvalidWhenCassiusTargetIsNotInPlay() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        ValidationResult result = validator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, target, "cassius-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.CASSIUS));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_pokemon_not_in_play");
    }

    @Test
    void shouldReturnInvalidWhenEvosodaTargetCannotEvolveFurther() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        target.setName("Venusaur");
        target.setEvolutionStage(EvolutionStage.STAGE_2);
        turnInPlayProvider.set(target, 2);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        when(turnManager.isFirstTurnOfPlayer(0)).thenReturn(false);
        
        // Put Venusaur in play (active)
        final BattlefieldStateProvider bfp = Mockito.mock(BattlefieldStateProvider.class);
        when(bfp.getActivePokemon(0)).thenReturn(target);
        
        final RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, benchProvider, handProvider, null, bfp);
        
        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, target, "evosoda-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.EVOSODA));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "cannot_evolve_further");
    }

    @Test
    void shouldAllowEvosodaOnFirstTurnIfTargetHasAdaptiveEvolution() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.GRASS, null, null, false);
        target.setName("Caterpie");
        target.setEvolutionStage(EvolutionStage.BASIC);
        target.setAbilities(List.of(new ar.edu.utn.frc.tup.piii.engine.model.Ability(
                "Adaptive Evolution", "", ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.ADAPTIVE_EVOLUTION)));
        turnInPlayProvider.set(target, 0);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        when(turnManager.isFirstTurnOfPlayer(0)).thenReturn(true);
        
        final BattlefieldStateProvider bfp = Mockito.mock(BattlefieldStateProvider.class);
        when(bfp.getActivePokemon(0)).thenReturn(target);
        
        final RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, benchProvider, handProvider, null, bfp);
        
        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, target, "evosoda-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.EVOSODA));
        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnInvalidWhenMaxReviveHasNoBasicPokemonInDiscard() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        
        // Mock BenchStateProvider that also implements DiscardPileStateProvider
        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class, Mockito.withSettings().extraInterfaces(
                ar.edu.utn.frc.tup.piii.engine.listener.DiscardPileStateProvider.class));
        when(((ar.edu.utn.frc.tup.piii.engine.listener.DiscardPileStateProvider) mockBench).getDiscardPile(0)).thenReturn(List.of());
        
        final RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider);
        
        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "max-revive-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.MAX_REVIVE));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "no_basic_pokemon_in_discard_pile");
    }

    @Test
    void shouldReturnInvalidWhenGreatBallDeckIsEmpty() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        
        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class, Mockito.withSettings().extraInterfaces(
                ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider.class));
        when(((ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider) mockBench).getDeckSize(0)).thenReturn(0);
        
        final RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider);
        
        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "great-ball-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.GREAT_BALL));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "deck_is_empty");
    }

    @Test
    void shouldReturnInvalidWhenTeamFlareGruntOpponentHasNoActive() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        when(turnManager.activePlayerIndex()).thenReturn(0);
        
        final BattlefieldStateProvider bfp = Mockito.mock(BattlefieldStateProvider.class);
        when(bfp.getActivePokemon(1)).thenReturn(null); // opponent active is null
        
        final RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, benchProvider, handProvider, null, bfp);
        
        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, null, "tf-grunt-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.TEAM_FLARE_GRUNT));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "opponent_has_no_active_pokemon");
    }

    @Test
    void shouldReturnInvalidWhenRedCardOpponentHandIsEmpty() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        when(turnManager.activePlayerIndex()).thenReturn(0);
        
        final HandStateProvider mockHand = Mockito.mock(HandStateProvider.class);
        when(mockHand.getHandSize(1)).thenReturn(0);
        
        final RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, benchProvider, mockHand);
        
        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "red-card-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.RED_CARD));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "opponent_hand_is_empty");
    }

    @Test
    void shouldReturnInvalidWhenLysandreOpponentBenchIsEmpty() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        when(turnManager.activePlayerIndex()).thenReturn(0);
        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class);
        when(mockBench.getBenchSize(1)).thenReturn(0);
        final RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider);
        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, null, "lysandre-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.LYSANDRE));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "opponent_bench_empty");
    }

    @Test
    void shouldReturnInvalidWhenLysandreTargetIsNull() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        when(turnManager.activePlayerIndex()).thenReturn(0);
        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class);
        when(mockBench.getBenchSize(1)).thenReturn(2);
        final RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider);
        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, null, "lysandre-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.LYSANDRE));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_pokemon_required");
    }

    @Test
    void shouldReturnInvalidWhenLysandreTargetNotOnOpponentBench() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        when(turnManager.activePlayerIndex()).thenReturn(0);
        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class);
        when(mockBench.getBenchSize(1)).thenReturn(1);
        final FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        when(mockBench.getBenchedPokemon(1)).thenReturn(List.of());
        final RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider);
        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, target, "lysandre-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.LYSANDRE));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_must_be_on_opponent_bench");
    }

    @Test
    void shouldReturnInvalidWhenSacredAshNoPokemonInDiscard() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class, Mockito.withSettings().extraInterfaces(
                ar.edu.utn.frc.tup.piii.engine.listener.DiscardPileStateProvider.class));
        when(((ar.edu.utn.frc.tup.piii.engine.listener.DiscardPileStateProvider) mockBench).getDiscardPile(0)).thenReturn(List.of());
        final RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider);
        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "sacred-ash-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.SACRED_ASH));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "no_pokemon_in_discard_pile");
    }

    @Test
    void shouldReturnInvalidWhenFieryTorchNoFireEnergyInHand() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class, Mockito.withSettings().extraInterfaces(
                ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider.class));
        when(((ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider) mockBench).getDeckSize(0)).thenReturn(10);
        final HandStateProvider mockHand = Mockito.mock(HandStateProvider.class);
        when(mockHand.getHandCards(0)).thenReturn(List.of(new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard(
                "water-energy-id", "Water Energy", ar.edu.utn.frc.tup.piii.engine.model.PokemonType.WATER, false)));
        final RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, mockHand);
        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "fiery-torch-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FIERY_TORCH));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "fire_energy_required_in_hand");
    }

    @Test
    void shouldReturnInvalidWhenPalPadNoSupporterInDiscard() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class, Mockito.withSettings().extraInterfaces(
                ar.edu.utn.frc.tup.piii.engine.listener.DiscardPileStateProvider.class));
        when(((ar.edu.utn.frc.tup.piii.engine.listener.DiscardPileStateProvider) mockBench).getDiscardPile(0)).thenReturn(List.of());
        final RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider);
        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "pal-pad-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.PAL_PAD));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "no_supporter_in_discard_pile");
    }

    @Test
    void shouldReturnInvalidWhenBlacksmithNoTarget() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        ValidationResult result = validator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, null, "blacksmith-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BLACKSMITH));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_pokemon_required");
    }

    @Test
    void shouldReturnInvalidWhenBlacksmithTargetNotFire() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        final FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.WATER, null, null, false);
        ValidationResult result = validator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, target, "blacksmith-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BLACKSMITH));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_must_be_fire_pokemon");
    }

    @Test
    void shouldReturnInvalidWhenPokemonCenterLadyNoTarget() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        ValidationResult result = validator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, null, "pcl-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.POKEMON_CENTER_LADY));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_pokemon_required");
    }

    @Test
    void shouldReturnInvalidWhenPokemonCenterLadyNoDamageOrStatus() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        final FakeBattlePokemonState target = new FakeBattlePokemonState(0, PokemonType.FIRE, null, null, false);
        final BattlefieldStateProvider bfp = Mockito.mock(BattlefieldStateProvider.class);
        when(bfp.getActivePokemon(0)).thenReturn(target);
        when(statusEffectManager.activeEffects()).thenReturn(java.util.Set.of());
        final RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, benchProvider, handProvider, null, bfp);
        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, target, "pcl-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.POKEMON_CENTER_LADY));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_has_no_damage_or_status");
    }

    @Test
    void shouldReturnInvalidWhenUltraBallHandLessThan3() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        final HandStateProvider mockHand = Mockito.mock(HandStateProvider.class);
        when(mockHand.getHandSize(0)).thenReturn(2);
        final RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, benchProvider, mockHand);
        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "ultra-ball-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.ULTRA_BALL));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "insufficient_cards_in_hand");
    }

    @Test
    void shouldValidateSelectCardsExactCountEnforcement() {
        when(turnManager.currentPhase()).thenReturn(Mockito.mock(ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase.class));

        // Sacred Ash: requires exactly maxSelections (e.g. 5)
        ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest sacredAshRequest =
                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.SACRED_ASH, null, 5,
                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DISCARD_PILE);
        
        // Under-selection
        ValidationResult result = validator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("c1", "c2", "c3"), sacredAshRequest), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "must_select_exact_amount");

        // Exact selection
        result = validator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("c1", "c2", "c3", "c4", "c5"), sacredAshRequest), 0);
        assertInstanceOf(ValidationResult.Valid.class, result);

        // Over-selection
        result = validator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("c1", "c2", "c3", "c4", "c5", "c6"), sacredAshRequest), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "too_many_cards_selected");

        // Ultra Ball (Hand): requires exactly 2 cards
        ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest ultraBallRequest =
                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.ULTRA_BALL, null, 2,
                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND);

        // Under-selection
        result = validator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("c1"), ultraBallRequest), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "must_select_exact_amount");

        // Exact selection
        result = validator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("c1", "c2"), ultraBallRequest), 0);
        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void testRuleValidatorBlocksAttackWhenSelfDisabledNextTurn() {
        FakeBattlePokemonState activeAttacker = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        Attack attack = new Attack("Slash", 20, List.of(PokemonType.COLORLESS));
        when(statusEffectManager.isSelfDisabledNextTurn()).thenReturn(true);
        when(statusEffectManager.canAttack()).thenReturn(true);

        ValidationResult result = validator.validate(new DeclareAttackAction(activeAttacker, attack));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "attack_disabled_by_effect");
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private void assertInvalidReason(final ValidationResult result, final String expectedReason) {
        org.junit.jupiter.api.Assertions.assertEquals(expectedReason,
                ((ValidationResult.Invalid) result).reason());
    }
}
