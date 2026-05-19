package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.engine.exception.InvalidActionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that GlobalExceptionHandler maps domain exceptions to correct HTTP status codes.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn404WhenNoSuchElementExceptionIsThrown() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenIllegalArgumentExceptionIsThrown() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn422WhenInvalidActionExceptionIsThrown() throws Exception {
        mockMvc.perform(get("/test/invalid-action"))
                .andExpect(status().isUnprocessableEntity());
    }

    @RestController
    static final class ThrowingController {

        @GetMapping("/test/not-found")
        String notFound() {
            throw new NoSuchElementException("match not found");
        }

        @GetMapping("/test/bad-request")
        String badRequest() {
            throw new IllegalArgumentException("invalid input");
        }

        @GetMapping("/test/invalid-action")
        String invalidAction() {
            throw new InvalidActionException("retreat_blocked");
        }
    }
}
