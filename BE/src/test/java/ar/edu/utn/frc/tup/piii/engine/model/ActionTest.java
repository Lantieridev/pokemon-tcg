package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for the Action sealed interface and its five permits. FR-004.
 */
class ActionTest {

    private static final int HP = 100;

    private FakeBattlePokemonState fakePokemon() {
        return new FakeBattlePokemonState(HP, PokemonType.FIRE, null, null, false);
    }

    @Test
    void shouldHaveTargetComponentWhenEvolveActionIsCreated() {
        FakeBattlePokemonState target = fakePokemon();
        EvolveAction action = new EvolveAction(target, null);

        assertEquals(target, action.target());
    }

    @Test
    void shouldHaveActiveComponentWhenRetreatActionIsCreated() {
        FakeBattlePokemonState active = fakePokemon();
        RetreatAction action = new RetreatAction(active);

        assertEquals(active, action.active());
    }

    @Test
    void shouldHaveTrainerTypeComponentWhenPlayTrainerActionIsCreated() {
        PlayTrainerAction action = new PlayTrainerAction(TrainerType.SUPPORTER);

        assertEquals(TrainerType.SUPPORTER, action.trainerType());
    }

    @Test
    void shouldHaveEnergyTypeComponentWhenAttachEnergyActionIsCreated() {
        AttachEnergyAction action = new AttachEnergyAction(fakePokemon(), PokemonType.FIRE);

        assertEquals(PokemonType.FIRE, action.energyType());
    }

    @Test
    void shouldHaveTwoComponentsWhenDeclareAttackActionIsCreated() {
        FakeBattlePokemonState attacker = fakePokemon();
        Attack attack = new Attack("Ember", 30, List.of(PokemonType.FIRE));
        DeclareAttackAction action = new DeclareAttackAction(attacker, attack);

        assertEquals(attacker, action.attacker());
        assertEquals(attack, action.attack());
    }

    @Test
    void shouldCoverAllSevenPermitsWhenSwitchIsExhaustiveWhenActionIsDispatched() {
        // This test verifies sealed exhaustiveness at compile time — if Action gains
        // a new permit without updating this switch, compilation will fail.
        FakeBattlePokemonState pokemon = fakePokemon();
        Attack attack = new Attack("Splash", 0, List.of());
        Action[] actions = {
            new EvolveAction(pokemon, null),
            new RetreatAction(pokemon),
            new PlayTrainerAction(TrainerType.ITEM),
            new AttachEnergyAction(pokemon, PokemonType.WATER),
            new DeclareAttackAction(pokemon, attack),
            new PlaceBasicPokemonAction("pikachu-xy1-42"),
            new UseAbilityAction(pokemon, "ability-1")
        };

        for (Action action : actions) {
            String label = switch (action) {
                case EvolveAction a            -> "evolve";
                case RetreatAction a           -> "retreat";
                case PlayTrainerAction a       -> "trainer";
                case AttachEnergyAction a      -> "energy";
                case DeclareAttackAction a     -> "attack";
                case PlaceBasicPokemonAction a -> "place";
                case UseAbilityAction a        -> "ability";
            };
            assertNotNull(label);
        }
    }
}
