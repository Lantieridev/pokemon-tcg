package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.exception.BenchFullException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Mutable bench holding up to 5 Pokémon for one player.
 * Enforces the XY1 maximum bench size at placement time.
 */
public final class Bench {

    private static final int MAX_SIZE = 5;

    private final List<BattlePokemonState> slots = new ArrayList<>();

    /**
     * Places a Pokémon on the bench.
     *
     * @throws BenchFullException    if the bench already holds 5 Pokémon
     * @throws NullPointerException  if pokemon is null
     */
    public void place(final BattlePokemonState pokemon) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        if (slots.size() >= MAX_SIZE) {
            throw new BenchFullException();
        }
        slots.add(pokemon);
    }

    /**
     * Removes and returns the Pokémon at the given position.
     *
     * @param index 0-based bench index
     * @return the removed Pokémon
     */
    public BattlePokemonState remove(final int index) {
        return slots.remove(index);
    }

    /**
     * Removes the Pokémon at the given position and returns it as the new Active Pokémon.
     * Semantically identical to remove; exists to express intent at call sites.
     *
     * @param index 0-based bench index
     * @return the promoted Pokémon
     */
    public BattlePokemonState promote(final int index) {
        return remove(index);
    }

    /** Returns an unmodifiable view of the bench contents. */
    public List<BattlePokemonState> getAll() {
        return Collections.unmodifiableList(slots);
    }

    public boolean isFull() {
        return slots.size() >= MAX_SIZE;
    }

    public boolean isEmpty() {
        return slots.isEmpty();
    }

    public int size() {
        return slots.size();
    }
}
