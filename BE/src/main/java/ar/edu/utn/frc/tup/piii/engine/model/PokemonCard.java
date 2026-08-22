package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.List;
import java.util.Objects;

/**
 * Immutable data representation of a Pokémon card as read from persistence.
 * Holds all static card properties. Does NOT track mutable battle state
 * (damage counters, attached energies) — that belongs to BattlePokemonState.
 */
@SuppressWarnings("PMD.DataClass") // Immutable domain model representation of a Pokémon card
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
    private final List<Ability> abilities;
    private final List<Attack> attacks;

    private PokemonCard(final Builder builder) {
        this.cardId = builder.cardId;
        this.name = builder.name;
        this.hp = builder.hp;
        this.pokemonType = builder.pokemonType;
        this.weaknessType = builder.weaknessTypeVal;
        this.resistanceType = builder.resistanceTypeVal;
        this.retreatCost = builder.retreatCostVal;
        this.ex = builder.exVal;
        this.evolutionStage = builder.evolutionStageVal;
        this.evolvesFrom = builder.evolvesFromVal;
        this.abilities = List.copyOf(builder.abilitiesVal);
        this.attacks = List.copyOf(builder.attacksVal);
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

    public List<Ability> getAbilities() {
        return abilities;
    }

    public List<Attack> getAttacks() {
        return attacks;
    }

    public static final class Builder {
        private final String cardId;
        private final String name;
        private final int hp;
        private final PokemonType pokemonType;
        private PokemonType weaknessTypeVal;
        private PokemonType resistanceTypeVal;
        private int retreatCostVal;
        private boolean exVal;
        private EvolutionStage evolutionStageVal = EvolutionStage.BASIC;
        private String evolvesFromVal;
        private List<Ability> abilitiesVal = List.of();
        private List<Attack> attacksVal = List.of();

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
            this.weaknessTypeVal = type;
            return this;
        }

        public Builder resistanceType(final PokemonType type) {
            this.resistanceTypeVal = type;
            return this;
        }

        public Builder retreatCost(final int cost) {
            this.retreatCostVal = cost;
            return this;
        }

        public Builder ex(final boolean isEx) {
            this.exVal = isEx;
            return this;
        }

        public Builder evolutionStage(final EvolutionStage stage) {
            this.evolutionStageVal = stage;
            return this;
        }

        public Builder evolvesFrom(final String species) {
            this.evolvesFromVal = species;
            return this;
        }

        public Builder abilities(final List<Ability> abilityList) {
            this.abilitiesVal = Objects.requireNonNull(abilityList, "abilities must not be null");
            return this;
        }

        public Builder attacks(final List<Attack> attackList) {
            this.attacksVal = Objects.requireNonNull(attackList, "attacks must not be null");
            return this;
        }

        public PokemonCard build() {
            return new PokemonCard(this);
        }
    }
}
