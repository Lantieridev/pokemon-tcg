package ar.edu.utn.frc.tup.piii.dtos;

import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;

/**
 * DTO carrying a player action sent from the client over WebSocket.
 *
 * @param type          the action type (never null)
 * @param cardId        card identifier, nullable depending on action type
 * @param targetId      target card identifier, nullable depending on action type
 * @param targetIndex   bench slot index, used for RETREAT
 * @param trainerType   trainer card category, used for PLAY_TRAINER
 * @param attackIndex   zero-based attack index, used for DECLARE_ATTACK
 */
public record ActionRequestDTO(
        ActionType type,
        String cardId,
        String targetId,
        Integer targetIndex,
        TrainerType trainerType,
        Integer attackIndex) {
}
