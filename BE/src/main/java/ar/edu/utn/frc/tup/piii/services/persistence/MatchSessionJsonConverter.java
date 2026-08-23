package ar.edu.utn.frc.tup.piii.services.persistence;

import ar.edu.utn.frc.tup.piii.engine.infra.RandomCoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.*;
import ar.edu.utn.frc.tup.piii.engine.session.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Converter(autoApply = false)
public class MatchSessionJsonConverter implements AttributeConverter<MatchSession, String> {

    private static final String CARD_ID_KEY = "cardId";
    private static final String NAME_KEY = "name";
    private static final String ACTIVE_POKEMON_KEY = "activePokemon";
    private static final String BENCH_KEY = "bench";
    private static final String HAND_KEY = "hand";
    private static final String TURNS_IN_PLAY_KEY = "turnsInPlay";
    private static final String ACTIVE_KEY = "active";
    private static final String BENCH_PREFIX = "bench_";
    private static final String ACTIVE_STADIUM_KEY = "activeStadium";
    private static final String PLAYERS_KEY = "players";
    private static final String PRIZE_PILE_KEY = "prizePile";
    private static final String CARDS_KEY = "cards";
    private static final String MATCH_ID_KEY = "matchId";
    private static final String STATE_KEY = "state";
    private static final String ACTIVE_PLAYER_INDEX_KEY = "activePlayerIndex";
    private static final String VERSION_KEY = "version";
    private static final String WINNER_ID_KEY = "winnerId";
    private static final String VICTORY_REASON_KEY = "victoryReason";
    private static final String PENDING_SELECTION_REQUEST_KEY = "pendingSelectionRequest";
    private static final String PLAYER_RUNTIMES_KEY = "playerRuntimes";
    private static final String UUID_KEY = "uuid";
    private static final String ATTACHED_ENERGIES_KEY = "attachedEnergies";
    private static final String ATTACHED_ENERGY_CARDS_KEY = "attachedEnergyCards";
    private static final String ATTACHED_TOOL_KEY = "attachedTool";
    private static final String BASIC_KEY = "basic";
    private static final String ENERGY_COUNT_KEY = "energyCount";
    private static final String PROVIDES_ALL_TYPES_KEY = "providesAllTypes";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        // Register Mixins
        OBJECT_MAPPER.addMixIn(Card.class, CardMixin.class);
        OBJECT_MAPPER.addMixIn(PokemonCard.class, CardMixin.class);
        OBJECT_MAPPER.addMixIn(TrainerCard.class, CardMixin.class);
        OBJECT_MAPPER.addMixIn(EnergyCard.class, CardMixin.class);
        OBJECT_MAPPER.addMixIn(BattlePokemonState.class, BattlePokemonStateMixin.class);
        OBJECT_MAPPER.addMixIn(InPlayPokemon.class, BattlePokemonStateMixin.class);
        OBJECT_MAPPER.addMixIn(MatchSession.class, MatchSessionMixin.class);

        // Register custom serializers and deserializers
        SimpleModule module = new SimpleModule();
        module.addSerializer(PlayerState.class, new PlayerStateSerializer());
        module.addDeserializer(PlayerState.class, new PlayerStateDeserializer());
        module.addSerializer(MatchBoard.class, new MatchBoardSerializer());
        module.addDeserializer(MatchBoard.class, new MatchBoardDeserializer());
        module.addSerializer(PlayerRuntime.class, new PlayerRuntimeSerializer());
        module.addDeserializer(PlayerRuntime.class, new PlayerRuntimeDeserializer());
        module.addSerializer(StatusEffectManager.class, new StatusEffectManagerSerializer());
        module.addDeserializer(StatusEffectManager.class, new StatusEffectManagerDeserializer());
        module.addSerializer(Deck.class, new DeckSerializer());
        module.addDeserializer(Deck.class, new DeckDeserializer());
        module.addSerializer(Hand.class, new HandSerializer());
        module.addDeserializer(Hand.class, new HandDeserializer());
        module.addSerializer(Bench.class, new BenchSerializer());
        module.addDeserializer(Bench.class, new BenchDeserializer());
        module.addSerializer(DiscardPile.class, new DiscardPileSerializer());
        module.addDeserializer(DiscardPile.class, new DiscardPileDeserializer());
        module.addDeserializer(PokemonCard.class, new PokemonCardDeserializer());
        module.addDeserializer(TrainerCard.class, new TrainerCardDeserializer());
        module.addDeserializer(EnergyCard.class, new EnergyCardDeserializer());
        module.addSerializer(MatchSession.class, new MatchSessionSerializer());
        module.addDeserializer(MatchSession.class, new MatchSessionDeserializer());
        module.addSerializer(InPlayPokemon.class, new InPlayPokemonSerializer());
        module.addDeserializer(InPlayPokemon.class, new InPlayPokemonDeserializer());

        OBJECT_MAPPER.registerModule(module);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String convertToDatabaseColumn(MatchSession attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to serialize MatchSession to JSON string", e);
        }
    }

    @Override
    public MatchSession convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || "null".equalsIgnoreCase(dbData.trim())) {
            return null;
        }
        try {
            String cleanData = dbData.trim();
            boolean changed = true;
            while (changed) {
                String unwrapped = unwrapJsonWrapping(cleanData);
                if (unwrapped.equals(cleanData)) {
                    changed = false;
                } else {
                    cleanData = unwrapped;
                }
            }
            return OBJECT_MAPPER.readValue(cleanData, MatchSession.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON string to MatchSession", e);
        }
    }

    private String unwrapJsonWrapping(String data) {
        if (data.startsWith("[") && data.endsWith("]")) {
            try {
                JsonNode arrayNode = OBJECT_MAPPER.readTree(data);
                if (arrayNode.isArray() && arrayNode.size() == 1) {
                    return arrayNode.get(0).asText().trim();
                }
            } catch (IOException e) {
                return data;
            }
        }
        if (data.startsWith("\"") && data.endsWith("\"")) {
            try {
                return OBJECT_MAPPER.readValue(data, String.class).trim();
            } catch (IOException e) {
                log.warn("Could not unwrap double-encoded MatchSession JSON string, using it as-is: {}", e.getMessage());
            }
        }
        return data;
    }

    // -------------------------------------------------------------------------
    // Mixins
    // -------------------------------------------------------------------------

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "@type"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = PokemonCard.class, name = "pokemon"),
            @JsonSubTypes.Type(value = TrainerCard.class, name = "trainer"),
            @JsonSubTypes.Type(value = EnergyCard.class, name = "energy")
    })
    public interface CardMixin {}

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "@type"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = InPlayPokemon.class, name = "inPlay")
    })
    public interface BattlePokemonStateMixin {}

    public interface MatchSessionMixin {
        @JsonIgnore
        CoinFlipper getCoinFlipper();

        @JsonIgnore
        void setCoinFlipper(CoinFlipper coinFlipper);
    }

    // -------------------------------------------------------------------------
    // Custom Serializers and Deserializers
    // -------------------------------------------------------------------------

    public static class PokemonCardDeserializer extends JsonDeserializer<PokemonCard> {
        @Override
        public PokemonCard deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            String cardId = node.get(CARD_ID_KEY).asText();
            String name = node.get(NAME_KEY).asText();
            int hp = node.get("hp").asInt();
            PokemonType pokemonType = PokemonType.valueOf(node.get("pokemonType").asText());

            PokemonCard.Builder builder = new PokemonCard.Builder(cardId, name, hp, pokemonType);
            parseTypes(node, builder);
            parseAttributes(node, builder);
            parseAttacks(p, node, builder);
            return builder.build();
        }

        private void parseTypes(JsonNode node, PokemonCard.Builder builder) {
            if (node.has("weaknessType") && !node.get("weaknessType").isNull()) {
                builder.weaknessType(PokemonType.valueOf(node.get("weaknessType").asText()));
            }
            if (node.has("resistanceType") && !node.get("resistanceType").isNull()) {
                builder.resistanceType(PokemonType.valueOf(node.get("resistanceType").asText()));
            }
        }

        private void parseAttributes(JsonNode node, PokemonCard.Builder builder) {
            if (node.has("retreatCost")) {
                builder.retreatCost(node.get("retreatCost").asInt());
            }
            if (node.has("ex")) {
                builder.ex(node.get("ex").asBoolean());
            }
            if (node.has("evolutionStage") && !node.get("evolutionStage").isNull()) {
                builder.evolutionStage(EvolutionStage.valueOf(node.get("evolutionStage").asText()));
            }
            if (node.has("evolvesFrom") && !node.get("evolvesFrom").isNull()) {
                builder.evolvesFrom(node.get("evolvesFrom").asText());
            }
        }

        private void parseAttacks(JsonParser p, JsonNode node, PokemonCard.Builder builder) throws IOException {
            if (node.has("attacks")) {
                List<Attack> attacks = new ArrayList<>();
                for (JsonNode attNode : node.get("attacks")) {
                    attacks.add(p.getCodec().treeToValue(attNode, Attack.class));
                }
                builder.attacks(attacks);
            }
        }
    }

    public static class TrainerCardDeserializer extends JsonDeserializer<TrainerCard> {
        @Override
        public TrainerCard deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            String cardId = node.get(CARD_ID_KEY).asText();
            String name = node.get(NAME_KEY).asText();
            TrainerType trainerType = TrainerType.valueOf(node.get("trainerType").asText());

            TrainerCard.Builder builder = new TrainerCard.Builder(cardId, name, trainerType);
            if (node.has("aceSpec")) {
                builder.aceSpec(node.get("aceSpec").asBoolean());
            }
            if (node.has("effectText")) {
                builder.effectText(node.get("effectText").asText());
            }
            if (node.has("effectId") && !node.get("effectId").isNull()) {
                builder.effectId(TrainerEffectId.valueOf(node.get("effectId").asText()));
            }
            if (node.has("toolEffectId") && !node.get("toolEffectId").isNull()) {
                builder.toolEffectId(PokemonToolEffectId.valueOf(node.get("toolEffectId").asText()));
            }
            return builder.build();
        }
    }

    public static class PlayerStateSerializer extends JsonSerializer<PlayerState> {
        @Override
        public void serialize(PlayerState value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            if (value.getActivePokemon() != null) {
                gen.writeObjectField(ACTIVE_POKEMON_KEY, value.getActivePokemon());
            } else {
                gen.writeNullField(ACTIVE_POKEMON_KEY);
            }

            gen.writeArrayFieldStart(BENCH_KEY);
            for (BattlePokemonState pk : value.getBench()) {
                if (pk != null) {
                    gen.writeObject(pk);
                } else {
                    gen.writeNull();
                }
            }
            gen.writeEndArray();

            gen.writeObjectField(HAND_KEY, value.getHand());
            gen.writeObjectField("activeAttacks", value.getActiveAttacks());
            gen.writeNumberField("deckSize", value.getDeckSize());
            gen.writeNumberField("prizeCount", value.getPrizeCount());

            // turnsInPlay using location markers
            gen.writeObjectFieldStart(TURNS_IN_PLAY_KEY);
            if (value.getActivePokemon() != null) {
                int activeTurns = value.getTurnsInPlay(value.getActivePokemon());
                if (activeTurns > 0) {
                    gen.writeNumberField(ACTIVE_KEY, activeTurns);
                }
            }
            List<BattlePokemonState> bench = value.getBench();
            for (int i = 0; i < bench.size(); i++) {
                BattlePokemonState pk = bench.get(i);
                int benchTurns = value.getTurnsInPlay(pk);
                if (benchTurns > 0) {
                    gen.writeNumberField(BENCH_PREFIX + i, benchTurns);
                }
            }
            gen.writeEndObject();

            gen.writeEndObject();
        }
    }

    public static class PlayerStateDeserializer extends JsonDeserializer<PlayerState> {
        @Override
        public PlayerState deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);

            BattlePokemonState activePokemon = readActivePokemon(p, node);
            List<BattlePokemonState> bench = readBench(p, node);
            List<String> hand = readHand(node);
            List<Attack> activeAttacks = readActiveAttacks(p, node);

            int deckSize = node.get("deckSize").asInt();
            int prizeCount = node.get("prizeCount").asInt();

            Map<BattlePokemonState, Integer> turnsInPlay = readTurnsInPlay(node, activePokemon, bench);

            return new PlayerState(activePokemon, bench, hand, activeAttacks, deckSize, prizeCount, turnsInPlay);
        }

        private BattlePokemonState readActivePokemon(JsonParser p, JsonNode node) throws IOException {
            if (node.has(ACTIVE_POKEMON_KEY) && !node.get(ACTIVE_POKEMON_KEY).isNull()) {
                return p.getCodec().treeToValue(node.get(ACTIVE_POKEMON_KEY), BattlePokemonState.class);
            }
            return null;
        }

        private List<BattlePokemonState> readBench(JsonParser p, JsonNode node) throws IOException {
            List<BattlePokemonState> bench = new ArrayList<>();
            if (node.has(BENCH_KEY)) {
                for (JsonNode bNode : node.get(BENCH_KEY)) {
                    bench.add(p.getCodec().treeToValue(bNode, BattlePokemonState.class));
                }
            }
            return bench;
        }

        private List<String> readHand(JsonNode node) {
            List<String> hand = new ArrayList<>();
            if (node.has(HAND_KEY)) {
                for (JsonNode hNode : node.get(HAND_KEY)) {
                    hand.add(hNode.asText());
                }
            }
            return hand;
        }

        private List<Attack> readActiveAttacks(JsonParser p, JsonNode node) throws IOException {
            List<Attack> activeAttacks = new ArrayList<>();
            if (node.has("activeAttacks")) {
                for (JsonNode aNode : node.get("activeAttacks")) {
                    activeAttacks.add(p.getCodec().treeToValue(aNode, Attack.class));
                }
            }
            return activeAttacks;
        }

        private Map<BattlePokemonState, Integer> readTurnsInPlay(JsonNode node, BattlePokemonState activePokemon, List<BattlePokemonState> bench) {
            Map<BattlePokemonState, Integer> turnsInPlay = new HashMap<>();
            if (!node.has(TURNS_IN_PLAY_KEY)) {
                return turnsInPlay;
            }
            JsonNode turnsNode = node.get(TURNS_IN_PLAY_KEY);
            Iterator<Map.Entry<String, JsonNode>> fields = turnsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                int val = field.getValue().asInt();
                if (ACTIVE_KEY.equals(key)) {
                    if (activePokemon != null) {
                        turnsInPlay.put(activePokemon, val);
                    }
                } else if (key.startsWith(BENCH_PREFIX)) {
                    parseBenchTurns(key, val, bench, turnsInPlay);
                }
            }
            return turnsInPlay;
        }

        private void parseBenchTurns(String key, int val, List<BattlePokemonState> bench, Map<BattlePokemonState, Integer> turnsInPlay) {
            try {
                int index = Integer.parseInt(key.substring(BENCH_PREFIX.length()));
                if (index >= 0 && index < bench.size()) {
                    turnsInPlay.put(bench.get(index), val);
                }
            } catch (NumberFormatException e) {
                log.warn("Ignoring turnsInPlay key with invalid bench index: {}", key);
            }
        }
    }

    public static class MatchBoardSerializer extends JsonSerializer<MatchBoard> {
        @Override
        public void serialize(MatchBoard value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            if (value.getActiveStadium() != null) {
                gen.writeObjectField(ACTIVE_STADIUM_KEY, value.getActiveStadium());
            } else {
                gen.writeNullField(ACTIVE_STADIUM_KEY);
            }
            gen.writeNumberField("activeStadiumOwnerIndex", value.getActiveStadiumOwnerIndex());
            List<PlayerState> players = List.of(value.getPlayerState(0), value.getPlayerState(1));
            gen.writeObjectField(PLAYERS_KEY, players);
            gen.writeEndObject();
        }
    }

    public static class MatchBoardDeserializer extends JsonDeserializer<MatchBoard> {
        @Override
        public MatchBoard deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            List<PlayerState> players = new ArrayList<>();
            if (node.has(PLAYERS_KEY)) {
                for (JsonNode pNode : node.get(PLAYERS_KEY)) {
                    players.add(p.getCodec().treeToValue(pNode, PlayerState.class));
                }
            }
            MatchBoard board = new MatchBoard(players);
            if (node.has(ACTIVE_STADIUM_KEY) && !node.get(ACTIVE_STADIUM_KEY).isNull()) {
                board.replaceStadium(p.getCodec().treeToValue(node.get(ACTIVE_STADIUM_KEY), TrainerCard.class));
            }
            if (node.has("activeStadiumOwnerIndex")) {
                board.setActiveStadiumOwnerIndex(node.get("activeStadiumOwnerIndex").asInt());
            }
            return board;
        }
    }

    public static class PlayerRuntimeSerializer extends JsonSerializer<PlayerRuntime> {
        @Override
        public void serialize(PlayerRuntime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeObjectField("deck", value.getDeck());
            gen.writeObjectField(HAND_KEY, value.getHand());
            gen.writeObjectField(BENCH_KEY, value.getBench());
            gen.writeObjectField("discardPile", value.getDiscardPile());
            gen.writeObjectField("statusEffectManager", value.getStatusEffectManager());
            if (value.getActivePokemon() != null) {
                gen.writeObjectField(ACTIVE_POKEMON_KEY, value.getActivePokemon());
            } else {
                gen.writeNullField(ACTIVE_POKEMON_KEY);
            }
            // Serialize prizePile
            gen.writeArrayFieldStart(PRIZE_PILE_KEY);
            for (Card card : value.getPrizePile()) {
                if (card != null) {
                    gen.writeObject(card);
                } else {
                    gen.writeNull();
                }
            }
            gen.writeEndArray();

            // Serialize turnsInPlay using location markers
            gen.writeObjectFieldStart(TURNS_IN_PLAY_KEY);
            if (value.getActivePokemon() != null) {
                int activeTurns = value.getTurnsInPlay(value.getActivePokemon());
                gen.writeNumberField(ACTIVE_KEY, activeTurns);
            }
            List<BattlePokemonState> bench = value.getBench().getAll();
            for (int i = 0; i < bench.size(); i++) {
                BattlePokemonState pk = bench.get(i);
                if (pk != null) {
                    int benchTurns = value.getTurnsInPlay(pk);
                    gen.writeNumberField(BENCH_PREFIX + i, benchTurns);
                }
            }
            gen.writeEndObject();

            gen.writeBooleanField("knockedOutLastTurn", value.isKnockedOutLastTurn());
            gen.writeNumberField("startingPrizeCount", value.getStartingPrizeCount());

            gen.writeEndObject();
        }
    }

    public static class PlayerRuntimeDeserializer extends JsonDeserializer<PlayerRuntime> {
        @Override
        public PlayerRuntime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            Deck deck = p.getCodec().treeToValue(node.get("deck"), Deck.class);
            Hand hand = p.getCodec().treeToValue(node.get(HAND_KEY), Hand.class);
            Bench bench = p.getCodec().treeToValue(node.get(BENCH_KEY), Bench.class);
            DiscardPile discardPile = p.getCodec().treeToValue(node.get("discardPile"), DiscardPile.class);
            StatusEffectManager statusEffectManager = p.getCodec().treeToValue(node.get("statusEffectManager"), StatusEffectManager.class);
            BattlePokemonState activePokemon = p.getCodec().treeToValue(node.get(ACTIVE_POKEMON_KEY), BattlePokemonState.class);

            List<Card> prizePile = readPrizePile(p, node);
            Map<BattlePokemonState, Integer> turnsInPlay = readTurnsInPlay(node, activePokemon, bench);

            boolean knockedOutLastTurn = node.has("knockedOutLastTurn") && node.get("knockedOutLastTurn").asBoolean();
            int startingPrizeCount = node.has("startingPrizeCount") ? node.get("startingPrizeCount").asInt() : 6;

            PlayerRuntime playerRuntime = new PlayerRuntime(deck, hand, bench, discardPile, statusEffectManager, activePokemon, prizePile, turnsInPlay);
            playerRuntime.setKnockedOutLastTurn(knockedOutLastTurn);
            playerRuntime.setStartingPrizeCount(startingPrizeCount);
            return playerRuntime;
        }

        private List<Card> readPrizePile(JsonParser p, JsonNode node) throws IOException {
            List<Card> prizePile = new ArrayList<>();
            if (node.has(PRIZE_PILE_KEY) && !node.get(PRIZE_PILE_KEY).isNull()) {
                for (JsonNode pNode : node.get(PRIZE_PILE_KEY)) {
                    prizePile.add(p.getCodec().treeToValue(pNode, Card.class));
                }
            }
            return prizePile;
        }

        private Map<BattlePokemonState, Integer> readTurnsInPlay(JsonNode node, BattlePokemonState activePokemon, Bench bench) {
            Map<BattlePokemonState, Integer> turnsInPlay = new HashMap<>();
            if (!node.has(TURNS_IN_PLAY_KEY) || node.get(TURNS_IN_PLAY_KEY).isNull()) {
                return turnsInPlay;
            }
            JsonNode turnsNode = node.get(TURNS_IN_PLAY_KEY);
            Iterator<Map.Entry<String, JsonNode>> fields = turnsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                int val = field.getValue().asInt();
                if (ACTIVE_KEY.equals(key)) {
                    if (activePokemon != null) {
                        turnsInPlay.put(activePokemon, val);
                    }
                } else if (key.startsWith(BENCH_PREFIX)) {
                    parseBenchTurns(key, val, bench, turnsInPlay);
                }
            }
            return turnsInPlay;
        }

        private void parseBenchTurns(String key, int val, Bench bench, Map<BattlePokemonState, Integer> turnsInPlay) {
            try {
                int index = Integer.parseInt(key.substring(BENCH_PREFIX.length()));
                List<BattlePokemonState> benched = bench.getAll();
                if (index >= 0 && index < benched.size()) {
                    turnsInPlay.put(benched.get(index), val);
                }
            } catch (NumberFormatException e) {
                log.warn("Ignoring turnsInPlay key with invalid bench index: {}", key);
            }
        }
    }

    public static class StatusEffectManagerSerializer extends JsonSerializer<StatusEffectManager> {
        @Override
        public void serialize(StatusEffectManager value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeObjectField("activeEffects", value.activeEffects());
            gen.writeEndObject();
        }
    }

    public static class StatusEffectManagerDeserializer extends JsonDeserializer<StatusEffectManager> {
        @Override
        public StatusEffectManager deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            StatusEffectManager manager = new StatusEffectManager(new RandomCoinFlipper());
            if (node.has("activeEffects")) {
                for (JsonNode typeNode : node.get("activeEffects")) {
                    manager.apply(StatusEffectType.valueOf(typeNode.asText()));
                }
            }
            return manager;
        }
    }

    public static class DeckSerializer extends JsonSerializer<Deck> {
        @Override
        public void serialize(Deck value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeArrayFieldStart(CARDS_KEY);
            for (Card card : value.getCards()) {
                if (card != null) {
                    gen.writeObject(card);
                } else {
                    gen.writeNull();
                }
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
    }

    public static class DeckDeserializer extends JsonDeserializer<Deck> {
        @Override
        public Deck deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            List<Card> cards = new ArrayList<>();
            if (node.has(CARDS_KEY)) {
                for (JsonNode cNode : node.get(CARDS_KEY)) {
                    cards.add(p.getCodec().treeToValue(cNode, Card.class));
                }
            }
            return new Deck(cards);
        }
    }

    public static class HandSerializer extends JsonSerializer<Hand> {
        @Override
        public void serialize(Hand value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeArrayFieldStart(CARDS_KEY);
            for (Card card : value.getCards()) {
                if (card != null) {
                    gen.writeObject(card);
                } else {
                    gen.writeNull();
                }
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
    }

    public static class HandDeserializer extends JsonDeserializer<Hand> {
        @Override
        public Hand deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            Hand hand = new Hand();
            if (node.has(CARDS_KEY)) {
                for (JsonNode cNode : node.get(CARDS_KEY)) {
                    hand.addCard(p.getCodec().treeToValue(cNode, Card.class));
                }
            }
            return hand;
        }
    }

    public static class BenchSerializer extends JsonSerializer<Bench> {
        @Override
        public void serialize(Bench value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeArrayFieldStart("slots");
            for (BattlePokemonState pk : value.getAll()) {
                if (pk != null) {
                    gen.writeObject(pk);
                } else {
                    gen.writeNull();
                }
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
    }

    public static class BenchDeserializer extends JsonDeserializer<Bench> {
        @Override
        public Bench deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            Bench bench = new Bench();
            if (node.has("slots")) {
                for (JsonNode sNode : node.get("slots")) {
                    bench.place(p.getCodec().treeToValue(sNode, BattlePokemonState.class));
                }
            }
            return bench;
        }
    }

    public static class DiscardPileSerializer extends JsonSerializer<DiscardPile> {
        @Override
        public void serialize(DiscardPile value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeArrayFieldStart(CARDS_KEY);
            for (Card card : value.getCards()) {
                if (card != null) {
                    gen.writeObject(card);
                } else {
                    gen.writeNull();
                }
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
    }

    public static class DiscardPileDeserializer extends JsonDeserializer<DiscardPile> {
        @Override
        public DiscardPile deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            DiscardPile discardPile = new DiscardPile();
            if (node.has(CARDS_KEY)) {
                for (JsonNode cNode : node.get(CARDS_KEY)) {
                    discardPile.add(p.getCodec().treeToValue(cNode, Card.class));
                }
            }
            return discardPile;
        }
    }

    public static class MatchSessionSerializer extends JsonSerializer<MatchSession> {
        @Override
        public void serialize(MatchSession value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField(MATCH_ID_KEY, value.getMatchId());
            gen.writeObjectField("playerIds", value.getPlayerIds());
            gen.writeObjectField("board", value.getBoard());
            gen.writeStringField(STATE_KEY, value.getState().name());
            gen.writeNumberField(ACTIVE_PLAYER_INDEX_KEY, value.getActivePlayerIndex());
            gen.writeNumberField(VERSION_KEY, value.getVersion());
            if (value.getWinnerId() != null) {
                gen.writeStringField(WINNER_ID_KEY, value.getWinnerId());
            } else {
                gen.writeNullField(WINNER_ID_KEY);
            }
            if (value.getVictoryReason() != null) {
                gen.writeStringField(VICTORY_REASON_KEY, value.getVictoryReason());
            } else {
                gen.writeNullField(VICTORY_REASON_KEY);
            }
            if (value.getPendingSelectionRequest() != null) {
                gen.writeObjectField(PENDING_SELECTION_REQUEST_KEY, value.getPendingSelectionRequest());
            } else {
                gen.writeNullField(PENDING_SELECTION_REQUEST_KEY);
            }

            List<PlayerRuntime> runtimes = value.hasPlayerRuntimes()
                    ? List.of(value.getPlayerRuntime(0), value.getPlayerRuntime(1))
                    : null;
            gen.writeObjectField(PLAYER_RUNTIMES_KEY, runtimes);

            gen.writeEndObject();
        }
    }

    public static class MatchSessionDeserializer extends JsonDeserializer<MatchSession> {
        @Override
        public MatchSession deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            if (node == null || node.isNull()) {
                return null;
            }
            String matchId = readStringProp(node, MATCH_ID_KEY, null);
            List<String> playerIds = readPlayerIds(node);
            MatchBoard board = p.getCodec().treeToValue(node.get("board"), MatchBoard.class);
            List<PlayerRuntime> playerRuntimes = readPlayerRuntimes(p, node);

            MatchSessionState state = readState(node);
            int activePlayerIndex = readIntProp(node, ACTIVE_PLAYER_INDEX_KEY, -1);
            String winnerId = readStringProp(node, WINNER_ID_KEY, null);
            String victoryReason = readStringProp(node, VICTORY_REASON_KEY, null);
            long version = readLongProp(node, VERSION_KEY, 1L);
            PendingSelectionRequest pendingSelectionRequest = readPendingSelectionRequest(p, node);

            if (board != null && playerRuntimes != null) {
                board.bindRuntimes(playerRuntimes);
            }
            MatchSession session = new MatchSession(matchId, playerIds, board, playerRuntimes);

            restoreSessionState(session, state, activePlayerIndex, winnerId, victoryReason, version, pendingSelectionRequest);

            // Re-inject the default CoinFlipper
            session.setCoinFlipper(new RandomCoinFlipper());

            return session;
        }

        private String readStringProp(JsonNode node, String name, String defaultValue) {
            return node.has(name) && !node.get(name).isNull() ? node.get(name).asText() : defaultValue;
        }

        private int readIntProp(JsonNode node, String name, int defaultValue) {
            return node.has(name) && !node.get(name).isNull() ? node.get(name).asInt() : defaultValue;
        }

        private long readLongProp(JsonNode node, String name, long defaultValue) {
            return node.has(name) && !node.get(name).isNull() ? node.get(name).asLong() : defaultValue;
        }

        private MatchSessionState readState(JsonNode node) {
            return node.has(STATE_KEY) && !node.get(STATE_KEY).isNull()
                    ? MatchSessionState.valueOf(node.get(STATE_KEY).asText())
                    : MatchSessionState.WAITING;
        }

        private PendingSelectionRequest readPendingSelectionRequest(JsonParser p, JsonNode node) throws IOException {
            return node.has(PENDING_SELECTION_REQUEST_KEY) && !node.get(PENDING_SELECTION_REQUEST_KEY).isNull()
                    ? p.getCodec().treeToValue(node.get(PENDING_SELECTION_REQUEST_KEY), PendingSelectionRequest.class)
                    : null;
        }

        private List<String> readPlayerIds(JsonNode node) {
            List<String> playerIds = new ArrayList<>();
            if (node.has("playerIds")) {
                for (JsonNode idNode : node.get("playerIds")) {
                    playerIds.add(idNode.asText());
                }
            }
            return playerIds;
        }

        // Suppress ReturnEmptyCollectionRatherThanNull: null indicates legacy format without player runtimes
        @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
        private List<PlayerRuntime> readPlayerRuntimes(JsonParser p, JsonNode node) throws IOException {
            if (node.has(PLAYER_RUNTIMES_KEY) && !node.get(PLAYER_RUNTIMES_KEY).isNull()) {
                List<PlayerRuntime> playerRuntimes = new ArrayList<>();
                for (JsonNode rNode : node.get(PLAYER_RUNTIMES_KEY)) {
                    playerRuntimes.add(p.getCodec().treeToValue(rNode, PlayerRuntime.class));
                }
                return playerRuntimes;
            }
            return null;
        }

        private void restoreSessionState(MatchSession session, MatchSessionState state, int activePlayerIndex,
                                         String winnerId, String victoryReason, long version,
                                         PendingSelectionRequest pendingSelectionRequest) {
            if (state == MatchSessionState.SETUP) {
                session.setup();
            } else if (state == MatchSessionState.ACTIVE) {
                session.setup();
                session.start();
            } else if (state == MatchSessionState.FINISHED) {
                session.setup();
                session.start();
                session.finish();
            }

            session.setActivePlayerIndex(activePlayerIndex);
            session.setWinnerId(winnerId);
            session.setVictoryReason(victoryReason);

            for (long v = session.getVersion(); v < version; v++) {
                session.incrementVersion();
            }

            session.setPendingSelectionRequest(pendingSelectionRequest);
        }
    }

    public static class InPlayPokemonSerializer extends JsonSerializer<InPlayPokemon> {
        @Override
        public void serialize(InPlayPokemon value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("@type", "inPlay");
            gen.writeStringField(UUID_KEY, value.getUuid());
            gen.writeObjectField("card", value.getCard());
            gen.writeNumberField("damageCounters", value.getDamageCounters());
            gen.writeObjectField(ATTACHED_ENERGIES_KEY, value.getAttachedEnergies());
            gen.writeObjectField(ATTACHED_ENERGY_CARDS_KEY, value.getAttachedEnergyCards());
            if (value.getAttachedTool().isPresent()) {
                gen.writeObjectField(ATTACHED_TOOL_KEY, value.getAttachedTool().get());
            } else {
                gen.writeNullField(ATTACHED_TOOL_KEY);
            }
            gen.writeEndObject();
        }

        @Override
        public void serializeWithType(InPlayPokemon value, JsonGenerator gen, SerializerProvider serializers, com.fasterxml.jackson.databind.jsontype.TypeSerializer typeSer) throws IOException {
            com.fasterxml.jackson.core.type.WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen,
                    typeSer.typeId(value, com.fasterxml.jackson.core.JsonToken.START_OBJECT));
            gen.writeStringField(UUID_KEY, value.getUuid());
            gen.writeObjectField("card", value.getCard());
            gen.writeNumberField("damageCounters", value.getDamageCounters());
            gen.writeObjectField(ATTACHED_ENERGIES_KEY, value.getAttachedEnergies());
            gen.writeObjectField(ATTACHED_ENERGY_CARDS_KEY, value.getAttachedEnergyCards());
            if (value.getAttachedTool().isPresent()) {
                gen.writeObjectField(ATTACHED_TOOL_KEY, value.getAttachedTool().get());
            } else {
                gen.writeNullField(ATTACHED_TOOL_KEY);
            }
            typeSer.writeTypeSuffix(gen, typeIdDef);
        }
    }

    public static class InPlayPokemonDeserializer extends JsonDeserializer<InPlayPokemon> {
        @Override
        public InPlayPokemon deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            PokemonCard card = p.getCodec().treeToValue(node.get("card"), PokemonCard.class);
            int damageCounters = node.get("damageCounters").asInt();
            List<PokemonType> attachedEnergies = parseAttachedEnergies(node);
            List<EnergyCard> attachedEnergyCards = parseAttachedEnergyCards(node, attachedEnergies);

            TrainerCard tool = null;
            if (node.has(ATTACHED_TOOL_KEY) && !node.get(ATTACHED_TOOL_KEY).isNull()) {
                tool = p.getCodec().treeToValue(node.get(ATTACHED_TOOL_KEY), TrainerCard.class);
            }
            String uuidVal = node.has(UUID_KEY) ? node.get(UUID_KEY).asText() : UUID.randomUUID().toString();
            final InPlayPokemon ip = new InPlayPokemon(card, damageCounters, attachedEnergies, attachedEnergyCards, tool);
            ip.setUuid(uuidVal);
            return ip;
        }

        private List<PokemonType> parseAttachedEnergies(JsonNode node) {
            List<PokemonType> attachedEnergies = new ArrayList<>();
            if (node.has(ATTACHED_ENERGIES_KEY)) {
                for (JsonNode eNode : node.get(ATTACHED_ENERGIES_KEY)) {
                    attachedEnergies.add(PokemonType.valueOf(eNode.asText()));
                }
            }
            return attachedEnergies;
        }

        private List<EnergyCard> parseAttachedEnergyCards(JsonNode node, List<PokemonType> attachedEnergies) {
            List<EnergyCard> attachedEnergyCards = new ArrayList<>();
            if (node.has(ATTACHED_ENERGY_CARDS_KEY) && !node.get(ATTACHED_ENERGY_CARDS_KEY).isNull()) {
                for (JsonNode eNode : node.get(ATTACHED_ENERGY_CARDS_KEY)) {
                    attachedEnergyCards.add(parseSingleEnergyCard(eNode));
                }
            } else {
                for (PokemonType type : attachedEnergies) {
                    attachedEnergyCards.add(new EnergyCard("dummy-" + UUID.randomUUID(), type.name() + " Energy", type, true));
                }
            }
            return attachedEnergyCards;
        }

        private EnergyCard parseSingleEnergyCard(JsonNode eNode) {
            if (eNode.isTextual()) {
                return new EnergyCard("dummy-" + UUID.randomUUID(), eNode.asText() + " Energy", PokemonType.valueOf(eNode.asText()), true);
            }
            String cardId = eNode.has(CARD_ID_KEY) ? eNode.get(CARD_ID_KEY).asText() : "dummy-" + UUID.randomUUID();
            String name = eNode.has(NAME_KEY) ? eNode.get(NAME_KEY).asText() : "Energy";
            PokemonType energyType = eNode.has("energyType") ? PokemonType.valueOf(eNode.get("energyType").asText()) : PokemonType.COLORLESS;
            boolean basic = !eNode.has(BASIC_KEY) || eNode.get(BASIC_KEY).asBoolean();
            int energyCount = eNode.has(ENERGY_COUNT_KEY) ? eNode.get(ENERGY_COUNT_KEY).asInt(1) : 1;
            boolean providesAllTypes = eNode.has(PROVIDES_ALL_TYPES_KEY) && eNode.get(PROVIDES_ALL_TYPES_KEY).asBoolean();
            return new EnergyCard(cardId, name, energyType, basic, energyCount, providesAllTypes);
        }
    }

    public static class EnergyCardDeserializer extends JsonDeserializer<EnergyCard> {
        @Override
        public EnergyCard deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            if (node == null || node.isNull()) {
                return null;
            }
            String cardId = node.get(CARD_ID_KEY).asText();
            String name = node.get(NAME_KEY).asText();
            PokemonType energyType = PokemonType.valueOf(node.get("energyType").asText());
            boolean basic = node.has(BASIC_KEY) && node.get(BASIC_KEY).asBoolean();
            int energyCount = node.has(ENERGY_COUNT_KEY) ? node.get(ENERGY_COUNT_KEY).asInt(1) : 1;
            boolean providesAllTypes = node.has(PROVIDES_ALL_TYPES_KEY) && node.get(PROVIDES_ALL_TYPES_KEY).asBoolean();
            return new EnergyCard(cardId, name, energyType, basic, energyCount, providesAllTypes);
        }
    }
}
