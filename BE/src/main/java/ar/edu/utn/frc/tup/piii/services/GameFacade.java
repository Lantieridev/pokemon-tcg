package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.engine.manager.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.manager.EvolveExecutor;
import ar.edu.utn.frc.tup.piii.engine.manager.RetreatExecutor;
import ar.edu.utn.frc.tup.piii.engine.manager.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.model.Action;
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
        final int playerIndex = session.getActivePlayerIndex();
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
                .ifPresent(ability -> {
                    var effect = abilityEffectResolver.resolve(ability.effectId());
                    if (effect != null) {
                        effect.apply(session, source);
                    }
                });
    }

    private void applyPlacePokemon(final PlaceBasicPokemonAction action, final PlayerRuntime runtime) {
        final Card card = runtime.getHand().removeCard(action.cardId());
        final InPlayPokemon placed = new InPlayPokemon((PokemonCard) card);
        runtime.getBench().place(placed);
        // Register the newly placed Pokémon so evolution restriction tracking starts at 0 turns.
        runtime.recordPokemonEntered(placed);
    }

    private void applyAttachEnergy(final AttachEnergyAction action, final PlayerRuntime runtime) {
        final EnergyCard energyCard = findEnergyInHand(runtime, action.energyType());
        runtime.getHand().removeCard(energyCard.getCardId());
        action.target().attachEnergy(energyCard);
    }

    private void applyEvolve(final EvolveAction action, final PlayerRuntime runtime) {
        if (action.evolution() != null) {
            PokemonCard newCard = (PokemonCard) runtime.getHand().removeCard(action.evolution().getCardId());
            action.target().evolveInto(newCard);
        }
        
        if (action.target() == runtime.getActivePokemon()) {
            new EvolveExecutor(runtime.getStatusEffectManager()).executeEvolve(action.target());
        }
    }

    private void applyRetreat(final RetreatAction action, final PlayerRuntime runtime) {
        new RetreatExecutor(runtime.getStatusEffectManager()).executeRetreat(action);
        final BattlePokemonState newActive = runtime.getBench().promote(action.replacementIndex());
        final BattlePokemonState oldActive = runtime.getActivePokemon();
        runtime.setActivePokemon(newActive);
        runtime.getBench().place(oldActive);
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

        final AttackContext ctx = new AttackContext.Builder(
                action.attacker(),
                defender.getActivePokemon(),
                action.attack(),
                attacker.getStatusEffectManager(),
                defender.getStatusEffectManager(),
                session.getKnockoutHandler(),
                session.getCoinFlipper()::flip
        )
        .defenderBench(defender.getBench().getAll())
        .stadiumProvider(session.getBoard())
        .build();

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

                    // Opponent-targeting effects are handled here since TrainerEffect.apply()
                    // only receives the actor's PlayerRuntime (not the opponent's).
                    if (effectId == TrainerEffectId.RED_CARD) {
                        applyRedCard(session);
                    } else if (effectId == TrainerEffectId.TEAM_FLARE_GRUNT) {
                        applyTeamFlareGrunt(session);
                    } else {
                        TrainerEffect effect = trainerCard.getEffect();
                        if (effect == null && effectId != null) {
                            effect = trainerEffectResolver.resolve(effectId, session.getCoinFlipper());
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
            case RETREAT             -> new RetreatAction(board.getActivePokemon(playerIndex),
                                            dto.targetIndex() != null ? dto.targetIndex() : 0,
                                            dto.selectedEnergyIndices() != null ? dto.selectedEnergyIndices() : java.util.Collections.emptyList());
            case PLAY_TRAINER        -> new PlayTrainerAction(dto.trainerType(),
                                            resolveTarget(board, playerIndex, dto),
                                            dto.cardId());
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
                                            board.getActivePokemon(playerIndex),
                                            dto.cardId());
            case END_TURN            -> new EndTurnAction();
            case PROMOTE_ACTIVE      -> new PromoteActiveAction(
                                            dto.targetIndex() != null ? dto.targetIndex() : 0);
        };
    }

    private DeclareAttackAction buildDeclareAttack(final MatchBoard board,
                                                    final int playerIndex,
                                                    final ActionRequestDTO dto) {
        final int attackIndex = dto.attackIndex() != null ? dto.attackIndex() : 0;
        return new DeclareAttackAction(
                board.getActivePokemon(playerIndex),
                board.getActiveAttacks(playerIndex).get(attackIndex));
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
        if (dto.trainerType() == TrainerType.POKEMON_TOOL && dto.targetIndex() != null) {
            return board.getBenchedPokemon(playerIndex).get(dto.targetIndex());
        }
        return null;
    }
}
