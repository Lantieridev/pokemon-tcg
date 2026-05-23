package ar.edu.utn.frc.tup.piii.services.persistence;

public record LogActionEvent(
        String matchId,
        int turnNumber,
        String playerId,
        String actionType,
        String result
) {}
