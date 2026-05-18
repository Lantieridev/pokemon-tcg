package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.List;

/**
 * Extended state view of the active Pokémon needed by damage calculation (§3) and
 * knockout detection (§4/§5). Extends {@link ActivePokemonState} with type, HP cap,
 * weakness/resistance, and EX flag. FR-002.
 *
 * <p>Null return values for {@link #getWeaknessType()} and {@link #getResistanceType()}
 * signal the absence of a weakness or resistance — no sentinel enum value is used.</p>
 */
public interface BattlePokemonState extends ActivePokemonState {

    /**
     * Returns the maximum HP of this Pokémon. Always positive.
     *
     * @return max HP
     */
    int getMaxHp();

    /**
     * Returns the energy type of this Pokémon, used as the attacking type in §3.
     *
     * @return pokemon type (never null)
     */
    PokemonType getPokemonType();

    /**
     * Returns the type this Pokémon is weak to, or {@code null} if it has no weakness.
     *
     * @return weakness type, or null
     */
    PokemonType getWeaknessType();

    /**
     * Returns the type this Pokémon is resistant to, or {@code null} if it has no resistance.
     *
     * @return resistance type, or null
     */
    PokemonType getResistanceType();

    /**
     * Returns {@code true} if this is an EX Pokémon (worth 2 prize cards when knocked out).
     *
     * @return true if EX
     */
    boolean isEx();

    /**
     * Returns the number of Energy cards required to retreat this Pokémon.
     *
     * @return retreat cost (always &gt;= 0)
     */
    int getRetreatCost();

    /**
     * Returns the list of energy types currently attached to this Pokémon, in attachment order.
     *
     * @return attached energies (never null; may be empty)
     */
    List<PokemonType> getAttachedEnergies();
}
