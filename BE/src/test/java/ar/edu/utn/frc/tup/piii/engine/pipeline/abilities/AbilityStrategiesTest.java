package ar.edu.utn.frc.tup.piii.engine.pipeline.abilities;

import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.model.*;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import ar.edu.utn.frc.tup.piii.engine.manager.TurnManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbilityStrategiesTest {

    private MatchSession session;
    private PlayerRuntime activeRuntime;
    private PlayerRuntime opponentRuntime;
    private MatchBoard board;
    private TurnManager turnManager;

    @BeforeEach
    void setUp() {
        session = mock(MatchSession.class);
        activeRuntime = mock(PlayerRuntime.class);
        opponentRuntime = mock(PlayerRuntime.class);
        board = mock(MatchBoard.class);
        turnManager = mock(TurnManager.class);

        when(session.getPlayerRuntime(0)).thenReturn(activeRuntime);
        when(session.getPlayerRuntime(1)).thenReturn(opponentRuntime);
        when(session.getActivePlayerIndex()).thenReturn(0);
        when(session.getBoard()).thenReturn(board);
        when(session.getTurnManager()).thenReturn(turnManager);
    }

    @Test
    void testFairyTransferStrategy() {
        FairyTransferStrategy strategy = new FairyTransferStrategy(PokemonType.FAIRY, List.of("rainbow"));
        
        BattlePokemonState fromPokemon = mock(BattlePokemonState.class);
        BattlePokemonState toPokemon = mock(BattlePokemonState.class);
        
        when(board.getActivePokemon(0)).thenReturn(fromPokemon);
        when(board.getBenchedPokemon(0)).thenReturn(List.of(toPokemon));
        
        EnergyCard fairyEnergy = new EnergyCard("fairy-1", "Fairy Energy", PokemonType.FAIRY, true);
        when(fromPokemon.getAttachedEnergyCards()).thenReturn(List.of(fairyEnergy));
        
        UseAbilityAction action = new UseAbilityAction(null, "Fairy Transfer", -1, 0, List.of(0));
        
        strategy.apply(session, action);
        
        verify(fromPokemon, times(1)).removeEnergies(List.of(0));
        verify(toPokemon, times(1)).attachEnergy(fairyEnergy);
    }

    @Test
    void testFairyTransferStrategySpecialEnergy() {
        FairyTransferStrategy strategy = new FairyTransferStrategy(PokemonType.FAIRY, List.of("rainbow"));
        
        BattlePokemonState fromPokemon = mock(BattlePokemonState.class);
        BattlePokemonState toPokemon = mock(BattlePokemonState.class);
        
        when(board.getActivePokemon(0)).thenReturn(fromPokemon);
        when(board.getBenchedPokemon(0)).thenReturn(List.of(toPokemon));
        
        EnergyCard rainbowEnergy = new EnergyCard("rainbow-1", "Rainbow Energy", PokemonType.COLORLESS, true);
        when(fromPokemon.getAttachedEnergyCards()).thenReturn(List.of(rainbowEnergy));
        
        UseAbilityAction action = new UseAbilityAction(null, "Fairy Transfer", -1, 0, List.of(0));
        
        strategy.apply(session, action);
        
        verify(fromPokemon, times(1)).removeEnergies(List.of(0));
        verify(toPokemon, times(1)).attachEnergy(rainbowEnergy);
    }

    @Test
    void testDrawUntilHandSizeStrategy() {
        DrawUntilHandSizeStrategy strategy = new DrawUntilHandSizeStrategy(6);
        
        Hand hand = new Hand();
        hand.addCard(new PokemonCard.Builder("p-1", "Pikachu", 60, PokemonType.LIGHTNING).build());
        when(activeRuntime.getHand()).thenReturn(hand);
        
        Deck deck = mock(Deck.class);
        when(activeRuntime.getDeck()).thenReturn(deck);
        
        Card drawnCard = new PokemonCard.Builder("p-2", "Raichu", 90, PokemonType.LIGHTNING).build();
        when(deck.drawMultiple(5)).thenReturn(List.of(drawnCard));
        
        UseAbilityAction action = new UseAbilityAction(null, "Mystical Fire", -1, -1, List.of());
        
        strategy.apply(session, action);
        
        assertEquals(2, hand.size());
        assertTrue(hand.getCards().contains(drawnCard));
    }

    @Test
    void testDriveOffStrategy() {
        DriveOffStrategy strategy = new DriveOffStrategy();
        Bench opponentBench = mock(Bench.class);
        when(opponentRuntime.getBench()).thenReturn(opponentBench);
        when(opponentBench.isEmpty()).thenReturn(false);

        UseAbilityAction action = new UseAbilityAction(null, "Drive Off", -1, -1, List.of());
        strategy.apply(session, action);

        verify(session, times(1)).setAwaitingPromotion(1);
        verify(turnManager, times(1)).interruptMainPhase();
    }

    @Test
    void testWaterShurikenStrategy() {
        WaterShurikenStrategy strategy = new WaterShurikenStrategy(PokemonType.WATER, 3);
        
        Hand hand = new Hand();
        EnergyCard waterEnergy = new EnergyCard("water-1", "Water Energy", PokemonType.WATER, true);
        hand.addCard(waterEnergy);
        when(activeRuntime.getHand()).thenReturn(hand);
        
        DiscardPile discardPile = new DiscardPile();
        when(activeRuntime.getDiscardPile()).thenReturn(discardPile);
        
        BattlePokemonState targetPokemon = mock(BattlePokemonState.class);
        when(opponentRuntime.getActivePokemon()).thenReturn(targetPokemon);
        when(targetPokemon.getMaxHp()).thenReturn(100);
        when(targetPokemon.getDamageCounters()).thenReturn(0);
        
        UseAbilityAction action = new UseAbilityAction(null, "Water Shuriken", -1, -1, List.of());
        strategy.apply(session, action);

        assertFalse(hand.getCards().contains(waterEnergy));
        assertTrue(discardPile.getCards().contains(waterEnergy));
        verify(targetPokemon, times(1)).addDamageCounters(3);
    }

    @Test
    void testStanceChangeStrategy() {
        StanceChangeStrategy strategy = new StanceChangeStrategy();
        
        InPlayPokemon source = mock(InPlayPokemon.class);
        when(source.getName()).thenReturn("Aegislash");
        when(source.getCardId()).thenReturn("aegislash-blade");
        
        PokemonCard baseCard = new PokemonCard.Builder("aegislash-blade", "Aegislash", 140, PokemonType.METAL).build();
        when(source.getBaseCard()).thenReturn(baseCard);

        Hand hand = new Hand();
        PokemonCard shieldCard = new PokemonCard.Builder("aegislash-shield", "Aegislash", 140, PokemonType.METAL).build();
        hand.addCard(shieldCard);
        when(activeRuntime.getHand()).thenReturn(hand);

        UseAbilityAction action = new UseAbilityAction(source, "Stance Change", -1, -1, List.of());
        strategy.apply(session, action);

        verify(source, times(1)).swapCard(shieldCard);
        assertFalse(hand.getCards().contains(shieldCard));
        assertTrue(hand.getCards().stream().anyMatch(c -> c.getCardId().equals("aegislash-blade")));
    }

    @Test
    void testUpsideDownEvolutionStrategy() {
        UpsideDownEvolutionStrategy strategy = new UpsideDownEvolutionStrategy();

        InPlayPokemon source = mock(InPlayPokemon.class);
        when(source.getName()).thenReturn("Inkay");

        PokemonCard malamarCard = new PokemonCard.Builder("malamar", "Malamar", 100, PokemonType.DARKNESS)
                .evolvesFrom("Inkay").build();

        Deck deck = new Deck(List.of(malamarCard));
        when(activeRuntime.getDeck()).thenReturn(deck);

        UseAbilityAction action = new UseAbilityAction(source, "Upside-Down Evolution", -1, -1, List.of());
        strategy.apply(session, action);

        verify(source, times(1)).evolveInto(malamarCard);
        assertTrue(deck.getCards().isEmpty());
    }

    @Test
    void testBigJumpStrategy_clearsActiveSlot_whenActivePokemonEqualsSourceButIsADifferentInstance() {
        // Regression test: BigJumpStrategy used to compare `runtime.getActivePokemon() == source`
        // by reference. InPlayPokemon.equals() is UUID-based specifically so that the *same*
        // Pokemon survives a JSON deserialization round-trip (new object, same UUID) - the
        // reference check silently failed to recognize that case, leaving a stale Pokemon "in
        // play" with no active slot. Built with two distinct InPlayPokemon instances sharing a
        // UUID (not mocks, since Mockito mocks default equals() to identity too) to prove the
        // fix actually compares by value.
        BigJumpStrategy strategy = new BigJumpStrategy();

        PokemonCard lopunnyCard = new PokemonCard.Builder("lopunny-1", "Lopunny", 90, PokemonType.COLORLESS).build();
        InPlayPokemon source = new InPlayPokemon(lopunnyCard);
        InPlayPokemon activePokemonAfterRoundTrip = new InPlayPokemon(lopunnyCard);
        activePokemonAfterRoundTrip.setUuid(source.getUuid());

        when(activeRuntime.getActivePokemon()).thenReturn(activePokemonAfterRoundTrip);

        Hand hand = new Hand();
        when(activeRuntime.getHand()).thenReturn(hand);

        Bench bench = new Bench();
        when(activeRuntime.getBench()).thenReturn(bench);

        UseAbilityAction action = new UseAbilityAction(source, "Big Jump", -1, -1, List.of());
        strategy.apply(session, action);

        verify(activeRuntime, times(1)).clearActivePokemon();
        assertTrue(hand.getCards().stream().anyMatch(c -> c.getCardId().equals("lopunny-1")));
    }

    @Test
    void testGooeyRegenerationStrategy() {
        GooeyRegenerationStrategy strategy = new GooeyRegenerationStrategy();

        BattlePokemonState source = mock(BattlePokemonState.class);
        EnergyCard fairyEnergy = new EnergyCard("fairy-1", "Fairy Energy", PokemonType.FAIRY, true);
        when(source.getAttachedEnergyCards()).thenReturn(List.of(fairyEnergy));

        DiscardPile discardPile = new DiscardPile();
        when(activeRuntime.getDiscardPile()).thenReturn(discardPile);

        UseAbilityAction action = new UseAbilityAction(source, "Gooey Regeneration", -1, -1, List.of(0));
        strategy.apply(session, action);

        verify(source, times(1)).removeEnergies(List.of(0));
        verify(source, times(1)).heal(60);
        assertTrue(discardPile.getCards().contains(fairyEnergy));
    }
}
