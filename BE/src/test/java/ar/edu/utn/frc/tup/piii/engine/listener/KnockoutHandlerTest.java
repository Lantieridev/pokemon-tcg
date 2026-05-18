package ar.edu.utn.frc.tup.piii.engine.listener;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for the KnockoutHandler functional interface. FR-014.
 */
class KnockoutHandlerTest {

    private static final int MAX_HP = 100;
    private static final int PRIZES = 2;

    @Test
    void shouldAcceptLambdaImplementationWhenUsedAsFunctionalInterface() {
        KnockoutHandler handler = (knocked, prizes) -> { };
        assertNotNull(handler);
    }

    @Test
    void shouldInvokeCallbackWithCorrectArgumentsWhenKnockoutOccurs() {
        BattlePokemonState pokemon = new FakeBattlePokemonState(MAX_HP, PokemonType.FIRE, null, null, true);
        AtomicReference<BattlePokemonState> capturedKnocked = new AtomicReference<>();
        AtomicInteger capturedPrizes = new AtomicInteger();

        KnockoutHandler handler = (knocked, prizes) -> {
            capturedKnocked.set(knocked);
            capturedPrizes.set(prizes);
        };

        handler.onKnockout(pokemon, PRIZES);

        assertSame(pokemon, capturedKnocked.get());
        assertEquals(PRIZES, capturedPrizes.get());
    }
}
