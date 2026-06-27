package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.ActionType;
import ar.edu.utn.frc.tup.piii.engine.model.*;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

public class BotDecisionServiceTest {

    private MatchSessionRegistry registry;
    private MatchService matchService;
    private GameFacade facade;
    private BotDecisionService botDecisionService;

    @BeforeEach
    public void setUp() {
        registry = mock(MatchSessionRegistry.class);
        matchService = mock(MatchService.class);
        facade = mock(GameFacade.class);
        botDecisionService = new BotDecisionService(registry, matchService, facade);
    }

    @Test
    public void testEvaluateAndPlaySessionNotFound() {
        when(registry.find("match-1")).thenReturn(Optional.empty());
        botDecisionService.evaluateAndPlay("match-1");
        verifyNoInteractions(matchService);
    }

    @Test
    public void testEvaluateAndPlayGameFinished() {
        MatchSession session = mock(MatchSession.class);
        when(session.getWinnerId()).thenReturn("Player-1");
        when(registry.find("match-1")).thenReturn(Optional.of(session));

        botDecisionService.evaluateAndPlay("match-1");
        verifyNoInteractions(matchService);
    }

    @Test
    public void testEvaluateAndPlayNoBotInSession() {
        MatchSession session = mock(MatchSession.class);
        when(session.getWinnerId()).thenReturn(null);
        when(session.getPlayerIds()).thenReturn(List.of("Player-1", "Player-2"));
        when(registry.find("match-1")).thenReturn(Optional.of(session));

        botDecisionService.evaluateAndPlay("match-1");
        verifyNoInteractions(matchService);
    }

    @Test
    public void testEvaluateAndPlayNotBotTurn() {
        MatchSession session = mock(MatchSession.class, RETURNS_DEEP_STUBS);
        when(session.getWinnerId()).thenReturn(null);
        when(session.getPlayerIds()).thenReturn(List.of("Player-1", "Bot-1")); // Bot index = 1
        when(session.isAwaitingPromotion()).thenReturn(false);
        when(session.getPendingSelectionRequest()).thenReturn(null);
        when(session.getTurnManager().activePlayerIndex()).thenReturn(0); // Player-1's turn
        when(registry.find("match-1")).thenReturn(Optional.of(session));

        botDecisionService.evaluateAndPlay("match-1");
        verifyNoInteractions(matchService);
    }

    @Test
    public void testEvaluateAndPlayAwaitingPromotionForBot() throws Exception {
        MatchSession session = mock(MatchSession.class, RETURNS_DEEP_STUBS);
        when(session.getWinnerId()).thenReturn(null);
        when(session.getPlayerIds()).thenReturn(List.of("Player-1", "Bot-1"));
        when(session.isAwaitingPromotion()).thenReturn(true);
        when(session.getPromotingPlayerIndex()).thenReturn(1); // It is the bot promoting

        PlayerRuntime botRuntime = mock(PlayerRuntime.class, RETURNS_DEEP_STUBS);
        when(session.getPlayerRuntime(1)).thenReturn(botRuntime);

        BattlePokemonState benchPokemon = mock(BattlePokemonState.class);
        when(benchPokemon.getMaxHp()).thenReturn(70);
        when(benchPokemon.getDamageCounters()).thenReturn(2); // Remaining HP = 50
        when(botRuntime.getBench().getAll()).thenReturn(List.of(benchPokemon));

        when(registry.find("match-1")).thenReturn(Optional.of(session));

        botDecisionService.evaluateAndPlay("match-1");

        verify(matchService, times(1)).processAction(
                eq("match-1"),
                eq("Bot-1"),
                argThat(action -> action.type() == ActionType.PROMOTE_ACTIVE && action.targetIndex() == 0)
        );
    }

    @Test
    public void testEvaluateAndPlayPendingSelectionRequestProfessorsLetter() throws Exception {
        MatchSession session = mock(MatchSession.class, RETURNS_DEEP_STUBS);
        when(session.getWinnerId()).thenReturn(null);
        when(session.getPlayerIds()).thenReturn(List.of("Player-1", "Bot-1"));
        when(session.isAwaitingPromotion()).thenReturn(false);
        
        PendingSelectionRequest pendingRequest = mock(PendingSelectionRequest.class);
        when(pendingRequest.sourceEffect()).thenReturn(TrainerEffectId.PROFESSORS_LETTER);
        when(pendingRequest.maxSelections()).thenReturn(2);
        when(session.getPendingSelectionRequest()).thenReturn(pendingRequest);
        when(session.getTurnManager().activePlayerIndex()).thenReturn(1);

        PlayerRuntime botRuntime = mock(PlayerRuntime.class, RETURNS_DEEP_STUBS);
        when(session.getPlayerRuntime(1)).thenReturn(botRuntime);

        EnergyCard basicEnergy1 = mock(EnergyCard.class);
        when(basicEnergy1.isBasic()).thenReturn(true);
        when(basicEnergy1.getCardId()).thenReturn("energy-1");

        EnergyCard basicEnergy2 = mock(EnergyCard.class);
        when(basicEnergy2.isBasic()).thenReturn(true);
        when(basicEnergy2.getCardId()).thenReturn("energy-2");

        when(botRuntime.getDeck().getCards()).thenReturn(List.of(basicEnergy1, basicEnergy2));
        when(registry.find("match-1")).thenReturn(Optional.of(session));

        botDecisionService.evaluateAndPlay("match-1");

        verify(matchService, times(1)).processAction(
                eq("match-1"),
                eq("Bot-1"),
                argThat(action -> action.type() == ActionType.SELECT_CARDS && action.selectedCardIds().contains("energy-1"))
        );
    }

    @Test
    public void testEvaluateAndPlayNormalTurnEndTurn() throws Exception {
        MatchSession session = mock(MatchSession.class, RETURNS_DEEP_STUBS);
        when(session.getWinnerId()).thenReturn(null);
        when(session.getPlayerIds()).thenReturn(List.of("Player-1", "Bot-1"));
        when(session.isAwaitingPromotion()).thenReturn(false);
        when(session.getPendingSelectionRequest()).thenReturn(null);
        when(session.getTurnManager().activePlayerIndex()).thenReturn(1);

        PlayerRuntime botRuntime = mock(PlayerRuntime.class, RETURNS_DEEP_STUBS);
        when(session.getPlayerRuntime(1)).thenReturn(botRuntime);
        when(botRuntime.getHand().getCards()).thenReturn(Collections.emptyList());
        when(botRuntime.getActivePokemon()).thenReturn(null);

        when(registry.find("match-1")).thenReturn(Optional.of(session));

        botDecisionService.evaluateAndPlay("match-1");

        verify(matchService, times(1)).processAction(
                eq("match-1"),
                eq("Bot-1"),
                argThat(action -> action.type() == ActionType.END_TURN)
        );
    }

    @Test
    public void testEvaluateAndPlayNormalTurnWithAttack() throws Exception {
        MatchSession session = mock(MatchSession.class);
        ar.edu.utn.frc.tup.piii.engine.manager.TurnManager turnManager = mock(ar.edu.utn.frc.tup.piii.engine.manager.TurnManager.class);
        ar.edu.utn.frc.tup.piii.engine.manager.RuleValidator ruleValidator = mock(ar.edu.utn.frc.tup.piii.engine.manager.RuleValidator.class);
        when(session.getWinnerId()).thenReturn(null);
        when(session.getPlayerIds()).thenReturn(List.of("Player-1", "Bot-1"));
        when(session.isAwaitingPromotion()).thenReturn(false);
        when(session.getPendingSelectionRequest()).thenReturn(null);
        when(session.getTurnManager()).thenReturn(turnManager);
        when(session.getRuleValidator()).thenReturn(ruleValidator);
        when(turnManager.activePlayerIndex()).thenReturn(1);

        PlayerRuntime botRuntime = mock(PlayerRuntime.class);
        ar.edu.utn.frc.tup.piii.engine.model.Hand hand = mock(ar.edu.utn.frc.tup.piii.engine.model.Hand.class);
        ar.edu.utn.frc.tup.piii.engine.model.Bench bench = mock(ar.edu.utn.frc.tup.piii.engine.model.Bench.class);
        when(session.getPlayerRuntime(1)).thenReturn(botRuntime);
        when(botRuntime.getHand()).thenReturn(hand);
        when(hand.getCards()).thenReturn(Collections.emptyList());
        when(botRuntime.getBench()).thenReturn(bench);
        when(bench.size()).thenReturn(0);
        when(bench.getAll()).thenReturn(Collections.emptyList());

        BattlePokemonState activePokemon = mock(BattlePokemonState.class);
        Attack mockAttack = mock(Attack.class);
        when(activePokemon.getAttacks()).thenReturn(List.of(mockAttack));
        when(botRuntime.getActivePokemon()).thenReturn(activePokemon);

        // Stub findValidAction to find a valid attack
        Action mockAction = new DeclareAttackAction(activePokemon, mockAttack);
        when(facade.toEngineAction(eq(session), eq(1), any(ActionRequestDTO.class))).thenReturn(mockAction);
        when(session.getRuleValidator().validate(eq(mockAction), eq(1))).thenReturn(new ValidationResult.Valid());

        when(registry.find("match-1")).thenReturn(Optional.of(session));

        botDecisionService.evaluateAndPlay("match-1");

        verify(matchService, times(1)).processAction(
                eq("match-1"),
                eq("Bot-1"),
                argThat(action -> action.type() == ActionType.DECLARE_ATTACK && action.attackIndex() == 0)
        );
    }
}
