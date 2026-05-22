package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse;
import ar.edu.utn.frc.tup.piii.dtos.ChatReportRequest;
import ar.edu.utn.frc.tup.piii.dtos.ReplayResponseDTO;
import ar.edu.utn.frc.tup.piii.services.ChatService;
import ar.edu.utn.frc.tup.piii.services.ReplayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final ReplayService replayService;

    public ReplayController(final ChatService chatService, final ReplayService replayService) {
        this.chatService = Objects.requireNonNull(chatService, "chatService must not be null");
        this.replayService = Objects.requireNonNull(replayService, "replayService must not be null");
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

    /**
     * Creates a behavior report for a player in a match.
     *
     * @param matchId the match ID
     * @param request the chat report request details
     * @return 200 OK if created successfully
     */
    @PostMapping("/matches/{matchId}/reports")
    public ResponseEntity<Void> createChatReport(@PathVariable final String matchId,
                                                 @RequestBody final ChatReportRequest request) {
        chatService.createReport(matchId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves the replay events for a given match.
     * Secured by default via JWT security filter.
     *
     * @param matchId the match ID
     * @return the replay response DTO
     */
    @GetMapping("/matches/{matchId}/replay")
    public ReplayResponseDTO getReplay(@PathVariable final Long matchId) {
        return replayService.getReplay(matchId);
    }
}
