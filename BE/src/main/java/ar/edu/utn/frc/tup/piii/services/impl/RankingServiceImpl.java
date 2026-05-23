package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.RankingDto;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository;
import ar.edu.utn.frc.tup.piii.services.RankingService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class RankingServiceImpl implements RankingService {

    private final MatchRepository matchRepository;

    public RankingServiceImpl(final MatchRepository matchRepository) {
        this.matchRepository = Objects.requireNonNull(matchRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<RankingDto> getGlobalRanking(final Pageable pageable) {
        return matchRepository.getGlobalRanking(pageable);
    }
}
