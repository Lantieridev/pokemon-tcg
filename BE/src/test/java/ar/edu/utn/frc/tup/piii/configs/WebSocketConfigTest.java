package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private ChannelRegistration channelRegistration;

    @Mock
    private MessageChannel messageChannel;

    private WebSocketConfig webSocketConfig;

    @BeforeEach
    void setUp() {
        webSocketConfig = new WebSocketConfig(jwtUtil, userDetailsService);
    }

    @Test
    void shouldRegisterInterceptorAndAuthenticateValidToken() {
        final ArgumentCaptor<ChannelInterceptor> captor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        webSocketConfig.configureClientInboundChannel(channelRegistration);
        verify(channelRegistration).interceptors(captor.capture());
        final ChannelInterceptor interceptor = captor.getValue();
        assertNotNull(interceptor);

        final StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer valid-token");
        accessor.setLeaveMutable(true);
        final Message<byte[]> stompMessage = org.springframework.messaging.support.MessageBuilder
                .createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUtil.isValidToken("valid-token")).thenReturn(true);
        when(jwtUtil.getUsernameFromToken("valid-token")).thenReturn("john_doe");
        final UserDetails userDetails = new User("john_doe", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername("john_doe")).thenReturn(userDetails);

        final Message<?> resultMessage = interceptor.preSend(stompMessage, messageChannel);
        assertNotNull(resultMessage);

        final StompHeaderAccessor resultAccessor = MessageHeaderAccessor.getAccessor(resultMessage, StompHeaderAccessor.class);
        assertNotNull(resultAccessor);
        final UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) resultAccessor.getUser();
        assertNotNull(auth);
        assertEquals("john_doe", auth.getName());
    }

    @Test
    void shouldThrowExceptionWhenTokenIsInvalid() {
        final ArgumentCaptor<ChannelInterceptor> captor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        webSocketConfig.configureClientInboundChannel(channelRegistration);
        verify(channelRegistration).interceptors(captor.capture());
        final ChannelInterceptor interceptor = captor.getValue();

        final StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer invalid-token");
        accessor.setLeaveMutable(true);
        final Message<byte[]> stompMessage = org.springframework.messaging.support.MessageBuilder
                .createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUtil.isValidToken("invalid-token")).thenReturn(false);

        assertThrows(org.springframework.messaging.MessagingException.class, () -> {
            interceptor.preSend(stompMessage, messageChannel);
        });
    }

    @Test
    void shouldThrowExceptionWhenTokenIsMissing() {
        final ArgumentCaptor<ChannelInterceptor> captor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        webSocketConfig.configureClientInboundChannel(channelRegistration);
        verify(channelRegistration).interceptors(captor.capture());
        final ChannelInterceptor interceptor = captor.getValue();

        final StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        final Message<byte[]> stompMessage = org.springframework.messaging.support.MessageBuilder
                .createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(org.springframework.messaging.MessagingException.class, () -> {
            interceptor.preSend(stompMessage, messageChannel);
        });
    }

    @Test
    void shouldAuthorizeSubscribeToOwnChannel() {
        final ArgumentCaptor<ChannelInterceptor> captor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        webSocketConfig.configureClientInboundChannel(channelRegistration);
        verify(channelRegistration).interceptors(captor.capture());
        final ChannelInterceptor interceptor = captor.getValue();

        final StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/match/match-123/player/john_doe");
        final UsernamePasswordAuthenticationToken auth = mock(UsernamePasswordAuthenticationToken.class);
        when(auth.getName()).thenReturn("john_doe");
        accessor.setUser(auth);
        accessor.setLeaveMutable(true);
        final Message<byte[]> stompMessage = org.springframework.messaging.support.MessageBuilder
                .createMessage(new byte[0], accessor.getMessageHeaders());

        final Message<?> resultMessage = interceptor.preSend(stompMessage, messageChannel);
        assertNotNull(resultMessage);
    }

    @Test
    void shouldRejectSubscribeToOtherChannel() {
        final ArgumentCaptor<ChannelInterceptor> captor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        webSocketConfig.configureClientInboundChannel(channelRegistration);
        verify(channelRegistration).interceptors(captor.capture());
        final ChannelInterceptor interceptor = captor.getValue();

        final StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/match/match-123/player/misty");
        final UsernamePasswordAuthenticationToken auth = mock(UsernamePasswordAuthenticationToken.class);
        when(auth.getName()).thenReturn("john_doe");
        accessor.setUser(auth);
        accessor.setLeaveMutable(true);
        final Message<byte[]> stompMessage = org.springframework.messaging.support.MessageBuilder
                .createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(org.springframework.messaging.MessagingException.class, () -> {
            interceptor.preSend(stompMessage, messageChannel);
        });
    }

    @Test
    void shouldRejectSubscribeWhenOwnUsernameIsOnlyATextualPrefixOfTheChannelOwner() {
        final ArgumentCaptor<ChannelInterceptor> captor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        webSocketConfig.configureClientInboundChannel(channelRegistration);
        verify(channelRegistration).interceptors(captor.capture());
        final ChannelInterceptor interceptor = captor.getValue();

        // Destination belongs to "john_doe2024" — "john_doe" is a textual prefix of it,
        // which used to fool a contains()-based check.
        final StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/match/match-123/player/john_doe2024");
        final UsernamePasswordAuthenticationToken auth = mock(UsernamePasswordAuthenticationToken.class);
        when(auth.getName()).thenReturn("john_doe");
        accessor.setUser(auth);
        accessor.setLeaveMutable(true);
        final Message<byte[]> stompMessage = org.springframework.messaging.support.MessageBuilder
                .createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(org.springframework.messaging.MessagingException.class, () -> {
            interceptor.preSend(stompMessage, messageChannel);
        });
    }
}
