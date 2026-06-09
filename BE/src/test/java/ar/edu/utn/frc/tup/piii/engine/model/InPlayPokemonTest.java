package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InPlayPokemonTest {

    private PokemonCard pikachu() {
        return new PokemonCard.Builder("xy1-42", "Pikachu", 60, PokemonType.LIGHTNING)
                .weaknessType(PokemonType.FIGHTING)
                .resistanceType(PokemonType.METAL)
                .retreatCost(1)
                .ex(false)
                .evolutionStage(EvolutionStage.BASIC)
                .attacks(List.of(new Attack("Nuzzle", 0, List.of(PokemonType.COLORLESS))))
                .build();
    }

    private PokemonCard venusaurEx() {
        return new PokemonCard.Builder("xy1-1", "Venusaur-EX", 180, PokemonType.GRASS)
                .weaknessType(PokemonType.FIRE)
                .ex(true)
                .evolutionStage(EvolutionStage.BASIC)
                .build();
    }

    private PokemonCard raichu() {
        return new PokemonCard.Builder("xy1-43", "Raichu", 90, PokemonType.LIGHTNING)
                .weaknessType(PokemonType.FIGHTING)
                .retreatCost(0)
                .evolutionStage(EvolutionStage.STAGE_1)
                .evolvesFrom("Pikachu")
                .attacks(List.of(new Attack("Thunderbolt", 100,
                        List.of(PokemonType.LIGHTNING, PokemonType.LIGHTNING, PokemonType.COLORLESS))))
                .build();
    }

    @Test
    void shouldThrowWhenCardIsNull() {
        assertThrows(NullPointerException.class, () -> new InPlayPokemon(null));
    }

    @Test
    void shouldDelegateCardId() {
        assertEquals("xy1-42", new InPlayPokemon(pikachu()).getCardId());
    }

    @Test
    void shouldDelegateName() {
        assertEquals("Pikachu", new InPlayPokemon(pikachu()).getName());
    }

    @Test
    void shouldReturnCardTypePokemon() {
        assertEquals(CardType.POKEMON, new InPlayPokemon(pikachu()).getCardType());
    }

    @Test
    void shouldDelegateMaxHp() {
        assertEquals(60, new InPlayPokemon(pikachu()).getMaxHp());
    }

    @Test
    void shouldDelegatePokemonType() {
        assertEquals(PokemonType.LIGHTNING, new InPlayPokemon(pikachu()).getPokemonType());
    }

    @Test
    void shouldDelegateWeaknessType() {
        assertEquals(PokemonType.FIGHTING, new InPlayPokemon(pikachu()).getWeaknessType());
    }

    @Test
    void shouldDelegateResistanceType() {
        assertEquals(PokemonType.METAL, new InPlayPokemon(pikachu()).getResistanceType());
    }

    @Test
    void shouldReturnNullResistanceWhenNotSet() {
        assertNull(new InPlayPokemon(venusaurEx()).getResistanceType());
    }

    @Test
    void shouldDelegateRetreatCost() {
        assertEquals(1, new InPlayPokemon(pikachu()).getRetreatCost());
    }

    @Test
    void shouldDelegateIsEx() {
        assertTrue(new InPlayPokemon(venusaurEx()).isEx());
        assertFalse(new InPlayPokemon(pikachu()).isEx());
    }

    @Test
    void shouldDelegateEvolutionStage() {
        assertEquals(EvolutionStage.BASIC, new InPlayPokemon(pikachu()).getEvolutionStage());
        assertEquals(EvolutionStage.STAGE_1, new InPlayPokemon(raichu()).getEvolutionStage());
    }

    @Test
    void shouldDelegateEvolvesFrom() {
        assertNull(new InPlayPokemon(pikachu()).getEvolvesFrom());
        assertEquals("Pikachu", new InPlayPokemon(raichu()).getEvolvesFrom());
    }

    @Test
    void shouldDelegateAttacks() {
        assertEquals(1, new InPlayPokemon(pikachu()).getAttacks().size());
        assertEquals("Nuzzle", new InPlayPokemon(pikachu()).getAttacks().get(0).name());
    }

    @Test
    void shouldStartWithZeroDamageCounters() {
        assertEquals(0, new InPlayPokemon(pikachu()).getDamageCounters());
    }

    @Test
    void addDamageCountersShouldAccumulate() {
        final InPlayPokemon p = new InPlayPokemon(pikachu());
        p.addDamageCounters(10);
        p.addDamageCounters(20);
        assertEquals(30, p.getDamageCounters());
    }

    @Test
    void shouldStartWithNoAttachedEnergies() {
        assertTrue(new InPlayPokemon(pikachu()).getAttachedEnergies().isEmpty());
    }

    @Test
    void attachedEnergiesListShouldBeImmutable() {
        assertThrows(UnsupportedOperationException.class,
                () -> new InPlayPokemon(pikachu()).getAttachedEnergies().add(PokemonType.LIGHTNING));
    }

    @Test
    void removeEnergiesShouldReduceAttachedList() {
        final InPlayPokemon p = new InPlayPokemon(pikachu());
        p.attachEnergy(new EnergyCard("e1", "Energy", PokemonType.LIGHTNING, true));
        p.attachEnergy(new EnergyCard("e2", "Energy", PokemonType.COLORLESS, true));
        p.removeEnergies(1);
        assertEquals(1, p.getAttachedEnergies().size());
        assertEquals(1, p.getAttachedEnergyCards().size());
    }

    @Test
    void removeSpecialEnergyDiscardEntireCard() {
        final InPlayPokemon p = new InPlayPokemon(pikachu());
        p.attachEnergy(new EnergyCard("e1", "Energy", PokemonType.LIGHTNING, true));
        p.attachEnergy(new EnergyCard("dce", "Double Colorless Energy", PokemonType.COLORLESS, false, 2, false));
        
        assertEquals(3, p.getAttachedEnergies().size());
        assertEquals(2, p.getAttachedEnergyCards().size());
        
        p.removeEnergies(List.of(1));
        
        assertEquals(1, p.getAttachedEnergies().size());
        assertEquals(PokemonType.LIGHTNING, p.getAttachedEnergies().get(0));
        assertEquals(1, p.getAttachedEnergyCards().size());
        assertEquals("e1", p.getAttachedEnergyCards().get(0).getCardId());
    }

    @Test
    void removeEnergiesMoreThanAttachedShouldClearAll() {
        final InPlayPokemon p = new InPlayPokemon(pikachu());
        p.attachEnergy(new EnergyCard("e1", "Energy", PokemonType.LIGHTNING, true));
        p.removeEnergies(5);
        assertTrue(p.getAttachedEnergies().isEmpty());
        assertTrue(p.getAttachedEnergyCards().isEmpty());
    }

    @Test
    void shouldStartWithNoToolAttached() {
        assertFalse(new InPlayPokemon(pikachu()).hasToolAttached());
    }

    @Test
    void setToolAttachedShouldPersist() {
        final InPlayPokemon p = new InPlayPokemon(pikachu());
        p.attachTool(new TrainerCard.Builder("tool", "tool", TrainerType.POKEMON_TOOL).build());
        assertTrue(p.hasToolAttached());
    }

    @Test
    void evolveIntoShouldPreserveDamageAndEnergies() {
        final InPlayPokemon p = new InPlayPokemon(pikachu());
        p.addDamageCounters(30);
        p.attachEnergy(new EnergyCard("e1", "Energy", PokemonType.LIGHTNING, true));

        final PokemonCard evolution = raichu();
        p.evolveInto(evolution);

        assertEquals("xy1-43", p.getCardId()); // Raichu's ID
        assertEquals(30, p.getDamageCounters()); // Damage is preserved
        assertEquals(1, p.getAttachedEnergies().size()); // Energy is preserved
        assertEquals(1, p.getAttachedEnergyCards().size());
        assertEquals(PokemonType.LIGHTNING, p.getAttachedEnergies().get(0));
        assertEquals(1, p.getUnderlyingCards().size()); // Base card is preserved underneath
    }
}
