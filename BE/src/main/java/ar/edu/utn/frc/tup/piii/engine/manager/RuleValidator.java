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
import ar.edu.utn.frc.tup.piii.engine.model.Card;
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
@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity", "PMD.TooManyMethods",
        "PMD.ExcessiveImports", "PMD.CouplingBetweenObjects"})
// Rule validator for every player Action type (10 sealed variants) plus per-TrainerCard,
// per-Ability, and per-SelectCards-effect preconditions — each dispatched through its own
// Strategy-map (trainerCardValidators/abilityValidators/selectionCountValidators) so per-method
// complexity stays low. The aggregate class-level metrics reflect the number of independent
// game-rule cases covered, not entangled logic; touching every Action/Card/Ability model type is
// inherent to a pure-read rule validator, not accidental coupling.
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
    private static final String TARGET_POKEMON_REQUIRED = "target_pokemon_required";
    private static final String PLAYER_RUNTIME_REQUIRED = "player_runtime_required";
    private static final String MUST_SELECT_EXACT_AMOUNT = "must_select_exact_amount";
    private static final int MIN_TURNS_TO_EVOLVE = 1;
    private static final int MAX_ENERGY_PER_TURN = 1;
    private static final int MAX_BENCH_SIZE = 5;
    private static final int MIN_CARDS_FOR_ULTRA_BALL = 3;
    private static final int MIN_DAMAGE_FOR_SHADOW_VOID = 10;
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
            case EndTurnAction a            -> validateEndTurn(a, playerIndex);
            case PromoteActiveAction a      -> validatePromoteActive(a, playerIndex);
            case ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction a -> validateSelectCards(a, playerIndex);
        };
    }

    private ValidationResult validateEvolve(final EvolveAction action, final int playerIndex) {
        if (action.target() == null) {
            return new ValidationResult.Invalid(TARGET_POKEMON_REQUIRED);
        }
        
        boolean hasAdaptiveEvolution = action.target().getAbilities().stream()
                .anyMatch(a -> a.effectId() == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.ADAPTIVE_EVOLUTION);

        if (!hasAdaptiveEvolution) {
            if (turnManager.isFirstTurnOfPlayer(playerIndex)) {
                return new ValidationResult.Invalid(CANNOT_EVOLVE_FIRST_TURN);
            }
            if (turnInPlayProvider.getTurnsInPlay(action.target()) < MIN_TURNS_TO_EVOLVE) {
                return new ValidationResult.Invalid(POKEMON_ENTERED_THIS_TURN);
            }
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
        final ValidationResult preconditionResult = validateRetreatPreconditions(action, playerIndex);
        if (preconditionResult instanceof ValidationResult.Invalid) {
            return preconditionResult;
        }

        // Fairy Garden (xy1-117): if active and Pokémon has ≥1 Fairy energy, retreat is free.
        final int retreatCost = (isFairyGardenActive() && hasFairyEnergy(action.active()))
                ? 0
                : action.active().getRetreatCost();

        if (action.active().getAttachedEnergies().size() < retreatCost) {
            return new ValidationResult.Invalid(INSUFFICIENT_ENERGY_FOR_RETREAT);
        }

        return validateRetreatEnergyIndices(action, retreatCost);
    }

    private ValidationResult validateRetreatPreconditions(final RetreatAction action, final int playerIndex) {
        if (action.active() == null) {
            return new ValidationResult.Invalid("no_active_pokemon");
        }
        if (!getActiveStatusEffectManager(playerIndex).canRetreat()) {
            return new ValidationResult.Invalid(RETREAT_BLOCKED_BY_STATUS);
        }
        if (getActiveStatusEffectManager(playerIndex).has(ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType.ENVENENADO)
                && opponentHasAbility(playerIndex, AbilityEffectId.POISON_BARRIER)) {
            return new ValidationResult.Invalid("retreat_blocked_by_poison_barrier");
        }
        if (benchStateProvider.getBenchSize(playerIndex) == 0) {
            return new ValidationResult.Invalid(EMPTY_BENCH_FOR_RETREAT);
        }
        final MainPhase mainPhase = turnManager.requireMainPhase();
        if (mainPhase.isRetreatUsed()) {
            return new ValidationResult.Invalid(RETREAT_ALREADY_USED);
        }
        return new ValidationResult.Valid();
    }

    private ValidationResult validateRetreatEnergyIndices(final RetreatAction action, final int retreatCost) {
        final java.util.List<Integer> indices = action.energyIndicesToDiscard();
        if (indices == null || indices.size() != retreatCost) {
            return new ValidationResult.Invalid("Must specify exactly " + retreatCost + " energy indices to discard.");
        }

        final long uniqueIndices = indices.stream().distinct().count();
        if (uniqueIndices != indices.size()) {
            return new ValidationResult.Invalid("Duplicate energy indices are not allowed.");
        }

        final int attachedSize = action.active().getAttachedEnergies().size();
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
        return pokemon.getAttachedEnergies().contains(PokemonType.FIRE.getClass().cast(PokemonType.FAIRY)) // wait, no need for cast, just standard contains is fine
                || pokemon.getAttachedEnergies().contains(PokemonType.FAIRY)
                || pokemon.getAttachedEnergyCards().stream().anyMatch(EnergyCard::isProvidesAllTypes);
    }

    private List<ar.edu.utn.frc.tup.piii.engine.model.Card> getDiscardPile(final int playerIndex) {
        if (benchStateProvider instanceof ar.edu.utn.frc.tup.piii.engine.listener.DiscardPileStateProvider dp) {
            return dp.getDiscardPile(playerIndex);
        }
        return List.of();
    }

    private int getDeckSize(final int playerIndex) {
        if (benchStateProvider instanceof ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider dp) {
            return dp.getDeckSize(playerIndex);
        }
        return 60;
    }

    private record TrainerCardContext(PlayTrainerAction action, int playerIndex, int opponentIndex) {
    }

    /**
     * Validates one {@link TrainerEffectId}'s card-specific preconditions (e.g. Super Potion
     * needs a damaged target). Returns {@code null} when the card's preconditions are met, or
     * an {@link ValidationResult.Invalid} describing which precondition failed.
     */
    @FunctionalInterface
    private interface TrainerCardValidator {
        ValidationResult.Invalid validate(TrainerCardContext ctx);
    }

    private final Map<ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId, TrainerCardValidator> trainerCardValidators = buildTrainerCardValidators();

    private Map<ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId, TrainerCardValidator> buildTrainerCardValidators() {
        final Map<ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId, TrainerCardValidator> v =
                new java.util.EnumMap<>(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.class);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.SUPER_POTION, this::validateSuperPotionCard);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.CASSIUS, this::validateCassiusCard);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.EVOSODA, this::validateEvosodaCard);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.MAX_REVIVE, this::validateMaxReviveCard);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.PROFESSORS_LETTER, this::validateNonEmptyDeckCard);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.GREAT_BALL, this::validateNonEmptyDeckCard);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.POKEMON_FAN_CLUB, this::validateNonEmptyDeckCard);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.TEAM_FLARE_GRUNT, this::validateTeamFlareGruntCard);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.RED_CARD, this::validateRedCardCard);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.LYSANDRE, this::validateLysandreCard);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.SACRED_ASH, this::validateSacredAshCard);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FIERY_TORCH, this::validateFieryTorchCard);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.TRICK_SHOVEL, this::validateTrickShovelCard);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.STARTLING_MEGAPHONE, this::validateStartlingMegaphoneCard);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.PAL_PAD, this::validatePalPadCard);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BLACKSMITH, this::validateBlacksmithCard);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.POKEMON_CENTER_LADY, this::validatePokemonCenterLadyCard);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.ULTRA_BALL, this::validateUltraBallCard);
        return v;
    }

    private ValidationResult validatePlayTrainer(final PlayTrainerAction action, final int playerIndex) {
        MainPhase mainPhase = turnManager.requireMainPhase();

        final int opponentIndex = 1 - playerIndex;
        if (battlefieldProvider != null) {
            final BattlePokemonState opponentActive = battlefieldProvider.getActivePokemon(opponentIndex);
            if (opponentActive != null && hasAbility(opponentActive, AbilityEffectId.FOREST_CURSE)
                    && action.trainerType() == TrainerType.ITEM) {
                return new ValidationResult.Invalid("opponent_forests_curse_active");
            }
        }

        final var effectId = action.effectId();
        final TrainerCardValidator validator = trainerCardValidators.get(effectId);
        if (validator != null) {
            final ValidationResult.Invalid invalid = validator.validate(new TrainerCardContext(action, playerIndex, opponentIndex));
            if (invalid != null) {
                return invalid;
            }
        }

        return switch (action.trainerType()) {
            case SUPPORTER    -> validateSupporter(mainPhase);
            case STADIUM      -> validateStadium(mainPhase, action, playerIndex);
            case ITEM         -> new ValidationResult.Valid();
            case POKEMON_TOOL -> validatePokemonTool(action.target());
        };
    }

    private ValidationResult.Invalid validateSuperPotionCard(final TrainerCardContext ctx) {
        if (ctx.action().target() == null) {
            return new ValidationResult.Invalid(TARGET_POKEMON_REQUIRED);
        }
        if (ctx.action().target().getDamageCounters() == 0) {
            return new ValidationResult.Invalid("target_has_no_damage");
        }
        if (ctx.action().target().getAttachedEnergies().isEmpty()) {
            return new ValidationResult.Invalid("target_has_no_energy");
        }
        return null;
    }

    private ValidationResult.Invalid validateCassiusCard(final TrainerCardContext ctx) {
        if (ctx.action().target() == null) {
            return new ValidationResult.Invalid(TARGET_POKEMON_REQUIRED);
        }
        final boolean inPlay = (battlefieldProvider != null && ctx.action().target().equals(battlefieldProvider.getActivePokemon(ctx.playerIndex())))
                || benchStateProvider.getBenchedPokemon(ctx.playerIndex()).contains(ctx.action().target());
        if (!inPlay) {
            return new ValidationResult.Invalid("target_pokemon_not_in_play");
        }
        return null;
    }

    private ValidationResult.Invalid validateEvosodaCard(final TrainerCardContext ctx) {
        if (ctx.action().target() == null) {
            return new ValidationResult.Invalid(TARGET_POKEMON_REQUIRED);
        }
        final boolean inPlay = (battlefieldProvider != null && ctx.action().target().equals(battlefieldProvider.getActivePokemon(ctx.playerIndex())))
                || benchStateProvider.getBenchedPokemon(ctx.playerIndex()).contains(ctx.action().target());
        if (!inPlay) {
            return new ValidationResult.Invalid("target_pokemon_not_in_play");
        }
        boolean hasAdaptiveEvolution = ctx.action().target().getAbilities().stream()
                .anyMatch(a -> a.effectId() == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.ADAPTIVE_EVOLUTION);
        if (!hasAdaptiveEvolution) {
            if (turnManager.isFirstTurnOfPlayer(ctx.playerIndex())) {
                return new ValidationResult.Invalid(CANNOT_EVOLVE_FIRST_TURN);
            }
            if (turnInPlayProvider.getTurnsInPlay(ctx.action().target()) < MIN_TURNS_TO_EVOLVE) {
                return new ValidationResult.Invalid(POKEMON_ENTERED_THIS_TURN);
            }
        }
        if (ctx.action().target().getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.STAGE_2
                || ctx.action().target().getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.MEGA) {
            return new ValidationResult.Invalid("cannot_evolve_further");
        }
        return null;
    }

    private ValidationResult.Invalid validateMaxReviveCard(final TrainerCardContext ctx) {
        final List<ar.edu.utn.frc.tup.piii.engine.model.Card> discard = getDiscardPile(ctx.playerIndex());
        final boolean hasBasicPokemon = discard.stream()
                .anyMatch(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard pc && pc.getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.BASIC);
        if (!hasBasicPokemon) {
            return new ValidationResult.Invalid("no_basic_pokemon_in_discard_pile");
        }
        return null;
    }

    private ValidationResult.Invalid validateNonEmptyDeckCard(final TrainerCardContext ctx) {
        if (getDeckSize(ctx.playerIndex()) == 0) {
            return new ValidationResult.Invalid("deck_is_empty");
        }
        return null;
    }

    private ValidationResult.Invalid validateTeamFlareGruntCard(final TrainerCardContext ctx) {
        if (battlefieldProvider == null) {
            return new ValidationResult.Invalid("battlefield_provider_required");
        }
        final BattlePokemonState opponentActive = battlefieldProvider.getActivePokemon(ctx.opponentIndex());
        if (opponentActive == null) {
            return new ValidationResult.Invalid("opponent_has_no_active_pokemon");
        }
        if (opponentActive.getAttachedEnergies().isEmpty()) {
            return new ValidationResult.Invalid("opponent_active_has_no_energy");
        }
        return null;
    }

    private ValidationResult.Invalid validateRedCardCard(final TrainerCardContext ctx) {
        if (handStateProvider.getHandSize(ctx.opponentIndex()) == 0) {
            return new ValidationResult.Invalid("opponent_hand_is_empty");
        }
        return null;
    }

    private ValidationResult.Invalid validateLysandreCard(final TrainerCardContext ctx) {
        if (benchStateProvider == null || benchStateProvider.getBenchSize(ctx.opponentIndex()) == 0) {
            return new ValidationResult.Invalid("opponent_bench_empty");
        }
        if (ctx.action().target() == null) {
            return new ValidationResult.Invalid(TARGET_POKEMON_REQUIRED);
        }
        if (!benchStateProvider.getBenchedPokemon(ctx.opponentIndex()).contains(ctx.action().target())) {
            return new ValidationResult.Invalid("target_must_be_on_opponent_bench");
        }
        return null;
    }

    private ValidationResult.Invalid validateSacredAshCard(final TrainerCardContext ctx) {
        final List<Card> discard = getDiscardPile(ctx.playerIndex());
        final boolean hasPokemon = discard.stream().anyMatch(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard);
        if (!hasPokemon) {
            return new ValidationResult.Invalid("no_pokemon_in_discard_pile");
        }
        return null;
    }

    private ValidationResult.Invalid validateFieryTorchCard(final TrainerCardContext ctx) {
        if (getDeckSize(ctx.playerIndex()) == 0) {
            return new ValidationResult.Invalid("deck_is_empty");
        }
        final List<Card> hand = handStateProvider.getHandCards(ctx.playerIndex());
        final boolean hasFireEnergy = hand.stream().anyMatch(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard ec && ec.getEnergyType() == PokemonType.FIRE);
        if (!hasFireEnergy) {
            return new ValidationResult.Invalid("fire_energy_required_in_hand");
        }
        return null;
    }

    private ValidationResult.Invalid validateTrickShovelCard(final TrainerCardContext ctx) {
        final int targetPlayerIndex = (ctx.action().target() != null && battlefieldProvider != null && ctx.action().target().equals(battlefieldProvider.getActivePokemon(ctx.playerIndex())))
                ? ctx.playerIndex() : ctx.opponentIndex();
        if (getDeckSize(targetPlayerIndex) == 0) {
            return new ValidationResult.Invalid("deck_is_empty");
        }
        return null;
    }

    private ValidationResult.Invalid validateStartlingMegaphoneCard(final TrainerCardContext ctx) {
        boolean opponentHasTool = false;
        final BattlePokemonState opponentActive = battlefieldProvider.getActivePokemon(ctx.opponentIndex());
        if (opponentActive != null && opponentActive.hasToolAttached()) {
            opponentHasTool = true;
        }
        if (!opponentHasTool && benchStateProvider != null) {
            for (final BattlePokemonState benched : benchStateProvider.getBenchedPokemon(ctx.opponentIndex())) {
                if (benched.hasToolAttached()) {
                    opponentHasTool = true;
                    break;
                }
            }
        }
        if (!opponentHasTool) {
            return new ValidationResult.Invalid("opponent_has_no_tools_attached");
        }
        return null;
    }

    private ValidationResult.Invalid validatePalPadCard(final TrainerCardContext ctx) {
        final List<Card> discard = getDiscardPile(ctx.playerIndex());
        final boolean hasSupporter = discard.stream()
                .anyMatch(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.TrainerCard tc && tc.getTrainerType() == TrainerType.SUPPORTER);
        if (!hasSupporter) {
            return new ValidationResult.Invalid("no_supporter_in_discard_pile");
        }
        return null;
    }

    private ValidationResult.Invalid validateBlacksmithCard(final TrainerCardContext ctx) {
        if (ctx.action().target() == null) {
            return new ValidationResult.Invalid(TARGET_POKEMON_REQUIRED);
        }
        if (ctx.action().target().getPokemonType() != PokemonType.FIRE) {
            return new ValidationResult.Invalid("target_must_be_fire_pokemon");
        }
        final List<Card> discard = getDiscardPile(ctx.playerIndex());
        final boolean hasFireEnergy = discard.stream()
                .anyMatch(c -> c instanceof EnergyCard ec && ec.getEnergyType() == PokemonType.FIRE);
        if (!hasFireEnergy) {
            return new ValidationResult.Invalid("fire_energy_required_in_discard");
        }
        return null;
    }

    private ValidationResult.Invalid validatePokemonCenterLadyCard(final TrainerCardContext ctx) {
        if (ctx.action().target() == null) {
            return new ValidationResult.Invalid(TARGET_POKEMON_REQUIRED);
        }
        final boolean isActive = battlefieldProvider != null && ctx.action().target().equals(battlefieldProvider.getActivePokemon(ctx.playerIndex()));
        final boolean hasStatus = isActive && !getActiveStatusEffectManager(ctx.playerIndex()).activeEffects().isEmpty();
        if (ctx.action().target().getDamageCounters() == 0 && !hasStatus) {
            return new ValidationResult.Invalid("target_has_no_damage_or_status");
        }
        return null;
    }

    private ValidationResult.Invalid validateUltraBallCard(final TrainerCardContext ctx) {
        if (handStateProvider.getHandSize(ctx.playerIndex()) < MIN_CARDS_FOR_ULTRA_BALL) {
            return new ValidationResult.Invalid("insufficient_cards_in_hand");
        }
        return null;
    }

    private ValidationResult validateSupporter(final MainPhase mainPhase) {
        if (mainPhase.isSupporterPlayed()) {
            return new ValidationResult.Invalid(SUPPORTER_ALREADY_PLAYED);
        }
        return new ValidationResult.Valid();
    }

    private ValidationResult validateStadium(final MainPhase mainPhase, final PlayTrainerAction action, final int playerIndex) {
        if (mainPhase.isStadiumPlayed()) {
            return new ValidationResult.Invalid(STADIUM_ALREADY_PLAYED);
        }
        if (isSameNameStadiumInPlay(action, playerIndex)) {
            return new ValidationResult.Invalid("stadium_same_name_in_play");
        }
        return new ValidationResult.Valid();
    }

    private boolean isSameNameStadiumInPlay(final PlayTrainerAction action, final int playerIndex) {
        if (stadiumProvider == null || action.cardId() == null || handStateProvider == null) {
            return false;
        }
        final TrainerCard activeStadium = stadiumProvider.getActiveStadium();
        if (activeStadium == null) {
            return false;
        }
        final java.util.Optional<Card> playedCardOpt = handStateProvider.getCardInHand(playerIndex, action.cardId());
        return playedCardOpt.isPresent() && playedCardOpt.get() instanceof TrainerCard playedStadium
                && activeStadium.getName().equalsIgnoreCase(playedStadium.getName());
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
            return new ValidationResult.Invalid(TARGET_POKEMON_REQUIRED);
        }
        if (action.energyType() == null) {
            return new ValidationResult.Invalid("energy_type_required");
        }
        MainPhase mainPhase = turnManager.requireMainPhase();
        if (mainPhase.getEnergyAttached() >= MAX_ENERGY_PER_TURN) {
            return new ValidationResult.Invalid(ENERGY_ALREADY_ATTACHED);
        }

        if (opponentHasAbility(playerIndex, AbilityEffectId.HAND_LOCK) && isSpecialEnergyOfType(playerIndex, action.energyType())) {
            return new ValidationResult.Invalid("opponent_hand_lock_active");
        }

        return new ValidationResult.Valid();
    }

    private boolean isSpecialEnergyOfType(final int playerIndex, final PokemonType energyType) {
        final java.util.List<Card> handCards = handStateProvider.getHandCards(playerIndex);
        final java.util.Optional<EnergyCard> maybeEnergy = handCards.stream()
                .filter(c -> c instanceof EnergyCard e && e.getEnergyType() == energyType)
                .map(c -> (EnergyCard) c)
                .findFirst();
        return maybeEnergy.isPresent() && maybeEnergy.get().isSpecial();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    // Kept for signature parity with every other validateX(Action, int) method dispatched from
    // the sealed switch in validate() — EndTurnAction itself carries no fields to check.
    private ValidationResult validateEndTurn(final EndTurnAction action, final int playerIndex) {
        if (battlefieldProvider != null && battlefieldProvider.getActivePokemon(playerIndex) == null) {
            return new ValidationResult.Invalid("Debes colocar un Pokémon Activo antes de pasar de turno.");
        }
        return new ValidationResult.Valid();
    }

    private ValidationResult validateDeclareAttack(final DeclareAttackAction action, final int playerIndex) {
        if (action.attacker() == null) {
            return new ValidationResult.Invalid("attacker_required");
        }
        if (action.attack() == null) {
            return new ValidationResult.Invalid("attack_required");
        }
        if (battlefieldProvider != null && battlefieldProvider.getActivePokemon(playerIndex) == null) {
            return new ValidationResult.Invalid("No tienes un Pokémon Activo para atacar.");
        }
        if (turnManager.getStartingPlayerIndex() == playerIndex && turnManager.isFirstTurnOfPlayer(playerIndex)) {
            return new ValidationResult.Invalid(CANNOT_ATTACK_FIRST_TURN);
        }
        final ValidationResult disabledResult = validateAttackNotDisabled(action, playerIndex);
        if (disabledResult instanceof ValidationResult.Invalid) {
            return disabledResult;
        }
        if (!hasEnoughEnergyForAttack(action.attacker(), action.attack())) {
            return new ValidationResult.Invalid(INSUFFICIENT_ENERGY_FOR_ATTACK);
        }
        return new ValidationResult.Valid();
    }

    private ValidationResult validateAttackNotDisabled(final DeclareAttackAction action, final int playerIndex) {
        final StatusEffectManager sem = getActiveStatusEffectManager(playerIndex);
        if (!sem.canAttack()) {
            return new ValidationResult.Invalid(ATTACK_BLOCKED_BY_STATUS);
        }
        final String attackName = action.attack().name();
        final String disabledAttack = sem.getDisabledAttackName();
        if (disabledAttack != null && disabledAttack.equalsIgnoreCase(attackName)) {
            return new ValidationResult.Invalid(ATTACK_DISABLED_BY_EFFECT);
        }
        final String selfDisabledAttack = sem.getSelfDisabledAttackName();
        if (selfDisabledAttack != null && selfDisabledAttack.equalsIgnoreCase(attackName)) {
            return new ValidationResult.Invalid(ATTACK_DISABLED_BY_EFFECT);
        }
        if (sem.isSelfDisabledNextTurn()) {
            return new ValidationResult.Invalid(ATTACK_DISABLED_BY_EFFECT);
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
        if (benchStateProvider.getBenchSize(playerIndex) >= MAX_BENCH_SIZE) {
            return new ValidationResult.Invalid("bench_full");
        }
        return new ValidationResult.Valid();
    }

    private static final java.util.Set<AbilityEffectId> ONCE_PER_TURN_ABILITIES = java.util.Set.of(
            AbilityEffectId.MYSTICAL_FIRE,
            AbilityEffectId.MAGNETIC_DRAW,
            AbilityEffectId.DRIVE_OFF,
            AbilityEffectId.WATER_SHURIKEN,
            AbilityEffectId.STANCE_CHANGE,
            AbilityEffectId.LEAF_DRAW,
            AbilityEffectId.ENERGY_GRACE,
            AbilityEffectId.BIG_JUMP,
            AbilityEffectId.UPSIDE_DOWN_EVOLUTION
    );

    private record AbilityContext(UseAbilityAction action, BattlePokemonState source,
            ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime, int playerIndex) {
    }

    /**
     * Validates one {@link AbilityEffectId}'s use-specific preconditions (e.g. Drive Off needs
     * the source active and the opponent's bench non-empty). Returns {@code null} when met.
     */
    @FunctionalInterface
    private interface AbilityValidator {
        ValidationResult.Invalid validate(AbilityContext ctx);
    }

    private final Map<AbilityEffectId, AbilityValidator> abilityValidators = buildAbilityValidators();

    private Map<AbilityEffectId, AbilityValidator> buildAbilityValidators() {
        final Map<AbilityEffectId, AbilityValidator> v = new java.util.EnumMap<>(AbilityEffectId.class);
        v.put(AbilityEffectId.GOOEY_REGENERATION, this::validateGooeyRegenerationAbility);
        v.put(AbilityEffectId.DRIVE_OFF, this::validateDriveOffAbility);
        v.put(AbilityEffectId.WATER_SHURIKEN, this::validateWaterShurikenAbility);
        v.put(AbilityEffectId.LEAF_DRAW, this::validateLeafDrawAbility);
        v.put(AbilityEffectId.ENERGY_GRACE, this::validateEnergyGraceAbility);
        v.put(AbilityEffectId.STANCE_CHANGE, this::validateStanceChangeAbility);
        v.put(AbilityEffectId.UPSIDE_DOWN_EVOLUTION, this::validateUpsideDownEvolutionAbility);
        v.put(AbilityEffectId.SHADOW_VOID, this::validateShadowVoidAbility);
        return v;
    }

    private ValidationResult validateUseAbility(final UseAbilityAction action, final int playerIndex) {
        turnManager.requireMainPhase();

        final BattlePokemonState source = action.source();
        if (source == null) {
            return new ValidationResult.Invalid(TARGET_POKEMON_REQUIRED);
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

        if (ONCE_PER_TURN_ABILITIES.contains(effId) && source.hasUsedAbilityThisTurn(effId.name())) {
            return new ValidationResult.Invalid("ability_already_used_this_turn");
        }

        final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime = getActiveStatusEffectManager(playerIndex).getPlayerRuntime();

        final AbilityValidator validator = abilityValidators.get(effId);
        if (validator != null) {
            final ValidationResult.Invalid invalid = validator.validate(new AbilityContext(action, source, runtime, playerIndex));
            if (invalid != null) {
                return invalid;
            }
        }

        return new ValidationResult.Valid();
    }

    private ValidationResult.Invalid validateGooeyRegenerationAbility(final AbilityContext ctx) {
        if (ctx.runtime() == null) {
            return new ValidationResult.Invalid(PLAYER_RUNTIME_REQUIRED);
        }
        if (ctx.source().getAttachedEnergyCards().isEmpty()) {
            return new ValidationResult.Invalid("no_energy_attached");
        }
        if (ctx.source().getDamageCounters() == 0) {
            return new ValidationResult.Invalid("no_damage_to_heal");
        }
        return null;
    }

    private ValidationResult.Invalid validateDriveOffAbility(final AbilityContext ctx) {
        if (ctx.runtime() == null || !ctx.source().equals(ctx.runtime().getActivePokemon())) {
            return new ValidationResult.Invalid("pokemon_must_be_active");
        }
        final int opponentIndex = 1 - ctx.playerIndex();
        if (benchStateProvider.getBenchSize(opponentIndex) == 0) {
            return new ValidationResult.Invalid("opponent_bench_empty");
        }
        return null;
    }

    private ValidationResult.Invalid validateWaterShurikenAbility(final AbilityContext ctx) {
        if (ctx.runtime() == null) {
            return new ValidationResult.Invalid(PLAYER_RUNTIME_REQUIRED);
        }
        final boolean hasWaterEnergy = ctx.runtime().getHand().getCards().stream()
                .anyMatch(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard ec && ec.getEnergyType() == PokemonType.WATER);
        if (!hasWaterEnergy) {
            return new ValidationResult.Invalid("water_energy_required_in_hand");
        }
        return null;
    }

    private ValidationResult.Invalid validateLeafDrawAbility(final AbilityContext ctx) {
        if (ctx.runtime() == null) {
            return new ValidationResult.Invalid(PLAYER_RUNTIME_REQUIRED);
        }
        final boolean hasGrassEnergy = ctx.runtime().getHand().getCards().stream()
                .anyMatch(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard ec && ec.getEnergyType() == PokemonType.GRASS);
        if (!hasGrassEnergy) {
            return new ValidationResult.Invalid("grass_energy_required_in_hand");
        }
        return null;
    }

    private ValidationResult.Invalid validateEnergyGraceAbility(final AbilityContext ctx) {
        if (ctx.runtime() == null) {
            return new ValidationResult.Invalid(PLAYER_RUNTIME_REQUIRED);
        }
        if (!hasBasicEnergyAvailable(ctx)) {
            return new ValidationResult.Invalid("basic_energy_required_in_discard");
        }
        if (!hasNonExTargetInPlay(ctx)) {
            return new ValidationResult.Invalid("no_valid_non_ex_target_in_play");
        }
        return validateEnergyGraceTarget(ctx);
    }

    private boolean hasBasicEnergyAvailable(final AbilityContext ctx) {
        return ctx.runtime().getDiscardPile().getCards().stream()
                .anyMatch(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.EnergyCard ec && ec.isBasic())
                || ctx.source().getAttachedEnergyCards().stream().anyMatch(ec -> ec.isBasic());
    }

    private boolean hasNonExTargetInPlay(final AbilityContext ctx) {
        final BattlePokemonState active = ctx.runtime().getActivePokemon();
        if (active != null && !active.equals(ctx.source()) && !active.isEx()) {
            return true;
        }
        for (final var benched : ctx.runtime().getBench().getAll()) {
            if (!benched.equals(ctx.source()) && !benched.isEx()) {
                return true;
            }
        }
        return false;
    }

    private ValidationResult.Invalid validateEnergyGraceTarget(final AbilityContext ctx) {
        final Integer targetIndex = ctx.action().targetIndex();
        final BattlePokemonState specificTarget;
        if (targetIndex == null || targetIndex == -1) {
            specificTarget = ctx.runtime().getActivePokemon();
        } else if (targetIndex >= 0 && targetIndex < ctx.runtime().getBench().getAll().size()) {
            specificTarget = ctx.runtime().getBench().getAll().get(targetIndex);
        } else {
            return new ValidationResult.Invalid("invalid_target_index");
        }

        if (specificTarget == null) {
            return new ValidationResult.Invalid(TARGET_POKEMON_REQUIRED);
        }
        if (specificTarget.equals(ctx.source())) {
            return new ValidationResult.Invalid("cannot_target_self");
        }
        if (specificTarget.isEx()) {
            return new ValidationResult.Invalid("cannot_target_ex_pokemon");
        }
        return null;
    }

    private ValidationResult.Invalid validateStanceChangeAbility(final AbilityContext ctx) {
        if (ctx.runtime() == null) {
            return new ValidationResult.Invalid(PLAYER_RUNTIME_REQUIRED);
        }
        final boolean hasAegislash = ctx.runtime().getHand().getCards().stream()
                .anyMatch(c -> "Aegislash".equalsIgnoreCase(c.getName()) && !c.getCardId().equals(ctx.source().getCardId()));
        if (!hasAegislash) {
            return new ValidationResult.Invalid("aegislash_required_in_hand");
        }
        return null;
    }

    private ValidationResult.Invalid validateUpsideDownEvolutionAbility(final AbilityContext ctx) {
        if (ctx.runtime() == null || !ctx.source().equals(ctx.runtime().getActivePokemon())) {
            return new ValidationResult.Invalid("pokemon_must_be_active");
        }
        if (!getActiveStatusEffectManager(ctx.playerIndex()).has(ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType.CONFUNDIDO)) {
            return new ValidationResult.Invalid("pokemon_must_be_confused");
        }
        return null;
    }

    private ValidationResult.Invalid validateShadowVoidAbility(final AbilityContext ctx) {
        if (ctx.action().targetIndex() == null) {
            return new ValidationResult.Invalid(TARGET_POKEMON_REQUIRED);
        }
        BattlePokemonState targetPokemon = ctx.action().targetIndex() < 0
                ? battlefieldProvider.getActivePokemon(ctx.playerIndex())
                : (benchStateProvider != null && ctx.action().targetIndex() < benchStateProvider.getBenchedPokemon(ctx.playerIndex()).size()
                    ? benchStateProvider.getBenchedPokemon(ctx.playerIndex()).get(ctx.action().targetIndex())
                    : null);

        if (targetPokemon == null) {
            return new ValidationResult.Invalid(TARGET_POKEMON_REQUIRED);
        }
        if (targetPokemon.getDamageCounters() * 10 < MIN_DAMAGE_FOR_SHADOW_VOID) {
            return new ValidationResult.Invalid("target_has_no_damage");
        }
        if (ctx.source().getDamageCounters() * 10 + 10 >= ctx.source().getMaxHp()) {
            return new ValidationResult.Invalid("dusknoir_max_hp_reached");
        }
        return null;
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
        final List<PokemonType> pool = new java.util.ArrayList<>(attacker.getAttachedEnergies());
        final List<Boolean> wildcard = buildWildcardFlags(attacker.getAttachedEnergyCards());

        int colorlessRequired = 0;
        for (final PokemonType required : attack.requiredEnergies()) {
            if (required == PokemonType.COLORLESS) {
                colorlessRequired++;
                continue;
            }
            if (!consumeMatchingEnergy(pool, wildcard, required)) {
                return false;
            }
        }
        return pool.size() >= colorlessRequired;
    }

    // Each entry tracks whether it is a wildcard (Rainbow Energy — provides all types); an
    // EnergyCard may contribute multiple aligned entries (e.g. Double Colorless = 2).
    private List<Boolean> buildWildcardFlags(final List<EnergyCard> energyCards) {
        final List<Boolean> wildcard = new java.util.ArrayList<>();
        for (final EnergyCard ec : energyCards) {
            for (int i = 0; i < ec.getEnergyCount(); i++) {
                wildcard.add(ec.isProvidesAllTypes());
            }
        }
        return wildcard;
    }

    // Prefers an exact-match (non-wildcard) energy; falls back to a wildcard (Rainbow Energy).
    private boolean consumeMatchingEnergy(final List<PokemonType> pool, final List<Boolean> wildcard, final PokemonType required) {
        for (int i = 0; i < pool.size(); i++) {
            if (!wildcard.get(i) && pool.get(i) == required) {
                pool.remove(i);
                wildcard.remove(i);
                return true;
            }
        }
        for (int i = 0; i < pool.size(); i++) {
            if (wildcard.get(i)) {
                pool.remove(i);
                wildcard.remove(i);
                return true;
            }
        }
        return false;
    }

    private record SelectionValidationContext(
            ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction action,
            ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest req, int playerIndex) {
    }

    /**
     * Validates one {@link ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId}'s expected
     * selection count/shape for a pending {@code SelectCardsAction}. Returns {@code null} when
     * the selection is acceptable.
     */
    @FunctionalInterface
    private interface SelectionCountValidator {
        ValidationResult.Invalid validate(SelectionValidationContext ctx);
    }

    private final Map<ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId, SelectionCountValidator> selectionCountValidators =
            buildSelectionCountValidators();

    private Map<ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId, SelectionCountValidator> buildSelectionCountValidators() {
        final Map<ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId, SelectionCountValidator> v =
                new java.util.EnumMap<>(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.class);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.SACRED_ASH, this::validateExactMaxSelection);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BLACKSMITH, this::validateExactMaxSelection);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FIERY_TORCH, this::validateExactMaxSelection);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.CLAIRVOYANT_EYE, this::validateExactMaxSelection);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.CURSED_DROP, this::validateExactMaxSelection);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.RESCUE, this::validateExactMaxSelection);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.ULTRA_BALL, this::validateUltraBallSelection);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BURIED_TREASURE_HUNT, this::validateBuriedTreasureHuntSelection);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.DUAL_BULLET, this::validateDualBulletSelection);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.PAIN_PELLETS, ctx -> validateExactCountSelection(ctx.action(), 1));
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BENCH_DAMAGE_ONE, ctx -> validateExactCountSelection(ctx.action(), 1));
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BOUNCE, ctx -> validateExactCountSelection(ctx.action(), 1));
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.EAR_INFLUENCE, this::validateEarInfluenceSelection);
        v.put(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.FANG_SNIPE, this::validateFangSnipeSelection);
        return v;
    }

    private ValidationResult validateSelectCards(final ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction action, final int playerIndex) {
        if (!(turnManager.currentPhase() instanceof ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase)) {
            return new ValidationResult.Invalid("wrong_phase_for_selection");
        }

        final ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest req = action.request();
        if (req == null) {
            // Zone and Type validation is deferred to GameFacade because RuleValidator lacks Deck/Discard access.
            return new ValidationResult.Valid();
        }
        if (action.cardIds().size() > req.maxSelections()) {
            return new ValidationResult.Invalid("too_many_cards_selected");
        }

        final SelectionCountValidator validator = selectionCountValidators.get(req.sourceEffect());
        if (validator != null) {
            final ValidationResult.Invalid invalid = validator.validate(new SelectionValidationContext(action, req, playerIndex));
            if (invalid != null) {
                return invalid;
            }
        }

        return new ValidationResult.Valid();
    }

    private ValidationResult.Invalid validateExactCountSelection(
            final ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction action, final int expected) {
        if (action.cardIds().size() != expected) {
            return new ValidationResult.Invalid(MUST_SELECT_EXACT_AMOUNT);
        }
        return null;
    }

    private ValidationResult.Invalid validateExactMaxSelection(final SelectionValidationContext ctx) {
        return validateExactCountSelection(ctx.action(), ctx.req().maxSelections());
    }

    private ValidationResult.Invalid validateUltraBallSelection(final SelectionValidationContext ctx) {
        if (ctx.req().source() != ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND) {
            return null;
        }
        return validateExactCountSelection(ctx.action(), 2);
    }

    private ValidationResult.Invalid validateBuriedTreasureHuntSelection(final SelectionValidationContext ctx) {
        final int deckSize = getDeckSize(ctx.playerIndex());
        return validateExactCountSelection(ctx.action(), Math.min(2, deckSize));
    }

    private ValidationResult.Invalid validateDualBulletSelection(final SelectionValidationContext ctx) {
        final int opponentIndex = 1 - ctx.playerIndex();
        final int opponentBenchCount = benchStateProvider != null ? benchStateProvider.getBenchSize(opponentIndex) : 0;
        final BattlePokemonState opponentActive = battlefieldProvider != null ? battlefieldProvider.getActivePokemon(opponentIndex) : null;
        final int opponentPokemonCount = (opponentActive != null ? 1 : 0) + opponentBenchCount;
        return validateExactCountSelection(ctx.action(), Math.min(2, opponentPokemonCount));
    }

    private ValidationResult.Invalid validateEarInfluenceSelection(final SelectionValidationContext ctx) {
        final int size = ctx.action().cardIds().size();
        if (size % 2 != 0 || size > ctx.req().maxSelections()) {
            return new ValidationResult.Invalid("must_select_pairs");
        }
        return null;
    }

    private ValidationResult.Invalid validateFangSnipeSelection(final SelectionValidationContext ctx) {
        final int opponentIndex = 1 - turnManager.activePlayerIndex();
        final boolean opponentHasTrainer = handStateProvider.getHandCards(opponentIndex).stream()
                .anyMatch(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.TrainerCard);
        final int expectedAmount = opponentHasTrainer ? ctx.req().maxSelections() : 0;
        if (ctx.action().cardIds().size() != expectedAmount) {
            return new ValidationResult.Invalid(MUST_SELECT_EXACT_AMOUNT);
        }
        if (opponentHasTrainer && !ctx.action().cardIds().isEmpty()) {
            final String cardId = ctx.action().cardIds().get(0);
            final java.util.Optional<Card> optCard = handStateProvider.getCardInHand(opponentIndex, cardId);
            if (optCard.isEmpty() || !(optCard.get() instanceof ar.edu.utn.frc.tup.piii.engine.model.TrainerCard)) {
                return new ValidationResult.Invalid("must_select_trainer_card");
            }
        }
        return null;
    }

    private boolean opponentHasAbility(final int playerIndex, final AbilityEffectId abilityId) {
        final int opponentIndex = 1 - playerIndex;
        if (battlefieldProvider != null) {
            final BattlePokemonState active = battlefieldProvider.getActivePokemon(opponentIndex);
            if (active != null && hasAbility(active, abilityId)) {
                return true;
            }
        }
        if (benchStateProvider != null) {
            for (final BattlePokemonState benched : benchStateProvider.getBenchedPokemon(opponentIndex)) {
                if (benched != null && hasAbility(benched, abilityId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasAbility(final BattlePokemonState pokemon, final AbilityEffectId abilityId) {
        return pokemon != null && pokemon.getAbilities().stream().anyMatch(a -> a.effectId() == abilityId);
    }
}
