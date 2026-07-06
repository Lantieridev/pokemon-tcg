package ar.edu.utn.frc.tup.piii.dtos.deck;

import ar.edu.utn.frc.tup.piii.engine.model.DeckStatus;
import java.util.List;

/**
 * Owner is never taken from this DTO — the server derives it from the
 * authenticated principal so a client can never act on another user's deck.
 */
public record DeckRequestDTO(String name, DeckStatus status, List<DeckCardRequestDTO> cards) {}
