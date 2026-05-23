package ar.edu.utn.frc.tup.piii.engine.action;

import ar.edu.utn.frc.tup.piii.engine.FakeBenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakePokemonTurnInPlayProvider;
import ar.edu.utn.frc.tup.piii.engine.manager.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.manager.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.model.MainPhase;
import ar.edu.utn.frc.tup.piii.engine.model.PlaceBasicPokemonAction;
import ar.edu.utn.frc.tup.piii.engine.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

/**
 * Tests for PlaceBasicPokemonAction validation in RuleValidator.
 */
class PlaceBasicPokemonActionTest {

    private TurnManager turnManager;
    private StatusEffectManager statusEffectManager;
    private FakePokemonTurnInPlayProvider turnInPlayProvider;
    private FakeBenchStateProvider benchProvider;
    private ar.edu.utn.frc.tup.piii.engine.FakeHandStateProvider handProvider;
    private RuleValidator validator;

    @BeforeEach
    void setUp() {
        turnManager = Mockito.mock(TurnManager.class);
        statusEffectManager = Mockito.mock(StatusEffectManager.class);
        turnInPlayProvider = new FakePokemonTurnInPlayProvider();
        benchProvider = new FakeBenchStateProvider();
        handProvider = new ar.edu.utn.frc.tup.piii.engine.FakeHandStateProvider();
        validator = new RuleValidator(turnManager, statusEffectManager, turnInPlayProvider, benchProvider, handProvider);
    }

    @Test
    void shouldReturnValidWhenPlaceBasicPokemonActionInMainPhase() {
        MainPhase mainPhase = new MainPhase();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);
        when(turnManager.activePlayerIndex()).thenReturn(0);
        benchProvider.set(0, 0);
        handProvider.addCard(0, new ar.edu.utn.frc.tup.piii.engine.model.PokemonCard.Builder("pikachu-xy1-42", "Pikachu", 60, ar.edu.utn.frc.tup.piii.engine.model.PokemonType.LIGHTNING).evolutionStage(ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.BASIC).build());

        ValidationResult result = validator.validate(new PlaceBasicPokemonAction("pikachu-xy1-42"));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnInvalidWhenPlaceBasicPokemonActionInAttackPhase() {
        when(turnManager.requireMainPhase()).thenThrow(
                new ar.edu.utn.frc.tup.piii.engine.exception.InvalidTurnPhaseException(
                        "Expected MainPhase but was: AttackPhase"));

        org.junit.jupiter.api.Assertions.assertThrows(
                ar.edu.utn.frc.tup.piii.engine.exception.InvalidTurnPhaseException.class,
                () -> validator.validate(new PlaceBasicPokemonAction("pikachu-xy1-42")));
    }
}
