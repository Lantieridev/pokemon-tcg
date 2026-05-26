package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.ActionType;
import ar.edu.utn.frc.tup.piii.engine.exception.InvalidActionException;
import ar.edu.utn.frc.tup.piii.services.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameWebSocketControllerTest {

    @Mock
    private MatchService matchService;

    private GameWebSocketController controller;

    @BeforeEach
    void setUp() {
        controller = new GameWebSocketController(matchService);
    }

    @Test
    void shouldRouteActionToMatchServiceWhenMessageMappingIsCalled() {
        final String matchId = "match-1";
        final String playerId = "player-a";
        final ActionRequestDTO dto = new ActionRequestDTO(ActionType.PLACE_BASIC_POKEMON, null, null, null, null, null);

        controller.handleAction(matchId, playerId, () -> playerId, dto);

        verify(matchService).processAction(matchId, playerId, dto);
    }

    @Test
    void shouldThrowWhenMatchServiceThrows() {
        final String matchId = "match-1";
        final String playerId = "player-a";
        final ActionRequestDTO dto = new ActionRequestDTO(ActionType.DECLARE_ATTACK, null, null, null, null, 0);

        doThrow(new InvalidActionException("attack not valid"))
                .when(matchService).processAction(matchId, playerId, dto);

        assertThrows(InvalidActionException.class,
                () -> controller.handleAction(matchId, playerId, () -> playerId, dto));
    }
}
