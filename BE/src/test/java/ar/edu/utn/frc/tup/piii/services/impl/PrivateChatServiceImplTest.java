package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.friends.ChatMessageDTO;
import ar.edu.utn.frc.tup.piii.persistence.entity.ChatMessageEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.ChatMessageRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PrivateChatServiceImplTest {

    private ChatMessageRepository chatMessageRepository;
    private UserRepository userRepository;
    private PrivateChatServiceImpl privateChatService;

    @BeforeEach
    public void setUp() {
        chatMessageRepository = mock(ChatMessageRepository.class);
        userRepository = mock(UserRepository.class);
        privateChatService = new PrivateChatServiceImpl(chatMessageRepository, userRepository);
    }

    @Test
    public void testSaveAndSendMessageUserNotFound() {
        when(userRepository.findFirstByUsername("unknown")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> {
            privateChatService.saveAndSendMessage("unknown", "lucas", "hello");
        });
    }

    @Test
    public void testSaveAndSendMessageSuccess() {
        UserEntity sender = UserEntity.builder().id(1L).username("sender").build();
        UserEntity receiver = UserEntity.builder().id(2L).username("receiver").build();
        LocalDateTime time = LocalDateTime.now();
        ChatMessageEntity chatMsg = ChatMessageEntity.builder()
                .id(100L)
                .sender(sender)
                .receiver(receiver)
                .content("hola amigo")
                .createdAt(time)
                .build();

        when(userRepository.findFirstByUsername("sender")).thenReturn(Optional.of(sender));
        when(userRepository.findFirstByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(chatMessageRepository.save(any(ChatMessageEntity.class))).thenReturn(chatMsg);

        ChatMessageDTO result = privateChatService.saveAndSendMessage("sender", "receiver", "hola amigo");

        assertNotNull(result);
        assertEquals("sender", result.getSenderUsername());
        assertEquals("receiver", result.getReceiverUsername());
        assertEquals("hola amigo", result.getContent());
        assertEquals(time, result.getTimestamp());
        verify(chatMessageRepository, times(1)).save(any(ChatMessageEntity.class));
    }

    @Test
    public void testGetChatHistory() {
        UserEntity user1 = UserEntity.builder().id(1L).username("user1").build();
        UserEntity user2 = UserEntity.builder().id(2L).username("user2").build();
        LocalDateTime time = LocalDateTime.now();
        ChatMessageEntity chatMsg = ChatMessageEntity.builder()
                .sender(user1)
                .receiver(user2)
                .content("hi")
                .createdAt(time)
                .build();

        when(userRepository.findFirstByUsername("user1")).thenReturn(Optional.of(user1));
        when(userRepository.findFirstByUsername("user2")).thenReturn(Optional.of(user2));
        when(chatMessageRepository.findChatHistory(user1, user2)).thenReturn(List.of(chatMsg));

        List<ChatMessageDTO> history = privateChatService.getChatHistory("user1", "user2");

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("user1", history.get(0).getSenderUsername());
        assertEquals("user2", history.get(0).getReceiverUsername());
        assertEquals("hi", history.get(0).getContent());
        assertEquals(time, history.get(0).getTimestamp());
    }
}
