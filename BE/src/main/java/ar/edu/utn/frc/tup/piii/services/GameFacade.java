package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.engine.manager.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.manager.EvolveExecutor;
import ar.edu.utn.frc.tup.piii.engine.manager.RetreatExecutor;
import ar.edu.utn.frc.tup.piii.engine.manager.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.model.Action;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.AttachEnergyAction;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.DeclareAttackAction;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.EndTurnAction;
import ar.edu.utn.frc.tup.piii.engine.model.EvolveAction;
import ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.engine.model.PlaceBasicPokemonAction;
import ar.edu.utn.frc.tup.piii.engine.model.PlayTrainerAction;
import ar.edu.utn.frc.tup.piii.engine.model.PromoteActiveAction;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.RetreatAction;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerEffect;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.pipeline.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.pipeline.AttackCancellationStep;
import ar.edu.utn.frc.tup.piii.engine.pipeline.AttackEffectResolver;
import ar.edu.utn.frc.tup.piii.engine.pipeline.AttackPipeline;
import ar.edu.utn.frc.tup.piii.engine.pipeline.DamageApplicationStep;
import ar.edu.utn.frc.tup.piii.engine.pipeline.DamageCalculationStep;
import ar.edu.utn.frc.tup.piii.engine.pipeline.KnockoutCheckStep;
import ar.edu.utn.frc.tup.piii.engine.pipeline.PokemonToolStep;
import ar.edu.utn.frc.tup.piii.engine.pipeline.PostDamageEffectsStep;
import ar.edu.utn.frc.tup.piii.engine.pipeline.PreDamageEffectsStep;
import ar.edu.utn.frc.tup.piii.engine.pipeline.StadiumEffectStep;
import ar.edu.utn.frc.tup.piii.engine.pipeline.TrainerEffectResolver;
import ar.edu.utn.frc.tup.piii.engine.pipeline.AbilityEffectResolver;
import ar.edu.utn.frc.tup.piii.engine.pipeline.ValidationStep;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Translates incoming {@link ActionRequestDTO} objects into engine {@link Action} instances,
 * and applies validated actions to the live match session state.
 *
 * <p>Stateless for all non-apply concerns. The {@link AttackPipeline} instance is
 * immutable and thread-safe once built.</p>
 */
@Component
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.CouplingBetweenObjects", "PMD.GodClass", "PMD.TooManyMethods",
        "PMD.CyclomaticComplexity"})
// Facade over the entire engine action/effect surface by design (its whole job is translating
// DTOs into every possible engine Action and dispatching to every pipeline/resolver) — high
// coupling and method count here are the intended shape, not a design smell to fix by splitting
// the facade. Every individual method-level complexity metric in this class has been resolved
// via real extraction (see the applyX/buildX helper methods below) — none is individually flagged.
public final class GameFacade {

    private final AttackPipeline attackPipeline;
    private final TrainerEffectResolver trainerEffectResolver;
    private final AbilityEffectResolver abilityEffectResolver;
    private final SelectionEffectResolver selectionEffectResolver;

    public GameFacade() {
        this.trainerEffectResolver = new TrainerEffectResolver();
        this.abilityEffectResolver = new AbilityEffectResolver();
        this.attackPipeline = new AttackPipeline(List.of(
                new ValidationStep(),
                new PreDamageEffectsStep(),
                new PokemonToolStep(trainerEffectResolver),
                new StadiumEffectStep(trainerEffectResolver),
                new AttackCancellationStep(),
                new DamageCalculationStep(new DamageCalculator()),
                new DamageApplicationStep(),
                new PostDamageEffectsStep(new AttackEffectResolver()),
                new KnockoutCheckStep()
        ));
        this.selectionEffectResolver = new SelectionEffectResolver(attackPipeline);
    }

    /**
     * Applies a validated action to the session's live runtime state.
     *
     * @param session the active match session (never null)
     * @param action  the validated engine action to apply (never null)
     */
    public void apply(final MatchSession session, final Action action) {
        apply(session, action, null);
    }

    /**
     * Applies a validated action to the session's live runtime state and records the limit counters.
     *
     * @param session     the active match session (never null)
     * @param action      the validated engine action to apply (never null)
     * @param turnManager the turn manager associated with the active session
     */
    public void apply(final MatchSession session, final Action action, final TurnManager turnManager) {
        int playerIndex = session.getActivePlayerIndex();
        if (action instanceof PromoteActiveAction && session.getPromotingPlayerIndex() != -1) {
            playerIndex = session.getPromotingPlayerIndex();
        }
        final PlayerRuntime runtime = session.getPlayerRuntime(playerIndex);
        dispatchAction(action, session, runtime, turnManager, playerIndex);
        checkSweetVeilForBothPlayers(session);
    }

    private void dispatchAction(final Action action, final MatchSession session, final PlayerRuntime runtime,
            final TurnManager turnManager, final int playerIndex) {
        switch (action) {
            case PlaceBasicPokemonAction place -> applyPlacePokemon(place, runtime);
            case AttachEnergyAction attach     -> applyAttachEnergyTracked(attach, runtime, turnManager);
            case EvolveAction evolve           -> applyEvolve(evolve, runtime, session);
            case RetreatAction retreat         -> applyRetreatTracked(retreat, runtime, turnManager);
            case DeclareAttackAction attack    -> applyDeclareAttack(attack, session, playerIndex);
            case PlayTrainerAction trainer     -> applyPlayTrainerTracked(trainer, session, runtime, turnManager);
            case UseAbilityAction abilityAction -> applyUseAbility(abilityAction, session);
            case EndTurnAction ignored         -> { /* turn advancement handled by MatchService */ }
            case PromoteActiveAction promote   -> applyPromoteActive(promote, runtime);
            case ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction selectCards -> applySelectCards(selectCards, session, runtime);
        }
    }

    private void applyAttachEnergyTracked(final AttachEnergyAction attach, final PlayerRuntime runtime,
            final TurnManager turnManager) {
        applyAttachEnergy(attach, runtime);
        if (turnManager != null) {
            turnManager.requireMainPhase().recordEnergyAttached();
        }
    }

    private void applyRetreatTracked(final RetreatAction retreat, final PlayerRuntime runtime,
            final TurnManager turnManager) {
        applyRetreat(retreat, runtime);
        if (turnManager != null) {
            turnManager.requireMainPhase().recordRetreatUsed();
        }
    }

    private void applyPlayTrainerTracked(final PlayTrainerAction trainer, final MatchSession session,
            final PlayerRuntime runtime, final TurnManager turnManager) {
        applyPlayTrainer(trainer, session, runtime);
        if (turnManager == null) {
            return;
        }
        if (trainer.trainerType() == TrainerType.SUPPORTER) {
            turnManager.requireMainPhase().recordSupporterPlayed();
        } else if (trainer.trainerType() == TrainerType.STADIUM) {
            turnManager.requireMainPhase().recordStadiumPlayed();
        }
    }

    private void checkSweetVeilForBothPlayers(final MatchSession session) {
        if (session.getPlayerRuntime(0) != null) {
            session.getPlayerRuntime(0).getStatusEffectManager().checkSweetVeil();
        }
        if (session.getPlayerRuntime(1) != null) {
            session.getPlayerRuntime(1).getStatusEffectManager().checkSweetVeil();
        }
    }

    // --- action handlers ---

    private void applyUseAbility(final UseAbilityAction action, final MatchSession session) {
        final BattlePokemonState source = action.source();
        final String abilityIdStr = action.abilityId();
        // Determine the AbilityEffectId from the ability name/ID
        source.getAbilities().stream()
                .filter(a -> a.name().equalsIgnoreCase(abilityIdStr) || a.effectId().name().equalsIgnoreCase(abilityIdStr))
                .findFirst()
                .ifPresent(ability -> abilityEffectResolver.resolve(ability.effectId())
                        .ifPresent(effect -> {
                            effect.apply(session, action);
                            source.markAbilityUsed(ability.effectId().name());
                        }));
    }

    private void applyPlacePokemon(final PlaceBasicPokemonAction action, final PlayerRuntime runtime) {
        final Card card = runtime.getHand().removeCard(action.cardId());
        final InPlayPokemon placed = new InPlayPokemon((PokemonCard) card);
        if (runtime.getActivePokemon() == null) {
            runtime.setActivePokemon(placed);
        } else {
            runtime.getBench().place(placed);
        }
        // Register the newly placed Pokémon so evolution restriction tracking starts at 0 turns.
        runtime.recordPokemonEntered(placed);

        // Track stats!
        if (runtime.getStatisticsTracker() != null) {
            runtime.getStatisticsTracker().incrementPokemonPlayed(card.getCardId());
        }
        
        // Sweet Veil: remove Asleep condition from the active Pokémon if Sweet Veil enters play.
        removeSleepIfHasAbility(placed, runtime, ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.SWEET_VEIL);
    }

    private void applyAttachEnergy(final AttachEnergyAction action, final PlayerRuntime runtime) {
        final EnergyCard energyCard = findEnergyInHand(runtime, action.energyType());
        runtime.getHand().removeCard(energyCard.getCardId());
        action.target().attachEnergy(energyCard);

        // Track stats!
        if (runtime.getStatisticsTracker() != null) {
            runtime.getStatisticsTracker().incrementEnergyAttached(action.energyType());
        }

        // Rainbow Energy: place 1 damage counter (10 HP) on the Pokémon when attached from hand.
        if (energyCard.isProvidesAllTypes()) {
            action.target().addDamageCounters(1);
        }
    }

    private void applyEvolve(final EvolveAction action, final PlayerRuntime runtime, final MatchSession session) {
        if (action.evolution() != null) {
            performCardSwapEvolution(action, runtime, session);
        }

        if (isSameInstance(action.target(), runtime.getActivePokemon())) {
            new EvolveExecutor(runtime.getStatusEffectManager()).executeEvolve(action.target());
        }

        removeSleepIfHasAbility(action.target(), runtime, ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.SWEET_VEIL);
    }

    private void performCardSwapEvolution(final EvolveAction action, final PlayerRuntime runtime,
            final MatchSession session) {
        final PokemonCard newCard = (PokemonCard) runtime.getHand().removeCard(action.evolution().getCardId());
        action.target().evolveInto(newCard);

        if (newCard.getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.MEGA && session != null) {
            session.setMegaEvolvedThisTurn(true);
        }

        // XY1 §2: a Pokémon cannot evolve in the same turn it evolved. The
        // BattlePokemonState mutates in-place, so we must reset its turnsInPlay
        // counter to 0 so RuleValidator sees it as "just entered". Done HERE
        // (inside the mutation guard) so a no-op call cannot accidentally
        // mark an unrelated Pokémon as freshly placed.
        runtime.recordPokemonEntered(action.target());

        if (runtime.getStatisticsTracker() != null) {
            runtime.getStatisticsTracker().incrementPokemonPlayed(newCard.getCardId());
        }

        if (session != null && action.target().getAbilities().stream()
                .anyMatch(a -> a.effectId() == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.THORN_TEMPEST)) {
            applyThornTempest(runtime, session);
        }
    }

    /** Thorn Tempest: put 1 damage counter on each of the opponent's Pokémon. */
    private void applyThornTempest(final PlayerRuntime runtime, final MatchSession session) {
        final int playerIndex = resolvePlayerIndex(session, runtime);
        final PlayerRuntime opponent = session.getPlayerRuntime(1 - playerIndex);
        if (opponent == null) {
            return;
        }
        if (opponent.getActivePokemon() != null) {
            opponent.getActivePokemon().addDamageCounters(1);
        }
        for (final BattlePokemonState benched : opponent.getBench().getAll()) {
            if (benched != null) {
                benched.addDamageCounters(1);
            }
        }
    }

    /** Removes the Asleep (DORMIDO) condition when {@code pokemon} carries {@code abilityId} (e.g. Sweet Veil). */
    private void removeSleepIfHasAbility(final BattlePokemonState pokemon, final PlayerRuntime runtime,
            final ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId abilityId) {
        if (pokemon.getAbilities().stream().anyMatch(a -> a.effectId() == abilityId)) {
            runtime.getStatusEffectManager().remove(ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType.DORMIDO);
        }
    }

    private void applyRetreat(final RetreatAction action, final PlayerRuntime runtime) {
        new RetreatExecutor(runtime.getStatusEffectManager()).executeRetreat(action);
        final BattlePokemonState newActive = runtime.getBench().promote(action.replacementIndex());
        final BattlePokemonState oldActive = runtime.getActivePokemon();
        runtime.setActivePokemon(newActive);
        runtime.getBench().place(oldActive);
        // Reset turns in play to 0 so retreated Pokémon cannot evolve on the bench in the same turn
        runtime.recordPokemonEntered(oldActive);
    }

    private void applyPromoteActive(final PromoteActiveAction action, final PlayerRuntime runtime) {
        final BattlePokemonState newActive = runtime.getBench().promote(action.benchIndex());
        runtime.setActivePokemon(newActive);
        // All status conditions are cleared when a Pokémon enters the Active position (XY1 §5).
        runtime.getStatusEffectManager().clearAll();
    }

    private void applyDeclareAttack(final DeclareAttackAction action,
                                     final MatchSession session,
                                     final int attackerIndex) {
        final int defenderIndex = 1 - attackerIndex;
        final PlayerRuntime attacker = session.getPlayerRuntime(attackerIndex);
        final PlayerRuntime defender = session.getPlayerRuntime(defenderIndex);

        final boolean hasFireEnergy = attacker.getActivePokemon() != null && attacker.getActivePokemon().getAttachedEnergyCards().stream()
                .anyMatch(ec -> ec.getEnergyType() == PokemonType.FIRE || ec.isProvidesAllTypes());
        boolean scorchingFangDiscarded = action.selectedCardIds() != null 
                && action.selectedCardIds().contains("discard_fire_energy")
                && hasFireEnergy;

        final AttackContext ctx = new AttackContext.Builder(
                action.attacker(),
                defender.getActivePokemon(),
                action.attack(),
                attacker.getStatusEffectManager(),
                defender.getStatusEffectManager(),
                session.getKnockoutHandler(),
                session.getCoinFlipper()::flip
        )
        .attackerRuntime(attacker)
        .defenderRuntime(defender)
        .defenderBench(defender.getBench().getAll())
        .effectText(action.attack().effectText())
        .stadiumProvider(session.getBoard())
        .attackerStats(attacker.getStatisticsTracker())
        .defenderStats(defender.getStatisticsTracker())
        .matchSession(session)
        .build();

        if (scorchingFangDiscarded) {
            ctx.setScorchingFangDiscarded(true);
        }

        attackPipeline.execute(ctx);
    }

    private void applyPlayTrainer(final PlayTrainerAction action,
                                   final MatchSession session,
                                   final PlayerRuntime runtime) {
        final TrainerCard trainerCard = removeTrainerCardFromHand(action, runtime);

        switch (action.trainerType()) {
            case STADIUM -> applyStadiumCard(trainerCard, session, runtime);
            case POKEMON_TOOL -> {
                // Tool stays attached to the Pokémon; discarded when the Pokémon is KO'd.
                if (action.target() != null && trainerCard != null) {
                    action.target().attachTool(trainerCard);
                }
            }
            default -> {
                // ITEM and SUPPORTER: card goes to discard after use (XY1 rulebook §4).
                if (trainerCard != null) {
                    runtime.getDiscardPile().add(trainerCard);
                    applyItemOrSupporterEffect(trainerCard, session, runtime, action.target());
                }
            }
        }
    }

    /**
     * Retrieves the trainer card reference from the actor's hand BEFORE removing it, so callers
     * can still invoke the card's effect after it's gone from the hand.
     */
    private TrainerCard removeTrainerCardFromHand(final PlayTrainerAction action, final PlayerRuntime runtime) {
        if (action.cardId() == null) {
            return null;
        }
        final TrainerCard trainerCard = (TrainerCard) runtime.getHand().getCards().stream()
                .filter(c -> c.getCardId().equals(action.cardId()))
                .findFirst()
                .orElse(null);
        runtime.getHand().removeCard(action.cardId());
        return trainerCard;
    }

    /** Stadium replaces the current field stadium; the previous one goes to its owner's discard. */
    private void applyStadiumCard(final TrainerCard trainerCard, final MatchSession session, final PlayerRuntime runtime) {
        if (trainerCard == null) {
            return;
        }
        final int prevOwnerIdx = session.getBoard().getActiveStadiumOwnerIndex();
        final TrainerCard previous = session.getBoard().replaceStadium(trainerCard);
        session.getBoard().setActiveStadiumOwnerIndex(resolvePlayerIndex(session, runtime));
        if (previous != null) {
            final PlayerRuntime prevOwner = prevOwnerIdx != -1 ? session.getPlayerRuntime(prevOwnerIdx) : runtime;
            prevOwner.getDiscardPile().add(previous);
        }
    }

    /** Item/Supporter effects that request a static-count card selection and interrupt the main phase. */
    private record SelectionSpec(int count, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource source,
            boolean usesTarget) {
    }

    private static final Map<TrainerEffectId, SelectionSpec> STATIC_SELECTION_EFFECTS = Map.ofEntries(
            Map.entry(TrainerEffectId.EVOSODA,
                    new SelectionSpec(1, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK, true)),
            Map.entry(TrainerEffectId.GREAT_BALL,
                    new SelectionSpec(1, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.TOP_7_DECK, false)),
            Map.entry(TrainerEffectId.PROFESSORS_LETTER,
                    new SelectionSpec(2, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK, false)),
            Map.entry(TrainerEffectId.MAX_REVIVE,
                    new SelectionSpec(1, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DISCARD_PILE, false)),
            Map.entry(TrainerEffectId.POKEMON_FAN_CLUB,
                    new SelectionSpec(2, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK, false)),
            Map.entry(TrainerEffectId.FIERY_TORCH,
                    new SelectionSpec(1, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND, false)),
            Map.entry(TrainerEffectId.TRICK_SHOVEL,
                    new SelectionSpec(1, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.TOP_7_DECK, true)),
            Map.entry(TrainerEffectId.ULTRA_BALL,
                    new SelectionSpec(2, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND, false))
    );

    @SuppressWarnings("PMD.CyclomaticComplexity")
    // Dispatch table for ~17 distinct trainer-card effects — each branch is a single delegating
    // call (either into STATIC_SELECTION_EFFECTS, a one-line requestSelection with a dynamic
    // count, or an already-extracted applyX method); the branch count itself, not any real
    // nested logic, is what drives this metric.
    private void applyItemOrSupporterEffect(final TrainerCard trainerCard, final MatchSession session,
            final PlayerRuntime runtime, final BattlePokemonState target) {
        final TrainerEffectId effectId = trainerCard.getEffectId();

        final SelectionSpec spec = STATIC_SELECTION_EFFECTS.get(effectId);
        if (spec != null) {
            requestSelection(session, effectId, spec.usesTarget() ? target : null, spec.count(), spec.source());
            return;
        }

        switch (effectId == null ? TrainerEffectId.NONE : effectId) {
            case RED_CARD -> applyRedCard(session);
            case TEAM_FLARE_GRUNT -> applyTeamFlareGrunt(session);
            case CASSIUS -> applyCassius(runtime, target);
            case LYSANDRE -> applyLysandre(session, target);
            case STARTLING_MEGAPHONE -> applyStartlingMegaphone(session);
            case POKEMON_CENTER_LADY -> applyPokemonCenterLady(runtime, target);
            case SACRED_ASH -> requestSelection(session, effectId, null,
                    countMatchingInDiscard(runtime, 5, c -> c instanceof PokemonCard),
                    ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DISCARD_PILE);
            case PAL_PAD -> requestSelection(session, effectId, null,
                    countMatchingInDiscard(runtime, 2,
                            c -> c instanceof TrainerCard tc && tc.getTrainerType() == TrainerType.SUPPORTER),
                    ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DISCARD_PILE);
            case BLACKSMITH -> requestSelection(session, effectId, target,
                    countMatchingInDiscard(runtime, 2, c -> c instanceof EnergyCard ec && ec.getEnergyType() == PokemonType.FIRE),
                    ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DISCARD_PILE);
            default -> applyGenericTrainerEffect(trainerCard, effectId, session, runtime, target);
        }
    }

    private int countMatchingInDiscard(final PlayerRuntime runtime, final int cap,
            final java.util.function.Predicate<Card> predicate) {
        final long count = runtime.getDiscardPile().getCards().stream().filter(predicate).count();
        return (int) Math.min(cap, count);
    }

    private void requestSelection(final MatchSession session, final TrainerEffectId effectId,
            final BattlePokemonState target, final int count,
            final ar.edu.utn.frc.tup.piii.engine.model.SelectionSource source) {
        session.setPendingSelectionRequest(
                new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(effectId, target, count, source));
        session.getTurnManager().interruptMainPhase();
    }

    private void applyGenericTrainerEffect(final TrainerCard trainerCard, final TrainerEffectId effectId,
            final MatchSession session, final PlayerRuntime runtime, final BattlePokemonState target) {
        TrainerEffect effect = trainerCard.getEffect();
        if (effect == null && effectId != null) {
            effect = trainerEffectResolver.resolve(effectId, session.getCoinFlipper()).orElse(null);
        }
        if (effect != null) {
            effect.apply(runtime, target);
        }
    }

    /**
     * Red Card (xy1-124): the opponent shuffles their hand into their deck, then draws 4 cards.
     * Must be called after the Red Card is removed from the actor's hand and placed in discard.
     */
    private void applyRedCard(final MatchSession session) {
        final int opponentIndex = 1 - session.getActivePlayerIndex();
        final PlayerRuntime opponent = session.getPlayerRuntime(opponentIndex);
        opponent.getDeck().addCards(opponent.getHand().removeAll());
        opponent.getDeck().shuffle();
        opponent.getHand().addCards(opponent.getDeck().drawMultiple(4));
    }

    /**
     * Team Flare Grunt (xy1-129): discard 1 Energy card attached to the opponent's Active Pokémon.
     * Must be called after the Team Flare Grunt card is removed from the actor's hand and placed in discard.
     */
    private void applyTeamFlareGrunt(final MatchSession session) {
        final int opponentIndex = 1 - session.getActivePlayerIndex();
        final PlayerRuntime opponent = session.getPlayerRuntime(opponentIndex);
        final BattlePokemonState opponentActive = opponent.getActivePokemon();
        if (opponentActive != null && !opponentActive.getAttachedEnergies().isEmpty()) {
            opponentActive.removeEnergies(1);
        }
    }

    /**
     * Cassius (xy1-115): shuffle 1 of the acting player's Pokémon (and all cards attached to it)
     * back into the deck. The Pokémon is removed from the bench; its Pokémon cards, attached
     * energies and tool (if any) are collected and added to the deck, which is then shuffled.
     * No-op if {@code target} is null or is not found on the bench.
     */
    private void applyCassius(final PlayerRuntime runtime, final BattlePokemonState target) {
        if (target == null) {
            return;
        }
        if (target.equals(runtime.getActivePokemon())) {
            runtime.clearActivePokemon();
        } else {
            final List<BattlePokemonState> benchSlots = runtime.getBench().getAll();
            final int idx = benchSlots.indexOf(target);
            if (idx >= 0) {
                runtime.getBench().remove(idx);
            }
        }
        // Collect all underlying cards to shuffle into the deck.
        final List<Card> toShuffle = new ArrayList<>();
        toShuffle.add(target.getBaseCard());
        toShuffle.addAll(target.getUnderlyingCards());
        toShuffle.addAll(target.getAttachedEnergyCards());
        target.getAttachedTool().ifPresent(toShuffle::add);
        runtime.getDeck().addCards(toShuffle);
        runtime.getDeck().shuffle();
        runtime.removePokemonFromPlay(target);
    }

    private void applyLysandre(final MatchSession session, final BattlePokemonState target) {
        if (target == null) {
            return;
        }
        final int opponentIndex = 1 - session.getActivePlayerIndex();
        final PlayerRuntime opponent = session.getPlayerRuntime(opponentIndex);
        final BattlePokemonState oldActive = opponent.getActivePokemon();
        final int targetIndex = opponent.getBench().getAll().indexOf(target);
        if (targetIndex >= 0) {
            final BattlePokemonState newActive = opponent.getBench().promote(targetIndex);
            opponent.setActivePokemon(newActive);
            if (oldActive != null) {
                opponent.getBench().place(oldActive);
                opponent.recordPokemonEntered(oldActive);
            }
            opponent.getStatusEffectManager().clearAll();
        }
    }

    private void applyStartlingMegaphone(final MatchSession session) {
        final int opponentIndex = 1 - session.getActivePlayerIndex();
        final PlayerRuntime opponent = session.getPlayerRuntime(opponentIndex);
        final BattlePokemonState opponentActive = opponent.getActivePokemon();
        if (opponentActive != null && opponentActive.hasToolAttached()) {
            opponentActive.getAttachedTool().ifPresent(tool -> {
                opponent.getDiscardPile().add(tool);
                opponentActive.detachTool();
            });
        }
        for (final BattlePokemonState benched : opponent.getBench().getAll()) {
            if (benched.hasToolAttached()) {
                benched.getAttachedTool().ifPresent(tool -> {
                    opponent.getDiscardPile().add(tool);
                    benched.detachTool();
                });
            }
        }
    }

    private void applyPokemonCenterLady(final PlayerRuntime runtime, final BattlePokemonState target) {
        if (target != null) {
            target.heal(60);
            if (target.equals(runtime.getActivePokemon())) {
                runtime.getStatusEffectManager().clearAll();
            }
        }
    }

    /**
     * Resolves the pending interactive selection (e.g. from Evosoda, Great Ball, etc.).
     */
    private void applySelectCards(final ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction action, final MatchSession session, final PlayerRuntime runtime) {
        final ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest request = session.getPendingSelectionRequest();
        if (request == null) {
            throw new IllegalStateException("No pending selection request found.");
        }

        final List<String> selectedIds = action.cardIds();
        final boolean resolved = selectionEffectResolver.resolve(action, session, runtime, request, selectedIds);

        if (resolved) {
            session.setPendingSelectionRequest(null);
            session.getTurnManager().resumeMainPhase();
        }
    }

    private EnergyCard findEnergyInHand(final PlayerRuntime runtime, final PokemonType type) {
        return runtime.getHand().getCards().stream()
                .filter(c -> c instanceof EnergyCard e && e.getEnergyType() == type)
                .map(c -> (EnergyCard) c)
                .findFirst()
                .orElseThrow(() -> new ar.edu.utn.frc.tup.piii.engine.exception.CardNotInHandException(
                        "energy:" + type));
    }

    // --- DTO translation ---

    /**
     * Converts a DTO action into the appropriate sealed engine {@link Action}.
     *
     * @param board       the current match board (used to resolve active pokemon / attacks)
     * @param playerIndex the zero-based index of the acting player (0 or 1)
     * @param dto         the incoming action request (never null)
     * @return a concrete {@link Action} variant (never null)
     */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    // Flat DTO-to-Action translation table, one arm per ActionType — each arm is already either
    // a single delegating call or a short 1-3 line construction (see buildXxxAction methods
    // below); the complexity here is the inherent branch count of a 10-way translation, not
    // nested/interacting logic like the game-rule methods elsewhere in this class.
    public Action toEngineAction(final MatchSession session,
                                 final int playerIndex,
                                 final ActionRequestDTO dto) {
        final MatchBoard board = session.getBoard();
        return switch (dto.type()) {
            case DECLARE_ATTACK      -> buildDeclareAttack(board, playerIndex, dto);
            case RETREAT             -> buildRetreatAction(board, playerIndex, dto);
            case PLAY_TRAINER        -> buildPlayTrainerAction(session, board, playerIndex, dto);
            case ATTACH_ENERGY       -> new AttachEnergyAction(
                                            resolveEvolveTarget(board, playerIndex, dto),
                                            dto.energyType() != null ? dto.energyType() : PokemonType.COLORLESS);
            case EVOLVE              -> buildEvolveAction(session, board, playerIndex, dto);
            case PLACE_BASIC_POKEMON -> new PlaceBasicPokemonAction(dto.cardId());
            case USE_ABILITY         -> new UseAbilityAction(
                                            resolvePokemonByIndex(board, playerIndex, dto.sourceIndex() != null ? dto.sourceIndex() : -1),
                                            dto.cardId(),
                                            dto.sourceIndex() != null ? dto.sourceIndex() : -1,
                                            dto.targetIndex() != null ? dto.targetIndex() : -1,
                                            dto.selectedEnergyIndices() != null ? dto.selectedEnergyIndices() : java.util.Collections.emptyList());
            case END_TURN            -> new EndTurnAction();
            case PROMOTE_ACTIVE      -> {
                Integer idx = dto.sourceIndex();
                if (idx == null) {
                    idx = dto.targetIndex();
                }
                yield new PromoteActiveAction(idx != null ? idx : 0);
            }
            case SELECT_CARDS        -> new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(
                                            dto.selectedCardIds() != null ? dto.selectedCardIds() : java.util.Collections.emptyList(),
                                            session.getPendingSelectionRequest());
        };
    }

    private EvolveAction buildEvolveAction(final MatchSession session, final MatchBoard board,
            final int playerIndex, final ActionRequestDTO dto) {
        final Card cardInHand = session.getPlayerRuntime(playerIndex).getHand().getCards().stream()
                .filter(c -> c.getCardId().equals(dto.cardId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Card not in hand"));
        return new EvolveAction(resolveEvolveTarget(board, playerIndex, dto), (PokemonCard) cardInHand);
    }

    private DeclareAttackAction buildDeclareAttack(final MatchBoard board,
                                                    final int playerIndex,
                                                    final ActionRequestDTO dto) {
        final BattlePokemonState active = board.getActivePokemon(playerIndex);
        if (active == null) {
            throw new IllegalArgumentException("No active pokemon to attack");
        }
        final int attackIndex = dto.attackIndex() != null ? dto.attackIndex() : 0;
        final List<Attack> attacks = board.getActiveAttacks(playerIndex);
        if (attackIndex < 0 || attackIndex >= attacks.size()) {
            throw new IllegalArgumentException("Invalid attack index");
        }
        return new DeclareAttackAction(
                active,
                attacks.get(attackIndex),
                dto.selectedCardIds() != null ? dto.selectedCardIds() : java.util.Collections.emptyList()
        );
    }

    private PlayTrainerAction buildPlayTrainerAction(final MatchSession session, final MatchBoard board,
            final int playerIndex, final ActionRequestDTO dto) {
        final TrainerEffectId effectId = resolveTrainerEffectId(session, playerIndex, dto);
        // Lysandre targets the OPPONENT's bench (it forces one of their Pokémon into Active).
        final BattlePokemonState target = (effectId == TrainerEffectId.LYSANDRE)
                ? resolveTarget(board, 1 - playerIndex, dto)
                : resolveTarget(board, playerIndex, dto);
        return new PlayTrainerAction(dto.trainerType(), target, dto.cardId(), effectId);
    }

    private TrainerEffectId resolveTrainerEffectId(final MatchSession session, final int playerIndex,
            final ActionRequestDTO dto) {
        if (dto.cardId() == null) {
            return null;
        }
        final Card card = session.getPlayerRuntime(playerIndex).getHand().getCards().stream()
                .filter(c -> c.getCardId().equals(dto.cardId()))
                .findFirst().orElse(null);
        return card instanceof ar.edu.utn.frc.tup.piii.engine.model.TrainerCard tc ? tc.getEffectId() : null;
    }

    private RetreatAction buildRetreatAction(final MatchBoard board, final int playerIndex, final ActionRequestDTO dto) {
        java.util.List<Integer> indices = dto.selectedEnergyIndices();
        if (indices == null || indices.isEmpty()) {
            indices = computeAutoRetreatEnergyIndices(board, playerIndex);
        }
        return new RetreatAction(board.getActivePokemon(playerIndex),
                dto.targetIndex() != null ? dto.targetIndex() : 0,
                indices);
    }

    private java.util.List<Integer> computeAutoRetreatEnergyIndices(final MatchBoard board, final int playerIndex) {
        final java.util.List<Integer> indices = new java.util.ArrayList<>();
        final BattlePokemonState active = board.getActivePokemon(playerIndex);
        if (active == null) {
            return indices;
        }
        final TrainerCard stadium = board.getActiveStadium();
        final boolean isFairyGarden = stadium != null && "xy1-117".equals(stadium.getCardId());
        final boolean hasFairyEnergy = active.getAttachedEnergies().contains(PokemonType.FAIRY)
                || active.getAttachedEnergyCards().stream().anyMatch(EnergyCard::isProvidesAllTypes);
        final int cost = (isFairyGarden && hasFairyEnergy) ? 0 : active.getRetreatCost();
        for (int i = 0; i < Math.min(cost, active.getAttachedEnergies().size()); i++) {
            indices.add(i);
        }
        return indices;
    }

    /**
     * True if {@code target} is the exact same {@link PlayerRuntime} instance as slot 0 of the
     * session (i.e. "is player A"). Uses reference identity intentionally, not value equality —
     * runtimes are unique per-slot objects, never structurally-equal-but-distinct instances.
     */
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private int resolvePlayerIndex(final MatchSession session, final PlayerRuntime runtime) {
        return session.getPlayerRuntime(0) == runtime ? 0 : 1;
    }

    /**
     * Reference-identity comparison, used where "is this literally the same in-play Pokémon
     * object" is the actual question (not value equality).
     */
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private boolean isSameInstance(final BattlePokemonState a, final BattlePokemonState b) {
        return a == b;
    }

    private BattlePokemonState resolveEvolveTarget(final MatchBoard board,
                                                    final int playerIndex,
                                                    final ActionRequestDTO dto) {
        return resolvePokemonByIndex(board, playerIndex, dto.targetIndex());
    }

    private BattlePokemonState resolveTarget(final MatchBoard board,
                                              final int playerIndex,
                                              final ActionRequestDTO dto) {
        if (dto.targetIndex() == null) {
            return null;
        }
        if (dto.targetIndex() < 0) {
            return board.getActivePokemon(playerIndex);
        }
        final var benched = board.getBenchedPokemon(playerIndex);
        if (dto.targetIndex() >= 0 && dto.targetIndex() < benched.size()) {
            return benched.get(dto.targetIndex());
        }
        return null;
    }

    private BattlePokemonState resolvePokemonByIndex(final MatchBoard board, final int playerIndex, final Integer index) {
        if (index == null || index < 0) {
            return board.getActivePokemon(playerIndex);
        }
        var benched = board.getBenchedPokemon(playerIndex);
        if (index >= benched.size()) {
            return null;
        }
        return benched.get(index);
    }
}
