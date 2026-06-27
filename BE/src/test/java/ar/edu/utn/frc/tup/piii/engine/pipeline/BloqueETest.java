package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.manager.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.*;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.services.GameFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BloqueETest {

    private AttackEffectResolver resolver;
    private FakeBattlePokemonState attacker;
    private FakeBattlePokemonState defender;
    private StatusEffectManager attackerSM;
    private StatusEffectManager defenderSM;
    private KnockoutHandler knockoutHandler;
    private AttackPipeline pipeline;

    @BeforeEach
    void setUp() {
        resolver = new AttackEffectResolver();
        attacker = new FakeBattlePokemonState(100, PokemonType.COLORLESS, null, null, false);
        defender = new FakeBattlePokemonState(100, PokemonType.COLORLESS, null, null, false);
        attackerSM = new StatusEffectManager(() -> true);
        defenderSM = new StatusEffectManager(() -> true);
        knockoutHandler = mock(KnockoutHandler.class);
        pipeline = new AttackPipeline(List.of(
                new ValidationStep(),
                new PreDamageEffectsStep(),
                new DamageCalculationStep(new DamageCalculator()),
                new DamageApplicationStep(),
                new PostDamageEffectsStep(resolver)
        ));
    }

    @Test
    void shouldResolveBloqueETypes() {
        assertEquals(AttackEffectType.SEARCH_DECK_ENERGY, resolver.resolveType("search_deck_energy:2"));
    }

    @Test
    void shouldVerifyFlowerVeilPassiveHpBonus() {
        // Prepare a grass pokemon
        PokemonCard caterpie = new PokemonCard.Builder("caterpie", "Caterpie", 40, PokemonType.GRASS).build();
        InPlayPokemon grassPk = new InPlayPokemon(caterpie);

        // Without Flower Veil
        assertEquals(40, grassPk.getMaxHp());

        // With Floette having Flower Veil in play
        PlayerRuntime runtime = mock(PlayerRuntime.class);
        when(runtime.hasAbility(AbilityEffectId.FLOWER_VEIL)).thenReturn(true);
        grassPk.setOwner(runtime);

        assertEquals(60, grassPk.getMaxHp());
    }

    @Test
    void shouldTriggerParabolicChargeSelectionRequest() {
        final PlayerRuntime attackerRuntime = mock(PlayerRuntime.class);
        final ar.edu.utn.frc.tup.piii.engine.model.Deck deck = mock(ar.edu.utn.frc.tup.piii.engine.model.Deck.class);
        final ar.edu.utn.frc.tup.piii.engine.session.MatchSession session = mock(ar.edu.utn.frc.tup.piii.engine.session.MatchSession.class);
        final ar.edu.utn.frc.tup.piii.engine.manager.TurnManager turnManager = mock(ar.edu.utn.frc.tup.piii.engine.manager.TurnManager.class);
        
        when(attackerRuntime.getDeck()).thenReturn(deck);
        when(session.getTurnManager()).thenReturn(turnManager);
        
        final AttackContext ctx = new AttackContext.Builder(attacker, defender,
                new Attack("Parabolic Charge", 20, List.of(), ""),
                attackerSM, defenderSM,
                knockoutHandler, () -> true)
                .effectText("search_deck_energy:2")
                .attackerRuntime(attackerRuntime)
                .matchSession(session)
                .build();
                
        resolver.apply(ctx);
        
        verify(session).setPendingSelectionRequest(argThat(req ->
                req.sourceEffect() == TrainerEffectId.PARABOLIC_CHARGE
                && req.maxSelections() == 2
                && req.source() == SelectionSource.DECK
        ));
        verify(turnManager).interruptMainPhase();
    }

    @Test
    void shouldApplyParabolicChargeSelection() {
        final GameFacade facade = new GameFacade();
        final MatchSession session = mock(MatchSession.class);
        final PlayerRuntime runtime = mock(PlayerRuntime.class);
        final ar.edu.utn.frc.tup.piii.engine.model.Deck deck = mock(ar.edu.utn.frc.tup.piii.engine.model.Deck.class);
        final ar.edu.utn.frc.tup.piii.engine.model.Hand hand = mock(ar.edu.utn.frc.tup.piii.engine.model.Hand.class);
        final ar.edu.utn.frc.tup.piii.engine.manager.TurnManager turnManager = mock(ar.edu.utn.frc.tup.piii.engine.manager.TurnManager.class);
        
        final PlayerRuntime opponentRuntime = mock(PlayerRuntime.class);
        final StatusEffectManager sem0 = mock(StatusEffectManager.class);
        final StatusEffectManager sem1 = mock(StatusEffectManager.class);
        
        when(session.getActivePlayerIndex()).thenReturn(0);
        when(session.getPlayerRuntime(0)).thenReturn(runtime);
        when(session.getPlayerRuntime(1)).thenReturn(opponentRuntime);
        when(runtime.getStatusEffectManager()).thenReturn(sem0);
        when(opponentRuntime.getStatusEffectManager()).thenReturn(sem1);
        when(runtime.getDeck()).thenReturn(deck);
        when(runtime.getHand()).thenReturn(hand);
        when(session.getTurnManager()).thenReturn(turnManager);
        
        PendingSelectionRequest pendingReq = new PendingSelectionRequest(
                TrainerEffectId.PARABOLIC_CHARGE,
                null,
                2,
                SelectionSource.DECK
        );
        when(session.getPendingSelectionRequest()).thenReturn(pendingReq);
        
        EnergyCard fireEnergy = new EnergyCard("e1", "Fire Energy", PokemonType.FIRE, true);
        when(deck.searchAndRemove(any(), eq(1))).thenReturn(List.of(fireEnergy));
        
        SelectCardsAction action = new SelectCardsAction(List.of("e1"), pendingReq);
        facade.apply(session, action);
        
        verify(hand).addCard(fireEnergy);
        verify(deck).shuffle();
        verify(session).setPendingSelectionRequest(null);
        verify(turnManager).resumeMainPhase();
    }
}
