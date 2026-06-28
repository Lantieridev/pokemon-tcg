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
    void shouldReturnInvalidWhenStadiumSameNameIsActive() {
        final TrainerCard activeStadium = new TrainerCard.Builder("xy2-91", "Magnetic Storm", TrainerType.STADIUM).build();
        final RuleValidator sameNameValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, benchProvider, handProvider,
                () -> activeStadium);

        final TrainerCard playedStadium = new TrainerCard.Builder("xy2-91-copy", "Magnetic Storm", TrainerType.STADIUM).build();
        handProvider.addCard(0, playedStadium);

        ar.edu.utn.frc.tup.piii.engine.model.MainPhase mainPhase =
                new ar.edu.utn.frc.tup.piii.engine.model.MainPhase();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        ValidationResult result = sameNameValidator.validate(
                new PlayTrainerAction(TrainerType.STADIUM, null, "xy2-91-copy", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.MAGNETIC_STORM), 0);

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "stadium_same_name_in_play");
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

    // --- Additional Trainer Effect Validation Tests ---
    
    @Test
    void shouldReturnInvalidWhenEvosodaTargetIsNull() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        ValidationResult result = validator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "evosoda", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.EVOSODA));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_pokemon_required");
    }

    @Test
    void shouldReturnInvalidWhenMaxReviveAndNoBasicInDiscard() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        ValidationResult result = validator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "max-revive", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.MAX_REVIVE));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "no_basic_pokemon_in_discard_pile");
    }

    @Test
    void shouldReturnInvalidWhenRedCardAndOpponentHandEmpty() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        when(turnManager.activePlayerIndex()).thenReturn(0);
        ValidationResult result = validator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "red-card", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.RED_CARD));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "opponent_hand_is_empty");
    }

    @Test
    void shouldReturnInvalidWhenSacredAshAndNoPokemonInDiscard() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        ValidationResult result = validator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "sacred-ash", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.SACRED_ASH));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "no_pokemon_in_discard_pile");
    }

    // ─── Additional RuleValidator Tests (for 90%+ Coverage) ───────────────────

    @Test
    void shouldValidatePromoteActiveBenchIndexValidation() {
        // Bench size = 2
        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class);
        when(mockBench.getBenchSize(0)).thenReturn(2);
        
        final RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider);
        
        // Invalid: negative index
        ValidationResult result = localValidator.validate(new ar.edu.utn.frc.tup.piii.engine.model.PromoteActiveAction(-1), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "invalid_bench_index");

        // Invalid: index out of bounds (index >= bench size)
        result = localValidator.validate(new ar.edu.utn.frc.tup.piii.engine.model.PromoteActiveAction(2), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "bench_index_out_of_bounds");

        // Valid: index < bench size
        result = localValidator.validate(new ar.edu.utn.frc.tup.piii.engine.model.PromoteActiveAction(1), 0);
        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldValidateStartlingMegaphoneToolChecks() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        when(turnManager.activePlayerIndex()).thenReturn(0);

        final BattlefieldStateProvider bfp = Mockito.mock(BattlefieldStateProvider.class);
        
        // 1. Opponent has no tools at all
        FakeBattlePokemonState oppActive = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        when(bfp.getActivePokemon(1)).thenReturn(oppActive);
        
        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class);
        when(mockBench.getBenchedPokemon(1)).thenReturn(List.of());

        RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider, null, bfp);

        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "megaphone-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.STARTLING_MEGAPHONE));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "opponent_has_no_tools_attached");

        // 2. Opponent has tool on active
        oppActive.attachTool(new ar.edu.utn.frc.tup.piii.engine.model.TrainerCard.Builder("tool-id", "Muscle Band", TrainerType.POKEMON_TOOL).build());
        result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "megaphone-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.STARTLING_MEGAPHONE));
        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldValidatePalPadSupporterChecks() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        when(turnManager.activePlayerIndex()).thenReturn(0);

        // Discard has no supporter
        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class, Mockito.withSettings().extraInterfaces(
                ar.edu.utn.frc.tup.piii.engine.listener.DiscardPileStateProvider.class));
        when(((ar.edu.utn.frc.tup.piii.engine.listener.DiscardPileStateProvider) mockBench).getDiscardPile(0)).thenReturn(List.of());

        RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider);

        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "palpad-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.PAL_PAD));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "no_supporter_in_discard_pile");

        // Discard has supporter
        TrainerCard supporter = new TrainerCard.Builder("supp-id", "Professor Sycamore", TrainerType.SUPPORTER).build();
        when(((ar.edu.utn.frc.tup.piii.engine.listener.DiscardPileStateProvider) mockBench).getDiscardPile(0)).thenReturn(List.of(supporter));
        result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "palpad-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.PAL_PAD));
        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldValidateBlacksmithRules() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        when(turnManager.activePlayerIndex()).thenReturn(0);

        // 1. Target is null
        ValidationResult result = validator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, null, "blacksmith-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BLACKSMITH));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_pokemon_required");

        // 2. Target is not FIRE
        FakeBattlePokemonState waterPokemon = new FakeBattlePokemonState(HP, PokemonType.WATER, null, null, false);
        result = validator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, waterPokemon, "blacksmith-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BLACKSMITH));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_must_be_fire_pokemon");

        // 3. Target is FIRE but no Fire energy in discard
        FakeBattlePokemonState firePokemon = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class, Mockito.withSettings().extraInterfaces(
                ar.edu.utn.frc.tup.piii.engine.listener.DiscardPileStateProvider.class));
        when(((ar.edu.utn.frc.tup.piii.engine.listener.DiscardPileStateProvider) mockBench).getDiscardPile(0)).thenReturn(List.of());

        RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider);
        result = localValidator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, firePokemon, "blacksmith-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BLACKSMITH));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "fire_energy_required_in_discard");

        // 4. Target is FIRE and Fire energy is in discard
        EnergyCard fireEnergy = new EnergyCard("energy-id", "Fire Energy", PokemonType.FIRE, true);
        when(((ar.edu.utn.frc.tup.piii.engine.listener.DiscardPileStateProvider) mockBench).getDiscardPile(0)).thenReturn(List.of(fireEnergy));
        result = localValidator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, firePokemon, "blacksmith-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BLACKSMITH));
        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldValidatePokemonCenterLadyRules() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        when(turnManager.activePlayerIndex()).thenReturn(0);

        // 1. Target null
        ValidationResult result = validator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, null, "pcl-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.POKEMON_CENTER_LADY));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_pokemon_required");

        // 2. Target has no damage and no status conditions
        FakeBattlePokemonState healthyPokemon = new FakeBattlePokemonState(HP, PokemonType.COLORLESS, null, null, false);
        healthyPokemon.setDamageCounters(0);
        
        final BattlefieldStateProvider bfp = Mockito.mock(BattlefieldStateProvider.class);
        when(bfp.getActivePokemon(0)).thenReturn(healthyPokemon);
        when(statusEffectManager.activeEffects()).thenReturn(java.util.Collections.emptySet());

        RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, benchProvider, handProvider, null, bfp);

        result = localValidator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, healthyPokemon, "pcl-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.POKEMON_CENTER_LADY));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_has_no_damage_or_status");

        // 3. Target has damage
        healthyPokemon.setDamageCounters(2);
        result = localValidator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, healthyPokemon, "pcl-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.POKEMON_CENTER_LADY));
        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldValidateBuriedTreasureHuntSelectCardsCount() {
        when(turnManager.currentPhase()).thenReturn(Mockito.mock(ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase.class));

        // Buried Treasure Hunt request
        ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest bthRequest =
                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BURIED_TREASURE_HUNT, null, 2,
                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK);

        // Deck size is 5, expecting 2 selections
        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class, Mockito.withSettings().extraInterfaces(
                ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider.class));
        when(((ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider) mockBench).getDeckSize(0)).thenReturn(5);

        RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider);

        // Under-selected (expected 2, got 1)
        ValidationResult result = localValidator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("c1"), bthRequest), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "must_select_exact_amount");

        // Exact selection (2)
        result = localValidator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("c1", "c2"), bthRequest), 0);
        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldValidateDualBulletSelectCardsCount() {
        when(turnManager.currentPhase()).thenReturn(Mockito.mock(ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase.class));
        when(turnManager.activePlayerIndex()).thenReturn(0);

        // Dual Bullet request
        ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest dbRequest =
                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.DUAL_BULLET, null, 2,
                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.OPPONENT_FIELD);

        // Opponent has 1 active and 1 benched Pokemon (total 2)
        final BattlefieldStateProvider bfp = Mockito.mock(BattlefieldStateProvider.class);
        FakeBattlePokemonState activePokemon = new FakeBattlePokemonState(HP, PokemonType.COLORLESS, null, null, false);
        when(bfp.getActivePokemon(1)).thenReturn(activePokemon);

        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class);
        when(mockBench.getBenchSize(1)).thenReturn(1);

        RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider, null, bfp);

        // Under-selected (expected 2, got 1)
        ValidationResult result = localValidator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("c1"), dbRequest), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "must_select_exact_amount");

        // Exact selection (2)
        result = localValidator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("c1", "c2"), dbRequest), 0);
        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldValidateEarInfluenceSelectCardsCount() {
        when(turnManager.currentPhase()).thenReturn(Mockito.mock(ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase.class));

        // Ear Influence request: maximum 4 selections
        ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest eiRequest =
                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.EAR_INFLUENCE, null, 4,
                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.OPPONENT_FIELD);

        // Odd amount selected (3) -> invalid
        ValidationResult result = validator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("c1", "c2", "c3"), eiRequest), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "must_select_pairs");

        // Even amount selected (2) -> valid
        result = validator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("c1", "c2"), eiRequest), 0);
        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldValidateFangSnipeSelectCardsCountAndTrainerRules() {
        when(turnManager.currentPhase()).thenReturn(Mockito.mock(ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase.class));
        when(turnManager.activePlayerIndex()).thenReturn(0);

        // Fang Snipe request: max 1 card from hand
        ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest fsRequest =
                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FANG_SNIPE, null, 1,
                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND);

        // 1. Opponent hand has NO trainers (so we expect 0 selections)
        final HandStateProvider mockHand = Mockito.mock(HandStateProvider.class);
        EnergyCard energy = new EnergyCard("energy-id", "Water Energy", PokemonType.WATER, true);
        when(mockHand.getHandCards(1)).thenReturn(List.of(energy));

        RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, benchProvider, mockHand);

        // Selected 1 but expected 0 -> invalid
        ValidationResult result = localValidator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("energy-id"), fsRequest), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "must_select_exact_amount");

        // Selected 0 -> valid
        result = localValidator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of(), fsRequest), 0);
        assertInstanceOf(ValidationResult.Valid.class, result);

        // 2. Opponent hand has a trainer (Professor Letter)
        TrainerCard trainer = new TrainerCard.Builder("trainer-id", "Professor Letter", TrainerType.ITEM).build();
        when(mockHand.getHandCards(1)).thenReturn(List.of(trainer));
        when(mockHand.getCardInHand(1, "trainer-id")).thenReturn(java.util.Optional.of(trainer));
        when(mockHand.getCardInHand(1, "energy-id")).thenReturn(java.util.Optional.of(energy));

        // Selected 0 but expected 1 -> invalid
        result = localValidator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of(), fsRequest), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "must_select_exact_amount");

        // Selected non-trainer card (energy) -> invalid
        result = localValidator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("energy-id"), fsRequest), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "must_select_trainer_card");

        // Selected trainer card -> valid
        result = localValidator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("trainer-id"), fsRequest), 0);
        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldValidatePoisonBarrierBlockingRetreat() {
        FakeBattlePokemonState active = new FakeBattlePokemonState(HP, PokemonType.COLORLESS, null, null, false);
        when(statusEffectManager.canRetreat()).thenReturn(true);
        when(statusEffectManager.has(ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType.ENVENENADO)).thenReturn(true);
        
        final BattlefieldStateProvider bfp = Mockito.mock(BattlefieldStateProvider.class);
        
        // Opponent active has POISON_BARRIER
        FakeBattlePokemonState oppActive = new FakeBattlePokemonState(HP, PokemonType.GRASS, null, null, false);
        oppActive.setAbilities(List.of(new ar.edu.utn.frc.tup.piii.engine.model.Ability(
                "Poison Barrier", "", ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.POISON_BARRIER)));
        when(bfp.getActivePokemon(1)).thenReturn(oppActive);

        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class);
        when(mockBench.getBenchSize(0)).thenReturn(1);
        when(mockBench.getBenchedPokemon(1)).thenReturn(List.of());

        RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider, null, bfp);

        ValidationResult result = localValidator.validate(new RetreatAction(active), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "retreat_blocked_by_poison_barrier");
    }

    @Test
    void shouldValidateShadowVoidAbilityRules() {
        FakeBattlePokemonState source = new FakeBattlePokemonState(HP, PokemonType.PSYCHIC, null, null, false);
        source.setDamageCounters(0);
        source.setAbilities(List.of(new ar.edu.utn.frc.tup.piii.engine.model.Ability(
                "Shadow Void", "", ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.SHADOW_VOID)));
        
        final BattlefieldStateProvider bfp = Mockito.mock(BattlefieldStateProvider.class);
        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class);

        RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider, null, bfp);

        // 1. targetIndex is null
        ValidationResult result = localValidator.validate(new ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction(
                source, "Shadow Void", null, null, null), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_pokemon_required");

        // 2. target Pokemon is null
        result = localValidator.validate(new ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction(
                source, "Shadow Void", null, 0, null), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_pokemon_required");

        // 3. target has no damage
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.COLORLESS, null, null, false);
        target.setDamageCounters(0);
        when(bfp.getActivePokemon(0)).thenReturn(target);
        result = localValidator.validate(new ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction(
                source, "Shadow Void", null, -1, null), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "target_has_no_damage");

        // 4. Dusknoir max HP reached (damage * 10 + 10 >= maxHp)
        target.setDamageCounters(2);
        source.setDamageCounters(9); // 90 damage + 10 = 100 >= 100 max HP
        result = localValidator.validate(new ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction(
                source, "Shadow Void", null, -1, null), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "dusknoir_max_hp_reached");
    }

    @Test
    void shouldValidateVariousSelectCardsActions() {
        when(turnManager.currentPhase()).thenReturn(Mockito.mock(ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase.class));

        // 1. Pain Pellets (expected 1)
        ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest ppRequest =
                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.PAIN_PELLETS, null, 1,
                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.OPPONENT_FIELD);
        
        ValidationResult result = validator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of(), ppRequest), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "must_select_exact_amount");

        // 2. Bench Damage One (expected 1)
        ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest bdRequest =
                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BENCH_DAMAGE_ONE, null, 1,
                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.BENCH);
        
        result = validator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of(), bdRequest), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "must_select_exact_amount");

        // 3. Parabolic Charge (up to maxSelections)
        ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest pcRequest =
                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.PARABOLIC_CHARGE, null, 2,
                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DISCARD_PILE);
        
        // Over-selected (3 > 2) -> invalid
        result = validator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("c1", "c2", "c3"), pcRequest), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "too_many_cards_selected");

        // Selected 1 (<= 2) -> valid
        result = validator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("c1"), pcRequest), 0);
        assertInstanceOf(ValidationResult.Valid.class, result);

        // 4. Bounce (expected 1)
        ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest bounceRequest =
                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BOUNCE, null, 1,
                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.OPPONENT_FIELD);
        
        result = validator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of(), bounceRequest), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "must_select_exact_amount");

        // 5. Rescue (expected exact maxSelections)
        ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest rescueRequest =
                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                        ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.RESCUE, null, 1,
                        ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DISCARD_PILE);
        
        result = validator.validate(
                new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of(), rescueRequest), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "must_select_exact_amount");
    }

    @Test
    void shouldValidatePoisonBarrierOnBenchBlockingRetreat() {
        FakeBattlePokemonState active = new FakeBattlePokemonState(HP, PokemonType.COLORLESS, null, null, false);
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        when(statusEffectManager.canRetreat()).thenReturn(true);
        when(statusEffectManager.has(ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType.ENVENENADO)).thenReturn(true);
        
        final BattlefieldStateProvider bfp = Mockito.mock(BattlefieldStateProvider.class);
        
        // Opponent active has NO abilities
        FakeBattlePokemonState oppActive = new FakeBattlePokemonState(HP, PokemonType.GRASS, null, null, false);
        when(bfp.getActivePokemon(1)).thenReturn(oppActive);

        // Opponent bench has POISON_BARRIER
        FakeBattlePokemonState oppBenched = new FakeBattlePokemonState(HP, PokemonType.GRASS, null, null, false);
        oppBenched.setAbilities(List.of(new ar.edu.utn.frc.tup.piii.engine.model.Ability(
                "Poison Barrier", "", ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.POISON_BARRIER)));

        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class);
        when(mockBench.getBenchSize(0)).thenReturn(1);
        when(mockBench.getBenchedPokemon(1)).thenReturn(List.of(oppBenched));

        RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider, null, bfp);

        ValidationResult result = localValidator.validate(new RetreatAction(active), 0);
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "retreat_blocked_by_poison_barrier");
    }

    @Test
    void shouldAllowRetreatWhenNoPoisonBarrier() {
        FakeBattlePokemonState active = new FakeBattlePokemonState(HP, PokemonType.COLORLESS, null, null, false);
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        when(statusEffectManager.canRetreat()).thenReturn(true);
        when(statusEffectManager.has(ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType.ENVENENADO)).thenReturn(true);
        
        final BattlefieldStateProvider bfp = Mockito.mock(BattlefieldStateProvider.class);
        
        // Opponent active has NO abilities
        FakeBattlePokemonState oppActive = new FakeBattlePokemonState(HP, PokemonType.GRASS, null, null, false);
        when(bfp.getActivePokemon(1)).thenReturn(oppActive);

        // Opponent bench has NO abilities
        FakeBattlePokemonState oppBenched = new FakeBattlePokemonState(HP, PokemonType.GRASS, null, null, false);

        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class);
        when(mockBench.getBenchSize(0)).thenReturn(1);
        when(mockBench.getBenchedPokemon(1)).thenReturn(List.of(oppBenched));

        RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider, null, bfp);

        // Since no poison barrier, retreat should be valid
        ValidationResult result = localValidator.validate(new RetreatAction(active), 0);
        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldValidatePokemonFanClubRules() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        when(turnManager.activePlayerIndex()).thenReturn(0);

        // Deck is empty
        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class, Mockito.withSettings().extraInterfaces(
                ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider.class));
        when(((ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider) mockBench).getDeckSize(0)).thenReturn(0);

        RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider);

        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, null, "fanclub-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.POKEMON_FAN_CLUB));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "deck_is_empty");

        // Deck is not empty
        when(((ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider) mockBench).getDeckSize(0)).thenReturn(10);
        result = localValidator.validate(new PlayTrainerAction(
                TrainerType.SUPPORTER, null, "fanclub-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.POKEMON_FAN_CLUB));
        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldValidateFieryTorchRules() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        when(turnManager.activePlayerIndex()).thenReturn(0);

        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class, Mockito.withSettings().extraInterfaces(
                ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider.class));
        final HandStateProvider mockHand = Mockito.mock(HandStateProvider.class);

        RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, mockHand);

        // 1. Deck is empty
        when(((ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider) mockBench).getDeckSize(0)).thenReturn(0);
        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "torch-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FIERY_TORCH));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "deck_is_empty");

        // 2. Hand has no Fire energy
        when(((ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider) mockBench).getDeckSize(0)).thenReturn(10);
        EnergyCard waterEnergy = new EnergyCard("water-energy", "Water Energy", PokemonType.WATER, true);
        when(mockHand.getHandCards(0)).thenReturn(List.of(waterEnergy));

        result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "torch-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FIERY_TORCH));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "fire_energy_required_in_hand");

        // 3. Hand has Fire energy (valid)
        EnergyCard fireEnergy = new EnergyCard("fire-energy", "Fire Energy", PokemonType.FIRE, true);
        when(mockHand.getHandCards(0)).thenReturn(List.of(fireEnergy));

        result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "torch-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FIERY_TORCH));
        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldValidateTrickShovelRules() {
        when(turnManager.requireMainPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.MainPhase());
        when(turnManager.activePlayerIndex()).thenReturn(0);

        final BenchStateProvider mockBench = Mockito.mock(BenchStateProvider.class, Mockito.withSettings().extraInterfaces(
                ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider.class));
        
        // Opponent index = 1, deck size = 0
        when(((ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider) mockBench).getDeckSize(1)).thenReturn(0);

        RuleValidator localValidator = new RuleValidator(
                turnManager, List.of(statusEffectManager), turnInPlayProvider, mockBench, handProvider);

        // target is null -> target player is opponent (1) -> deck is empty
        ValidationResult result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "shovel-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.TRICK_SHOVEL));
        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertInvalidReason(result, "deck_is_empty");

        // Deck is not empty
        when(((ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider) mockBench).getDeckSize(1)).thenReturn(5);
        result = localValidator.validate(new PlayTrainerAction(
                TrainerType.ITEM, null, "shovel-id", ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.TRICK_SHOVEL));
        assertInstanceOf(ValidationResult.Valid.class, result);
    }
}
