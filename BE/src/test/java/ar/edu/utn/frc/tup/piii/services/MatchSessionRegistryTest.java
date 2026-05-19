package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchSessionRegistryTest {

    private MatchSessionRegistry registry;
    private MatchSession session;

    @BeforeEach
    void setUp() {
        registry = new MatchSessionRegistry();
        session = buildSession("match-1", "player-A", "player-B");
    }

    @Test
    void shouldRegisterAndFindMatchSession() {
        registry.register(session);
        final Optional<MatchSession> found = registry.find("match-1");
        assertTrue(found.isPresent());
        assertEquals(session, found.get());
    }

    @Test
    void shouldReturnEmptyOptionalForUnknownMatchId() {
        final Optional<MatchSession> result = registry.find("unknown-match");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowWhenRegisteringDuplicateMatchId() {
        registry.register(session);
        final MatchSession duplicate = buildSession("match-1", "player-C", "player-D");
        assertThrows(IllegalArgumentException.class, () -> registry.register(duplicate));
    }

    @Test
    void shouldRemoveMatchSession() {
        registry.register(session);
        registry.remove("match-1");
        assertTrue(registry.find("match-1").isEmpty());
    }

    private MatchSession buildSession(final String matchId,
                                      final String p0Id,
                                      final String p1Id) {
        final FakeBattlePokemonState p0Active =
                new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false);
        final FakeBattlePokemonState p1Active =
                new FakeBattlePokemonState(100, PokemonType.WATER, null, null, false);

        final Map<BattlePokemonState, Integer> empty = new HashMap<>();
        final PlayerState ps0 = new PlayerState(p0Active, List.of(), 40, 6, empty);
        final PlayerState ps1 = new PlayerState(p1Active, List.of(), 40, 6, empty);
        final MatchBoard board = new MatchBoard(List.of(ps0, ps1));

        return new MatchSession(matchId, List.of(p0Id, p1Id), board);
    }
}
