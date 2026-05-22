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
    private boolean toolAttached;

    /**
     * @param card the source card data (never null)
     */
    public InPlayPokemon(final PokemonCard card) {
        this.card = Objects.requireNonNull(card, "card must not be null");
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
    public String getEvolvesFrom() {
        return card.getEvolvesFrom();
    }

    // -------------------------------------------------------------------------
    // BattlePokemonState — mutable battle state
    // -------------------------------------------------------------------------

    @Override
    public void evolveInto(final PokemonCard evolution) {
        Objects.requireNonNull(evolution, "evolution card must not be null");
        this.underlyingCards.add(this.card);
        this.card = evolution;
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
     *
     * @param energyCard the energy card to attach (never null)
     */
    @Override
    public void attachEnergy(final EnergyCard energyCard) {
        Objects.requireNonNull(energyCard, "energy card must not be null");
        attachedEnergyCards.add(energyCard);
        attachedEnergies.add(energyCard.getEnergyType());
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
        return toolAttached;
    }

    /**
     * Sets whether a Pokémon Tool card is attached. Called by the engine when a Tool is
     * played or discarded.
     *
     * @param attached true to mark a tool as attached, false to remove it
     */
    @Override
    public void setToolAttached(final boolean attached) {
        this.toolAttached = attached;
    }
}
