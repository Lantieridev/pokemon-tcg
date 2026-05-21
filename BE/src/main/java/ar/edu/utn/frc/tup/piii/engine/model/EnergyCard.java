package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.Objects;

/**
 * Immutable representation of an Energy card. FR-003.
 */
public final class EnergyCard implements Card {

    private final String cardId;
    private final String name;
    private final PokemonType energyType;
    private final boolean basic;

    public EnergyCard(final String cardId,
                      final String name,
                      final PokemonType energyType,
                      final boolean basic) {
        this.cardId = Objects.requireNonNull(cardId, "cardId must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.energyType = Objects.requireNonNull(energyType, "energyType must not be null");
        this.basic = basic;
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
}
