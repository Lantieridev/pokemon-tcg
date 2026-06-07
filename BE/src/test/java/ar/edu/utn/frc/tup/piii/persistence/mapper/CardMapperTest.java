package ar.edu.utn.frc.tup.piii.persistence.mapper;

import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;
import ar.edu.utn.frc.tup.piii.persistence.entity.CardEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardMapperTest {

    private CardMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CardMapper(new ObjectMapper());
    }

    // --- Pokémon cards ---

    @Test
    void shouldMapBasicPokemonCard() {
        final CardEntity entity = pokemonEntity("xy1-3", "Weedle", "Basic", 50,
                "[{\"name\":\"Leaf Munch\",\"cost\":[\"Grass\"],\"convertedEnergyCost\":1,"
                + "\"damage\":\"10\",\"text\":\"\"}]",
                "[{\"type\":\"Fire\",\"value\":\"\\u00d72\"}]",
                "[]",
                "[\"Colorless\"]");

        final Card card = mapper.map(entity);

        final PokemonCard pokemon = assertInstanceOf(PokemonCard.class, card);
        assertEquals("xy1-3",          pokemon.getCardId());
        assertEquals("Weedle",         pokemon.getName());
        assertEquals(50,               pokemon.getHp());
        assertEquals(EvolutionStage.BASIC, pokemon.getEvolutionStage());
        assertEquals(PokemonType.GRASS, pokemon.getPokemonType());
        assertEquals(PokemonType.FIRE,  pokemon.getWeaknessType());
        assertNull(pokemon.getResistanceType());
        assertEquals(1, pokemon.getRetreatCost());
        assertEquals(1, pokemon.getAttacks().size());
        assertEquals("Leaf Munch", pokemon.getAttacks().get(0).name());
        assertEquals(10, pokemon.getAttacks().get(0).baseDamage());
    }

    @Test
    void shouldMapStage1PokemonCard() {
        final CardEntity entity = pokemonEntity("xy1-4", "Kakuna", "Stage 1", 70,
                "[{\"name\":\"Harden\",\"cost\":[\"Grass\"],\"convertedEnergyCost\":1,"
                + "\"damage\":\"\",\"text\":\"Reduce damage\"}]",
                "[{\"type\":\"Fire\",\"value\":\"\\u00d72\"}]",
                "[]",
                "[\"Colorless\",\"Colorless\"]");

        final PokemonCard pokemon = assertInstanceOf(PokemonCard.class, mapper.map(entity));
        assertEquals(EvolutionStage.STAGE_1, pokemon.getEvolutionStage());
        assertEquals(2, pokemon.getRetreatCost());
        assertEquals(0, pokemon.getAttacks().get(0).baseDamage()); // empty damage string → 0
    }

    @Test
    void shouldMapStage2PokemonCard() {
        final CardEntity entity = pokemonEntity("xy1-5", "Beedrill", "Stage 2", 120,
                "[{\"name\":\"Poison Jab\",\"cost\":[\"Grass\"],\"convertedEnergyCost\":1,"
                + "\"damage\":\"20\",\"text\":\"\"}]",
                "[{\"type\":\"Fire\",\"value\":\"\\u00d72\"}]",
                "[]",
                "[\"Colorless\"]");

        final PokemonCard pokemon = assertInstanceOf(PokemonCard.class, mapper.map(entity));
        assertEquals(EvolutionStage.STAGE_2, pokemon.getEvolutionStage());
    }

    @Test
    void shouldMapExPokemonAndFlagItCorrectly() {
        final CardEntity entity = pokemonEntity("xy1-1", "Venusaur-EX", "Basic, EX", 180,
                "[{\"name\":\"Jungle Hammer\",\"cost\":[\"Grass\",\"Grass\"],\"convertedEnergyCost\":2,"
                + "\"damage\":\"90\",\"text\":\"\"}]",
                "[{\"type\":\"Fire\",\"value\":\"\\u00d72\"}]",
                "[]",
                "[\"Colorless\",\"Colorless\",\"Colorless\",\"Colorless\"]");

        final PokemonCard pokemon = assertInstanceOf(PokemonCard.class, mapper.map(entity));
        assertTrue(pokemon.isEx());
        assertEquals(EvolutionStage.BASIC, pokemon.getEvolutionStage());
        assertEquals(4, pokemon.getRetreatCost());
        assertEquals(PokemonType.GRASS, pokemon.getPokemonType());
    }

    @Test
    void shouldParsePlusInDamageAsBaseValueOnly() {
        final CardEntity entity = pokemonEntity("xy1-102", "Taillow", "Basic", 50,
                "[{\"name\":\"Aerial Ace\",\"cost\":[\"Colorless\",\"Colorless\"],\"convertedEnergyCost\":2,"
                + "\"damage\":\"10+\",\"text\":\"Flip a coin.\"}]",
                "[{\"type\":\"Lightning\",\"value\":\"\\u00d72\"}]",
                "[{\"type\":\"Fighting\",\"value\":\"-20\"}]",
                "[\"Colorless\"]");

        final PokemonCard pokemon = assertInstanceOf(PokemonCard.class, mapper.map(entity));
        assertEquals(10, pokemon.getAttacks().get(0).baseDamage());
        assertEquals(PokemonType.LIGHTNING, pokemon.getWeaknessType());
        assertEquals(PokemonType.FIGHTING,  pokemon.getResistanceType());
        assertEquals(PokemonType.COLORLESS, pokemon.getPokemonType());
    }

    // --- Trainer cards ---

    @Test
    void shouldMapSupporterCard() {
        final CardEntity entity = trainerEntity("xy1-115", "Cassius", "Supporter", "[]");

        final TrainerCard trainer = assertInstanceOf(TrainerCard.class, mapper.map(entity));
        assertEquals("xy1-115",        trainer.getCardId());
        assertEquals("Cassius",        trainer.getName());
        assertEquals(TrainerType.SUPPORTER, trainer.getTrainerType());
        assertFalse(trainer.isAceSpec());
    }

    @Test
    void shouldMapItemCard() {
        final CardEntity entity = trainerEntity("xy1-116", "Evosoda", "Item", "[]");
        final TrainerCard trainer = assertInstanceOf(TrainerCard.class, mapper.map(entity));
        assertEquals(TrainerType.ITEM, trainer.getTrainerType());
    }

    @Test
    void shouldMapStadiumCard() {
        final CardEntity entity = trainerEntity("xy1-117", "Fairy Garden", "Stadium", "[]");
        final TrainerCard trainer = assertInstanceOf(TrainerCard.class, mapper.map(entity));
        assertEquals(TrainerType.STADIUM, trainer.getTrainerType());
    }

    @Test
    void shouldMapPokemonToolCard() {
        final CardEntity entity = trainerEntity("xy1-119", "Hard Charm", "Pokémon Tool", "[]");
        final TrainerCard trainer = assertInstanceOf(TrainerCard.class, mapper.map(entity));
        assertEquals(TrainerType.POKEMON_TOOL, trainer.getTrainerType());
    }

    @Test
    void shouldDetectAceSpecFromRulesText() {
        final CardEntity entity = trainerEntity("bcr-138", "Computer Search", "Item",
                "[\"You can't have more than 1 ACE SPEC card in your deck.\","
                + "\"You may play as many Item cards as you like during your turn.\"]");

        final TrainerCard trainer = assertInstanceOf(TrainerCard.class, mapper.map(entity));
        assertTrue(trainer.isAceSpec());
    }

    @Test
    void shouldNotMarkRegularItemAsAceSpec() {
        final CardEntity entity = trainerEntity("xy1-118", "Great Ball", "Item",
                "[\"Look at the top 7 cards of your deck.\","
                + "\"You may play as many Item cards as you like during your turn.\"]");

        final TrainerCard trainer = assertInstanceOf(TrainerCard.class, mapper.map(entity));
        assertFalse(trainer.isAceSpec());
    }

    // --- Energy cards ---

    @Test
    void shouldMapBasicFireEnergy() {
        final CardEntity entity = energyEntity("xy1-133", "Fire Energy", "Basic");
        final EnergyCard energy = assertInstanceOf(EnergyCard.class, mapper.map(entity));
        assertEquals(PokemonType.FIRE, energy.getEnergyType());
        assertTrue(energy.isBasic());
    }

    @Test
    void shouldMapBasicWaterEnergy() {
        final CardEntity entity = energyEntity("xy1-134", "Water Energy", "Basic");
        final EnergyCard energy = assertInstanceOf(EnergyCard.class, mapper.map(entity));
        assertEquals(PokemonType.WATER, energy.getEnergyType());
    }

    @Test
    void shouldMapSpecialEnergyAsColorlessAndNotBasic() {
        final CardEntity entity = energyEntity("xy1-130", "Double Colorless Energy", "Special");
        final EnergyCard energy = assertInstanceOf(EnergyCard.class, mapper.map(entity));
        assertEquals(PokemonType.COLORLESS, energy.getEnergyType());
        assertFalse(energy.isBasic());
    }

    // --- Edge cases ---

    @Test
    void shouldThrowForNullEntity() {
        assertThrows(NullPointerException.class, () -> mapper.map(null));
    }

    @Test
    void shouldHandleEmptyAttacksList() {
        final CardEntity entity = pokemonEntity("xy1-x", "TestMon", "Basic", 60,
                "[]", "[]", "[]", "[]");
        final PokemonCard pokemon = assertInstanceOf(PokemonCard.class, mapper.map(entity));
        assertNotNull(pokemon.getAttacks());
        assertTrue(pokemon.getAttacks().isEmpty());
        assertEquals(PokemonType.COLORLESS, pokemon.getPokemonType());
    }

    @Test
    void shouldParseAttackEffectTextCorrectly() {
        final CardEntity entity = pokemonEntity("xy1-x", "TestMon", "Basic", 60,
                "[{\"name\":\"Slash\",\"cost\":[\"Colorless\"],\"convertedEnergyCost\":1,"
                + "\"damage\":\"10\",\"text\":\"Flip a coin. If heads, the Defending Pokémon is now paralyzed.\"},"
                + "{\"name\":\"Toxic\",\"cost\":[\"Grass\"],\"convertedEnergyCost\":1,"
                + "\"damage\":\"20\",\"text\":\"The Defending Pokémon is now poisoned.\"},"
                + "{\"name\":\"Hyper Fang\",\"cost\":[\"Colorless\",\"Colorless\"],\"convertedEnergyCost\":2,"
                + "\"damage\":\"40\",\"text\":\"Flip a coin. If tails, this attack does nothing.\"}]",
                "[]", "[]", "[]");
        final PokemonCard pokemon = assertInstanceOf(PokemonCard.class, mapper.map(entity));
        assertEquals(3, pokemon.getAttacks().size());
        assertEquals("coin_flip_paralysis", pokemon.getAttacks().get(0).effectText());
        assertEquals("poison", pokemon.getAttacks().get(1).effectText());
        assertEquals("coin_flip_fail", pokemon.getAttacks().get(2).effectText());
    }

    // --- helpers ---

    private static CardEntity pokemonEntity(final String id, final String name,
                                            final String subtype, final int hp,
                                            final String attacks, final String weaknesses,
                                            final String resistances, final String retreatCost) {
        return CardEntity.builder()
                .id(id).name(name).supertype("Pokémon").subtype(subtype).hp(hp)
                .rules("[]").attacks(attacks).weaknesses(weaknesses)
                .resistances(resistances).retreatCost(retreatCost)
                .setId("xy1").build();
    }

    private static CardEntity trainerEntity(final String id, final String name,
                                            final String subtype, final String rules) {
        return CardEntity.builder()
                .id(id).name(name).supertype("Trainer").subtype(subtype).hp(0)
                .rules(rules).attacks("[]").weaknesses("[]")
                .resistances("[]").retreatCost("[]")
                .setId("xy1").build();
    }

    private static CardEntity energyEntity(final String id, final String name,
                                           final String subtype) {
        return CardEntity.builder()
                .id(id).name(name).supertype("Energy").subtype(subtype).hp(0)
                .rules("[]").attacks("[]").weaknesses("[]")
                .resistances("[]").retreatCost("[]")
                .setId("xy1").build();
    }
}
