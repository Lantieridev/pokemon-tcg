package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.MatchHistoryDto;
import ar.edu.utn.frc.tup.piii.services.HistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class HistoryControllerTest {

    @Mock
    private HistoryService historyService;

    private MockMvc mockMvc;
    private UserDetails testUser;

    @BeforeEach
    void setUp() {
        testUser = User.withUsername("player-alice")
                .password("pwd")
                .authorities("ROLE_USER")
                .build();

        final HistoryController historyController = new HistoryController(historyService);

        final HandlerMethodArgumentResolver mockResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(final MethodParameter parameter) {
                return parameter.getParameterType().equals(UserDetails.class);
            }

            @Override
            public Object resolveArgument(final MethodParameter parameter, final ModelAndViewContainer mavContainer,
                                          final NativeWebRequest webRequest, final WebDataBinderFactory binderFactory) {
                return webRequest.getUserPrincipal() != null ? testUser : null;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(historyController)
                .setCustomArgumentResolvers(mockResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getUserHistorySuccessTest() throws Exception {
        final List<MatchHistoryDto> historyList = List.of(
                new MatchHistoryDto(1001L, "player-bob", "FINISHED", "VICTORY", LocalDateTime.now()),
                new MatchHistoryDto(1002L, "player-charlie", "ACTIVE", "IN_PROGRESS", LocalDateTime.now())
        );
        final Slice<MatchHistoryDto> slice = new SliceImpl<>(historyList, PageRequest.of(0, 10), false);

        when(historyService.getUserMatchHistory("player-alice", PageRequest.of(0, 10))).thenReturn(slice);

        mockMvc.perform(get("/api/users/me/history")
                        .principal(new UsernamePasswordAuthenticationToken(testUser, null))
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].matchId").value(1001))
                .andExpect(jsonPath("$.content[0].opponent").value("player-bob"))
                .andExpect(jsonPath("$.content[0].result").value("VICTORY"))
                .andExpect(jsonPath("$.content[1].matchId").value(1002))
                .andExpect(jsonPath("$.content[1].result").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getUserHistoryValidationTest() throws Exception {
        // Negative page index -> 400 Bad Request
        mockMvc.perform(get("/api/users/me/history")
                        .principal(new UsernamePasswordAuthenticationToken(testUser, null))
                        .param("page", "-1")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Zero size -> 400 Bad Request
        mockMvc.perform(get("/api/users/me/history")
                        .principal(new UsernamePasswordAuthenticationToken(testUser, null))
                        .param("page", "0")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserHistoryCappingTest() throws Exception {
        final List<MatchHistoryDto> historyList = List.of(
                new MatchHistoryDto(1001L, "player-bob", "FINISHED", "VICTORY", LocalDateTime.now())
        );
        final Slice<MatchHistoryDto> slice = new SliceImpl<>(historyList, PageRequest.of(0, 50), false);

        // Asking for size 100 should cap it to 50
        when(historyService.getUserMatchHistory("player-alice", PageRequest.of(0, 50))).thenReturn(slice);

        mockMvc.perform(get("/api/users/me/history")
                        .principal(new UsernamePasswordAuthenticationToken(testUser, null))
                        .param("page", "0")
                        .param("size", "100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].opponent").value("player-bob"));
    }

    @Test
    void getUserHistoryUnauthenticatedTest() throws Exception {
        // No principal -> Should return 400 Bad Request because UserDetails is null
        mockMvc.perform(get("/api/users/me/history")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
