package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.manager.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import ar.edu.utn.frc.tup.piii.engine.model.Deck;
import ar.edu.utn.frc.tup.piii.engine.model.Hand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class BloqueCTest {

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
    void shouldResolveBloqueCTypes() {
        assertEquals(AttackEffectType.DRAW_CARDS, resolver.resolveType("draw_cards:3"));
        assertEquals(AttackEffectType.DAMAGE_TIMES_SELF_COUNTERS, resolver.resolveType("damage_times_self_counters:10"));
        assertEquals(AttackEffectType.DAMAGE_PER_RETREAT_COST, resolver.resolveType("damage_per_retreat_cost:20"));
    }

    @Test
    void shouldDrawCardsOnAttack() {
        final PlayerRuntime attackerRuntime = mock(PlayerRuntime.class);
        final Hand hand = new Hand();
        final Deck deck = new Deck(new ArrayList<>(List.of(mock(Card.class), mock(Card.class), mock(Card.class))));

        when(attackerRuntime.getHand()).thenReturn(hand);
        when(attackerRuntime.getDeck()).thenReturn(deck);

        final Attack attack = new Attack("Triple Draw", 0, List.of());
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .attackerRuntime(attackerRuntime)
                .effectText("draw_cards:3")
                .build();

        pipeline.execute(ctx);

        assertEquals(3, hand.getCards().size());
        assertEquals(0, deck.size());
    }

    @Test
    void shouldDealDamageTimesSelfCounters() {
        attacker.addDamageCounters(3); // 30 damage on self

        final Attack attack = new Attack("Flail", 0, List.of());
        final AttackContext ctx = new AttackContext.Builder(attacker, defender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .effectText("damage_times_self_counters:10")
                .build();

        pipeline.execute(ctx);

        // 3 counters * 10 = 30 damage -> 3 counters on defender
        assertEquals(3, defender.getDamageCounters());
    }

    @Test
    void shouldDealExtraDamagePerRetreatCost() {
        final FakeBattlePokemonState highRetreatDefender = new FakeBattlePokemonState(100, PokemonType.COLORLESS, null, null, false) {
            @Override
            public int getRetreatCost() {
                return 3;
            }
        };

        final Attack attack = new Attack("Iron Crash", 20, List.of());
        final AttackContext ctx = new AttackContext.Builder(attacker, highRetreatDefender, attack,
                attackerSM, defenderSM, knockoutHandler, () -> true)
                .effectText("damage_per_retreat_cost:20")
                .build();

        pipeline.execute(ctx);

        // 20 base + 3 * 20 = 80 damage -> 8 counters on defender
        assertEquals(8, highRetreatDefender.getDamageCounters());
    }
}
