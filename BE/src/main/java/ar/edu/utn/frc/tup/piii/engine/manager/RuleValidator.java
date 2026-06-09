package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.PokemonTurnInPlayProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.StadiumStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.BattlefieldStateProvider;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;
import ar.edu.utn.frc.tup.piii.engine.model.Action;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.AttachEnergyAction;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.DeclareAttackAction;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.EndTurnAction;
import ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage;
import ar.edu.utn.frc.tup.piii.engine.model.EvolveAction;
import ar.edu.utn.frc.tup.piii.engine.model.MainPhase;
import ar.edu.utn.frc.tup.piii.engine.model.PlaceBasicPokemonAction;
import ar.edu.utn.frc.tup.piii.engine.model.PlayTrainerAction;
import ar.edu.utn.frc.tup.piii.engine.model.PromoteActiveAction;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.RetreatAction;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.model.ValidationResult;
import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId;

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
    private static final String ATTACK_DISABLED_BY_EFFECT = "attack_disabled_by_effect";
    private static final int MIN_TURNS_TO_EVOLVE = 1;
    private static final int MAX_ENERGY_PER_TURN = 1;
    /** Card ID of Fairy Garden in the XY1 set. */
    private static final String FAIRY_GARDEN_ID = "xy1-117";

    private final TurnManager turnManager;
    private final List<StatusEffectManager> statusEffectManagers;
    private final PokemonTurnInPlayProvider turnInPlayProvider;
    private final BenchStateProvider benchStateProvider;
    private final HandStateProvider handStateProvider;
    /** Optional — null means no Stadium awareness (no effects applied). */
    private final StadiumStateProvider stadiumProvider;
    private final BattlefieldStateProvider battlefieldProvider;

    /**
     * Full constructor. Accepts one {@link StatusEffectManager} per player, an optional
     * {@link StadiumStateProvider} for Stadium-dependent rule modifications (e.g. Fairy Garden),
     * and an optional {@link BattlefieldStateProvider} for Pokémon state checks (e.g. Trevenant).
     */
    public RuleValidator(final TurnManager turnManager,
                         final List<StatusEffectManager> statusEffectManagers,
                         final PokemonTurnInPlayProvider turnInPlayProvider,
                         final BenchStateProvider benchStateProvider,
                         final ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider handStateProvider,
                         final StadiumStateProvider stadiumProvider,
                         final BattlefieldStateProvider battlefieldProvider) {
        this.turnManager = Objects.requireNonNull(turnManager, "turnManager");
        this.statusEffectManagers = List.copyOf(
                Objects.requireNonNull(statusEffectManagers, "statusEffectManagers"));
        this.turnInPlayProvider = Objects.requireNonNull(turnInPlayProvider, "turnInPlayProvider");
        this.benchStateProvider = Objects.requireNonNull(benchStateProvider, "benchStateProvider");
        this.handStateProvider = Objects.requireNonNull(handStateProvider, "handStateProvider");
        this.stadiumProvider = stadiumProvider;
        this.battlefieldProvider = battlefieldProvider;
    }

    /**
     * Backward-compatible 6-argument constructor.
     */
    public RuleValidator(final TurnManager turnManager,
                         final List<StatusEffectManager> statusEffectManagers,
                         final PokemonTurnInPlayProvider turnInPlayProvider,
                         final BenchStateProvider benchStateProvider,
                         final ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider handStateProvider,
                         final StadiumStateProvider stadiumProvider) {
        this(turnManager, statusEffectManagers, turnInPlayProvider, benchStateProvider, handStateProvider, stadiumProvider,
             turnInPlayProvider instanceof BattlefieldStateProvider bp ? bp : null);
    }

    /**
     * Multi-player constructor without Stadium awareness (backward-compatible).
     *
     * @param turnManager          manages turn phases and player state (must not be null)
     * @param statusEffectManagers one SEM per player, indexed by player index (must not be null)
     * @param turnInPlayProvider   reports how many turns a Pokémon has been in play (must not be null)
     * @param benchStateProvider   provides bench size per player (must not be null)
     * @param handStateProvider    provides read-only access to player's hands (must not be null)
     */
    public RuleValidator(final TurnManager turnManager,
                         final List<StatusEffectManager> statusEffectManagers,
                         final PokemonTurnInPlayProvider turnInPlayProvider,
                         final BenchStateProvider benchStateProvider,
                         final ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider handStateProvider) {
        this(turnManager, statusEffectManagers, turnInPlayProvider, benchStateProvider, handStateProvider, null,
             turnInPlayProvider instanceof BattlefieldStateProvider bp ? bp : null);
    }

    /**
     * Single-player backward-compatible constructor (used in tests and legacy callers).
     *
     * @param turnManager         manages turn phases and player state (must not be null)
     * @param statusEffectManager manages active status effects on the current Pokémon (must not be null)
     * @param turnInPlayProvider  reports how many turns a Pokémon has been in play (must not be null)
     * @param benchStateProvider  provides bench size per player (must not be null)
     * @param handStateProvider   provides read-only access to player's hands (must not be null)
     */
    public RuleValidator(final TurnManager turnManager,
                         final StatusEffectManager statusEffectManager,
                         final PokemonTurnInPlayProvider turnInPlayProvider,
                         final BenchStateProvider benchStateProvider,
                         final ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider handStateProvider) {
        this(turnManager, List.of(statusEffectManager), turnInPlayProvider, benchStateProvider, handStateProvider, null,
             turnInPlayProvider instanceof BattlefieldStateProvider bp ? bp : null);
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
        return validate(action, turnManager.activePlayerIndex());
    }

    public ValidationResult validate(final Action action, final int playerIndex) {
        return switch (action) {
            case EvolveAction a             -> validateEvolve(a, playerIndex);
            case RetreatAction a            -> validateRetreat(a, playerIndex);
            case PlayTrainerAction a        -> validatePlayTrainer(a, playerIndex);
            case AttachEnergyAction a       -> validateAttachEnergy(a, playerIndex);
            case DeclareAttackAction a      -> validateDeclareAttack(a, playerIndex);
            case PlaceBasicPokemonAction a  -> validatePlaceBasicPokemon(a, playerIndex);
            case UseAbilityAction a         -> validateUseAbility(a, playerIndex);
            case EndTurnAction a            -> new ValidationResult.Valid();
            case PromoteActiveAction a      -> validatePromoteActive(a, playerIndex);
            case ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction a -> validateSelectCards(a, playerIndex);
        };
    }

    private ValidationResult validateEvolve(final EvolveAction action, final int playerIndex) {
        if (action.target() == null) {
            return new ValidationResult.Invalid("target_pokemon_required");
        }
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
             || (targetStage == EvolutionStage.BASIC     && evolutionStage == EvolutionStage.MEGA)
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

    private ValidationResult validateRetreat(final RetreatAction action, final int playerIndex) {
        if (action.active() == null) {
            return new ValidationResult.Invalid("no_active_pokemon");
        }
        if (!getActiveStatusEffectManager(playerIndex).canRetreat()) {
            return new ValidationResult.Invalid(RETREAT_BLOCKED_BY_STATUS);
        }
        if (benchStateProvider.getBenchSize(playerIndex) == 0) {
            return new ValidationResult.Invalid(EMPTY_BENCH_FOR_RETREAT);
        }
        MainPhase mainPhase = turnManager.requireMainPhase();
        if (mainPhase.isRetreatUsed()) {
            return new ValidationResult.Invalid(RETREAT_ALREADY_USED);
        }

        // Fairy Garden (xy1-117): if active and Pokémon has ≥1 Fairy energy, retreat is free.
        final int retreatCost = (isFairyGardenActive() && hasFairyEnergy(action.active()))
                ? 0
                : action.active().getRetreatCost();

        if (action.active().getAttachedEnergies().size() < retreatCost) {
            return new ValidationResult.Invalid(INSUFFICIENT_ENERGY_FOR_RETREAT);
        }

        java.util.List<Integer> indices = action.energyIndicesToDiscard();
        if (indices == null || indices.size() != retreatCost) {
            return new ValidationResult.Invalid("Must specify exactly " + retreatCost + " energy indices to discard.");
        }

        long uniqueIndices = indices.stream().distinct().count();
        if (uniqueIndices != indices.size()) {
            return new ValidationResult.Invalid("Duplicate energy indices are not allowed.");
        }

        int attachedSize = action.active().getAttachedEnergies().size();
        if (indices.stream().anyMatch(i -> i < 0 || i >= attachedSize)) {
            return new ValidationResult.Invalid("Invalid energy index specified.");
        }

        return new ValidationResult.Valid();
    }

    /**
     * Returns {@code true} if Fairy Garden (xy1-117) is the currently active Stadium.
     */
    private boolean isFairyGardenActive() {
        if (stadiumProvider == null) {
            return false;
        }
        final TrainerCard stadium = stadiumProvider.getActiveStadium();
        return stadium != null && FAIRY_GARDEN_ID.equals(stadium.getCardId());
    }

    /**
     * Returns {@code true} if the given Pokémon has at least one FAIRY energy attached.
     * Required by Fairy Garden's free-retreat condition.
     */
    private boolean hasFairyEnergy(final BattlePokemonState pokemon) {
        return pokemon.getAttachedEnergies().contains(PokemonType.FAIRY);
    }

    private ValidationResult validatePlayTrainer(final PlayTrainerAction action, final int playerIndex) {
        MainPhase mainPhase = turnManager.requireMainPhase();

        final int opponentIndex = 1 - playerIndex;
        if (battlefieldProvider != null) {
            final BattlePokemonState opponentActive = battlefieldProvider.getActivePokemon(opponentIndex);
            if (opponentActive != null && hasAbility(opponentActive, AbilityEffectId.FOREST_CURSE)) {
                if (action.trainerType() == TrainerType.ITEM) {
                    return new ValidationResult.Invalid("opponent_forests_curse_active");
                }
            }
        }

        if (action.effectId() == ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.EVOSODA) {
            if (action.target() == null) {
                return new ValidationResult.Invalid("target_pokemon_required");
            }
            if (turnManager.isFirstTurnOfPlayer(playerIndex)) {
                return new ValidationResult.Invalid(CANNOT_EVOLVE_FIRST_TURN);
            }
            if (turnInPlayProvider.getTurnsInPlay(action.target()) < MIN_TURNS_TO_EVOLVE) {
                return new ValidationResult.Invalid(POKEMON_ENTERED_THIS_TURN);
            }
        }
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

    private ValidationResult validateAttachEnergy(final AttachEnergyAction action, final int playerIndex) {
        if (action.target() == null) {
            return new ValidationResult.Invalid("target_pokemon_required");
        }
        if (action.energyType() == null) {
            return new ValidationResult.Invalid("energy_type_required");
        }
        MainPhase mainPhase = turnManager.requireMainPhase();
        if (mainPhase.getEnergyAttached() >= MAX_ENERGY_PER_TURN) {
            return new ValidationResult.Invalid(ENERGY_ALREADY_ATTACHED);
        }
        return new ValidationResult.Valid();
    }

    private ValidationResult validateDeclareAttack(final DeclareAttackAction action, final int playerIndex) {
        if (turnManager.getStartingPlayerIndex() == playerIndex && turnManager.isFirstTurnOfPlayer(playerIndex)) {
            return new ValidationResult.Invalid(CANNOT_ATTACK_FIRST_TURN);
        }
        if (!getActiveStatusEffectManager(playerIndex).canAttack()) {
            return new ValidationResult.Invalid(ATTACK_BLOCKED_BY_STATUS);
        }
        final String disabledAttack = getActiveStatusEffectManager(playerIndex).getDisabledAttackName();
        if (disabledAttack != null && disabledAttack.equalsIgnoreCase(action.attack().name())) {
            return new ValidationResult.Invalid(ATTACK_DISABLED_BY_EFFECT);
        }
        if (!hasEnoughEnergyForAttack(action.attacker(), action.attack())) {
            return new ValidationResult.Invalid(INSUFFICIENT_ENERGY_FOR_ATTACK);
        }
        return new ValidationResult.Valid();
    }

    private ValidationResult validatePlaceBasicPokemon(final PlaceBasicPokemonAction action, final int playerIndex) {
        turnManager.requireMainPhase();
        
        final java.util.Optional<ar.edu.utn.frc.tup.piii.engine.model.Card> maybeCard =
                handStateProvider.getCardInHand(playerIndex, action.cardId());

        if (maybeCard.isEmpty()) {
            return new ValidationResult.Invalid("card_not_in_hand");
        }
        final ar.edu.utn.frc.tup.piii.engine.model.Card card = maybeCard.get();
        if (!card.isBasicPokemon()) {
            return new ValidationResult.Invalid("card_not_basic_pokemon");
        }
        if (benchStateProvider.getBenchSize(playerIndex) >= 5) {
            return new ValidationResult.Invalid("bench_full");
        }
        return new ValidationResult.Valid();
    }

    private ValidationResult validateUseAbility(final UseAbilityAction action, final int playerIndex) {
        turnManager.requireMainPhase();
        
        final BattlePokemonState source = action.source();
        if (source == null) {
            return new ValidationResult.Invalid("target_pokemon_required");
        }
        
        final String abilityIdStr = action.abilityId();
        
        var abilityOpt = source.getAbilities().stream()
                .filter(a -> a.name().equalsIgnoreCase(abilityIdStr) || a.effectId().name().equalsIgnoreCase(abilityIdStr))
                .findFirst();
                
        if (abilityOpt.isEmpty()) {
            return new ValidationResult.Invalid("ability_not_found");
        }
        
        var ability = abilityOpt.get();
        final AbilityEffectId effId = ability.effectId();
        
        if (effId == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.MYSTICAL_FIRE || 
            effId == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.MAGNETIC_DRAW ||
            effId == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.DRIVE_OFF ||
            effId == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.WATER_SHURIKEN ||
            effId == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.STANCE_CHANGE ||
            effId == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.UPSIDE_DOWN_EVOLUTION) {
            if (source.hasUsedAbilityThisTurn(effId.name())) {
                return new ValidationResult.Invalid("ability_already_used_this_turn");
            }
        }
        
        final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime = getActiveStatusEffectManager(playerIndex).getPlayerRuntime();
        
        if (effId == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.DRIVE_OFF) {
            if (runtime == null || runtime.getActivePokemon() != source) {
                return new ValidationResult.Invalid("pokemon_must_be_active");
            }
            final int opponentIndex = 1 - playerIndex;
            if (benchStateProvider.getBenchSize(opponentIndex) == 0) {
                return new ValidationResult.Invalid("opponent_bench_empty");
            }
        }
        
        if (effId == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.WATER_SHURIKEN) {
            if (runtime == null) {
                return new ValidationResult.Invalid("player_runtime_required");
            }
            final boolean hasWaterEnergy = runtime.getHand().getCards().stream()
                    .anyMatch(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard ec && ec.getEnergyType() == PokemonType.WATER);
            if (!hasWaterEnergy) {
                return new ValidationResult.Invalid("water_energy_required_in_hand");
            }
        }
        
        if (effId == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.STANCE_CHANGE) {
            if (runtime == null) {
                return new ValidationResult.Invalid("player_runtime_required");
            }
            final boolean hasAegislash = runtime.getHand().getCards().stream()
                    .anyMatch(c -> c.getName().equalsIgnoreCase("Aegislash") && !c.getCardId().equals(source.getCardId()));
            if (!hasAegislash) {
                return new ValidationResult.Invalid("aegislash_required_in_hand");
            }
        }
        
        if (effId == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.UPSIDE_DOWN_EVOLUTION) {
            if (runtime == null || runtime.getActivePokemon() != source) {
                return new ValidationResult.Invalid("pokemon_must_be_active");
            }
            if (!getActiveStatusEffectManager(playerIndex).has(ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType.CONFUNDIDO)) {
                return new ValidationResult.Invalid("pokemon_must_be_confused");
            }
        }
        
        return new ValidationResult.Valid();
    }

    /**
     * Validates a PROMOTE_ACTIVE action. Checks that the bench index is non-negative.
     * The service layer is responsible for verifying that promotion is actually pending
     * and that the correct player is promoting.
     *
     * @param action the promote action (never null)
     * @return Valid if the bench index is ≥ 0; Invalid otherwise
     */
    private ValidationResult validatePromoteActive(final PromoteActiveAction action, final int playerIndex) {
        if (action.benchIndex() < 0) {
            return new ValidationResult.Invalid("invalid_bench_index");
        }
        final int size = benchStateProvider.getBenchSize(playerIndex);
        if (action.benchIndex() >= size) {
            return new ValidationResult.Invalid("bench_index_out_of_bounds");
        }
        return new ValidationResult.Valid();
    }

    /**
     * Returns the StatusEffectManager for the currently active player.
     * Falls back to the first manager if only one is registered (legacy / test usage).
     *
     * @return the active player's SEM (never null)
     */
    private StatusEffectManager getActiveStatusEffectManager(final int playerIndex) {
        if (playerIndex < 0 || playerIndex >= statusEffectManagers.size()) {
            return statusEffectManagers.get(0);
        }
        return statusEffectManagers.get(playerIndex);
    }


    private boolean hasEnoughEnergyForAttack(final BattlePokemonState attacker, final Attack attack) {
        // Build a mutable pool of energy entries. Each entry tracks its type and whether
        // it is a wildcard (Rainbow Energy — provides all types).
        List<EnergyCard> energyCards = attacker.getAttachedEnergyCards();
        List<PokemonType> pool = new java.util.ArrayList<>(attacker.getAttachedEnergies());
        List<Boolean> wildcard = new java.util.ArrayList<>();
        // Build wildcard flags aligned with the pool. Each EnergyCard may contribute
        // multiple entries (e.g. Double Colorless = 2). We expand accordingly.
        for (EnergyCard ec : energyCards) {
            for (int i = 0; i < ec.getEnergyCount(); i++) {
                wildcard.add(ec.isProvidesAllTypes());
            }
        }

        int colorlessRequired = 0;
        for (PokemonType required : attack.requiredEnergies()) {
            if (required == PokemonType.COLORLESS) {
                colorlessRequired++;
                continue;
            }
            // First try to satisfy with exact-match (non-wildcard) energies.
            boolean satisfied = false;
            for (int i = 0; i < pool.size(); i++) {
                if (!wildcard.get(i) && pool.get(i) == required) {
                    pool.remove(i);
                    wildcard.remove(i);
                    satisfied = true;
                    break;
                }
            }
            if (!satisfied) {
                // Fall back to wildcard energies (Rainbow Energy).
                for (int i = 0; i < pool.size(); i++) {
                    if (wildcard.get(i)) {
                        pool.remove(i);
                        wildcard.remove(i);
                        satisfied = true;
                        break;
                    }
                }
            }
            if (!satisfied) {
                return false;
            }
        }
        return pool.size() >= colorlessRequired;
    }

    private ValidationResult validateSelectCards(final ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction action, final int playerIndex) {
        if (!(turnManager.currentPhase() instanceof ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase)) {
            return new ValidationResult.Invalid("wrong_phase_for_selection");
        }
        
        final ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest req = action.request();
        if (req != null) {
            if (action.cardIds().size() > req.maxSelections()) {
                return new ValidationResult.Invalid("too_many_cards_selected");
            }
        }
        
        // Zone and Type validation is deferred to GameFacade because RuleValidator lacks Deck/Discard access.
        return new ValidationResult.Valid();
    }

    private boolean hasAbility(final BattlePokemonState pokemon, final AbilityEffectId abilityId) {
        return pokemon != null && pokemon.getAbilities().stream().anyMatch(a -> a.effectId() == abilityId);
    }
}
