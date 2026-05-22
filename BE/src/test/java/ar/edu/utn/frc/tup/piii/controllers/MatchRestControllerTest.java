package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.CreateMatchRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.GameStateResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.PlayerPerspectiveMapper;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.services.CardResolutionService;
import ar.edu.utn.frc.tup.piii.services.MatchCreationService;
import ar.edu.utn.frc.tup.piii.services.MatchSessionRegistry;
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

    private MatchRestController controller;

    @BeforeEach
    void setUp() {
        controller = new MatchRestController(
                registry, perspectiveMapper, matchCreationService, cardResolutionService);
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

        final GameStateResponseDTO result = controller.getState(matchId, playerId);

        assertSame(expected, result);
    }

    @Test
    void shouldThrowWhenMatchNotFound() {
        final String matchId = "unknown-match";
        final String playerId = "player-a";

        when(registry.find(matchId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> controller.getState(matchId, playerId));
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

        when(cardResolutionService.resolveCards(deckAId)).thenReturn(deckA);
        when(cardResolutionService.resolveCards(deckBId)).thenReturn(deckB);
        when(matchCreationService.createMatch(playerAId, playerBId, deckA, deckB))
                .thenReturn(expectedMatchId);

        final Map<String, String> result = controller.createMatch(request);

        assertEquals(expectedMatchId, result.get("matchId"));
    }

    @Test
    void shouldThrowWhenNullRegistryIsPassedToConstructor() {
        assertThrows(NullPointerException.class,
                () -> new MatchRestController(
                        null, perspectiveMapper, matchCreationService, cardResolutionService));
    }

    @Test
    void shouldThrowWhenNullMatchCreationServiceIsPassedToConstructor() {
        assertThrows(NullPointerException.class,
                () -> new MatchRestController(
                        registry, perspectiveMapper, null, cardResolutionService));
    }
}
