package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.HonorType;
import ar.edu.utn.frc.tup.piii.services.HonorService;
import ar.edu.utn.frc.tup.piii.services.MuteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private MuteService muteService;

    @Mock
    private HonorService honorService;

    @Mock
    private Principal principal;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final UserController userController = new UserController(muteService, honorService);
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    void shouldMuteUserSuccessfully() throws Exception {
        final String targetUser = "toxic_player";
        when(principal.getName()).thenReturn("active_user");

        mockMvc.perform(post("/api/users/mute/{targetUsername}", targetUser)
                        .principal(principal))
                .andExpect(status().isOk());

        verify(muteService).muteUser("active_user", targetUser);
    }

    @Test
    void shouldUnmuteUserSuccessfully() throws Exception {
        final String targetUser = "toxic_player";
        when(principal.getName()).thenReturn("active_user");

        mockMvc.perform(delete("/api/users/mute/{targetUsername}", targetUser)
                        .principal(principal))
                .andExpect(status().isOk());

        verify(muteService).unmuteUser("active_user", targetUser);
    }

    @Test
    void shouldReturnMutedUsersList() throws Exception {
        when(principal.getName()).thenReturn("active_user");
        when(muteService.getMutedUsers("active_user")).thenReturn(Set.of("user_a", "user_b"));

        mockMvc.perform(get("/api/users/mute")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"user_a\",\"user_b\"]"));
    }

    @Test
    void shouldAwardHonorSuccessfully() throws Exception {
        final String targetUser = "good_player";
        final String requestJson = "{\"honorType\":\"GOOD_SPORTSMAN\"}";
        when(principal.getName()).thenReturn("active_user");

        mockMvc.perform(post("/api/users/{username}/honor", targetUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .principal(principal))
                .andExpect(status().isOk());

        verify(honorService).awardHonor("active_user", targetUser, HonorType.GOOD_SPORTSMAN);
    }

    @Test
    void shouldReturnUserHonors() throws Exception {
        final String targetUser = "good_player";
        when(honorService.getHonors(targetUser)).thenReturn(Map.of(
                HonorType.GOOD_SPORTSMAN, 3,
                HonorType.FRIENDLY, 5
        ));

        mockMvc.perform(get("/api/users/{username}/honor", targetUser))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"GOOD_SPORTSMAN\":3,\"FRIENDLY\":5}"));
    }
}
