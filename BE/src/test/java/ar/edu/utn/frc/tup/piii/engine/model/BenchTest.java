package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.exception.BenchFullException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BenchTest {

    private static final int MAX_BENCH_SIZE = 5;

    private FakeBattlePokemonState pokemon(final String name) {
        final FakeBattlePokemonState p = new FakeBattlePokemonState(60, PokemonType.FIRE, null, null, false);
        p.setName(name);
        return p;
    }

    @Test
    void shouldStartEmpty() {
        assertTrue(new Bench().isEmpty());
        assertEquals(0, new Bench().size());
    }

    @Test
    void placeShouldAddPokemonToBench() {
        final Bench bench = new Bench();
        bench.place(pokemon("Charmander"));
        assertEquals(1, bench.size());
        assertFalse(bench.isEmpty());
    }

    @Test
    void shouldAcceptUpToFivePokemon() {
        final Bench bench = new Bench();
        for (int i = 0; i < MAX_BENCH_SIZE; i++) {
            bench.place(pokemon("P" + i));
        }
        assertEquals(MAX_BENCH_SIZE, bench.size());
        assertTrue(bench.isFull());
    }

    @Test
    void placeShouldThrowBenchFullExceptionWhenAtCapacity() {
        final Bench bench = new Bench();
        for (int i = 0; i < MAX_BENCH_SIZE; i++) {
            bench.place(pokemon("P" + i));
        }
        assertThrows(BenchFullException.class, () -> bench.place(pokemon("sixth")));
    }

    @Test
    void isFullShouldReturnFalseWhenUnderCapacity() {
        final Bench bench = new Bench();
        bench.place(pokemon("P1"));
        assertFalse(bench.isFull());
    }

    @Test
    void removeShouldReturnAndRemovePokemonAtIndex() {
        final Bench bench = new Bench();
        final FakeBattlePokemonState p = pokemon("Squirtle");
        bench.place(p);
        final BattlePokemonState removed = bench.remove(0);
        assertSame(p, removed);
        assertTrue(bench.isEmpty());
    }

    @Test
    void removeShouldShiftRemainingPokemon() {
        final Bench bench = new Bench();
        bench.place(pokemon("A"));
        bench.place(pokemon("B"));
        bench.place(pokemon("C"));
        bench.remove(1);
        assertEquals(2, bench.size());
        assertEquals("C", bench.getAll().get(1).getName());
    }

    @Test
    void promoteShouldRemoveAndReturnPokemonAtIndex() {
        final Bench bench = new Bench();
        final FakeBattlePokemonState p = pokemon("Bulbasaur");
        bench.place(p);
        final BattlePokemonState promoted = bench.promote(0);
        assertSame(p, promoted);
        assertTrue(bench.isEmpty());
    }

    @Test
    void getAllShouldReturnUnmodifiableView() {
        final Bench bench = new Bench();
        bench.place(pokemon("Pikachu"));
        final List<BattlePokemonState> all = bench.getAll();
        assertThrows(UnsupportedOperationException.class, () -> all.add(pokemon("Raichu")));
    }

    @Test
    void getAllShouldReflectCurrentBenchContents() {
        final Bench bench = new Bench();
        bench.place(pokemon("A"));
        bench.place(pokemon("B"));
        assertEquals(2, bench.getAll().size());
        assertEquals("A", bench.getAll().get(0).getName());
    }

    @Test
    void placeShouldThrowWhenNull() {
        assertThrows(NullPointerException.class, () -> new Bench().place(null));
    }

    @Test
    void removeAfterMultiplePlacesShouldNotCorruptState() {
        final Bench bench = new Bench();
        for (int i = 0; i < MAX_BENCH_SIZE; i++) {
            bench.place(pokemon("P" + i));
        }
        bench.remove(2);
        assertFalse(bench.isFull());
        assertEquals(4, bench.size());
        bench.place(pokemon("new"));
        assertTrue(bench.isFull());
    }
}
