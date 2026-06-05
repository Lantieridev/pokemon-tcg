package ar.edu.utn.frc.tup.piii.services;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread-safe in-memory store for the public matchmaking queue and private rooms.
 *
 * <p>Both structures are held in-memory only — they are intentionally not persisted.
 * If the server restarts, pending queue entries and open rooms are discarded (acceptable
 * for the current testing scope).</p>
 */
@Component
public final class LobbyQueue {

    private static final String ROOM_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int ROOM_CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** FIFO queue of players waiting for a public match. */
    private final ConcurrentLinkedQueue<QueueEntry> publicQueue = new ConcurrentLinkedQueue<>();

    /** Active private rooms: roomCode → room data. */
    private final ConcurrentHashMap<String, RoomEntry> privateRooms = new ConcurrentHashMap<>();

    // ── Inner records ─────────────────────────────────────────────────────────

    /**
     * An entry in the public matchmaking queue.
     *
     * @param playerId the username of the waiting player
     * @param deckId   the deck they want to use
     */
    public record QueueEntry(String playerId, Long deckId) {
    }

    /**
     * A pending private room created by one player and waiting for another.
     *
     * @param roomCode  the 6-character alphanumeric code to share with the opponent
     * @param creatorId the username of the room creator
     * @param deckId    the deck the creator wants to use
     */
    public record RoomEntry(String roomCode, String creatorId, Long deckId) {
    }

    // ── Public queue operations ───────────────────────────────────────────────

    /**
     * Adds a player to the public matchmaking queue.
     * No-op if the player is already queued (prevents duplicates).
     *
     * @param playerId the player's username (never null)
     * @param deckId   the deck to use (never null)
     */
    public void enqueue(final String playerId, final Long deckId) {
        final boolean alreadyQueued = publicQueue.stream()
                .anyMatch(e -> e.playerId().equals(playerId));
        if (!alreadyQueued) {
            publicQueue.add(new QueueEntry(playerId, deckId));
        }
    }

    /**
     * Polls the first queued opponent who is NOT {@code requestingPlayerId}.
     * Removes and returns that entry atomically.
     *
     * @param requestingPlayerId the player requesting a match (must not match their own entry)
     * @return an Optional containing the matched opponent, or empty if none available
     */
    public Optional<QueueEntry> pollOpponent(final String requestingPlayerId) {
        // Iterate to find the first entry that belongs to a different player
        for (final QueueEntry entry : publicQueue) {
            if (!entry.playerId().equals(requestingPlayerId)) {
                // Remove it atomically; if another thread already removed it, skip
                if (publicQueue.remove(entry)) {
                    return Optional.of(entry);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Removes a player from the public queue (used when they cancel the search).
     *
     * @param playerId the player to remove
     * @return {@code true} if the player was in the queue and was removed
     */
    public boolean removeFromQueue(final String playerId) {
        return publicQueue.removeIf(e -> e.playerId().equals(playerId));
    }

    /**
     * Returns {@code true} if the player is currently in the public queue.
     *
     * @param playerId the player's username
     */
    public boolean isInQueue(final String playerId) {
        return publicQueue.stream().anyMatch(e -> e.playerId().equals(playerId));
    }

    // ── Private room operations ───────────────────────────────────────────────

    /**
     * Creates a new private room for the given player and returns its unique code.
     * If the player already has an open room, it is replaced.
     *
     * @param creatorId the room creator's username (never null)
     * @param deckId    the creator's chosen deck (never null)
     * @return the generated 6-character room code
     */
    public String createRoom(final String creatorId, final Long deckId) {
        // Remove any previously open room by this creator
        privateRooms.values().removeIf(r -> r.creatorId().equals(creatorId));

        final String code = generateRoomCode();
        privateRooms.put(code, new RoomEntry(code, creatorId, deckId));
        return code;
    }

    /**
     * Finds and removes a private room by its code.
     *
     * @param roomCode the 6-character code
     * @return an Optional containing the room entry, or empty if not found
     */
    public Optional<RoomEntry> consumeRoom(final String roomCode) {
        final RoomEntry entry = privateRooms.remove(roomCode.toUpperCase());
        return Optional.ofNullable(entry);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generateRoomCode() {
        String code;
        do {
            final StringBuilder sb = new StringBuilder(ROOM_CODE_LENGTH);
            for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
                sb.append(ROOM_CODE_CHARS.charAt(RANDOM.nextInt(ROOM_CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (privateRooms.containsKey(code)); // Ensure uniqueness
        return code;
    }
}
