package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.GameStateResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.PlayerPerspectiveMapper;
import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.manager.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerState;
import ar.edu.utn.frc.tup.piii.services.persistence.GameStatePersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for MatchService abandonment lifecycle (disconnect/reconnect/timeout).
 */
class MatchServiceAbandonTest {

    private static final String MATCH_ID = "match-abandon";
    private static final String PLAYER_A_ID = "playerA";
    private static final String PLAYER_B_ID = "playerB";
    private static final long TIMEOUT_SECONDS = 1L;

    private MatchSessionRegistry registry;
    private GameFacade facade;
    private RuleValidator ruleValidator;
    private GameStatePersistence persistence;
    private SimpMessagingTemplate messaging;
    private PlayerPerspectiveMapper mapper;
    private ScheduledExecutorService scheduler;

    private MatchService matchService;
    private MatchSession session;

    @BeforeEach
    void setUp() {
        registry = mock(MatchSessionRegistry.class);
        facade = mock(GameFacade.class);
        ruleValidator = mock(RuleValidator.class);
        persistence = mock(GameStatePersistence.class);
        messaging = mock(SimpMessagingTemplate.class);
        mapper = mock(PlayerPerspectiveMapper.class);
        scheduler = mock(ScheduledExecutorService.class);

        final FakeBattlePokemonState active = new FakeBattlePokemonState(
                100, PokemonType.FIRE, null, null, false);
        final PlayerState player0 = new PlayerState(active, List.of(), 45, 6, Map.of());
        final PlayerState player1 = new PlayerState(active, List.of(), 45, 6, Map.of());
        final MatchBoard board = new MatchBoard(List.of(player0, player1));
        session = new MatchSession(MATCH_ID, List.of(PLAYER_A_ID, PLAYER_B_ID), board);
        session.setup();
        session.start();

        when(registry.find(MATCH_ID)).thenReturn(Optional.of(session));

        final GameStateResponseDTO fakeView = new GameStateResponseDTO(
                MATCH_ID, 1L, 0, "ACTIVE",
                new GameStateResponseDTO.PlayerView(PLAYER_A_ID, null, List.of(), List.of(), 45, 6),
                new GameStateResponseDTO.OpponentView(PLAYER_B_ID, null, List.of(), 0, 45, 6));
        when(mapper.toResponse(any(), any(Integer.class))).thenReturn(fakeView);

        matchService = new MatchService(
                registry, facade, ruleValidator, persistence, mapper, messaging,
                scheduler, TIMEOUT_SECONDS);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void shouldScheduleAbandonmentFutureOnDisconnect() {
        final ScheduledFuture mockFuture = mock(ScheduledFuture.class);
        when(scheduler.schedule(any(Runnable.class), eq(TIMEOUT_SECONDS), eq(TimeUnit.SECONDS)))
                .thenReturn(mockFuture);

        matchService.onPlayerDisconnect(MATCH_ID, PLAYER_A_ID);

        // verify a future was scheduled
        verify(scheduler).schedule(any(Runnable.class), eq(TIMEOUT_SECONDS), eq(TimeUnit.SECONDS));
        // verify the future is stored on the session
        final Object storedFuture = session.getTimeoutFuture(PLAYER_A_ID);
        assertThat(storedFuture).isNotNull().isEqualTo(mockFuture);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void shouldCancelAbandonmentFutureAtomicallyInsideLockOnReconnect() {
        final ScheduledFuture mockFuture = mock(ScheduledFuture.class);
        when(scheduler.schedule(any(Runnable.class), eq(TIMEOUT_SECONDS), eq(TimeUnit.SECONDS)))
                .thenReturn(mockFuture);

        matchService.onPlayerDisconnect(MATCH_ID, PLAYER_A_ID);
        matchService.onPlayerReconnect(MATCH_ID, PLAYER_A_ID);

        // the future must have been cancelled
        verify(mockFuture).cancel(false);
        // the session must have cleared the stored future reference
        final Object clearedFuture = session.getTimeoutFuture(PLAYER_A_ID);
        assertThat(clearedFuture).isNull();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void shouldHoldLockWhenSettingDisconnectTimeout() {
        // Verify the timeout is set while the lock is held by checking that no
        // concurrent reconnect can cancel a future that hasn't been stored yet.
        // We do this by running disconnect and then immediately reconnect — without
        // the lock, the cancel in reconnect would race with the store in disconnect.
        // With the lock, disconnect stores first, reconnect cancels after.
        final ScheduledFuture mockFuture = mock(ScheduledFuture.class);
        when(scheduler.schedule(any(Runnable.class), eq(TIMEOUT_SECONDS), eq(TimeUnit.SECONDS)))
                .thenReturn(mockFuture);

        // Acquire the lock ourselves before calling disconnect — if disconnect also
        // tries to acquire the lock, it will block until we release it.
        session.getLock().lock();
        try {
            // Spawn disconnect in a separate thread that must wait for our lock
            final Thread disconnectThread = new Thread(
                    () -> matchService.onPlayerDisconnect(MATCH_ID, PLAYER_A_ID));
            disconnectThread.start();

            // Sleep briefly to let the thread reach the lock acquisition point
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // While we hold the lock, the timeout future must NOT be stored yet
            assertNull(session.getTimeoutFuture(PLAYER_A_ID),
                    "timeout must not be stored until the lock is released");
        } finally {
            session.getLock().unlock();
        }

        // After releasing, the disconnect thread completes — future is now stored
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertNotNull(session.getTimeoutFuture(PLAYER_A_ID),
                "timeout future must be stored after disconnect completes");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void shouldBroadcastAndRemoveMatchWhenAbandonmentTimerFires() {
        final ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        final ScheduledFuture mockFuture = mock(ScheduledFuture.class);
        when(scheduler.schedule(taskCaptor.capture(), eq(TIMEOUT_SECONDS), eq(TimeUnit.SECONDS)))
                .thenReturn(mockFuture);

        matchService.onPlayerDisconnect(MATCH_ID, PLAYER_A_ID);

        // fire the scheduled task directly (simulating timer expiry)
        taskCaptor.getValue().run();

        // broadcast must have been called (for both players)
        verify(messaging, org.mockito.Mockito.atLeastOnce())
                .convertAndSend(any(String.class), any(Object.class));
        // registry must have removed the match
        verify(registry).remove(MATCH_ID);
    }
}
