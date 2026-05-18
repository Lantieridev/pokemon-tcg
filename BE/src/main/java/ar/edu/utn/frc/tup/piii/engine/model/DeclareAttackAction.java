package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Action representing a player declaring an attack. FR-004.
 *
 * @param attacker the BattlePokemonState of the attacking Pokémon
 * @param attack   the attack being declared
 */
public record DeclareAttackAction(BattlePokemonState attacker, Attack attack) implements Action {
}
