package ar.edu.utn.frc.tup.piii.services.persistence;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NoOpGameStatePersistenceTest {

    @Test
    void shouldCompleteWithoutThrowingWhenSaveIsCalled() {
        final NoOpGameStatePersistence noOp = new NoOpGameStatePersistence();
        final GameStateSnapshot snapshot = new GameStateSnapshot("match-1", 3, List.of("p1", "p2"));
        assertDoesNotThrow(() -> noOp.save(snapshot));
    }
}
