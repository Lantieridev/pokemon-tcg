package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.CreateMatchRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.GameStateResponseDTO;
import ar.edu.utn.frc.tup.piii.services.PlayerPerspectiveMapper;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.services.CardResolutionService;
import ar.edu.utn.frc.tup.piii.services.MatchCreationService;
import ar.edu.utn.frc.tup.piii.services.MatchSessionRegistry;
import ar.edu.utn.frc.tup.piii.services.deck.DeckService;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchRestControllerTest {

    @Mock
    private MatchSessionRegistry registry;

    @Mock
    private PlayerPerspectiveMapper perspectiveMapper;

    @Mock
    private MatchCreationService matchCreationService;

    @Mock
    private CardResolutionService cardResolutionService;

    @Mock
    private ar.edu.utn.frc.tup.piii.services.MatchService matchService;

    @Mock
    private DeckService deckService;

    private MatchRestController controller;

    @BeforeEach
    void setUp() {
        controller = new MatchRestController(
                registry, perspectiveMapper, matchCreationService, cardResolutionService, matchService, deckService);
    }

    @Test
    void shouldReturnMatchStateWhenGetStateIsCalled() {
        final String matchId = "match-1";
        final String playerId = "player-a";

        final MatchSession session = mock(MatchSession.class);
        when(session.indexOf(playerId)).thenReturn(0);
        when(registry.find(matchId)).thenReturn(Optional.of(session));

        final GameStateResponseDTO expected = mock(GameStateResponseDTO.class);
        when(perspectiveMapper.toResponse(session, 0)).thenReturn(expected);

        final GameStateResponseDTO result = controller.getState(matchId, playerId, () -> playerId);

        assertSame(expected, result);
    }

    @Test
    void shouldThrowWhenMatchNotFound() {
        final String matchId = "unknown-match";
        final String playerId = "player-a";

        when(registry.find(matchId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> controller.getState(matchId, playerId, () -> playerId));
    }

    @Test
    void shouldReturnMatchIdWhenCreateMatchIsCalled() {
        final String playerAId = "player-a";
        final String playerBId = "player-b";
        final Long deckAId = 1L;
        final Long deckBId = 2L;
        final String expectedMatchId = "new-match-id";

        final List<Card> deckA = List.of();
        final List<Card> deckB = List.of();

        final CreateMatchRequestDTO request = new CreateMatchRequestDTO(
                playerAId, playerBId, deckAId, deckBId);

        when(deckService.getById(eq(deckAId), eq(playerAId))).thenReturn(mock(DeckResponseDTO.class));
        when(deckService.getById(eq(deckBId), eq(playerBId))).thenReturn(mock(DeckResponseDTO.class));
        when(cardResolutionService.resolveCards(deckAId)).thenReturn(deckA);
        when(cardResolutionService.resolveCards(deckBId)).thenReturn(deckB);
        when(matchCreationService.createMatch(playerAId,  playerBId,  deckA,  deckB, false))
                .thenReturn(expectedMatchId);

        final Map<String, String> result = controller.createMatch(request, () -> playerAId);

        assertEquals(expectedMatchId, result.get("matchId"));
    }

    @Test
    void shouldReturnMatchIdWhenCreateBotMatchIsCalled() {
        final String playerAId = "player-a";
        final Long deckAId = 1L;
        final String expectedMatchId = "bot-match-id";
        final List<Card> deckA = List.of();

        final CreateMatchRequestDTO request = new CreateMatchRequestDTO(playerAId, null, deckAId, null);

        when(deckService.getById(deckAId, playerAId)).thenReturn(mock(DeckResponseDTO.class));
        when(cardResolutionService.resolveCards(deckAId)).thenReturn(deckA);
        when(matchCreationService.createMatch(playerAId, "Bot-001", deckA, deckA, false))
                .thenReturn(expectedMatchId);

        final Map<String, String> result = controller.createBotMatch(request, () -> playerAId);

        assertEquals(expectedMatchId, result.get("matchId"));
    }

    @Test
    void shouldRejectCreateBotMatchWhenCallerIsNotPlayerA() {
        final CreateMatchRequestDTO request = new CreateMatchRequestDTO("player-a", null, 1L, null);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> controller.createBotMatch(request, () -> "someone-else"));
    }

    @Test
    void shouldRejectCreateBotMatchWhenDeckIsNotOwnedByCaller() {
        final String playerAId = "player-a";
        final CreateMatchRequestDTO request = new CreateMatchRequestDTO(playerAId, null, 1L, null);

        when(deckService.getById(1L, playerAId))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("not your deck"));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> controller.createBotMatch(request, () -> playerAId));
    }

    @Test
    void shouldRejectCreateMatchWhenCallerIsNotPlayerA() {
        final CreateMatchRequestDTO request = new CreateMatchRequestDTO("player-a", "player-b", 1L, 2L);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> controller.createMatch(request, () -> "someone-else"));
    }

    @Test
    void shouldRejectCreateMatchWhenEitherDeckIsNotOwnedByItsPlayer() {
        final String playerAId = "player-a";
        final String playerBId = "player-b";
        final CreateMatchRequestDTO request = new CreateMatchRequestDTO(playerAId, playerBId, 1L, 2L);

        when(deckService.getById(1L, playerAId))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("not your deck"));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> controller.createMatch(request, () -> playerAId));
    }

    @Test
    void shouldThrowWhenNullRegistryIsPassedToConstructor() {
        assertThrows(NullPointerException.class,
                () -> new MatchRestController(
                        null, perspectiveMapper, matchCreationService, cardResolutionService, matchService, deckService));
    }

    @Test
    void shouldThrowWhenNullMatchCreationServiceIsPassedToConstructor() {
        assertThrows(NullPointerException.class,
                () -> new MatchRestController(
                        registry, perspectiveMapper, null, cardResolutionService, matchService, deckService));
    }
}
