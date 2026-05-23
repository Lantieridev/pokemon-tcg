package ar.edu.utn.frc.tup.piii.dtos;

import java.time.LocalDateTime;

public record MatchHistoryDto(
        Long matchId,
        String opponent,
        String status,
        String result,
        LocalDateTime date
) {}
