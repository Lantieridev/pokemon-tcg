package ar.edu.utn.frc.tup.piii.dtos;

import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;

import java.util.List;

/**
 * Read-only view of an attack sent to clients.
 *
 * @param name        the attack's display name
 * @param baseDamage  base damage output (0 for effect-only attacks)
 * @param energyCost  list of energy types required to use this attack
 */
public record AttackDTO(
        String name,
        int baseDamage,
        List<PokemonType> energyCost) {
}
