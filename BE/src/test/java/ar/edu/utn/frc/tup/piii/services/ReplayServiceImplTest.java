package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ReplayResponseDTO;
import ar.edu.utn.frc.tup.piii.persistence.entity.MatchEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.MatchLogEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchLogRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository;
import ar.edu.utn.frc.tup.piii.services.impl.ReplayServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplayServiceImplTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchLogRepository matchLogRepository;

    private ReplayService replayService;

    @BeforeEach
    void setUp() {
        replayService = new ReplayServiceImpl(matchRepository, matchLogRepository);
    }

    @Test
    void shouldReturnReplaySuccessfully() {
        final Long matchId = 1L;
        final MatchEntity match = new MatchEntity();
        match.setId(matchId);

        final UserEntity player = new UserEntity();
        player.setUsername("player1");

        final MatchLogEntity log1 = MatchLogEntity.builder()
                .turnNumber(1)
                .player(player)
                .actionType("DRAW_CARD")
                .result("Drew Pikachu")
                .createdAt(LocalDateTime.now())
                .build();

        final MatchLogEntity log2 = MatchLogEntity.builder()
                .turnNumber(1)
                .player(player)
                .actionType("PLAY_POKEMON")
                .result("Played Pikachu to active")
                .createdAt(LocalDateTime.now().plusSeconds(1))
                .build();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchLogRepository.findByMatchIdOrderByCreatedAtAsc(matchId)).thenReturn(List.of(log1, log2));

        final ReplayResponseDTO replay = replayService.getReplay(matchId);

        assertNotNull(replay);
        assertEquals(matchId, replay.getMatchId());
        assertEquals(2, replay.getEvents().size());
        assertEquals(1, replay.getEvents().get(0).getTurn());
        assertEquals("player1", replay.getEvents().get(0).getPlayer());
        assertEquals("DRAW_CARD", replay.getEvents().get(0).getAction());
        assertEquals("Drew Pikachu", replay.getEvents().get(0).getResult());

        assertEquals("PLAY_POKEMON", replay.getEvents().get(1).getAction());
    }

    @Test
    void shouldThrowExceptionWhenMatchNotFound() {
        final Long matchId = 999L;
        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> replayService.getReplay(matchId));
    }
}
