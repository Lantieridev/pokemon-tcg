package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.Objects;

/**
 * Immutable representation of an Energy card. FR-003.
 *
 * <p>Special energies may provide multiple energy units ({@link #getEnergyCount()})
 * or count as every type ({@link #isProvidesAllTypes()}).</p>
 */
public final class EnergyCard implements Card {

    private final String cardId;
    private final String name;
    private final PokemonType energyType;
    private final boolean basic;
    private final int energyCount;
    private final boolean providesAllTypes;

    /**
     * Standard constructor for basic energies (1 unit, single type).
     */
    public EnergyCard(final String cardId,
                      final String name,
                      final PokemonType energyType,
                      final boolean basic) {
        this(cardId, name, energyType, basic, 1, false);
    }

    /**
     * Full constructor for special energies.
     *
     * @param energyCount      how many energy units this card provides (e.g. 2 for Double Colorless)
     * @param providesAllTypes true if this card counts as every type (e.g. Rainbow Energy)
     */
    public EnergyCard(final String cardId,
                      final String name,
                      final PokemonType energyType,
                      final boolean basic,
                      final int energyCount,
                      final boolean providesAllTypes) {
        this.cardId = Objects.requireNonNull(cardId, "cardId must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.energyType = Objects.requireNonNull(energyType, "energyType must not be null");
        this.basic = basic;
        this.energyCount = Math.max(1, energyCount);
        this.providesAllTypes = providesAllTypes;
    }

    @Override
    public String getCardId() {
        return cardId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CardType getCardType() {
        return CardType.ENERGY;
    }

    public PokemonType getEnergyType() {
        return energyType;
    }

    public boolean isBasic() {
        return basic;
    }

    public boolean isSpecial() {
        return !basic;
    }

    /**
     * Returns how many energy units this single card provides.
     * Basic energies return 1. Double Colorless Energy returns 2.
     */
    public int getEnergyCount() {
        return energyCount;
    }

    /**
     * Returns true if this energy counts as every type when satisfying attack costs.
     * Used by Rainbow Energy.
     */
    public boolean isProvidesAllTypes() {
        return providesAllTypes;
    }
}
