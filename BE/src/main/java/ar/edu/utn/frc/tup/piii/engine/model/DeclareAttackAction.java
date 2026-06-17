package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.List;
import java.util.Collections;

/**
 * Action representing a player declaring an attack. FR-004.
 *
 * @param attacker the BattlePokemonState of the attacking Pokémon
 * @param attack   the attack being declared
 * @param selectedCardIds additional cards selected for the attack (e.g. optional energy discard)
 */
public record DeclareAttackAction(BattlePokemonState attacker, Attack attack, List<String> selectedCardIds) implements Action {
    public DeclareAttackAction(BattlePokemonState attacker, Attack attack) {
        this(attacker, attack, Collections.emptyList());
    }
}
