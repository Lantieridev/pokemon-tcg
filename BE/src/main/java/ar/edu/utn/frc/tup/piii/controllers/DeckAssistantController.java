package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.SmartDeckRequestDTO;
import ar.edu.utn.frc.tup.piii.services.deck.DeckAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/decks/assistant")
public class DeckAssistantController {

    private final DeckAssistantService assistantService;

    public DeckAssistantController(DeckAssistantService assistantService) {
        this.assistantService = Objects.requireNonNull(assistantService);
    }

    @PostMapping("/autocomplete")
    public ResponseEntity<List<DeckCardRequestDTO>> autocomplete(@RequestBody List<DeckCardRequestDTO> currentCards) {
        return ResponseEntity.ok(assistantService.autocomplete(currentCards));
    }

    @PostMapping("/suggestions")
    public ResponseEntity<List<DeckCardResponseDTO>> getSuggestions(@RequestBody List<DeckCardRequestDTO> currentCards) {
        return ResponseEntity.ok(assistantService.getSuggestions(currentCards));
    }

    @PostMapping("/wizard")
    public ResponseEntity<List<DeckCardRequestDTO>> generateWizardDeck(@RequestBody SmartDeckRequestDTO request) {
        return ResponseEntity.ok(assistantService.generateWizardDeck(request));
    }
}
