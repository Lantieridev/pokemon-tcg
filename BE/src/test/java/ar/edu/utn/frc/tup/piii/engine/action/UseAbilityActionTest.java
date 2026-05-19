package ar.edu.utn.frc.tup.piii.engine.action;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.FakeBenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakePokemonTurnInPlayProvider;
import ar.edu.utn.frc.tup.piii.engine.exception.InvalidTurnPhaseException;
import ar.edu.utn.frc.tup.piii.engine.manager.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.manager.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.model.MainPhase;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Tests for UseAbilityAction validation in RuleValidator.
 */
class UseAbilityActionTest {

    private static final int HP = 100;

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
    void shouldReturnValidWhenUseAbilityActionInMainPhase() {
        FakeBattlePokemonState source = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        MainPhase mainPhase = new MainPhase();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        ValidationResult result = validator.validate(new UseAbilityAction(source, "speed-boost"));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }

    @Test
    void shouldReturnInvalidWhenUseAbilityActionInAttackPhase() {
        FakeBattlePokemonState source = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        when(turnManager.requireMainPhase()).thenThrow(
                new InvalidTurnPhaseException("Expected MainPhase but was: AttackPhase"));

        assertThrows(InvalidTurnPhaseException.class,
                () -> validator.validate(new UseAbilityAction(source, "speed-boost")));
    }
}
