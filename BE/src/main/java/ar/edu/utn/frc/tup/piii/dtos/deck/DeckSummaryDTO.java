package ar.edu.utn.frc.tup.piii.dtos.deck;

import ar.edu.utn.frc.tup.piii.engine.model.DeckStatus;
import java.time.LocalDateTime;

public record DeckSummaryDTO(Long id, String name, DeckStatus status, LocalDateTime createdAt, int totalCards) {}
