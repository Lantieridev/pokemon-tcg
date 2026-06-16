package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.auth.AuthLoginRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.auth.AuthRegisterRequestDTO;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private Filter springSecurityFilterChain;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @Autowired
    private ar.edu.utn.frc.tup.piii.persistence.repository.MatchLogRepository matchLogRepository;

    @Autowired
    private ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository matchRepository;

    @Autowired
    private ar.edu.utn.frc.tup.piii.persistence.repository.UserCardStatRepository userCardStatRepository;

    @Autowired
    private ar.edu.utn.frc.tup.piii.persistence.repository.UserEnergyStatRepository userEnergyStatRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
        matchLogRepository.deleteAll();
        matchRepository.deleteAll();
        userCardStatRepository.deleteAll();
        userEnergyStatRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testRegisterAndLoginWorkflow() throws Exception {
        // 1. Try to access a secured match endpoint without authentication -> Should be blocked/rejected (401 Unauthorized or 403 Forbidden)
        mockMvc.perform(get("/api/matches/1/state")
                        .header("X-Player-Id", "player-a"))
                .andExpect(status().isForbidden()); // Spring Security returns 403 Forbidden by default if not authenticated

        // 2. Register a new user
        AuthRegisterRequestDTO registerRequest = new AuthRegisterRequestDTO();
        registerRequest.setUsername("ash_ketchum");
        registerRequest.setEmail("ash@pallet.com");
        registerRequest.setPassword("pikachu_forever");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        // 3. Register user again -> Should fail with BadRequest (Duplicate username)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());

        // 4. Login with registered user
        AuthLoginRequestDTO loginRequest = new AuthLoginRequestDTO();
        loginRequest.setUsername("ash_ketchum");
        loginRequest.setPassword("pikachu_forever");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username").value("ash_ketchum"));

        // 5. Login with invalid password -> Should return BadRequest or Unauthorized
        AuthLoginRequestDTO badLoginRequest = new AuthLoginRequestDTO();
        badLoginRequest.setUsername("ash_ketchum");
        badLoginRequest.setPassword("wrong_password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLoginRequest)))
                .andExpect(status().isBadRequest());
    }
}
