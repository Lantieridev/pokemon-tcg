package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.PokemonTurnInPlayProvider;
import ar.edu.utn.frc.tup.piii.engine.model.Action;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.AttachEnergyAction;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.DeclareAttackAction;
import ar.edu.utn.frc.tup.piii.engine.model.EndTurnAction;
import ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage;
import ar.edu.utn.frc.tup.piii.engine.model.EvolveAction;
import ar.edu.utn.frc.tup.piii.engine.model.MainPhase;
import ar.edu.utn.frc.tup.piii.engine.model.PlaceBasicPokemonAction;
import ar.edu.utn.frc.tup.piii.engine.model.PlayTrainerAction;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.RetreatAction;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.model.ValidationResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Pure-read rule validator for player actions. Returns a {@link ValidationResult}
 * for every well-formed {@link Action} without throwing or mutating any state.
 * FR-009 through FR-015.
 */
public final class RuleValidator {

    private static final String CANNOT_EVOLVE_FIRST_TURN = "cannot_evolve_first_turn";
    private static final String POKEMON_ENTERED_THIS_TURN = "pokemon_entered_this_turn";
    private static final String INVALID_EVOLUTION_STAGE = "invalid_evolution_stage";
    private static final String WRONG_EVOLUTION_TARGET = "wrong_evolution_target";
    private static final String RETREAT_BLOCKED_BY_STATUS = "retreat_blocked_by_status";
    private static final String RETREAT_ALREADY_USED = "retreat_already_used";
    private static final String EMPTY_BENCH_FOR_RETREAT = "empty_bench_for_retreat";
    private static final String INSUFFICIENT_ENERGY_FOR_RETREAT = "insufficient_energy_for_retreat";
    private static final String SUPPORTER_ALREADY_PLAYED = "supporter_already_played";
    private static final String STADIUM_ALREADY_PLAYED = "stadium_already_played";
    private static final String POKEMON_TOOL_ALREADY_ATTACHED = "pokemon_tool_already_attached";
    private static final String POKEMON_TOOL_REQUIRES_TARGET = "pokemon_tool_requires_target";
    private static final String ENERGY_ALREADY_ATTACHED = "energy_already_attached";
    private static final String ATTACK_BLOCKED_BY_STATUS = "attack_blocked_by_status";
    private static final String INSUFFICIENT_ENERGY_FOR_ATTACK = "insufficient_energy_for_attack";
    private static final String CANNOT_ATTACK_FIRST_TURN = "cannot_attack_first_turn";
    private static final int MIN_TURNS_TO_EVOLVE = 1;
    private static final int MAX_ENERGY_PER_TURN = 1;

    private final TurnManager turnManager;
    private final List<StatusEffectManager> statusEffectManagers;
    private final PokemonTurnInPlayProvider turnInPlayProvider;
    private final BenchStateProvider benchStateProvider;

    /**
     * Multi-player constructor. Accepts one {@link StatusEffectManager} per player so that
     * the validator always reads the active player's status state.
     *
     * @param turnManager          manages turn phases and player state (must not be null)
     * @param statusEffectManagers one SEM per player, indexed by player index (must not be null)
     * @param turnInPlayProvider   reports how many turns a Pokémon has been in play (must not be null)
     * @param benchStateProvider   provides bench size per player (must not be null)
     */
    public RuleValidator(final TurnManager turnManager,
                         final List<StatusEffectManager> statusEffectManagers,
                         final PokemonTurnInPlayProvider turnInPlayProvider,
                         final BenchStateProvider benchStateProvider) {
        this.turnManager = Objects.requireNonNull(turnManager, "turnManager");
        this.statusEffectManagers = List.copyOf(
                Objects.requireNonNull(statusEffectManagers, "statusEffectManagers"));
        this.turnInPlayProvider = Objects.requireNonNull(turnInPlayProvider, "turnInPlayProvider");
        this.benchStateProvider = Objects.requireNonNull(benchStateProvider, "benchStateProvider");
    }

    /**
     * Single-player backward-compatible constructor (used in tests and legacy callers).
     *
     * @param turnManager         manages turn phases and player state (must not be null)
     * @param statusEffectManager manages active status effects on the current Pokémon (must not be null)
     * @param turnInPlayProvider  reports how many turns a Pokémon has been in play (must not be null)
     * @param benchStateProvider  provides bench size per player (must not be null)
     */
    public RuleValidator(final TurnManager turnManager,
                         final StatusEffectManager statusEffectManager,
                         final PokemonTurnInPlayProvider turnInPlayProvider,
                         final BenchStateProvider benchStateProvider) {
        this(turnManager, List.of(statusEffectManager), turnInPlayProvider, benchStateProvider);
    }

    /**
     * Returns the turn manager associated with this validator.
     *
     * @return the TurnManager instance (never null)
     */
    public TurnManager getTurnManager() {
        return turnManager;
    }

    /**
     * Validates the given action against the current game state rules.
     *
     * @param action the action to validate (must not be null)
     * @return a non-null {@link ValidationResult}
     */
    public ValidationResult validate(final Action action) {
        return switch (action) {
            case EvolveAction a             -> validateEvolve(a);
            case RetreatAction a            -> validateRetreat(a);
            case PlayTrainerAction a        -> validatePlayTrainer(a);
            case AttachEnergyAction a       -> validateAttachEnergy(a);
            case DeclareAttackAction a      -> validateDeclareAttack(a);
            case PlaceBasicPokemonAction a  -> validatePlaceBasicPokemon();
            case UseAbilityAction a         -> validateUseAbility();
            case EndTurnAction a            -> new ValidationResult.Valid();
        };
    }

    private ValidationResult validateEvolve(final EvolveAction action) {
        if (action.target() == null) {
            return new ValidationResult.Invalid("target_pokemon_required");
        }
        final int playerIndex = turnManager.activePlayerIndex();
        if (turnManager.isFirstTurnOfPlayer(playerIndex)) {
            return new ValidationResult.Invalid(CANNOT_EVOLVE_FIRST_TURN);
        }
        if (turnInPlayProvider.getTurnsInPlay(action.target()) < MIN_TURNS_TO_EVOLVE) {
            return new ValidationResult.Invalid(POKEMON_ENTERED_THIS_TURN);
        }
        if (action.evolution() != null) {
            final ValidationResult stageResult = validateEvolutionStage(action);
            if (stageResult instanceof ValidationResult.Invalid) {
                return stageResult;
            }
        }
        return new ValidationResult.Valid();
    }

    private ValidationResult validateEvolutionStage(final EvolveAction action) {
        final EvolutionStage targetStage = action.target().getEvolutionStage();
        final EvolutionStage evolutionStage = action.evolution().getEvolutionStage();

        final boolean validProgression =
                (targetStage == EvolutionStage.BASIC     && evolutionStage == EvolutionStage.STAGE_1)
             || (targetStage == EvolutionStage.STAGE_1   && evolutionStage == EvolutionStage.STAGE_2);

        if (!validProgression) {
            return new ValidationResult.Invalid(INVALID_EVOLUTION_STAGE);
        }

        final String evolvesFrom = action.evolution().getEvolvesFrom();
        if (evolvesFrom != null && !evolvesFrom.equals(action.target().getName())) {
            return new ValidationResult.Invalid(WRONG_EVOLUTION_TARGET);
        }

        return new ValidationResult.Valid();
    }

    private ValidationResult validateRetreat(final RetreatAction action) {
        if (!getActiveStatusEffectManager().canRetreat()) {
            return new ValidationResult.Invalid(RETREAT_BLOCKED_BY_STATUS);
        }
        if (benchStateProvider.getBenchSize(turnManager.activePlayerIndex()) == 0) {
            return new ValidationResult.Invalid(EMPTY_BENCH_FOR_RETREAT);
        }
        MainPhase mainPhase = turnManager.requireMainPhase();
        if (mainPhase.isRetreatUsed()) {
            return new ValidationResult.Invalid(RETREAT_ALREADY_USED);
        }
        if (action.active().getAttachedEnergies().size() < action.active().getRetreatCost()) {
            return new ValidationResult.Invalid(INSUFFICIENT_ENERGY_FOR_RETREAT);
        }
        return new ValidationResult.Valid();
    }

    private ValidationResult validatePlayTrainer(final PlayTrainerAction action) {
        MainPhase mainPhase = turnManager.requireMainPhase();
        return switch (action.trainerType()) {
            case SUPPORTER    -> validateSupporter(mainPhase);
            case STADIUM      -> validateStadium(mainPhase);
            case ITEM         -> new ValidationResult.Valid();
            case POKEMON_TOOL -> validatePokemonTool(action.target());
        };
    }

    private ValidationResult validateSupporter(final MainPhase mainPhase) {
        if (mainPhase.isSupporterPlayed()) {
            return new ValidationResult.Invalid(SUPPORTER_ALREADY_PLAYED);
        }
        return new ValidationResult.Valid();
    }

    private ValidationResult validateStadium(final MainPhase mainPhase) {
        if (mainPhase.isStadiumPlayed()) {
            return new ValidationResult.Invalid(STADIUM_ALREADY_PLAYED);
        }
        return new ValidationResult.Valid();
    }

    private ValidationResult validatePokemonTool(final BattlePokemonState target) {
        if (target == null) {
            return new ValidationResult.Invalid(POKEMON_TOOL_REQUIRES_TARGET);
        }
        if (target.hasToolAttached()) {
            return new ValidationResult.Invalid(POKEMON_TOOL_ALREADY_ATTACHED);
        }
        return new ValidationResult.Valid();
    }

    private ValidationResult validateAttachEnergy(final AttachEnergyAction action) {
        if (action.target() == null) {
            return new ValidationResult.Invalid("target_pokemon_required");
        }
        MainPhase mainPhase = turnManager.requireMainPhase();
        if (mainPhase.getEnergyAttached() >= MAX_ENERGY_PER_TURN) {
            return new ValidationResult.Invalid(ENERGY_ALREADY_ATTACHED);
        }
        return new ValidationResult.Valid();
    }

    private ValidationResult validateDeclareAttack(final DeclareAttackAction action) {
        final int activePlayerIndex = turnManager.activePlayerIndex();
        if (turnManager.getStartingPlayerIndex() == activePlayerIndex && turnManager.isFirstTurnOfPlayer(activePlayerIndex)) {
            return new ValidationResult.Invalid(CANNOT_ATTACK_FIRST_TURN);
        }
        if (!getActiveStatusEffectManager().canAttack()) {
            return new ValidationResult.Invalid(ATTACK_BLOCKED_BY_STATUS);
        }
        if (!hasEnoughEnergyForAttack(action.attacker(), action.attack())) {
            return new ValidationResult.Invalid(INSUFFICIENT_ENERGY_FOR_ATTACK);
        }
        return new ValidationResult.Valid();
    }

    private ValidationResult validatePlaceBasicPokemon() {
        turnManager.requireMainPhase();
        return new ValidationResult.Valid();
    }

    private ValidationResult validateUseAbility() {
        turnManager.requireMainPhase();
        return new ValidationResult.Valid();
    }

    /**
     * Returns the StatusEffectManager for the currently active player.
     * Falls back to the first manager if only one is registered (legacy / test usage).
     *
     * @return the active player's SEM (never null)
     */
    private StatusEffectManager getActiveStatusEffectManager() {
        final int idx = turnManager.activePlayerIndex();
        if (idx < 0 || idx >= statusEffectManagers.size()) {
            return statusEffectManagers.get(0);
        }
        return statusEffectManagers.get(idx);
    }

    private boolean hasEnoughEnergyForAttack(final BattlePokemonState attacker, final Attack attack) {
        Map<PokemonType, Long> available = new HashMap<>(
                attacker.getAttachedEnergies().stream()
                        .collect(Collectors.groupingBy(e -> e, Collectors.counting())));
        int colorlessRequired = 0;
        for (PokemonType required : attack.requiredEnergies()) {
            if (required == PokemonType.COLORLESS) {
                colorlessRequired++;
                continue;
            }
            long count = available.getOrDefault(required, 0L);
            if (count == 0) {
                return false;
            }
            available.put(required, count - 1);
        }
        long remaining = available.values().stream().mapToLong(Long::longValue).sum();
        return remaining >= colorlessRequired;
    }
}
