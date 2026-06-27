package ar.edu.utn.frc.tup.piii.dtos;

/**
 * Read-only view of a Pokémon Ability sent to clients.
 *
 * @param name  the ability's display name (e.g. "Leaf Draw")
 * @param text  the ability's rules text
 */
public record AbilityDTO(String name, String text) {
}
