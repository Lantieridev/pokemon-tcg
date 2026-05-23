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
     * Returns the display name of this Pokémon (e.g. "Charmander").
     *
     * @return name, or null if not set
     */
    String getName();

    /**
     * Returns the card identifier for this Pokémon (e.g. "xy1-46").
     *
     * @return cardId, or null if not set
     */
    String getCardId();

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

    /**
     * Returns {@code true} if this Pokémon already has a Pokémon Tool card attached.
     * Used to enforce the one-tool-per-Pokémon rule (FR-003).
     *
     * @return true if a Pokémon Tool is currently attached
     */
    boolean hasToolAttached();

    /**
     * Attaches a Pokémon Tool card to this Pokémon, recording the card reference
     * so the damage pipeline can read its {@link PokemonToolEffectId}.
     * Replaces any previously attached tool.
     *
     * @param tool the tool card to attach (never null)
     */
    void attachTool(TrainerCard tool);

    /**
     * Returns the Pokémon Tool card currently attached to this Pokémon,
     * wrapped in an Optional.
     *
     * @return an Optional containing the attached tool card, or empty if none
     */
    java.util.Optional<TrainerCard> getAttachedTool();

    /**
     * Removes the specified number of energy cards from this Pokémon's attached energies.
     * Any energy type counts toward the cost (as per XY1 retreat rules).
     * If {@code count} is 0, this is a no-op.
     *
     * @param count number of energy cards to remove (must be &gt;= 0)
     */
    void removeEnergies(int count);
    void removeEnergies(java.util.List<Integer> indices);

    /**
     * Heals this Pokémon by reducing damage counters. One counter equals 10 HP.
     * Healing cannot drop counters below zero.
     *
     * @param amount HP to restore (in HP units, not counter units)
     */
    void heal(int amount);

    /**
     * Attaches an energy card to this Pokémon.
     *
     * @param energyCard the energy card to attach (never null)
     */
    void attachEnergy(EnergyCard energyCard);

    /**
     * Detaches the currently attached Pokémon Tool (e.g. when the Pokémon is KO'd
     * and the tool has already been moved to the discard pile).
     */
    void detachTool();

    /**
     * Returns the list of attacks available to this Pokémon.
     *
     * @return attacks (never null; may be empty)
     */
    List<Attack> getAttacks();

    /**
     * Returns the list of abilities available to this Pokémon.
     *
     * @return abilities (never null; may be empty)
     */
    List<Ability> getAbilities();

    /**
     * Marks a specific ability as having been used this turn.
     */
    void markAbilityUsed(String abilityName);

    /**
     * Checks if a specific ability has been used this turn.
     */
    boolean hasUsedAbilityThisTurn(String abilityName);

    /**
     * Resets all ability usage tracking for the current turn.
     */
    void resetAbilitiesUsedThisTurn();

    /**
     * Returns the evolution stage of this Pokémon card.
     *
     * @return evolution stage (never null)
     */
    EvolutionStage getEvolutionStage();

    /**
     * Returns the species name this card evolves FROM, or {@code null} if this is a BASIC Pokémon.
     * Used to validate that the evolution target matches the card's pre-evolution.
     *
     * @return pre-evolution species name, or null for BASIC Pokémon
     */
    String getEvolvesFrom();

    /**
     * Evolves this Pokémon into the new specified card.
     * Retains attached energies and damage counters, but replaces the underlying card stats.
     *
     * @param evolution the new evolution card
     */
    void evolveInto(PokemonCard evolution);

    /**
     * Returns the current underlying Pokémon card (the topmost evolution card).
     * Used by the KO handler to move the card to the discard pile.
     *
     * @return the current card (never null)
     */
    Card getBaseCard();

    /**
     * Returns the list of underlying pre-evolution Pokémon cards.
     * Used by the KO handler to move them to the discard pile.
     *
     * @return list of underlying cards (never null; may be empty)
     */
    List<PokemonCard> getUnderlyingCards();

    /**
     * Returns the list of actual Energy cards currently attached to this Pokémon.
     * Used by the KO handler to move them to the discard pile.
     *
     * @return list of attached energy cards (never null; may be empty)
     */
    List<EnergyCard> getAttachedEnergyCards();
}
