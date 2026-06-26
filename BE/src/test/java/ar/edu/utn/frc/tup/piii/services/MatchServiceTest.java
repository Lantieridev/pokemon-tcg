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
import ar.edu.utn.frc.tup.piii.services.persistence.GameStatePersistence;
import ar.edu.utn.frc.tup.piii.services.persistence.GameStateSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.messaging.simp.SimpMessagingTemplate;

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
        when(registry.find(MATCH_ID)).thenReturn(Optional.of(session));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(UserEntity.builder().id(1L).username("test").build()));

        final GameStateResponseDTO fakeView = new GameStateResponseDTO(
                MATCH_ID, 1L, 1, 0, "ACTIVE", null,
                new GameStateResponseDTO.PlayerView(PLAYER_A_ID, null, List.of(), List.of(), 45, 6),
                new GameStateResponseDTO.OpponentView(PLAYER_B_ID, null, List.of(), 0, 45, 6),
                null);
        when(mapper.toResponse(any(), any(Integer.class))).thenReturn(fakeView);

        matchService = new MatchService(
                registry, facade, persistence, mapper, messaging,
                scheduler, penaltyService, profileService, userRepository, botDecisionService, mmrCalculationService, TIMEOUT_SECONDS);
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
}
