package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.model.*;
import ar.edu.utn.frc.tup.piii.engine.pipeline.*;
import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.listener.PokemonTurnInPlayProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.StadiumStateProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PokemonAbilitiesTest {

    private CoinFlipper coinFlipper;
    private TurnManager turnManager;
    private RuleValidator ruleValidator;
    private StatusEffectManager activeSem;
    private StatusEffectManager opponentSem;
    private MatchSession session;
    private PlayerRuntime activeRuntime;
    private PlayerRuntime opponentRuntime;

    @BeforeEach
    void setUp() {
        coinFlipper = mock(CoinFlipper.class);
        turnManager = mock(TurnManager.class);
        activeSem = mock(StatusEffectManager.class);
        opponentSem = mock(StatusEffectManager.class);
        session = mock(MatchSession.class);
        activeRuntime = mock(PlayerRuntime.class);
        opponentRuntime = mock(PlayerRuntime.class);

        when(session.getPlayerRuntime(0)).thenReturn(activeRuntime);
        when(session.getPlayerRuntime(1)).thenReturn(opponentRuntime);
        when(session.getActivePlayerIndex()).thenReturn(0);
        when(session.getTurnManager()).thenReturn(turnManager);
        when(session.getCoinFlipper()).thenReturn(coinFlipper);

        when(activeSem.getPlayerRuntime()).thenReturn(activeRuntime);
        when(opponentSem.getPlayerRuntime()).thenReturn(opponentRuntime);
    }

    @Test
    void testFurCoatPassiveDamageReduction() {
        BattlePokemonState attacker = mock(BattlePokemonState.class);
        BattlePokemonState defender = mock(BattlePokemonState.class);
        Attack attack = new Attack("Slash", 50, List.of(), "");

        when(defender.getAbilities()).thenReturn(List.of(new Ability("Fur Coat", "", AbilityEffectId.FUR_COAT)));

        AttackContext ctx = new AttackContext.Builder(
                attacker, defender, attack, activeSem, opponentSem,
                mock(KnockoutHandler.class), coinFlipper)
                .build();

        int result = PassiveAbilityRegistry.modifyIncomingDamage(50, ctx);
        assertEquals(30, result);
    }

    @Test
    void testSpikyShieldReactiveCounterDamage() {
        BattlePokemonState attacker = mock(BattlePokemonState.class);
        BattlePokemonState defender = mock(BattlePokemonState.class);
        Attack attack = new Attack("Slash", 50, List.of(), "");

        when(defender.getAbilities()).thenReturn(List.of(new Ability("Spiky Shield", "", AbilityEffectId.SPIKY_SHIELD)));

        AttackContext ctx = new AttackContext.Builder(
                attacker, defender, attack, activeSem, opponentSem,
                mock(KnockoutHandler.class), coinFlipper)
                .build();

        ReactiveAbilityHandler.onDamageDealt(ctx, 30);
        verify(attacker, times(1)).addDamageCounters(3);
    }

    @Test
    void testDestinyBurstReactiveKnockoutDamage() {
        BattlePokemonState attacker = mock(BattlePokemonState.class);
        BattlePokemonState defender = mock(BattlePokemonState.class);
        Attack attack = new Attack("Slash", 50, List.of(), "");

        when(defender.getAbilities()).thenReturn(List.of(new Ability("Destiny Burst", "", AbilityEffectId.DESTINY_BURST)));
        when(coinFlipper.flip()).thenReturn(true);

        AttackContext ctx = new AttackContext.Builder(
                attacker, defender, attack, activeSem, opponentSem,
                mock(KnockoutHandler.class), coinFlipper)
                .build();

        ReactiveAbilityHandler.onKnockout(ctx, defender);
        verify(attacker, times(1)).addDamageCounters(5);
    }

    @Test
    void testWaterShurikenActiveAbility() {
        InPlayPokemon source = new InPlayPokemon(new PokemonCard.Builder("greninja", "Greninja", 130, PokemonType.WATER)
                .abilities(List.of(new Ability("Water Shuriken", "", AbilityEffectId.WATER_SHURIKEN)))
                .build());

        Hand activeHand = new Hand();
        EnergyCard waterEnergy = new EnergyCard("water-1", "Water Energy", PokemonType.WATER, true);
        activeHand.addCard(waterEnergy);
        when(activeRuntime.getHand()).thenReturn(activeHand);

        DiscardPile activeDiscard = new DiscardPile();
        when(activeRuntime.getDiscardPile()).thenReturn(activeDiscard);

        BattlePokemonState opponentActive = mock(BattlePokemonState.class);
        when(opponentRuntime.getActivePokemon()).thenReturn(opponentActive);
        when(opponentActive.getMaxHp()).thenReturn(100);

        UseAbilityAction action = new UseAbilityAction(source, "Water Shuriken", -1, -1, List.of());
        
        AbilityEffectResolver resolver = new AbilityEffectResolver();
        resolver.resolve(AbilityEffectId.WATER_SHURIKEN).ifPresent(effect -> effect.apply(session, action));

        // Check Water Energy was discarded from hand
        assertFalse(activeHand.getCards().contains(waterEnergy));
        assertTrue(activeDiscard.getCards().contains(waterEnergy));
        
        // Check 3 damage counters put on opponent active
        verify(opponentActive, times(1)).addDamageCounters(3);
    }

    @Test
    void testDriveOffActiveAbility() {
        InPlayPokemon source = new InPlayPokemon(new PokemonCard.Builder("swellow", "Swellow", 90, PokemonType.COLORLESS)
                .abilities(List.of(new Ability("Drive Off", "", AbilityEffectId.DRIVE_OFF)))
                .build());

        Bench opponentBench = new Bench();
        InPlayPokemon benched = new InPlayPokemon(new PokemonCard.Builder("spewpa", "Spewpa", 70, PokemonType.GRASS).build());
        opponentBench.place(benched);
        when(opponentRuntime.getBench()).thenReturn(opponentBench);

        UseAbilityAction action = new UseAbilityAction(source, "Drive Off", -1, -1, List.of());

        AbilityEffectResolver resolver = new AbilityEffectResolver();
        resolver.resolve(AbilityEffectId.DRIVE_OFF).ifPresent(effect -> effect.apply(session, action));

        verify(session, times(1)).setAwaitingPromotion(1);
        verify(turnManager, times(1)).interruptMainPhase();
    }

    @Test
    void testStanceChangeActiveAbility() {
        PokemonCard aegisBlade = new PokemonCard.Builder("aegislash-blade", "Aegislash", 140, PokemonType.METAL).build();
        PokemonCard aegisShield = new PokemonCard.Builder("aegislash-shield", "Aegislash", 140, PokemonType.METAL).build();

        InPlayPokemon source = new InPlayPokemon(aegisBlade);
        Hand activeHand = new Hand();
        activeHand.addCard(aegisShield);
        when(activeRuntime.getHand()).thenReturn(activeHand);

        UseAbilityAction action = new UseAbilityAction(source, "Stance Change", -1, -1, List.of());

        AbilityEffectResolver resolver = new AbilityEffectResolver();
        resolver.resolve(AbilityEffectId.STANCE_CHANGE).ifPresent(effect -> effect.apply(session, action));

        // Source should now wrap aegisShield
        assertEquals("aegislash-shield", source.getCardId());
        // AegisBlade should be back in hand
        assertTrue(activeHand.getCards().stream().anyMatch(c -> c.getCardId().equals("aegislash-blade")));
        assertFalse(activeHand.getCards().stream().anyMatch(c -> c.getCardId().equals("aegislash-shield")));
    }

    @Test
    void testUpsideDownEvolutionActiveAbility() {
        PokemonCard inkayCard = new PokemonCard.Builder("inkay", "Inkay", 60, PokemonType.DARKNESS).build();
        PokemonCard malamarCard = new PokemonCard.Builder("malamar", "Malamar", 100, PokemonType.DARKNESS).evolvesFrom("Inkay").build();

        InPlayPokemon source = new InPlayPokemon(inkayCard);
        Deck activeDeck = new Deck(List.of(malamarCard));
        when(activeRuntime.getDeck()).thenReturn(activeDeck);

        UseAbilityAction action = new UseAbilityAction(source, "Upside-Down Evolution", -1, -1, List.of());

        AbilityEffectResolver resolver = new AbilityEffectResolver();
        resolver.resolve(AbilityEffectId.UPSIDE_DOWN_EVOLUTION).ifPresent(effect -> effect.apply(session, action));

        // Source should now have evolved into Malamar
        assertEquals("malamar", source.getCardId());
        assertEquals(inkayCard, source.getUnderlyingCards().get(0));
        assertTrue(activeDeck.getCards().isEmpty());
    }
    @Test
    void testRuleValidatorDriveOff() {
        PokemonTurnInPlayProvider tip = mock(PokemonTurnInPlayProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider bsp = mock(ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider hsp = mock(ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider.class);

        ruleValidator = new RuleValidator(turnManager, List.of(activeSem, opponentSem), tip, bsp, hsp);

        InPlayPokemon swellow = new InPlayPokemon(new PokemonCard.Builder("swellow", "Swellow", 90, PokemonType.COLORLESS)
                .abilities(List.of(new Ability("Drive Off", "", AbilityEffectId.DRIVE_OFF)))
                .build());

        UseAbilityAction action = new UseAbilityAction(swellow, "Drive Off", -1, -1, List.of());

        // 1. Swellow is not Active
        when(activeRuntime.getActivePokemon()).thenReturn(mock(BattlePokemonState.class));
        when(bsp.getBenchSize(1)).thenReturn(1);
        ValidationResult result = ruleValidator.validate(action, 0);
        assertTrue(result instanceof ValidationResult.Invalid);
        assertEquals("pokemon_must_be_active", ((ValidationResult.Invalid) result).reason());

        // 2. Swellow is Active, but opponent bench is empty
        when(activeRuntime.getActivePokemon()).thenReturn(swellow);
        when(bsp.getBenchSize(1)).thenReturn(0);
        result = ruleValidator.validate(action, 0);
        assertTrue(result instanceof ValidationResult.Invalid);
        assertEquals("opponent_bench_empty", ((ValidationResult.Invalid) result).reason());

        // 3. Valid
        when(bsp.getBenchSize(1)).thenReturn(2);
        result = ruleValidator.validate(action, 0);
        assertTrue(result instanceof ValidationResult.Valid);
    }

    @Test
    void testRuleValidatorWaterShuriken() {
        PokemonTurnInPlayProvider tip = mock(PokemonTurnInPlayProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider bsp = mock(ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider hsp = mock(ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider.class);

        ruleValidator = new RuleValidator(turnManager, List.of(activeSem, opponentSem), tip, bsp, hsp);

        InPlayPokemon greninja = new InPlayPokemon(new PokemonCard.Builder("greninja", "Greninja", 130, PokemonType.WATER)
                .abilities(List.of(new Ability("Water Shuriken", "", AbilityEffectId.WATER_SHURIKEN)))
                .build());

        UseAbilityAction action = new UseAbilityAction(greninja, "Water Shuriken", -1, -1, List.of());

        // 1. No Water Energy in hand
        Hand handWithoutWater = new Hand();
        handWithoutWater.addCard(new EnergyCard("fairy", "Fairy Energy", PokemonType.FAIRY, true));
        when(activeRuntime.getHand()).thenReturn(handWithoutWater);
        ValidationResult result = ruleValidator.validate(action, 0);
        assertTrue(result instanceof ValidationResult.Invalid);
        assertEquals("water_energy_required_in_hand", ((ValidationResult.Invalid) result).reason());

        // 2. Valid
        Hand handWithWater = new Hand();
        handWithWater.addCard(new EnergyCard("water", "Water Energy", PokemonType.WATER, true));
        when(activeRuntime.getHand()).thenReturn(handWithWater);
        result = ruleValidator.validate(action, 0);
        assertTrue(result instanceof ValidationResult.Valid);
    }

    @Test
    void testRuleValidatorStanceChange() {
        PokemonTurnInPlayProvider tip = mock(PokemonTurnInPlayProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider bsp = mock(ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider hsp = mock(ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider.class);

        ruleValidator = new RuleValidator(turnManager, List.of(activeSem, opponentSem), tip, bsp, hsp);

        InPlayPokemon aegislash = new InPlayPokemon(new PokemonCard.Builder("aegislash-blade", "Aegislash", 140, PokemonType.METAL)
                .abilities(List.of(new Ability("Stance Change", "", AbilityEffectId.STANCE_CHANGE)))
                .build());

        UseAbilityAction action = new UseAbilityAction(aegislash, "Stance Change", -1, -1, List.of());

        // 1. No other Aegislash in hand
        Hand handWithoutAegislash = new Hand();
        when(activeRuntime.getHand()).thenReturn(handWithoutAegislash);
        ValidationResult result = ruleValidator.validate(action, 0);
        assertTrue(result instanceof ValidationResult.Invalid);
        assertEquals("aegislash_required_in_hand", ((ValidationResult.Invalid) result).reason());

        // 2. Valid
        Hand handWithAegislash = new Hand();
        handWithAegislash.addCard(new PokemonCard.Builder("aegislash-shield", "Aegislash", 140, PokemonType.METAL).build());
        when(activeRuntime.getHand()).thenReturn(handWithAegislash);
        result = ruleValidator.validate(action, 0);
        assertTrue(result instanceof ValidationResult.Valid);
    }

    @Test
    void testRuleValidatorUpsideDownEvolution() {
        PokemonTurnInPlayProvider tip = mock(PokemonTurnInPlayProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider bsp = mock(ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider hsp = mock(ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider.class);

        ruleValidator = new RuleValidator(turnManager, List.of(activeSem, opponentSem), tip, bsp, hsp);

        InPlayPokemon inkay = new InPlayPokemon(new PokemonCard.Builder("inkay", "Inkay", 60, PokemonType.DARKNESS)
                .abilities(List.of(new Ability("Upside-Down Evolution", "", AbilityEffectId.UPSIDE_DOWN_EVOLUTION)))
                .build());

        UseAbilityAction action = new UseAbilityAction(inkay, "Upside-Down Evolution", -1, -1, List.of());

        // 1. Inkay is not Active
        when(activeRuntime.getActivePokemon()).thenReturn(mock(BattlePokemonState.class));
        ValidationResult result = ruleValidator.validate(action, 0);
        assertTrue(result instanceof ValidationResult.Invalid);
        assertEquals("pokemon_must_be_active", ((ValidationResult.Invalid) result).reason());

        // 2. Inkay is Active, but not confused
        when(activeRuntime.getActivePokemon()).thenReturn(inkay);
        when(activeSem.has(StatusEffectType.CONFUNDIDO)).thenReturn(false);
        result = ruleValidator.validate(action, 0);
        assertTrue(result instanceof ValidationResult.Invalid);
        assertEquals("pokemon_must_be_confused", ((ValidationResult.Invalid) result).reason());

        // 3. Valid
        when(activeSem.has(StatusEffectType.CONFUNDIDO)).thenReturn(true);
        result = ruleValidator.validate(action, 0);
        assertTrue(result instanceof ValidationResult.Valid);
    }

    @Test
    void testTrevenantForestsCurseItemBlocking() {
        PokemonTurnInPlayProvider tip = mock(PokemonTurnInPlayProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider bsp = mock(ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider hsp = mock(ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider.class);
        StadiumStateProvider ssp = mock(StadiumStateProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.BattlefieldStateProvider bfp = mock(ar.edu.utn.frc.tup.piii.engine.listener.BattlefieldStateProvider.class);

        ruleValidator = new RuleValidator(turnManager, List.of(activeSem, opponentSem), tip, bsp, hsp, ssp, bfp);

        InPlayPokemon trevenant = new InPlayPokemon(new PokemonCard.Builder("trevenant", "Trevenant", 110, PokemonType.PSYCHIC)
                .abilities(List.of(new Ability("Forest's Curse", "", AbilityEffectId.FOREST_CURSE)))
                .build());

        when(bfp.getActivePokemon(1)).thenReturn(trevenant);
        when(turnManager.requireMainPhase()).thenReturn(mock(MainPhase.class));

        // 1. Play Item card -> Should be invalid
        PlayTrainerAction playItem = new PlayTrainerAction(TrainerType.ITEM, null, "xy1-125", TrainerEffectId.ROLLER_SKATES);
        ValidationResult result = ruleValidator.validate(playItem, 0);
        assertTrue(result instanceof ValidationResult.Invalid);
        assertEquals("opponent_forests_curse_active", ((ValidationResult.Invalid) result).reason());

        // 2. Play Supporter card -> Should be valid (or pass Trevenant check)
        PlayTrainerAction playSupporter = new PlayTrainerAction(TrainerType.SUPPORTER, null, "xy1-127", TrainerEffectId.SHAUNA);
        result = ruleValidator.validate(playSupporter, 0);
        assertFalse(result instanceof ValidationResult.Invalid && "opponent_forests_curse_active".equals(((ValidationResult.Invalid) result).reason()));
    }

    @Test
    void testPreDamageEffectsStepCoinFlipsMultiplier() {
        BattlePokemonState attacker = mock(BattlePokemonState.class);
        BattlePokemonState defender = mock(BattlePokemonState.class);
        Attack attack = new Attack("Flash Needle", 40, List.of(), "coin_flips_multiplier:3:40");

        when(coinFlipper.flip()).thenReturn(true, true, false); // 2 heads, 1 tail

        AttackContext ctx = new AttackContext.Builder(
                attacker, defender, attack, activeSem, opponentSem,
                mock(KnockoutHandler.class), coinFlipper)
                .effectText(attack.effectText())
                .build();

        PreDamageEffectsStep step = new PreDamageEffectsStep();
        step.process(ctx, () -> {});

        // 2 heads of 40 = 80 damage
        assertEquals(1, ctx.getAttackerModifiers().size());
        int finalDmg = ctx.getAttackerModifiers().get(0).apply(40);
        assertEquals(80, finalDmg);
    }

    @Test
    void testPreDamageEffectsStepCoinFlipsUntilTails() {
        BattlePokemonState attacker = mock(BattlePokemonState.class);
        BattlePokemonState defender = mock(BattlePokemonState.class);
        Attack attack = new Attack("Continuous Tumble", 30, List.of(), "coin_flips_until_tails:30");

        when(coinFlipper.flip()).thenReturn(true, true, false); // 2 heads, then tail

        AttackContext ctx = new AttackContext.Builder(
                attacker, defender, attack, activeSem, opponentSem,
                mock(KnockoutHandler.class), coinFlipper)
                .effectText(attack.effectText())
                .build();

        PreDamageEffectsStep step = new PreDamageEffectsStep();
        step.process(ctx, () -> {});

        assertEquals(1, ctx.getAttackerModifiers().size());
        int finalDmg = ctx.getAttackerModifiers().get(0).apply(30);
        assertEquals(60, finalDmg);
    }

    @Test
    void testPreDamageEffectsStepCoinFlipsPerEnergy() {
        BattlePokemonState attacker = mock(BattlePokemonState.class);
        BattlePokemonState defender = mock(BattlePokemonState.class);
        Attack attack = new Attack("Rock Black", 50, List.of(), "coin_flips_per_energy:Fighting:50");

        EnergyCard energy1 = new EnergyCard("energy-1", "Fighting Energy", PokemonType.FIGHTING, true);
        EnergyCard energy2 = new EnergyCard("energy-2", "Fighting Energy", PokemonType.FIGHTING, true);
        when(attacker.getAttachedEnergyCards()).thenReturn(List.of(energy1, energy2));

        when(coinFlipper.flip()).thenReturn(true, false); // 1 head, 1 tail

        AttackContext ctx = new AttackContext.Builder(
                attacker, defender, attack, activeSem, opponentSem,
                mock(KnockoutHandler.class), coinFlipper)
                .effectText(attack.effectText())
                .build();

        PreDamageEffectsStep step = new PreDamageEffectsStep();
        step.process(ctx, () -> {});

        assertEquals(1, ctx.getAttackerModifiers().size());
        int finalDmg = ctx.getAttackerModifiers().get(0).apply(50);
        assertEquals(50, finalDmg);
    }

    @Test
    void testPreDamageEffectsStepCoinFlipsPerDamageCounter() {
        BattlePokemonState attacker = mock(BattlePokemonState.class);
        BattlePokemonState defender = mock(BattlePokemonState.class);
        Attack attack = new Attack("Seething Anger", 30, List.of(), "coin_flips_per_damage_counter:30");

        when(attacker.getDamageCounters()).thenReturn(4); // 4 counters
        when(coinFlipper.flip()).thenReturn(true, true, false, false); // 2 heads, 2 tails

        AttackContext ctx = new AttackContext.Builder(
                attacker, defender, attack, activeSem, opponentSem,
                mock(KnockoutHandler.class), coinFlipper)
                .effectText(attack.effectText())
                .build();

        PreDamageEffectsStep step = new PreDamageEffectsStep();
        step.process(ctx, () -> {});

        assertEquals(1, ctx.getAttackerModifiers().size());
        int finalDmg = ctx.getAttackerModifiers().get(0).apply(30);
        assertEquals(60, finalDmg);
    }

    @Test
    void testPlayerRuntimeClearActivePokemonResetsStatusEffects() {
        Deck deck = mock(Deck.class);
        Hand hand = mock(Hand.class);
        Bench bench = mock(Bench.class);
        DiscardPile dp = mock(DiscardPile.class);
        StatusEffectManager sem = mock(StatusEffectManager.class);
        BattlePokemonState active = mock(BattlePokemonState.class);
        List<Card> prizes = new ArrayList<>();

        PlayerRuntime runtime = new PlayerRuntime(deck, hand, bench, dp, sem, active, prizes);
        runtime.clearActivePokemon();

        assertNull(runtime.getActivePokemon());
        verify(sem, times(1)).clearAll();
    }
}
