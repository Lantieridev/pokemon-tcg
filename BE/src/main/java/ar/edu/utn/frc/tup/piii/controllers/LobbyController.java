package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.LobbyJoinRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.LobbyResponseDTO;
import ar.edu.utn.frc.tup.piii.services.LobbyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;
import java.util.Objects;

/**
 * REST controller exposing lobby endpoints for public matchmaking and private room management.
 *
 * <p>All endpoints require an authenticated user (JWT via Spring Security).
 * The player's identity is always taken from {@link Principal#getName()} — never from a request
 * body field — to prevent impersonation.</p>
 *
 * <p>WebSocket channel for lobby events: {@code /topic/lobby/{username}}</p>
 */
@RestController
@RequestMapping("/api/lobby")
public final class LobbyController {

    private final LobbyService lobbyService;

    /**
     * Constructs the controller with its required service.
     *
     * @param lobbyService handles matchmaking and room logic (never null)
     */
    public LobbyController(final LobbyService lobbyService) {
        this.lobbyService = Objects.requireNonNull(lobbyService, "lobbyService must not be null");
    }

    // ── Public matchmaking queue ──────────────────────────────────────────────

    /**
     * Joins the public matchmaking queue.
     *
     * <p>If another player is already waiting, the match is created immediately and the response
     * contains {@code status: "MATCH_READY"} with the {@code matchId}.
     * Otherwise, the response is {@code status: "WAITING"} and the player waits for a WebSocket
     * notification on {@code /topic/lobby/{username}} when a match is found.</p>
     *
     * @param principal the authenticated user (injected by Spring Security, never null)
     * @param request   body containing the chosen {@code deckId}
     * @return lobby response (WAITING or MATCH_READY)
     */
    @PostMapping("/queue")
    @ResponseStatus(HttpStatus.OK)
    public LobbyResponseDTO joinQueue(final Principal principal,
                                      @RequestBody final LobbyJoinRequestDTO request) {
        Objects.requireNonNull(principal, "User must be authenticated");
        return lobbyService.joinQueue(principal.getName(), request.deckId());
    }

    /**
     * Leaves the public matchmaking queue.
     * No-op and returns 204 if the player is not currently queued.
     *
     * @param principal the authenticated user
     * @return 204 No Content
     */
    @DeleteMapping("/queue")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveQueue(final Principal principal) {
        Objects.requireNonNull(principal, "User must be authenticated");
        lobbyService.leaveQueue(principal.getName());
    }

    /**
     * Returns the current queue status for the authenticated player.
     *
     * @param principal the authenticated user
     * @return a map with {@code "inQueue": true/false}
     */
    @GetMapping("/queue/status")
    public Map<String, Boolean> queueStatus(final Principal principal) {
        Objects.requireNonNull(principal, "User must be authenticated");
        return Map.of("inQueue", lobbyService.isInQueue(principal.getName()));
    }

    // ── Private rooms ─────────────────────────────────────────────────────────

    /**
     * Creates a private room and returns the room code.
     * The code should be shared out-of-band with the intended opponent.
     *
     * @param principal the authenticated user
     * @param request   body containing the chosen {@code deckId}
     * @return WAITING response with {@code roomCode}
     */
    @PostMapping("/room")
    @ResponseStatus(HttpStatus.CREATED)
    public LobbyResponseDTO createRoom(final Principal principal,
                                       @RequestBody final LobbyJoinRequestDTO request) {
        Objects.requireNonNull(principal, "User must be authenticated");
        return lobbyService.createRoom(principal.getName(), request.deckId());
    }

    /**
     * Joins an existing private room by its code.
     *
     * <p>The room creator is notified via WebSocket on {@code /topic/lobby/{creatorUsername}}.
     * This response also contains {@code status: "MATCH_READY"} with the new {@code matchId}.</p>
     *
     * @param roomCode  the 6-character code (path variable, case-insensitive)
     * @param principal the authenticated joining player
     * @param request   body containing the joining player's chosen {@code deckId}
     * @return MATCH_READY response with {@code matchId} and {@code opponentId}
     */
    @PostMapping("/room/{roomCode}/join")
    @ResponseStatus(HttpStatus.OK)
    public LobbyResponseDTO joinRoom(@PathVariable final String roomCode,
                                     final Principal principal,
                                     @RequestBody final LobbyJoinRequestDTO request) {
        Objects.requireNonNull(principal, "User must be authenticated");
        return lobbyService.joinRoom(roomCode, principal.getName(), request.deckId());
    }
}
