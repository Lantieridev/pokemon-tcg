package ar.edu.utn.frc.tup.piii.services.persistence;

import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.*;
import ar.edu.utn.frc.tup.piii.engine.session.*;
import ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto;
import ar.edu.utn.frc.tup.piii.dtos.RankingDto;
import ar.edu.utn.frc.tup.piii.persistence.entity.MatchEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.MatchLogEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchLogRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
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
        activeA.attachEnergy(energy);
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

    @Test
    void testDeclareWinnerAsynchronously() throws InterruptedException {
        String matchId = "winner-match-999";
        Long numericId = (long) Math.abs(matchId.hashCode());

        UserEntity player1 = userRepository.findByUsername("user-x").orElseGet(() ->
                userRepository.save(UserEntity.builder().username("user-x").email("x@x.com").password("pwd").build()));
        UserEntity player2 = userRepository.findByUsername("user-y").orElseGet(() ->
                userRepository.save(UserEntity.builder().username("user-y").email("y@y.com").password("pwd").build()));

        // Clean previous if any
        matchRepository.findById(numericId).ifPresent(matchRepository::delete);

        // Build match session
        PokemonCard pokemon = new PokemonCard.Builder("p-002", "Charmander", 50, PokemonType.FIRE)
                .evolutionStage(EvolutionStage.BASIC)
                .build();
        InPlayPokemon active = new InPlayPokemon(pokemon);
        PlayerState psA = new PlayerState(active, List.of(), List.of(), 60, 6, new HashMap<>());
        PlayerState psB = new PlayerState(active, List.of(), List.of(), 60, 6, new HashMap<>());
        MatchBoard board = new MatchBoard(List.of(psA, psB));

        MatchSession session = new MatchSession(
                matchId,
                List.of("user-x", "user-y"),
                board
        );
        session.setWinnerId("user-y");
        session.setup();
        session.start();
        session.finish();

        // Save match
        persistence.saveMatch(session);

        MatchEntity updatedMatch = null;
        for (int i = 0; i < 20; i++) {
            Optional<MatchEntity> opt = matchRepository.findById(numericId);
            if (opt.isPresent() && opt.get().getWinner() != null) {
                updatedMatch = opt.get();
                break;
            }
            Thread.sleep(100);
        }

        assertNotNull(updatedMatch, "Winner should be eventually set asynchronously through saveMatch");
        assertNotNull(updatedMatch.getWinner());
        assertEquals("user-y", userRepository.findById(updatedMatch.getWinner().getId()).map(UserEntity::getUsername).orElse(null));
    }

    @Test
    void testGetGlobalRankingIntegration() {
        // Clear all matches to have a controlled database state for pagination/ranking verification
        matchLogRepository.deleteAll();
        matchRepository.deleteAll();

        final UserEntity alice = userRepository.findByUsername("user-x").orElseGet(() ->
                userRepository.save(UserEntity.builder().username("user-x").email("x@x.com").password("pwd").build()));
        final UserEntity bob = userRepository.findByUsername("user-y").orElseGet(() ->
                userRepository.save(UserEntity.builder().username("user-y").email("y@y.com").password("pwd").build()));
        final UserEntity charlie = userRepository.findByUsername("user-z").orElseGet(() ->
                userRepository.save(UserEntity.builder().username("user-z").email("z@z.com").password("pwd").build()));

        // Seed matches:
        // Match 1: Finished, winner is Alice
        matchRepository.save(MatchEntity.builder().id(2001L).status("FINISHED").player1(alice).player2(bob).winner(alice).build());
        // Match 2: Finished, winner is Alice
        matchRepository.save(MatchEntity.builder().id(2002L).status("FINISHED").player1(alice).player2(charlie).winner(alice).build());
        // Match 3: Finished, winner is Bob
        matchRepository.save(MatchEntity.builder().id(2003L).status("FINISHED").player1(bob).player2(charlie).winner(bob).build());
        // Match 4: Active (ignored), winner is Alice (simulating inconsistent data)
        matchRepository.save(MatchEntity.builder().id(2004L).status("ACTIVE").player1(alice).player2(bob).winner(alice).build());
        // Match 5: Finished, winner is null (ignored)
        matchRepository.save(MatchEntity.builder().id(2005L).status("FINISHED").player1(alice).player2(bob).winner(null).build());

        // Call repository
        final Slice<RankingDto> ranking = matchRepository.getGlobalRanking(PageRequest.of(0, 10));

        // Assertions
        assertNotNull(ranking);
        final List<RankingDto> content = ranking.getContent();
        // Alice should have 2 wins, Bob 1 win. Charlie 0 wins (not in list).
        assertEquals(2, content.size());

        // Check order (DESC)
        assertEquals("user-x", content.get(0).username());
        assertEquals(2L, content.get(0).wins());

        assertEquals("user-y", content.get(1).username());
        assertEquals(1L, content.get(1).wins());
    }

    @Test
    void testGetUserMatchHistoryIntegration() {
        // Clear all matches to have controlled database state
        matchLogRepository.deleteAll();
        matchRepository.deleteAll();

        final UserEntity alice = userRepository.findByUsername("user-x").orElseGet(() ->
                userRepository.save(UserEntity.builder().username("user-x").email("x@x.com").password("pwd").build()));
        final UserEntity bob = userRepository.findByUsername("user-y").orElseGet(() ->
                userRepository.save(UserEntity.builder().username("user-y").email("y@y.com").password("pwd").build()));
        final UserEntity charlie = userRepository.findByUsername("user-z").orElseGet(() ->
                userRepository.save(UserEntity.builder().username("user-z").email("z@z.com").password("pwd").build()));

        // Seed matches:
        // Match 1: Alice vs Bob, finished, Alice wins
        matchRepository.save(MatchEntity.builder().id(3001L).status("FINISHED").player1(alice).player2(bob).winner(alice).createdAt(java.time.LocalDateTime.now().minusMinutes(10)).build());
        // Match 2: Bob vs Charlie, finished, Bob wins
        matchRepository.save(MatchEntity.builder().id(3002L).status("FINISHED").player1(bob).player2(charlie).winner(bob).createdAt(java.time.LocalDateTime.now().minusMinutes(5)).build());
        // Match 3: Alice vs Charlie, ACTIVE (in progress), winner null
        matchRepository.save(MatchEntity.builder().id(3003L).status("ACTIVE").player1(alice).player2(charlie).winner(null).createdAt(java.time.LocalDateTime.now()).build());

        // Get history for Alice: should contain Match 1 and Match 3, but NOT Match 2
        final Slice<MatchHistoryProjectionDto> rawHistoryAlice = matchRepository.findUserMatchHistory("user-x", PageRequest.of(0, 10));
        assertNotNull(rawHistoryAlice);
        final List<MatchHistoryProjectionDto> listAlice = rawHistoryAlice.getContent();
        // Alice matches: 3001, 3003 (total: 2 matches)
        assertEquals(2, listAlice.size());

        // Assert sorting DESC: 3003 (saved last) should be first, 3001 (saved first) second
        assertEquals(3003L, listAlice.get(0).id());
        assertEquals("ACTIVE", listAlice.get(0).status());
        assertEquals("user-x", listAlice.get(0).player1Username());
        assertEquals("user-z", listAlice.get(0).player2Username());
        assertNull(listAlice.get(0).winnerUsername());

        assertEquals(3001L, listAlice.get(1).id());
        assertEquals("FINISHED", listAlice.get(1).status());
        assertEquals("user-x", listAlice.get(1).player1Username());
        assertEquals("user-y", listAlice.get(1).player2Username());
        assertEquals("user-x", listAlice.get(1).winnerUsername());
    }
}


