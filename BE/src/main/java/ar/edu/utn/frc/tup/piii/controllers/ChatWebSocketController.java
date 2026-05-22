package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.ChatMessageRequest;
import ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse;
import ar.edu.utn.frc.tup.piii.services.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Controller for handling in-game chat events via WebSockets.
 */
@Controller
public class ChatWebSocketController {

    private final ChatService chatService;

    public ChatWebSocketController(final ChatService chatService) {
        this.chatService = Objects.requireNonNull(chatService, "chatService must not be null");
    }

    /**
     * Receives a message, saves it to memory cache, and broadcasts it to all subscribers.
     * Extracts sender from the WebSocket principal to prevent impersonation.
     *
     * @param matchId   the match ID
     * @param request   the chat message payload
     * @param principal the authenticated user principal
     * @return the broadcasted response payload
     */
    @MessageMapping("/chat/{matchId}")
    @SendTo("/topic/chat/{matchId}")
    public ChatMessageResponse broadcastMessage(@DestinationVariable final String matchId,
                                                final ChatMessageRequest request,
                                                final Principal principal) {
        final String senderName = (principal != null) ? principal.getName() : request.getSender();

        final ChatMessageResponse response = ChatMessageResponse.builder()
                .sender(senderName)
                .message(request.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        chatService.addMessage(matchId, response);
        return response;
    }
}
