package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.ActionRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.ActionType;
import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Action;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.AttachEnergyAction;
import ar.edu.utn.frc.tup.piii.engine.model.DeclareAttackAction;
import ar.edu.utn.frc.tup.piii.engine.model.EvolveAction;
import ar.edu.utn.frc.tup.piii.engine.model.PlaceBasicPokemonAction;
import ar.edu.utn.frc.tup.piii.engine.model.PlayTrainerAction;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.RetreatAction;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.model.Deck;
import ar.edu.utn.frc.tup.piii.engine.model.Hand;
import ar.edu.utn.frc.tup.piii.engine.model.Bench;
import ar.edu.utn.frc.tup.piii.engine.model.DiscardPile;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GameFacade.toEngineAction() — one test per ActionType.
 */
class GameFacadeTest {

    private static final int PLAYER_INDEX = 0;
    private static final String CARD_ID = "xy1-001";
    private static final String TARGET_ID = "xy1-002";
    private static final String ABILITY_ID = "ability-001";

    private GameFacade facade;
    private MatchSession session;
    private MatchBoard board;
    private FakeBattlePokemonState activePokemon;
    private FakeBattlePokemonState targetPokemon;
    private Attack sampleAttack;

    @BeforeEach
    void setUp() {
        facade = new GameFacade();

        activePokemon = new FakeBattlePokemonState(100, PokemonType.FIRE, PokemonType.WATER, null, false);
        targetPokemon = new FakeBattlePokemonState(80, PokemonType.WATER, null, null, false);

        sampleAttack = new Attack("Flamethrower", 40, List.of(PokemonType.FIRE));

        final PlayerState player0 = new PlayerState(
                activePokemon,
                List.of(targetPokemon),
                List.of(),
                List.of(sampleAttack),
                45, 6, Map.of());

        final PlayerState player1 = new PlayerState(
                targetPokemon,
                List.of(),
                45, 6, Map.of());

        board = new MatchBoard(List.of(player0, player1));
        
        Hand hand0 = new Hand();
        hand0.addCard(new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder(
            "evo-id", "Evo", 100, PokemonType.FIRE)
            .evolutionStage(ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.STAGE_1)
            .evolvesFrom("Squirtle")
            .build());
            
        Card dummyCard = new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder("dummy", "Dummy", 10, PokemonType.FIRE).build();
        Deck deck = new Deck(List.of(dummyCard));
        PlayerRuntime runtime0 = new PlayerRuntime(deck, hand0, new Bench(), new DiscardPile(), new StatusEffectManager(() -> true), activePokemon);
        PlayerRuntime runtime1 = new PlayerRuntime(deck, new Hand(), new Bench(), new DiscardPile(), new StatusEffectManager(() -> true), targetPokemon);
        session = new MatchSession("test-match", List.of("p1", "p2"), board, List.of(runtime0, runtime1));
        session.setCoinFlipper(() -> true);
    }

    @Test
    void shouldProduceDeclareAttackActionWhenTypeIsDeclareAttack() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.DECLARE_ATTACK, null, null, null, null, 0);

        final Action action = facade.toEngineAction(session, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(DeclareAttackAction.class);
        final DeclareAttackAction declareAttack = (DeclareAttackAction) action;
        assertThat(declareAttack.attacker()).isEqualTo(activePokemon);
        assertThat(declareAttack.attack()).isEqualTo(sampleAttack);
    }

    @Test
    void shouldProduceRetreatActionWhenTypeIsRetreat() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.RETREAT, null, null, null, null, null);

        final Action action = facade.toEngineAction(session, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(RetreatAction.class);
        assertThat(((RetreatAction) action).active()).isEqualTo(activePokemon);
    }

    @Test
    void shouldAutoPopulateRetreatEnergyIndicesBasedOnCost() {
        activePokemon.setRetreatCost(2);
        
        ar.edu.utn.frc.tup.piii.engine.model.EnergyCard energy1 = new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("e1", "Fire Energy", PokemonType.FIRE, true, 1, false);
        ar.edu.utn.frc.tup.piii.engine.model.EnergyCard energy2 = new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("e2", "Fire Energy", PokemonType.FIRE, true, 1, false);
        activePokemon.attachEnergy(energy1);
        activePokemon.attachEnergy(energy2);

        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.RETREAT, null, null, null, null, null);

        final Action action = facade.toEngineAction(session, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(RetreatAction.class);
        final RetreatAction retreatAction = (RetreatAction) action;
        assertThat(retreatAction.energyIndicesToDiscard()).containsExactly(0, 1);
    }

    @Test
    void shouldAutoPopulateZeroRetreatEnergyIndicesWhenFairyGardenAndFairyEnergyActive() {
        activePokemon.setRetreatCost(2);
        
        ar.edu.utn.frc.tup.piii.engine.model.EnergyCard fairyEnergy = new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("e1", "Fairy Energy", PokemonType.FAIRY, true, 1, false);
        activePokemon.attachEnergy(fairyEnergy);

        final ar.edu.utn.frc.tup.piii.engine.model.TrainerCard fairyGarden = new ar.edu.utn.frc.tup.piii.engine.model.TrainerCard.Builder("xy1-117", "Fairy Garden", TrainerType.STADIUM).build();
        board.replaceStadium(fairyGarden);

        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.RETREAT, null, null, null, null, null);

        final Action action = facade.toEngineAction(session, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(RetreatAction.class);
        final RetreatAction retreatAction = (RetreatAction) action;
        assertThat(retreatAction.energyIndicesToDiscard()).isEmpty();
    }

    @Test
    void shouldAutoPopulateZeroRetreatEnergyIndicesWhenFairyGardenAndRainbowEnergyActive() {
        activePokemon.setRetreatCost(2);
        
        ar.edu.utn.frc.tup.piii.engine.model.EnergyCard rainbowEnergy = new ar.edu.utn.frc.tup.piii.engine.model.EnergyCard("e1", "Rainbow Energy", PokemonType.COLORLESS, false, 1, true);
        activePokemon.attachEnergy(rainbowEnergy);

        final ar.edu.utn.frc.tup.piii.engine.model.TrainerCard fairyGarden = new ar.edu.utn.frc.tup.piii.engine.model.TrainerCard.Builder("xy1-117", "Fairy Garden", TrainerType.STADIUM).build();
        board.replaceStadium(fairyGarden);

        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.RETREAT, null, null, null, null, null);

        final Action action = facade.toEngineAction(session, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(RetreatAction.class);
        final RetreatAction retreatAction = (RetreatAction) action;
        assertThat(retreatAction.energyIndicesToDiscard()).isEmpty();
    }

    @Test
    void shouldProducePlayTrainerActionWhenTypeIsPlayTrainer() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.PLAY_TRAINER, null, null, null, TrainerType.SUPPORTER, null);

        final Action action = facade.toEngineAction(session, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(PlayTrainerAction.class);
        assertThat(((PlayTrainerAction) action).trainerType()).isEqualTo(TrainerType.SUPPORTER);
    }

    @Test
    void shouldProduceAttachEnergyActionWhenTypeIsAttachEnergy() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.ATTACH_ENERGY, "hand-energy", "active-poke", null, null, null, null, null, null, null);

        final Action action = facade.toEngineAction(session, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(AttachEnergyAction.class);
    }

    @Test
    void shouldUseEnergyTypeFromDtoWhenBuildingAttachEnergyAction() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.ATTACH_ENERGY, null, null, null, null, null, PokemonType.FIRE, java.util.Collections.emptyList(), null, null);

        final Action action = facade.toEngineAction(session, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(AttachEnergyAction.class);
        assertThat(((AttachEnergyAction) action).energyType()).isEqualTo(PokemonType.FIRE);
    }

    @Test
    void shouldProduceEvolveActionWhenTypeIsEvolve() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.EVOLVE, "evo-id", TARGET_ID, null, null, null);

        final Action action = facade.toEngineAction(session, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(EvolveAction.class);
        assertThat(((EvolveAction) action).target()).isEqualTo(activePokemon);
    }

    @Test
    void shouldResolveBenchPokemonAsEvolveTargetWhenTargetIndexIsProvided() {
        // targetIndex=0 → bench slot 0 → targetPokemon
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.EVOLVE, "evo-id", null, 0, null, null);

        final Action action = facade.toEngineAction(session, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(EvolveAction.class);
        assertThat(((EvolveAction) action).target()).isEqualTo(targetPokemon);
    }

    @Test
    void shouldProducePlaceBasicPokemonActionWhenTypeIsPlaceBasicPokemon() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.PLACE_BASIC_POKEMON, CARD_ID, null, null, null, null);

        final Action action = facade.toEngineAction(session, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(PlaceBasicPokemonAction.class);
        assertThat(((PlaceBasicPokemonAction) action).cardId()).isEqualTo(CARD_ID);
    }

    @Test
    void shouldProduceUseAbilityActionWhenTypeIsUseAbility() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.USE_ABILITY, ABILITY_ID, null, null, null, null);

        final Action action = facade.toEngineAction(session, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(UseAbilityAction.class);
        assertThat(((UseAbilityAction) action).source()).isEqualTo(activePokemon);
        assertThat(((UseAbilityAction) action).abilityId()).isEqualTo(ABILITY_ID);
    }
}
