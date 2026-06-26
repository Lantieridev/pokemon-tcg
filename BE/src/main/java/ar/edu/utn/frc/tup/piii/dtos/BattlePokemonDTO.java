package ar.edu.utn.frc.tup.piii.dtos;

import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;

import java.util.List;

/**
 * Read-only view of a Pokémon's battle state sent to clients.
 *
 * @param cardId          card identifier (e.g. "xy1-46"), or null if unknown
 * @param name            display name (e.g. "Charmander"), or null if unknown
 * @param pokemonType     the energy/type category of this Pokémon
 * @param maxHp           maximum hit points
 * @param damageCounters  current damage counters placed on this Pokémon
 * @param isEx            true if this is an EX Pokémon (worth 2 prize cards)
 * @param weaknessType    the type this Pokémon is weak to, or null if none
 * @param resistanceType  the type this Pokémon is resistant to, or null if none
 * @param attachedEnergies energy types currently attached to this Pokémon
 * @param retreatCost     number of energy cards required to retreat
 * @param hasToolAttached true if a Pokémon Tool card is currently attached
 * @param attachedToolCardId  card ID of the attached Pokémon Tool, or null if none
 * @param attacks         list of available attacks for this Pokémon
 */
public record BattlePokemonDTO(
        String cardId,
        String name,
        PokemonType pokemonType,
        int maxHp,
        int damageCounters,
        boolean isEx,
        PokemonType weaknessType,
        PokemonType resistanceType,
        List<PokemonType> attachedEnergies,
        int retreatCost,
        boolean hasToolAttached,
        String attachedToolCardId,
        List<AttackDTO> attacks,
        List<String> statusConditions,
        List<String> attachedEnergyCardIds) {
}

