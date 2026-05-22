package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse;
import java.util.List;

/**
 * Service interface for handling in-game chat.
 */
public interface ChatService {

    /**
     * Adds a chat message to the in-memory cache for a specific match.
     * Keeps only the latest 50 messages (FIFO).
     *
     * @param matchId the match ID
     * @param message the chat message response DTO
     */
    void addMessage(String matchId, ChatMessageResponse message);

    /**
     * Retrieves the list of cached chat messages for a specific match.
     *
     * @param matchId the match ID
     * @return the list of cached chat messages, or an empty list if none
     */
    List<ChatMessageResponse> getMessages(String matchId);

    /**
     * Clears all cached chat messages for a specific match.
     *
     * @param matchId the match ID
     */
    void clearMessages(String matchId);
}
