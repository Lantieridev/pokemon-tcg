package ar.edu.utn.frc.tup.piii.services.persistence;

import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.*;
import ar.edu.utn.frc.tup.piii.engine.session.*;
import ar.edu.utn.frc.tup.piii.engine.infra.RandomCoinFlipper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.*;

@Converter(autoApply = false)
public class MatchSessionJsonConverter implements AttributeConverter<MatchSession, String> {

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
            e.printStackTrace();
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
            while (true) {
                if (cleanData.startsWith("[") && cleanData.endsWith("]")) {
                    JsonNode arrayNode = OBJECT_MAPPER.readTree(cleanData);
                    if (arrayNode.isArray() && arrayNode.size() == 1) {
                        cleanData = arrayNode.get(0).asText().trim();
                        continue;
                    }
                }
                if (cleanData.startsWith("\"") && cleanData.endsWith("\"")) {
                    try {
                        cleanData = OBJECT_MAPPER.readValue(cleanData, String.class).trim();
                        continue;
                    } catch (IOException e) {
                        // ignore and break
                    }
                }
                break;
            }
            return OBJECT_MAPPER.readValue(cleanData, MatchSession.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON string to MatchSession", e);
        }
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
            String cardId = node.get("cardId").asText();
            String name = node.get("name").asText();
            int hp = node.get("hp").asInt();
            PokemonType pokemonType = PokemonType.valueOf(node.get("pokemonType").asText());

            PokemonCard.Builder builder = new PokemonCard.Builder(cardId, name, hp, pokemonType);
            if (node.has("weaknessType") && !node.get("weaknessType").isNull()) {
                builder.weaknessType(PokemonType.valueOf(node.get("weaknessType").asText()));
            }
            if (node.has("resistanceType") && !node.get("resistanceType").isNull()) {
                builder.resistanceType(PokemonType.valueOf(node.get("resistanceType").asText()));
            }
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
            if (node.has("attacks")) {
                List<Attack> attacks = new ArrayList<>();
                for (JsonNode attNode : node.get("attacks")) {
                    attacks.add(p.getCodec().treeToValue(attNode, Attack.class));
                }
                builder.attacks(attacks);
            }
            return builder.build();
        }
    }

    public static class TrainerCardDeserializer extends JsonDeserializer<TrainerCard> {
        @Override
        public TrainerCard deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            String cardId = node.get("cardId").asText();
            String name = node.get("name").asText();
            TrainerType trainerType = TrainerType.valueOf(node.get("trainerType").asText());

            TrainerCard.Builder builder = new TrainerCard.Builder(cardId, name, trainerType);
            if (node.has("aceSpec")) {
                builder.aceSpec(node.get("aceSpec").asBoolean());
            }
            if (node.has("effectText")) {
                builder.effectText(node.get("effectText").asText());
            }
            if (node.has("effectId") && !node.get("effectId").isNull()) {
                builder.effectId(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.valueOf(node.get("effectId").asText()));
            }
            if (node.has("toolEffectId") && !node.get("toolEffectId").isNull()) {
                builder.toolEffectId(ar.edu.utn.frc.tup.piii.engine.model.PokemonToolEffectId.valueOf(node.get("toolEffectId").asText()));
            }
            return builder.build();
        }
    }

    public static class PlayerStateSerializer extends JsonSerializer<PlayerState> {
        @Override
        public void serialize(PlayerState value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            if (value.getActivePokemon() != null) {
                gen.writeObjectField("activePokemon", value.getActivePokemon());
            } else {
                gen.writeNullField("activePokemon");
            }

            gen.writeArrayFieldStart("bench");
            for (BattlePokemonState pk : value.getBench()) {
                if (pk != null) {
                    gen.writeObject(pk);
                } else {
                    gen.writeNull();
                }
            }
            gen.writeEndArray();

            gen.writeObjectField("hand", value.getHand());
            gen.writeObjectField("activeAttacks", value.getActiveAttacks());
            gen.writeNumberField("deckSize", value.getDeckSize());
            gen.writeNumberField("prizeCount", value.getPrizeCount());

            // turnsInPlay using location markers
            gen.writeObjectFieldStart("turnsInPlay");
            if (value.getActivePokemon() != null) {
                int activeTurns = value.getTurnsInPlay(value.getActivePokemon());
                if (activeTurns > 0) {
                    gen.writeNumberField("active", activeTurns);
                }
            }
            List<BattlePokemonState> bench = value.getBench();
            for (int i = 0; i < bench.size(); i++) {
                BattlePokemonState pk = bench.get(i);
                int benchTurns = value.getTurnsInPlay(pk);
                if (benchTurns > 0) {
                    gen.writeNumberField("bench_" + i, benchTurns);
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

            BattlePokemonState activePokemon = null;
            if (node.has("activePokemon") && !node.get("activePokemon").isNull()) {
                activePokemon = p.getCodec().treeToValue(node.get("activePokemon"), BattlePokemonState.class);
            }

            List<BattlePokemonState> bench = new ArrayList<>();
            if (node.has("bench")) {
                for (JsonNode bNode : node.get("bench")) {
                    bench.add(p.getCodec().treeToValue(bNode, BattlePokemonState.class));
                }
            }

            List<String> hand = new ArrayList<>();
            if (node.has("hand")) {
                for (JsonNode hNode : node.get("hand")) {
                    hand.add(hNode.asText());
                }
            }

            List<Attack> activeAttacks = new ArrayList<>();
            if (node.has("activeAttacks")) {
                for (JsonNode aNode : node.get("activeAttacks")) {
                    activeAttacks.add(p.getCodec().treeToValue(aNode, Attack.class));
                }
            }

            int deckSize = node.get("deckSize").asInt();
            int prizeCount = node.get("prizeCount").asInt();

            Map<BattlePokemonState, Integer> turnsInPlay = new java.util.HashMap<>();
            if (node.has("turnsInPlay")) {
                JsonNode turnsNode = node.get("turnsInPlay");
                Iterator<Map.Entry<String, JsonNode>> fields = turnsNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String key = field.getKey();
                    int val = field.getValue().asInt();
                    if ("active".equals(key)) {
                        if (activePokemon != null) {
                            turnsInPlay.put(activePokemon, val);
                        }
                    } else if (key.startsWith("bench_")) {
                        try {
                            int index = Integer.parseInt(key.substring(6));
                            if (index >= 0 && index < bench.size()) {
                                turnsInPlay.put(bench.get(index), val);
                            }
                        } catch (NumberFormatException e) {
                            // ignore invalid keys
                        }
                    }
                }
            }

            return new PlayerState(activePokemon, bench, hand, activeAttacks, deckSize, prizeCount, turnsInPlay);
        }
    }

    public static class MatchBoardSerializer extends JsonSerializer<MatchBoard> {
        @Override
        public void serialize(MatchBoard value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            if (value.getActiveStadium() != null) {
                gen.writeObjectField("activeStadium", value.getActiveStadium());
            } else {
                gen.writeNullField("activeStadium");
            }
            try {
                java.lang.reflect.Field field = MatchBoard.class.getDeclaredField("players");
                field.setAccessible(true);
                List<PlayerState> players = (List<PlayerState>) field.get(value);
                gen.writeObjectField("players", players);
            } catch (Exception e) {
                e.printStackTrace();
                throw new IOException("Failed to serialize MatchBoard.players", e);
            }
            gen.writeEndObject();
        }
    }

    public static class MatchBoardDeserializer extends JsonDeserializer<MatchBoard> {
        @Override
        public MatchBoard deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            List<PlayerState> players = new ArrayList<>();
            if (node.has("players")) {
                for (JsonNode pNode : node.get("players")) {
                    players.add(p.getCodec().treeToValue(pNode, PlayerState.class));
                }
            }
            MatchBoard board = new MatchBoard(players);
            if (node.has("activeStadium") && !node.get("activeStadium").isNull()) {
                board.replaceStadium(p.getCodec().treeToValue(node.get("activeStadium"), TrainerCard.class));
            }
            return board;
        }
    }

    public static class PlayerRuntimeSerializer extends JsonSerializer<PlayerRuntime> {
        @Override
        public void serialize(PlayerRuntime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeObjectField("deck", value.getDeck());
            gen.writeObjectField("hand", value.getHand());
            gen.writeObjectField("bench", value.getBench());
            gen.writeObjectField("discardPile", value.getDiscardPile());
            gen.writeObjectField("statusEffectManager", value.getStatusEffectManager());
            if (value.getActivePokemon() != null) {
                gen.writeObjectField("activePokemon", value.getActivePokemon());
            } else {
                gen.writeNullField("activePokemon");
            }
            // Serialize prizePile
            gen.writeArrayFieldStart("prizePile");
            for (Card card : value.getPrizePile()) {
                if (card != null) {
                    gen.writeObject(card);
                } else {
                    gen.writeNull();
                }
            }
            gen.writeEndArray();

            // Serialize turnsInPlay using location markers
            gen.writeObjectFieldStart("turnsInPlay");
            if (value.getActivePokemon() != null) {
                int activeTurns = value.getTurnsInPlay(value.getActivePokemon());
                gen.writeNumberField("active", activeTurns);
            }
            List<BattlePokemonState> bench = value.getBench().getAll();
            for (int i = 0; i < bench.size(); i++) {
                BattlePokemonState pk = bench.get(i);
                if (pk != null) {
                    int benchTurns = value.getTurnsInPlay(pk);
                    gen.writeNumberField("bench_" + i, benchTurns);
                }
            }
            gen.writeEndObject();

            gen.writeEndObject();
        }
    }

    public static class PlayerRuntimeDeserializer extends JsonDeserializer<PlayerRuntime> {
        @Override
        public PlayerRuntime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            Deck deck = p.getCodec().treeToValue(node.get("deck"), Deck.class);
            Hand hand = p.getCodec().treeToValue(node.get("hand"), Hand.class);
            Bench bench = p.getCodec().treeToValue(node.get("bench"), Bench.class);
            DiscardPile discardPile = p.getCodec().treeToValue(node.get("discardPile"), DiscardPile.class);
            StatusEffectManager statusEffectManager = p.getCodec().treeToValue(node.get("statusEffectManager"), StatusEffectManager.class);
            BattlePokemonState activePokemon = p.getCodec().treeToValue(node.get("activePokemon"), BattlePokemonState.class);

            List<Card> prizePile = new ArrayList<>();
            if (node.has("prizePile") && !node.get("prizePile").isNull()) {
                for (JsonNode pNode : node.get("prizePile")) {
                    prizePile.add(p.getCodec().treeToValue(pNode, Card.class));
                }
            }

            Map<BattlePokemonState, Integer> turnsInPlay = new java.util.HashMap<>();
            if (node.has("turnsInPlay") && !node.get("turnsInPlay").isNull()) {
                JsonNode turnsNode = node.get("turnsInPlay");
                Iterator<Map.Entry<String, JsonNode>> fields = turnsNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String key = field.getKey();
                    int val = field.getValue().asInt();
                    if ("active".equals(key)) {
                        if (activePokemon != null) {
                            turnsInPlay.put(activePokemon, val);
                        }
                    } else if (key.startsWith("bench_")) {
                        try {
                            int index = Integer.parseInt(key.substring(6));
                            List<BattlePokemonState> benched = bench.getAll();
                            if (index >= 0 && index < benched.size()) {
                                turnsInPlay.put(benched.get(index), val);
                            }
                        } catch (NumberFormatException e) {
                            // ignore invalid keys
                        }
                    }
                }
            }

            return new PlayerRuntime(deck, hand, bench, discardPile, statusEffectManager, activePokemon, prizePile, turnsInPlay);
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
            try {
                java.lang.reflect.Field field = Deck.class.getDeclaredField("cards");
                field.setAccessible(true);
                List<Card> cards = (List<Card>) field.get(value);
                gen.writeStartObject();
                gen.writeArrayFieldStart("cards");
                for (Card card : cards) {
                    if (card != null) {
                        gen.writeObject(card);
                    } else {
                        gen.writeNull();
                    }
                }
                gen.writeEndArray();
                gen.writeEndObject();
            } catch (Exception e) {
                throw new IOException("Failed to serialize Deck", e);
            }
        }
    }

    public static class DeckDeserializer extends JsonDeserializer<Deck> {
        @Override
        public Deck deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            List<Card> cards = new ArrayList<>();
            if (node.has("cards")) {
                for (JsonNode cNode : node.get("cards")) {
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
            gen.writeArrayFieldStart("cards");
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
            if (node.has("cards")) {
                for (JsonNode cNode : node.get("cards")) {
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
            gen.writeArrayFieldStart("cards");
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
            if (node.has("cards")) {
                for (JsonNode cNode : node.get("cards")) {
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
            gen.writeStringField("matchId", value.getMatchId());
            gen.writeObjectField("playerIds", value.getPlayerIds());
            gen.writeObjectField("board", value.getBoard());
            gen.writeStringField("state", value.getState().name());
            gen.writeNumberField("activePlayerIndex", value.getActivePlayerIndex());
            gen.writeNumberField("version", value.getVersion());
            if (value.getWinnerId() != null) {
                gen.writeStringField("winnerId", value.getWinnerId());
            } else {
                gen.writeNullField("winnerId");
            }
            if (value.getVictoryReason() != null) {
                gen.writeStringField("victoryReason", value.getVictoryReason());
            } else {
                gen.writeNullField("victoryReason");
            }
            if (value.getPendingSelectionRequest() != null) {
                gen.writeObjectField("pendingSelectionRequest", value.getPendingSelectionRequest());
            } else {
                gen.writeNullField("pendingSelectionRequest");
            }

            try {
                java.lang.reflect.Field runtimesField = MatchSession.class.getDeclaredField("playerRuntimes");
                runtimesField.setAccessible(true);
                List<PlayerRuntime> runtimes = (List<PlayerRuntime>) runtimesField.get(value);
                gen.writeObjectField("playerRuntimes", runtimes);
            } catch (Exception e) {
                throw new IOException("Failed to serialize MatchSession.playerRuntimes", e);
            }

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
            String matchId = node.has("matchId") && !node.get("matchId").isNull() ? node.get("matchId").asText() : null;

            List<String> playerIds = new ArrayList<>();
            if (node.has("playerIds")) {
                for (JsonNode idNode : node.get("playerIds")) {
                    playerIds.add(idNode.asText());
                }
            }

            MatchBoard board = p.getCodec().treeToValue(node.get("board"), MatchBoard.class);

            List<PlayerRuntime> playerRuntimes = null;
            if (node.has("playerRuntimes") && !node.get("playerRuntimes").isNull()) {
                playerRuntimes = new ArrayList<>();
                for (JsonNode rNode : node.get("playerRuntimes")) {
                    playerRuntimes.add(p.getCodec().treeToValue(rNode, PlayerRuntime.class));
                }
            }

            MatchSessionState state = node.has("state") && !node.get("state").isNull()
                    ? MatchSessionState.valueOf(node.get("state").asText())
                    : MatchSessionState.WAITING;
            int activePlayerIndex = node.has("activePlayerIndex") && !node.get("activePlayerIndex").isNull()
                    ? node.get("activePlayerIndex").asInt()
                    : -1;
            String winnerId = node.has("winnerId") && !node.get("winnerId").isNull()
                    ? node.get("winnerId").asText()
                    : null;
            String victoryReason = node.has("victoryReason") && !node.get("victoryReason").isNull()
                    ? node.get("victoryReason").asText()
                    : null;
            long version = node.has("version") && !node.get("version").isNull()
                    ? node.get("version").asLong()
                    : 1L;
            ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest pendingSelectionRequest = node.has("pendingSelectionRequest") && !node.get("pendingSelectionRequest").isNull()
                    ? p.getCodec().treeToValue(node.get("pendingSelectionRequest"), ar.edu.utn.frc.tup.piii.engine.model.PendingSelectionRequest.class)
                    : null;

            if (board != null && playerRuntimes != null) {
                board.bindRuntimes(playerRuntimes);
            }
            MatchSession session = new MatchSession(matchId, playerIds, board, playerRuntimes);

            try {
                java.lang.reflect.Field stateField = MatchSession.class.getDeclaredField("state");
                stateField.setAccessible(true);
                stateField.set(session, state);

                java.lang.reflect.Field activePlayerField = MatchSession.class.getDeclaredField("activePlayerIndex");
                activePlayerField.setAccessible(true);
                activePlayerField.set(session, activePlayerIndex);

                java.lang.reflect.Field winnerIdField = MatchSession.class.getDeclaredField("winnerId");
                winnerIdField.setAccessible(true);
                winnerIdField.set(session, winnerId);

                java.lang.reflect.Field victoryReasonField = MatchSession.class.getDeclaredField("victoryReason");
                victoryReasonField.setAccessible(true);
                victoryReasonField.set(session, victoryReason);

                java.lang.reflect.Field versionField = MatchSession.class.getDeclaredField("version");
                versionField.setAccessible(true);
                versionField.set(session, version);

                java.lang.reflect.Field pendingReqField = MatchSession.class.getDeclaredField("pendingSelectionRequest");
                pendingReqField.setAccessible(true);
                pendingReqField.set(session, pendingSelectionRequest);
            } catch (Exception e) {
                throw new IOException("Failed to restore MatchSession state fields", e);
            }

            // Re-inject the default CoinFlipper
            session.setCoinFlipper(new RandomCoinFlipper());

            return session;
        }
    }

    public static class InPlayPokemonSerializer extends JsonSerializer<InPlayPokemon> {
        @Override
        public void serialize(InPlayPokemon value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("@type", "inPlay");
            gen.writeObjectField("card", value.getCard());
            gen.writeNumberField("damageCounters", value.getDamageCounters());
            gen.writeObjectField("attachedEnergies", value.getAttachedEnergies());
            gen.writeObjectField("attachedEnergyCards", value.getAttachedEnergyCards());
            if (value.getAttachedTool().isPresent()) {
                gen.writeObjectField("attachedTool", value.getAttachedTool().get());
            } else {
                gen.writeNullField("attachedTool");
            }
            gen.writeEndObject();
        }

        @Override
        public void serializeWithType(InPlayPokemon value, JsonGenerator gen, SerializerProvider serializers, com.fasterxml.jackson.databind.jsontype.TypeSerializer typeSer) throws IOException {
            com.fasterxml.jackson.core.type.WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen,
                    typeSer.typeId(value, com.fasterxml.jackson.core.JsonToken.START_OBJECT));
            gen.writeObjectField("card", value.getCard());
            gen.writeNumberField("damageCounters", value.getDamageCounters());
            gen.writeObjectField("attachedEnergies", value.getAttachedEnergies());
            gen.writeObjectField("attachedEnergyCards", value.getAttachedEnergyCards());
            if (value.getAttachedTool().isPresent()) {
                gen.writeObjectField("attachedTool", value.getAttachedTool().get());
            } else {
                gen.writeNullField("attachedTool");
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
            List<PokemonType> attachedEnergies = new ArrayList<>();
            if (node.has("attachedEnergies")) {
                for (JsonNode eNode : node.get("attachedEnergies")) {
                    attachedEnergies.add(PokemonType.valueOf(eNode.asText()));
                }
            }
            List<EnergyCard> attachedEnergyCards = new ArrayList<>();
            if (node.has("attachedEnergyCards") && !node.get("attachedEnergyCards").isNull()) {
                for (JsonNode eNode : node.get("attachedEnergyCards")) {
                    if (eNode.isTextual()) {
                        attachedEnergyCards.add(new EnergyCard("dummy-" + UUID.randomUUID(), eNode.asText() + " Energy", PokemonType.valueOf(eNode.asText()), true));
                    } else {
                        String cardId = eNode.has("cardId") ? eNode.get("cardId").asText() : "dummy-" + UUID.randomUUID();
                        String name = eNode.has("name") ? eNode.get("name").asText() : "Energy";
                        PokemonType energyType = eNode.has("energyType") ? PokemonType.valueOf(eNode.get("energyType").asText()) : PokemonType.COLORLESS;
                        boolean basic = !eNode.has("basic") || eNode.get("basic").asBoolean();
                        int energyCount = eNode.has("energyCount") ? eNode.get("energyCount").asInt(1) : 1;
                        boolean providesAllTypes = eNode.has("providesAllTypes") && eNode.get("providesAllTypes").asBoolean();
                        attachedEnergyCards.add(new EnergyCard(cardId, name, energyType, basic, energyCount, providesAllTypes));
                    }
                }
            } else {
                for (PokemonType type : attachedEnergies) {
                    attachedEnergyCards.add(new EnergyCard("dummy-" + UUID.randomUUID(), type.name() + " Energy", type, true));
                }
            }
            TrainerCard tool = null;
            if (node.has("attachedTool") && !node.get("attachedTool").isNull()) {
                tool = p.getCodec().treeToValue(node.get("attachedTool"), TrainerCard.class);
            }
            return new InPlayPokemon(card, damageCounters, attachedEnergies, attachedEnergyCards, tool);
        }
    }

    public static class EnergyCardDeserializer extends JsonDeserializer<EnergyCard> {
        @Override
        public EnergyCard deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            if (node == null || node.isNull()) {
                return null;
            }
            String cardId = node.get("cardId").asText();
            String name = node.get("name").asText();
            PokemonType energyType = PokemonType.valueOf(node.get("energyType").asText());
            boolean basic = node.has("basic") && node.get("basic").asBoolean();
            int energyCount = node.has("energyCount") ? node.get("energyCount").asInt(1) : 1;
            boolean providesAllTypes = node.has("providesAllTypes") && node.get("providesAllTypes").asBoolean();
            return new EnergyCard(cardId, name, energyType, basic, energyCount, providesAllTypes);
        }
    }
}
