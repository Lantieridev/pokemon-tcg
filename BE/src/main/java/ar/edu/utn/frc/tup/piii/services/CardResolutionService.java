package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.engine.model.Card;

import java.util.List;

/**
 * Port — resolves a saved deck (by ID) into an ordered list of {@link Card} objects
 * ready for the engine. Implementations are provided per module:
 *
 * <ul>
 *   <li>{@code StubCardResolutionService} — minimal in-memory stub used before Module 4 is wired.</li>
 *   <li>{@code JpaCardResolutionService} — reads from PostgreSQL (Module 4).</li>
 * </ul>
 *
 * <p>Pure port interface — zero Spring annotations here.</p>
 */
public interface CardResolutionService {

    /**
     * Returns the full 60-card list for the given deck, ordered for play.
     *
     * @param deckId the persisted deck identifier
     * @return list of exactly 60 cards (never null)
     * @throws IllegalArgumentException if the deck is not found or invalid
     */
    List<Card> resolveCards(Long deckId);
}
