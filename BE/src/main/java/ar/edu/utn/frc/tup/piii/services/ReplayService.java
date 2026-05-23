package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ReplayResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.UserMatchHistoryDTO;

import java.util.List;

/**
 * Service interface for match replays and history.
 */
public interface ReplayService {

    /**
     * Retrieves the list of actions recorded for a given match, ordered chronologically.
     *
     * @param matchId the match ID
     * @return the replay response DTO
     */
    ReplayResponseDTO getReplay(Long matchId);

    /**
     * Retrieves the list of matches in which the user participated.
     *
     * @param username the username of the player
     * @return the list of match history DTOs
     */
    List<UserMatchHistoryDTO> getUserMatchHistory(String username);
}
