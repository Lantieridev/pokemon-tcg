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
    void testIntimidatingManePassiveDamageBlock() {
        BattlePokemonState attacker = mock(BattlePokemonState.class);
        BattlePokemonState defender = mock(BattlePokemonState.class);
        Attack attack = new Attack("Slash", 50, List.of(), "");

        when(defender.getAbilities()).thenReturn(List.of(new Ability("Intimidating Mane", "", AbilityEffectId.INTIMIDATING_MANE)));

        // Test Case 1: Attacker is a Basic Pokémon -> Damage should be blocked (0)
        when(attacker.getEvolutionStage()).thenReturn(EvolutionStage.BASIC);
        AttackContext ctx1 = new AttackContext.Builder(
                attacker, defender, attack, activeSem, opponentSem,
                mock(KnockoutHandler.class), coinFlipper)
                .build();

        int result1 = PassiveAbilityRegistry.modifyIncomingDamage(50, ctx1);
        assertEquals(0, result1);

        // Test Case 2: Attacker is a Stage 1 Pokémon -> Damage should NOT be blocked (50)
        when(attacker.getEvolutionStage()).thenReturn(EvolutionStage.STAGE_1);
        AttackContext ctx2 = new AttackContext.Builder(
                attacker, defender, attack, activeSem, opponentSem,
                mock(KnockoutHandler.class), coinFlipper)
                .build();

        int result2 = PassiveAbilityRegistry.modifyIncomingDamage(50, ctx2);
        assertEquals(50, result2);
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
    void testMysticalFireActiveAbility() {
        InPlayPokemon source = new InPlayPokemon(new PokemonCard.Builder("delphox", "Delphox", 140, PokemonType.FIRE)
                .abilities(List.of(new Ability("Mystical Fire", "", AbilityEffectId.MYSTICAL_FIRE)))
                .build());

        Hand activeHand = new Hand();
        activeHand.addCard(new EnergyCard("fire-1", "Fire Energy", PokemonType.FIRE, true));
        when(activeRuntime.getHand()).thenReturn(activeHand);

        List<Card> deckCards = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            deckCards.add(new EnergyCard("fire-" + (i + 2), "Fire Energy", PokemonType.FIRE, true));
        }
        Deck activeDeck = new Deck(deckCards);
        when(activeRuntime.getDeck()).thenReturn(activeDeck);

        UseAbilityAction action = new UseAbilityAction(source, "Mystical Fire", -1, -1, List.of());

        AbilityEffectResolver resolver = new AbilityEffectResolver();
        resolver.resolve(AbilityEffectId.MYSTICAL_FIRE).ifPresent(effect -> effect.apply(session, action));

        assertEquals(6, activeHand.size());
        assertEquals(5, activeDeck.getCards().size());
    }

    @Test
    void testLeafDrawActiveAbility() {
        InPlayPokemon source = new InPlayPokemon(new PokemonCard.Builder("shiftry", "Shiftry", 140, PokemonType.GRASS)
                .abilities(List.of(new Ability("Leaf Draw", "", AbilityEffectId.LEAF_DRAW)))
                .build());

        Hand activeHand = new Hand();
        EnergyCard grassEnergy = new EnergyCard("grass-1", "Grass Energy", PokemonType.GRASS, true);
        activeHand.addCard(grassEnergy);
        when(activeRuntime.getHand()).thenReturn(activeHand);

        DiscardPile activeDiscard = new DiscardPile();
        when(activeRuntime.getDiscardPile()).thenReturn(activeDiscard);

        List<Card> deckCards = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            deckCards.add(new EnergyCard("grass-" + (i + 2), "Grass Energy", PokemonType.GRASS, true));
        }
        Deck activeDeck = new Deck(deckCards);
        when(activeRuntime.getDeck()).thenReturn(activeDeck);

        UseAbilityAction action = new UseAbilityAction(source, "Leaf Draw", -1, -1, List.of());

        AbilityEffectResolver resolver = new AbilityEffectResolver();
        resolver.resolve(AbilityEffectId.LEAF_DRAW).ifPresent(effect -> effect.apply(session, action));

        assertFalse(activeHand.getCards().contains(grassEnergy));
        assertTrue(activeDiscard.getCards().contains(grassEnergy));
        assertEquals(3, activeHand.size());
        assertEquals(2, activeDeck.getCards().size());
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

    @Test
    void testRuleValidatorLeafDraw() {
        PokemonTurnInPlayProvider tip = mock(PokemonTurnInPlayProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider bsp = mock(ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider hsp = mock(ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider.class);

        ruleValidator = new RuleValidator(turnManager, List.of(activeSem, opponentSem), tip, bsp, hsp);

        InPlayPokemon shiftry = new InPlayPokemon(new PokemonCard.Builder("shiftry", "Shiftry", 140, PokemonType.GRASS)
                .abilities(List.of(new Ability("Leaf Draw", "", AbilityEffectId.LEAF_DRAW)))
                .build());

        UseAbilityAction action = new UseAbilityAction(shiftry, "Leaf Draw", -1, -1, List.of());

        // 1. No Grass Energy in hand
        Hand handWithoutGrass = new Hand();
        handWithoutGrass.addCard(new EnergyCard("fire", "Fire Energy", PokemonType.FIRE, true));
        when(activeRuntime.getHand()).thenReturn(handWithoutGrass);
        ValidationResult result = ruleValidator.validate(action, 0);
        assertTrue(result instanceof ValidationResult.Invalid);
        assertEquals("grass_energy_required_in_hand", ((ValidationResult.Invalid) result).reason());

        // 2. Valid
        Hand handWithGrass = new Hand();
        handWithGrass.addCard(new EnergyCard("grass", "Grass Energy", PokemonType.GRASS, true));
        when(activeRuntime.getHand()).thenReturn(handWithGrass);
        result = ruleValidator.validate(action, 0);
        assertTrue(result instanceof ValidationResult.Valid);
    }

    @Test
    void testEnergyGraceActiveAbility() {
        InPlayPokemon milotic = new InPlayPokemon(new PokemonCard.Builder("milotic", "Milotic", 100, PokemonType.WATER)
                .abilities(List.of(new Ability("Energy Grace", "", AbilityEffectId.ENERGY_GRACE)))
                .build());

        InPlayPokemon activeTarget = new InPlayPokemon(new PokemonCard.Builder("target", "Non-EX Target", 90, PokemonType.GRASS).build());

        Bench activeBench = new Bench();
        activeBench.place(milotic);
        when(activeRuntime.getBench()).thenReturn(activeBench);
        when(activeRuntime.getActivePokemon()).thenReturn(activeTarget);

        DiscardPile discardPile = new DiscardPile();
        EnergyCard energy1 = new EnergyCard("water-1", "Water Energy", PokemonType.WATER, true);
        EnergyCard energy2 = new EnergyCard("water-2", "Water Energy", PokemonType.WATER, true);
        EnergyCard energy3 = new EnergyCard("water-3", "Water Energy", PokemonType.WATER, true);
        discardPile.add(energy1);
        discardPile.add(energy2);
        discardPile.add(energy3);
        when(activeRuntime.getDiscardPile()).thenReturn(discardPile);

        KnockoutHandler knockoutHandler = mock(KnockoutHandler.class);
        when(session.getKnockoutHandler()).thenReturn(knockoutHandler);

        UseAbilityAction action = new UseAbilityAction(milotic, "Energy Grace", 0, -1, List.of());

        AbilityEffectResolver resolver = new AbilityEffectResolver();
        resolver.resolve(AbilityEffectId.ENERGY_GRACE).ifPresent(effect -> effect.apply(session, action));

        verify(knockoutHandler, times(1)).onKnockout(milotic, 1);
        assertEquals(3, activeTarget.getAttachedEnergyCards().size());
        assertTrue(activeTarget.getAttachedEnergyCards().contains(energy1));
        assertTrue(activeTarget.getAttachedEnergyCards().contains(energy2));
        assertTrue(activeTarget.getAttachedEnergyCards().contains(energy3));
        assertTrue(discardPile.getCards().isEmpty());
    }

    @Test
    void testRuleValidatorEnergyGrace() {
        PokemonTurnInPlayProvider tip = mock(PokemonTurnInPlayProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider bsp = mock(ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider hsp = mock(ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider.class);

        ruleValidator = new RuleValidator(turnManager, List.of(activeSem, opponentSem), tip, bsp, hsp);

        InPlayPokemon milotic = new InPlayPokemon(new PokemonCard.Builder("milotic", "Milotic", 100, PokemonType.WATER)
                .abilities(List.of(new Ability("Energy Grace", "", AbilityEffectId.ENERGY_GRACE)))
                .build());

        InPlayPokemon exTarget = new InPlayPokemon(new PokemonCard.Builder("target-ex", "EX Target", 180, PokemonType.FIRE)
                .ex(true)
                .build());

        Bench activeBench = new Bench();
        activeBench.place(milotic);
        when(activeRuntime.getBench()).thenReturn(activeBench);
        when(activeRuntime.getActivePokemon()).thenReturn(exTarget);

        DiscardPile discardPile = new DiscardPile();
        when(activeRuntime.getDiscardPile()).thenReturn(discardPile);

        UseAbilityAction action = new UseAbilityAction(milotic, "Energy Grace", 0, -1, List.of());

        ValidationResult result = ruleValidator.validate(action, 0);
        assertTrue(result instanceof ValidationResult.Invalid);
        assertEquals("basic_energy_required_in_discard", ((ValidationResult.Invalid) result).reason());

        discardPile.add(new EnergyCard("grass", "Grass Energy", PokemonType.GRASS, true));

        result = ruleValidator.validate(action, 0);
        assertTrue(result instanceof ValidationResult.Invalid);
        assertEquals("no_valid_non_ex_target_in_play", ((ValidationResult.Invalid) result).reason());

        InPlayPokemon nonExTarget = new InPlayPokemon(new PokemonCard.Builder("target-non-ex", "Non-EX Target", 90, PokemonType.FIRE).build());
        when(activeRuntime.getActivePokemon()).thenReturn(nonExTarget);
        result = ruleValidator.validate(action, 0);
        assertTrue(result instanceof ValidationResult.Valid);
    }

    @Test
    void testHandLockEffect() {
        PokemonTurnInPlayProvider tip = mock(PokemonTurnInPlayProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider bsp = mock(ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider hsp = mock(ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.BattlefieldStateProvider bfp = mock(ar.edu.utn.frc.tup.piii.engine.listener.BattlefieldStateProvider.class);
        ar.edu.utn.frc.tup.piii.engine.listener.StadiumStateProvider ssp = mock(ar.edu.utn.frc.tup.piii.engine.listener.StadiumStateProvider.class);

        ruleValidator = new RuleValidator(turnManager, List.of(activeSem, opponentSem), tip, bsp, hsp, ssp, bfp);

        InPlayPokemon myPokemon = new InPlayPokemon(new PokemonCard.Builder("my-pokemon", "Active", 100, PokemonType.FIRE).build());
        InPlayPokemon barbaracle = new InPlayPokemon(new PokemonCard.Builder("barbaracle", "Barbaracle", 100, PokemonType.WATER)
                .abilities(List.of(new Ability("Hand Lock", "", AbilityEffectId.HAND_LOCK)))
                .build());

        when(bfp.getActivePokemon(1)).thenReturn(barbaracle);
        when(bfp.getActivePokemon(0)).thenReturn(myPokemon);
        when(bsp.getBenchedPokemon(1)).thenReturn(List.of());

        MainPhase mainPhase = mock(MainPhase.class);
        when(mainPhase.getEnergyAttached()).thenReturn(0);
        when(turnManager.requireMainPhase()).thenReturn(mainPhase);

        AttachEnergyAction attachSpecial = new AttachEnergyAction(myPokemon, PokemonType.COLORLESS);
        AttachEnergyAction attachBasic = new AttachEnergyAction(myPokemon, PokemonType.FIRE);

        EnergyCard specialEnergy = new EnergyCard("double-colorless", "Double Colorless", PokemonType.COLORLESS, false, 2, false);
        EnergyCard basicEnergy = new EnergyCard("fire-energy", "Fire Energy", PokemonType.FIRE, true);
        when(hsp.getHandCards(0)).thenReturn(List.of(specialEnergy, basicEnergy));

        ValidationResult resultBasic = ruleValidator.validate(attachBasic, 0);
        assertTrue(resultBasic instanceof ValidationResult.Valid);

        ValidationResult resultSpecial = ruleValidator.validate(attachSpecial, 0);
        assertTrue(resultSpecial instanceof ValidationResult.Invalid);
        assertEquals("opponent_hand_lock_active", ((ValidationResult.Invalid) resultSpecial).reason());
    }
}
