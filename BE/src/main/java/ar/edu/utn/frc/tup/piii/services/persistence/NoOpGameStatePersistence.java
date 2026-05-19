package ar.edu.utn.frc.tup.piii.services.persistence;

import org.springframework.stereotype.Component;

/**
 * No-operation implementation of {@link GameStatePersistence}.
 * Silently discards snapshots — used until the Flyway/JPA persistence layer is wired (Module 4).
 */
@Component
public class NoOpGameStatePersistence implements GameStatePersistence {

    /**
     * Discards the snapshot without performing any I/O.
     *
     * @param snapshot the snapshot to discard (never null)
     */
    @Override
    public void save(final GameStateSnapshot snapshot) {
        // intentional no-op — persistence wired in Module 4
    }
}
