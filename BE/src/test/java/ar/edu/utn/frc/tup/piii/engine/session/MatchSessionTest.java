package ar.edu.utn.frc.tup.piii.engine.session;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.infra.RandomCoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.Deck;
import ar.edu.utn.frc.tup.piii.engine.model.Bench;
import ar.edu.utn.frc.tup.piii.engine.model.DiscardPile;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage;
import ar.edu.utn.frc.tup.piii.engine.model.Hand;
import ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.exception.IllegalMatchStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void shouldThrowWhenResetForSuddenDeathCalledOnActiveSession() {
        session.setup();
        session.start();
        assertThrows(IllegalMatchStateTransitionException.class, session::resetForSuddenDeath);
    }

    @Test
    void shouldResetForSuddenDeathAndReturnToActiveState() {
        // Build a session that has the full runtime wiring needed for resetForSuddenDeath
        final PokemonCard basicCard0 = new PokemonCard.Builder("p0-1", "Charmander", 70, PokemonType.FIRE)
                .evolutionStage(EvolutionStage.BASIC).build();
        final PokemonCard basicCard1 = new PokemonCard.Builder("p1-1", "Squirtle", 60, PokemonType.WATER)
                .evolutionStage(EvolutionStage.BASIC).build();

        final List<Card> deckCards0 = new ArrayList<>();
        deckCards0.add(new PokemonCard.Builder("p0-2", "Char2", 70, PokemonType.FIRE)
                .evolutionStage(EvolutionStage.BASIC).build());
        for (int i = 0; i < 9; i++) {
            deckCards0.add(new EnergyCard("e0-" + i, "Fire Energy", PokemonType.FIRE, true));
        }

        final List<Card> deckCards1 = new ArrayList<>();
        deckCards1.add(new PokemonCard.Builder("p1-2", "Squirt2", 60, PokemonType.WATER)
                .evolutionStage(EvolutionStage.BASIC).build());
        for (int i = 0; i < 9; i++) {
            deckCards1.add(new EnergyCard("e1-" + i, "Water Energy", PokemonType.WATER, true));
        }

        final RandomCoinFlipper coinFlipper = new RandomCoinFlipper();
        final PlayerRuntime rt0 = new PlayerRuntime(
                new Deck(deckCards0), new Hand(), new Bench(), new DiscardPile(),
                new StatusEffectManager(coinFlipper),
                new InPlayPokemon(basicCard0),
                List.of());
        final PlayerRuntime rt1 = new PlayerRuntime(
                new Deck(deckCards1), new Hand(), new Bench(), new DiscardPile(),
                new StatusEffectManager(coinFlipper),
                new InPlayPokemon(basicCard1),
                List.of());

        final Map<BattlePokemonState, Integer> emptyMap = new HashMap<>();
        final PlayerState ps0 = new PlayerState(rt0.getActivePokemon(), List.of(), 10, 0, emptyMap);
        final PlayerState ps1 = new PlayerState(rt1.getActivePokemon(), List.of(), 10, 0, emptyMap);
        final MatchBoard board = new MatchBoard(List.of(ps0, ps1));

        final MatchSession sdSession = new MatchSession(
                "sd-match", List.of("pA", "pB"), board, List.of(rt0, rt1));
        sdSession.setCoinFlipper(coinFlipper);

        // Bring to FINISHED state
        sdSession.setup();
        sdSession.start();
        sdSession.finish();
        assertEquals(MatchSessionState.FINISHED, sdSession.getState());

        // Reset for Sudden Death — must not throw and must return to ACTIVE
        sdSession.resetForSuddenDeath();
        assertEquals(MatchSessionState.ACTIVE, sdSession.getState());

        // Both decks must now have at least 1 card (active pokemon returned + original deck cards)
        assertTrue(rt0.getDeck().size() > 0, "Player 0 deck must have cards after reset");
        assertTrue(rt1.getDeck().size() > 0, "Player 1 deck must have cards after reset");
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
