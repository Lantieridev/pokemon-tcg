package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.RankingDto;
import ar.edu.utn.frc.tup.piii.services.RankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class RankingControllerTest {

    @Mock
    private RankingService rankingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final RankingController rankingController = new RankingController(rankingService);
        mockMvc = MockMvcBuilders.standaloneSetup(rankingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getGlobalRankingTest() throws Exception {
        final List<RankingDto> rankings = List.of(
                new RankingDto("player-alice", 10L),
                new RankingDto("player-bob", 5L)
        );
        final Slice<RankingDto> slice = new SliceImpl<>(rankings, PageRequest.of(0, 10), false);

        when(rankingService.getGlobalRanking(PageRequest.of(0, 10))).thenReturn(slice);

        mockMvc.perform(get("/api/rankings")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("player-alice"))
                .andExpect(jsonPath("$.content[0].wins").value(10))
                .andExpect(jsonPath("$.content[1].username").value("player-bob"))
                .andExpect(jsonPath("$.content[1].wins").value(5))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getGlobalRankingValidationTest() throws Exception {
        // Negative page index -> 400 Bad Request
        mockMvc.perform(get("/api/rankings")
                        .param("page", "-1")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Zero size -> 400 Bad Request
        mockMvc.perform(get("/api/rankings")
                        .param("page", "0")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getGlobalRankingSizeCappingTest() throws Exception {
        final List<RankingDto> rankings = List.of(
                new RankingDto("player-alice", 10L)
        );
        final Slice<RankingDto> slice = new SliceImpl<>(rankings, PageRequest.of(0, 50), false);

        // Even though user asks for 100, the controller should cap it to 50
        when(rankingService.getGlobalRanking(PageRequest.of(0, 50))).thenReturn(slice);

        mockMvc.perform(get("/api/rankings")
                        .param("page", "0")
                        .param("size", "100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("player-alice"));
    }
}
