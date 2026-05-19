package ar.edu.utn.frc.tup.piii.listeners;

import ar.edu.utn.frc.tup.piii.services.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SessionDisconnectEventListenerTest {

    @Mock
    private MatchService matchService;

    private SessionDisconnectEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new SessionDisconnectEventListener(matchService);
    }

    private Message<byte[]> buildStompMessage(final Map<String, List<String>> nativeHeaders) {
        final StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        nativeHeaders.forEach((key, values) -> values.forEach(v -> accessor.addNativeHeader(key, v)));
        final Map<String, Object> springHeaders = new HashMap<>(accessor.toMap());
        return new GenericMessage<>(new byte[0], springHeaders);
    }

    @Test
    void shouldCallOnPlayerDisconnectWhenSessionDisconnectEventFired() {
        final String matchId = "match-1";
        final String playerId = "player-a";

        final Map<String, List<String>> native_ = new HashMap<>();
        native_.put("matchId", List.of(matchId));
        native_.put("playerId", List.of(playerId));
        final Message<byte[]> message = buildStompMessage(native_);

        final SessionDisconnectEvent event = new SessionDisconnectEvent(
                new Object(), message, "session-1", null);

        listener.onDisconnect(event);

        verify(matchService).onPlayerDisconnect(matchId, playerId);
    }

    @Test
    void shouldNotThrowWhenMatchIdHeaderIsMissing() {
        final Map<String, List<String>> native_ = new HashMap<>();
        native_.put("playerId", List.of("player-a"));
        final Message<byte[]> message = buildStompMessage(native_);

        final SessionDisconnectEvent event = new SessionDisconnectEvent(
                new Object(), message, "session-1", null);

        assertDoesNotThrow(() -> listener.onDisconnect(event));
        verifyNoInteractions(matchService);
    }
}
