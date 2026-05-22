package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.CreateMatchRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.GameStateResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.PlayerPerspectiveMapper;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.services.CardResolutionService;
import ar.edu.utn.frc.tup.piii.services.MatchCreationService;
import ar.edu.utn.frc.tup.piii.services.MatchSessionRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
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
    private final MatchCreationService matchCreationService;
    private final CardResolutionService cardResolutionService;

    /**
     * Constructs the controller with its required collaborators.
     *
     * @param registry              holds all active sessions (never null)
     * @param perspectiveMapper     builds war-fog-safe per-player responses (never null)
     * @param matchCreationService  orchestrates match creation and setup (never null)
     * @param cardResolutionService resolves deck IDs to card lists (never null)
     */
    public MatchRestController(final MatchSessionRegistry registry,
                               final PlayerPerspectiveMapper perspectiveMapper,
                               final MatchCreationService matchCreationService,
                               final CardResolutionService cardResolutionService) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.perspectiveMapper = Objects.requireNonNull(perspectiveMapper,
                "perspectiveMapper must not be null");
        this.matchCreationService = Objects.requireNonNull(matchCreationService,
                "matchCreationService must not be null");
        this.cardResolutionService = Objects.requireNonNull(cardResolutionService,
                "cardResolutionService must not be null");
    }

    /**
     * Creates a new match between two players using their saved decks.
     * Resolves both decks via {@link CardResolutionService}, runs the Setup Phase,
     * and returns the new match identifier.
     *
     * @param request body containing playerAId, playerBId, deckAId, deckBId (never null)
     * @return a map with a single entry {@code matchId → UUID string}
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createMatch(@RequestBody final CreateMatchRequestDTO request) {
        final List<Card> deckA = cardResolutionService.resolveCards(request.deckAId());
        final List<Card> deckB = cardResolutionService.resolveCards(request.deckBId());
        final String matchId = matchCreationService.createMatch(
                request.playerAId(), request.playerBId(), deckA, deckB);
        return Map.of("matchId", matchId);
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
