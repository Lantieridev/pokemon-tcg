package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.listener.StadiumStateProvider;
import ar.edu.utn.frc.tup.piii.engine.manager.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.DamageModifier;
import ar.edu.utn.frc.tup.piii.engine.model.DamageResult;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import ar.edu.utn.frc.tup.piii.engine.session.MatchStatisticsTracker;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
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
    private final List<BattlePokemonState> defenderBench;
    private final Attack attack;
    private final String effectText;
    private final StatusEffectManager attackerStatusManager;
    private final StatusEffectManager defenderStatusManager;
    private final KnockoutHandler knockoutHandler;
    private final CoinFlipper coinFlipper;
    private final StadiumStateProvider stadiumProvider;
    private final MatchStatisticsTracker attackerStats;
    private final MatchStatisticsTracker defenderStats;
    private final PlayerRuntime attackerRuntime;
    private final PlayerRuntime defenderRuntime;
    private final MatchSession matchSession;

    // --- Mutable pipeline state ---
    private boolean attackBlocked;
    private boolean scorchingFangDiscarded;
    private boolean weaknessSuppressed;
    private int rockRushDiscardCount;
    private boolean rockRushResolved;
    private final List<DamageModifier> attackerModifiers = new ArrayList<>();
    private final List<DamageModifier> defenderModifiers = new ArrayList<>();
    private DamageResult damageResult;

    private AttackContext(final Builder b) {
        this.attacker = b.attacker;
        this.defender = b.defender;
        this.defenderBench = List.copyOf(b.defenderBench);
        this.attack = b.attack;
        this.effectText = b.effectText;
        this.attackerStatusManager = b.attackerStatusManager;
        this.defenderStatusManager = b.defenderStatusManager;
        this.knockoutHandler = b.knockoutHandler;
        this.coinFlipper = b.coinFlipper;
        this.stadiumProvider = b.stadiumProvider;
        this.attackerStats = b.attackerStats;
        this.defenderStats = b.defenderStats;
        this.attackerRuntime = b.attackerRuntime;
        this.defenderRuntime = b.defenderRuntime;
        this.matchSession = b.matchSession;
    }

    // --- Immutable getters ---

    public BattlePokemonState getAttacker() {
        return attacker;
    }

    public BattlePokemonState getDefender() {
        return defender;
    }

    /**
     * Returns the list of Pokémon on the defender's bench (may be empty if bench access
     * was not provided when building the context).
     *
     * @return defender's benched Pokémon (never null; may be empty)
     */
    public List<BattlePokemonState> getDefenderBench() {
        return defenderBench;
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

    public PlayerRuntime getAttackerRuntime() {
        return attackerRuntime;
    }

    public PlayerRuntime getDefenderRuntime() {
        return defenderRuntime;
    }

    public MatchSession getMatchSession() {
        return matchSession;
    }

    /**
     * Returns the currently active Stadium card, or {@code null} if no Stadium is in play
     * or no provider was set when building the context.
     *
     * @return active Stadium, or null
     */
    public TrainerCard getActiveStadium() {
        return stadiumProvider != null ? stadiumProvider.getActiveStadium() : null;
    }

    // --- Mutable state ---

    public boolean isScorchingFangDiscarded() {
        return scorchingFangDiscarded;
    }

    public void setScorchingFangDiscarded(final boolean scorchingFangDiscarded) {
        this.scorchingFangDiscarded = scorchingFangDiscarded;
    }

    public int getRockRushDiscardCount() {
        return rockRushDiscardCount;
    }

    public void setRockRushDiscardCount(final int rockRushDiscardCount) {
        this.rockRushDiscardCount = rockRushDiscardCount;
    }

    public boolean isRockRushResolved() {
        return rockRushResolved;
    }

    public void setRockRushResolved(final boolean rockRushResolved) {
        this.rockRushResolved = rockRushResolved;
    }

    public boolean isAttackBlocked() {
        return attackBlocked;
    }

    public void setAttackBlocked(final boolean blocked) {
        this.attackBlocked = blocked;
    }

    /**
     * Returns {@code true} if a Stadium effect (e.g. Shadow Circle) has suppressed
     * the defender's Weakness for this attack.
     *
     * @return true if weakness should be ignored in damage calculation
     */
    public boolean isWeaknessSuppressed() {
        return weaknessSuppressed;
    }

    /**
     * Marks the defender's Weakness as suppressed for this attack.
     * Invoked by {@code StadiumEffectStep} when Shadow Circle is active.
     *
     * @param suppressed true to suppress weakness
     */
    public void setWeaknessSuppressed(final boolean suppressed) {
        this.weaknessSuppressed = suppressed;
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

    public MatchStatisticsTracker getAttackerStats() {
        return attackerStats;
    }

    public MatchStatisticsTracker getDefenderStats() {
        return defenderStats;
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
        private List<BattlePokemonState> defenderBench = List.of();
        private StadiumStateProvider stadiumProvider = () -> null;
        private MatchStatisticsTracker attackerStats = new MatchStatisticsTracker();
        private MatchStatisticsTracker defenderStats = new MatchStatisticsTracker();
        private PlayerRuntime attackerRuntime;
        private PlayerRuntime defenderRuntime;
        private MatchSession matchSession;

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

        /**
         * Optional list of Pokémon on the defender's bench (needed for bench-damage effects).
         * Defaults to an empty list.
         *
         * @param bench defender's benched Pokémon (null treated as empty)
         * @return this builder
         */
        public Builder defenderBench(final List<BattlePokemonState> bench) {
            this.defenderBench = bench != null ? bench : List.of();
            return this;
        }

        /**
         * Optional provider for the currently active Stadium card.
         * Defaults to a no-op provider (returns null → no stadium).
         *
         * @param provider the stadium provider (null treated as no-op)
         * @return this builder
         */
        public Builder stadiumProvider(final StadiumStateProvider provider) {
            this.stadiumProvider = provider != null ? provider : () -> null;
            return this;
        }

        public Builder attackerStats(final MatchStatisticsTracker stats) {
            if (stats != null) {
                this.attackerStats = stats;
            }
            return this;
        }

        public Builder defenderStats(final MatchStatisticsTracker stats) {
            if (stats != null) {
                this.defenderStats = stats;
            }
            return this;
        }

        public Builder attackerRuntime(final PlayerRuntime attackerRuntime) {
            this.attackerRuntime = attackerRuntime;
            return this;
        }

        public Builder defenderRuntime(final PlayerRuntime defenderRuntime) {
            this.defenderRuntime = defenderRuntime;
            return this;
        }

        public Builder matchSession(final MatchSession matchSession) {
            this.matchSession = matchSession;
            return this;
        }

        public AttackContext build() {
            return new AttackContext(this);
        }
    }
}
