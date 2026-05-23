package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.model.Ability;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test helper — in-memory implementation of BattlePokemonState.
 * Mutable damage counters; all other fields are final and set via constructor.
 */
public class FakeBattlePokemonState implements BattlePokemonState {

    private int damageCounters;
    private final int maxHp;
    private final PokemonType type;
    private String name;
    private String cardId;
    private final PokemonType weaknessType;
    private final PokemonType resistanceType;
    private final boolean ex;
    private int retreatCost = 0;
    private final List<PokemonCard> underlyingCards = new ArrayList<>();
    private final List<EnergyCard> attachedEnergyCards = new ArrayList<>();
    private final List<PokemonType> attachedEnergies = new ArrayList<>();
    private TrainerCard attachedTool = null;
    private final List<Attack> attacks = new ArrayList<>();
    private EvolutionStage evolutionStage = EvolutionStage.BASIC;
    private String evolvesFrom = null;
    private List<Ability> abilities = new ArrayList<>();

    public FakeBattlePokemonState(final int maxHp, final PokemonType type,
                                  final PokemonType weaknessType,
                                  final PokemonType resistanceType,
                                  final boolean ex) {
        this.maxHp = maxHp;
        this.type = type;
        this.weaknessType = weaknessType;
        this.resistanceType = resistanceType;
        this.ex = ex;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setCardId(final String cardId) {
        this.cardId = cardId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCardId() {
        return cardId;
    }

    @Override
    public void evolveInto(ar.edu.utn.frc.tup.piii.engine.model.PokemonCard evolutionCard) {
        this.evolutionStage = evolutionCard.getEvolutionStage();
        this.evolvesFrom = evolutionCard.getEvolvesFrom();
        this.name = evolutionCard.getName();
    }

    @Override
    public int getDamageCounters() {
        return damageCounters;
    }

    @Override
    public void addDamageCounters(final int amount) {
        damageCounters += amount;
    }

    @Override
    public int getMaxHp() {
        return maxHp;
    }

    @Override
    public PokemonType getPokemonType() {
        return type;
    }

    @Override
    public PokemonType getWeaknessType() {
        return weaknessType;
    }

    @Override
    public PokemonType getResistanceType() {
        return resistanceType;
    }

    @Override
    public boolean isEx() {
        return ex;
    }

    public void setRetreatCost(final int cost) {
        this.retreatCost = cost;
    }

    public void addAttachedEnergy(final PokemonType pokemonType) {
        attachedEnergies.add(pokemonType);
        attachedEnergyCards.add(new EnergyCard("fake_energy", "Fake Energy", pokemonType, true));
    }

    @Override
    public void attachEnergy(final EnergyCard energyCard) {
        attachedEnergyCards.add(energyCard);
        for (int i = 0; i < energyCard.getEnergyCount(); i++) {
            attachedEnergies.add(energyCard.getEnergyType());
        }
    }

    @Override
    public int getRetreatCost() {
        return retreatCost;
    }

    @Override
    public List<PokemonType> getAttachedEnergies() {
        return List.copyOf(attachedEnergies);
    }

    @Override
    public void attachTool(final TrainerCard tool) {
        this.attachedTool = tool;
    }

    @Override
    public java.util.Optional<TrainerCard> getAttachedTool() {
        return java.util.Optional.ofNullable(attachedTool);
    }

    @Override
    public void detachTool() {
        this.attachedTool = null;
    }

    @Override
    public boolean hasToolAttached() {
        return attachedTool != null;
    }

    @Override
    public void heal(final int amount) {
        damageCounters = Math.max(0, damageCounters - amount / 10);
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

    public void addAttack(final Attack attack) {
        attacks.add(attack);
    }

    @Override
    public List<Attack> getAttacks() {
        return List.copyOf(attacks);
    }

    public void setEvolutionStage(final EvolutionStage stage) {
        this.evolutionStage = stage;
    }

    public void setAbilities(final List<Ability> abilities) {
        this.abilities = new ArrayList<>(abilities);
    }

    @Override
    public List<Ability> getAbilities() {
        return List.copyOf(abilities);
    }

    @Override
    public EvolutionStage getEvolutionStage() {
        return evolutionStage;
    }

    public void setEvolvesFrom(final String species) {
        this.evolvesFrom = species;
    }

    @Override
    public String getEvolvesFrom() {
        return evolvesFrom;
    }

    @Override
    public Card getBaseCard() {
        return null;
    }

    @Override
    public List<PokemonCard> getUnderlyingCards() {
        return underlyingCards;
    }

    @Override
    public List<EnergyCard> getAttachedEnergyCards() {
        return attachedEnergyCards;
    }

    @Override
    public boolean hasUsedAbilityThisTurn(String abilityName) {
        return false;
    }

    @Override
    public void markAbilityUsed(String abilityName) {
    }
    
    @Override
    public void resetAbilitiesUsedThisTurn() {
    }
}
