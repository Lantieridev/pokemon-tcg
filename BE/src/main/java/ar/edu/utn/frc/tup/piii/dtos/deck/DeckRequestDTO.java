package ar.edu.utn.frc.tup.piii.dtos.deck;

import ar.edu.utn.frc.tup.piii.engine.model.DeckStatus;
import java.util.List;

public record DeckRequestDTO(Long userId, String name, DeckStatus status, List<DeckCardRequestDTO> cards) {}
