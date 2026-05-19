package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.services.MatchService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Objects;

/**
 * STOMP WebSocket controller for in-match player actions.
 * Clients publish to {@code /app/match/{matchId}/action} with a {@code playerId} header.
 */
@Controller
public final class GameWebSocketController {

    private final MatchService matchService;

    /**
     * Constructs the controller with the required service collaborator.
     *
     * @param matchService handles action routing and state management (never null)
     */
    public GameWebSocketController(final MatchService matchService) {
        this.matchService = Objects.requireNonNull(matchService, "matchService must not be null");
    }

    /**
     * Receives a player action over STOMP and delegates to {@link MatchService}.
     *
     * @param matchId  path variable identifying the match
     * @param playerId player header carrying the acting player's identifier
     * @param action   the deserialized action payload
     */
    @MessageMapping("/match/{matchId}/action")
    public void handleAction(@DestinationVariable final String matchId,
                             @Header("playerId") final String playerId,
                             @Payload final ActionRequestDTO action) {
        matchService.processAction(matchId, playerId, action);
    }
}
