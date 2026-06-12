package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.RankingDto;

import ar.edu.utn.frc.tup.piii.services.RankingService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;

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
            String tier = determineTier(dto.mmr(), dto.rankedMatchesPlayed());
            return new RankingDto(dto.username(), dto.mmr(), tier, dto.rankedMatchesPlayed());
        });
    }

    private String determineTier(Integer mmr, Integer matchesPlayed) {
        if (mmr == null) return "Unranked";
        if (matchesPlayed == null || matchesPlayed < 10) return "Unranked";
        
        if (mmr < 1200) return "Iron";
        if (mmr < 1400) return "Bronze";
        if (mmr < 1600) return "Silver";
        if (mmr < 1800) return "Gold";
        if (mmr < 2000) return "Platinum";
        if (mmr < 2200) return "Diamond";
        if (mmr < 2400) return "Master";
        return "Grandmaster";
    }
}
