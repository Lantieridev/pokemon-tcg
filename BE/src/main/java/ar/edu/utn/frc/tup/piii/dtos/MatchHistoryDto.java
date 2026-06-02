package ar.edu.utn.frc.tup.piii.dtos;

import java.time.LocalDateTime;

public record MatchHistoryDto(
        Long matchId,
        String opponent,
        String status,
        String result,
        LocalDateTime date,
        String playerStatsJson,
        String opponentStatsJson
) {
    public MatchHistoryDto(Long matchId, String opponent, String status, String result, LocalDateTime date) {
        this(matchId, opponent, status, result, date, null, null);
    }
}
