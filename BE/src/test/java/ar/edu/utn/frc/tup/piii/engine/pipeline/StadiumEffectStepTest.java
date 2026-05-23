package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link StadiumEffectStep}.
 *
 * <p>Validates that:
 * <ul>
 *   <li>Shadow Circle (xy1-126) suppresses Weakness for DARKNESS defenders</li>
 *   <li>Shadow Circle does NOT suppress Weakness for non-DARKNESS defenders</li>
 *   <li>No stadium → Weakness is never suppressed</li>
 * </ul>
 * </p>
 */
class StadiumEffectStepTest {

    private static final String SHADOW_CIRCLE_ID = "xy1-126";
    private static final String FAIRY_GARDEN_ID  = "xy1-117";

    private StadiumEffectStep step;

    @BeforeEach
    void setUp() {
        step = new StadiumEffectStep(new TrainerEffectResolver());
    }

    // -----------------------------------------------------------------------
    // Shadow Circle (xy1-126)
    // -----------------------------------------------------------------------

    @Test
    void shouldSuppressWeaknessWhenShadowCircleActiveAndDefenderIsDarkness() {
        final TrainerCard shadowCircle = new TrainerCard.Builder(SHADOW_CIRCLE_ID, "Shadow Circle", TrainerType.STADIUM)
                .build();

        // Defender is DARKNESS type with PSYCHIC weakness
        final FakeBattlePokemonState defender =
                new FakeBattlePokemonState(100, PokemonType.DARKNESS, PokemonType.PSYCHIC, null, false);
        final FakeBattlePokemonState attacker =
                new FakeBattlePokemonState(80, PokemonType.PSYCHIC, null, null, false);

        final AttackContext ctx = buildContext(attacker, defender, shadowCircle);

        assertFalse(ctx.isWeaknessSuppressed(), "weakness not suppressed before step");
        step.process(ctx, () -> { });
        assertTrue(ctx.isWeaknessSuppressed(), "Shadow Circle must suppress weakness for DARKNESS defender");
    }

    @Test
    void shouldNotSuppressWeaknessWhenShadowCircleActiveButDefenderIsNotDarkness() {
        final TrainerCard shadowCircle = new TrainerCard.Builder(SHADOW_CIRCLE_ID, "Shadow Circle", TrainerType.STADIUM)
                .build();

        // Defender is FIRE type (not DARKNESS)
        final FakeBattlePokemonState defender =
                new FakeBattlePokemonState(100, PokemonType.FIRE, PokemonType.WATER, null, false);
        final FakeBattlePokemonState attacker =
                new FakeBattlePokemonState(80, PokemonType.WATER, null, null, false);

        final AttackContext ctx = buildContext(attacker, defender, shadowCircle);

        step.process(ctx, () -> { });
        assertFalse(ctx.isWeaknessSuppressed(), "Shadow Circle must NOT suppress weakness for non-DARKNESS defender");
    }

    @Test
    void shouldNotSuppressWeaknessWhenNoStadiumIsActive() {
        // No stadium (provider returns null)
        final FakeBattlePokemonState defender =
                new FakeBattlePokemonState(100, PokemonType.DARKNESS, PokemonType.PSYCHIC, null, false);
        final FakeBattlePokemonState attacker =
                new FakeBattlePokemonState(80, PokemonType.PSYCHIC, null, null, false);

        final AttackContext ctx = buildContext(attacker, defender, null);

        step.process(ctx, () -> { });
        assertFalse(ctx.isWeaknessSuppressed(), "No stadium → weakness must not be suppressed");
    }

    @Test
    void shouldNotSuppressWeaknessWhenFairyGardenIsActiveInsteadOfShadowCircle() {
        final TrainerCard fairyGarden = new TrainerCard.Builder(FAIRY_GARDEN_ID, "Fairy Garden", TrainerType.STADIUM)
                .build();

        final FakeBattlePokemonState defender =
                new FakeBattlePokemonState(100, PokemonType.DARKNESS, PokemonType.PSYCHIC, null, false);
        final FakeBattlePokemonState attacker =
                new FakeBattlePokemonState(80, PokemonType.PSYCHIC, null, null, false);

        final AttackContext ctx = buildContext(attacker, defender, fairyGarden);

        step.process(ctx, () -> { });
        assertFalse(ctx.isWeaknessSuppressed(), "Fairy Garden must not suppress weakness");
    }

    @Test
    void shouldCallNextRegardlessOfStadiumEffect() {
        final boolean[] nextCalled = {false};

        final TrainerCard shadowCircle = new TrainerCard.Builder(SHADOW_CIRCLE_ID, "Shadow Circle", TrainerType.STADIUM)
                .build();
        final FakeBattlePokemonState defender =
                new FakeBattlePokemonState(100, PokemonType.DARKNESS, null, null, false);
        final FakeBattlePokemonState attacker =
                new FakeBattlePokemonState(80, PokemonType.FIRE, null, null, false);

        final AttackContext ctx = buildContext(attacker, defender, shadowCircle);
        step.process(ctx, () -> nextCalled[0] = true);

        assertTrue(nextCalled[0], "next() must always be invoked by StadiumEffectStep");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private AttackContext buildContext(final FakeBattlePokemonState attacker,
                                       final FakeBattlePokemonState defender,
                                       final TrainerCard activeStadium) {
        final Attack attack = new Attack("Tackle", 40, List.of(PokemonType.COLORLESS));
        final StatusEffectManager semA = new StatusEffectManager(() -> true);
        final StatusEffectManager semD = new StatusEffectManager(() -> true);
        return new AttackContext.Builder(
                attacker, defender, attack, semA, semD,
                (knocked, prizes) -> { }, () -> false
        )
        .stadiumProvider(() -> activeStadium)
        .build();
    }
}
