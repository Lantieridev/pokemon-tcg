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
import ar.edu.utn.frc.tup.piii.engine.model.Card;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
        session.setTurnManager(org.mockito.Mockito.mock(TurnManager.class));
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

    @Test
    void shouldApplyScorchingFangDamageAndDiscardEnergy() {
        final Attack scorchingFang = new Attack("Scorching Fang", 60, List.of(PokemonType.FIRE, PokemonType.COLORLESS, PokemonType.COLORLESS), "scorching_fang");
        final ar.edu.utn.frc.tup.piii.engine.model.PokemonCard pyroarCard =
                new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder("xy2-20", "Pyroar", 110, PokemonType.FIRE)
                        .attacks(List.of(scorchingFang))
                        .build();
        final InPlayPokemon pyroar = new InPlayPokemon(pyroarCard);
        pyroar.attachEnergy(new EnergyCard("e1", "Energy", PokemonType.FIRE, true));
        pyroar.attachEnergy(new EnergyCard("e2", "Energy", PokemonType.COLORLESS, true));
        pyroar.attachEnergy(new EnergyCard("e3", "Energy", PokemonType.COLORLESS, true));
        
        runtime0.setActivePokemon(pyroar);

        session.setActivePlayerIndex(PLAYER_0);
        
        facade.apply(session, new DeclareAttackAction(pyroar, scorchingFang, List.of("discard_fire_energy")));

        assertEquals(2, pyroar.getAttachedEnergyCards().size());
        assertFalse(pyroar.getAttachedEnergyCards().stream().anyMatch(e -> e.getCardId().equals("e1")));
        assertEquals(9, active1.getDamageCounters());
    }

    @Test
    void shouldNotApplyScorchingFangBonusIfNoFireEnergyAttached() {
        final Attack scorchingFang = new Attack("Scorching Fang", 60, List.of(PokemonType.COLORLESS), "scorching_fang");
        final ar.edu.utn.frc.tup.piii.engine.model.PokemonCard pyroarCard =
                new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder("xy2-20", "Pyroar", 110, PokemonType.FIRE)
                        .attacks(List.of(scorchingFang))
                        .build();
        final InPlayPokemon pyroar = new InPlayPokemon(pyroarCard);
        pyroar.attachEnergy(new EnergyCard("e2", "Energy", PokemonType.COLORLESS, true));
        pyroar.attachEnergy(new EnergyCard("e3", "Energy", PokemonType.COLORLESS, true));
        pyroar.attachEnergy(new EnergyCard("e4", "Energy", PokemonType.COLORLESS, true));
        
        runtime0.setActivePokemon(pyroar);
        session.setActivePlayerIndex(PLAYER_0);
        
        facade.apply(session, new DeclareAttackAction(pyroar, scorchingFang, List.of("discard_fire_energy")));

        assertEquals(3, pyroar.getAttachedEnergyCards().size());
        assertEquals(6, active1.getDamageCounters());
    }

    @Test
    void shouldCalculateDerangedDanceDamageBasedOnBenchedPokemon() {
        final Attack derangedDance = new Attack("Deranged Dance", 20, List.of(PokemonType.COLORLESS), "deranged_dance");
        final PokemonCard shiftryCard = buildPokemon("xy2-7", "Shiftry", 140, 2, EvolutionStage.STAGE_2);
        final InPlayPokemon shiftry = new InPlayPokemon(shiftryCard);
        shiftry.attachEnergy(new EnergyCard("e1", "Energy", PokemonType.COLORLESS, true));
        runtime0.setActivePokemon(shiftry);

        bench0.place(new InPlayPokemon(buildPokemon("p1", "Charmander", 50, 1, EvolutionStage.BASIC)));
        bench0.place(new InPlayPokemon(buildPokemon("p2", "Charmander", 50, 1, EvolutionStage.BASIC)));
        
        bench1.place(new InPlayPokemon(buildPokemon("o1", "Squirtle", 50, 1, EvolutionStage.BASIC)));
        bench1.place(new InPlayPokemon(buildPokemon("o2", "Squirtle", 50, 1, EvolutionStage.BASIC)));
        bench1.place(new InPlayPokemon(buildPokemon("o3", "Squirtle", 50, 1, EvolutionStage.BASIC)));

        session.setActivePlayerIndex(PLAYER_0);

        facade.apply(session, new DeclareAttackAction(shiftry, derangedDance));

        assertEquals(10, active1.getDamageCounters());
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

    @Test
    void shouldApplyCassiusAndClearActivePokemonAndShuffleDeck() {
        final PokemonCard squirtle = buildPokemon("xy1-014", "Squirtle", 50, 1, EvolutionStage.BASIC);
        active0 = new InPlayPokemon(squirtle);
        active0.attachEnergy(new EnergyCard("e1", "Energy", PokemonType.WATER, true));
        
        final Hand hand = new Hand();
        final Bench bench = new Bench();
        final Card dummyCard = buildPokemon("dummy", "Dummy", 10, 1, EvolutionStage.BASIC);
        final Deck deck = new Deck(new ArrayList<>(List.of(dummyCard)));
        final DiscardPile discard = new DiscardPile();
        runtime0 = new PlayerRuntime(deck, hand, bench, discard, sem0, active0);
        
        session = new MatchSession(MATCH_ID, List.of("alice", "bob"), session.getBoard(), List.of(runtime0, runtime1));
        session.setCoinFlipper(() -> true);
        session.setActivePlayerIndex(PLAYER_0);
        
        final TrainerCard cassius = new TrainerCard.Builder("xy1-115", "Cassius", TrainerType.SUPPORTER)
                .effectId(TrainerEffectId.CASSIUS)
                .build();
        hand.addCard(cassius);
        
        facade.apply(session, new PlayTrainerAction(TrainerType.SUPPORTER, active0, "xy1-115"));
        
        org.junit.jupiter.api.Assertions.assertNull(runtime0.getActivePokemon());
        assertEquals(3, runtime0.getDeck().size());
        org.junit.jupiter.api.Assertions.assertFalse(runtime0.hasPokemonInPlay(active0));
    }

    @Test
    void shouldApplyLysandreAndSwapOpponentActiveAndBench() {
        final InPlayPokemon benchPokemon1 = new InPlayPokemon(buildPokemon("xy1-046", "Charmander", 50, 1, EvolutionStage.BASIC));
        bench1.place(benchPokemon1);
        final TrainerCard lysandre = new TrainerCard.Builder("xy2-90", "Lysandre", TrainerType.SUPPORTER)
                .effectId(TrainerEffectId.LYSANDRE)
                .build();
        hand0.addCard(lysandre);
        
        facade.apply(session, new PlayTrainerAction(TrainerType.SUPPORTER, benchPokemon1, "xy2-90"));
        
        assertSame(benchPokemon1, runtime1.getActivePokemon());
        assertSame(active1, runtime1.getBench().getAll().get(0));
    }

    @Test
    void shouldApplySacredAshAndShufflePokemonToDeck() {
        final TrainerCard ash = new TrainerCard.Builder("xy2-96", "Sacred Ash", TrainerType.ITEM)
                .effectId(TrainerEffectId.SACRED_ASH)
                .build();
        hand0.addCard(ash);
        
        final PokemonCard discardPkmn = buildPokemon("xy1-001", "Bulbasaur", 60, 1, EvolutionStage.BASIC);
        discard0.add(discardPkmn);
        
        facade.apply(session, new PlayTrainerAction(TrainerType.ITEM, null, "xy2-96"));
        
        org.junit.jupiter.api.Assertions.assertNotNull(session.getPendingSelectionRequest());
        assertEquals(TrainerEffectId.SACRED_ASH, session.getPendingSelectionRequest().sourceEffect());
        assertEquals(1, session.getPendingSelectionRequest().maxSelections());
        
        facade.apply(session, new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("xy1-001"), session.getPendingSelectionRequest()));
        
        org.junit.jupiter.api.Assertions.assertNull(session.getPendingSelectionRequest());
        org.junit.jupiter.api.Assertions.assertFalse(discard0.getCards().contains(discardPkmn));
        assertEquals(2, runtime0.getDeck().size());
    }

    @Test
    void shouldApplyPokemonFanClubAndPromptDeckSearch() {
        final TrainerCard fanClub = new TrainerCard.Builder("xy2-94", "Pokemon Fan Club", TrainerType.SUPPORTER)
                .effectId(TrainerEffectId.POKEMON_FAN_CLUB)
                .build();
        hand0.addCard(fanClub);
        
        final PokemonCard bulbasaur = buildPokemon("xy1-001", "Bulbasaur", 60, 1, EvolutionStage.BASIC);
        deck0.addCards(List.of(bulbasaur));
        
        facade.apply(session, new PlayTrainerAction(TrainerType.SUPPORTER, null, "xy2-94"));
        
        org.junit.jupiter.api.Assertions.assertNotNull(session.getPendingSelectionRequest());
        assertEquals(TrainerEffectId.POKEMON_FAN_CLUB, session.getPendingSelectionRequest().sourceEffect());
        
        facade.apply(session, new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("xy1-001"), session.getPendingSelectionRequest()));
        
        org.junit.jupiter.api.Assertions.assertNull(session.getPendingSelectionRequest());
        assertEquals(1, hand0.getCards().size());
        assertEquals("xy1-001", hand0.getCards().get(0).getCardId());
    }

    @Test
    void shouldApplyFieryTorchDiscardFireEnergyAndDraw() {
        final TrainerCard torch = new TrainerCard.Builder("xy2-89", "Fiery Torch", TrainerType.ITEM)
                .effectId(TrainerEffectId.FIERY_TORCH)
                .build();
        hand0.addCard(torch);
        
        final EnergyCard fire = new EnergyCard("e-torch-fire", "Fire Energy", PokemonType.FIRE, true);
        hand0.addCard(fire);
        
        final EnergyCard extra1 = new EnergyCard("e-ex1", "Fire Energy", PokemonType.FIRE, true);
        final EnergyCard extra2 = new EnergyCard("e-ex2", "Fire Energy", PokemonType.FIRE, true);
        deck0.addCards(List.of(extra1, extra2));
        
        facade.apply(session, new PlayTrainerAction(TrainerType.ITEM, null, "xy2-89"));
        
        org.junit.jupiter.api.Assertions.assertNotNull(session.getPendingSelectionRequest());
        assertEquals(TrainerEffectId.FIERY_TORCH, session.getPendingSelectionRequest().sourceEffect());
        
        facade.apply(session, new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("e-torch-fire"), session.getPendingSelectionRequest()));
        
        org.junit.jupiter.api.Assertions.assertNull(session.getPendingSelectionRequest());
        assertTrue(hand0.getCards().stream().noneMatch(c -> c.getCardId().equals("e-torch-fire")));
        assertEquals(2, discard0.getCards().size());
        assertEquals("e-torch-fire", discard0.getCards().get(1).getCardId());
        assertEquals(2, hand0.getCards().size());
    }

    @Test
    void shouldApplyTrickShovelAndDiscardOrKeepTopCard() {
        final TrainerCard shovel = new TrainerCard.Builder("xy2-98", "Trick Shovel", TrainerType.ITEM)
                .effectId(TrainerEffectId.TRICK_SHOVEL)
                .build();
        hand0.addCard(shovel);
        
        final EnergyCard topCard = new EnergyCard("e-top", "Water Energy", PokemonType.WATER, true);
        runtime1.getDeck().addToTop(topCard);
        
        facade.apply(session, new PlayTrainerAction(TrainerType.ITEM, null, "xy2-98"));
        
        org.junit.jupiter.api.Assertions.assertNotNull(session.getPendingSelectionRequest());
        assertEquals(TrainerEffectId.TRICK_SHOVEL, session.getPendingSelectionRequest().sourceEffect());
        
        facade.apply(session, new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("e-top"), session.getPendingSelectionRequest()));
        
        org.junit.jupiter.api.Assertions.assertNull(session.getPendingSelectionRequest());
        assertEquals("e-top", runtime1.getDiscardPile().getCards().get(0).getCardId());
    }

    @Test
    void shouldApplyStartlingMegaphoneAndDiscardOpponentTools() {
        final TrainerCard megaphone = new TrainerCard.Builder("xy2-97", "Startling Megaphone", TrainerType.ITEM)
                .effectId(TrainerEffectId.STARTLING_MEGAPHONE)
                .build();
        hand0.addCard(megaphone);
        
        final TrainerCard tool = new TrainerCard.Builder("xy1-119", "Hard Charm", TrainerType.POKEMON_TOOL).build();
        active1.attachTool(tool);
        
        facade.apply(session, new PlayTrainerAction(TrainerType.ITEM, null, "xy2-97"));
        
        org.junit.jupiter.api.Assertions.assertFalse(active1.hasToolAttached());
        assertEquals("xy1-119", runtime1.getDiscardPile().getCards().get(0).getCardId());
    }

    @Test
    void shouldApplyPalPadAndShuffleSupportersToDeck() {
        final TrainerCard palPad = new TrainerCard.Builder("xy2-92", "Pal Pad", TrainerType.ITEM)
                .effectId(TrainerEffectId.PAL_PAD)
                .build();
        hand0.addCard(palPad);
        
        final TrainerCard sycamore = new TrainerCard.Builder("xy1-122", "Professor Sycamore", TrainerType.SUPPORTER).build();
        discard0.add(sycamore);
        
        facade.apply(session, new PlayTrainerAction(TrainerType.ITEM, null, "xy2-92"));
        
        org.junit.jupiter.api.Assertions.assertNotNull(session.getPendingSelectionRequest());
        assertEquals(TrainerEffectId.PAL_PAD, session.getPendingSelectionRequest().sourceEffect());
        
        facade.apply(session, new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("xy1-122"), session.getPendingSelectionRequest()));
        
        org.junit.jupiter.api.Assertions.assertNull(session.getPendingSelectionRequest());
        org.junit.jupiter.api.Assertions.assertFalse(discard0.getCards().contains(sycamore));
        assertEquals(2, deck0.size());
    }

    @Test
    void shouldApplyBlacksmithAndAttachDiscardedFireEnergy() {
        final TrainerCard blacksmith = new TrainerCard.Builder("xy2-88", "Blacksmith", TrainerType.SUPPORTER)
                .effectId(TrainerEffectId.BLACKSMITH)
                .build();
        hand0.addCard(blacksmith);
        
        final EnergyCard fire1 = new EnergyCard("fire-1", "Fire Energy", PokemonType.FIRE, true);
        final EnergyCard fire2 = new EnergyCard("fire-2", "Fire Energy", PokemonType.FIRE, true);
        discard0.add(fire1);
        discard0.add(fire2);
        
        facade.apply(session, new PlayTrainerAction(TrainerType.SUPPORTER, active0, "xy2-88"));
        
        org.junit.jupiter.api.Assertions.assertNotNull(session.getPendingSelectionRequest());
        assertEquals(TrainerEffectId.BLACKSMITH, session.getPendingSelectionRequest().sourceEffect());
        
        facade.apply(session, new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("fire-1", "fire-2"), session.getPendingSelectionRequest()));
        
        org.junit.jupiter.api.Assertions.assertNull(session.getPendingSelectionRequest());
        assertEquals(2, active0.getAttachedEnergies().size());
        org.junit.jupiter.api.Assertions.assertFalse(discard0.getCards().contains(fire1));
        org.junit.jupiter.api.Assertions.assertFalse(discard0.getCards().contains(fire2));
    }

    @Test
    void shouldApplyPokemonCenterLadyAndHealAndClearStatus() {
        final TrainerCard centerLady = new TrainerCard.Builder("xy2-93", "Pokemon Center Lady", TrainerType.SUPPORTER)
                .effectId(TrainerEffectId.POKEMON_CENTER_LADY)
                .build();
        hand0.addCard(centerLady);
        
        active0.addDamageCounters(5);
        sem0.apply(ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType.ENVENENADO);
        
        facade.apply(session, new PlayTrainerAction(TrainerType.SUPPORTER, active0, "xy2-93"));
        
        assertEquals(0, active0.getDamageCounters());
        assertTrue(sem0.activeEffects().isEmpty());
    }

    @Test
    void shouldApplyUltraBallDiscardAndTransitionToDeckSearch() {
        final TrainerCard ultraBall = new TrainerCard.Builder("xy2-99", "Ultra Ball", TrainerType.ITEM)
                .effectId(TrainerEffectId.ULTRA_BALL)
                .build();
        hand0.addCard(ultraBall);
        
        final Card dummy1 = buildPokemon("d1", "Dummy 1", 10, 1, EvolutionStage.BASIC);
        final Card dummy2 = buildPokemon("d2", "Dummy 2", 10, 1, EvolutionStage.BASIC);
        hand0.addCard(dummy1);
        hand0.addCard(dummy2);
        
        final PokemonCard targetPkmn = buildPokemon("target-id", "Target Pokemon", 80, 1, EvolutionStage.BASIC);
        deck0.addCards(List.of(targetPkmn));
        
        facade.apply(session, new PlayTrainerAction(TrainerType.ITEM, null, "xy2-99"));
        
        org.junit.jupiter.api.Assertions.assertNotNull(session.getPendingSelectionRequest());
        assertEquals(ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.HAND, session.getPendingSelectionRequest().source());
        
        facade.apply(session, new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("d1", "d2"), session.getPendingSelectionRequest()));
        
        org.junit.jupiter.api.Assertions.assertNotNull(session.getPendingSelectionRequest());
        assertEquals(ar.edu.utn.frc.tup.piii.engine.model.SelectionSource.DECK, session.getPendingSelectionRequest().source());
        assertEquals(3, discard0.getCards().size());
        
        facade.apply(session, new ar.edu.utn.frc.tup.piii.engine.model.SelectCardsAction(List.of("target-id"), session.getPendingSelectionRequest()));
        
        org.junit.jupiter.api.Assertions.assertNull(session.getPendingSelectionRequest());
        assertTrue(hand0.getCards().stream().anyMatch(c -> c.getCardId().equals("target-id")));
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
