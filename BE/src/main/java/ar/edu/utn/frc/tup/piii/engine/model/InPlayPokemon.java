package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Concrete, mutable battle representation of a Pokémon card that has been played onto the field.
 * Delegates all static card data to the wrapped {@link PokemonCard} and maintains mutable
 * battle state (damage counters, attached energies, tool) independently.
 *
 * <p>This is the sole concrete implementation of {@link BattlePokemonState} in the engine.</p>
 */
public final class InPlayPokemon implements BattlePokemonState {

    private static final int DAMAGE_PER_COUNTER = 10;

    private PokemonCard card;
    private int damageCounters;
    private final List<PokemonCard> underlyingCards = new ArrayList<>();
    private final List<EnergyCard> attachedEnergyCards = new ArrayList<>();
    private final List<PokemonType> attachedEnergies = new ArrayList<>();
    private final java.util.Set<String> usedAbilitiesThisTurn = new java.util.HashSet<>();
    private TrainerCard attachedTool;

    /**
     * @param card the source card data (never null)
     */
    public InPlayPokemon(final PokemonCard card) {
        this.card = Objects.requireNonNull(card, "card must not be null");
    }

    /**
     * Comprehensive constructor for deserialization.
     */
    public InPlayPokemon(final PokemonCard card, final int damageCounters, final List<PokemonType> attachedEnergies, final TrainerCard attachedTool) {
        this.card = Objects.requireNonNull(card, "card must not be null");
        this.damageCounters = damageCounters;
        if (attachedEnergies != null) {
            this.attachedEnergies.addAll(attachedEnergies);
        }
        this.attachedTool = attachedTool;
    }

    /**
     * Expose the wrapped PokemonCard.
     */
    public PokemonCard getCard() {
        return card;
    }

    // -------------------------------------------------------------------------
    // ActivePokemonState
    // -------------------------------------------------------------------------

    @Override
    public int getDamageCounters() {
        return damageCounters;
    }

    @Override
    public void addDamageCounters(final int amount) {
        damageCounters += amount;
    }

    // -------------------------------------------------------------------------
    // BattlePokemonState — static data delegated to card
    // -------------------------------------------------------------------------

    @Override
    public String getCardId() {
        return card.getCardId();
    }

    @Override
    public String getName() {
        return card.getName();
    }

    public CardType getCardType() {
        return card.getCardType();
    }

    @Override
    public int getMaxHp() {
        return card.getHp();
    }

    @Override
    public PokemonType getPokemonType() {
        return card.getPokemonType();
    }

    @Override
    public PokemonType getWeaknessType() {
        return card.getWeaknessType();
    }

    @Override
    public PokemonType getResistanceType() {
        return card.getResistanceType();
    }

    @Override
    public boolean isEx() {
        return card.isEx();
    }

    @Override
    public int getRetreatCost() {
        return card.getRetreatCost();
    }

    @Override
    public List<Attack> getAttacks() {
        return card.getAttacks();
    }

    @Override
    public EvolutionStage getEvolutionStage() {
        return card.getEvolutionStage();
    }

    @Override
    public List<Ability> getAbilities() {
        return card.getAbilities();
    }

    @Override
    public String getEvolvesFrom() {
        return card.getEvolvesFrom();
    }

    @Override
    public void markAbilityUsed(final String abilityName) {
        if (abilityName != null) {
            usedAbilitiesThisTurn.add(abilityName);
        }
    }

    @Override
    public boolean hasUsedAbilityThisTurn(final String abilityName) {
        return usedAbilitiesThisTurn.contains(abilityName);
    }

    @Override
    public void resetAbilitiesUsedThisTurn() {
        usedAbilitiesThisTurn.clear();
    }

    // -------------------------------------------------------------------------
    // BattlePokemonState — mutable battle state
    // -------------------------------------------------------------------------

    @Override
    public void evolveInto(final PokemonCard evolution) {
        Objects.requireNonNull(evolution, "evolution card must not be null");
        this.underlyingCards.add(this.card);
        this.card = evolution;
        this.usedAbilitiesThisTurn.clear();
    }

    /**
     * Returns the current underlying Pokémon card (the topmost evolution).
     *
     * @return current card (never null)
     */
    @Override
    public Card getBaseCard() {
        return card;
    }

    @Override
    public List<PokemonType> getAttachedEnergies() {
        return Collections.unmodifiableList(attachedEnergies);
    }

    @Override
    public List<PokemonCard> getUnderlyingCards() {
        return Collections.unmodifiableList(underlyingCards);
    }

    @Override
    public List<EnergyCard> getAttachedEnergyCards() {
        return Collections.unmodifiableList(attachedEnergyCards);
    }

    /**
     * Attaches an energy card to this Pokémon.
     * Special energies may provide multiple energy units (e.g. Double Colorless)
     * or count as all types (e.g. Rainbow Energy).
     *
     * @param energyCard the energy card to attach (never null)
     */
    @Override
    public void attachEnergy(final EnergyCard energyCard) {
        Objects.requireNonNull(energyCard, "energy card must not be null");
        attachedEnergyCards.add(energyCard);
        for (int i = 0; i < energyCard.getEnergyCount(); i++) {
            attachedEnergies.add(energyCard.getEnergyType());
        }
    }

    @Override
    public void heal(final int amount) {
        damageCounters = Math.max(0, damageCounters - amount / DAMAGE_PER_COUNTER);
    }

    @Override
    public void removeEnergies(final int count) {
        for (int i = 0; i < count && !attachedEnergies.isEmpty(); i++) {
            attachedEnergies.remove(attachedEnergies.size() - 1);
            attachedEnergyCards.remove(attachedEnergyCards.size() - 1);
        }
    }

    @Override
    public void removeEnergies(final java.util.List<Integer> indices) {
        java.util.List<Integer> sortedIndices = new java.util.ArrayList<>(indices);
        sortedIndices.sort(java.util.Collections.reverseOrder());
        for (int index : sortedIndices) {
            if (index >= 0 && index < attachedEnergies.size()) {
                attachedEnergies.remove(index);
                attachedEnergyCards.remove(index);
            }
        }
    }

    @Override
    public boolean hasToolAttached() {
        return attachedTool != null;
    }

    /**
     * Attaches a Pokémon Tool card to this Pokémon. Replaces any previously attached tool.
     * Callers are responsible for discarding the old tool before calling this method.
     *
     * @param tool the tool card to attach (never null)
     */
    @Override
    public void attachTool(final TrainerCard tool) {
        this.attachedTool = Objects.requireNonNull(tool, "tool must not be null");
    }

    /**
     * Returns the Pokémon Tool card currently attached to this Pokémon, wrapped in an Optional.
     *
     * @return an Optional containing the attached tool, or empty if none
     */
    @Override
    public java.util.Optional<TrainerCard> getAttachedTool() {
        return java.util.Optional.ofNullable(attachedTool);
    }

    /**
     * Detaches the currently attached Pokémon Tool (called after it has been moved
     * to the discard pile by the KO handler).
     */
    @Override
    public void detachTool() {
        this.attachedTool = null;
    }
}
