package ar.edu.utn.frc.tup.piii.services.persistence;

import java.util.List;

/**
 * Immutable snapshot of game state passed to the persistence port after each action.
 *
 * @param matchId   unique identifier for the match
 * @param round     current round number
 * @param playerIds ordered list of player identifiers (index 0 = player one)
 */
public record GameStateSnapshot(String matchId, int round, List<String> playerIds) {
}
