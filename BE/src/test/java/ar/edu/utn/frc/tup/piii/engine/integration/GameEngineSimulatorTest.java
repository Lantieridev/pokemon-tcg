package ar.edu.utn.frc.tup.piii.engine.integration;

import ar.edu.utn.frc.tup.piii.engine.FakeBattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.FakeBattlefieldStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakeBenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakeDeckStateProvider;
import ar.edu.utn.frc.tup.piii.engine.FakePrizeStateProvider;
import ar.edu.utn.frc.tup.piii.engine.listener.PokemonTurnInPlayProvider;
import ar.edu.utn.frc.tup.piii.engine.manager.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.manager.KnockoutManager;
import ar.edu.utn.frc.tup.piii.engine.manager.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.manager.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.manager.VictoryConditionChecker;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.DamageContext;
import ar.edu.utn.frc.tup.piii.engine.model.DamageResult;
import ar.edu.utn.frc.tup.piii.engine.model.DeclareAttackAction;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.ValidationResult;
import ar.edu.utn.frc.tup.piii.engine.model.VictoryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * End-to-End Simulation of the complete Game Engine.
 */
public class GameEngineSimulatorTest {

    private TurnManager turnManager;
    private StatusEffectManager statusEffectManager;
    private DamageCalculator damageCalculator;
    private KnockoutManager knockoutManager;
    private RuleValidator ruleValidator;
    private VictoryConditionChecker victoryChecker;

    private FakeBattlePokemonState p0Active;
    private FakeBattlePokemonState p1Active;
    
    private FakePrizeStateProvider prizeProvider;
    private FakeDeckStateProvider deckProvider;
    private FakeBenchStateProvider benchProvider;
    private FakeBattlefieldStateProvider battlefieldProvider;

    private List<VictoryResult> victories;
    private List<BattlePokemonState> knockouts;

    @BeforeEach
    void setUp() {
        victories = new ArrayList<>();
        knockouts = new ArrayList<>();

        p0Active = new FakeBattlePokemonState(60, PokemonType.FIRE, PokemonType.WATER, PokemonType.GRASS, false);
        p1Active = new FakeBattlePokemonState(60, PokemonType.WATER, PokemonType.LIGHTNING, PokemonType.FIGHTING, false);

        prizeProvider = new FakePrizeStateProvider();
        deckProvider = new FakeDeckStateProvider();
        benchProvider = new FakeBenchStateProvider();
        battlefieldProvider = new FakeBattlefieldStateProvider(p0Active, p1Active);

        turnManager = new TurnManager();
        statusEffectManager = new StatusEffectManager(() -> true);
        damageCalculator = new DamageCalculator();
        
        victoryChecker = new VictoryConditionChecker(prizeProvider, deckProvider, benchProvider, battlefieldProvider, r -> victories.add(r));
        knockoutManager = new KnockoutManager(battlefieldProvider, benchProvider, (pokemon, prizes) -> {
            knockouts.add(pokemon);
            victoryChecker.onKnockout(pokemon, prizes);
        });

        turnManager.registerListener(knockoutManager);
        turnManager.registerListener(victoryChecker);

        PokemonTurnInPlayProvider turnInPlayProvider = p -> 1; // dummy 1 turn in play

        ruleValidator = new RuleValidator(turnManager, statusEffectManager, turnInPlayProvider, benchProvider);
    }

    @Test
    void simulateFullGameLoop() {
        System.out.println("=== GAME START ===");
        
        // P0 (Player 0) Turn 1
        System.out.println("Player 0 starts their first turn.");
        turnManager.startTurn(0);
        turnManager.endDraw();

        Attack ember = new Attack("Ember", 20, List.of(PokemonType.FIRE));
        p0Active.addAttachedEnergy(PokemonType.FIRE); // manually attach energy

        DeclareAttackAction p0Attack = new DeclareAttackAction(p0Active, ember);
        ValidationResult p0Val = ruleValidator.validate(p0Attack);
        
        System.out.println("Player 0 tries to attack... Result: " + p0Val.getClass().getSimpleName());
        // Since it's P0's first turn, it should fail before even hitting the validator if TurnManager was checking, 
        // but RuleValidator only validates pure rules. Wait, RuleValidator does NOT check first turn attack?
        // Ah, TurnManager.declareAttack() throws FirstTurnAttackException!
        
        try {
            turnManager.declareAttack();
        } catch (Exception e) {
            System.out.println("Engine correctly blocked P0 attack: " + e.getMessage());
        }

        turnManager.passTurn();
        turnManager.endBetweenTurns();

        // P1 (Player 1) Turn 1
        System.out.println("\nPlayer 1 starts their turn.");
        // turnManager.startTurn(1); is automatically done by endBetweenTurns!
        turnManager.endDraw();

        Attack waterGun = new Attack("Water Gun", 40, List.of(PokemonType.WATER));
        p1Active.addAttachedEnergy(PokemonType.WATER); // P1 attaches energy

        DeclareAttackAction p1Attack = new DeclareAttackAction(p1Active, waterGun);
        ValidationResult p1Val = ruleValidator.validate(p1Attack);
        System.out.println("Player 1 tries to attack... RuleValidator says: " + p1Val.getClass().getSimpleName());
        
        if (p1Val instanceof ValidationResult.Valid) {
            turnManager.declareAttack();
            System.out.println("P1 attacks P0! Calculating damage...");
            DamageContext ctx = new DamageContext(p1Active, p0Active, waterGun, List.of(), List.of());
            DamageResult dmgResult = damageCalculator.calculate(ctx);
            System.out.println("Base Damage: 40. P0 Weakness to Water? " + (p0Active.getWeaknessType() == PokemonType.WATER));
            System.out.println("Final Damage: " + dmgResult.finalDamage() + " (" + dmgResult.damageCountersToPlace() + " counters)");
            
            p0Active.addDamageCounters(dmgResult.damageCountersToPlace());
            System.out.println("P0 HP remaining: " + (p0Active.getMaxHp() - (p0Active.getDamageCounters() * 10)));
        }

        // P0 is knocked out?
        turnManager.endAttack();
        turnManager.endBetweenTurns(); // Triggers PhaseExited(AttackPhase) which triggers KnockoutManager

        System.out.println("\nKnockouts detected: " + knockouts.size());
        if (!knockouts.isEmpty()) {
            System.out.println("Knocked out Pokémon: " + knockouts.get(0).getPokemonType());
        }
        
        System.out.println("Victories detected: " + victories.size());
        if (victories.isEmpty()) {
            System.out.println("No one won yet... (because prizes wasn't 0 and bench wasn't 0)");
            
            // Let's force a bench-out victory
            System.out.println("Setting P0 bench to 0 to simulate Bench-Out Victory...");
            benchProvider.set(0, 0); // P0 has 0 benched pokemon left
            
            // Re-trigger KO
            victoryChecker.onKnockout(p0Active, 1);
            System.out.println("Victories detected now: " + victories.size());
            if (!victories.isEmpty()) {
                System.out.println("Victory type: " + victories.get(0).getClass().getSimpleName());
            }
        }
    }
}
