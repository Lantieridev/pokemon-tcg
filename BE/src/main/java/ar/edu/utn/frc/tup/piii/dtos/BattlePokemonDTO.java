package ar.edu.utn.frc.tup.piii.dtos;

import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;

/**
 * Read-only view of a Pokémon's battle state sent to clients.
 *
 * @param pokemonType    the energy/type category of this Pokémon
 * @param maxHp          maximum hit points
 * @param damageCounters current damage counters placed on this Pokémon
 * @param isEx           true if this is an EX Pokémon (worth 2 prize cards)
 * @param weaknessType   the type this Pokémon is weak to, or null if none
 * @param resistanceType the type this Pokémon is resistant to, or null if none
 */
public record BattlePokemonDTO(
        PokemonType pokemonType,
        int maxHp,
        int damageCounters,
        boolean isEx,
        PokemonType weaknessType,
        PokemonType resistanceType) {
}
