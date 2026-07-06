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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/decks")
public final class DeckController {

    private final DeckService deckService;
    private final ar.edu.utn.frc.tup.piii.services.deck.DeckTemplateService templateService;

    public DeckController(final DeckService deckService, final ar.edu.utn.frc.tup.piii.services.deck.DeckTemplateService templateService) {
        this.deckService = Objects.requireNonNull(deckService);
        this.templateService = Objects.requireNonNull(templateService);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<DeckSummaryDTO>> getMine(final Principal principal) {
        return ResponseEntity.ok(deckService.getByUsername(principal.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeckResponseDTO> getById(@PathVariable final Long id, final Principal principal) {
        return ResponseEntity.ok(deckService.getById(id, principal.getName()));
    }

    @GetMapping("/templates")
    public ResponseEntity<List<DeckSummaryDTO>> getTemplates() {
        return ResponseEntity.ok(templateService.getTemplates());
    }

    @PostMapping("/clone/{templateId}")
    public ResponseEntity<DeckResponseDTO> cloneTemplate(@PathVariable final Long templateId, final Principal principal) {
        final DeckResponseDTO template = templateService.getTemplateById(templateId);
        final List<ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO> cards = template.cards().stream()
                .map(c -> new ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO(c.cardId(), c.quantity()))
                .toList();

        final DeckRequestDTO request = new DeckRequestDTO(
                template.name() + " (Copia)",
                ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                cards
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(deckService.create(request, principal.getName()));
    }

    @PostMapping
    public ResponseEntity<DeckResponseDTO> create(@RequestBody final DeckRequestDTO request, final Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deckService.create(request, principal.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeckResponseDTO> update(@PathVariable final Long id, @RequestBody final DeckRequestDTO request,
                                                   final Principal principal) {
        return ResponseEntity.ok(deckService.update(id, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final Long id, final Principal principal) {
        deckService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
