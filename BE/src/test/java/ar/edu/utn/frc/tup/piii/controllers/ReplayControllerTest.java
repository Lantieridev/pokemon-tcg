package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse;
import ar.edu.utn.frc.tup.piii.services.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReplayControllerTest {

    @Mock
    private ChatService chatService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final ReplayController replayController = new ReplayController(chatService);
        mockMvc = MockMvcBuilders.standaloneSetup(replayController).build();
    }

    @Test
    void shouldReturnChatHistory() throws Exception {
        final String matchId = "match-123";
        final List<ChatMessageResponse> messages = List.of(
                ChatMessageResponse.builder()
                        .sender("user1")
                        .message("hello")
                        .timestamp(LocalDateTime.now())
                        .build(),
                ChatMessageResponse.builder()
                        .sender("user2")
                        .message("world")
                        .timestamp(LocalDateTime.now())
                        .build()
        );

        when(chatService.getMessages(matchId)).thenReturn(messages);

        mockMvc.perform(get("/api/matches/{matchId}/chat", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].sender").value("user1"))
                .andExpect(jsonPath("$[0].message").value("hello"))
                .andExpect(jsonPath("$[1].sender").value("user2"))
                .andExpect(jsonPath("$[1].message").value("world"));
    }
}
