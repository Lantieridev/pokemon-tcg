package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.List;

/**
 * Action triggered by the client to resolve a pending interactive selection.
 *
 * @param cardIds the list of selected card IDs
 */
public record SelectCardsAction(List<String> cardIds, PendingSelectionRequest request) implements Action {
}
