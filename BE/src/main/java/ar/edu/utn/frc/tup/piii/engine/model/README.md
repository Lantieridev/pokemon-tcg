# Engine Model — Card Hierarchy and Battle State

## Card Hierarchy

```
Card (interface)
├── PokemonCard      — immutable static data (HP, type, attacks, evolution stage)
├── TrainerCard      — immutable static data (TrainerType, aceSpec flag)
└── EnergyCard       — immutable static data (energyType, isBasic)
```

Cards are **data objects** only. They live in the `Hand`, `Deck`, or `DiscardPile`.

## BattlePokemonState vs InPlayPokemon

`BattlePokemonState` (interface) represents a Pokémon that has been placed on the field.
`InPlayPokemon` is the sole concrete implementation:

| Aspect | Source |
|--------|--------|
| Static data (HP, type, attacks) | Delegated to the wrapped `PokemonCard` |
| Mutable battle state (damage counters, energies, tool) | Owned by `InPlayPokemon` |

`FakeBattlePokemonState` (test-only) implements the same interface for unit tests that do not need
a real `PokemonCard`.

## Runtime Aggregates

| Class | Contents |
|-------|----------|
| `Deck` | Ordered list of `Card`; `draw()` removes the top card or throws `DeckEmptyException` |
| `Hand` | List of `Card`; `removeCard(cardId)` or throws `CardNotInHandException` |
| `Bench` | Up to 5 `BattlePokemonState` slots; `place()` / `remove()` / `promote()` |
| `DiscardPile` | Append-only list of discarded `Card` objects |

## PlayerRuntime

`PlayerRuntime` aggregates the live mutable state for one player during an active match:
`Deck`, `Hand`, `Bench`, `DiscardPile`, `StatusEffectManager`, and the current `activePokemon`
(mutable via `setActivePokemon()`).

`PlayerState` is an **immutable snapshot** used by `MatchBoard` and provider interfaces
(`BattlefieldStateProvider`, `DeckStateProvider`, etc.) — it does not track live mutations.
