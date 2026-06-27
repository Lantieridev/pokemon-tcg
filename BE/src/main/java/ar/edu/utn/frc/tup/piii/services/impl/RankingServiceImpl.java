package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.RankingDto;

import ar.edu.utn.frc.tup.piii.services.RankingService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.persistence.entity.Tier;

@Service
public class RankingServiceImpl implements RankingService {

    private final UserRepository userRepository;

    public RankingServiceImpl(final UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<RankingDto> getGlobalRanking(final Pageable pageable) {
        return userRepository.getGlobalRanking(pageable).map(dto -> {
            String tierName = Tier.fromMmrAndMatches(dto.mmr(), dto.rankedMatchesPlayed()).getName();
            return new RankingDto(dto.username(), dto.mmr(), tierName, dto.rankedMatchesPlayed());
        });
    }
}
