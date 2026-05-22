package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckSummaryDTO;
import ar.edu.utn.frc.tup.piii.services.deck.DeckService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/decks")
public final class DeckController {

    private final DeckService deckService;

    public DeckController(final DeckService deckService) {
        this.deckService = Objects.requireNonNull(deckService);
    }

    @GetMapping
    public ResponseEntity<List<DeckSummaryDTO>> getAll() {
        return ResponseEntity.ok(deckService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeckResponseDTO> getById(@PathVariable final Long id) {
        return ResponseEntity.ok(deckService.getById(id));
    }

    @PostMapping
    public ResponseEntity<DeckResponseDTO> create(@RequestBody final DeckRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deckService.create(request));
    }
}
