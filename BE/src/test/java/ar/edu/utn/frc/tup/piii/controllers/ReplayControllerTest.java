package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse;
import ar.edu.utn.frc.tup.piii.dtos.ChatReportRequest;
import ar.edu.utn.frc.tup.piii.dtos.ReplayEventDTO;
import ar.edu.utn.frc.tup.piii.dtos.ReplayResponseDTO;
import ar.edu.utn.frc.tup.piii.services.ChatService;
import ar.edu.utn.frc.tup.piii.services.ReplayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frc.tup.piii.services.MuteService;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class ReplayControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private ReplayService replayService;

    @Mock
    private MuteService muteService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final ReplayController replayController = new ReplayController(chatService, replayService, muteService);
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

    @Test
    void shouldCreateChatReportSuccessfully() throws Exception {
        final String matchId = "123";
        final String jsonRequest = "{\"reporterId\":1,\"reportedId\":2,\"reason\":\"Toxic behavior\"}";

        mockMvc.perform(post("/api/matches/{matchId}/reports", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk());

        verify(chatService).createReport(eq(matchId), any(ChatReportRequest.class));
    }

    @Test
    void shouldReturnReplayEvents() throws Exception {
        final Long matchId = 123L;
        final ReplayResponseDTO replayResponse = ReplayResponseDTO.builder()
                .matchId(matchId)
                .events(List.of(
                        ReplayEventDTO.builder()
                                .turn(1)
                                .player("player1")
                                .action("DRAW_CARD")
                                .result("Drew Pikachu")
                                .timestamp(LocalDateTime.now())
                                .build()
                ))
                .build();

        when(replayService.getReplay(matchId)).thenReturn(replayResponse);

        mockMvc.perform(get("/api/matches/{matchId}/replay", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(matchId))
                .andExpect(jsonPath("$.events", hasSize(1)))
                .andExpect(jsonPath("$.events[0].player").value("player1"))
                .andExpect(jsonPath("$.events[0].action").value("DRAW_CARD"));
    }
}
