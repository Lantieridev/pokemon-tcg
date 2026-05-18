package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.listener.PokemonTurnInPlayProvider;
import ar.edu.utn.frc.tup.piii.engine.model.Action;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.AttachEnergyAction;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.DeclareAttackAction;
import ar.edu.utn.frc.tup.piii.engine.model.EvolveAction;
import ar.edu.utn.frc.tup.piii.engine.model.MainPhase;
import ar.edu.utn.frc.tup.piii.engine.model.PlayTrainerAction;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.RetreatAction;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;
import ar.edu.utn.frc.tup.piii.engine.model.ValidationResult;

import java.util.HashMap;
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
    private static final String RETREAT_BLOCKED_BY_STATUS = "retreat_blocked_by_status";
    private static final String RETREAT_ALREADY_USED = "retreat_already_used";
    private static final String INSUFFICIENT_ENERGY_FOR_RETREAT = "insufficient_energy_for_retreat";
    private static final String SUPPORTER_ALREADY_PLAYED = "supporter_already_played";
    private static final String STADIUM_ALREADY_PLAYED = "stadium_already_played";
    private static final String ENERGY_ALREADY_ATTACHED = "energy_already_attached";
    private static final String ATTACK_BLOCKED_BY_STATUS = "attack_blocked_by_status";
    private static final String INSUFFICIENT_ENERGY_FOR_ATTACK = "insufficient_energy_for_attack";
    private static final int MIN_TURNS_TO_EVOLVE = 1;
    private static final int MAX_ENERGY_PER_TURN = 1;

    private final TurnManager turnManager;
    private final StatusEffectManager statusEffectManager;
    private final PokemonTurnInPlayProvider turnInPlayProvider;

    /**
     * Constructs a RuleValidator with the required collaborators.
     *
     * @param turnManager         manages turn phases and player state (must not be null)
     * @param statusEffectManager manages active status effects on the current Pokémon (must not be null)
     * @param turnInPlayProvider  reports how many turns a Pokémon has been in play (must not be null)
     */
    public RuleValidator(final TurnManager turnManager,
                         final StatusEffectManager statusEffectManager,
                         final PokemonTurnInPlayProvider turnInPlayProvider) {
        this.turnManager = Objects.requireNonNull(turnManager, "turnManager");
        this.statusEffectManager = Objects.requireNonNull(statusEffectManager, "statusEffectManager");
        this.turnInPlayProvider = Objects.requireNonNull(turnInPlayProvider, "turnInPlayProvider");
    }

    /**
     * Validates the given action against the current game state rules.
     *
     * @param action the action to validate (must not be null)
     * @return a non-null {@link ValidationResult}
     */
    public ValidationResult validate(final Action action) {
        return switch (action) {
            case EvolveAction a        -> validateEvolve(a);
            case RetreatAction a       -> validateRetreat(a);
            case PlayTrainerAction a   -> validatePlayTrainer(a);
            case AttachEnergyAction a  -> validateAttachEnergy();
            case DeclareAttackAction a -> validateDeclareAttack(a);
        };
    }

    private ValidationResult validateEvolve(final EvolveAction action) {
        int playerIndex = turnManager.activePlayerIndex();
        if (turnManager.isFirstTurnOfPlayer(playerIndex)) {
            return new ValidationResult.Invalid(CANNOT_EVOLVE_FIRST_TURN);
        }
        if (turnInPlayProvider.getTurnsInPlay(action.target()) < MIN_TURNS_TO_EVOLVE) {
            return new ValidationResult.Invalid(POKEMON_ENTERED_THIS_TURN);
        }
        return new ValidationResult.Valid();
    }

    private ValidationResult validateRetreat(final RetreatAction action) {
        if (!statusEffectManager.canRetreat()) {
            return new ValidationResult.Invalid(RETREAT_BLOCKED_BY_STATUS);
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
            case SUPPORTER -> validateSupporter(mainPhase);
            case STADIUM   -> validateStadium(mainPhase);
            case ITEM      -> new ValidationResult.Valid();
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

    private ValidationResult validateAttachEnergy() {
        MainPhase mainPhase = turnManager.requireMainPhase();
        if (mainPhase.getEnergyAttached() >= MAX_ENERGY_PER_TURN) {
            return new ValidationResult.Invalid(ENERGY_ALREADY_ATTACHED);
        }
        return new ValidationResult.Valid();
    }

    private ValidationResult validateDeclareAttack(final DeclareAttackAction action) {
        if (!statusEffectManager.canAttack()) {
            return new ValidationResult.Invalid(ATTACK_BLOCKED_BY_STATUS);
        }
        if (!hasEnoughEnergyForAttack(action.attacker(), action.attack())) {
            return new ValidationResult.Invalid(INSUFFICIENT_ENERGY_FOR_ATTACK);
        }
        return new ValidationResult.Valid();
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
