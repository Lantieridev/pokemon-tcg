package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.FakeBenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakePokemonTurnInPlayProvider;
import ar.edu.utn.frc.tup.piii.engine.manager.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.manager.TurnManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests for TrainerType enum. FR-003.
 */
class TrainerTypeTest {

    private static final int HP = 100;

    private TurnManager turnManager;
    private StatusEffectManager statusEffectManager;
    private RuleValidator validator;

    @BeforeEach
    void setUp() {
        turnManager = Mockito.mock(TurnManager.class);
        statusEffectManager = Mockito.mock(StatusEffectManager.class);
        validator = new RuleValidator(
                turnManager, statusEffectManager,
                new FakePokemonTurnInPlayProvider(), new FakeBenchStateProvider(), new ar.edu.utn.frc.tup.piii.engine.FakeHandStateProvider());
    }

    @Test
    void shouldHaveExactlyFourConstantsWhenValuesIsCalled() {
        assertEquals(4, TrainerType.values().length);
    }

    @Test
    void shouldContainItemSupporterStadiumAndPokemonToolWhenValuesIsCalled() {
        Set<String> names = Arrays.stream(TrainerType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        assertTrue(names.contains("ITEM"));
        assertTrue(names.contains("SUPPORTER"));
        assertTrue(names.contains("STADIUM"));
        assertTrue(names.contains("POKEMON_TOOL"));
    }

    @Test
    void shouldHavePokemonToolVariant() {
        // Compile-time check: TrainerType.POKEMON_TOOL must exist as an enum constant
        TrainerType tool = TrainerType.POKEMON_TOOL;
        assertEquals("POKEMON_TOOL", tool.name());
    }

    @Test
    void shouldReturnInvalidWhenPokemonToolAlreadyAttached() {
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        target.attachTool(new TrainerCard.Builder("tool", "tool", TrainerType.POKEMON_TOOL).build());
        MainPhase mainPhase = new MainPhase();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        ValidationResult result = validator.validate(new PlayTrainerAction(TrainerType.POKEMON_TOOL, target));

        assertInstanceOf(ValidationResult.Invalid.class, result);
        assertEquals("pokemon_tool_already_attached",
                ((ValidationResult.Invalid) result).reason());
    }

    @Test
    void shouldReturnValidWhenPokemonToolIsAttachedToTargetWithNoTool() {
        FakeBattlePokemonState target = new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
        target.detachTool();
        MainPhase mainPhase = new MainPhase();
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        ValidationResult result = validator.validate(new PlayTrainerAction(TrainerType.POKEMON_TOOL, target));

        assertInstanceOf(ValidationResult.Valid.class, result);
    }
}
