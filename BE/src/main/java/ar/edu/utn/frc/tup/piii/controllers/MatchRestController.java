package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.GameStateResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.PlayerPerspectiveMapper;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.services.MatchSessionRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * REST controller exposing match-state endpoints for polling clients.
 * The response enforces war-fog: the opponent's hand is reduced to a count only.
 */
@RestController
@RequestMapping("/api/matches")
public final class MatchRestController {

    private final MatchSessionRegistry registry;
    private final PlayerPerspectiveMapper perspectiveMapper;

    /**
     * Constructs the controller with its required collaborators.
     *
     * @param registry          holds all active sessions (never null)
     * @param perspectiveMapper builds war-fog-safe per-player responses (never null)
     */
    public MatchRestController(final MatchSessionRegistry registry,
                               final PlayerPerspectiveMapper perspectiveMapper) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.perspectiveMapper = Objects.requireNonNull(perspectiveMapper, "perspectiveMapper must not be null");
    }

    /**
     * Returns the current match state from the requesting player's perspective.
     *
     * @param matchId  path variable identifying the match
     * @param playerId header identifying the requesting player
     * @return a war-fog-safe game state DTO
     * @throws NoSuchElementException if the match does not exist
     */
    @GetMapping("/{matchId}/state")
    public GameStateResponseDTO getState(@PathVariable final String matchId,
                                         @RequestHeader("X-Player-Id") final String playerId) {
        final MatchSession session = registry.find(matchId)
                .orElseThrow(() -> new NoSuchElementException("Match not found: " + matchId));
        final int viewerIndex = session.indexOf(playerId);
        return perspectiveMapper.toResponse(session, viewerIndex);
    }
}
