package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.AttachEnergyAction;
import ar.edu.utn.frc.tup.piii.engine.model.Bench;
import ar.edu.utn.frc.tup.piii.engine.model.Deck;
import ar.edu.utn.frc.tup.piii.engine.model.DeclareAttackAction;
import ar.edu.utn.frc.tup.piii.engine.model.DiscardPile;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.EvolveAction;
import ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage;
import ar.edu.utn.frc.tup.piii.engine.model.Hand;
import ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.engine.model.PlaceBasicPokemonAction;
import ar.edu.utn.frc.tup.piii.engine.model.PlayTrainerAction;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.RetreatAction;
import ar.edu.utn.frc.tup.piii.engine.model.PromoteActiveAction;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.manager.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.model.MainPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for GameFacade.apply() — one test per ActionType.
 */
class GameFacadeApplyTest {

    private static final int PLAYER_0 = 0;
    private static final int PLAYER_1 = 1;
    private static final String MATCH_ID = "match-001";

    private GameFacade facade;
    private Hand hand0;
    private Bench bench0;
    private Deck deck0;
    private DiscardPile discard0;
    private StatusEffectManager sem0;
    private InPlayPokemon active0;

    private Hand hand1;
    private Bench bench1;
    private StatusEffectManager sem1;
    private InPlayPokemon active1;

    private PlayerRuntime runtime0;
    private PlayerRuntime runtime1;
    private MatchSession session;

    @BeforeEach
    void setUp() {
        facade = new GameFacade();

        // Player 0 setup
        active0 = new InPlayPokemon(buildPokemon("xy1-006", "Charizard", 120, 2, EvolutionStage.STAGE_2));
        hand0 = new Hand();
        bench0 = new Bench();
        deck0 = new Deck(List.of(new EnergyCard("e-001", "Fire Energy", PokemonType.FIRE, true)));
        discard0 = new DiscardPile();
        sem0 = new StatusEffectManager(() -> true);
        runtime0 = new PlayerRuntime(deck0, hand0, bench0, discard0, sem0, active0);

        // Player 1 setup
        active1 = new InPlayPokemon(buildPokemon("xy1-010", "Blastoise", 150, 3, EvolutionStage.STAGE_2));
        hand1 = new Hand();
        bench1 = new Bench();
        final Deck deck1 = new Deck(List.of(new EnergyCard("e-002", "Water Energy", PokemonType.WATER, true)));
        final DiscardPile discard1 = new DiscardPile();
        sem1 = new StatusEffectManager(() -> true);
        runtime1 = new PlayerRuntime(deck1, hand1, bench1, discard1, sem1, active1);

        // Board (snapshot — not used for apply logic, but required by MatchSession)
        final FakeBattlePokemonState fakeActive0 = new FakeBattlePokemonState(120, PokemonType.FIRE, PokemonType.WATER, null, false);
        final FakeBattlePokemonState fakeActive1 = new FakeBattlePokemonState(150, PokemonType.WATER, null, null, false);
        final Map<ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState, Integer> t0 = new HashMap<>();
        t0.put(fakeActive0, 1);
        final Map<ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState, Integer> t1 = new HashMap<>();
        t1.put(fakeActive1, 1);
        final PlayerState ps0 = new PlayerState(fakeActive0, List.of(), 30, 4, t0);
        final PlayerState ps1 = new PlayerState(fakeActive1, List.of(), 30, 6, t1);
        final MatchBoard board = new MatchBoard(List.of(ps0, ps1));

        session = new MatchSession(MATCH_ID, List.of("alice", "bob"), board, List.of(runtime0, runtime1));
        session.setCoinFlipper(() -> true);
        session.setActivePlayerIndex(PLAYER_0);
    }

    // --- PlaceBasicPokemonAction ---

    @Test
    void shouldMovePokemonFromHandToBench() {
        final PokemonCard charmander = buildPokemon("xy1-046", "Charmander", 50, 1, EvolutionStage.BASIC);
        hand0.addCard(charmander);

        facade.apply(session, new PlaceBasicPokemonAction("xy1-046"));

        assertEquals(0, hand0.size(), "hand should be empty after placing");
        assertEquals(1, bench0.size(), "bench should have 1 pokemon");
    }

    // --- AttachEnergyAction ---

    @Test
    void shouldAttachEnergyFromHandToActivePokemon() {
        final EnergyCard fireEnergy = new EnergyCard("xy1-133", "Fire Energy", PokemonType.FIRE, true);
        hand0.addCard(fireEnergy);

        facade.apply(session, new AttachEnergyAction(active0, PokemonType.FIRE));

        assertEquals(0, hand0.size(), "energy card removed from hand");
        assertEquals(1, active0.getAttachedEnergies().size(), "1 energy attached to active");
        assertEquals(PokemonType.FIRE, active0.getAttachedEnergies().get(0));
    }

    // --- EvolveAction ---

    @Test
    void shouldRemoveEvolutionCardFromHandAndClearStatus() {
        final PokemonCard charmeleon = buildPokemon("xy1-023", "Charmeleon", 80, 1, EvolutionStage.STAGE_1);
        final InPlayPokemon charmeleonInPlay = new InPlayPokemon(charmeleon);
        hand0.addCard(charmeleon);

        facade.apply(session, new EvolveAction(active0, charmeleon));

        assertEquals(0, hand0.size(), "evolution card removed from hand");
    }

    // --- RetreatAction ---

    @Test
    void shouldSwapActivePokemonWithBenchOnRetreat() {
        // active0 has retreat cost 2, attach 2 energies
        active0.attachEnergy(new EnergyCard("e1", "Energy", PokemonType.FIRE, true));
        active0.attachEnergy(new EnergyCard("e2", "Energy", PokemonType.FIRE, true));

        final InPlayPokemon benchPokemon = new InPlayPokemon(buildPokemon("xy1-046", "Charmander", 50, 1, EvolutionStage.BASIC));
        bench0.place(benchPokemon);

        facade.apply(session, new RetreatAction(active0, 0, java.util.List.of(0, 1)));

        assertSame(benchPokemon, runtime0.getActivePokemon(), "bench pokemon promoted to active");
        assertEquals(1, bench0.size(), "old active moved to bench");
        assertEquals(0, active0.getAttachedEnergies().size(), "retreat cost energies discarded");
    }

    // --- DeclareAttackAction ---

    @Test
    void shouldApplyDamageToDefenderViaAttackPipeline() {
        final Attack flamethrower = new Attack("Flamethrower", 90, List.of(PokemonType.FIRE, PokemonType.FIRE));
        active0.attachEnergy(new EnergyCard("e1", "Energy", PokemonType.FIRE, true));
        active0.attachEnergy(new EnergyCard("e2", "Energy", PokemonType.FIRE, true));

        session.setActivePlayerIndex(PLAYER_0);
        facade.apply(session, new DeclareAttackAction(active0, flamethrower));

        assertTrue(active1.getDamageCounters() > 0, "defender should have taken damage");
    }

    // --- PlayTrainerAction: STADIUM ---

    @Test
    void shouldRegisterStadiumOnBoard() {
        final ar.edu.utn.frc.tup.piii.engine.model.TrainerCard stadium =
                new ar.edu.utn.frc.tup.piii.engine.model.TrainerCard.Builder("xy1-117", "Fairy Garden", TrainerType.STADIUM)
                        .build();
        hand0.addCard(stadium);

        facade.apply(session, new PlayTrainerAction(TrainerType.STADIUM, null, "xy1-117"));

        assertEquals("xy1-117", session.getBoard().getActiveStadium().getCardId());
        assertEquals(0, hand0.size(), "stadium card removed from hand");
    }

    // --- PlayTrainerAction: POKEMON_TOOL ---

    @Test
    void shouldAttachToolToTargetPokemon() {
        final ar.edu.utn.frc.tup.piii.engine.model.TrainerCard tool =
                new ar.edu.utn.frc.tup.piii.engine.model.TrainerCard.Builder("xy1-119", "Hard Charm", TrainerType.POKEMON_TOOL)
                        .build();
        hand0.addCard(tool);

        facade.apply(session, new PlayTrainerAction(TrainerType.POKEMON_TOOL, active0, "xy1-119"));

        assertTrue(active0.hasToolAttached(), "tool should be attached to target");
        assertEquals(0, hand0.size(), "tool card removed from hand");
    }

    @Test
    void shouldRecordMainPhaseTurnLimitsWhenTurnManagerProvided() {
        final TurnManager turnManager = new TurnManager();
        turnManager.startTurn(PLAYER_0);
        turnManager.endDraw();

        // 1. Energy limit recording test
        final EnergyCard fireEnergy = new EnergyCard("xy1-133", "Fire Energy", PokemonType.FIRE, true);
        hand0.addCard(fireEnergy);

        facade.apply(session, new AttachEnergyAction(active0, PokemonType.FIRE), turnManager);

        assertEquals(1, turnManager.requireMainPhase().getEnergyAttached(), "Energy attachment should be recorded in MainPhase");

        // 2. Retreat limit recording test
        active0.attachEnergy(new EnergyCard("e1", "Energy", PokemonType.FIRE, true));
        active0.attachEnergy(new EnergyCard("e2", "Energy", PokemonType.FIRE, true));
        final InPlayPokemon benchPokemon = new InPlayPokemon(buildPokemon("xy1-046", "Charmander", 50, 1, EvolutionStage.BASIC));
        bench0.place(benchPokemon);

        facade.apply(session, new RetreatAction(active0, 0, java.util.List.of(0, 1)), turnManager);

        assertTrue(turnManager.requireMainPhase().isRetreatUsed(), "Retreat should be recorded in MainPhase");

        // 3. Supporter limit recording test
        final ar.edu.utn.frc.tup.piii.engine.model.TrainerCard supporter =
                new ar.edu.utn.frc.tup.piii.engine.model.TrainerCard.Builder("xy1-118", "Professor Sycamore", TrainerType.SUPPORTER)
                        .build();
        hand0.addCard(supporter);

        facade.apply(session, new PlayTrainerAction(TrainerType.SUPPORTER, null, "xy1-118"), turnManager);

        assertTrue(turnManager.requireMainPhase().isSupporterPlayed(), "Supporter play should be recorded in MainPhase");

        // 4. Stadium limit recording test
        final ar.edu.utn.frc.tup.piii.engine.model.TrainerCard stadium =
                new ar.edu.utn.frc.tup.piii.engine.model.TrainerCard.Builder("xy1-117", "Fairy Garden", TrainerType.STADIUM)
                        .build();
        hand0.addCard(stadium);

        facade.apply(session, new PlayTrainerAction(TrainerType.STADIUM, null, "xy1-117"), turnManager);

        assertTrue(turnManager.requireMainPhase().isStadiumPlayed(), "Stadium play should be recorded in MainPhase");
    }

    // --- PlayTrainerAction: RED_CARD ---

    @Test
    void shouldShuffleOpponentHandIntoDeckAndDrawFourWhenRedCardPlayed() {
        // Opponent (Player 1) has 3 cards in hand; deck has 1 card (from setUp)
        // Total after shuffle: 1 + 3 = 4 → draw 4 → hand1 = 4, deck1 = 0
        hand1.addCard(new EnergyCard("h1-1", "Water Energy", PokemonType.WATER, true));
        hand1.addCard(new EnergyCard("h1-2", "Water Energy", PokemonType.WATER, true));
        hand1.addCard(new EnergyCard("h1-3", "Water Energy", PokemonType.WATER, true));

        final TrainerCard redCard = new TrainerCard.Builder("xy1-124", "Red Card", TrainerType.SUPPORTER)
                .effectId(TrainerEffectId.RED_CARD)
                .build();
        hand0.addCard(redCard);

        facade.apply(session, new PlayTrainerAction(TrainerType.SUPPORTER, null, "xy1-124"));

        assertEquals(4, hand1.size(), "Opponent must draw exactly 4 cards after Red Card");
        // deck1 should be empty (1 original + 3 hand - 4 drawn = 0)
        assertEquals(0, runtime1.getDeck().size(), "Opponent's deck exhausted after draw");
    }

    @Test
    void shouldDiscardOneEnergyFromOpponentActiveWhenTeamFlareGruntPlayed() {
        // Attach 2 energies to opponent's active Pokémon
        active1.attachEnergy(new EnergyCard("e-op1", "Water Energy", PokemonType.WATER, true));
        active1.attachEnergy(new EnergyCard("e-op2", "Water Energy", PokemonType.WATER, true));

        final TrainerCard grunt = new TrainerCard.Builder("xy1-129", "Team Flare Grunt", TrainerType.SUPPORTER)
                .effectId(TrainerEffectId.TEAM_FLARE_GRUNT)
                .build();
        hand0.addCard(grunt);

        facade.apply(session, new PlayTrainerAction(TrainerType.SUPPORTER, null, "xy1-129"));

        assertEquals(1, active1.getAttachedEnergies().size(),
                "Team Flare Grunt must remove exactly 1 energy from opponent's Active");
    }

    @Test
    void shouldNotRemoveEnergyWhenTeamFlareGruntPlayedAndOpponentHasNoEnergy() {
        // active1 has no energies attached by default
        final TrainerCard grunt = new TrainerCard.Builder("xy1-129", "Team Flare Grunt", TrainerType.SUPPORTER)
                .effectId(TrainerEffectId.TEAM_FLARE_GRUNT)
                .build();
        hand0.addCard(grunt);

        facade.apply(session, new PlayTrainerAction(TrainerType.SUPPORTER, null, "xy1-129"));

        assertEquals(0, active1.getAttachedEnergies().size(),
                "No energy to discard — active should remain at 0");
    }

    @Test
    void shouldPromoteActiveUsingPromotingPlayerIndexWhenAwaitingPromotion() {
        // Player 1 needs to promote.
        // Create a benched Pokémon for Player 1 (Bob).
        final InPlayPokemon benchPokemon1 = new InPlayPokemon(buildPokemon("xy1-046", "Charmander", 50, 1, EvolutionStage.BASIC));
        bench1.place(benchPokemon1);

        // Clear active Pokémon for Player 1 so they must promote.
        runtime1.clearActivePokemon();

        // Set match session to awaiting promotion for Player 1 (Bob)
        session.setAwaitingPromotion(PLAYER_1);

        // The active turn player index is PLAYER_0 (Alice).
        session.setActivePlayerIndex(PLAYER_0);

        // Apply PromoteActiveAction for Player 1's bench index 0
        final TurnManager turnManager = new TurnManager();
        facade.apply(session, new PromoteActiveAction(0), turnManager);

        // Assert that Player 1's active Pokémon is now set to benchPokemon1,
        // and Player 0's active Pokémon remains untouched.
        assertSame(benchPokemon1, runtime1.getActivePokemon(), "Player 1's benched Pokémon should be promoted");
        assertSame(active0, runtime0.getActivePokemon(), "Player 0's active Pokémon should remain unchanged");
    }

    // --- helpers ---

    private static PokemonCard buildPokemon(final String id, final String name,
                                             final int hp, final int retreatCost,
                                             final EvolutionStage stage) {
        return new PokemonCard.Builder(id, name, hp, PokemonType.FIRE)
                .retreatCost(retreatCost)
                .evolutionStage(stage)
                .build();
    }
}
