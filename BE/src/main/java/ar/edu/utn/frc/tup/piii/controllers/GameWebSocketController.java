package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.services.MatchService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Objects;

import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * STOMP WebSocket controller for in-match player actions.
 * Clients publish to {@code /app/match/{matchId}/action} with a {@code playerId} header.
 */
@Controller
public final class GameWebSocketController {

    private final MatchService matchService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Constructs the controller with the required service collaborator.
     *
     * @param matchService handles action routing and state management (never null)
     * @param messagingTemplate handles sending messages to clients
     */
    public GameWebSocketController(final MatchService matchService, final SimpMessagingTemplate messagingTemplate) {
        this.matchService = Objects.requireNonNull(matchService, "matchService must not be null");
        this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
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
                             final java.security.Principal principal,
                             @Payload final ActionRequestDTO action) {
        if (principal == null) {
            throw new IllegalArgumentException("User must be authenticated");
        }
        if (!principal.getName().equals(playerId)) {
            throw new IllegalArgumentException("Player ID must match the authenticated user");
        }
        matchService.processAction(matchId, playerId, action);
    }

    /**
     * Handles exceptions thrown during message processing and sends an error message
     * back to the specific player's error topic.
     */
    @org.springframework.messaging.handler.annotation.MessageExceptionHandler(Exception.class)
    public void handleException(Exception ex,
                                @Header("playerId") String playerId,
                                @DestinationVariable("matchId") String matchId) {
        if (playerId != null && matchId != null) {
            String topic = String.format("/topic/match/%s/player/%s/errors", matchId, playerId);
            messagingTemplate.convertAndSend(topic, (Object) Map.of("error", ex.getMessage()));
        }
    }
}
