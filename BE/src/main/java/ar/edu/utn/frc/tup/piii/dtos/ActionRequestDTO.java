package ar.edu.utn.frc.tup.piii.dtos;

import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;

/**
 * DTO carrying a player action sent from the client over WebSocket.
 *
 * @param type          the action type (never null)
 * @param cardId        card identifier, nullable depending on action type
 * @param targetId      target card identifier, nullable depending on action type
 * @param targetIndex   bench slot index, used for RETREAT and EVOLVE
 * @param trainerType   trainer card category, used for PLAY_TRAINER
 * @param attackIndex   zero-based attack index, used for DECLARE_ATTACK
 * @param energyType    energy type to attach, used for ATTACH_ENERGY; null defaults to COLORLESS
 */
public record ActionRequestDTO(
        ActionType type,
        String cardId,
        String targetId,
        Integer targetIndex,
        TrainerType trainerType,
        Integer attackIndex,
        PokemonType energyType) {

    /**
     * Backward-compatible constructor for callers that do not supply energyType.
     * Defaults energyType to {@code null} (callers should handle null → COLORLESS).
     */
    public ActionRequestDTO(final ActionType type,
                            final String cardId,
                            final String targetId,
                            final Integer targetIndex,
                            final TrainerType trainerType,
                            final Integer attackIndex) {
        this(type, cardId, targetId, targetIndex, trainerType, attackIndex, null);
    }
}
