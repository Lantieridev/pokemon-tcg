package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Represents a pending interactive request where the client must select cards.
 *
 * @param sourceEffect  the effect that triggered this selection
 * @param target        the primary target of the effect (e.g., the Pokémon to evolve), if any
 * @param maxSelections the maximum number of cards the user can select
 * @param source        the place where the cards are being selected from
 */
public record PendingSelectionRequest(
        TrainerEffectId sourceEffect,
        BattlePokemonState target,
        int maxSelections,
        SelectionSource source
) {
}
