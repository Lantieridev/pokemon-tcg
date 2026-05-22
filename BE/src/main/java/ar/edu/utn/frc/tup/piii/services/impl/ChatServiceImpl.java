package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse;
import ar.edu.utn.frc.tup.piii.dtos.ChatReportRequest;
import ar.edu.utn.frc.tup.piii.persistence.entity.ChatReportEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.MatchEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.ChatReportRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.ChatService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Service implementation for managing in-memory chat cache and behavior reports.
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final int MAX_CACHE_SIZE = 50;

    // In-memory cache to hold the last 50 chat messages per match ID.
    private final Map<String, Queue<ChatMessageResponse>> chatCache = new ConcurrentHashMap<>();

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final ChatReportRepository chatReportRepository;

    public ChatServiceImpl(final MatchRepository matchRepository,
                           final UserRepository userRepository,
                           final ChatReportRepository chatReportRepository) {
        this.matchRepository = Objects.requireNonNull(matchRepository, "matchRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.chatReportRepository = Objects.requireNonNull(chatReportRepository, "chatReportRepository must not be null");
    }

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

    @Override
    public void createReport(final String matchId, final ChatReportRequest request) {
        if (matchId == null || request == null) {
            return;
        }
        final Long matchLongId = Long.valueOf(matchId);
        final MatchEntity match = matchRepository.findById(matchLongId)
                .orElseThrow(() -> new NoSuchElementException("Match not found with id: " + matchId));
        final UserEntity reporter = userRepository.findById(request.getReporterId())
                .orElseThrow(() -> new NoSuchElementException("Reporter user not found with id: " + request.getReporterId()));
        final UserEntity reported = userRepository.findById(request.getReportedId())
                .orElseThrow(() -> new NoSuchElementException("Reported user not found with id: " + request.getReportedId()));

        final List<ChatMessageResponse> history = getMessages(matchId);

        final ChatReportEntity reportEntity = ChatReportEntity.builder()
                .match(match)
                .reporter(reporter)
                .reported(reported)
                .reason(request.getReason())
                .chatHistory(history)
                .build();

        chatReportRepository.save(reportEntity);
    }
}
