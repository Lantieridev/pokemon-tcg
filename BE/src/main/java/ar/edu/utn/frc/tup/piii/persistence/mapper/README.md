# CardMapper — Persistence Port

## Role

`CardMapper` is the only class responsible for converting a `CardEntity` (JPA, persistence layer)
into a domain `Card` subtype (`PokemonCard`, `TrainerCard`, `EnergyCard`). It lives in
`persistence/mapper/` — the engine knows nothing about it.

## Dispatch

Type dispatch uses `Map<String, Function<CardEntity, Card>>` keyed on `entity.getSupertype()`
(`"Pokémon"`, `"Trainer"`, `"Energy"`). No `switch` or `instanceof` on the supertype string.

## JSONB Handling

`CardEntity` fields such as `attacks`, `weaknesses`, `resistances`, and `retreatCost` are typed
`Object`. Hibernate may return them as a raw `String` (during tests with `ObjectMapper`) or as an
already-deserialized `List<LinkedHashMap<String, Object>>`. `toListOfMaps(Object raw)` handles
both cases transparently.

## Key Decisions

- **Pokemon type inference**: `CardEntity` has no dedicated type column; the type is inferred from
  the first non-`COLORLESS` energy in the Pokémon's attack costs.
- **ACE SPEC detection**: checking `subtype` alone is insufficient (e.g., "Computer Search" has
  subtype `"Item"` but is ACE SPEC). Detection reads the `rules` JSON array for the substring
  `"ACE SPEC"`.
- **Damage string parsing**: strips non-digits via `replaceAll("[^0-9]", "")` so `"30+"`, `"30×"`,
  and `"30"` all yield `30`.
- **Evolution stage**: `"Stage 2"` is checked before `"Stage 1"` to avoid `contains("Stage 1")`
  matching a `"Stage 2"` subtype string.
