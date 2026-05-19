package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry that holds all active {@link MatchSession} instances.
 * Backed by a {@link ConcurrentHashMap} keyed on matchId.
 */
@Component
public class MatchSessionRegistry {

    private final ConcurrentHashMap<String, MatchSession> sessions = new ConcurrentHashMap<>();

    /**
     * Registers a new match session.
     *
     * @param session the session to register (never null)
     * @throws IllegalArgumentException if a session with the same matchId is already registered
     */
    public void register(final MatchSession session) {
        final MatchSession existing = sessions.putIfAbsent(session.getMatchId(), session);
        if (existing != null) {
            throw new IllegalArgumentException(
                    "A session with matchId '" + session.getMatchId() + "' is already registered.");
        }
    }

    /**
     * Finds a match session by its matchId.
     *
     * @param matchId the identifier to look up
     * @return an Optional containing the session, or empty if not found
     */
    public Optional<MatchSession> find(final String matchId) {
        return Optional.ofNullable(sessions.get(matchId));
    }

    /**
     * Removes a match session from the registry.
     * This is a no-op if no session with the given matchId exists.
     *
     * @param matchId the identifier of the session to remove
     */
    public void remove(final String matchId) {
        sessions.remove(matchId);
    }
}
