# /audit — Compliance Audit

Run this audit after completing each PR batch or before committing. Read the two source-of-truth files FIRST, then check every item below against the ACTUAL code.

## Step 1 — Load Sources of Truth

1. Read `docs/references/consigna.txt` (full file)
2. Read `docs/SKILLS/game-rules-reference.md` (full file)
3. Read `docs/ARCHITECTURE.md`

## Step 2 — Engine Rules Checklist (RF-01)

For each item, grep/read the relevant source file and verify correctness:

- [ ] **Draw Phase**: DrawPhase forces exactly 1 card draw. DeckOutVictory fires if deck is empty.
- [ ] **Main Phase limits**: 1 energy, 1 supporter, 1 stadium, 1 retreat per turn — enforced by MainPhase counters.
- [ ] **Evolution restrictions**: Cannot evolve on first turn of player. Cannot evolve a Pokémon that entered play this turn (turnsInPlay < 1).
- [ ] **Evolution clears status**: Evolving a Pokémon MUST call statusEffectManager.clearAll().
- [ ] **First-turn attack ban**: The player who goes FIRST cannot attack on their first turn (not hardcoded to player index 0).
- [ ] **Retreat discards energy**: Retreat must discard energy cards equal to retreatCost from the active Pokémon.
- [ ] **Retreat clears status**: RetreatExecutor calls clearAll() on status effects.
- [ ] **Attack energy validation**: Colorless requirement accepts ANY energy type (wildcard).
- [ ] **Damage calculation order**: base → attacker modifiers → weakness (×2) → resistance (−20) → defender modifiers → floor(0).
- [ ] **Between-turns order**: Poison → Burn → Abilities (in that order).
- [ ] **Status effect exclusion**: Asleep/Confused/Paralyzed share one rotation slot — applying one removes the other.
- [ ] **KO detection**: damageCounters × 10 >= maxHp.
- [ ] **Prize cards**: 1 for normal KO, 2 for EX KO.
- [ ] **Bench-out**: Checked for BOTH players (attacker can lose active to poison in between-turns).
- [ ] **Sudden Death**: If both players reach 0 remaining prizes simultaneously → SuddenDeath, not PrizeVictory.
- [ ] **Pokémon Tool**: Max 1 tool per Pokémon. PlayTrainerAction(POKEMON_TOOL, null) must be INVALID.
- [ ] **Mulligan**: Setup phase with mulligan logic exists and is tested.

## Step 3 — Card Types Checklist (RF-02)

- [ ] **EvolutionStage enum**: BASIC, STAGE_1, STAGE_2 exists. EvolveAction validates evolution chain.
- [ ] **ACE SPEC (AS TÁCTICO)**: Modeled in engine. Deck validation enforces max 1 ACE SPEC per deck.
- [ ] **Stadium replacement**: Playing a new Stadium discards the previous one.

## Step 4 — Session & Transport Checklist (RF-03, RF-06)

- [ ] **Match states**: WAITING → SETUP → ACTIVE → FINISHED (4 states, not 3).
- [ ] **War-fog DTOs**: OpponentView has ONLY `handSize` (int), never card IDs. Prizes content hidden. Deck order hidden.
- [ ] **BattlePokemonDTO completeness**: Contains name, cardId, attachedEnergies, retreatCost, hasToolAttached, attacks.
- [ ] **Lock discipline**: lock → validate → apply → persist → unlock → broadcast (ADR-5).
- [ ] **Disconnect lock**: onPlayerDisconnect acquires lock before setting timeout future.
- [ ] **GameFacade mapping**: ATTACH_ENERGY uses real energy type from DTO, not hardcoded COLORLESS.
- [ ] **GameFacade evolve**: EVOLVE resolves target from targetIndex (can evolve benched Pokémon too).
- [ ] **processAction applies**: After validation, the action is ACTUALLY applied to the board (not just validated and ignored).

## Step 5 — Infrastructure Checklist (RNF + Entregables)

- [ ] **Engine isolation**: Zero `org.springframework.*` imports in `engine/**` package.
- [ ] **JaCoCo plugin**: Present in pom.xml with ≥80% global and ≥90% thresholds for critical managers.
- [ ] **Swagger/OpenAPI**: `springdoc-openapi-starter-webmvc-ui` dependency in pom.xml.
- [ ] **Flyway**: Migration scripts exist in `src/main/resources/db/migration/`.
- [ ] **Chain of Responsibility**: Attack resolution pipeline uses explicit chain pattern (RNF-04 requirement).

## Step 6 — Output

Generate a report with 3 sections:

### ✅ Rules Passed
List items that are correctly implemented with file:line evidence.

### ⚠️ Warnings
Items that work but are fragile or have edge cases not covered by tests.

### 🚨 VIOLATIONS (Blockers)
Items that contradict the consigna or rulebook. For each:
1. Cite the exact consigna line number
2. Show the offending code
3. Explain what it should do instead

**IMPORTANT**: After completing the audit, print this summary line so the user knows it ran:

```
🔍 AUDIT COMPLETE — ✅ {passed} passed | ⚠️ {warnings} warnings | 🚨 {blockers} blockers
```
