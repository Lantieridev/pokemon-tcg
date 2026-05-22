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
import ar.edu.utn.frc.tup.piii.engine.model.EvolveAction;
import ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.engine.model.PlaceBasicPokemonAction;
import ar.edu.utn.frc.tup.piii.engine.model.PlayTrainerAction;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.RetreatAction;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.pipeline.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.pipeline.AttackEffectResolver;
import ar.edu.utn.frc.tup.piii.engine.pipeline.AttackPipeline;
import ar.edu.utn.frc.tup.piii.engine.pipeline.DamageApplicationStep;
import ar.edu.utn.frc.tup.piii.engine.pipeline.DamageCalculationStep;
import ar.edu.utn.frc.tup.piii.engine.pipeline.KnockoutCheckStep;
import ar.edu.utn.frc.tup.piii.engine.pipeline.PostDamageEffectsStep;
import ar.edu.utn.frc.tup.piii.engine.pipeline.PreDamageEffectsStep;
import ar.edu.utn.frc.tup.piii.engine.pipeline.ValidationStep;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

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

    public GameFacade() {
        this.attackPipeline = new AttackPipeline(List.of(
                new ValidationStep(),
                new PreDamageEffectsStep(),
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
            case UseAbilityAction ignored      -> { /* FR-TODO: ability effects not yet implemented */ }
        }
    }

    // --- action handlers ---

    private void applyPlacePokemon(final PlaceBasicPokemonAction action, final PlayerRuntime runtime) {
        final Card card = runtime.getHand().removeCard(action.cardId());
        runtime.getBench().place(new InPlayPokemon((PokemonCard) card));
    }

    private void applyAttachEnergy(final AttachEnergyAction action, final PlayerRuntime runtime) {
        final EnergyCard energyCard = findEnergyInHand(runtime, action.energyType());
        runtime.getHand().removeCard(energyCard.getCardId());
        action.target().attachEnergy(action.energyType());
    }

    private void applyEvolve(final EvolveAction action, final PlayerRuntime runtime) {
        if (action.evolution() != null) {
            PokemonCard newCard = (PokemonCard) runtime.getHand().removeCard(action.evolution().getCardId());
            action.target().evolveInto(newCard);
        }
        new EvolveExecutor(runtime.getStatusEffectManager()).executeEvolve(action.target());
    }

    private void applyRetreat(final RetreatAction action, final PlayerRuntime runtime) {
        new RetreatExecutor(runtime.getStatusEffectManager()).executeRetreat(action);
        final BattlePokemonState newActive = runtime.getBench().promote(action.replacementIndex());
        final BattlePokemonState oldActive = runtime.getActivePokemon();
        runtime.setActivePokemon(newActive);
        runtime.getBench().place(oldActive);
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
        ).build();

        attackPipeline.execute(ctx);
    }

    private void applyPlayTrainer(final PlayTrainerAction action,
                                   final MatchSession session,
                                   final PlayerRuntime runtime) {
        if (action.cardId() != null) {
            runtime.getHand().removeCard(action.cardId());
        }

        switch (action.trainerType()) {
            case STADIUM -> {
                final String previous = session.getBoard().replaceStadium(action.cardId() != null
                        ? action.cardId() : "");
                if (previous != null && !previous.isEmpty()) {
                    // Previous stadium goes to discard; card object not tracked by cardId here
                    // — the board only holds the ID; the actual card was already discarded above
                }
            }
            case POKEMON_TOOL -> {
                if (action.target() != null) {
                    action.target().setToolAttached(true);
                }
            }
            default -> { /* ITEM and SUPPORTER effects are applied by RuleValidator/EffectResolver */ }
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
                                            dto.targetIndex() != null ? dto.targetIndex() : 0);
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
            return board.getBenchedPokemon(playerIndex).get(dto.targetIndex());
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
