package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse;
import ar.edu.utn.frc.tup.piii.dtos.ChatReportRequest;
import ar.edu.utn.frc.tup.piii.persistence.entity.ChatReportEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.MatchEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.ChatReportRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatReportRepository chatReportRepository;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(matchRepository, userRepository, chatReportRepository);
    }

    @Test
    void shouldAddAndRetrieveMessages() {
        final String matchId = "123";
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
        final String matchId = "limit-match";

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
        final String matchId = "clear-match";
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

    @Test
    void shouldCreateReportSuccessfully() {
        final String matchId = "123";
        final ChatReportRequest request = new ChatReportRequest(1L, 2L, "Toxic behavior");

        final MatchEntity match = new MatchEntity();
        match.setId(123L);

        final UserEntity reporter = new UserEntity();
        reporter.setId(1L);

        final UserEntity reported = new UserEntity();
        reported.setId(2L);

        when(matchRepository.findById(123L)).thenReturn(Optional.of(match));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reported));

        // Add some chat messages to cache to verify snapshot capture
        final ChatMessageResponse msg = ChatMessageResponse.builder()
                .sender("user2")
                .message("Bad words")
                .timestamp(LocalDateTime.now())
                .build();
        chatService.addMessage(matchId, msg);

        chatService.createReport(matchId, request);

        final ArgumentCaptor<ChatReportEntity> reportCaptor = ArgumentCaptor.forClass(ChatReportEntity.class);
        verify(chatReportRepository).save(reportCaptor.capture());

        final ChatReportEntity savedReport = reportCaptor.getValue();
        assertEquals(match, savedReport.getMatch());
        assertEquals(reporter, savedReport.getReporter());
        assertEquals(reported, savedReport.getReported());
        assertEquals("Toxic behavior", savedReport.getReason());
        assertEquals(1, savedReport.getChatHistory().size());
        assertEquals("Bad words", savedReport.getChatHistory().get(0).getMessage());
    }

    @Test
    void shouldThrowExceptionWhenMatchNotFound() {
        final String matchId = "123";
        final ChatReportRequest request = new ChatReportRequest(1L, 2L, "Toxic");

        when(matchRepository.findById(123L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> chatService.createReport(matchId, request));
    }
}
