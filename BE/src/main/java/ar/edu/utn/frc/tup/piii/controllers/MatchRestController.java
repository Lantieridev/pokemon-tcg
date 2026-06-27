package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.CreateMatchRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.GameStateResponseDTO;
import ar.edu.utn.frc.tup.piii.services.PlayerPerspectiveMapper;
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

import java.security.Principal;
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
    private final ar.edu.utn.frc.tup.piii.services.MatchService matchService;

    /**
     * Constructs the controller.
     *
     * @param registry              the registry holding match sessions
     * @param perspectiveMapper     maps state to a player's perspective
     * @param matchCreationService  service to create matches
     * @param cardResolutionService service to resolve card references
     * @param matchService          service to surrender matches
     */
    public MatchRestController(final MatchSessionRegistry registry,
                               final PlayerPerspectiveMapper perspectiveMapper,
                               final MatchCreationService matchCreationService,
                               final CardResolutionService cardResolutionService,
                               final ar.edu.utn.frc.tup.piii.services.MatchService matchService) {
        this.registry = Objects.requireNonNull(registry, "Registry cannot be null");
        this.perspectiveMapper = Objects.requireNonNull(perspectiveMapper, "Mapper cannot be null");
        this.matchCreationService = Objects.requireNonNull(matchCreationService);
        this.cardResolutionService = Objects.requireNonNull(cardResolutionService);
        this.matchService = Objects.requireNonNull(matchService);
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
                request.playerAId(), request.playerBId(), deckA, deckB, false);
        return Map.of("matchId", matchId);
    }

    /**
     * Creates a new match against a Bot.
     * Uses the player's deck and uses a copy of the player's deck (or deck ID 1 if none provided) for the bot.
     *
     * @param request body containing playerAId and deckAId (never null)
     * @return a map with a single entry {@code matchId → UUID string}
     */
    @PostMapping("/bot")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createBotMatch(@RequestBody final CreateMatchRequestDTO request, Principal principal) {
        if (principal == null || !principal.getName().equals(request.playerAId())) {
            throw new org.springframework.security.access.AccessDeniedException("Cannot create bot match for another player");
        }
        
        final List<Card> deckA = cardResolutionService.resolveCards(request.deckAId());
        
        // For the bot, just use the same deck as player A for simplicity, or hardcode a deck ID if preferred.
        // In this basic version, playing a mirror match is the easiest way to ensure the bot has a valid deck.
        final List<Card> deckB = cardResolutionService.resolveCards(request.deckAId());
        
        final String matchId = matchCreationService.createMatch(
                request.playerAId(), "Bot-001", deckA, deckB, false);
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
                                         @RequestHeader("X-Player-Id") final String playerId,
                                         final java.security.Principal principal) {
        if (principal == null) {
            throw new org.springframework.security.authentication.BadCredentialsException("User must be authenticated");
        }
        if (!principal.getName().equals(playerId)) {
            throw new org.springframework.security.access.AccessDeniedException("Cannot access another player's state");
        }
        final MatchSession session = registry.find(matchId)
                .orElseThrow(() -> new NoSuchElementException("Match not found: " + matchId));
        final int viewerIndex = session.indexOf(playerId);
        return perspectiveMapper.toResponse(session, viewerIndex);
    }

    /**
     * Allows a player to explicitly surrender/abandon a match.
     *
     * @param matchId  path variable identifying the match
     * @param playerId header identifying the requesting player
     */
    @PostMapping("/{matchId}/surrender")
    @ResponseStatus(HttpStatus.OK)
    public void surrender(@PathVariable final String matchId,
                          @RequestHeader("X-Player-Id") final String playerId,
                          final Principal principal) {
        matchService.surrenderMatch(matchId, playerId);
    }
}
