# Engine Managers & Coordinators

This package contains the core state machine, rule validation, flow orchestrations, and specialized game logic executors for the Pokémon TCG (XY1 rulebook). All classes in `engine/**` are written as pure Java objects (POJOs) completely decoupled from any framework or database concerns.

## Key Components

### 1. State Machine & Flow
* **[TurnManager.java](./TurnManager.java)**: Manages the turn progression lifecycle through its sequential phases: `DrawPhase` -> `MainPhase` -> (`AttackPhase` | `BetweenTurnsPhase`). It coordinates transitions, player turn flips, first-turn attack restrictions, and broadcasts changes via `PhaseEvent` instances to registered `PhaseListener` observers.
* **[DrawPhaseExecutor.java](./DrawPhaseExecutor.java)**: An observer that listens to turn starts to execute the draw action at the beginning of each player's turn. It enforces the first-turn exception (the starting player does not draw on their very first turn) and triggers a `DeckOutVictory` condition if the deck is empty during a required draw.

### 2. Validation & Rules
* **[RuleValidator.java](./RuleValidator.java)**: A pure-read validator that asserts the legality of any incoming `Action` against the current board status, active turn phase, and status effects. It returns a `ValidationResult` (either `Valid` or `Invalid` with a descriptive key) without throwing exceptions or modifying state.
* **[DeckValidator.java](./DeckValidator.java)**: Enforces deck construction constraints (e.g. exactly 60 cards, maximum 4 copies of any unique card name, and at most 1 `ACE SPEC` card).

### 3. Action Executors
* **[RetreatExecutor.java](./RetreatExecutor.java)**: Performs the retreat mechanic by discarding the required retreat cost energies from the active Pokémon, swapping it with the benched candidate, and clearing all turn-based status effects.
* **[EvolveExecutor.java](./EvolveExecutor.java)**: Handles evolution application on a target in-play Pokémon, replacing its base card and clearing any active status effects.
* **[DamageCalculator.java](./DamageCalculator.java)**: Computes the net attack damage after applying Weakness (usually x2) and Resistance (usually -20).

### 4. Special System Coordinators
* **[SetupManager.java](./SetupManager.java)**: Coordinates the initial match setup flow: shuffles decks, deals 7-card starting hands, handles the mulligan verification loop (drawing extra cards for opponents if a player has no basic Pokémon in hand), lets players place active/benched basic Pokémon, and sets aside the 6 prize cards.
* **[StatusEffectManager.java](./StatusEffectManager.java)**: Stores and transitions conditions like ASLEEP, CONFUSED, PARALYZED, POISONED, and BURNED. It processes turn-based damage and recovery checks during the between-turns phase.
* **[KnockoutManager.java](./KnockoutManager.java)** & **[VictoryConditionChecker.java](./VictoryConditionChecker.java)**: Monitor damage counters and card status after attacks, orchestrating prize card collection, bench validation, and victory resolution (standard wins, deck-out, or sudden death).
