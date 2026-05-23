package ar.edu.utn.frc.tup.piii.engine.listener;

import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;

/**
 * Read-only view of the currently active Stadium card on the field.
 * Implemented by {@code MatchBoard}; injected into engine components that need
 * to apply Stadium-based modifiers without depending on the full board object.
 *
 * <p>Pure engine interface — zero Spring imports. FR-002d.</p>
 */
@FunctionalInterface
public interface StadiumStateProvider {

    /**
     * Returns the Stadium card currently in play, or {@code null} if no Stadium is active.
     *
     * @return active Stadium, or null
     */
    TrainerCard getActiveStadium();
}
