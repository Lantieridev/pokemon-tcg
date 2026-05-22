package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.ChatMessageRequest;
import ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse;
import ar.edu.utn.frc.tup.piii.services.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private Principal principal;

    private ChatWebSocketController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatWebSocketController(chatService);
    }

    @Test
    void shouldBroadcastAndSaveMessageUsingPrincipalName() {
        final String matchId = "match-456";
        final ChatMessageRequest request = new ChatMessageRequest("fake_sender", "Hello World");
        when(principal.getName()).thenReturn("authenticated_user");

        final ChatMessageResponse response = controller.broadcastMessage(matchId, request, principal);

        assertNotNull(response);
        assertEquals("authenticated_user", response.getSender()); // Impersonation prevented
        assertEquals("Hello World", response.getMessage());
        assertNotNull(response.getTimestamp());

        final ArgumentCaptor<ChatMessageResponse> captor = ArgumentCaptor.forClass(ChatMessageResponse.class);
        verify(chatService).addMessage(eq(matchId), captor.capture());
        assertEquals("authenticated_user", captor.getValue().getSender());
        assertEquals("Hello World", captor.getValue().getMessage());
    }

    @Test
    void shouldFallbackToRequestSenderWhenPrincipalIsNull() {
        final String matchId = "match-789";
        final ChatMessageRequest request = new ChatMessageRequest("anon_user", "Hello anonymous");

        final ChatMessageResponse response = controller.broadcastMessage(matchId, request, null);

        assertNotNull(response);
        assertEquals("anon_user", response.getSender());
        assertEquals("Hello anonymous", response.getMessage());

        verify(chatService).addMessage(eq(matchId), any(ChatMessageResponse.class));
    }
}
