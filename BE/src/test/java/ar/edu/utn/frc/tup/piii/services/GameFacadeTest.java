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
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerState;
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
    }

    @Test
    void shouldProduceDeclareAttackActionWhenTypeIsDeclareAttack() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.DECLARE_ATTACK, null, null, null, null, 0);

        final Action action = facade.toEngineAction(board, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(DeclareAttackAction.class);
        final DeclareAttackAction declareAttack = (DeclareAttackAction) action;
        assertThat(declareAttack.attacker()).isEqualTo(activePokemon);
        assertThat(declareAttack.attack()).isEqualTo(sampleAttack);
    }

    @Test
    void shouldProduceRetreatActionWhenTypeIsRetreat() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.RETREAT, null, null, null, null, null);

        final Action action = facade.toEngineAction(board, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(RetreatAction.class);
        assertThat(((RetreatAction) action).active()).isEqualTo(activePokemon);
    }

    @Test
    void shouldProducePlayTrainerActionWhenTypeIsPlayTrainer() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.PLAY_TRAINER, null, null, null, TrainerType.SUPPORTER, null);

        final Action action = facade.toEngineAction(board, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(PlayTrainerAction.class);
        assertThat(((PlayTrainerAction) action).trainerType()).isEqualTo(TrainerType.SUPPORTER);
    }

    @Test
    void shouldProduceAttachEnergyActionWhenTypeIsAttachEnergy() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.ATTACH_ENERGY, null, null, null, null, null);

        final Action action = facade.toEngineAction(board, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(AttachEnergyAction.class);
    }

    @Test
    void shouldProduceEvolveActionWhenTypeIsEvolve() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.EVOLVE, null, TARGET_ID, null, null, null);

        final Action action = facade.toEngineAction(board, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(EvolveAction.class);
        assertThat(((EvolveAction) action).target()).isEqualTo(activePokemon);
    }

    @Test
    void shouldProducePlaceBasicPokemonActionWhenTypeIsPlaceBasicPokemon() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.PLACE_BASIC_POKEMON, CARD_ID, null, null, null, null);

        final Action action = facade.toEngineAction(board, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(PlaceBasicPokemonAction.class);
        assertThat(((PlaceBasicPokemonAction) action).cardId()).isEqualTo(CARD_ID);
    }

    @Test
    void shouldProduceUseAbilityActionWhenTypeIsUseAbility() {
        final ActionRequestDTO dto = new ActionRequestDTO(
                ActionType.USE_ABILITY, ABILITY_ID, null, null, null, null);

        final Action action = facade.toEngineAction(board, PLAYER_INDEX, dto);

        assertThat(action).isInstanceOf(UseAbilityAction.class);
        assertThat(((UseAbilityAction) action).source()).isEqualTo(activePokemon);
        assertThat(((UseAbilityAction) action).abilityId()).isEqualTo(ABILITY_ID);
    }
}
