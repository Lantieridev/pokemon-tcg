package ar.edu.utn.frc.tup.piii.engine.session;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.exception.IllegalMatchStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatchSessionTest {

    private MatchSession session;

    @BeforeEach
    void setUp() {
        final FakeBattlePokemonState p0Active =
                new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false);
        final FakeBattlePokemonState p1Active =
                new FakeBattlePokemonState(100, PokemonType.WATER, null, null, false);

        final Map<BattlePokemonState, Integer> emptyMap = new HashMap<>();

        final PlayerState ps0 = new PlayerState(p0Active, List.of(), 40, 6, emptyMap);
        final PlayerState ps1 = new PlayerState(p1Active, List.of(), 40, 6, emptyMap);
        final MatchBoard board = new MatchBoard(List.of(ps0, ps1));

        session = new MatchSession("match-1", List.of("player-A", "player-B"), board);
    }

    @Test
    void shouldStartInWaitingState() {
        assertEquals(MatchSessionState.WAITING, session.getState());
    }

    @Test
    void shouldTransitionFromWaitingToSetupWhenSetupCalled() {
        session.setup();
        assertEquals(MatchSessionState.SETUP, session.getState());
    }

    @Test
    void shouldTransitionFromSetupToActiveWhenStartCalled() {
        session.setup();
        session.start();
        assertEquals(MatchSessionState.ACTIVE, session.getState());
    }

    @Test
    void shouldThrowWhenStartCalledDirectlyFromWaiting() {
        assertThrows(IllegalMatchStateTransitionException.class, session::start);
    }

    @Test
    void shouldTransitionToFinishedWhenFinished() {
        session.setup();
        session.start();
        session.finish();
        assertEquals(MatchSessionState.FINISHED, session.getState());
    }

    @Test
    void shouldThrowWhenStartCalledOnActiveSession() {
        session.setup();
        session.start();
        assertThrows(IllegalMatchStateTransitionException.class, session::start);
    }

    @Test
    void shouldThrowWhenStartCalledOnFinishedSession() {
        session.setup();
        session.start();
        session.finish();
        assertThrows(IllegalMatchStateTransitionException.class, session::start);
    }

    @Test
    void shouldThrowWhenFinishCalledOnWaitingSession() {
        assertThrows(IllegalMatchStateTransitionException.class, session::finish);
    }

    @Test
    void shouldThrowWhenConstructedWithNullMatchId() {
        assertThrows(NullPointerException.class,
                () -> new MatchSession(null, List.of("p1", "p2"), buildBoard()));
    }

    @Test
    void shouldThrowWhenConstructedWithNullPlayerIds() {
        assertThrows(NullPointerException.class,
                () -> new MatchSession("m1", null, buildBoard()));
    }

    @Test
    void shouldThrowWhenConstructedWithNullBoard() {
        assertThrows(NullPointerException.class,
                () -> new MatchSession("m1", List.of("p1", "p2"), null));
    }

    private MatchBoard buildBoard() {
        final FakeBattlePokemonState active =
                new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false);
        final Map<BattlePokemonState, Integer> map = new HashMap<>();
        final PlayerState ps0 = new PlayerState(active, List.of(), 40, 6, map);
        final PlayerState ps1 = new PlayerState(active, List.of(), 40, 6, map);
        return new MatchBoard(List.of(ps0, ps1));
    }
}
