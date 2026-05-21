package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.DamageModifier;
import ar.edu.utn.frc.tup.piii.engine.model.DamageResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Mutable context object shared across all steps in the {@link AttackPipeline}.
 * Carries both the immutable inputs (attacker, defender, attack, managers) and the
 * mutable intermediate state accumulated as each step runs.
 *
 * <p>Created via the inner {@link Builder}. Effect text defaults to the empty string
 * (no secondary effect) when not specified.</p>
 */
public final class AttackContext {

    // --- Immutable inputs ---
    private final BattlePokemonState attacker;
    private final BattlePokemonState defender;
    private final Attack attack;
    private final String effectText;
    private final StatusEffectManager attackerStatusManager;
    private final StatusEffectManager defenderStatusManager;
    private final KnockoutHandler knockoutHandler;
    private final CoinFlipper coinFlipper;

    // --- Mutable pipeline state ---
    private boolean attackBlocked;
    private final List<DamageModifier> attackerModifiers = new ArrayList<>();
    private final List<DamageModifier> defenderModifiers = new ArrayList<>();
    private DamageResult damageResult;

    private AttackContext(final Builder b) {
        this.attacker = b.attacker;
        this.defender = b.defender;
        this.attack = b.attack;
        this.effectText = b.effectText;
        this.attackerStatusManager = b.attackerStatusManager;
        this.defenderStatusManager = b.defenderStatusManager;
        this.knockoutHandler = b.knockoutHandler;
        this.coinFlipper = b.coinFlipper;
    }

    // --- Immutable getters ---

    public BattlePokemonState getAttacker() {
        return attacker;
    }

    public BattlePokemonState getDefender() {
        return defender;
    }

    public Attack getAttack() {
        return attack;
    }

    public String getEffectText() {
        return effectText;
    }

    public StatusEffectManager getAttackerStatusManager() {
        return attackerStatusManager;
    }

    public StatusEffectManager getDefenderStatusManager() {
        return defenderStatusManager;
    }

    public KnockoutHandler getKnockoutHandler() {
        return knockoutHandler;
    }

    public CoinFlipper getCoinFlipper() {
        return coinFlipper;
    }

    // --- Mutable state ---

    public boolean isAttackBlocked() {
        return attackBlocked;
    }

    public void setAttackBlocked(final boolean blocked) {
        this.attackBlocked = blocked;
    }

    public void addAttackerModifier(final DamageModifier modifier) {
        attackerModifiers.add(Objects.requireNonNull(modifier, "modifier must not be null"));
    }

    public void addDefenderModifier(final DamageModifier modifier) {
        defenderModifiers.add(Objects.requireNonNull(modifier, "modifier must not be null"));
    }

    public List<DamageModifier> getAttackerModifiers() {
        return Collections.unmodifiableList(attackerModifiers);
    }

    public List<DamageModifier> getDefenderModifiers() {
        return Collections.unmodifiableList(defenderModifiers);
    }

    public DamageResult getDamageResult() {
        return damageResult;
    }

    public void setDamageResult(final DamageResult result) {
        this.damageResult = result;
    }

    // --- Builder ---

    public static final class Builder {

        private final BattlePokemonState attacker;
        private final BattlePokemonState defender;
        private final Attack attack;
        private final StatusEffectManager attackerStatusManager;
        private final StatusEffectManager defenderStatusManager;
        private final KnockoutHandler knockoutHandler;
        private final CoinFlipper coinFlipper;
        private String effectText = "";

        /**
         * @param attacker              the attacking Pokémon (never null)
         * @param defender              the defending Pokémon (never null)
         * @param attack                the attack being executed (never null)
         * @param attackerStatusManager status manager for the attacker (never null)
         * @param defenderStatusManager status manager for the defender (never null)
         * @param knockoutHandler       called when the defender faints (never null)
         * @param coinFlipper           used for probabilistic effects (never null)
         */
        public Builder(
                final BattlePokemonState attacker,
                final BattlePokemonState defender,
                final Attack attack,
                final StatusEffectManager attackerStatusManager,
                final StatusEffectManager defenderStatusManager,
                final KnockoutHandler knockoutHandler,
                final CoinFlipper coinFlipper) {
            this.attacker = Objects.requireNonNull(attacker, "attacker must not be null");
            this.defender = Objects.requireNonNull(defender, "defender must not be null");
            this.attack = Objects.requireNonNull(attack, "attack must not be null");
            this.attackerStatusManager = Objects.requireNonNull(attackerStatusManager,
                    "attackerStatusManager must not be null");
            this.defenderStatusManager = Objects.requireNonNull(defenderStatusManager,
                    "defenderStatusManager must not be null");
            this.knockoutHandler = Objects.requireNonNull(knockoutHandler, "knockoutHandler must not be null");
            this.coinFlipper = Objects.requireNonNull(coinFlipper, "coinFlipper must not be null");
        }

        /**
         * Optional secondary-effect descriptor, e.g. {@code "poison"} or {@code "heal:30"}.
         * Defaults to {@code ""} (no effect).
         *
         * @param effectText effect descriptor (null treated as empty string)
         * @return this builder
         */
        public Builder effectText(final String effectText) {
            this.effectText = effectText != null ? effectText : "";
            return this;
        }

        public AttackContext build() {
            return new AttackContext(this);
        }
    }
}
