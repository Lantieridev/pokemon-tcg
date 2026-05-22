package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.services.MuteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
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
    private Principal principal;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final UserController userController = new UserController(muteService);
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
}
