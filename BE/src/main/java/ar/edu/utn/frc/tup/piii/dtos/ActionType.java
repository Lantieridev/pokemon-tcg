package ar.edu.utn.frc.tup.piii.dtos;

/**
 * Enumerates every player action type that can be sent over WebSocket.
 */
public enum ActionType {
    DECLARE_ATTACK,
    RETREAT,
    PLAY_TRAINER,
    ATTACH_ENERGY,
    EVOLVE,
    PLACE_BASIC_POKEMON,
    USE_ABILITY,
    /** Ends the current player's turn without attacking (passes to between-turns phase). */
    END_TURN
}
