package ar.edu.utn.frc.tup.piii.dtos.deck;

import java.time.LocalDateTime;
import java.util.List;

public record DeckResponseDTO(Long id, String name, LocalDateTime createdAt, List<DeckCardResponseDTO> cards) {}
