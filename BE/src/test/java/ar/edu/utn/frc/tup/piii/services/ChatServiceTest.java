package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse;
import ar.edu.utn.frc.tup.piii.services.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatServiceTest {

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl();
    }

    @Test
    void shouldAddAndRetrieveMessages() {
        final String matchId = "match-123";
        final ChatMessageResponse msg1 = ChatMessageResponse.builder()
                .sender("user1")
                .message("Hello")
                .timestamp(LocalDateTime.now())
                .build();
        final ChatMessageResponse msg2 = ChatMessageResponse.builder()
                .sender("user2")
                .message("Hi there")
                .timestamp(LocalDateTime.now())
                .build();

        chatService.addMessage(matchId, msg1);
        chatService.addMessage(matchId, msg2);

        final List<ChatMessageResponse> messages = chatService.getMessages(matchId);
        assertEquals(2, messages.size());
        assertEquals("user1", messages.get(0).getSender());
        assertEquals("Hello", messages.get(0).getMessage());
        assertEquals("user2", messages.get(1).getSender());
        assertEquals("Hi there", messages.get(1).getMessage());
    }

    @Test
    void shouldEnforceMaxLimitOf50MessagesFIFO() {
        final String matchId = "match-limit";

        // Add 55 messages
        for (int i = 1; i <= 55; i++) {
            final ChatMessageResponse msg = ChatMessageResponse.builder()
                    .sender("user")
                    .message("Msg " + i)
                    .timestamp(LocalDateTime.now())
                    .build();
            chatService.addMessage(matchId, msg);
        }

        final List<ChatMessageResponse> messages = chatService.getMessages(matchId);
        // Should only keep exactly 50
        assertEquals(50, messages.size());
        // The first one should be "Msg 6" (since 1 to 5 were evicted)
        assertEquals("Msg 6", messages.get(0).getMessage());
        // The last one should be "Msg 55"
        assertEquals("Msg 55", messages.get(49).getMessage());
    }

    @Test
    void shouldReturnEmptyListForUnknownMatch() {
        final List<ChatMessageResponse> messages = chatService.getMessages("non-existent");
        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    @Test
    void shouldClearMessagesSuccessfully() {
        final String matchId = "match-clear";
        final ChatMessageResponse msg = ChatMessageResponse.builder()
                .sender("user")
                .message("Clear me")
                .timestamp(LocalDateTime.now())
                .build();

        chatService.addMessage(matchId, msg);
        assertFalse(chatService.getMessages(matchId).isEmpty());

        chatService.clearMessages(matchId);
        assertTrue(chatService.getMessages(matchId).isEmpty());
    }
}
