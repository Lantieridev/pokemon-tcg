package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.ActionType;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.persistence.mapper.CardMapper;
import ar.edu.utn.frc.tup.piii.persistence.repository.CardRepository;
import ar.edu.utn.frc.tup.piii.services.MatchCreationService;
import ar.edu.utn.frc.tup.piii.services.MatchService;
import ar.edu.utn.frc.tup.piii.services.MatchSessionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.jdbc.Sql;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class XY1EndToEndIntegrationTest {

    @Autowired
    private MatchService matchService;

    @Autowired
    private MatchCreationService matchCreationService;

    @Autowired
    private MatchSessionRegistry sessionRegistry;

    @Autowired
    private CardRepository cardRepository;

    private CardMapper cardMapper = new CardMapper(new ObjectMapper());

    private MatchSession session;
    private String matchId;

    private Card resolveCard(String id) {
        ar.edu.utn.frc.tup.piii.persistence.entity.CardEntity entity = cardRepository.findById(id).orElseThrow();
        Card mapped = cardMapper.map(entity);
        if (mapped instanceof PokemonCard pc) {
            System.err.println("MAPPED POKEMON CARD " + id + " ATTACKS SIZE: " + pc.getAttacks().size());
        }
        return mapped;
    }

    @BeforeEach
    void setUp() {
        // Prepare Deck A (Grass / Trainers)
        List<Card> deckA = new ArrayList<>();
        // Add Basics so Mulligan is less likely
        for (int i = 0; i < 4; i++) {
            deckA.add(resolveCard("xy1-1")); // Venusaur-EX
            deckA.add(resolveCard("xy1-3")); // Weedle
        }
        deckA.add(resolveCard("xy1-2")); // M Venusaur-EX
        deckA.add(resolveCard("xy1-4")); // Kakuna
        deckA.add(resolveCard("xy1-5")); // Beedrill
        deckA.add(resolveCard("xy1-118")); // Great Ball
        deckA.add(resolveCard("xy1-123")); // Prof Letter
        
        // Add 40 Energies
        for (int i = 0; i < 40; i++) {
            deckA.add(new EnergyCard("basic-grass-a-" + i, "Grass Energy", PokemonType.GRASS, true));
        }
        
        // Prepare Deck B (Grass / Trainers)
        List<Card> deckB = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            deckB.add(resolveCard("xy1-15")); // Scatterbug
        }
        deckB.add(resolveCard("xy1-16")); // Spewpa
        deckB.add(resolveCard("xy1-17")); // Vivillon
        deckB.add(resolveCard("xy1-116")); // Evosoda
        
        // Add 40 Energies
        for (int i = 0; i < 40; i++) {
            deckB.add(new EnergyCard("basic-grass-b-" + i, "Grass Energy", PokemonType.GRASS, true));
        }

        matchId = matchCreationService.createMatch("playerA",  "playerB",  deckA,  deckB, false);
        session = sessionRegistry.find(matchId).orElseThrow();
        
        // We override the hands with specific setups so we can control the deterministic test
        session.getPlayerRuntime(0).getHand().removeAll();
        session.getPlayerRuntime(0).getHand().addCard(resolveCard("xy1-1")); // Venusaur-EX
        session.getPlayerRuntime(0).getHand().addCard(resolveCard("xy1-3")); // Weedle
        session.getPlayerRuntime(0).getHand().addCard(resolveCard("xy1-4")); // Kakuna
        session.getPlayerRuntime(0).getHand().addCard(resolveCard("xy1-118")); // Great Ball
        session.getPlayerRuntime(0).getHand().addCard(new EnergyCard("basic-grass-test1", "Grass Energy", PokemonType.GRASS, true));
        
        session.getPlayerRuntime(1).getHand().removeAll();
        session.getPlayerRuntime(1).getHand().addCard(resolveCard("xy1-15")); // Scatterbug
        session.getPlayerRuntime(1).getDeck().addCards(java.util.List.of(resolveCard("xy1-16"))); // Spewpa in deck for Evosoda
        session.getPlayerRuntime(1).getHand().addCard(resolveCard("xy1-116")); // Evosoda
        session.getPlayerRuntime(1).getHand().addCard(new EnergyCard("basic-grass-test2", "Grass Energy", PokemonType.GRASS, true));

        // Replace active pokemon manually because we messed with hands after setup()
        ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState active0 = new ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon((PokemonCard)resolveCard("xy1-3"));
        session.getPlayerRuntime(0).setActivePokemon(active0); // Weedle
        session.getPlayerRuntime(0).recordPokemonEntered(active0);
        
        ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState active1 = new ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon((PokemonCard)resolveCard("xy1-15"));
        session.getPlayerRuntime(1).setActivePokemon(active1); // Scatterbug
        session.getPlayerRuntime(1).recordPokemonEntered(active1);
        
        session.getPlayerRuntime(0).getBench().removeAll();
        session.getPlayerRuntime(1).getBench().removeAll();
    }
    
    @Test
    void testFullMatchFlow() {
        // Find who goes first according to the engine state
        int activeIndex = session.getTurnManager().activePlayerIndex();
        String activePlayerId = session.getPlayerIds().get(activeIndex);
        
        // Since coin flip is random during start(), we check the index dynamically
        if (activeIndex != 0) {
            // Force Player A to go first without advancing turn counts
            session.getTurnManager().reset();
            session.getTurnManager().startTurn(0);
            activeIndex = session.getTurnManager().activePlayerIndex();
            activePlayerId = session.getPlayerIds().get(activeIndex);
        }
        
        // Now Player A is active (index 0)
        assertThat(activeIndex).isEqualTo(0);
        assertThat(session.getTurnManager().currentPhase() instanceof ar.edu.utn.frc.tup.piii.engine.model.MainPhase).isTrue();
        
        // --- TURN (Player A) ---
        // Player A: Places a Venusaur-EX on the Bench
        ActionRequestDTO placeVenusaur = new ActionRequestDTO(ActionType.PLACE_BASIC_POKEMON, "xy1-1", null, null, null, null, null, Collections.emptyList(), null, Collections.emptyList());
        matchService.processAction(matchId, activePlayerId, placeVenusaur);
        assertThat(session.getPlayerRuntime(activeIndex).getBench().getAll()).hasSize(1);
        
        // Player A: Plays Great Ball
        ActionRequestDTO playGreatBall = new ActionRequestDTO(ActionType.PLAY_TRAINER, "xy1-118", null, null, TrainerType.ITEM, null, null, Collections.emptyList(), null, Collections.emptyList());
        matchService.processAction(matchId, activePlayerId, playGreatBall);
        
        // The game should be in ActionResolutionPhase waiting for a SelectCardsAction
        assertThat(session.getTurnManager().currentPhase() instanceof ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase).isTrue();
        assertThat(session.getPendingSelectionRequest()).isNotNull();
        assertThat(session.getPendingSelectionRequest().sourceEffect()).isEqualTo(ar.edu.utn.frc.tup.piii.engine.model.TrainerEffectId.GREAT_BALL);
        
        // Player A: Selects a Pokemon card from Great Ball
        String deckCardId = session.getPlayerRuntime(activeIndex).getDeck().getCards().stream()
                .filter(c -> c instanceof ar.edu.utn.frc.tup.piii.engine.model.PokemonCard)
                .findFirst()
                .orElseThrow()
                .getCardId();
        ActionRequestDTO selectCard = new ActionRequestDTO(ActionType.SELECT_CARDS, null, null, null, null, null, null, Collections.emptyList(), null, List.of(deckCardId));
        matchService.processAction(matchId, activePlayerId, selectCard);
        
        // Game resumes
        assertThat(session.getTurnManager().currentPhase() instanceof ar.edu.utn.frc.tup.piii.engine.model.MainPhase).isTrue();
        assertThat(session.getPendingSelectionRequest()).isNull();
        
        // Player A: Attaches energy to Active Weedle
        ActionRequestDTO attachEnergy = new ActionRequestDTO(ActionType.ATTACH_ENERGY, "basic-grass-test1", null, 0, null, null, PokemonType.GRASS, Collections.emptyList(), null, Collections.emptyList()); // targetIndex 0 is Active
        matchService.processAction(matchId, activePlayerId, attachEnergy);
        
        // Player A: Ends Turn (cannot attack on first turn)
        ActionRequestDTO endTurn = new ActionRequestDTO(ActionType.END_TURN, null, null, null, null, null, null, Collections.emptyList(), null, Collections.emptyList());
        matchService.processAction(matchId, activePlayerId, endTurn);
        
        // Player A ends turn
        activeIndex = session.getTurnManager().activePlayerIndex();
        activePlayerId = session.getPlayerIds().get(activeIndex);
        assertThat(activeIndex).isEqualTo(1); // Player B's turn
        
        // --- TURN (Player B) ---
        // Wait, Weedle did NOT attack. So Scatterbug has 0 damage.
        assertThat(session.getPlayerRuntime(activeIndex).getActivePokemon().getDamageCounters()).isEqualTo(0);
        
        // Player B: Attempts to play Evosoda (Should Fail - First Turn)
        final String currentActivePlayerId = activePlayerId;
        ActionRequestDTO playEvosodaIllegal = new ActionRequestDTO(ActionType.PLAY_TRAINER, "xy1-116", null, -1, TrainerType.ITEM, null, null, Collections.emptyList(), null, Collections.emptyList());
        org.junit.jupiter.api.Assertions.assertThrows(ar.edu.utn.frc.tup.piii.engine.exception.InvalidActionException.class, () -> {
            matchService.processAction(matchId, currentActivePlayerId, playEvosodaIllegal);
        });
        
        // Player B: Attaches energy
        ActionRequestDTO attachEnergyB = new ActionRequestDTO(ActionType.ATTACH_ENERGY, "basic-grass-test2", null, null, null, null, PokemonType.GRASS, Collections.emptyList(), null, Collections.emptyList());
        matchService.processAction(matchId, activePlayerId, attachEnergyB);
        
        // Player B: Ends turn (Scatterbug has no energy for its attack, Bug Bite costs 1 Colorless but wait, Bug Bite is Spewpa's attack! Scatterbug has String Shot for 1 Grass, does 10 damage and flip coin for paralysis. Let's just end turn to get to turn 2).
        ActionRequestDTO endTurnB = new ActionRequestDTO(ActionType.END_TURN, null, null, null, null, null, null, Collections.emptyList(), null, Collections.emptyList());
        matchService.processAction(matchId, activePlayerId, endTurnB);
        
        // --- TURN 2 (Player A) ---
        activeIndex = session.getTurnManager().activePlayerIndex();
        activePlayerId = session.getPlayerIds().get(activeIndex);
        assertThat(activeIndex).isEqualTo(0); // Player A's turn
        
        ActionRequestDTO endTurnA2 = new ActionRequestDTO(ActionType.END_TURN, null, null, null, null, null, null, Collections.emptyList(), null, Collections.emptyList());
        matchService.processAction(matchId, activePlayerId, endTurnA2);
        
        // --- TURN 2 (Player B) ---
        activeIndex = session.getTurnManager().activePlayerIndex();
        activePlayerId = session.getPlayerIds().get(activeIndex);
        assertThat(activeIndex).isEqualTo(1); // Player B's turn
        
        // Player B: Plays Evosoda (NOW LEGAL)
        ActionRequestDTO playEvosoda = new ActionRequestDTO(ActionType.PLAY_TRAINER, "xy1-116", null, -1, TrainerType.ITEM, null, null, Collections.emptyList(), null, Collections.emptyList());
        matchService.processAction(matchId, activePlayerId, playEvosoda);
        assertThat(session.getTurnManager().currentPhase() instanceof ar.edu.utn.frc.tup.piii.engine.model.ActionResolutionPhase).isTrue();
        
        // Select Spewpa from deck
        String spewpaId = session.getPlayerRuntime(activeIndex).getDeck().getCards().stream()
                .filter(c -> c.getCardId().equals("xy1-16"))
                .findFirst().get().getCardId();
        ActionRequestDTO selectEvosoda = new ActionRequestDTO(ActionType.SELECT_CARDS, null, null, null, null, null, null, Collections.emptyList(), null, List.of(spewpaId));
        matchService.processAction(matchId, activePlayerId, selectEvosoda);
        
        // Assert Scatterbug evolved into Spewpa (HP 80)
        assertThat(session.getPlayerRuntime(activeIndex).getActivePokemon().getBaseCard().getCardId()).isEqualTo("xy1-16");
        
        // Player B: Attacks with Spewpa (Bug Bite costs 1 Colorless, does 10 damage)
        ActionRequestDTO attackB = new ActionRequestDTO(ActionType.DECLARE_ATTACK, null, null, null, null, 0, null, Collections.emptyList(), null, Collections.emptyList());
        matchService.processAction(matchId, activePlayerId, attackB);
        
        // Turn switches back to Player A
        activeIndex = session.getTurnManager().activePlayerIndex();
        activePlayerId = session.getPlayerIds().get(activeIndex);
        assertThat(activeIndex).isEqualTo(0);
        
        // Weedle took 10 damage
        assertThat(session.getPlayerRuntime(activeIndex).getActivePokemon().getDamageCounters()).isEqualTo(1);
    }
}
