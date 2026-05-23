package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Action representing a player activating a Pokémon Ability during the Main Phase.
 * Abilities are optional once-per-turn activations that do not count as an attack.
 * FR-004.
 *
 * @param source    the Pokémon whose ability is being used
 * @param abilityId the identifier of the ability being activated
 */
public record UseAbilityAction(BattlePokemonState source, String abilityId, Integer sourceIndex, Integer targetIndex, java.util.List<Integer> selectedEnergyIndices) implements Action {
}
