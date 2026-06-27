package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.CampaignProgressResponseDTO;
import ar.edu.utn.frc.tup.piii.services.CampaignService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CampaignRestControllerTest {

    private MockMvc mockMvc;
    private CampaignService campaignService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        campaignService = mock(CampaignService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CampaignRestController(campaignService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getProgress_withNullPrincipal_returns401() throws Exception {
        mockMvc.perform(get("/api/campaign/progress"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProgress_withValidPrincipal_returns200() throws Exception {
        Principal principal = () -> "testUser";
        CampaignProgressResponseDTO progress = CampaignProgressResponseDTO.builder()
                .clearedNodesCount(1)
                .totalNodesCount(8)
                .nodes(List.of())
                .build();

        when(campaignService.getCampaignProgress("testUser")).thenReturn(progress);

        mockMvc.perform(get("/api/campaign/progress").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clearedNodesCount").value(1))
                .andExpect(jsonPath("$.totalNodesCount").value(8));
    }

    @Test
    void challengeNode_withNullPrincipal_returns401() throws Exception {
        mockMvc.perform(post("/api/campaign/challenge/1").param("deckId", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void challengeNode_withValidRequest_returns201() throws Exception {
        Principal principal = () -> "testUser";
        when(campaignService.iniciarDesafioPvE("testUser", 1, 10L)).thenReturn("match-123");

        mockMvc.perform(post("/api/campaign/challenge/1")
                        .param("deckId", "10")
                        .principal(principal))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchId").value("match-123"));
    }

    @Test
    void challengeNode_whenNodeNotFound_returns404() throws Exception {
        Principal principal = () -> "testUser";
        when(campaignService.iniciarDesafioPvE(anyString(), anyInt(), anyLong()))
                .thenThrow(new NoSuchElementException("Nodo de campaña inválido: 9"));

        mockMvc.perform(post("/api/campaign/challenge/9")
                        .param("deckId", "10")
                        .principal(principal))
                .andExpect(status().isNotFound());
    }

    @Test
    void challengeNode_whenNodeBlocked_returns400() throws Exception {
        Principal principal = () -> "testUser";
        when(campaignService.iniciarDesafioPvE(anyString(), anyInt(), anyLong()))
                .thenThrow(new IllegalArgumentException("Este nodo de la campaña se encuentra bloqueado."));

        mockMvc.perform(post("/api/campaign/challenge/2")
                        .param("deckId", "10")
                        .principal(principal))
                .andExpect(status().isBadRequest());
    }
}
