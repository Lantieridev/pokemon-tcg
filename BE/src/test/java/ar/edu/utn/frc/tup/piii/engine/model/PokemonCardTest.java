package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PokemonCardTest {

    private PokemonCard basicPikachu() {
        return new PokemonCard.Builder("xy1-42", "Pikachu", 60, PokemonType.LIGHTNING)
                .retreatCost(1)
                .weaknessType(PokemonType.FIGHTING)
                .resistanceType(PokemonType.METAL)
                .evolutionStage(EvolutionStage.BASIC)
                .attacks(List.of(new Attack("Nuzzle", 0, List.of(PokemonType.COLORLESS))))
                .build();
    }

    private PokemonCard stage1Raichu() {
        return new PokemonCard.Builder("xy1-43", "Raichu", 90, PokemonType.LIGHTNING)
                .retreatCost(0)
                .weaknessType(PokemonType.FIGHTING)
                .evolutionStage(EvolutionStage.STAGE_1)
                .evolvesFrom("Pikachu")
                .attacks(List.of(new Attack("Thunderbolt", 100, List.of(PokemonType.LIGHTNING, PokemonType.LIGHTNING, PokemonType.COLORLESS))))
                .build();
    }

    @Test
    void shouldReturnCardIdAndName() {
        final PokemonCard card = basicPikachu();
        assertEquals("xy1-42", card.getCardId());
        assertEquals("Pikachu", card.getName());
    }

    @Test
    void shouldReturnCardTypePokemon() {
        assertEquals(CardType.POKEMON, basicPikachu().getCardType());
    }

    @Test
    void shouldReturnHpAndType() {
        final PokemonCard card = basicPikachu();
        assertEquals(60, card.getHp());
        assertEquals(PokemonType.LIGHTNING, card.getPokemonType());
    }

    @Test
    void shouldReturnWeaknessAndResistance() {
        final PokemonCard card = basicPikachu();
        assertEquals(PokemonType.FIGHTING, card.getWeaknessType());
        assertEquals(PokemonType.METAL, card.getResistanceType());
    }

    @Test
    void shouldReturnNullWeaknessWhenNotSet() {
        final PokemonCard card = stage1Raichu();
        assertNull(card.getResistanceType());
    }

    @Test
    void shouldReturnRetreatCost() {
        assertEquals(1, basicPikachu().getRetreatCost());
    }

    @Test
    void shouldReturnEvolutionStage() {
        assertEquals(EvolutionStage.BASIC, basicPikachu().getEvolutionStage());
        assertEquals(EvolutionStage.STAGE_1, stage1Raichu().getEvolutionStage());
    }

    @Test
    void shouldReturnEvolvesFrom() {
        assertNull(basicPikachu().getEvolvesFrom());
        assertEquals("Pikachu", stage1Raichu().getEvolvesFrom());
    }

    @Test
    void shouldReturnAttacks() {
        assertEquals(1, basicPikachu().getAttacks().size());
        assertEquals("Nuzzle", basicPikachu().getAttacks().get(0).name());
    }

    @Test
    void isBasicPokemonShouldReturnTrueForBasic() {
        assertTrue(basicPikachu().isBasicPokemon());
    }

    @Test
    void isBasicPokemonShouldReturnFalseForStage1() {
        assertFalse(stage1Raichu().isBasicPokemon());
    }

    @Test
    void shouldDefaultExToFalse() {
        assertFalse(basicPikachu().isEx());
    }

    @Test
    void shouldReturnTrueForExPokemon() {
        final PokemonCard ex = new PokemonCard.Builder("xy1-1", "Venusaur-EX", 180, PokemonType.GRASS)
                .ex(true)
                .evolutionStage(EvolutionStage.BASIC)
                .build();
        assertTrue(ex.isEx());
    }

    @Test
    void builderShouldThrowWhenCardIdIsNull() {
        assertThrows(NullPointerException.class,
                () -> new PokemonCard.Builder(null, "Pikachu", 60, PokemonType.LIGHTNING).build());
    }

    @Test
    void builderShouldThrowWhenNameIsNull() {
        assertThrows(NullPointerException.class,
                () -> new PokemonCard.Builder("xy1-42", null, 60, PokemonType.LIGHTNING).build());
    }

    @Test
    void builderShouldThrowWhenTypeIsNull() {
        assertThrows(NullPointerException.class,
                () -> new PokemonCard.Builder("xy1-42", "Pikachu", 60, null).build());
    }

    @Test
    void attacksListShouldBeImmutable() {
        final PokemonCard card = basicPikachu();
        assertThrows(UnsupportedOperationException.class, () -> card.getAttacks().add(
                new Attack("X", 0, List.of())));
    }
}
