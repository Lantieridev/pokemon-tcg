package ar.edu.utn.frc.tup.piii.engine.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Tests for the turn-phase exception hierarchy. FR-013.
 */
class ExceptionHierarchyTest {

    @Test
    void shouldBeRuntimeExceptionSubtypeForInvalidTurnPhaseException() {
        assertInstanceOf(RuntimeException.class, new InvalidTurnPhaseException("test"));
    }

    @Test
    void shouldBeRuntimeExceptionSubtypeForInvalidPhaseTransitionException() {
        assertInstanceOf(RuntimeException.class, new InvalidPhaseTransitionException("test"));
    }

    @Test
    void shouldBeRuntimeExceptionSubtypeForFirstTurnAttackException() {
        assertInstanceOf(RuntimeException.class, new FirstTurnAttackException("test"));
    }

    @Test
    void shouldBeInvalidTurnPhaseExceptionSubtypeForFirstTurnAttack() {
        assertInstanceOf(InvalidTurnPhaseException.class, new FirstTurnAttackException("test"));
    }

    @Test
    void shouldPreserveMessageForInvalidTurnPhaseException() {
        assertEquals("msg", new InvalidTurnPhaseException("msg").getMessage());
    }

    @Test
    void shouldPreserveMessageForInvalidPhaseTransitionException() {
        assertEquals("msg", new InvalidPhaseTransitionException("msg").getMessage());
    }

    @Test
    void shouldPreserveMessageForFirstTurnAttackException() {
        assertEquals("msg", new FirstTurnAttackException("msg").getMessage());
    }
}
