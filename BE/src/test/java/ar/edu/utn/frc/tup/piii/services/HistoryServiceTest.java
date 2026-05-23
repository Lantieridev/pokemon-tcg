package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.MatchHistoryDto;
import ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository;
import ar.edu.utn.frc.tup.piii.services.impl.HistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HistoryServiceTest {

    @Mock
    private MatchRepository matchRepository;

    private HistoryService historyService;

    @BeforeEach
    void setUp() {
        historyService = new HistoryServiceImpl(matchRepository);
    }

    @Test
    void testHistorySemanticMapping() {
        final String currentUser = "player-alice";
        final PageRequest pageable = PageRequest.of(0, 10);
        final LocalDateTime now = LocalDateTime.now();

        // 1. Mock database projections
        final List<MatchHistoryProjectionDto> projections = List.of(
                // Match 1: ACTIVE (IN_PROGRESS), opponent player-bob
                new MatchHistoryProjectionDto(1L, "ACTIVE", "player-alice", "player-bob", null, now),
                // Match 2: FINISHED (VICTORY), current user won, opponent player-bob
                new MatchHistoryProjectionDto(2L, "FINISHED", "player-alice", "player-bob", "player-alice", now),
                // Match 3: FINISHED (DEFEAT), opponent player-charlie won
                new MatchHistoryProjectionDto(3L, "FINISHED", "player-charlie", "player-alice", "player-charlie", now),
                // Match 4: FINISHED (TIE), winner is null
                new MatchHistoryProjectionDto(4L, "FINISHED", "player-alice", "player-bob", null, now),
                // Match 5: ACTIVE (IN_PROGRESS), opponent player2 is null (waiting for player)
                new MatchHistoryProjectionDto(5L, "ACTIVE", "player-alice", null, null, now)
        );

        final Slice<MatchHistoryProjectionDto> sliceMock = new SliceImpl<>(projections, pageable, false);
        when(matchRepository.findUserMatchHistory(currentUser, pageable)).thenReturn(sliceMock);

        // 2. Call service
        final Slice<MatchHistoryDto> result = historyService.getUserMatchHistory(currentUser, pageable);

        // 3. Assertions
        assertNotNull(result);
        final List<MatchHistoryDto> content = result.getContent();
        assertEquals(5, content.size());

        // Match 1
        assertEquals(1L, content.get(0).matchId());
        assertEquals("player-bob", content.get(0).opponent());
        assertEquals("ACTIVE", content.get(0).status());
        assertEquals("IN_PROGRESS", content.get(0).result());

        // Match 2
        assertEquals(2L, content.get(1).matchId());
        assertEquals("player-bob", content.get(1).opponent());
        assertEquals("FINISHED", content.get(1).status());
        assertEquals("VICTORY", content.get(1).result());

        // Match 3
        assertEquals(3L, content.get(2).matchId());
        assertEquals("player-charlie", content.get(2).opponent());
        assertEquals("FINISHED", content.get(2).status());
        assertEquals("DEFEAT", content.get(2).result());

        // Match 4
        assertEquals(4L, content.get(3).matchId());
        assertEquals("player-bob", content.get(3).opponent());
        assertEquals("FINISHED", content.get(3).status());
        assertEquals("TIE", content.get(3).result());

        // Match 5
        assertEquals(5L, content.get(4).matchId());
        assertEquals("Waiting...", content.get(4).opponent());
        assertEquals("ACTIVE", content.get(4).status());
        assertEquals("IN_PROGRESS", content.get(4).result());
    }
}
