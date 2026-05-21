package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.Objects;

/**
 * Immutable representation of a Trainer card. FR-003, RF-02b.
 */
public final class TrainerCard implements Card {

    private final String cardId;
    private final String name;
    private final TrainerType trainerType;
    private final boolean aceSpec;

    private TrainerCard(final Builder builder) {
        this.cardId = builder.cardId;
        this.name = builder.name;
        this.trainerType = builder.trainerType;
        this.aceSpec = builder.aceSpec;
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
        return CardType.TRAINER;
    }

    @Override
    public boolean isAceSpec() {
        return aceSpec;
    }

    public TrainerType getTrainerType() {
        return trainerType;
    }

    public static final class Builder {
        private final String cardId;
        private final String name;
        private final TrainerType trainerType;
        private boolean aceSpec = false;

        public Builder(final String cardId, final String name, final TrainerType trainerType) {
            this.cardId = Objects.requireNonNull(cardId, "cardId must not be null");
            this.name = Objects.requireNonNull(name, "name must not be null");
            this.trainerType = Objects.requireNonNull(trainerType, "trainerType must not be null");
        }

        public Builder aceSpec(final boolean isAceSpec) {
            this.aceSpec = isAceSpec;
            return this;
        }

        public TrainerCard build() {
            return new TrainerCard(this);
        }
    }
}
