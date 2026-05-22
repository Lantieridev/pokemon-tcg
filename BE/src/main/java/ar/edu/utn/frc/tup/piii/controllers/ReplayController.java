package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse;
import ar.edu.utn.frc.tup.piii.services.ChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * REST controller for match Replays, Chat histories, and Reports.
 */
@RestController
@RequestMapping("/api")
public class ReplayController {

    private final ChatService chatService;

    public ReplayController(final ChatService chatService) {
        this.chatService = Objects.requireNonNull(chatService, "chatService must not be null");
    }

    /**
     * Retrieves the latest 50 messages of chat history for a match.
     * Secured by default via JWT security filter.
     *
     * @param matchId the match ID
     * @return the list of chat messages
     */
    @GetMapping("/matches/{matchId}/chat")
    public List<ChatMessageResponse> getChatHistory(@PathVariable final String matchId) {
        return chatService.getMessages(matchId);
    }
}
