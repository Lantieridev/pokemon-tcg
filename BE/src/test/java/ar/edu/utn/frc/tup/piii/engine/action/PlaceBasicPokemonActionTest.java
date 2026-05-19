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
    private RuleValidator validator;

    @BeforeEach
    void setUp() {
        turnManager = Mockito.mock(TurnManager.class);
        statusEffectManager = Mockito.mock(StatusEffectManager.class);
        turnInPlayProvider = new FakePokemonTurnInPlayProvider();
        benchProvider = new FakeBenchStateProvider();
        validator = new RuleValidator(turnManager, statusEffectManager, turnInPlayProvider, benchProvider);
    }

    @Test
    void shouldReturnValidWhenPlaceBasicPokemonActionInMainPhase() {
        MainPhase mainPhase = new MainPhase();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

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
