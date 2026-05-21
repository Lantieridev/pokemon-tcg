# Attack Pipeline — Chain of Responsibility

## Architecture

The `AttackPipeline` executes an ordered chain of `AttackPipelineStep` instances against a shared
`AttackContext`. Each step receives the context and a `Runnable next`; calling `next.run()` passes
control to the following step. Omitting the call halts the chain (e.g., blocked attack).

## Step Order (XY1 §3)

| # | Step | Responsibility |
|---|------|---------------|
| 1 | `ValidationStep` | Check that the attacker has the required energy types attached |
| 2 | `PreDamageEffectsStep` | Handle Confusion coin flip; if tails, apply 30 self-damage and block the chain |
| 3 | `DamageCalculationStep` | Base damage → attacker modifiers → Weakness ×2 → Resistance −20 (min 0) → defender modifiers |
| 4 | `DamageApplicationStep` | Write the computed `DamageResult` damage counters onto the defender |
| 5 | `PostDamageEffectsStep` | Apply secondary effects (Poison, Burn, Sleep, Paralysis, Confusion, heal, coin-flip extra) |
| 6 | `KnockoutCheckStep` | Detect defender KO; invoke `KnockoutHandler.onKnockout()` with prize count (2 for EX) |

## Effect Dispatch

`AttackEffectResolver` maps `AttackEffectType → BiConsumer<Integer, AttackContext>`. This avoids
switch/instanceof inside `PostDamageEffectsStep`; adding a new effect is a single map entry.

Effect text format: `"keyword"` or `"keyword:amount"` — e.g. `"poison"`, `"heal:30"`,
`"coin_flip_extra:20"`.

## Key Invariants

- `AttackContext` carries all mutable pipeline state; steps communicate exclusively through it.
- The pipeline is stateless and immutable after construction — safe to share across threads.
- `CoinFlipper` is injected so tests can use `() -> true` (HEADS) or `() -> false` (TAILS).
