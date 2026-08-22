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

    private static final String STATUS_FINISHED = "FINISHED";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String RESULT_IN_PROGRESS = "IN_PROGRESS";
    private static final String RESULT_TIE = "TIE";
    private static final String RESULT_VICTORY = "VICTORY";
    private static final String RESULT_DEFEAT = "DEFEAT";
    private static final String WAITING_LABEL = "Waiting...";

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
        final String opponent = determineOpponent(projection, username);
        final String result = determineResult(projection, username);
        final PlayerStats stats = determinePlayerStats(projection, username);

        return new MatchHistoryDto(
                projection.id(),
                opponent,
                projection.status(),
                result,
                projection.createdAt(),
                stats.playerStatsJson(),
                stats.opponentStatsJson()
        );
    }

    private String determineOpponent(final MatchHistoryProjectionDto projection, final String username) {
        if (username.equals(projection.player1Username())) {
            return projection.player2Username() != null ? projection.player2Username() : WAITING_LABEL;
        }
        if (username.equals(projection.player2Username())) {
            return projection.player1Username() != null ? projection.player1Username() : WAITING_LABEL;
        }
        // Defensive fallback if username is somehow not player1 or player2
        return projection.player1Username();
    }

    private String determineResult(final MatchHistoryProjectionDto projection, final String username) {
        if (STATUS_FINISHED.equalsIgnoreCase(projection.status())) {
            if (projection.winnerUsername() == null) {
                return RESULT_TIE;
            }
            return username.equals(projection.winnerUsername()) ? RESULT_VICTORY : RESULT_DEFEAT;
        }
        if (STATUS_ACTIVE.equalsIgnoreCase(projection.status())) {
            return RESULT_IN_PROGRESS;
        }
        return RESULT_IN_PROGRESS;
    }

    private PlayerStats determinePlayerStats(final MatchHistoryProjectionDto projection, final String username) {
        if (username.equals(projection.player1Username())) {
            return new PlayerStats(projection.player1StatsJson(), projection.player2StatsJson());
        }
        return new PlayerStats(projection.player2StatsJson(), projection.player1StatsJson());
    }

    private record PlayerStats(String playerStatsJson, String opponentStatsJson) {
    }
}
