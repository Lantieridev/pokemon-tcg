package ar.edu.utn.frc.tup.piii.services.persistence;

import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.*;
import ar.edu.utn.frc.tup.piii.engine.session.*;
import ar.edu.utn.frc.tup.piii.persistence.entity.MatchEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.MatchLogEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchLogRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MatchPersistenceTest {

    @Autowired
    private JpaGameStatePersistence persistence;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchLogRepository matchLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSerializationAndDeserializationWithCustomConverter() {
        MatchSessionJsonConverter converter = new MatchSessionJsonConverter();

        // 1. Build a complex MatchSession with polymorphic cards and StatusEffect
        PokemonCard pokemon = new PokemonCard.Builder("p-001", "Pikachu", 60, PokemonType.LIGHTNING)
                .evolutionStage(EvolutionStage.BASIC)
                .retreatCost(1)
                .build();
        TrainerCard trainer = new TrainerCard.Builder("t-001", "Professor's Research", TrainerType.SUPPORTER)
                .build();
        EnergyCard energy = new EnergyCard("e-001", "Lightning Energy", PokemonType.LIGHTNING, true);

        // Player A Setup
        InPlayPokemon activeA = new InPlayPokemon(pokemon);
        activeA.attachEnergy(PokemonType.LIGHTNING);
        activeA.addDamageCounters(20);

        Hand handA = new Hand();
        handA.addCard(pokemon);
        handA.addCard(trainer);
        handA.addCard(energy);

        Bench benchA = new Bench();
        InPlayPokemon benchPk = new InPlayPokemon(pokemon);
        benchA.place(benchPk);

        StatusEffectManager semA = new StatusEffectManager(() -> true);
        semA.apply(StatusEffectType.ENVENENADO);

        PlayerRuntime runtimeA = new PlayerRuntime(
                new Deck(List.of(pokemon)),
                handA,
                benchA,
                new DiscardPile(),
                semA,
                activeA
        );

        // Player B Setup
        InPlayPokemon activeB = new InPlayPokemon(pokemon);
        PlayerRuntime runtimeB = new PlayerRuntime(
                new Deck(List.of(pokemon)),
                new Hand(),
                new Bench(),
                new DiscardPile(),
                new StatusEffectManager(() -> true),
                activeB
        );

        // Board snapshots
        Map<BattlePokemonState, Integer> turnsInPlayMapA = new HashMap<>();
        turnsInPlayMapA.put(activeA, 3);
        turnsInPlayMapA.put(benchPk, 1);

        PlayerState psA = new PlayerState(activeA, List.of(benchPk), List.of("p-001"), 60, 6, turnsInPlayMapA);
        PlayerState psB = new PlayerState(activeB, List.of(), List.of(), 60, 6, new HashMap<>());

        MatchBoard board = new MatchBoard(List.of(psA, psB));

        MatchSession original = new MatchSession(
                "match-persist-test-123",
                List.of("player-alice", "player-bob"),
                board,
                List.of(runtimeA, runtimeB)
        );
        original.setActivePlayerIndex(0);
        original.setup();
        original.start();

        // 2. Serialize
        String json = converter.convertToDatabaseColumn(original);
        assertNotNull(json);

        // Assert coinFlipper field is NOT present in the serialized JSON
        assertFalse(json.contains("\"coinFlipper\""));
        assertFalse(json.contains("\"CoinFlipper\""));

        // 3. Deserialize
        MatchSession restored = converter.convertToEntityAttribute(json);
        assertNotNull(restored);
        assertEquals(original.getMatchId(), restored.getMatchId());
        assertEquals(original.getPlayerIds(), restored.getPlayerIds());
        assertEquals(original.getState(), restored.getState());
        assertEquals(original.getActivePlayerIndex(), restored.getActivePlayerIndex());

        // Check coinFlipper is successfully re-injected
        assertNotNull(restored.getCoinFlipper());

        // Verify player runtimes and cards
        PlayerRuntime restoredRuntimeA = restored.getPlayerRuntime(0);
        assertNotNull(restoredRuntimeA);
        assertEquals(3, restoredRuntimeA.getHand().size());

        // Verify status effects
        assertTrue(restoredRuntimeA.getStatusEffectManager().has(StatusEffectType.ENVENENADO));

        // Verify turnsInPlay reference identities
        PlayerState restoredPsA = restored.getBoard().getPlayerState(0);
        BattlePokemonState restoredActive = restoredPsA.getActivePokemon();
        assertNotNull(restoredActive);

        // turnsInPlay contains mapping for restoredActive
        assertEquals(3, restoredPsA.getTurnsInPlay(restoredActive));

        // The key in turnsInPlay map should be the EXACT same instance as restoredActive!
        boolean activeRefMatched = false;
        try {
            java.lang.reflect.Field field = PlayerState.class.getDeclaredField("turnsInPlay");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<BattlePokemonState, Integer> map = (Map<BattlePokemonState, Integer>) field.get(restoredPsA);
            for (BattlePokemonState key : map.keySet()) {
                if (key == restoredActive) {
                    activeRefMatched = true;
                }
            }
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
        assertTrue(activeRefMatched, "Active pokemon key in turnsInPlay map should be the same object instance as the active pokemon field");
    }

    @Test
    void testAsyncPersistenceAndDatabaseIntegration() throws InterruptedException {
        // Create a simple session to persist
        PokemonCard pokemon = new PokemonCard.Builder("p-002", "Charmander", 50, PokemonType.FIRE)
                .evolutionStage(EvolutionStage.BASIC)
                .build();
        InPlayPokemon active = new InPlayPokemon(pokemon);
        PlayerRuntime runtimeA = new PlayerRuntime(
                new Deck(List.of(pokemon)),
                new Hand(),
                new Bench(),
                new DiscardPile(),
                new StatusEffectManager(() -> true),
                active
        );
        PlayerRuntime runtimeB = new PlayerRuntime(
                new Deck(List.of(pokemon)),
                new Hand(),
                new Bench(),
                new DiscardPile(),
                new StatusEffectManager(() -> true),
                active
        );
        PlayerState psA = new PlayerState(active, List.of(), List.of(), 60, 6, new HashMap<>());
        PlayerState psB = new PlayerState(active, List.of(), List.of(), 60, 6, new HashMap<>());
        MatchBoard board = new MatchBoard(List.of(psA, psB));

        String matchId = "async-match-999";
        MatchSession session = new MatchSession(
                matchId,
                List.of("user-x", "user-y"),
                board,
                List.of(runtimeA, runtimeB)
        );

        // Time execution to verify non-blocking behavior (< 100ms, typically < 5ms)
        long startTime = System.currentTimeMillis();
        persistence.saveMatch(session);
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration < 100, "Persistence call should be non-blocking and return in less than 100ms (actual: " + duration + "ms)");

        // Wait up to 2 seconds for async listener to save it to database
        Long numericId = (long) Math.abs(matchId.hashCode());
        MatchEntity matchEntity = null;
        for (int i = 0; i < 20; i++) {
            Optional<MatchEntity> opt = matchRepository.findById(numericId);
            if (opt.isPresent() && opt.get().getCurrentState() != null) {
                matchEntity = opt.get();
                break;
            }
            Thread.sleep(100);
        }

        assertNotNull(matchEntity, "MatchEntity should be eventually saved asynchronously");
        assertEquals("WAITING", matchEntity.getStatus());
        assertEquals("user-x", userRepository.findById(matchEntity.getPlayer1().getId()).map(UserEntity::getUsername).orElse(null));
        assertEquals("user-y", userRepository.findById(matchEntity.getPlayer2().getId()).map(UserEntity::getUsername).orElse(null));

        // Test logging action asynchronously
        long logStartTime = System.currentTimeMillis();
        persistence.logAction(matchId, 2, "user-x", "ATTACK", "SUCCESS");
        long logDuration = System.currentTimeMillis() - logStartTime;

        assertTrue(logDuration < 100, "Log action call should be non-blocking and return in less than 100ms");

        // Wait up to 2 seconds for log entity to save
        MatchLogEntity logEntity = null;
        for (int i = 0; i < 20; i++) {
            List<MatchLogEntity> logs = matchLogRepository.findAll();
            Optional<MatchLogEntity> opt = logs.stream()
                    .filter(l -> l.getMatch().getId().equals(numericId) && l.getTurnNumber() == 2)
                    .findFirst();
            if (opt.isPresent()) {
                logEntity = opt.get();
                break;
            }
            Thread.sleep(100);
        }

        assertNotNull(logEntity, "MatchLogEntity should be eventually saved asynchronously");
        assertEquals("ATTACK", logEntity.getActionType());
        assertEquals("SUCCESS", logEntity.getResult());
        assertEquals("user-x", userRepository.findById(logEntity.getPlayer().getId()).map(UserEntity::getUsername).orElse(null));
    }
}
