package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse;
import ar.edu.utn.frc.tup.piii.services.ChatService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Service implementation for managing in-memory chat cache.
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final int MAX_CACHE_SIZE = 50;

    // In-memory cache to hold the last 50 chat messages per match ID.
    private final Map<String, Queue<ChatMessageResponse>> chatCache = new ConcurrentHashMap<>();

    @Override
    public void addMessage(final String matchId, final ChatMessageResponse message) {
        if (matchId == null || message == null) {
            return;
        }
        chatCache.compute(matchId, (id, queue) -> {
            final Queue<ChatMessageResponse> activeQueue = (queue != null) ? queue : new ConcurrentLinkedQueue<>();
            activeQueue.add(message);
            while (activeQueue.size() > MAX_CACHE_SIZE) {
                activeQueue.poll();
            }
            return activeQueue;
        });
    }

    @Override
    public List<ChatMessageResponse> getMessages(final String matchId) {
        if (matchId == null) {
            return List.of();
        }
        final Queue<ChatMessageResponse> queue = chatCache.get(matchId);
        if (queue == null) {
            return List.of();
        }
        return new ArrayList<>(queue);
    }

    @Override
    public void clearMessages(final String matchId) {
        if (matchId != null) {
            chatCache.remove(matchId);
        }
    }
}
