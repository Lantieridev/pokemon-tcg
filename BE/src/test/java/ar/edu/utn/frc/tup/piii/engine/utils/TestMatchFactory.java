package ar.edu.utn.frc.tup.piii.engine.utils;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.Bench;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.Deck;
import ar.edu.utn.frc.tup.piii.engine.model.DiscardPile;
import ar.edu.utn.frc.tup.piii.engine.model.Hand;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerState;

import java.util.List;
import java.util.Map;

/**
 * Factory method for creating completely deterministic MatchSession objects for testing.
 * Provides a reliable testing bed free of runtime NullPointerExceptions due to missing DI.
 */
public final class TestMatchFactory {

    private TestMatchFactory() { }

    public static MatchSession createDeterministicSession() {
        return createDeterministicSession("test-match", "p1", "p2");
    }

    public static MatchSession createDeterministicSession(final String matchId, final String p1, final String p2) {
        FakeBattlePokemonState active0 = new FakeBattlePokemonState(100, PokemonType.FIRE, null, null, false);
        FakeBattlePokemonState target0 = new FakeBattlePokemonState(80, PokemonType.WATER, null, null, false);
        FakeBattlePokemonState active1 = new FakeBattlePokemonState(100, PokemonType.WATER, null, null, false);
        
        Attack sampleAttack = new Attack("Flamethrower", 40, List.of(PokemonType.FIRE));

        PlayerState ps0 = new PlayerState(active0, List.of(target0), List.of(), List.of(sampleAttack), 45, 6, Map.of());
        PlayerState ps1 = new PlayerState(active1, List.of(), List.of(), List.of(), 45, 6, Map.of());

        MatchBoard board = new MatchBoard(List.of(ps0, ps1));

        Card dummyCard = new PokemonCard.Builder("dummy", "Dummy", 10, PokemonType.FIRE).build();
        Deck deck = new Deck(List.of(dummyCard));
        
        PlayerRuntime pr0 = new PlayerRuntime(deck, new Hand(), new Bench(), new DiscardPile(), new StatusEffectManager(() -> true), active0);
        PlayerRuntime pr1 = new PlayerRuntime(deck, new Hand(), new Bench(), new DiscardPile(), new StatusEffectManager(() -> true), active1);

        MatchSession session = new MatchSession(matchId, List.of(p1, p2), board, List.of(pr0, pr1));
        session.setCoinFlipper(() -> true);
        
        return session;
    }
}
