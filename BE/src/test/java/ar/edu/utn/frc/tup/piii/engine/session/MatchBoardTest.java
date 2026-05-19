package ar.edu.utn.frc.tup.piii.engine.session;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatchBoardTest {

    private FakeBattlePokemonState p0Active;
    private FakeBattlePokemonState p0Benched;
    private FakeBattlePokemonState p1Active;
    private PlayerState playerState0;
    private PlayerState playerState1;
    private MatchBoard board;

    @BeforeEach
    void setUp() {
        p0Active = new FakeBattlePokemonState(100, PokemonType.FIRE, PokemonType.WATER, null, false);
        p0Benched = new FakeBattlePokemonState(80, PokemonType.WATER, null, null, false);
        p1Active = new FakeBattlePokemonState(120, PokemonType.GRASS, PokemonType.FIRE, null, true);

        final Map<BattlePokemonState, Integer> p0TurnsMap = new HashMap<>();
        p0TurnsMap.put(p0Active, 2);
        p0TurnsMap.put(p0Benched, 1);

        playerState0 = new PlayerState(p0Active, List.of(p0Benched), 40, 4, p0TurnsMap);

        final Map<BattlePokemonState, Integer> p1TurnsMap = new HashMap<>();
        p1TurnsMap.put(p1Active, 0);

        playerState1 = new PlayerState(p1Active, List.of(), 35, 6, p1TurnsMap);

        board = new MatchBoard(List.of(playerState0, playerState1));
    }

    @Test
    void shouldReturnActiveP0WhenP0IsQueried() {
        assertEquals(p0Active, board.getActivePokemon(0));
    }

    @Test
    void shouldReturnActiveP1WhenP1IsQueried() {
        assertEquals(p1Active, board.getActivePokemon(1));
    }

    @Test
    void shouldReturnBenchSizeForPlayer() {
        assertEquals(1, board.getBenchSize(0));
        assertEquals(0, board.getBenchSize(1));
    }

    @Test
    void shouldReturnBenchedPokemonListForPlayer() {
        final List<BattlePokemonState> bench = board.getBenchedPokemon(0);
        assertEquals(1, bench.size());
        assertEquals(p0Benched, bench.get(0));
    }

    @Test
    void shouldReturnDeckSizeForPlayer() {
        assertEquals(40, board.getDeckSize(0));
        assertEquals(35, board.getDeckSize(1));
    }

    @Test
    void shouldReturnPrizeCountForPlayer() {
        assertEquals(4, board.getRemainingPrizes(0));
        assertEquals(6, board.getRemainingPrizes(1));
    }

    @Test
    void shouldReturnTurnsInPlayForPokemon() {
        assertEquals(2, board.getTurnsInPlay(p0Active));
        assertEquals(1, board.getTurnsInPlay(p0Benched));
        assertEquals(0, board.getTurnsInPlay(p1Active));
    }

    @Test
    void shouldThrowWhenConstructedWithNullPlayers() {
        assertThrows(NullPointerException.class, () -> new MatchBoard(null));
    }

    @Test
    void shouldThrowWhenConstructedWithWrongPlayerCount() {
        assertThrows(IllegalArgumentException.class,
                () -> new MatchBoard(List.of(playerState0)));
    }
}
