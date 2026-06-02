package ar.edu.utn.frc.tup.piii.dtos;

import java.time.LocalDateTime;

public record MatchHistoryProjectionDto(
        Long id,
        String status,
        String player1Username,
        String player2Username,
        String winnerUsername,
        LocalDateTime createdAt,
        String player1StatsJson,
        String player2StatsJson
) {
    public MatchHistoryProjectionDto(Long id, String status, String player1Username, String player2Username, String winnerUsername, LocalDateTime createdAt) {
        this(id, status, player1Username, player2Username, winnerUsername, createdAt, null, null);
    }
}
