package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.exception.FirstTurnAttackException;
import ar.edu.utn.frc.tup.piii.engine.exception.InvalidPhaseTransitionException;
import ar.edu.utn.frc.tup.piii.engine.exception.InvalidTurnPhaseException;
import ar.edu.utn.frc.tup.piii.engine.listener.PhaseEvent;
import ar.edu.utn.frc.tup.piii.engine.listener.PhaseListener;
import ar.edu.utn.frc.tup.piii.engine.model.AttackPhase;
import ar.edu.utn.frc.tup.piii.engine.model.BetweenTurnsPhase;
import ar.edu.utn.frc.tup.piii.engine.model.DrawPhase;
import ar.edu.utn.frc.tup.piii.engine.model.MainPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for TurnManager — all sub-batches A–E plus fix-batch F. FR-002 through FR-014.
 */
class TurnManagerTest {

    private TurnManager turnManager;
    private PhaseListener listener;

    @BeforeEach
    void setUp() {
        turnManager = new TurnManager();
        listener = Mockito.mock(PhaseListener.class);
    }

    // -------------------------------------------------------------------------
    // Sub-batch A: initial state + startTurn
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnNullPhaseWhenNoTurnHasStarted() {
        assertNull(turnManager.currentPhase());
    }

    @Test
    void shouldReturnMinusOnePlayerIndexWhenNoTurnHasStarted() {
        assertEquals(-1, turnManager.activePlayerIndex());
    }

    @Test
    void shouldReportBothPlayersAsFirstTurnWhenNoTurnHasStarted() {
        assertTrue(turnManager.isFirstTurnOfPlayer(0));
        assertTrue(turnManager.isFirstTurnOfPlayer(1));
    }

    @Test
    void shouldEnterDrawPhaseWhenStartTurnCalledFromNotStarted() {
        turnManager.startTurn(0);
        assertInstanceOf(DrawPhase.class, turnManager.currentPhase());
        assertEquals(0, turnManager.activePlayerIndex());
    }

    @Test
    void shouldFireTurnStartedAndPhaseEnteredEventsWhenStartTurnInvoked() {
        turnManager.registerListener(listener);
        turnManager.startTurn(0);

        ArgumentCaptor<PhaseEvent> captor = ArgumentCaptor.forClass(PhaseEvent.class);
        verify(listener, times(2)).on(captor.capture());

        List<PhaseEvent> events = captor.getAllValues();
        assertInstanceOf(PhaseEvent.TurnStarted.class, events.get(0));
        assertInstanceOf(PhaseEvent.PhaseEntered.class, events.get(1));
        assertEquals(0, events.get(0).playerIndex());
        assertEquals(0, events.get(1).playerIndex());
    }

    @Test
    void shouldThrowInvalidTurnPhaseExceptionWhenStartTurnCalledWithNegativePlayerIndex() {
        assertThrows(InvalidTurnPhaseException.class, () -> turnManager.startTurn(-1));
    }

    @Test
    void shouldThrowInvalidTurnPhaseExceptionWhenStartTurnCalledWithPlayerIndexTooHigh() {
        assertThrows(InvalidTurnPhaseException.class, () -> turnManager.startTurn(2));
    }

    // -------------------------------------------------------------------------
    // Sub-batch A — Fix-batch F (C-001): startTurn guard
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenStartTurnCalledWhileInDrawPhase() {
        turnManager.startTurn(0);
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.startTurn(0));
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenStartTurnCalledWhileInMainPhase() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.startTurn(0));
    }

    // -------------------------------------------------------------------------
    // Sub-batch B: endDraw + requireMainPhase
    // -------------------------------------------------------------------------

    @Test
    void shouldTransitionToMainPhaseWhenEndDrawCalledInDrawPhase() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        assertInstanceOf(MainPhase.class, turnManager.currentPhase());
    }

    @Test
    void shouldFirePhaseExitedAndPhaseEnteredEventsWhenEndDrawCalled() {
        turnManager.registerListener(listener);
        turnManager.startTurn(0);
        Mockito.reset(listener);

        turnManager.endDraw();

        ArgumentCaptor<PhaseEvent> captor = ArgumentCaptor.forClass(PhaseEvent.class);
        verify(listener, times(2)).on(captor.capture());

        List<PhaseEvent> events = captor.getAllValues();
        assertInstanceOf(PhaseEvent.PhaseExited.class, events.get(0));
        assertInstanceOf(DrawPhase.class, events.get(0).phase());
        assertInstanceOf(PhaseEvent.PhaseEntered.class, events.get(1));
        assertInstanceOf(MainPhase.class, events.get(1).phase());
    }

    @Test
    void shouldCreateFreshMainPhaseInstanceWhenEndDrawIsCalledOnNewTurn() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        MainPhase firstMain = (MainPhase) turnManager.currentPhase();

        // end turn and start a new one
        turnManager.passTurn();
        turnManager.endBetweenTurns();
        // now player 1 is active in DrawPhase
        turnManager.endDraw();
        MainPhase secondMain = (MainPhase) turnManager.currentPhase();

        assertNotSame(firstMain, secondMain);
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenEndDrawCalledInMainPhase() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.endDraw());
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenEndDrawCalledInAttackPhase() {
        turnManager.startTurn(1); // player 1 — no first-turn restriction
        turnManager.endDraw();
        turnManager.declareAttack();
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.endDraw());
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenEndDrawCalledInBetweenTurnsPhase() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        turnManager.passTurn();
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.endDraw());
    }

    @Test
    void shouldReturnMainPhaseInstanceWhenCurrentPhaseIsMain() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        MainPhase main = turnManager.requireMainPhase();
        assertNotNull(main);
        assertInstanceOf(MainPhase.class, main);
    }

    @Test
    void shouldThrowInvalidTurnPhaseExceptionWhenRequireMainPhaseCalledInDrawPhase() {
        turnManager.startTurn(0);
        assertThrows(InvalidTurnPhaseException.class, () -> turnManager.requireMainPhase());
    }

    @Test
    void shouldThrowInvalidTurnPhaseExceptionWhenRequireMainPhaseCalledInAttackPhase() {
        turnManager.startTurn(1);
        turnManager.endDraw();
        turnManager.declareAttack();
        assertThrows(InvalidTurnPhaseException.class, () -> turnManager.requireMainPhase());
    }

    @Test
    void shouldThrowInvalidTurnPhaseExceptionWhenRequireMainPhaseCalledInBetweenTurnsPhase() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        turnManager.passTurn();
        assertThrows(InvalidTurnPhaseException.class, () -> turnManager.requireMainPhase());
    }

    @Test
    void shouldHaveAllCountersResetWhenFreshMainPhaseIsCreated() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        MainPhase first = turnManager.requireMainPhase();
        first.recordEnergyAttached();
        first.recordSupporterPlayed();

        // advance to second turn
        turnManager.passTurn();
        turnManager.endBetweenTurns();
        turnManager.endDraw();
        MainPhase second = turnManager.requireMainPhase();

        assertEquals(0, second.getEnergyAttached());
        assertFalse(second.isSupporterPlayed());
    }

    // -------------------------------------------------------------------------
    // Sub-batch B — Fix-batch F (W-001): null guards
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenEndDrawCalledBeforeStartTurn() {
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.endDraw());
    }

    @Test
    void shouldThrowInvalidTurnPhaseExceptionWhenRequireMainPhaseCalledBeforeStartTurn() {
        assertThrows(InvalidTurnPhaseException.class, () -> turnManager.requireMainPhase());
    }

    // -------------------------------------------------------------------------
    // Sub-batch C: declareAttack + passTurn + endAttack
    // -------------------------------------------------------------------------

    @Test
    void shouldTransitionToAttackPhaseWhenDeclareAttackCalledInMainPhase() {
        turnManager.startTurn(1); // player 1 — no first-turn restriction
        turnManager.endDraw();
        turnManager.declareAttack();
        assertInstanceOf(AttackPhase.class, turnManager.currentPhase());
    }

    @Test
    void shouldFirePhaseExitedAndPhaseEnteredEventsWhenDeclareAttackCalled() {
        turnManager.startTurn(1);
        turnManager.endDraw();
        turnManager.registerListener(listener);

        turnManager.declareAttack();

        ArgumentCaptor<PhaseEvent> captor = ArgumentCaptor.forClass(PhaseEvent.class);
        verify(listener, times(2)).on(captor.capture());

        List<PhaseEvent> events = captor.getAllValues();
        assertInstanceOf(PhaseEvent.PhaseExited.class, events.get(0));
        assertInstanceOf(MainPhase.class, events.get(0).phase());
        assertInstanceOf(PhaseEvent.PhaseEntered.class, events.get(1));
        assertInstanceOf(AttackPhase.class, events.get(1).phase());
    }

    @Test
    void shouldThrowFirstTurnAttackExceptionWhenPlayerZeroAttacksOnFirstTurn() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        assertThrows(FirstTurnAttackException.class, () -> turnManager.declareAttack());
    }

    @Test
    void shouldAllowAttackWhenPlayerOneIsOnFirstTurn() {
        turnManager.startTurn(1);
        turnManager.endDraw();
        assertDoesNotThrow(() -> turnManager.declareAttack());
        assertInstanceOf(AttackPhase.class, turnManager.currentPhase());
    }

    @Test
    void shouldAllowPlayer0ToAttackAfterFirstTurnCompleted() {
        // Player 0 passes first turn
        turnManager.startTurn(0);
        turnManager.endDraw();
        turnManager.passTurn();
        turnManager.endBetweenTurns(); // marks player 0 first turn done, switches to player 1

        // Player 1 turn — need to complete it to get back to player 0
        turnManager.endDraw();
        turnManager.passTurn();
        turnManager.endBetweenTurns(); // switches back to player 0

        // Now player 0 is on second turn — attack should be allowed
        turnManager.endDraw();
        assertDoesNotThrow(() -> turnManager.declareAttack());
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenDeclareAttackCalledInDrawPhase() {
        turnManager.startTurn(1);
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.declareAttack());
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenDeclareAttackCalledInAttackPhase() {
        turnManager.startTurn(1);
        turnManager.endDraw();
        turnManager.declareAttack();
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.declareAttack());
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenDeclareAttackCalledInBetweenTurnsPhase() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        turnManager.passTurn();
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.declareAttack());
    }

    @Test
    void shouldTransitionToBetweenTurnsPhaseWhenPassTurnCalledInMainPhase() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        turnManager.passTurn();
        assertInstanceOf(BetweenTurnsPhase.class, turnManager.currentPhase());
    }

    @Test
    void shouldFireCorrectEventsWhenPassTurnCalled() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        turnManager.registerListener(listener);

        turnManager.passTurn();

        ArgumentCaptor<PhaseEvent> captor = ArgumentCaptor.forClass(PhaseEvent.class);
        verify(listener, times(2)).on(captor.capture());

        List<PhaseEvent> events = captor.getAllValues();
        assertInstanceOf(PhaseEvent.PhaseExited.class, events.get(0));
        assertInstanceOf(MainPhase.class, events.get(0).phase());
        assertInstanceOf(PhaseEvent.PhaseEntered.class, events.get(1));
        assertInstanceOf(BetweenTurnsPhase.class, events.get(1).phase());
    }

    @Test
    void shouldAllowPassTurnWhenPlayerZeroIsOnFirstTurn() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        assertDoesNotThrow(() -> turnManager.passTurn());
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenPassTurnCalledInDrawPhase() {
        turnManager.startTurn(0);
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.passTurn());
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenPassTurnCalledInAttackPhase() {
        turnManager.startTurn(1);
        turnManager.endDraw();
        turnManager.declareAttack();
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.passTurn());
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenPassTurnCalledInBetweenTurnsPhase() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        turnManager.passTurn();
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.passTurn());
    }

    @Test
    void shouldTransitionToBetweenTurnsPhaseWhenEndAttackCalledInAttackPhase() {
        turnManager.startTurn(1);
        turnManager.endDraw();
        turnManager.declareAttack();
        turnManager.endAttack();
        assertInstanceOf(BetweenTurnsPhase.class, turnManager.currentPhase());
    }

    @Test
    void shouldFirePhaseExitedAndPhaseEnteredEventsWhenEndAttackCalled() {
        turnManager.startTurn(1);
        turnManager.endDraw();
        turnManager.declareAttack();
        turnManager.registerListener(listener);

        turnManager.endAttack();

        ArgumentCaptor<PhaseEvent> captor = ArgumentCaptor.forClass(PhaseEvent.class);
        verify(listener, times(2)).on(captor.capture());

        List<PhaseEvent> events = captor.getAllValues();
        assertInstanceOf(PhaseEvent.PhaseExited.class, events.get(0));
        assertInstanceOf(AttackPhase.class, events.get(0).phase());
        assertInstanceOf(PhaseEvent.PhaseEntered.class, events.get(1));
        assertInstanceOf(BetweenTurnsPhase.class, events.get(1).phase());
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenEndAttackCalledInMainPhase() {
        turnManager.startTurn(1);
        turnManager.endDraw();
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.endAttack());
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenEndAttackCalledInDrawPhase() {
        turnManager.startTurn(1);
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.endAttack());
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenEndAttackCalledInBetweenTurnsPhase() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        turnManager.passTurn();
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.endAttack());
    }

    // -------------------------------------------------------------------------
    // Sub-batch C — Fix-batch F (W-001): null guards
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenDeclareAttackCalledBeforeStartTurn() {
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.declareAttack());
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenPassTurnCalledBeforeStartTurn() {
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.passTurn());
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenEndAttackCalledBeforeStartTurn() {
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.endAttack());
    }

    // -------------------------------------------------------------------------
    // Sub-batch D: endBetweenTurns + player switching + first-turn tracking
    // -------------------------------------------------------------------------

    @Test
    void shouldTransitionToDrawPhaseForNextPlayerWhenEndBetweenTurnsCalled() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        turnManager.passTurn();
        turnManager.endBetweenTurns();
        assertInstanceOf(DrawPhase.class, turnManager.currentPhase());
    }

    @Test
    void shouldSwitchActivePlayerWhenEndBetweenTurnsIsCalled() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        turnManager.passTurn();
        turnManager.endBetweenTurns();
        assertEquals(1, turnManager.activePlayerIndex());
    }

    @Test
    void shouldMarkFirstTurnCompletedForPlayer0WhenEndBetweenTurnsIsCalled() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        turnManager.passTurn();
        turnManager.endBetweenTurns();
        assertFalse(turnManager.isFirstTurnOfPlayer(0));
    }

    @Test
    void shouldNotMarkFirstTurnCompletedForPlayer1AfterPlayer0Turn() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        turnManager.passTurn();
        turnManager.endBetweenTurns();
        assertTrue(turnManager.isFirstTurnOfPlayer(1));
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenEndBetweenTurnsCalledInDrawPhase() {
        turnManager.startTurn(0);
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.endBetweenTurns());
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenEndBetweenTurnsCalledInMainPhase() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.endBetweenTurns());
    }

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenEndBetweenTurnsCalledInAttackPhase() {
        turnManager.startTurn(1);
        turnManager.endDraw();
        turnManager.declareAttack();
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.endBetweenTurns());
    }

    @Test
    void shouldFireEventsInCorrectOrderWhenEndBetweenTurnsCalled() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        turnManager.passTurn();
        turnManager.registerListener(listener);

        turnManager.endBetweenTurns();

        ArgumentCaptor<PhaseEvent> captor = ArgumentCaptor.forClass(PhaseEvent.class);
        verify(listener, times(4)).on(captor.capture());

        List<PhaseEvent> events = captor.getAllValues();
        // [0] PhaseExited — player 0
        assertInstanceOf(PhaseEvent.PhaseExited.class, events.get(0));
        assertEquals(0, events.get(0).playerIndex());
        assertInstanceOf(BetweenTurnsPhase.class, events.get(0).phase());

        // [1] TurnEnded — player 0
        assertInstanceOf(PhaseEvent.TurnEnded.class, events.get(1));
        assertEquals(0, events.get(1).playerIndex());

        // [2] TurnStarted — player 1
        assertInstanceOf(PhaseEvent.TurnStarted.class, events.get(2));
        assertEquals(1, events.get(2).playerIndex());
        assertInstanceOf(DrawPhase.class, events.get(2).phase());

        // [3] PhaseEntered — player 1
        assertInstanceOf(PhaseEvent.PhaseEntered.class, events.get(3));
        assertEquals(1, events.get(3).playerIndex());
        assertInstanceOf(DrawPhase.class, events.get(3).phase());
    }

    @Test
    void shouldAlternatePlayerAfterTwoEndBetweenTurnsCalls() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        turnManager.passTurn();
        turnManager.endBetweenTurns(); // switches to player 1

        turnManager.endDraw();
        turnManager.passTurn();
        turnManager.endBetweenTurns(); // switches back to player 0

        assertEquals(0, turnManager.activePlayerIndex());
    }

    @Test
    void shouldReturnTrueForIsFirstTurnOfPlayerZeroWhenNoTurnCompleted() {
        assertTrue(turnManager.isFirstTurnOfPlayer(0));
    }

    @Test
    void shouldReturnFalseForIsFirstTurnOfPlayerZeroWhenEndBetweenTurnsHasBeenCalled() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        turnManager.passTurn();
        turnManager.endBetweenTurns();
        assertFalse(turnManager.isFirstTurnOfPlayer(0));
    }

    @Test
    void shouldProceedToMainPhaseNormallyAfterEndDrawOnPlayerZeroFirstTurn() {
        turnManager.startTurn(0);
        turnManager.endDraw();
        assertInstanceOf(MainPhase.class, turnManager.currentPhase());
        assertEquals(0, turnManager.activePlayerIndex());
    }

    // -------------------------------------------------------------------------
    // Sub-batch D — Fix-batch F (W-001): null guards
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowInvalidPhaseTransitionExceptionWhenEndBetweenTurnsCalledBeforeStartTurn() {
        assertThrows(InvalidPhaseTransitionException.class, () -> turnManager.endBetweenTurns());
    }

    // -------------------------------------------------------------------------
    // Sub-batch E: observer wiring
    // -------------------------------------------------------------------------

    @Test
    void shouldNotifyListenerWhenEventIsFired() {
        turnManager.registerListener(listener);
        turnManager.startTurn(0);
        verify(listener, times(2)).on(any(PhaseEvent.class));
    }

    @Test
    void shouldNotNotifyUnregisteredListener() {
        turnManager.registerListener(listener);
        turnManager.unregisterListener(listener);
        turnManager.startTurn(0);
        verifyNoInteractions(listener);
    }

    @Test
    void shouldNotThrowWhenListenerListIsModifiedDuringEventDelivery() {
        PhaseListener[] captured = {null};
        PhaseListener selfAdding = event -> {
            PhaseListener extra = e -> { };
            turnManager.registerListener(extra);
            captured[0] = extra;
        };
        turnManager.registerListener(selfAdding);
        assertDoesNotThrow(() -> turnManager.startTurn(0));
    }

    @Test
    void shouldNotifyMultipleListenersWhenEventIsFired() {
        PhaseListener listenerA = Mockito.mock(PhaseListener.class);
        PhaseListener listenerB = Mockito.mock(PhaseListener.class);
        turnManager.registerListener(listenerA);
        turnManager.registerListener(listenerB);
        turnManager.startTurn(0);
        verify(listenerA, times(2)).on(any(PhaseEvent.class));
        verify(listenerB, times(2)).on(any(PhaseEvent.class));
    }

    @Test
    void shouldBeNoOpWhenUnregisteringAbsentListener() {
        assertDoesNotThrow(() -> turnManager.unregisterListener(listener));
    }

    @Test
    void shouldNotFireAnyEventWhenNoListenersRegistered() {
        assertDoesNotThrow(() -> turnManager.startTurn(0));
    }

    // -------------------------------------------------------------------------
    // Sub-batch E — Fix-batch F (W-002): null listener guard
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowIllegalArgumentExceptionWhenRegisteringNullListener() {
        assertThrows(IllegalArgumentException.class, () -> turnManager.registerListener(null));
    }

    // -------------------------------------------------------------------------
    // Bug-3: first-turn block must respect configured starting player
    // -------------------------------------------------------------------------

    @Test
    void shouldBlockFirstTurnAttackWhenStartingPlayerIsPlayer1() {
        turnManager.setStartingPlayer(1);
        turnManager.startTurn(1);
        turnManager.endDraw();
        assertThrows(FirstTurnAttackException.class, () -> turnManager.declareAttack());
    }

    @Test
    void shouldAllowAttackForNonStartingPlayerOnTheirFirstTurn() {
        // Player 1 is the starting player; Player 0 is second and should be
        // able to attack on their first move (they are not the starting player)
        turnManager.setStartingPlayer(1);
        // Player 1 goes first and passes
        turnManager.startTurn(1);
        turnManager.endDraw();
        turnManager.passTurn();
        turnManager.endBetweenTurns(); // switches to player 0

        // Player 0 is on their first turn but is NOT the starting player
        turnManager.endDraw();
        assertDoesNotThrow(() -> turnManager.declareAttack());
    }

    @Test
    void shouldResetAllTurnStateForSuddenDeathRestart() {
        // Simulate a completed turn cycle so manager has non-null state
        turnManager.startTurn(0);
        turnManager.endDraw();
        turnManager.passTurn();
        turnManager.endBetweenTurns(); // firstTurnCompleted[0] = true, now player 1's turn

        // Match ended mid-turn (e.g. KO during player 1's MainPhase)
        turnManager.endDraw();

        // reset() must clear everything so startTurn can be called again
        turnManager.reset();

        assertNull(turnManager.currentPhase(), "Phase should be null after reset");
        assertEquals(-1, turnManager.activePlayerIndex(), "Active player should be -1 after reset");
        assertTrue(turnManager.isFirstTurnOfPlayer(0), "Player 0 first-turn flag should be reset");
        assertTrue(turnManager.isFirstTurnOfPlayer(1), "Player 1 first-turn flag should be reset");

        // Must be able to start a fresh turn (Sudden Death) without exception
        assertDoesNotThrow(() -> turnManager.startTurn(1));
    }

    @Test
    void shouldRespectFirstTurnAttackRestrictionAfterReset() {
        // Complete a full match cycle
        turnManager.startTurn(0);
        turnManager.endDraw();
        turnManager.passTurn();
        turnManager.endBetweenTurns();
        turnManager.endDraw();
        turnManager.passTurn();
        turnManager.endBetweenTurns();

        // Reset for Sudden Death — starting player is now 1
        turnManager.reset();
        turnManager.setStartingPlayer(1);
        turnManager.startTurn(1);
        turnManager.endDraw();

        // Player 1 is the starting player on their first turn — attack must be blocked
        assertThrows(FirstTurnAttackException.class, () -> turnManager.declareAttack());
    }

    // -------------------------------------------------------------------------
    // Turn Counting API
    // -------------------------------------------------------------------------

    @Test
    void shouldTrackTurnCountsPerPlayer() {
        assertEquals(0, turnManager.getTurnCount(0));
        assertEquals(0, turnManager.getTurnCount(1));

        // Player 0 turn 1
        turnManager.startTurn(0);
        assertEquals(1, turnManager.getTurnCount(0));
        assertEquals(0, turnManager.getTurnCount(1));
        
        turnManager.endDraw();
        turnManager.passTurn();
        turnManager.endBetweenTurns(); // Starts player 1's turn

        assertEquals(1, turnManager.getTurnCount(0));
        assertEquals(1, turnManager.getTurnCount(1));

        turnManager.endDraw();
        turnManager.passTurn();
        turnManager.endBetweenTurns(); // Starts player 0's turn 2

        assertEquals(2, turnManager.getTurnCount(0));
        assertEquals(1, turnManager.getTurnCount(1));
    }

    @Test
    void shouldResetTurnCountsOnReset() {
        turnManager.startTurn(0);
        assertEquals(1, turnManager.getTurnCount(0));

        turnManager.reset();
        
        assertEquals(0, turnManager.getTurnCount(0));
        assertEquals(0, turnManager.getTurnCount(1));
    }

    @Test
    void shouldAllowInterruptDuringAttackPhase() {
        turnManager.startTurn(1);
        turnManager.endDraw();
        turnManager.declareAttack();

        assertInstanceOf(AttackPhase.class, turnManager.currentPhase());

        assertDoesNotThrow(() -> turnManager.interruptMainPhase());
        assertInstanceOf(ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase.class, turnManager.currentPhase());

        assertDoesNotThrow(() -> turnManager.resumeMainPhase());
        assertInstanceOf(AttackPhase.class, turnManager.currentPhase());
    }
}
