package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.ActionType;
import ar.edu.utn.frc.tup.piii.dtos.GameStateResponseDTO;
import ar.edu.utn.frc.tup.piii.services.PlayerPerspectiveMapper;
import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.exception.InvalidActionException;
import ar.edu.utn.frc.tup.piii.engine.manager.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.manager.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.ValidationResult;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerState;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.services.CampaignService;
import ar.edu.utn.frc.tup.piii.services.MmrCalculationService;
import ar.edu.utn.frc.tup.piii.services.persistence.GameStatePersistence;
import ar.edu.utn.frc.tup.piii.services.persistence.GameStateSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import ar.edu.utn.frc.tup.piii.services.ChatService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentMatchers;

/**
 * Tests for MatchService.processAction() — lock contract and validation.
 */
class MatchServiceTest {

    private static final String MATCH_ID = "match-abc";
    private static final String PLAYER_A_ID = "playerA";
    private static final String PLAYER_B_ID = "playerB";
    private static final long TIMEOUT_SECONDS = 60L;

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
    private MatchBoard board;

    private TurnManager turnManager;

    @BeforeEach
    void setUp() {
        registry = mock(MatchSessionRegistry.class);
        facade = mock(GameFacade.class);
        ruleValidator = mock(RuleValidator.class);
        persistence = mock(GameStatePersistence.class);
        turnManager = mock(TurnManager.class);
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
        board = new MatchBoard(List.of(player0, player1));

        ar.edu.utn.frc.tup.piii.engine.model.Card dummyCard = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder("dummy", "Dummy", 10, PokemonType.FIRE).build();
        ar.edu.utn.frc.tup.piii.engine.model.Deck dummyDeck = new ar.edu.utn.frc.tup.piii.engine.model.Deck(List.of(dummyCard));
        ar.edu.utn.frc.tup.piii.engine.model.Hand dummyHand = new ar.edu.utn.frc.tup.piii.engine.model.Hand();
        ar.edu.utn.frc.tup.piii.engine.model.Bench dummyBench = new ar.edu.utn.frc.tup.piii.engine.model.Bench();
        ar.edu.utn.frc.tup.piii.engine.model.DiscardPile dummyDiscard = new ar.edu.utn.frc.tup.piii.engine.model.DiscardPile();
        ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager sem0 = new ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager(() -> true);
        ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager sem1 = new ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager(() -> true);
        ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime0 = new ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime(dummyDeck, dummyHand, dummyBench, dummyDiscard, sem0, active);
        ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime1 = new ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime(dummyDeck, dummyHand, dummyBench, dummyDiscard, sem1, active);

        session = new MatchSession(MATCH_ID, List.of(PLAYER_A_ID, PLAYER_B_ID), board, List.of(runtime0, runtime1));
        session.setup();
        session.start();

        // Wire the per-session dependencies
        session.setRuleValidator(ruleValidator);
        session.setTurnManager(turnManager);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        when(turnManager.currentPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.BetweenTurnsPhase());
        when(registry.find(MATCH_ID)).thenReturn(Optional.of(session));
        when(userRepository.findFirstByUsername(anyString())).thenReturn(Optional.of(UserEntity.builder().id(1L).username("test").build()));

        final GameStateResponseDTO fakeView = new GameStateResponseDTO(
                MATCH_ID, 1L, 1, 0, "ACTIVE", null,
                new GameStateResponseDTO.PlayerView(PLAYER_A_ID, null, List.of(), List.of(), 45, 6),
                new GameStateResponseDTO.OpponentView(PLAYER_B_ID, null, List.of(), 0, 45, 6),
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null);
        when(mapper.toResponse(any(), any(Integer.class))).thenReturn(fakeView);

        matchService = new MatchService(
                registry, facade, persistence, mapper, messaging,
                scheduler, penaltyService, profileService, userRepository, botDecisionService, mmrCalculationService,
                campaignService, chatService, TIMEOUT_SECONDS);

        // The turn timer system calls scheduler.schedule() at the end of processAction.
        // Return a mock ScheduledFuture so setTurnTimeout's requireNonNull doesn't fail.
        java.util.concurrent.ScheduledFuture<?> mockFuture = mock(java.util.concurrent.ScheduledFuture.class);
        org.mockito.Mockito.doReturn(mockFuture)
                .when(scheduler).schedule(any(Runnable.class), any(Long.class), any(java.util.concurrent.TimeUnit.class));
    }

    @Test
    void shouldCallPersistInsideLockAndBroadcastAfterUnlock() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.RETREAT, null, null, null, null, null);
        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(
                new ar.edu.utn.frc.tup.piii.engine.model.RetreatAction(
                        board.getActivePokemon(0)));
        when(ruleValidator.validate(any(), any(Integer.class))).thenReturn(new ValidationResult.Valid());

        matchService.processAction(MATCH_ID, PLAYER_A_ID, dto);

        // verify persist happened before either broadcast call
        final InOrder order = inOrder(persistence, messaging);
        order.verify(persistence).save(any(GameStateSnapshot.class));
        // broadcast sends to both players — verify at least the first send comes after persist
        order.verify(messaging, org.mockito.Mockito.atLeastOnce())
                .convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void shouldApplyActionToBoardBeforePersisting() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.RETREAT, null, null, null, null, null);
        final ar.edu.utn.frc.tup.piii.engine.model.Action action =
                new ar.edu.utn.frc.tup.piii.engine.model.RetreatAction(board.getActivePokemon(0));
        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(action);
        when(ruleValidator.validate(any(), any(Integer.class))).thenReturn(new ValidationResult.Valid());

        matchService.processAction(MATCH_ID, PLAYER_A_ID, dto);

        final InOrder order = inOrder(facade, persistence);
        order.verify(facade).apply(any(ar.edu.utn.frc.tup.piii.engine.session.MatchSession.class),
                any(ar.edu.utn.frc.tup.piii.engine.model.Action.class),
                any(ar.edu.utn.frc.tup.piii.engine.manager.TurnManager.class));
        order.verify(persistence).save(any(GameStateSnapshot.class));
    }

    @Test
    void shouldThrowAndNotPersistWhenActionIsInvalid() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.RETREAT, null, null, null, null, null);
        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(
                new ar.edu.utn.frc.tup.piii.engine.model.RetreatAction(
                        board.getActivePokemon(0)));
        when(ruleValidator.validate(any(), any(Integer.class))).thenReturn(
                new ValidationResult.Invalid("retreat_blocked_by_status"));

        assertThatThrownBy(() -> matchService.processAction(MATCH_ID, PLAYER_A_ID, dto))
                .isInstanceOf(InvalidActionException.class);

        verify(persistence, never()).save(any());
        verify(messaging, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void shouldRegisterMatchFinishedLegitimatelyWhenSessionFinishes() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.RETREAT, null, null, null, null, null);
        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(
                new ar.edu.utn.frc.tup.piii.engine.model.RetreatAction(board.getActivePokemon(0)));
        when(ruleValidator.validate(any(), any(Integer.class))).thenReturn(new ValidationResult.Valid());

        // Force session state to FINISHED after apply
        org.mockito.Mockito.doAnswer(invocation -> {
            session.finish();
            return null;
        }).when(facade).apply(any(), any(), any());

        matchService.processAction(MATCH_ID, PLAYER_A_ID, dto);

        verify(penaltyService).registerMatchFinished(PLAYER_A_ID, true);
        verify(penaltyService).registerMatchFinished(PLAYER_B_ID, true);
    }

    @Test
    void shouldClearDamagePreventedFlagOnOpponentTurnEnd() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.END_TURN, null, null, null, null, null);
        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(
                new ar.edu.utn.frc.tup.piii.engine.model.EndTurnAction());
        when(ruleValidator.validate(any(), any(Integer.class))).thenReturn(new ValidationResult.Valid());

        session.getPlayerRuntime(0).getStatusEffectManager().setDamagePreventedNextTurn(true);
        session.getPlayerRuntime(1).getStatusEffectManager().setDamagePreventedNextTurn(true);

        when(turnManager.activePlayerIndex()).thenReturn(0);

        matchService.processAction(MATCH_ID, PLAYER_A_ID, dto);

        org.junit.jupiter.api.Assertions.assertTrue(
                session.getPlayerRuntime(0).getStatusEffectManager().isDamagePreventedNextTurn());
        org.junit.jupiter.api.Assertions.assertFalse(
                session.getPlayerRuntime(1).getStatusEffectManager().isDamagePreventedNextTurn());
    }

    @Test
    void shouldPauseTurnTransitionWhenActiveDiesBetweenTurns() {
        // 1. Setup real KnockoutResolutionHandler
        final ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler downstream = mock(ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler.class);
        final ar.edu.utn.frc.tup.piii.engine.manager.KnockoutResolutionHandler koResolution =
                new ar.edu.utn.frc.tup.piii.engine.manager.KnockoutResolutionHandler(
                        List.of(session.getPlayerRuntime(0), session.getPlayerRuntime(1)), turnManager, downstream);
        session.setKnockoutHandler(koResolution);

        // 2. Set active Pokémon of Player 0 at 90/100 HP, and poison it
        final ar.edu.utn.frc.tup.piii.engine.model.PokemonCard dummyCard =
                new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder("dummy", "Dummy", 100, PokemonType.FIRE).build();
        final FakeBattlePokemonState active = new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false) {
            @Override
            public ar.edu.utn.frc.tup.piii.engine.model.Card getBaseCard() {
                return dummyCard;
            }
        };
        active.addDamageCounters(9); // 90 damage
        session.getPlayerRuntime(0).setActivePokemon(active);
        session.getPlayerRuntime(0).getStatusEffectManager().apply(
                ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType.ENVENENADO);

        // 3. Add a benched Pokémon so promotion is possible
        final var benchedPokemon = new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false);
        session.getPlayerRuntime(0).getBench().place(benchedPokemon);

        // 4. Process EndTurnAction
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.END_TURN, null, null, null, null, null);
        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(
                new ar.edu.utn.frc.tup.piii.engine.model.EndTurnAction());
        when(ruleValidator.validate(any(), any(Integer.class))).thenReturn(new ValidationResult.Valid());

        matchService.processAction(MATCH_ID, PLAYER_A_ID, dto);

        // 5. Assertions
        // Active Pokémon should be knocked out (null active slot)
        org.junit.jupiter.api.Assertions.assertNull(session.getPlayerRuntime(0).getActivePokemon());
        // Session should be awaiting promotion for player 0
        org.junit.jupiter.api.Assertions.assertTrue(session.isAwaitingPromotion());
        org.junit.jupiter.api.Assertions.assertEquals(0, session.getPromotingPlayerIndex());
        org.junit.jupiter.api.Assertions.assertTrue(session.isBetweenTurnsProcessed());

        // turnManager.endBetweenTurns() should NOT have been called yet
        verify(turnManager, never()).endBetweenTurns();
    }

    @Test
    void shouldResumeAndEndBetweenTurnsOnPromotion() {
        // 1. Setup real KnockoutResolutionHandler
        final ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler downstream = mock(ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler.class);
        final ar.edu.utn.frc.tup.piii.engine.manager.KnockoutResolutionHandler koResolution =
                new ar.edu.utn.frc.tup.piii.engine.manager.KnockoutResolutionHandler(
                        List.of(session.getPlayerRuntime(0), session.getPlayerRuntime(1)), turnManager, downstream);
        session.setKnockoutHandler(koResolution);

        // 2. Set active Pokémon of Player 0 at 90/100 HP, and poison it
        final ar.edu.utn.frc.tup.piii.engine.model.PokemonCard dummyCard =
                new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder("dummy", "Dummy", 100, PokemonType.FIRE).build();
        final FakeBattlePokemonState active = new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false) {
            @Override
            public ar.edu.utn.frc.tup.piii.engine.model.Card getBaseCard() {
                return dummyCard;
            }
        };
        active.addDamageCounters(9); // 90 damage
        session.getPlayerRuntime(0).setActivePokemon(active);
        session.getPlayerRuntime(0).getStatusEffectManager().apply(
                ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType.ENVENENADO);

        // 3. Add a benched Pokémon and set session as awaiting promotion (simulating the paused state)
        final var benchedPokemon = new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false);
        session.getPlayerRuntime(0).getBench().place(benchedPokemon);

        // Manually trigger the between-turns KO and pause
        session.getPlayerRuntime(0).getStatusEffectManager().processBetweenTurns(active);
        koResolution.onKnockout(active, 1);
        session.setAwaitingPromotion(0);
        session.setBetweenTurnsProcessed(true);

        // 4. Process PromoteActiveAction
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.PROMOTE_ACTIVE, null, null, 0, null, null);
        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(
                new ar.edu.utn.frc.tup.piii.engine.model.PromoteActiveAction(0));
        when(ruleValidator.validate(any(), any(Integer.class))).thenReturn(new ValidationResult.Valid());

        // Stub facade.apply to perform the actual promotion on the session
        org.mockito.Mockito.doAnswer(invocation -> {
            session.getPlayerRuntime(0).setActivePokemon(benchedPokemon);
            session.getPlayerRuntime(0).getBench().remove(0);
            return null;
        }).when(facade).apply(any(), any(), any());

        matchService.processAction(MATCH_ID, PLAYER_A_ID, dto);

        // 5. Assertions
        // Active Pokémon should be promoted
        org.junit.jupiter.api.Assertions.assertEquals(benchedPokemon, session.getPlayerRuntime(0).getActivePokemon());
        // Session should not be awaiting promotion anymore
        org.junit.jupiter.api.Assertions.assertFalse(session.isAwaitingPromotion());
        // betweenTurnsProcessed should be reset to false
        org.junit.jupiter.api.Assertions.assertFalse(session.isBetweenTurnsProcessed());
        // turnManager.endBetweenTurns() should have been called
        verify(turnManager).endBetweenTurns();
    }

    @Test
    void shouldHandleDoubleKnockoutBetweenTurnsAndForceBothPlayersToPromote() {
        // 1. Setup real KnockoutResolutionHandler
        final ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler downstream = mock(ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler.class);
        final ar.edu.utn.frc.tup.piii.engine.manager.KnockoutResolutionHandler koResolution =
                new ar.edu.utn.frc.tup.piii.engine.manager.KnockoutResolutionHandler(
                        List.of(session.getPlayerRuntime(0), session.getPlayerRuntime(1)), turnManager, downstream);
        session.setKnockoutHandler(koResolution);

        // 2. Set active Pokémon of Player 0 and Player 1 to 90/100 HP, and poison them
        final ar.edu.utn.frc.tup.piii.engine.model.PokemonCard dummyCard =
                new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder("dummy", "Dummy", 100, PokemonType.FIRE).build();
        final FakeBattlePokemonState active0 = new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false) {
            @Override
            public ar.edu.utn.frc.tup.piii.engine.model.Card getBaseCard() {
                return dummyCard;
            }
        };
        active0.addDamageCounters(9); // 90 damage
        session.getPlayerRuntime(0).setActivePokemon(active0);
        session.getPlayerRuntime(0).getStatusEffectManager().apply(
                ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType.ENVENENADO);

        final FakeBattlePokemonState active1 = new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false) {
            @Override
            public ar.edu.utn.frc.tup.piii.engine.model.Card getBaseCard() {
                return dummyCard;
            }
        };
        active1.addDamageCounters(9); // 90 damage
        session.getPlayerRuntime(1).setActivePokemon(active1);
        session.getPlayerRuntime(1).getStatusEffectManager().apply(
                ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType.ENVENENADO);

        // 3. Add benched Pokémon to both players so promotion is possible
        final var benched0 = new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false);
        session.getPlayerRuntime(0).getBench().place(benched0);

        final var benched1 = new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false);
        session.getPlayerRuntime(1).getBench().place(benched1);

        // 4. Process EndTurnAction to trigger faints
        final ActionRequestDTO dtoEnd = new ActionRequestDTO(
                ActionType.END_TURN, null, null, null, null, null);
        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(
                new ar.edu.utn.frc.tup.piii.engine.model.EndTurnAction());
        when(ruleValidator.validate(any(), any(Integer.class))).thenReturn(new ValidationResult.Valid());

        matchService.processAction(MATCH_ID, PLAYER_A_ID, dtoEnd);

        // Assertions after EndTurnAction:
        // Player 0 should be awaiting promotion (since Player 0 is checked first)
        org.junit.jupiter.api.Assertions.assertTrue(session.isAwaitingPromotion());
        org.junit.jupiter.api.Assertions.assertEquals(0, session.getPromotingPlayerIndex());
        verify(turnManager, never()).endBetweenTurns();

        // 5. Player 0 promotes
        final ActionRequestDTO dtoPromote0 = new ActionRequestDTO(
                ActionType.PROMOTE_ACTIVE, null, null, 0, null, null);
        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(
                new ar.edu.utn.frc.tup.piii.engine.model.PromoteActiveAction(0));

        // Stub facade.apply to perform Player 0 promotion
        org.mockito.Mockito.doAnswer(invocation -> {
            session.getPlayerRuntime(0).setActivePokemon(benched0);
            session.getPlayerRuntime(0).getBench().remove(0);
            return null;
        }).when(facade).apply(any(), any(), any());

        matchService.processAction(MATCH_ID, PLAYER_A_ID, dtoPromote0);

        // Assertions after Player 0 promotion:
        // Now Player 1 should be awaiting promotion
        org.junit.jupiter.api.Assertions.assertTrue(session.isAwaitingPromotion());
        org.junit.jupiter.api.Assertions.assertEquals(1, session.getPromotingPlayerIndex());
        verify(turnManager, never()).endBetweenTurns();

        // 6. Player 1 promotes
        final ActionRequestDTO dtoPromote1 = new ActionRequestDTO(
                ActionType.PROMOTE_ACTIVE, null, null, 0, null, null);
        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(
                new ar.edu.utn.frc.tup.piii.engine.model.PromoteActiveAction(0));

        // Stub facade.apply to perform Player 1 promotion
        org.mockito.Mockito.doAnswer(invocation -> {
            session.getPlayerRuntime(1).setActivePokemon(benched1);
            session.getPlayerRuntime(1).getBench().remove(0);
            return null;
        }).when(facade).apply(any(), any(), any());

        matchService.processAction(MATCH_ID, PLAYER_B_ID, dtoPromote1);

        // Assertions after both promotions:
        org.junit.jupiter.api.Assertions.assertFalse(session.isAwaitingPromotion());
        org.junit.jupiter.api.Assertions.assertFalse(session.isBetweenTurnsProcessed());
        verify(turnManager).endBetweenTurns();
    }

    @Test
    void shouldResumeMainPhaseWhenPromotionOccursDuringActionResolutionPhase() {
        session.setAwaitingPromotion(0);
        
        final var benched = new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false);
        session.getPlayerRuntime(0).getBench().place(benched);
        
        when(turnManager.currentPhase()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase());
        
        final ActionRequestDTO dtoPromote = new ActionRequestDTO(
                ActionType.PROMOTE_ACTIVE, null, null, 0, null, null);
        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(
                new ar.edu.utn.frc.tup.piii.engine.model.PromoteActiveAction(0));
        when(ruleValidator.validate(any(), any(Integer.class))).thenReturn(new ValidationResult.Valid());

        matchService.processAction(MATCH_ID, PLAYER_A_ID, dtoPromote);

        org.junit.jupiter.api.Assertions.assertFalse(session.isAwaitingPromotion());
        verify(turnManager).resumeMainPhase();
        verify(turnManager, never()).endBetweenTurns();
    }

    @Test
    void shouldPassTurnAndEndBetweenTurnsWhenMegaEvolutionOccurs() {
        final var targetPokemon = mock(ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState.class);
        final var megaEvolutionCard = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder("mega-card", "Mega Charizard", 230, PokemonType.FIRE)
                .evolutionStage(ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.MEGA)
                .build();

        final ActionRequestDTO dtoEvolve = new ActionRequestDTO(
                ActionType.EVOLVE, "mega-card", null, 0, null, null);
        final var evolveAction = new ar.edu.utn.frc.tup.piii.engine.model.EvolveAction(targetPokemon, megaEvolutionCard);

        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(evolveAction);
        when(ruleValidator.validate(any(), any(Integer.class))).thenReturn(new ValidationResult.Valid());
        session.setMegaEvolvedThisTurn(true);

        matchService.processAction(MATCH_ID, PLAYER_A_ID, dtoEvolve);

        verify(turnManager).passTurn();
        verify(turnManager).endBetweenTurns();
    }

    @Test
    void shouldResolveBounceInteractiveSelection() {
        final ActionRequestDTO dtoSelect = new ActionRequestDTO(
                ActionType.SELECT_CARDS, null, null, null, null, null, null, java.util.Collections.emptyList(), null, List.of("benched-id"));
        final var request = new ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest(
                ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.BOUNCE,
                null,
                1,
                ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.BENCH
        );
        session.setPendingSelectionRequest(request);

        final var selectAction = new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("benched-id"), request);

        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(selectAction);
        when(ruleValidator.validate(any(), any(Integer.class))).thenReturn(new ValidationResult.Valid());

        matchService.processAction(MATCH_ID, PLAYER_A_ID, dtoSelect);

        verify(facade).apply(session, selectAction, turnManager);
    }

    @Test
    void shouldCompleteCampaignNodeWhenHumanWinsAgainstBot() {
        final FakeBattlePokemonState active = new FakeBattlePokemonState(
                100, PokemonType.FIRE, null, null, false);
        final PlayerState player0 = new PlayerState(active, List.of(), 45, 6, Map.of());
        final PlayerState player1 = new PlayerState(active, List.of(), 45, 6, Map.of());
        final MatchBoard campaignBoard = new MatchBoard(List.of(player0, player1));
        
        ar.edu.utn.frc.tup.piii.engine.model.Card dummyCard = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder("dummy", "Dummy", 10, PokemonType.FIRE).build();
        ar.edu.utn.frc.tup.piii.engine.model.Deck dummyDeck = new ar.edu.utn.frc.tup.piii.engine.model.Deck(List.of(dummyCard));
        ar.edu.utn.frc.tup.piii.engine.model.Hand dummyHand = new ar.edu.utn.frc.tup.piii.engine.model.Hand();
        ar.edu.utn.frc.tup.piii.engine.model.Bench dummyBench = new ar.edu.utn.frc.tup.piii.engine.model.Bench();
        ar.edu.utn.frc.tup.piii.engine.model.DiscardPile dummyDiscard = new ar.edu.utn.frc.tup.piii.engine.model.DiscardPile();
        ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager sem0 = new ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager(() -> true);
        ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager sem1 = new ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager(() -> true);
        ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime0 = new ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime(dummyDeck, dummyHand, dummyBench, dummyDiscard, sem0, active);
        ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime1 = new ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime(dummyDeck, dummyHand, dummyBench, dummyDiscard, sem1, active);

        final MatchSession campaignSession = new MatchSession(MATCH_ID, List.of("test", "Bot-Brock"), campaignBoard, List.of(runtime0, runtime1));
        campaignSession.setup();
        campaignSession.start();
        campaignSession.setRuleValidator(ruleValidator);
        campaignSession.setTurnManager(turnManager);

        when(registry.find(MATCH_ID)).thenReturn(Optional.of(campaignSession));
        when(turnManager.activePlayerIndex()).thenReturn(0);
        when(ruleValidator.validate(any(), any(Integer.class))).thenReturn(new ValidationResult.Valid());

        org.mockito.Mockito.doAnswer(invocation -> {
            campaignSession.finish();
            campaignSession.setWinnerId("test");
            return null;
        }).when(facade).apply(any(), any(), any());

        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.RETREAT, null, null, null, null, null);
        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(
                new ar.edu.utn.frc.tup.piii.engine.model.RetreatAction(campaignBoard.getActivePokemon(0)));

        matchService.processAction(MATCH_ID, "test", dto);

        verify(campaignService).completeNode("test", 1, MATCH_ID);
    }

    @Test
    void shouldUpdateMmrWhenSessionFinishesAndIsRanked() {
        final FakeBattlePokemonState active = new FakeBattlePokemonState(
                100, PokemonType.FIRE, null, null, false);
        final PlayerState player0 = new PlayerState(active, List.of(), 45, 6, Map.of());
        final PlayerState player1 = new PlayerState(active, List.of(), 45, 6, Map.of());
        MatchBoard rankedBoard = new MatchBoard(List.of(player0, player1));
        
        ar.edu.utn.frc.tup.piii.engine.model.Card dummyCard = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder("dummy", "Dummy", 10, PokemonType.FIRE).build();
        ar.edu.utn.frc.tup.piii.engine.model.Deck dummyDeck = new ar.edu.utn.frc.tup.piii.engine.model.Deck(List.of(dummyCard));
        ar.edu.utn.frc.tup.piii.engine.model.Hand dummyHand = new ar.edu.utn.frc.tup.piii.engine.model.Hand();
        ar.edu.utn.frc.tup.piii.engine.model.Bench dummyBench = new ar.edu.utn.frc.tup.piii.engine.model.Bench();
        ar.edu.utn.frc.tup.piii.engine.model.DiscardPile dummyDiscard = new ar.edu.utn.frc.tup.piii.engine.model.DiscardPile();
        ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager sem0 = new ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager(() -> true);
        ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager sem1 = new ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager(() -> true);
        ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime0 = new ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime(dummyDeck, dummyHand, dummyBench, dummyDiscard, sem0, active);
        ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime1 = new ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime(dummyDeck, dummyHand, dummyBench, dummyDiscard, sem1, active);

        MatchSession rankedSession = new MatchSession(MATCH_ID, List.of(PLAYER_A_ID, PLAYER_B_ID), rankedBoard, List.of(runtime0, runtime1), true);
        rankedSession.setup();
        rankedSession.start();
        rankedSession.setRuleValidator(ruleValidator);
        rankedSession.setTurnManager(turnManager);

        when(registry.find(MATCH_ID)).thenReturn(Optional.of(rankedSession));
        when(turnManager.activePlayerIndex()).thenReturn(0);
        when(ruleValidator.validate(any(), any(Integer.class))).thenReturn(new ValidationResult.Valid());

        // Mock users in database
        UserEntity alice = UserEntity.builder().id(1L).username(PLAYER_A_ID).mmr(1000).rankedMatchesPlayed(10).build();
        UserEntity bob = UserEntity.builder().id(2L).username(PLAYER_B_ID).mmr(1000).rankedMatchesPlayed(10).build();
        when(userRepository.findFirstByUsername(PLAYER_A_ID)).thenReturn(Optional.of(alice));
        when(userRepository.findFirstByUsername(PLAYER_B_ID)).thenReturn(Optional.of(bob));

        // Mock MMR calculations
        when(mmrCalculationService.calculateNewMmr(1000, 1000, true, 10)).thenReturn(1032);
        when(mmrCalculationService.calculateNewMmr(1000, 1000, false, 10)).thenReturn(968);

        // Force session state to FINISHED after apply and set winner
        org.mockito.Mockito.doAnswer(invocation -> {
            rankedSession.finish();
            rankedSession.setWinnerId(PLAYER_A_ID);
            return null;
        }).when(facade).apply(any(), any(), any());

        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.RETREAT, null, null, null, null, null);
        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(
                new ar.edu.utn.frc.tup.piii.engine.model.RetreatAction(rankedBoard.getActivePokemon(0)));

        matchService.processAction(MATCH_ID, PLAYER_A_ID, dto);

        // Verify save happened for both players with updated MMRs
        verify(userRepository).save(ArgumentMatchers.<UserEntity>argThat(u -> u.getUsername().equals(PLAYER_A_ID) && u.getMmr() == 1032 && u.getRankedMatchesPlayed() == 11));
        verify(userRepository).save(ArgumentMatchers.<UserEntity>argThat(u -> u.getUsername().equals(PLAYER_B_ID) && u.getMmr() == 968 && u.getRankedMatchesPlayed() == 11));

        // Verify transient fields set on the session
        org.junit.jupiter.api.Assertions.assertEquals(32, rankedSession.getMmrChangeA());
        org.junit.jupiter.api.Assertions.assertEquals(-32, rankedSession.getMmrChangeB());
        org.junit.jupiter.api.Assertions.assertEquals(1032, rankedSession.getCurrentMmrA());
        org.junit.jupiter.api.Assertions.assertEquals(968, rankedSession.getCurrentMmrB());
        org.junit.jupiter.api.Assertions.assertEquals("Iron", rankedSession.getCurrentTierA());
        org.junit.jupiter.api.Assertions.assertEquals("Iron", rankedSession.getCurrentTierB());
    }

    @Test
    void shouldAwardPerfectWinBonusWhenLoserNeverLostAPrize() {
        // Loser still has all 6 prizes remaining at match end -> perfect win for the winner.
        final MatchSession finishSession = buildFinishedMatchSession(6, PLAYER_A_ID);

        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.RETREAT, null, null, null, null, null);
        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(
                new ar.edu.utn.frc.tup.piii.engine.model.RetreatAction(finishSession.getBoard().getActivePokemon(0)));
        when(ruleValidator.validate(any(), any(Integer.class))).thenReturn(new ValidationResult.Valid());
        org.mockito.Mockito.doAnswer(invocation -> {
            finishSession.finish();
            finishSession.setWinnerId(PLAYER_A_ID);
            return null;
        }).when(facade).apply(any(), any(), any());

        matchService.processAction(MATCH_ID, PLAYER_A_ID, dto);

        verify(profileService).awardXpAndCheckAchievements(
                any(Long.class), org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.eq(true), org.mockito.ArgumentMatchers.eq(false), any(Integer.class));
        verify(profileService).awardXpAndCheckAchievements(
                any(Long.class), org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.eq(false), org.mockito.ArgumentMatchers.eq(false), any(Integer.class));
    }

    @Test
    void shouldAwardComebackWinBonusWhenLoserHadOnePrizeRemaining() {
        // Loser was down to their last prize (about to win) when the winner turned it around.
        final MatchSession finishSession = buildFinishedMatchSession(1, PLAYER_A_ID);

        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.RETREAT, null, null, null, null, null);
        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(
                new ar.edu.utn.frc.tup.piii.engine.model.RetreatAction(finishSession.getBoard().getActivePokemon(0)));
        when(ruleValidator.validate(any(), any(Integer.class))).thenReturn(new ValidationResult.Valid());
        org.mockito.Mockito.doAnswer(invocation -> {
            finishSession.finish();
            finishSession.setWinnerId(PLAYER_A_ID);
            return null;
        }).when(facade).apply(any(), any(), any());

        matchService.processAction(MATCH_ID, PLAYER_A_ID, dto);

        verify(profileService).awardXpAndCheckAchievements(
                any(Long.class), org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.eq(false), org.mockito.ArgumentMatchers.eq(true), any(Integer.class));
        verify(profileService).awardXpAndCheckAchievements(
                any(Long.class), org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.eq(false), org.mockito.ArgumentMatchers.eq(false), any(Integer.class));
    }

    /**
     * Builds a fresh, active, non-ranked session where the loser (player index 1) has
     * {@code loserRemainingPrizes} prizes left on the board, wired into the shared mocks
     * and registered with {@link #registry} so {@code processAction} can find it.
     */
    private MatchSession buildFinishedMatchSession(final int loserRemainingPrizes, final String winnerId) {
        final FakeBattlePokemonState active = new FakeBattlePokemonState(
                100, PokemonType.FIRE, null, null, false);
        final PlayerState winnerState = new PlayerState(active, List.of(), 45, 0, Map.of());
        final PlayerState loserState = new PlayerState(active, List.of(), 45, loserRemainingPrizes, Map.of());
        final MatchBoard finishBoard = new MatchBoard(List.of(winnerState, loserState));

        final ar.edu.utn.frc.tup.piii.engine.model.Card dummyCard = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder("dummy", "Dummy", 10, PokemonType.FIRE).build();
        final ar.edu.utn.frc.tup.piii.engine.model.Deck dummyDeck = new ar.edu.utn.frc.tup.piii.engine.model.Deck(List.of(dummyCard));
        final ar.edu.utn.frc.tup.piii.engine.model.Hand dummyHand = new ar.edu.utn.frc.tup.piii.engine.model.Hand();
        final ar.edu.utn.frc.tup.piii.engine.model.Bench dummyBench = new ar.edu.utn.frc.tup.piii.engine.model.Bench();
        final ar.edu.utn.frc.tup.piii.engine.model.DiscardPile dummyDiscard = new ar.edu.utn.frc.tup.piii.engine.model.DiscardPile();
        final ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager sem0 = new ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager(() -> true);
        final ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager sem1 = new ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager(() -> true);
        final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime0 = new ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime(dummyDeck, dummyHand, dummyBench, dummyDiscard, sem0, active);
        final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime runtime1 = new ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime(dummyDeck, dummyHand, dummyBench, dummyDiscard, sem1, active);

        final MatchSession finishSession = new MatchSession(MATCH_ID, List.of(PLAYER_A_ID, PLAYER_B_ID), finishBoard, List.of(runtime0, runtime1));
        finishSession.setup();
        finishSession.start();
        finishSession.setRuleValidator(ruleValidator);
        finishSession.setTurnManager(turnManager);

        when(registry.find(MATCH_ID)).thenReturn(Optional.of(finishSession));
        when(turnManager.activePlayerIndex()).thenReturn(0);

        return finishSession;
    }

    @Test
    void shouldLogTrainerCardPlayToChat() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.PLAY_TRAINER, "xy1-118", null, null, null, null);
        
        final ar.edu.utn.frc.tup.piii.engine.model.Card trainerCard = 
                new ar.edu.utn.frc.tup.piii.engine.model.TrainerCard.Builder("xy1-118", "Profesor Cipres", ar.edu.utn.frc.tup.piii.engine.model.TrainerType.SUPPORTER).build();
        session.getPlayerRuntime(0).getHand().addCard(trainerCard);

        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(
                new ar.edu.utn.frc.tup.piii.engine.model.PlayTrainerAction(
                        ar.edu.utn.frc.tup.piii.engine.model.TrainerType.SUPPORTER, null, "xy1-118"));
        when(ruleValidator.validate(any(), any(Integer.class))).thenReturn(new ValidationResult.Valid());

        matchService.processAction(MATCH_ID, PLAYER_A_ID, dto);

        final org.mockito.ArgumentCaptor<ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse> captor =
                org.mockito.ArgumentCaptor.forClass(ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse.class);
        verify(chatService).addMessage(org.mockito.ArgumentMatchers.eq(MATCH_ID), captor.capture());

        ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse loggedMessage = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(loggedMessage.getSender()).isEqualTo("SISTEMA");
        org.assertj.core.api.Assertions.assertThat(loggedMessage.getMessage()).isEqualTo(PLAYER_A_ID + " jugó la carta de Entrenador: Profesor Cipres");
    }

    @Test
    void shouldLogCoinFlipsToChat() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.RETREAT, null, null, null, null, null);
        
        when(facade.toEngineAction(any(), any(Integer.class), any())).thenReturn(
                new ar.edu.utn.frc.tup.piii.engine.model.RetreatAction(board.getActivePokemon(0)));
        when(ruleValidator.validate(any(), any(Integer.class))).thenReturn(new ValidationResult.Valid());

        session.setCoinFlipper(() -> true);
        org.mockito.Mockito.doAnswer(invocation -> {
            session.getCoinFlipper().flip();
            return null;
        }).when(facade).apply(any(), any(), any());

        matchService.processAction(MATCH_ID, PLAYER_A_ID, dto);

        final org.mockito.ArgumentCaptor<ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse> captor =
                org.mockito.ArgumentCaptor.forClass(ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse.class);
        verify(chatService).addMessage(org.mockito.ArgumentMatchers.eq(MATCH_ID), captor.capture());

        ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse loggedMessage = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(loggedMessage.getSender()).isEqualTo("SISTEMA");
        org.assertj.core.api.Assertions.assertThat(loggedMessage.getMessage()).contains("Lanzamiento de moneda");
        org.assertj.core.api.Assertions.assertThat(loggedMessage.getMessage()).contains("CARA");
    }
}
