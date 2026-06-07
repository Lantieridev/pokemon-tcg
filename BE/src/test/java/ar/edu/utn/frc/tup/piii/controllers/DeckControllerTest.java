package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckSummaryDTO;
import ar.edu.utn.frc.tup.piii.services.deck.DeckService;
import ar.edu.utn.frc.tup.piii.services.deck.InvalidDeckException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeckControllerTest {

    private MockMvc mockMvc;
    private DeckService deckService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        deckService = mock(DeckService.class);
        ar.edu.utn.frc.tup.piii.services.deck.DeckTemplateService templateService = mock(ar.edu.utn.frc.tup.piii.services.deck.DeckTemplateService.class);
        
        final DeckResponseDTO template = new DeckResponseDTO(
                -1L, "Template", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.PRECONSTRUCTED, LocalDateTime.now(), List.of());
        when(templateService.getTemplateById(any(Long.class))).thenReturn(template);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new DeckController(deckService, templateService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getAll_returns200WithEmptyList() throws Exception {
        when(deckService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/decks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAll_returns200WithDeckList() throws Exception {
        when(deckService.getAll()).thenReturn(List.of(
                new DeckSummaryDTO(1L, "Fire Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID, LocalDateTime.now(), 60),
                new DeckSummaryDTO(2L, "Water Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID, LocalDateTime.now(), 60)
        ));

        mockMvc.perform(get("/api/decks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Fire Deck"))
                .andExpect(jsonPath("$[1].name").value("Water Deck"));
    }

    @Test
    void getById_returns200WithDeck() throws Exception {
        final DeckResponseDTO response = new DeckResponseDTO(
                42L, "My Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID, LocalDateTime.now(),
                List.of(new DeckCardResponseDTO("xy1-1", "Bulbasaur", "Pokémon", "Basic", 4)));

        when(deckService.getById(42L)).thenReturn(response);

        mockMvc.perform(get("/api/decks/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.name").value("My Deck"))
                .andExpect(jsonPath("$.cards.length()").value(1))
                .andExpect(jsonPath("$.cards[0].cardId").value("xy1-1"));
    }

    @Test
    void getById_returns404WhenNotFound() throws Exception {
        when(deckService.getById(99L)).thenThrow(new NoSuchElementException("Deck not found: 99"));

        mockMvc.perform(get("/api/decks/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returns201WithCreatedDeck() throws Exception {
        final DeckResponseDTO response = new DeckResponseDTO(
                7L, "New Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID, LocalDateTime.now(), List.of());

        when(deckService.create(any(DeckRequestDTO.class))).thenReturn(response);

        final DeckRequestDTO request = new DeckRequestDTO(1L, "New Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                List.of(new DeckCardRequestDTO("xy1-1", 60)));

        mockMvc.perform(post("/api/decks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("New Deck"));
    }

    @Test
    void create_returns400WhenValidationFails() throws Exception {
        when(deckService.create(any(DeckRequestDTO.class)))
                .thenThrow(new InvalidDeckException("Deck must contain exactly 60 cards, but has 59"));

        final DeckRequestDTO request = new DeckRequestDTO(1L, "Bad Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                List.of(new DeckCardRequestDTO("xy1-1", 59)));

        mockMvc.perform(post("/api/decks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns404WhenUserNotFound() throws Exception {
        when(deckService.create(any(DeckRequestDTO.class)))
                .thenThrow(new NoSuchElementException("User not found: 999"));

        final DeckRequestDTO request = new DeckRequestDTO(999L, "My Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                List.of(new DeckCardRequestDTO("xy1-1", 60)));

        mockMvc.perform(post("/api/decks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cloneTemplate_returns201WithCreatedDeck() throws Exception {
        final DeckResponseDTO response = new DeckResponseDTO(
                10L, "Template (Copia)", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID, LocalDateTime.now(), List.of());

        when(deckService.create(any(DeckRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/decks/users/1/clone/-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Template (Copia)"));
    }
}
