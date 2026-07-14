package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.ActionType;
import ar.edu.utn.frc.tup.piii.dtos.GameStateResponseDTO;
import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.manager.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.manager.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.model.Action;
import ar.edu.utn.frc.tup.piii.engine.model.EndTurnAction;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerState;
import ar.edu.utn.frc.tup.piii.services.persistence.GameStatePersistence;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

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
    private PenaltyService penaltyService;
    private ProfileService profileService;
    private UserRepository userRepository;
    private BotDecisionService botDecisionService;
    private MmrCalculationService mmrCalculationService;
    private CampaignService campaignService;
    private ChatService chatService;

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
        penaltyService = mock(PenaltyService.class);
        profileService = mock(ProfileService.class);
        userRepository = mock(UserRepository.class);
        botDecisionService = mock(BotDecisionService.class);
        mmrCalculationService = mock(MmrCalculationService.class);
        campaignService = mock(CampaignService.class);
        chatService = mock(ChatService.class);

        final FakeBattlePokemonState active = new FakeBattlePokemonState(
                100, PokemonType.FIRE, null, null, false);
        final PlayerState player0 = new PlayerState(active, List.of(), 45, 6, Map.of());
        final PlayerState player1 = new PlayerState(active, List.of(), 45, 6, Map.of());
        final MatchBoard board = new MatchBoard(List.of(player0, player1));
        
        ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime0 = mock(ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime.class);
        ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime1 = mock(ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime.class);
        when(runtime0.getActivePokemon()).thenReturn(active);
        when(runtime1.getActivePokemon()).thenReturn(active);
        when(runtime0.getBench()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.Bench());
        when(runtime1.getBench()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.Bench());
        
        ar.edu.utn.frc.tup.piii.engine.session.MatchStatisticsTracker tracker0 = new ar.edu.utn.frc.tup.piii.engine.session.MatchStatisticsTracker();
        ar.edu.utn.frc.tup.piii.engine.session.MatchStatisticsTracker tracker1 = new ar.edu.utn.frc.tup.piii.engine.session.MatchStatisticsTracker();
        when(runtime0.getStatisticsTracker()).thenReturn(tracker0);
        when(runtime1.getStatisticsTracker()).thenReturn(tracker1);
        
        ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager sem0 = mock(ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager.class);
        ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager sem1 = mock(ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager.class);
        when(runtime0.getStatusEffectManager()).thenReturn(sem0);
        when(runtime1.getStatusEffectManager()).thenReturn(sem1);

        session = new MatchSession(MATCH_ID, List.of(PLAYER_A_ID, PLAYER_B_ID), board, List.of(runtime0, runtime1), false);
        session.setup();
        session.start();

        TurnManager turnManager = new TurnManager();
        turnManager.setStartingPlayer(0);
        turnManager.startTurn(0);
        turnManager.endDraw(); // Transition to MainPhase
        session.setTurnManager(turnManager);

        session.setRuleValidator(ruleValidator);
        when(registry.find(MATCH_ID)).thenReturn(Optional.of(session));
        when(userRepository.findFirstByUsername(anyString())).thenReturn(Optional.of(UserEntity.builder().id(1L).username("test").build()));

        final GameStateResponseDTO fakeView = new GameStateResponseDTO(
                MATCH_ID, 1L, 1, 0, "ACTIVE", null,
                new GameStateResponseDTO.PlayerView(PLAYER_A_ID, null, List.of(), List.of(), 45, 6),
                new GameStateResponseDTO.OpponentView(PLAYER_B_ID, null, List.of(), 0, 45, 6),
                null);
        when(mapper.toResponse(any(), any(Integer.class))).thenReturn(fakeView);

        final MatchRewardService matchRewardService =
                new MatchRewardService(userRepository, mmrCalculationService, campaignService);
        matchService = new MatchService(
                registry, facade, persistence, mapper, messaging,
                scheduler, penaltyService, profileService, userRepository, botDecisionService, matchRewardService,
                chatService, TIMEOUT_SECONDS);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void shouldScheduleTurnTimerForActivePlayerOnStart() {
        final ScheduledFuture mockFuture = mock(ScheduledFuture.class);
        when(scheduler.schedule(any(Runnable.class), eq(TIMEOUT_SECONDS), eq(TimeUnit.SECONDS)))
                .thenReturn(mockFuture);

        matchService.startTurnTimers(MATCH_ID);

        verify(scheduler).schedule(any(Runnable.class), eq(TIMEOUT_SECONDS), eq(TimeUnit.SECONDS));
        Object storedFuture = session.getTimeoutFuture(PLAYER_A_ID);
        assertThat(storedFuture).isNotNull().isEqualTo(mockFuture);
        assertNull(session.getTimeoutFuture(PLAYER_B_ID));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void shouldSkipTurnOnFirstTimeout() {
        final ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        final ScheduledFuture mockFuture = mock(ScheduledFuture.class);
        when(scheduler.schedule(taskCaptor.capture(), eq(TIMEOUT_SECONDS), eq(TimeUnit.SECONDS)))
                .thenReturn(mockFuture);
        
        when(ruleValidator.validate(any(), eq(0))).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.ValidationResult.Valid());
        when(facade.toEngineAction(any(), eq(0), any())).thenReturn(new EndTurnAction());

        matchService.startTurnTimers(MATCH_ID);

        // Simulate turn timer expiry
        taskCaptor.getValue().run();

        // Verify no exception caused an abandon
        verify(persistence, never()).declareWinner(any(), any());
        
        assertEquals(1, session.getMissedTurns(PLAYER_A_ID));
        verify(chatService).addMessage(eq(MATCH_ID), any()); // System message sent
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void shouldAbandonMatchOnSecondTimeout() {
        final ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        final ScheduledFuture mockFuture = mock(ScheduledFuture.class);
        when(scheduler.schedule(taskCaptor.capture(), eq(TIMEOUT_SECONDS), eq(TimeUnit.SECONDS)))
                .thenReturn(mockFuture);
        
        session.incrementMissedTurns(PLAYER_A_ID); // Assume they already missed 1 turn

        matchService.startTurnTimers(MATCH_ID);

        // Simulate turn timer expiry again
        taskCaptor.getValue().run();

        assertEquals(2, session.getMissedTurns(PLAYER_A_ID));
        
        // Verifies match abandonment
        verify(persistence).declareWinner(MATCH_ID, PLAYER_B_ID);
        verify(registry).remove(MATCH_ID);
    }

    @Test
    void shouldAwardBattlePointsToWinnerWhenRankedMatchIsSurrendered() {
        final String rankedMatchId = "match-ranked-surrender";

        final FakeBattlePokemonState active = new FakeBattlePokemonState(
                100, PokemonType.FIRE, null, null, false);
        final PlayerState player0 = new PlayerState(active, List.of(), 45, 6, Map.of());
        final PlayerState player1 = new PlayerState(active, List.of(), 45, 6, Map.of());
        final MatchBoard board = new MatchBoard(List.of(player0, player1));

        final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime rankedRuntime0 =
                mock(ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime.class);
        final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime rankedRuntime1 =
                mock(ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime.class);
        when(rankedRuntime0.getActivePokemon()).thenReturn(active);
        when(rankedRuntime1.getActivePokemon()).thenReturn(active);
        when(rankedRuntime0.getBench()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.Bench());
        when(rankedRuntime1.getBench()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.Bench());
        when(rankedRuntime0.getStatisticsTracker())
                .thenReturn(new ar.edu.utn.frc.tup.piii.engine.session.MatchStatisticsTracker());
        when(rankedRuntime1.getStatisticsTracker())
                .thenReturn(new ar.edu.utn.frc.tup.piii.engine.session.MatchStatisticsTracker());

        final MatchSession rankedSession = new MatchSession(
                rankedMatchId, List.of(PLAYER_A_ID, PLAYER_B_ID), board,
                List.of(rankedRuntime0, rankedRuntime1), true);
        rankedSession.setup();
        rankedSession.start();

        when(registry.find(rankedMatchId)).thenReturn(Optional.of(rankedSession));

        final UserEntity winner = UserEntity.builder().id(2L).username(PLAYER_B_ID).mmr(1000).battlePoints(5).build();
        final UserEntity forfeiter = UserEntity.builder().id(1L).username(PLAYER_A_ID).mmr(1000).battlePoints(0).build();
        when(userRepository.findFirstByUsername(PLAYER_A_ID)).thenReturn(Optional.of(forfeiter));
        when(userRepository.findFirstByUsername(PLAYER_B_ID)).thenReturn(Optional.of(winner));
        when(mmrCalculationService.calculateNewMmr(1000, 1000, true, 0)).thenReturn(1025);
        when(mmrCalculationService.calculateNewMmr(1000, 1000, false, 0)).thenReturn(975);

        matchService.surrenderMatch(rankedMatchId, PLAYER_A_ID);

        assertEquals(15, winner.getBattlePoints()); // 5 already earned + 10 for this ranked win
        verify(userRepository).save(winner);
        verify(penaltyService).applyRankedBan(PLAYER_A_ID, 15);
    }

    @Test
    void shouldHoldSessionLockForEntireAbandonMatchCriticalSection() throws InterruptedException {
        // Same gap as processAction: nothing exercised abandonMatch's lock under real
        // concurrency before this test. persistence.saveMatch() is called unconditionally,
        // early, inside the lock - a convenient hook to block a worker thread mid-critical-
        // section and prove a second lock acquisition attempt fails while it's held.
        final CountDownLatch insideCriticalSection = new CountDownLatch(1);
        final CountDownLatch releaseWorker = new CountDownLatch(1);
        doAnswer(invocation -> {
            insideCriticalSection.countDown();
            releaseWorker.await(2, TimeUnit.SECONDS);
            return null;
        }).when(persistence).saveMatch(any());

        final Thread worker = new Thread(() -> matchService.surrenderMatch(MATCH_ID, PLAYER_A_ID));
        worker.start();
        assertTrue(insideCriticalSection.await(2, TimeUnit.SECONDS),
                "worker thread never reached the critical section");

        final boolean acquiredWhileWorkerHoldsIt = session.getLock().tryLock();
        if (acquiredWhileWorkerHoldsIt) {
            session.getLock().unlock();
        }
        assertFalse(acquiredWhileWorkerHoldsIt,
                "session lock was available while abandonMatch should still be holding it");

        releaseWorker.countDown();
        worker.join(2000);

        assertTrue(session.getLock().tryLock(500, TimeUnit.MILLISECONDS),
                "session lock was not released after abandonMatch returned");
        session.getLock().unlock();
    }

    @Test
    void shouldNotReprocessAnAlreadyFinishedMatchOnAbandon() {
        // abandonMatch's FINISHED-state guard has never been exercised. Without it (or if a
        // split accidentally dropped it), calling surrenderMatch twice - or a timeout racing
        // with a manual surrender - would re-run reward/penalty accounting and re-broadcast
        // a second "match finished" event for a match that's already over.
        session.finish();
        session.setWinnerId(PLAYER_B_ID);

        matchService.surrenderMatch(MATCH_ID, PLAYER_A_ID);

        verify(persistence, never()).saveMatch(any());
        verify(penaltyService, never()).registerMatchFinished(anyString(), org.mockito.ArgumentMatchers.anyBoolean());
        verify(registry, never()).remove(MATCH_ID);
    }
}
