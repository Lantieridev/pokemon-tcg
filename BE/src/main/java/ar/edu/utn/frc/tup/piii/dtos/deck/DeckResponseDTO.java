package ar.edu.utn.frc.tup.piii.dtos.deck;

import ar.edu.utn.frc.tup.piii.engine.model.DeckStatus;
import java.time.LocalDateTime;
import java.util.List;

public record DeckResponseDTO(Long id, String name, DeckStatus status, LocalDateTime createdAt, List<DeckCardResponseDTO> cards) {}
