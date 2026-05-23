package ar.edu.utn.frc.tup.piii.services.persistence;

import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.services.MatchSessionRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Primary
public class JpaGameStatePersistence implements GameStatePersistence {

    private final MatchSessionRegistry registry;
    private final ApplicationEventPublisher eventPublisher;

    public JpaGameStatePersistence(final MatchSessionRegistry registry,
                                   final ApplicationEventPublisher eventPublisher) {
        this.registry = Objects.requireNonNull(registry);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    @Override
    public void save(final GameStateSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        registry.find(snapshot.matchId()).ifPresent(this::saveMatch);
    }

    @Override
    public void saveMatch(final MatchSession session) {
        Objects.requireNonNull(session, "session must not be null");
        eventPublisher.publishEvent(new SaveMatchEvent(session));
    }

    @Override
    public void logAction(final String matchId, final int turnNumber, final String playerId, final String actionType, final String result) {
        eventPublisher.publishEvent(new LogActionEvent(matchId, turnNumber, playerId, actionType, result));
    }

    @Override
    @Deprecated
    public void declareWinner(final String matchId, final String winnerUsername) {
        // Deprecated: Winner persistence has been unified into the saveMatch flow.
        // This method is now a no-op to maintain backward compatibility.
    }
}

