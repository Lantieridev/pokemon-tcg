package ar.edu.utn.frc.tup.piii.dtos.deck;

import java.time.LocalDateTime;

public record DeckSummaryDTO(Long id, String name, LocalDateTime createdAt, int totalCards) {}
