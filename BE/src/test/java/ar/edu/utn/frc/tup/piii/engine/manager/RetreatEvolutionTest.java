package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.*;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerState;
import ar.edu.utn.frc.tup.piii.services.GameFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class RetreatEvolutionTest {

    private static final int PLAYER_0 = 0;
    private static final String MATCH_ID = "match-001";

    private GameFacade facade;
    private RuleValidator validator;
    private MatchSession session;
    private PlayerRuntime runtime0;
    private TurnManager turnManager;

    private InPlayPokemon active0;
    private InPlayPokemon bench0;

    @BeforeEach
    void setUp() {
        facade = new GameFacade();
        turnManager = Mockito.mock(TurnManager.class);
        when(turnManager.activePlayerIndex()).thenReturn(PLAYER_0);
        when(turnManager.isFirstTurnOfPlayer(PLAYER_0)).thenReturn(false);
        when(turnManager.requireMainPhase()).thenReturn(new MainPhase());

        // Player 0 setup
        PokemonCard charmander = new PokemonCard.Builder("xy1-046", "Charmander", 50, PokemonType.FIRE)
                .retreatCost(0) // Free retreat for simplicity
                .evolutionStage(EvolutionStage.BASIC)
                .build();
        active0 = new InPlayPokemon(charmander);

        PokemonCard basicBench = new PokemonCard.Builder("xy1-003", "Weedle", 40, PokemonType.GRASS)
                .evolutionStage(EvolutionStage.BASIC)
                .build();
        bench0 = new InPlayPokemon(basicBench);

        Hand hand0 = new Hand();
        // Add evolution cards to hand
        PokemonCard charmeleon = new PokemonCard.Builder("xy1-023", "Charmeleon", 80, PokemonType.FIRE)
                .evolutionStage(EvolutionStage.STAGE_1)
                .evolvesFrom("Charmander")
                .build();
        PokemonCard kakuna = new PokemonCard.Builder("xy1-004", "Kakuna", 60, PokemonType.GRASS)
                .evolutionStage(EvolutionStage.STAGE_1)
                .evolvesFrom("Weedle")
                .build();
        hand0.addCard(charmeleon);
        hand0.addCard(kakuna);

        Bench player0Bench = new Bench();
        player0Bench.place(bench0);

        Deck deck0 = new Deck(List.of(new EnergyCard("dummy-energy-1", "Fire Energy", PokemonType.FIRE, true)));
        DiscardPile discard0 = new DiscardPile();
        StatusEffectManager sem0 = new StatusEffectManager(() -> true);

        runtime0 = new PlayerRuntime(deck0, hand0, player0Bench, discard0, sem0, active0);

        // Player 1 setup (dummy)
        InPlayPokemon active1 = new InPlayPokemon(new PokemonCard.Builder("dummy", "Dummy", 100, PokemonType.COLORLESS).build());
        Deck deck1 = new Deck(List.of(new EnergyCard("dummy-energy-2", "Fire Energy", PokemonType.FIRE, true)));
        PlayerRuntime runtime1 = new PlayerRuntime(deck1, new Hand(), new Bench(), new DiscardPile(), new StatusEffectManager(() -> true), active1);

        // Map turns in play. We simulate that they have been in play for 1 turn (already started)
        Map<BattlePokemonState, Integer> t0 = new HashMap<>();
        t0.put(active0, 1);
        t0.put(bench0, 1);

        PlayerState ps0 = new PlayerState(new FakeBattlePokemonState(50, PokemonType.FIRE, null, null, false), List.of(), 30, 4, t0);
        PlayerState ps1 = new PlayerState(new FakeBattlePokemonState(100, PokemonType.COLORLESS, null, null, false), List.of(), 30, 6, new HashMap<>());
        MatchBoard board = new MatchBoard(List.of(ps0, ps1));
        board.bindRuntimes(List.of(runtime0, runtime1));

        session = new MatchSession(MATCH_ID, List.of("alice", "bob"), board, List.of(runtime0, runtime1));
        session.setCoinFlipper(() -> true);
        session.setActivePlayerIndex(PLAYER_0);
        session.setTurnManager(turnManager);

        // Set live turns in play in runtimes
        runtime0.recordPokemonEntered(active0);
        runtime0.incrementAllTurnsInPlay(); // sets active0 to 1 turn in play
        runtime0.recordPokemonEntered(bench0);
        runtime0.incrementAllTurnsInPlay(); // sets active0 to 2, bench0 to 1

        validator = new RuleValidator(
                turnManager,
                List.of(sem0, runtime1.getStatusEffectManager()),
                board,
                board,
                board,
                board
        );

        // Mock setup covers turn phase and state
    }

    @Test
    void testRetreatedPokemonCannotEvolveInSameTurn() {
        // Assert that initially active0 can evolve (has >0 turns in play)
        PokemonCard charmeleon = (PokemonCard) runtime0.getHand().getCards().stream()
                .filter(c -> c.getCardId().equals("xy1-023"))
                .findFirst().orElseThrow();
        ValidationResult beforeRetreat = validator.validate(new EvolveAction(active0, charmeleon));
        assertInstanceOf(ValidationResult.Valid.class, beforeRetreat);

        // Perform retreat: active0 moves to bench, bench0 (Weedle) becomes active
        facade.apply(session, new RetreatAction(active0, 0, List.of()), turnManager);

        // Confirm active0 is on bench
        assertSame(active0, runtime0.getBench().getAll().get(0));

        // Attempt to evolve active0 (Charmander) on the bench in the same turn
        ValidationResult afterRetreat = validator.validate(new EvolveAction(active0, charmeleon));
        assertInstanceOf(ValidationResult.Invalid.class, afterRetreat);
    }

    @Test
    void testPromotedPokemonCanEvolveImmediately() {
        // Perform retreat: active0 moves to bench, bench0 (Weedle) becomes active
        facade.apply(session, new RetreatAction(active0, 0, List.of()), turnManager);

        // bench0 is now the active Pokémon (promoted from bench)
        assertSame(bench0, runtime0.getActivePokemon());

        // Attempt to evolve bench0 (Weedle) in the active position immediately
        PokemonCard kakuna = (PokemonCard) runtime0.getHand().getCards().stream()
                .filter(c -> c.getCardId().equals("xy1-004"))
                .findFirst().orElseThrow();
        ValidationResult promoteEvolve = validator.validate(new EvolveAction(bench0, kakuna));
        assertInstanceOf(ValidationResult.Valid.class, promoteEvolve);
    }
}
