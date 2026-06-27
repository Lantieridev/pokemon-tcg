package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.ActionType;
import ar.edu.utn.frc.tup.piii.engine.exception.InvalidActionException;
import ar.edu.utn.frc.tup.piii.engine.model.*;
import ar.edu.utn.frc.tup.piii.persistence.mapper.CardMapper;
import ar.edu.utn.frc.tup.piii.persistence.repository.CardRepository;
import ar.edu.utn.frc.tup.piii.services.MatchCreationService;
import ar.edu.utn.frc.tup.piii.services.MatchService;
import ar.edu.utn.frc.tup.piii.services.MatchSessionRegistry;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class ComprehensiveGameSimulationTest {

    // ANSI Colors for console presentation
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";

    @Autowired
    private MatchService matchService;

    @Autowired
    private MatchCreationService matchCreationService;

    @Autowired
    private MatchSessionRegistry sessionRegistry;

    @Autowired
    private CardRepository cardRepository;

    private final CardMapper cardMapper = new CardMapper(new ObjectMapper());
    private MatchSession session;
    private String matchId;

    private Card resolveCard(String id) {
        ar.edu.utn.frc.tup.piii.persistence.entity.CardEntity entity = cardRepository.findById(id).orElseThrow();
        return cardMapper.map(entity);
    }

    private void printHeader(String title, String color) {
        System.out.println("\n" + color + BOLD + "================================================================================" + RESET);
        System.out.println(color + BOLD + "  " + title + RESET);
        System.out.println(color + BOLD + "================================================================================" + RESET);
    }

    private void printStep(String stepDescription) {
        System.out.println(CYAN + "👉 " + RESET + stepDescription);
    }

    private void printSuccess(String message) {
        System.out.println(GREEN + "✔ OK: " + RESET + message);
    }

    private void printFailureExpected(String expectedRuleName, String errorMsg) {
        System.out.println(YELLOW + "💡 REGLA DISPARADA (" + expectedRuleName + "): " + RESET + errorMsg);
    }

    @BeforeEach
    void setUp() {
        // Prepare Decks complying with 60-card rule
        List<Card> deckA = new ArrayList<>();
        List<Card> deckB = new ArrayList<>();

        // Add 4 Venusaur-EX and Weedle cards
        for (int i = 0; i < 4; i++) {
            deckA.add(resolveCard("xy1-1"));   // Venusaur-EX
            deckA.add(resolveCard("xy1-3"));   // Weedle
            deckB.add(resolveCard("xy1-15"));  // Scatterbug
        }
        deckA.add(resolveCard("xy1-2"));       // M Venusaur-EX
        deckA.add(resolveCard("xy1-4"));       // Kakuna
        deckA.add(resolveCard("xy1-5"));       // Beedrill
        deckA.add(resolveCard("xy1-118"));     // Great Ball
        deckA.add(resolveCard("xy1-123"));     // Professor's Letter

        // Backfill with Energy Cards to exactly 60
        while (deckA.size() < 60) {
            deckA.add(new EnergyCard("basic-grass-a-" + deckA.size(), "Grass Energy", PokemonType.GRASS, true));
        }

        deckB.add(resolveCard("xy1-16"));      // Spewpa
        deckB.add(resolveCard("xy1-17"));      // Vivillon
        deckB.add(resolveCard("xy1-116"));     // Evosoda

        while (deckB.size() < 60) {
            deckB.add(new EnergyCard("basic-grass-b-" + deckB.size(), "Grass Energy", PokemonType.GRASS, true));
        }

        matchId = matchCreationService.createMatch("AshKetchum", "Misty", deckA, deckB);
        session = sessionRegistry.find(matchId).orElseThrow();
    }

    @Test
    void runComprehensiveSimulation() {
        printHeader("INICIANDO SIMULACIÓN DE PARTIDA COMPLETA — REGLAS POKÉMON XY1", BLUE);

        // ---------------------------------------------------------------------
        // 1. MULLIGAN & SETUP
        // ---------------------------------------------------------------------
        printHeader("1. PRUEBA DE SETUP Y MULLIGAN", PURPLE);
        int totalCardsA = session.getPlayerRuntime(0).getDeck().getCards().size()
                + session.getPlayerRuntime(0).getHand().getCards().size()
                + session.getPlayerRuntime(0).getPrizePile().size()
                + (session.getPlayerRuntime(0).getActivePokemon() != null ? 1 : 0)
                + session.getPlayerRuntime(0).getBench().getAll().size();
        int totalCardsB = session.getPlayerRuntime(1).getDeck().getCards().size()
                + session.getPlayerRuntime(1).getHand().getCards().size()
                + session.getPlayerRuntime(1).getPrizePile().size()
                + (session.getPlayerRuntime(1).getActivePokemon() != null ? 1 : 0)
                + session.getPlayerRuntime(1).getBench().getAll().size();
        assertThat(totalCardsA).isEqualTo(60);
        assertThat(totalCardsB).isEqualTo(60);
        printSuccess("Mazos válidos de 60 cartas cargados y distribuidos correctamente.");

        printStep("Simulando setup determinista para pruebas de reglas...");
        // Manually override player hands to test rules deterministically
        session.getPlayerRuntime(0).getHand().removeAll();
        session.getPlayerRuntime(0).getHand().addCard(resolveCard("xy1-1"));     // Venusaur-EX (Basic EX)
        session.getPlayerRuntime(0).getHand().addCard(resolveCard("xy1-3"));     // Weedle (Basic)
        session.getPlayerRuntime(0).getHand().addCard(resolveCard("xy1-4"));     // Kakuna (Stage 1)
        session.getPlayerRuntime(0).getHand().addCard(resolveCard("xy1-5"));     // Beedrill (Stage 2)
        session.getPlayerRuntime(0).getHand().addCard(resolveCard("xy1-118"));   // Great Ball
        session.getPlayerRuntime(0).getHand().addCard(resolveCard("xy1-2"));     // M Venusaur-EX
        session.getPlayerRuntime(0).getHand().addCard(new EnergyCard("energy-grass-1", "Grass Energy", PokemonType.GRASS, true));
        session.getPlayerRuntime(0).getHand().addCard(new EnergyCard("energy-grass-2", "Grass Energy", PokemonType.GRASS, true));

        session.getPlayerRuntime(1).getHand().removeAll();
        session.getPlayerRuntime(1).getHand().addCard(resolveCard("xy1-15"));    // Scatterbug (Basic)
        session.getPlayerRuntime(1).getHand().addCard(resolveCard("xy1-16"));    // Spewpa (Stage 1)
        session.getPlayerRuntime(1).getHand().addCard(resolveCard("xy1-116"));   // Evosoda
        session.getPlayerRuntime(1).getHand().addCard(new EnergyCard("energy-grass-3", "Grass Energy", PokemonType.GRASS, true));
        session.getPlayerRuntime(1).getHand().addCard(new EnergyCard("energy-grass-4", "Grass Energy", PokemonType.GRASS, true));

        // Clear benches to ensure setup randomness is isolated
        session.getPlayerRuntime(0).getBench().removeAll().forEach(session.getPlayerRuntime(0)::removePokemonFromPlay);
        session.getPlayerRuntime(1).getBench().removeAll().forEach(session.getPlayerRuntime(1)::removePokemonFromPlay);

        // Force Active Pokemon
        ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState activeAsh =
                new ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon((PokemonCard) resolveCard("xy1-3")); // Weedle
        session.getPlayerRuntime(0).setActivePokemon(activeAsh);
        session.getPlayerRuntime(0).recordPokemonEntered(activeAsh);

        ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState activeMisty =
                new ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon((PokemonCard) resolveCard("xy1-15")); // Scatterbug
        session.getPlayerRuntime(1).setActivePokemon(activeMisty);
        session.getPlayerRuntime(1).recordPokemonEntered(activeMisty);

        // Force Player index 0 (Ash) to go first
        session.getTurnManager().reset();
        session.getTurnManager().startTurn(0);
        int activeIndex = session.getTurnManager().activePlayerIndex();
        String activePlayerId = session.getPlayerIds().get(activeIndex);

        assertThat(activeIndex).isEqualTo(0);
        assertThat(activePlayerId).isEqualTo("AshKetchum");
        printSuccess("Setup forzado con éxito. Turno actual de: " + activePlayerId);

        // ---------------------------------------------------------------------
        // 2. BENCH PLACEMENT LIMITS (MAX 5)
        // ---------------------------------------------------------------------
        printHeader("2. PRUEBA DE BANCA Y LÍMITES DE POKÉMON", PURPLE);
        printStep("Ash intenta colocar una carta que NO es un Pokémon Básico en la Banca (Kakuna)...");
        ActionRequestDTO placeKakuna = new ActionRequestDTO(
                ActionType.PLACE_BASIC_POKEMON, "xy1-4", null, null, null, null, null,
                Collections.emptyList(), null, Collections.emptyList()
        );
        final String pid1 = activePlayerId;
        Exception exKakuna = assertThrows(InvalidActionException.class, () -> {
            matchService.processAction(matchId, pid1, placeKakuna);
        });
        printFailureExpected("card_not_basic_pokemon", exKakuna.getMessage());

        printStep("Ash coloca a Venusaur-EX (Pokémon Básico) en la Banca...");
        ActionRequestDTO placeVenusaur = new ActionRequestDTO(
                ActionType.PLACE_BASIC_POKEMON, "xy1-1", null, null, null, null, null,
                Collections.emptyList(), null, Collections.emptyList()
        );
        matchService.processAction(matchId, activePlayerId, placeVenusaur);
        assertThat(session.getPlayerRuntime(0).getBench().getAll()).hasSize(1);
        printSuccess("Venusaur-EX colocado exitosamente en la Banca de Ash.");

        // Forcing bench full of basics to verify max 5 rule
        printStep("Forzando banca llena (5 Pokémon Básicos) para verificar restricción...");
        for (int i = 0; i < 4; i++) {
            session.getPlayerRuntime(0).getBench().place(new ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon((PokemonCard) resolveCard("xy1-3"))); // Weedle
        }
        assertThat(session.getPlayerRuntime(0).getBench().getAll()).hasSize(5);

        printStep("Intentando colocar un 6to Pokémon Básico en banca...");
        session.getPlayerRuntime(0).getHand().addCard(resolveCard("xy1-3")); // Add basic to hand
        ActionRequestDTO placeSixth = new ActionRequestDTO(
                ActionType.PLACE_BASIC_POKEMON, "xy1-3", null, null, null, null, null,
                Collections.emptyList(), null, Collections.emptyList()
        );
        final String pid2 = activePlayerId;
        Exception exBenchFull = assertThrows(InvalidActionException.class, () -> {
            matchService.processAction(matchId, pid2, placeSixth);
        });
        printFailureExpected("bench_full", exBenchFull.getMessage());

        // Restore bench size to normal for following tests
        session.getPlayerRuntime(0).getBench().removeAll();
        ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon newVenusaur = new ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon((PokemonCard) resolveCard("xy1-1"));
        session.getPlayerRuntime(0).getBench().place(newVenusaur);
        session.getPlayerRuntime(0).recordPokemonEntered(newVenusaur);
        assertThat(session.getPlayerRuntime(0).getBench().getAll()).hasSize(1);

        // ---------------------------------------------------------------------
        // 3. ENERGY ATTACHMENT LIMIT (MAX 1 PER TURN)
        // ---------------------------------------------------------------------
        printHeader("3. PRUEBA DE LÍMITE DE ENERGÍA POR TURNO", PURPLE);
        printStep("Ash une una Energía Planta a su Weedle Activo...");
        ActionRequestDTO attachEnergy1 = new ActionRequestDTO(
                ActionType.ATTACH_ENERGY, "energy-grass-1", null, null, null, null, PokemonType.GRASS,
                Collections.emptyList(), null, Collections.emptyList()
        );
        matchService.processAction(matchId, activePlayerId, attachEnergy1);
        assertThat(session.getPlayerRuntime(0).getActivePokemon().getAttachedEnergies()).hasSize(1);
        printSuccess("Energía Planta unida a Weedle.");

        printStep("Ash intenta unir una SEGUNDA Energía Planta en el mismo turno (a Venusaur-EX en banca)...");
        ActionRequestDTO attachEnergy2 = new ActionRequestDTO(
                ActionType.ATTACH_ENERGY, "energy-grass-2", null, 1, null, null, PokemonType.GRASS,
                Collections.emptyList(), null, Collections.emptyList() // targetIndex 1 is Venusaur-EX on Bench
        );
        final String pid3 = activePlayerId;
        Exception exEnergyLimit = assertThrows(InvalidActionException.class, () -> {
            matchService.processAction(matchId, pid3, attachEnergy2);
        });
        printFailureExpected("energy_already_attached", exEnergyLimit.getMessage());

        // ---------------------------------------------------------------------
        // 4. EVOLUTION RULES & TIMING
        // ---------------------------------------------------------------------
        printHeader("4. PRUEBA DE REGLAS Y TIEMPOS DE EVOLUCIÓN", PURPLE);
        printStep("Ash intenta evolucionar a Weedle Activo en Kakuna (Turno 1)...");
        ActionRequestDTO evolveToKakuna = new ActionRequestDTO(
                ActionType.EVOLVE, "xy1-4", "xy1-3", null, null, null, null,
                Collections.emptyList(), null, Collections.emptyList()
        );
        final String pid4 = activePlayerId;
        Exception exEvolveTurn1 = assertThrows(InvalidActionException.class, () -> {
            matchService.processAction(matchId, pid4, evolveToKakuna);
        });
        printFailureExpected("cannot_evolve_first_turn", exEvolveTurn1.getMessage());

        printStep("Ash intenta evolucionar a Venusaur-EX en la Banca (mismo turno que entró)...");
        ActionRequestDTO evolveVenusaur = new ActionRequestDTO(
                ActionType.EVOLVE, "xy1-2", "xy1-1", null, null, null, null,
                Collections.emptyList(), null, Collections.emptyList()
        );
        final String pid5 = activePlayerId;
        Exception exEvolveSameTurn = assertThrows(InvalidActionException.class, () -> {
            // Venusaur-EX entered this turn, so it has 0 turns in play
            matchService.processAction(matchId, pid5, evolveVenusaur);
        });
        printFailureExpected("pokemon_entered_this_turn", exEvolveSameTurn.getMessage());

        printStep("Ash termina el turno (Turno 1 de Ash finalizado).");
        ActionRequestDTO endTurnAsh1 = new ActionRequestDTO(
                ActionType.END_TURN, null, null, null, null, null, null,
                Collections.emptyList(), null, Collections.emptyList()
        );
        matchService.processAction(matchId, activePlayerId, endTurnAsh1);

        // Turn switches to Misty (Player B)
        activeIndex = session.getTurnManager().activePlayerIndex();
        activePlayerId = session.getPlayerIds().get(activeIndex);
        assertThat(activeIndex).isEqualTo(1);
        assertThat(activePlayerId).isEqualTo("Misty");
        printSuccess("Turno actual de: " + activePlayerId);

        printStep("Misty une una Energía Planta a Scatterbug...");
        ActionRequestDTO attachEnergyMisty = new ActionRequestDTO(
                ActionType.ATTACH_ENERGY, "energy-grass-3", null, null, null, null, PokemonType.GRASS,
                Collections.emptyList(), null, Collections.emptyList()
        );
        matchService.processAction(matchId, activePlayerId, attachEnergyMisty);
        printSuccess("Energía Planta unida a Scatterbug.");

        printStep("Misty termina el turno (Turno 1 de Misty finalizado).");
        ActionRequestDTO endTurnMisty1 = new ActionRequestDTO(
                ActionType.END_TURN, null, null, null, null, null, null,
                Collections.emptyList(), null, Collections.emptyList()
        );
        matchService.processAction(matchId, activePlayerId, endTurnMisty1);

        // Turn switches back to Ash (Turno 2 - Ash)
        activeIndex = session.getTurnManager().activePlayerIndex();
        activePlayerId = session.getPlayerIds().get(activeIndex);
        assertThat(activeIndex).isEqualTo(0);
        printSuccess("Turno actual de: " + activePlayerId + " (Turno 2)");

        // ---------------------------------------------------------------------
        // 5. RETREAT ENERGY COST & LIMIT
        // ---------------------------------------------------------------------
        // ---------------------------------------------------------------------
        // 5. EVOLUTION SUCCESS BEFORE RETREAT
        // ---------------------------------------------------------------------
        printHeader("5. PRUEBA DE EVOLUCIÓN EXITOSA DEL ACTIVO", PURPLE);
        printStep("Ash une una SEGUNDA Energía Planta a Weedle Activo...");
        ActionRequestDTO attachEnergyTurn2 = new ActionRequestDTO(
                ActionType.ATTACH_ENERGY, "energy-grass-2", null, null, null, null, PokemonType.GRASS,
                Collections.emptyList(), null, Collections.emptyList()
        );
        matchService.processAction(matchId, activePlayerId, attachEnergyTurn2);

        printStep("Ash evoluciona Weedle Activo a Kakuna (Turno 2 - ya tiene >1 turno en juego)...");
        // Weedle entered during Setup, so it has been in play since start
        ActionRequestDTO evolveActiveSuccess = new ActionRequestDTO(
                ActionType.EVOLVE, "xy1-4", "xy1-3", null, null, null, null,
                Collections.emptyList(), null, Collections.emptyList()
        );
        matchService.processAction(matchId, activePlayerId, evolveActiveSuccess);
        assertThat(session.getPlayerRuntime(0).getActivePokemon().getBaseCard().getCardId()).isEqualTo("xy1-4");
        printSuccess("Weedle Activo evolucionó a Kakuna con éxito.");

        // ---------------------------------------------------------------------
        // 6. RETREAT ENERGY COST & LIMIT
        // ---------------------------------------------------------------------
        printHeader("6. PRUEBA DE RETIRO Y LÍMITE POR TURNO", PURPLE);
        printStep("Ash intenta retirar a Kakuna (Costo de retiro: 2) pagando solo 1 energía...");
        ActionRequestDTO retreatNoPay = new ActionRequestDTO(
                ActionType.RETREAT, null, null, 0, null, null, null, // Bench targetIndex 0 (Venusaur-EX)
                List.of(0), null, Collections.emptyList()
        );
        final String pid6 = activePlayerId;
        Exception exRetreatNoPay = assertThrows(InvalidActionException.class, () -> {
            matchService.processAction(matchId, pid6, retreatNoPay);
        });
        printFailureExpected("Must specify exactly 2 energy indices to discard.", exRetreatNoPay.getMessage());

        printStep("Ash retira a Kakuna pagando las 2 Energías Planta attached...");
        ActionRequestDTO retreatSuccess = new ActionRequestDTO(
                ActionType.RETREAT, null, null, 0, null, null, null, // Bench targetIndex 0 (Venusaur-EX)
                List.of(0, 1), null, Collections.emptyList() // Discard energy indices 0 and 1
        );
        matchService.processAction(matchId, activePlayerId, retreatSuccess);
        assertThat(session.getPlayerRuntime(0).getActivePokemon().getBaseCard().getCardId()).isEqualTo("xy1-1"); // Venusaur-EX is now active
        printSuccess("Kakuna retirado. Venusaur-EX es ahora el Pokémon Activo.");

        printStep("Ash intenta realizar un SEGUNDO retiro en el mismo turno...");
        ActionRequestDTO retreatSecond = new ActionRequestDTO(
                ActionType.RETREAT, null, null, 0, null, null, null,
                Collections.emptyList(), null, Collections.emptyList()
        );
        final String pid7 = activePlayerId;
        Exception exRetreatLimit = assertThrows(InvalidActionException.class, () -> {
            matchService.processAction(matchId, pid7, retreatSecond);
        });
        printFailureExpected("retreat_already_used", exRetreatLimit.getMessage());

        printStep("Ash intenta evolucionar Kakuna (en banca) a Beedrill en el mismo turno (debe fallar)...");
        ActionRequestDTO evolveKakunaSameTurn = new ActionRequestDTO(
                ActionType.EVOLVE, "xy1-5", "xy1-4", 0, null, null, null, // Bench targetIndex 0 (Kakuna)
                Collections.emptyList(), null, Collections.emptyList()
        );
        final String pid8 = activePlayerId;
        Exception exKakunaSameTurn = assertThrows(InvalidActionException.class, () -> {
            matchService.processAction(matchId, pid8, evolveKakunaSameTurn);
        });
        printFailureExpected("pokemon_entered_this_turn", exKakunaSameTurn.getMessage());

        printStep("Ash termina el turno (Turno 2 de Ash finalizado).");
        ActionRequestDTO endTurnAsh2 = new ActionRequestDTO(
                ActionType.END_TURN, null, null, null, null, null, null,
                Collections.emptyList(), null, Collections.emptyList()
        );
        matchService.processAction(matchId, activePlayerId, endTurnAsh2);

        // Turn switches to Misty (Turno 2 - Misty)
        activeIndex = session.getTurnManager().activePlayerIndex();
        activePlayerId = session.getPlayerIds().get(activeIndex);
        assertThat(activeIndex).isEqualTo(1);
        printSuccess("Turno actual de: " + activePlayerId + " (Turno 2)");

        // ---------------------------------------------------------------------
        // 7. ATTACKS, DAMAGE, AND EX KNOCKOUT PRIZE COLLECTION (2 PRIZES!)
        // ---------------------------------------------------------------------
        printHeader("7. PRUEBA DE ATAQUE, DAÑO Y RECLAMO DE PREMIOS (2 PREMIOS POKÉMON EX)", PURPLE);
        printStep("Misty evoluciona a Scatterbug Activo a Spewpa...");
        ActionRequestDTO evolveScatterbug = new ActionRequestDTO(
                ActionType.EVOLVE, "xy1-16", "xy1-15", null, null, null, null,
                Collections.emptyList(), null, Collections.emptyList()
        );
        matchService.processAction(matchId, activePlayerId, evolveScatterbug);
        assertThat(session.getPlayerRuntime(1).getActivePokemon().getBaseCard().getCardId()).isEqualTo("xy1-16");
        printSuccess("Scatterbug Activo evolucionó a Spewpa.");

        printStep("Simulando preparación de Knockout de Venusaur-EX (HP: 180) y condición de victoria para Misty...");
        // Misty initially has 6 prize cards. To verify victory condition simultaneously,
        // we'll reduce Misty's prize pile to exactly 2 cards first.
        // Then, knocking out the active Venusaur-EX (worth 2 prize cards) will empty her prize pile,
        // triggering the victory condition.
        List<Card> removed = new ArrayList<>(session.getPlayerRuntime(1).clearPrizes());
        session.getPlayerRuntime(1).addPrizes(removed.subList(0, 2));
        assertThat(session.getPlayerRuntime(1).getPrizeCount()).isEqualTo(2);
        printSuccess("Pila de premios de Misty forzada a 2 cartas para verificar condición de victoria.");

        // Add damage counters to active Pokémon so it is 10 HP away from KO
        int maxHp = session.getPlayerRuntime(0).getActivePokemon().getMaxHp();
        session.getPlayerRuntime(0).getActivePokemon().addDamageCounters((maxHp - 10) / 10);
        printSuccess("Pokémon Activo preparado con daño previo para KO.");

        printStep("Misty ataca con Spewpa (Bug Bite, costo: 1 Colorless, hace 10 de daño)...");
        // It has 1 Grass Energy attached, which satisfies the Colorless requirement
        ActionRequestDTO attackSpewpa = new ActionRequestDTO(
                ActionType.DECLARE_ATTACK, null, null, null, null, 0, null, // attackIndex 0 is Bug Bite
                Collections.emptyList(), null, Collections.emptyList()
        );
        matchService.processAction(matchId, activePlayerId, attackSpewpa);

        printStep("Verificando que Misty haya cobrado DOS cartas de Premio (Venusaur-EX es un Pokémon EX)...");
        // Misty should have taken both prize cards, leaving her prize pile empty
        assertThat(session.getPlayerRuntime(1).getPrizeCount()).isEqualTo(0);
        assertThat(session.getPlayerRuntime(1).getPrizePile()).isEmpty();
        printSuccess("¡Pokémon EX Noqueado de forma nativa! Misty cobró 2 cartas de Premio. Premios restantes: 0");

        // ---------------------------------------------------------------------
        // 8. VICTORY CONDITION CHECK
        // ---------------------------------------------------------------------
        printHeader("8. PRUEBA DE CONDICIÓN DE VICTORIA", PURPLE);
        printStep("Verificando si el VictoryConditionChecker nativo declaró ganadora a Misty...");
        assertThat(session.getState()).isEqualTo(MatchSessionState.FINISHED);
        assertThat(session.getWinnerId()).isEqualTo("Misty");
        printSuccess("Partida finalizada con éxito. ¡Ganadora declarada de forma nativa por el Engine: Misty!");

        printHeader("¡TODAS LAS REGLAS Y CONDICIONANTES COMPROBADOS CON ÉXITO!", GREEN);
    }
}
