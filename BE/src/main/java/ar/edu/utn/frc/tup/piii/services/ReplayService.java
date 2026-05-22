package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ReplayResponseDTO;

/**
 * Service interface for match replays.
 */
public interface ReplayService {

    /**
     * Retrieves the list of actions recorded for a given match, ordered chronologically.
     *
     * @param matchId the match ID
     * @return the replay response DTO
     */
    ReplayResponseDTO getReplay(Long matchId);
}
