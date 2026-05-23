package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Action sent by the defending player immediately after their Active Pokémon is knocked out.
 * The player must promote one benched Pokémon to the Active position before play continues.
 *
 * <p>Per XY1 Rulebook §2: when the Active Pokémon is Knocked Out, the owner must immediately
 * put a Benched Pokémon into the Active position. This action pauses normal phase progression
 * (between-turns processing, next turn start) until the promotion is resolved.</p>
 *
 * <p>Pure POJO — zero Spring imports. FR-020.</p>
 *
 * @param benchIndex zero-based index of the benched Pokémon to promote (must be ≥ 0)
 */
public record PromoteActiveAction(int benchIndex) implements Action {
}
