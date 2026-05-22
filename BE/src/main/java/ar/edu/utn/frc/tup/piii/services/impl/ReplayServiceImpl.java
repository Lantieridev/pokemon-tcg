package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.ReplayEventDTO;
import ar.edu.utn.frc.tup.piii.dtos.ReplayResponseDTO;
import ar.edu.utn.frc.tup.piii.persistence.entity.MatchEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.MatchLogEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchLogRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository;
import ar.edu.utn.frc.tup.piii.services.ReplayService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service implementation for managing match replays.
 */
@Service
public class ReplayServiceImpl implements ReplayService {

    private final MatchRepository matchRepository;
    private final MatchLogRepository matchLogRepository;

    public ReplayServiceImpl(final MatchRepository matchRepository, final MatchLogRepository matchLogRepository) {
        this.matchRepository = Objects.requireNonNull(matchRepository, "matchRepository must not be null");
        this.matchLogRepository = Objects.requireNonNull(matchLogRepository, "matchLogRepository must not be null");
    }

    @Override
    public ReplayResponseDTO getReplay(final Long matchId) {
        if (matchId == null) {
            throw new IllegalArgumentException("Match ID cannot be null");
        }

        final MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(() -> new NoSuchElementException("Match not found with id: " + matchId));

        final List<MatchLogEntity> logEntities = matchLogRepository.findByMatchIdOrderByCreatedAtAsc(matchId);

        final List<ReplayEventDTO> events = logEntities.stream()
                .map(log -> ReplayEventDTO.builder()
                        .turn(log.getTurnNumber())
                        .player(log.getPlayer() != null ? log.getPlayer().getUsername() : null)
                        .action(log.getActionType())
                        .result(log.getResult())
                        .timestamp(log.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ReplayResponseDTO.builder()
                .matchId(matchId)
                .events(events)
                .build();
    }
}
