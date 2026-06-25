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
import java.util.List;

/**
 * Translates incoming {@link ActionRequestDTO} objects into engine {@link Action} instances,
 * and applies validated actions to the live match session state.
 *
 * <p>Stateless for all non-apply concerns. The {@link AttackPipeline} instance is
 * immutable and thread-safe once built.</p>
 */
@Component
public final class GameFacade {

    private final AttackPipeline attackPipeline;
    private final TrainerEffectResolver trainerEffectResolver;
    private final AbilityEffectResolver abilityEffectResolver;

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

        switch (action) {
            case PlaceBasicPokemonAction place -> applyPlacePokemon(place, runtime);
            case AttachEnergyAction attach     -> {
                applyAttachEnergy(attach, runtime);
                if (turnManager != null) {
                    turnManager.requireMainPhase().recordEnergyAttached();
                }
            }
            case EvolveAction evolve           -> applyEvolve(evolve, runtime);
            case RetreatAction retreat         -> {
                applyRetreat(retreat, runtime);
                if (turnManager != null) {
                    turnManager.requireMainPhase().recordRetreatUsed();
                }
            }
            case DeclareAttackAction attack    -> applyDeclareAttack(attack, session, playerIndex);
            case PlayTrainerAction trainer     -> {
                applyPlayTrainer(trainer, session, runtime);
                if (turnManager != null) {
                    if (trainer.trainerType() == TrainerType.SUPPORTER) {
                        turnManager.requireMainPhase().recordSupporterPlayed();
                    } else if (trainer.trainerType() == TrainerType.STADIUM) {
                        turnManager.requireMainPhase().recordStadiumPlayed();
                    }
                }
            }
            case UseAbilityAction abilityAction -> applyUseAbility(abilityAction, session);
            case EndTurnAction ignored         -> { /* turn advancement handled by MatchService */ }
            case PromoteActiveAction promote   -> applyPromoteActive(promote, runtime);
            case ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction selectCards -> applySelectCards(selectCards, session, runtime);
        }
        
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
        if (placed.getAbilities().stream().anyMatch(a -> a.effectId() == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.SWEET_VEIL)) {
            runtime.getStatusEffectManager().remove(ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType.DORMIDO);
        }
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

    private void applyEvolve(final EvolveAction action, final PlayerRuntime runtime) {
        if (action.evolution() != null) {
            PokemonCard newCard = (PokemonCard) runtime.getHand().removeCard(action.evolution().getCardId());
            action.target().evolveInto(newCard);
            // XY1 §2: a Pokémon cannot evolve in the same turn it evolved. The
            // BattlePokemonState mutates in-place, so we must reset its turnsInPlay
            // counter to 0 so RuleValidator sees it as "just entered". Done HERE
            // (inside the mutation guard) so a no-op call cannot accidentally
            // mark an unrelated Pokémon as freshly placed.
            runtime.recordPokemonEntered(action.target());

            // Track stats!
            if (runtime.getStatisticsTracker() != null) {
                runtime.getStatisticsTracker().incrementPokemonPlayed(newCard.getCardId());
            }
        }
        
        if (action.target() == runtime.getActivePokemon()) {
            new EvolveExecutor(runtime.getStatusEffectManager()).executeEvolve(action.target());
        }
        
        // Sweet Veil: remove Asleep condition if the evolved Pokémon has Sweet Veil.
        if (action.target().getAbilities().stream().anyMatch(a -> a.effectId() == ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId.SWEET_VEIL)) {
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
        // Retrieve the card reference BEFORE removing from hand so we can invoke its effect.
        TrainerCard trainerCard = null;
        if (action.cardId() != null) {
            trainerCard = (TrainerCard) runtime.getHand().getCards().stream()
                    .filter(c -> c.getCardId().equals(action.cardId()))
                    .findFirst()
                    .orElse(null);
            runtime.getHand().removeCard(action.cardId());
        }

        switch (action.trainerType()) {
            case STADIUM -> {
                // Stadium replaces the current field stadium; previous one goes to discard.
                if (trainerCard != null) {
                    final TrainerCard previous = session.getBoard().replaceStadium(trainerCard);
                    if (previous != null) {
                        runtime.getDiscardPile().add(previous);
                    }
                }
            }
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
                    final TrainerEffectId effectId = trainerCard.getEffectId();

                    // Effects that need special dispatch (opponent runtime or bench mutation).
                    if (effectId == TrainerEffectId.RED_CARD) {
                        applyRedCard(session);
                    } else if (effectId == TrainerEffectId.TEAM_FLARE_GRUNT) {
                        applyTeamFlareGrunt(session);
                    } else if (effectId == TrainerEffectId.CASSIUS) {
                        applyCassius(runtime, action.target());
                    } else if (effectId == TrainerEffectId.EVOSODA) {
                        session.setPendingSelectionRequest(new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(effectId, action.target(), 1, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK));
                        session.getTurnManager().interruptMainPhase();
                    } else if (effectId == TrainerEffectId.GREAT_BALL) {
                        session.setPendingSelectionRequest(new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(effectId, null, 1, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.TOP_7_DECK));
                        session.getTurnManager().interruptMainPhase();
                    } else if (effectId == TrainerEffectId.PROFESSORS_LETTER) {
                        session.setPendingSelectionRequest(new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(effectId, null, 2, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK));
                        session.getTurnManager().interruptMainPhase();
                    } else if (effectId == TrainerEffectId.MAX_REVIVE) {
                        session.setPendingSelectionRequest(new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(effectId, null, 1, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DISCARD_PILE));
                        session.getTurnManager().interruptMainPhase();
                    } else if (effectId == TrainerEffectId.LYSANDRE) {
                        applyLysandre(session, action.target());
                    } else if (effectId == TrainerEffectId.SACRED_ASH) {
                        final long pokemonCount = runtime.getDiscardPile().getCards().stream()
                                .filter(c -> c instanceof PokemonCard)
                                .count();
                        final int selectCount = (int) Math.min(5, pokemonCount);
                        session.setPendingSelectionRequest(new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(effectId, null, selectCount, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DISCARD_PILE));
                        session.getTurnManager().interruptMainPhase();
                    } else if (effectId == TrainerEffectId.POKEMON_FAN_CLUB) {
                        session.setPendingSelectionRequest(new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(effectId, null, 2, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK));
                        session.getTurnManager().interruptMainPhase();
                    } else if (effectId == TrainerEffectId.FIERY_TORCH) {
                        session.setPendingSelectionRequest(new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(effectId, null, 1, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND));
                        session.getTurnManager().interruptMainPhase();
                    } else if (effectId == TrainerEffectId.TRICK_SHOVEL) {
                        session.setPendingSelectionRequest(new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(effectId, action.target(), 1, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.TOP_7_DECK));
                        session.getTurnManager().interruptMainPhase();
                    } else if (effectId == TrainerEffectId.STARTLING_MEGAPHONE) {
                        applyStartlingMegaphone(session);
                    } else if (effectId == TrainerEffectId.PAL_PAD) {
                        final long supporterCount = runtime.getDiscardPile().getCards().stream()
                                .filter(c -> c instanceof TrainerCard tc && tc.getTrainerType() == TrainerType.SUPPORTER)
                                .count();
                        final int selectCount = (int) Math.min(2, supporterCount);
                        session.setPendingSelectionRequest(new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(effectId, null, selectCount, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DISCARD_PILE));
                        session.getTurnManager().interruptMainPhase();
                    } else if (effectId == TrainerEffectId.BLACKSMITH) {
                        final long fireEnergyCount = runtime.getDiscardPile().getCards().stream()
                                .filter(c -> c instanceof EnergyCard ec && ec.getEnergyType() == PokemonType.FIRE)
                                .count();
                        final int selectCount = (int) Math.min(2, fireEnergyCount);
                        session.setPendingSelectionRequest(new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(effectId, action.target(), selectCount, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DISCARD_PILE));
                        session.getTurnManager().interruptMainPhase();
                    } else if (effectId == TrainerEffectId.POKEMON_CENTER_LADY) {
                        applyPokemonCenterLady(runtime, action.target());
                    } else if (effectId == TrainerEffectId.ULTRA_BALL) {
                        session.setPendingSelectionRequest(new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(effectId, null, 2, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND));
                        session.getTurnManager().interruptMainPhase();
                    } else {
                        TrainerEffect effect = trainerCard.getEffect();
                        if (effect == null && effectId != null) {
                            effect = trainerEffectResolver.resolve(effectId, session.getCoinFlipper()).orElse(null);
                        }
                        if (effect != null) {
                            effect.apply(runtime, action.target());
                        }
                    }
                }
            }
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
        
        final TrainerEffectId effectId = request.sourceEffect();
        final List<String> selectedIds = action.cardIds();
        
        if (effectId == TrainerEffectId.EVOSODA) {
            if (!selectedIds.isEmpty()) {
                final List<Card> found = runtime.getDeck().searchAndRemove(c -> c.getCardId().equals(selectedIds.get(0)), 1);
                if (!found.isEmpty()) {
                    Card selectedCard = found.get(0);
                    if (selectedCard instanceof PokemonCard pc && pc.getEvolvesFrom() != null && pc.getEvolvesFrom().equals(request.target().getName())) {
                        request.target().evolveInto(pc);
                    } else {
                        throw new IllegalArgumentException("Selected card is not a valid evolution for the target");
                    }
                }
            }
            runtime.getDeck().shuffle();
        } else if (effectId == TrainerEffectId.GREAT_BALL) {
            if (!selectedIds.isEmpty()) {
                final List<Card> top7 = runtime.getDeck().drawMultiple(Math.min(7, runtime.getDeck().size()));
                Card selected = null;
                for (Card c : top7) {
                    if (c.getCardId().equals(selectedIds.get(0))) {
                        selected = c;
                    }
                }
                if (selected != null) {
                    if (!(selected instanceof PokemonCard)) {
                        throw new IllegalArgumentException("Great Ball can only select a Pokemon card");
                    }
                    top7.remove(selected);
                    runtime.getHand().addCard(selected);
                }
                runtime.getDeck().addCards(top7);
                runtime.getDeck().shuffle();
            } else {
                runtime.getDeck().shuffle();
            }
        } else if (effectId == TrainerEffectId.PROFESSORS_LETTER) {
            if (!selectedIds.isEmpty()) {
                for (String id : selectedIds) {
                    final List<Card> found = runtime.getDeck().searchAndRemove(c -> c.getCardId().equals(id), 1);
                    if (!found.isEmpty()) {
                        if (!(found.get(0) instanceof EnergyCard ec) || !ec.isBasic()) {
                            throw new IllegalArgumentException("Professor's Letter can only select basic energy cards");
                        }
                        runtime.getHand().addCard(found.get(0));
                    }
                }
            }
            runtime.getDeck().shuffle();
        } else if (effectId == TrainerEffectId.MAX_REVIVE) {
            if (!selectedIds.isEmpty()) {
                Card found = null;
                for (Card c : runtime.getDiscardPile().getCards()) {
                    if (c.getCardId().equals(selectedIds.get(0))) {
                        found = c;
                        break;
                    }
                }
                if (found != null) {
                    if (!(found instanceof PokemonCard pc) || pc.getEvolutionStage() != ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.BASIC) {
                        throw new IllegalArgumentException("Max Revive can only select a Basic Pokemon card");
                    }
                    runtime.getDiscardPile().remove(found);
                    runtime.getDeck().addToTop(found);
                }
            }
        } else if (effectId == TrainerEffectId.SACRED_ASH) {
            if (!selectedIds.isEmpty()) {
                final List<Card> toReturn = new ArrayList<>();
                for (String id : selectedIds) {
                    final List<Card> found = runtime.getDiscardPile().getCards().stream()
                            .filter(c -> c.getCardId().equals(id))
                            .toList();
                    if (!found.isEmpty()) {
                        final Card card = found.get(0);
                        if (!(card instanceof PokemonCard)) {
                            throw new IllegalArgumentException("Sacred Ash can only select Pokemon cards");
                        }
                        runtime.getDiscardPile().remove(card);
                        toReturn.add(card);
                    }
                }
                if (!toReturn.isEmpty()) {
                    runtime.getDeck().addCards(toReturn);
                    runtime.getDeck().shuffle();
                }
            }
        } else if (effectId == TrainerEffectId.POKEMON_FAN_CLUB) {
            if (!selectedIds.isEmpty()) {
                for (String id : selectedIds) {
                    final List<Card> found = runtime.getDeck().searchAndRemove(c -> c.getCardId().equals(id), 1);
                    if (!found.isEmpty()) {
                        final Card card = found.get(0);
                        if (!(card instanceof PokemonCard pc && pc.getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.BASIC)) {
                            throw new IllegalArgumentException("Pokemon Fan Club can only select Basic Pokemon cards");
                        }
                        runtime.getHand().addCard(card);
                    }
                }
            }
            runtime.getDeck().shuffle();
        } else if (effectId == TrainerEffectId.QUIVER_DANCE) {
            boolean attached = false;
            if (!selectedIds.isEmpty()) {
                final List<Card> found = runtime.getDeck().searchAndRemove(c -> c.getCardId().equals(selectedIds.get(0)), 1);
                if (!found.isEmpty()) {
                    final Card card = found.get(0);
                    if (!(card instanceof EnergyCard ec && ec.isBasic())) {
                        throw new IllegalArgumentException("Quiver Dance can only select a Basic Energy card");
                    }
                    if (runtime.getActivePokemon() != null) {
                        runtime.getActivePokemon().attachEnergy((EnergyCard) card);
                        attached = true;
                    }
                }
            }
            runtime.getDeck().shuffle();
            if (attached && runtime.getActivePokemon() != null) {
                runtime.getActivePokemon().heal(40);
            }
        } else if (effectId == TrainerEffectId.FIERY_TORCH) {
            if (!selectedIds.isEmpty()) {
                final String cardId = selectedIds.get(0);
                final Card card = runtime.getHand().removeCard(cardId);
                if (card == null || !(card instanceof EnergyCard ec && ec.getEnergyType() == PokemonType.FIRE)) {
                    throw new IllegalArgumentException("Fiery Torch requires discarding a Fire Energy card from hand");
                }
                runtime.getDiscardPile().add(card);
                final int drawCount = Math.min(2, runtime.getDeck().size());
                if (drawCount > 0) {
                    runtime.getHand().addCards(runtime.getDeck().drawMultiple(drawCount));
                }
            }
        } else if (effectId == TrainerEffectId.TRICK_SHOVEL) {
            final BattlePokemonState target = request.target();
            final int targetPlayerIndex = (target != null && target.equals(runtime.getActivePokemon()))
                    ? session.getActivePlayerIndex() : (1 - session.getActivePlayerIndex());
            final PlayerRuntime targetRuntime = session.getPlayerRuntime(targetPlayerIndex);
            if (!selectedIds.isEmpty()) {
                final List<Card> topCardList = targetRuntime.getDeck().drawMultiple(1);
                if (!topCardList.isEmpty()) {
                    targetRuntime.getDiscardPile().add(topCardList.get(0));
                }
            }
        } else if (effectId == TrainerEffectId.PAL_PAD) {
            if (!selectedIds.isEmpty()) {
                final List<Card> toReturn = new ArrayList<>();
                for (String id : selectedIds) {
                    final List<Card> found = runtime.getDiscardPile().getCards().stream()
                            .filter(c -> c.getCardId().equals(id))
                            .toList();
                    if (!found.isEmpty()) {
                        final Card card = found.get(0);
                        if (!(card instanceof TrainerCard tc && tc.getTrainerType() == TrainerType.SUPPORTER)) {
                            throw new IllegalArgumentException("Pal Pad can only select Supporter cards");
                        }
                        runtime.getDiscardPile().remove(card);
                        toReturn.add(card);
                    }
                }
                if (!toReturn.isEmpty()) {
                    runtime.getDeck().addCards(toReturn);
                    runtime.getDeck().shuffle();
                }
            }
        } else if (effectId == TrainerEffectId.BLACKSMITH) {
            if (!selectedIds.isEmpty()) {
                final BattlePokemonState target = request.target();
                if (target == null) {
                    throw new IllegalStateException("Blacksmith requires a target Pokemon");
                }
                for (String id : selectedIds) {
                    final List<Card> found = runtime.getDiscardPile().getCards().stream()
                            .filter(c -> c.getCardId().equals(id))
                            .toList();
                    if (!found.isEmpty()) {
                        final Card card = found.get(0);
                        if (!(card instanceof EnergyCard ec && ec.getEnergyType() == PokemonType.FIRE)) {
                            throw new IllegalArgumentException("Blacksmith can only select Fire Energy cards");
                        }
                        runtime.getDiscardPile().remove(card);
                        target.attachEnergy((EnergyCard) card);
                    }
                }
            }
        } else if (effectId == TrainerEffectId.ULTRA_BALL) {
            if (request.source() == ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND) {
                if (selectedIds.size() != 2) {
                    throw new IllegalArgumentException("Ultra Ball requires discarding exactly 2 cards");
                }
                for (String id : selectedIds) {
                    final Card discarded = runtime.getHand().removeCard(id);
                    if (discarded != null) {
                        runtime.getDiscardPile().add(discarded);
                    }
                }
                // Transition to deck search
                session.setPendingSelectionRequest(new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(effectId, null, 1, ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK));
                return; // Do NOT resume the main phase yet
            } else if (request.source() == ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK) {
                if (!selectedIds.isEmpty()) {
                    final List<Card> found = runtime.getDeck().searchAndRemove(c -> c.getCardId().equals(selectedIds.get(0)), 1);
                    if (!found.isEmpty()) {
                        final Card card = found.get(0);
                        if (!(card instanceof PokemonCard)) {
                            throw new IllegalArgumentException("Ultra Ball can only select a Pokemon card");
                        }
                        runtime.getHand().addCard(card);
                    }
                }
                runtime.getDeck().shuffle();
            }
        } else if (effectId == TrainerEffectId.CLAIRVOYANT_EYE) {
            if (!selectedIds.isEmpty()) {
                final List<Card> topCards = runtime.getDeck().drawMultiple(selectedIds.size());
                final List<Card> reordered = new ArrayList<>();
                for (String id : selectedIds) {
                    Card foundCard = null;
                    for (Card c : topCards) {
                        if (c.getCardId().equals(id)) {
                            foundCard = c;
                            break;
                        }
                    }
                    if (foundCard != null) {
                        topCards.remove(foundCard);
                        reordered.add(foundCard);
                    }
                }
                reordered.addAll(topCards);
                for (int i = reordered.size() - 1; i >= 0; i--) {
                    runtime.getDeck().addToTop(reordered.get(i));
                }
            }
        }
        
        session.setPendingSelectionRequest(null);
        session.getTurnManager().resumeMainPhase();
    }

    // --- helpers ---

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
    public Action toEngineAction(final MatchSession session,
                                 final int playerIndex,
                                 final ActionRequestDTO dto) {
        final MatchBoard board = session.getBoard();
        return switch (dto.type()) {
            case DECLARE_ATTACK      -> buildDeclareAttack(board, playerIndex, dto);
            case RETREAT             -> {
                java.util.List<Integer> indices = dto.selectedEnergyIndices();
                if (indices == null || indices.isEmpty()) {
                    indices = new java.util.ArrayList<>();
                    BattlePokemonState active = board.getActivePokemon(playerIndex);
                    if (active != null) {
                        TrainerCard stadium = board.getActiveStadium();
                        boolean isFairyGarden = stadium != null && "xy1-117".equals(stadium.getCardId());
                        boolean hasFairyEnergy = active.getAttachedEnergies().contains(PokemonType.FAIRY)
                                || active.getAttachedEnergyCards().stream().anyMatch(EnergyCard::isProvidesAllTypes);
                        int cost = (isFairyGarden && hasFairyEnergy) ? 0 : active.getRetreatCost();
                        for (int i = 0; i < Math.min(cost, active.getAttachedEnergies().size()); i++) {
                            indices.add(i);
                        }
                    }
                }
                yield new RetreatAction(board.getActivePokemon(playerIndex),
                        dto.targetIndex() != null ? dto.targetIndex() : 0,
                        indices);
            }
            case PLAY_TRAINER        -> {
                TrainerEffectId effectId = null;
                if (dto.cardId() != null) {
                    final Card card = session.getPlayerRuntime(playerIndex).getHand().getCards().stream()
                            .filter(c -> c.getCardId().equals(dto.cardId()))
                            .findFirst().orElse(null);
                    if (card instanceof ar.edu.utn.frc.tup.piii.engine.model.TrainerCard tc) {
                        effectId = tc.getEffectId();
                    }
                }
                final BattlePokemonState target = (effectId == TrainerEffectId.LYSANDRE)
                        ? resolveTarget(board, 1 - playerIndex, dto)
                        : resolveTarget(board, playerIndex, dto);
                yield new PlayTrainerAction(dto.trainerType(),
                                            target,
                                            dto.cardId(),
                                            effectId);
            }
            case ATTACH_ENERGY       -> new AttachEnergyAction(
                                            resolveEvolveTarget(board, playerIndex, dto),
                                            dto.energyType() != null ? dto.energyType() : PokemonType.COLORLESS);
            case EVOLVE              -> {
                final Card cardInHand = session.getPlayerRuntime(playerIndex).getHand().getCards().stream()
                        .filter(c -> c.getCardId().equals(dto.cardId()))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("Card not in hand"));
                yield new EvolveAction(resolveEvolveTarget(board, playerIndex, dto), (PokemonCard) cardInHand);
            }
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

    private BattlePokemonState resolveEvolveTarget(final MatchBoard board,
                                                    final int playerIndex,
                                                    final ActionRequestDTO dto) {
        if (dto.targetIndex() != null) {
            var benched = board.getBenchedPokemon(playerIndex);
            if (dto.targetIndex() < 0 || dto.targetIndex() >= benched.size()) {
                return null;
            }
            return benched.get(dto.targetIndex());
        }
        return board.getActivePokemon(playerIndex);
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
