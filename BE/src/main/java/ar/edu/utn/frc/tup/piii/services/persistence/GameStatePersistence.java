package ar.edu.utn.frc.tup.piii.services.persistence;

/**
 * Port for persisting game state after each successful action.
 * Implementations may write to a database, a message bus, or a no-op sink.
 */
public interface GameStatePersistence {

    /**
     * Persists the given game state snapshot.
     * Called after every successful action, before broadcasting to clients.
     *
     * @param snapshot the snapshot to persist (never null)
     */
    void save(GameStateSnapshot snapshot);
}
