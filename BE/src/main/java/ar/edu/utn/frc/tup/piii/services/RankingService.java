package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.RankingDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface RankingService {

    /**
     * Retrieves the global leaderboard ranking ordered by number of wins.
     *
     * @param pageable pagination parameters (page, size)
     * @return a Slice containing RankingDto elements
     */
    Slice<RankingDto> getGlobalRanking(Pageable pageable);
}
