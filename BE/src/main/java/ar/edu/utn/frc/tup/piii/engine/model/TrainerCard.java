package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.Objects;

/**
 * Immutable representation of a Trainer card. FR-003, RF-02b.
 *
 * <p>Trainer cards carry an optional {@link TrainerEffect} strategy that encodes
 * what the card does when played. The effect is invoked by the game engine after
 * the card has been removed from the player's hand and placed in the discard pile.</p>
 */
public final class TrainerCard implements Card {

    private final String cardId;
    private final String name;
    private final TrainerType trainerType;
    private final boolean aceSpec;
    private final String effectText;
    private final TrainerEffectId effectId;
    private final TrainerEffect effect;

    private TrainerCard(final Builder builder) {
        this.cardId = builder.cardId;
        this.name = builder.name;
        this.trainerType = builder.trainerType;
        this.aceSpec = builder.aceSpec;
        this.effectText = builder.effectText;
        this.effectId = builder.effectId;
        this.effect = builder.effect;
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

    public String getEffectText() {
        return effectText;
    }

    public TrainerEffectId getEffectId() {
        return effectId;
    }

    /**
     * Returns the runtime effect of this Trainer card, or {@code null} if no effect is defined.
     * A {@code null} effect means the card's logic is not yet implemented.
     *
     * @return the {@link TrainerEffect} strategy, or {@code null}
     */
    public TrainerEffect getEffect() {
        return effect;
    }

    public static final class Builder {
        private final String cardId;
        private final String name;
        private final TrainerType trainerType;
        private boolean aceSpec = false;
        private String effectText;
        private TrainerEffectId effectId = TrainerEffectId.NONE;
        private TrainerEffect effect;

        public Builder(final String cardId, final String name, final TrainerType trainerType) {
            this.cardId = Objects.requireNonNull(cardId, "cardId must not be null");
            this.name = Objects.requireNonNull(name, "name must not be null");
            this.trainerType = Objects.requireNonNull(trainerType, "trainerType must not be null");
        }

        public Builder aceSpec(final boolean isAceSpec) {
            this.aceSpec = isAceSpec;
            return this;
        }

        public Builder effectText(final String effectText) {
            this.effectText = effectText;
            return this;
        }

        public Builder effectId(final TrainerEffectId effectId) {
            this.effectId = effectId != null ? effectId : TrainerEffectId.NONE;
            return this;
        }

        /**
         * Attaches a runtime {@link TrainerEffect} to this card.
         *
         * @param trainerEffect the effect to invoke when this card is played (may be null)
         * @return this builder
         */
        public Builder effect(final TrainerEffect trainerEffect) {
            this.effect = trainerEffect;
            return this;
        }

        public TrainerCard build() {
            return new TrainerCard(this);
        }
    }
}
