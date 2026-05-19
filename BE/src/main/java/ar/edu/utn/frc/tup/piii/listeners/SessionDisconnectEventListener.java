package ar.edu.utn.frc.tup.piii.listeners;

import ar.edu.utn.frc.tup.piii.services.MatchService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Objects;

/**
 * Spring event listener that fires when a WebSocket session disconnects.
 * Resolves match and player headers from the STOMP frame and delegates to
 * {@link MatchService} to start the abandonment countdown.
 *
 * <p>Graceful no-op when either header is missing (e.g. the client disconnected
 * before joining a match).</p>
 */
@Component
public final class SessionDisconnectEventListener {

    private static final String MATCH_ID_HEADER = "matchId";
    private static final String PLAYER_ID_HEADER = "playerId";

    private final MatchService matchService;

    /**
     * Constructs the listener with the required service collaborator.
     *
     * @param matchService handles player disconnect lifecycle (never null)
     */
    public SessionDisconnectEventListener(final MatchService matchService) {
        this.matchService = Objects.requireNonNull(matchService, "matchService must not be null");
    }

    /**
     * Handles a WebSocket session disconnect.
     * Reads {@code matchId} and {@code playerId} from the STOMP message headers.
     * If either header is absent the method returns silently (no-op).
     *
     * @param event the disconnect event published by Spring WebSocket infrastructure
     */
    @EventListener
    public void onDisconnect(final SessionDisconnectEvent event) {
        final StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        final String matchId = headers.getFirstNativeHeader(MATCH_ID_HEADER);
        final String playerId = headers.getFirstNativeHeader(PLAYER_ID_HEADER);
        if (matchId == null || playerId == null) {
            return;
        }
        matchService.onPlayerDisconnect(matchId, playerId);
    }
}
