package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.MatchHistoryDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface HistoryService {

    /**
     * Retrieves the match history for the specified user, mapped to business logic results.
     *
     * @param username the username of the authenticated player (never null)
     * @param pageable pagination parameters (page, size)
     * @return a Slice of MatchHistoryDto
     */
    Slice<MatchHistoryDto> getUserMatchHistory(String username, Pageable pageable);
}
