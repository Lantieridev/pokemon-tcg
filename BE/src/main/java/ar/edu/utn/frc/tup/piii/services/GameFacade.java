package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.engine.model.Action;
import ar.edu.utn.frc.tup.piii.engine.model.AttachEnergyAction;
import ar.edu.utn.frc.tup.piii.engine.model.DeclareAttackAction;
import ar.edu.utn.frc.tup.piii.engine.model.EvolveAction;
import ar.edu.utn.frc.tup.piii.engine.model.PlaceBasicPokemonAction;
import ar.edu.utn.frc.tup.piii.engine.model.PlayTrainerAction;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.RetreatAction;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import org.springframework.stereotype.Component;

/**
 * Translates incoming {@link ActionRequestDTO} objects into engine {@link Action} instances.
 * Stateless — safe for concurrent use.
 */
@Component
public final class GameFacade {

    /**
     * Converts a DTO action into the appropriate sealed engine {@link Action}.
     *
     * @param board       the current match board (used to resolve active pokemon / attacks)
     * @param playerIndex the zero-based index of the acting player (0 or 1)
     * @param dto         the incoming action request (never null)
     * @return a concrete {@link Action} variant (never null)
     */
    public Action toEngineAction(final MatchBoard board,
                                 final int playerIndex,
                                 final ActionRequestDTO dto) {
        return switch (dto.type()) {
            case DECLARE_ATTACK      -> buildDeclareAttack(board, playerIndex, dto);
            case RETREAT             -> new RetreatAction(board.getActivePokemon(playerIndex));
            case PLAY_TRAINER        -> new PlayTrainerAction(dto.trainerType(),
                                            resolveTarget(board, playerIndex, dto));
            case ATTACH_ENERGY       -> new AttachEnergyAction(PokemonType.COLORLESS);
            case EVOLVE              -> new EvolveAction(board.getActivePokemon(playerIndex));
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

    private ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState resolveTarget(
            final MatchBoard board,
            final int playerIndex,
            final ActionRequestDTO dto) {
        if (dto.trainerType() == ar.edu.utn.frc.tup.piii.engine.model.TrainerType.POKEMON_TOOL
                && dto.targetIndex() != null) {
            return board.getBenchedPokemon(playerIndex).get(dto.targetIndex());
        }
        return null;
    }
}
