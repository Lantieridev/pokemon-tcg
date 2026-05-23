package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.MatchHistoryDto;
import ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository;
import ar.edu.utn.frc.tup.piii.services.HistoryService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class HistoryServiceImpl implements HistoryService {

    private final MatchRepository matchRepository;

    public HistoryServiceImpl(final MatchRepository matchRepository) {
        this.matchRepository = Objects.requireNonNull(matchRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<MatchHistoryDto> getUserMatchHistory(final String username, final Pageable pageable) {
        Objects.requireNonNull(username, "username must not be null");
        final Slice<MatchHistoryProjectionDto> rawHistory = matchRepository.findUserMatchHistory(username, pageable);
        return rawHistory.map(projection -> mapToDto(projection, username));
    }

    private MatchHistoryDto mapToDto(final MatchHistoryProjectionDto projection, final String username) {
        // Determine opponent
        String opponent;
        if (username.equals(projection.player1Username())) {
            opponent = projection.player2Username() != null ? projection.player2Username() : "Waiting...";
        } else if (username.equals(projection.player2Username())) {
            opponent = projection.player1Username() != null ? projection.player1Username() : "Waiting...";
        } else {
            // Defensive fallback if username is somehow not player1 or player2
            opponent = projection.player1Username();
        }

        // Determine result semantically
        String result = "IN_PROGRESS";
        if ("FINISHED".equalsIgnoreCase(projection.status())) {
            if (projection.winnerUsername() == null) {
                result = "TIE";
            } else if (username.equals(projection.winnerUsername())) {
                result = "VICTORY";
            } else {
                result = "DEFEAT";
            }
        } else if ("ACTIVE".equalsIgnoreCase(projection.status())) {
            result = "IN_PROGRESS";
        }

        return new MatchHistoryDto(
                projection.id(),
                opponent,
                projection.status(),
                result,
                projection.createdAt()
        );
    }
}
