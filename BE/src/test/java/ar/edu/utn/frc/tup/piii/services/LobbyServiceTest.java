package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.LobbyResponseDTO;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LobbyServiceTest {

    private LobbyQueue lobbyQueue;
    private MatchCreationService matchCreationService;
    private CardResolutionService cardResolutionService;
    private SimpMessagingTemplate messaging;
    private PenaltyService penaltyService;
    private UserRepository userRepository;
    private LobbyService lobbyService;

    @BeforeEach
    public void setUp() {
        lobbyQueue = new LobbyQueue();
        matchCreationService = mock(MatchCreationService.class);
        cardResolutionService = mock(CardResolutionService.class);
        messaging = mock(SimpMessagingTemplate.class);
        penaltyService = mock(PenaltyService.class);
        userRepository = mock(UserRepository.class);
        lobbyService = new LobbyService(
                lobbyQueue,
                matchCreationService,
                cardResolutionService,
                messaging,
                penaltyService,
                userRepository
        );
    }

    @Test
    public void testJoinQueueCasualWaiting() {
        LobbyResponseDTO response = lobbyService.joinQueue("player1", 10L, false);
        assertNotNull(response);
        assertEquals("WAITING", response.status());
        assertTrue(lobbyService.isInQueue("player1"));
    }

    @Test
    public void testJoinQueueCasualMatched() {
        lobbyService.joinQueue("player1", 10L, false);

        List<Card> mockCards1 = List.of(mock(Card.class));
        List<Card> mockCards2 = List.of(mock(Card.class));
        when(cardResolutionService.resolveCards(10L)).thenReturn(mockCards1);
        when(cardResolutionService.resolveCards(20L)).thenReturn(mockCards2);
        when(matchCreationService.createMatch(eq("player2"), eq("player1"), anyList(), anyList(), eq(false)))
                .thenReturn("match-123");

        LobbyResponseDTO response = lobbyService.joinQueue("player2", 20L, false);

        assertNotNull(response);
        assertEquals("MATCH_READY", response.status());
        assertEquals("match-123", response.matchId());
        assertEquals("player1", response.opponentId());

        verify(messaging).convertAndSend(eq("/topic/lobby/player1"), any(LobbyResponseDTO.class));
        assertFalse(lobbyService.isInQueue("player1"));
        assertFalse(lobbyService.isInQueue("player2"));
    }

    @Test
    public void testJoinQueueRankedBanned() {
        when(penaltyService.isRankedBanned("player1")).thenReturn(true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            lobbyService.joinQueue("player1", 10L, true);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("You are currently banned from Ranked matches.", ex.getReason());
    }

    @Test
    public void testJoinQueueRankedUserNotFound() {
        when(penaltyService.isRankedBanned("player1")).thenReturn(false);
        when(userRepository.findFirstByUsername("player1")).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> {
            lobbyService.joinQueue("player1", 10L, true);
        });
    }

    @Test
    public void testJoinQueueRankedWaiting() {
        when(penaltyService.isRankedBanned("player1")).thenReturn(false);
        UserEntity user = UserEntity.builder().username("player1").mmr(1000).build();
        when(userRepository.findFirstByUsername("player1")).thenReturn(Optional.of(user));

        LobbyResponseDTO response = lobbyService.joinQueue("player1", 10L, true);
        assertNotNull(response);
        assertEquals("WAITING", response.status());
        assertTrue(lobbyService.isInQueue("player1"));
    }

    @Test
    public void testJoinQueueRankedMatchedWithinMmrTolerance() {
        // Enqueue first player
        when(penaltyService.isRankedBanned("player1")).thenReturn(false);
        UserEntity user1 = UserEntity.builder().username("player1").mmr(1000).build();
        when(userRepository.findFirstByUsername("player1")).thenReturn(Optional.of(user1));
        lobbyService.joinQueue("player1", 10L, true);

        // Second player joins with mmr 1050 (difference <= 100)
        when(penaltyService.isRankedBanned("player2")).thenReturn(false);
        UserEntity user2 = UserEntity.builder().username("player2").mmr(1050).build();
        when(userRepository.findFirstByUsername("player2")).thenReturn(Optional.of(user2));

        List<Card> mockCards1 = List.of(mock(Card.class));
        List<Card> mockCards2 = List.of(mock(Card.class));
        when(cardResolutionService.resolveCards(10L)).thenReturn(mockCards1);
        when(cardResolutionService.resolveCards(20L)).thenReturn(mockCards2);
        when(matchCreationService.createMatch(eq("player2"), eq("player1"), anyList(), anyList(), eq(true)))
                .thenReturn("match-ranked-456");

        LobbyResponseDTO response = lobbyService.joinQueue("player2", 20L, true);

        assertNotNull(response);
        assertEquals("MATCH_READY", response.status());
        assertEquals("match-ranked-456", response.matchId());
        assertEquals("player1", response.opponentId());

        verify(messaging).convertAndSend(eq("/topic/lobby/player1"), any(LobbyResponseDTO.class));
        assertFalse(lobbyService.isInQueue("player1"));
    }

    @Test
    public void testJoinQueueRankedNoMatchOutsideMmrTolerance() {
        // Enqueue first player with 1000 mmr
        when(penaltyService.isRankedBanned("player1")).thenReturn(false);
        UserEntity user1 = UserEntity.builder().username("player1").mmr(1000).build();
        when(userRepository.findFirstByUsername("player1")).thenReturn(Optional.of(user1));
        lobbyService.joinQueue("player1", 10L, true);

        // Second player joins with 1150 mmr (difference > 100)
        when(penaltyService.isRankedBanned("player2")).thenReturn(false);
        UserEntity user2 = UserEntity.builder().username("player2").mmr(1150).build();
        when(userRepository.findFirstByUsername("player2")).thenReturn(Optional.of(user2));

        LobbyResponseDTO response = lobbyService.joinQueue("player2", 20L, true);

        assertNotNull(response);
        assertEquals("WAITING", response.status());
        assertTrue(lobbyService.isInQueue("player1"));
        assertTrue(lobbyService.isInQueue("player2"));
    }

    @Test
    public void testLeaveQueue() {
        lobbyQueue.enqueue("player1", 10L);
        assertTrue(lobbyService.isInQueue("player1"));

        boolean removed = lobbyService.leaveQueue("player1");
        assertTrue(removed);
        assertFalse(lobbyService.isInQueue("player1"));

        boolean removedAgain = lobbyService.leaveQueue("player1");
        assertFalse(removedAgain);
    }

    @Test
    public void testCreateRoom() {
        LobbyResponseDTO response = lobbyService.createRoom("creator1", 10L);
        assertNotNull(response);
        assertEquals("WAITING", response.status());
        assertNotNull(response.roomCode());
        assertEquals(6, response.roomCode().length());
    }

    @Test
    public void testJoinRoomNotFound() {
        assertThrows(NoSuchElementException.class, () -> {
            lobbyService.joinRoom("INVALID", "player2", 20L);
        });
    }

    @Test
    public void testJoinRoomOwnRoomThrows() {
        LobbyResponseDTO response = lobbyService.createRoom("creator1", 10L);
        String code = response.roomCode();

        assertThrows(IllegalArgumentException.class, () -> {
            lobbyService.joinRoom(code, "creator1", 20L);
        });
    }

    @Test
    public void testJoinRoomSuccess() {
        LobbyResponseDTO createResp = lobbyService.createRoom("creator1", 10L);
        String code = createResp.roomCode();

        List<Card> mockCards1 = List.of(mock(Card.class));
        List<Card> mockCards2 = List.of(mock(Card.class));
        when(cardResolutionService.resolveCards(10L)).thenReturn(mockCards1);
        when(cardResolutionService.resolveCards(20L)).thenReturn(mockCards2);
        when(matchCreationService.createMatch(eq("creator1"), eq("player2"), anyList(), anyList(), eq(false)))
                .thenReturn("match-room-789");

        LobbyResponseDTO joinResp = lobbyService.joinRoom(code, "player2", 20L);

        assertNotNull(joinResp);
        assertEquals("MATCH_READY", joinResp.status());
        assertEquals("match-room-789", joinResp.matchId());
        assertEquals("creator1", joinResp.opponentId());

        verify(messaging).convertAndSend(eq("/topic/lobby/creator1"), any(LobbyResponseDTO.class));
    }
}
