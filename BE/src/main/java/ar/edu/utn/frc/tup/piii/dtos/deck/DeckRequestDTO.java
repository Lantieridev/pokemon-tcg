package ar.edu.utn.frc.tup.piii.dtos.deck;

import java.util.List;

public record DeckRequestDTO(Long userId, String name, List<DeckCardRequestDTO> cards) {}
