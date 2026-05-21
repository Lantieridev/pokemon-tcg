package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.List;
import java.util.Objects;

/**
 * Immutable data representation of a Pokémon card as read from persistence.
 * Holds all static card properties. Does NOT track mutable battle state
 * (damage counters, attached energies) — that belongs to BattlePokemonState.
 */
public final class PokemonCard implements Card {

    private final String cardId;
    private final String name;
    private final int hp;
    private final PokemonType pokemonType;
    private final PokemonType weaknessType;
    private final PokemonType resistanceType;
    private final int retreatCost;
    private final boolean ex;
    private final EvolutionStage evolutionStage;
    private final String evolvesFrom;
    private final List<Attack> attacks;

    private PokemonCard(final Builder builder) {
        this.cardId = builder.cardId;
        this.name = builder.name;
        this.hp = builder.hp;
        this.pokemonType = builder.pokemonType;
        this.weaknessType = builder.weaknessType;
        this.resistanceType = builder.resistanceType;
        this.retreatCost = builder.retreatCost;
        this.ex = builder.ex;
        this.evolutionStage = builder.evolutionStage;
        this.evolvesFrom = builder.evolvesFrom;
        this.attacks = List.copyOf(builder.attacks);
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
        return CardType.POKEMON;
    }

    @Override
    public boolean isBasicPokemon() {
        return evolutionStage == EvolutionStage.BASIC;
    }

    public int getHp() {
        return hp;
    }

    public PokemonType getPokemonType() {
        return pokemonType;
    }

    /** Returns the type this Pokémon is weak to, or null if it has no weakness. */
    public PokemonType getWeaknessType() {
        return weaknessType;
    }

    /** Returns the type this Pokémon is resistant to, or null if it has no resistance. */
    public PokemonType getResistanceType() {
        return resistanceType;
    }

    public int getRetreatCost() {
        return retreatCost;
    }

    public boolean isEx() {
        return ex;
    }

    public EvolutionStage getEvolutionStage() {
        return evolutionStage;
    }

    /** Returns the species name this card evolves from, or null for Basic Pokémon. */
    public String getEvolvesFrom() {
        return evolvesFrom;
    }

    public List<Attack> getAttacks() {
        return attacks;
    }

    public static final class Builder {
        private final String cardId;
        private final String name;
        private final int hp;
        private final PokemonType pokemonType;
        private PokemonType weaknessType;
        private PokemonType resistanceType;
        private int retreatCost;
        private boolean ex;
        private EvolutionStage evolutionStage = EvolutionStage.BASIC;
        private String evolvesFrom;
        private List<Attack> attacks = List.of();

        public Builder(final String cardId,
                       final String name,
                       final int hp,
                       final PokemonType pokemonType) {
            this.cardId = Objects.requireNonNull(cardId, "cardId must not be null");
            this.name = Objects.requireNonNull(name, "name must not be null");
            this.pokemonType = Objects.requireNonNull(pokemonType, "pokemonType must not be null");
            this.hp = hp;
        }

        public Builder weaknessType(final PokemonType type) {
            this.weaknessType = type;
            return this;
        }

        public Builder resistanceType(final PokemonType type) {
            this.resistanceType = type;
            return this;
        }

        public Builder retreatCost(final int cost) {
            this.retreatCost = cost;
            return this;
        }

        public Builder ex(final boolean isEx) {
            this.ex = isEx;
            return this;
        }

        public Builder evolutionStage(final EvolutionStage stage) {
            this.evolutionStage = stage;
            return this;
        }

        public Builder evolvesFrom(final String species) {
            this.evolvesFrom = species;
            return this;
        }

        public Builder attacks(final List<Attack> attackList) {
            this.attacks = Objects.requireNonNull(attackList, "attacks must not be null");
            return this;
        }

        public PokemonCard build() {
            return new PokemonCard(this);
        }
    }
}
