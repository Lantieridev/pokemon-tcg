package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.LobbyResponseDTO;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.deck.DeckService;

/**
 * Orchestrates public matchmaking (queue) and private room matching.
 *
 * <p>Uses a {@link ReentrantLock} around the critical "join queue" section to prevent
 * race conditions when two players join simultaneously and both could see an empty queue.</p>
 *
 * <p>Once two players are paired, delegates to {@link MatchCreationService} to build the
 * full game session, then notifies both players via WebSocket on their private lobby channels:
 * {@code /topic/lobby/{playerId}}.</p>
 */
@Service
public final class LobbyService {

    private static final String LOBBY_TOPIC = "/topic/lobby/";

    private final LobbyQueue lobbyQueue;
    private final MatchCreationService matchCreationService;
    private final CardResolutionService cardResolutionService;
    private final SimpMessagingTemplate messaging;
    private final PenaltyService penaltyService;
    private final UserRepository userRepository;
    private final DeckService deckService;

    /** Guards the "poll + enqueue" section to avoid pairing a player with themselves
     * or creating duplicate matches when two requests arrive concurrently. */
    private final ReentrantLock queueLock = new ReentrantLock();

    /**
     * Constructs the service with all required collaborators.
     *
     * @param lobbyQueue           holds the queue and private rooms (never null)
     * @param matchCreationService creates and initialises match sessions (never null)
     * @param cardResolutionService resolves deck IDs to card lists (never null)
     * @param messaging            broadcasts WebSocket events (never null)
     */
    public LobbyService(final LobbyQueue lobbyQueue,
                        final MatchCreationService matchCreationService,
                        final CardResolutionService cardResolutionService,
                        final SimpMessagingTemplate messaging,
                        final PenaltyService penaltyService,
                        final UserRepository userRepository,
                        final DeckService deckService) {
        this.lobbyQueue = Objects.requireNonNull(lobbyQueue, "lobbyQueue must not be null");
        this.matchCreationService = Objects.requireNonNull(matchCreationService,
                "matchCreationService must not be null");
        this.cardResolutionService = Objects.requireNonNull(cardResolutionService,
                "cardResolutionService must not be null");
        this.messaging = Objects.requireNonNull(messaging, "messaging must not be null");
        this.penaltyService = Objects.requireNonNull(penaltyService, "penaltyService must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.deckService = Objects.requireNonNull(deckService, "deckService must not be null");
    }

    // ── Public matchmaking ────────────────────────────────────────────────────

    /**
     * Attempts to join the public matchmaking queue.
     *
     * <p>If another player is already waiting, pairs them immediately and creates a match.
     * Otherwise, adds the requesting player to the queue and returns a WAITING response.
     * The paired players are also notified via WebSocket so they can react
     * (e.g., auto-navigate) even if the HTTP response arrived first.</p>
     *
     * @param playerId the authenticated player's username (never null)
     * @param deckId   the deck they want to use (never null)
     * @param isRanked true if searching for a ranked match
     * @return {@link LobbyResponseDTO#queued()} if waiting, or
     *         {@link LobbyResponseDTO#matchReady(String, String)} if a match was created
     * @throws IllegalArgumentException if {@code deckId} is invalid
     */
    public LobbyResponseDTO joinQueue(final String playerId, final Long deckId, final Boolean isRanked) {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(deckId, "deckId must not be null");
        deckService.getById(deckId, playerId);

        final boolean ranked = Boolean.TRUE.equals(isRanked);

        if (ranked && penaltyService.isRankedBanned(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are currently banned from Ranked matches.");
        }

        queueLock.lock();
        try {
            if (ranked) {
                UserEntity user = userRepository.findFirstByUsername(playerId)
                        .orElseThrow(() -> new NoSuchElementException("User not found"));
                int mmr = user.getMmr();

                final Optional<LobbyQueue.RankedQueueEntry> opponentOpt = lobbyQueue.pollRankedOpponent(playerId, mmr);
                if (opponentOpt.isEmpty()) {
                    lobbyQueue.enqueueRanked(playerId, deckId, mmr);
                    return LobbyResponseDTO.queued();
                }

                final LobbyQueue.RankedQueueEntry opponent = opponentOpt.get();
                final String matchId = createMatch(playerId, deckId, opponent.playerId(), opponent.deckId(), true);

                final LobbyResponseDTO readyForOpponent = LobbyResponseDTO.matchReady(matchId, playerId);
                messaging.convertAndSend(LOBBY_TOPIC + opponent.playerId(), readyForOpponent);

                return LobbyResponseDTO.matchReady(matchId, opponent.playerId());
            } else {
                final Optional<LobbyQueue.QueueEntry> opponentOpt = lobbyQueue.pollOpponent(playerId);

                if (opponentOpt.isEmpty()) {
                    // No opponent yet — add to queue and wait
                    lobbyQueue.enqueue(playerId, deckId);
                    return LobbyResponseDTO.queued();
                }

                // Found an opponent — create the match
                final LobbyQueue.QueueEntry opponent = opponentOpt.get();
                final String matchId = createMatch(playerId, deckId, opponent.playerId(), opponent.deckId(), false);

                // Notify both via WS (the joining player gets the response directly over HTTP)
                final LobbyResponseDTO readyForOpponent = LobbyResponseDTO.matchReady(matchId, playerId);
                messaging.convertAndSend(LOBBY_TOPIC + opponent.playerId(), readyForOpponent);

                return LobbyResponseDTO.matchReady(matchId, opponent.playerId());
            }

        } finally {
            queueLock.unlock();
        }
    }

    /**
     * Removes a player from the public matchmaking queue.
     * No-op if the player is not currently queued.
     *
     * @param playerId the player's username (never null)
     * @return {@code true} if the player was removed, {@code false} if they weren't queued
     */
    public boolean leaveQueue(final String playerId) {
        Objects.requireNonNull(playerId, "playerId must not be null");
        return lobbyQueue.removeFromQueue(playerId);
    }

    /**
     * Returns whether a player is currently in the public matchmaking queue.
     *
     * @param playerId the player's username (never null)
     */
    public boolean isInQueue(final String playerId) {
        Objects.requireNonNull(playerId, "playerId must not be null");
        return lobbyQueue.isInQueue(playerId);
    }

    // ── Private rooms ─────────────────────────────────────────────────────────

    /**
     * Creates a private room for the given player.
     * The returned room code should be shared with the desired opponent out-of-band.
     *
     * @param playerId the room creator's username (never null)
     * @param deckId   the creator's chosen deck (never null)
     * @return a WAITING response containing the room code
     */
    public LobbyResponseDTO createRoom(final String playerId, final Long deckId) {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(deckId, "deckId must not be null");
        deckService.getById(deckId, playerId);

        final String roomCode = lobbyQueue.createRoom(playerId, deckId);
        return LobbyResponseDTO.waiting(roomCode);
    }

    /**
     * Joins an existing private room by code and creates the match.
     *
     * <p>Both the joining player (via HTTP response) and the room creator (via WebSocket)
     * are notified with a {@code MATCH_READY} event.</p>
     *
     * @param roomCode the 6-character room code (never null, case-insensitive)
     * @param joiningPlayerId the joining player's username (never null)
     * @param deckId          the joining player's chosen deck (never null)
     * @return a {@link LobbyResponseDTO#matchReady(String, String)} response
     * @throws NoSuchElementException   if the room code does not exist
     * @throws IllegalArgumentException if the joining player is the same as the creator
     */
    public LobbyResponseDTO joinRoom(final String roomCode,
                                     final String joiningPlayerId,
                                     final Long deckId) {
        Objects.requireNonNull(roomCode, "roomCode must not be null");
        Objects.requireNonNull(joiningPlayerId, "joiningPlayerId must not be null");
        Objects.requireNonNull(deckId, "deckId must not be null");
        deckService.getById(deckId, joiningPlayerId);

        final LobbyQueue.RoomEntry room = lobbyQueue.consumeRoom(roomCode)
                .orElseThrow(() -> new NoSuchElementException("Room not found: " + roomCode));

        if (room.creatorId().equals(joiningPlayerId)) {
            // Re-put the room so the creator can try again
            lobbyQueue.createRoom(room.creatorId(), room.deckId());
            throw new IllegalArgumentException("You cannot join your own room");
        }

        final String matchId = createMatch(
                room.creatorId(), room.deckId(),
                joiningPlayerId, deckId, false);

        // Notify creator via WebSocket
        final LobbyResponseDTO readyForCreator = LobbyResponseDTO.matchReady(matchId, joiningPlayerId);
        messaging.convertAndSend(LOBBY_TOPIC + room.creatorId(), readyForCreator);

        return LobbyResponseDTO.matchReady(matchId, room.creatorId());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String createMatch(final String playerAId,
                               final Long deckAId,
                               final String playerBId,
                               final Long deckBId,
                               final boolean isRanked) {
        final List<Card> deckA = cardResolutionService.resolveCards(deckAId);
        final List<Card> deckB = cardResolutionService.resolveCards(deckBId);
        // We will pass isRanked to matchCreationService
        return matchCreationService.createMatch(playerAId, playerBId, deckA, deckB, isRanked);
    }
}
