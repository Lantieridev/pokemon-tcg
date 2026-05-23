package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Rainbow Energy and Double Colorless Energy special behaviour.
 */
class SpecialEnergyTest {

    // -------------------------------------------------------------------------
    // EnergyCard model
    // -------------------------------------------------------------------------

    @Test
    void rainbowEnergyShouldBeFlaggedAsProvidesAllTypes() {
        EnergyCard rainbow = new EnergyCard("rainbow-1", "Rainbow Energy",
                PokemonType.COLORLESS, false, 1, true);

        assertThat(rainbow.isProvidesAllTypes()).isTrue();
        assertThat(rainbow.getEnergyCount()).isEqualTo(1);
        assertThat(rainbow.isSpecial()).isTrue();
        assertThat(rainbow.isBasic()).isFalse();
    }

    @Test
    void doubleColorlessEnergyShouldProvide2Units() {
        EnergyCard dce = new EnergyCard("dce-1", "Double Colorless Energy",
                PokemonType.COLORLESS, false, 2, false);

        assertThat(dce.getEnergyCount()).isEqualTo(2);
        assertThat(dce.isProvidesAllTypes()).isFalse();
        assertThat(dce.isSpecial()).isTrue();
    }

    @Test
    void basicEnergyShouldDefaultTo1UnitAndNotAllTypes() {
        EnergyCard fire = new EnergyCard("fire-1", "Fire Energy", PokemonType.FIRE, true);

        assertThat(fire.getEnergyCount()).isEqualTo(1);
        assertThat(fire.isProvidesAllTypes()).isFalse();
        assertThat(fire.isBasic()).isTrue();
    }

    // -------------------------------------------------------------------------
    // InPlayPokemon — attachment counting
    // -------------------------------------------------------------------------

    @Test
    void doubleColorlessEnergyShouldAdd2EntriesToAttachedEnergiesList() {
        PokemonCard card = new PokemonCard.Builder("pk-1", "TestPokemon", 100, PokemonType.FIRE).build();
        InPlayPokemon pokemon = new InPlayPokemon(card);

        EnergyCard dce = new EnergyCard("dce-1", "Double Colorless Energy",
                PokemonType.COLORLESS, false, 2, false);
        pokemon.attachEnergy(dce);

        // 1 card, 2 energy entries
        assertThat(pokemon.getAttachedEnergyCards()).hasSize(1);
        assertThat(pokemon.getAttachedEnergies()).hasSize(2);
        assertThat(pokemon.getAttachedEnergies()).containsExactly(PokemonType.COLORLESS, PokemonType.COLORLESS);
    }

    @Test
    void rainbowEnergyShouldAdd1EntryToAttachedEnergiesList() {
        PokemonCard card = new PokemonCard.Builder("pk-1", "TestPokemon", 100, PokemonType.WATER).build();
        InPlayPokemon pokemon = new InPlayPokemon(card);

        EnergyCard rainbow = new EnergyCard("rainbow-1", "Rainbow Energy",
                PokemonType.COLORLESS, false, 1, true);
        pokemon.attachEnergy(rainbow);

        assertThat(pokemon.getAttachedEnergyCards()).hasSize(1);
        assertThat(pokemon.getAttachedEnergies()).hasSize(1);
    }

    @Test
    void basicEnergyShouldAdd1EntryToAttachedEnergiesList() {
        PokemonCard card = new PokemonCard.Builder("pk-1", "TestPokemon", 100, PokemonType.FIRE).build();
        InPlayPokemon pokemon = new InPlayPokemon(card);

        EnergyCard fire = new EnergyCard("fire-1", "Fire Energy", PokemonType.FIRE, true);
        pokemon.attachEnergy(fire);

        assertThat(pokemon.getAttachedEnergyCards()).hasSize(1);
        assertThat(pokemon.getAttachedEnergies()).hasSize(1);
        assertThat(pokemon.getAttachedEnergies()).containsExactly(PokemonType.FIRE);
    }
}
