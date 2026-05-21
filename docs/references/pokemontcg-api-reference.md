# pokemontcg.io v2 — Referencia de API

> Generado con datos reales de la API el 2026-05-18.
> Set obligatorio de la consigna: **xy1** (XY Unlimited, 146 cartas, lanzamiento 05/02/2014).

## ¿En qué módulo se usa?

**Módulo 3: Deck Builder.**
La consigna dice textualmente: *"El motor del juego no debe realizar llamadas directas a la API externa durante una partida."*
La API solo se llama durante la construcción de mazos. Los datos se guardan en caché local (PostgreSQL) para que el motor opere solo con la DB.

---

## Endpoints Relevantes

### Base URL
```
https://api.pokemontcg.io/v2/
```
No requiere API Key para consultas básicas. Con Key (header `X-Api-Key`) el rate limit sube a 20.000 req/día. Sin key: 1.000 req/día por IP.

### 1. Obtener todas las cartas del set xy1
```
GET /v2/cards?q=set.id:xy1&pageSize=250&page=1
```
Devuelve hasta 250 cartas por página. El set tiene 146 en total — con una sola llamada se bajan todas.

**Query filters útiles:**
- `q=set.id:xy1` → filtrar por set
- `q=set.id:xy1 supertype:Pokémon` → solo Pokémon del set
- `q=set.id:xy1 name:Charizard` → buscar por nombre
- `select=id,name,supertype,subtypes,hp,types,attacks,weaknesses,retreatCost,images` → traer solo campos necesarios (reduce payload)

### 2. Obtener una carta específica por ID
```
GET /v2/cards/xy1-1
```

### 3. Información del Set
```
GET /v2/sets/xy1
```

---

## Estructura de Respuesta — Carta Pokémon

Ejemplo real: `Venusaur-EX` (xy1-1)

```json
{
  "id": "xy1-1",
  "name": "Venusaur-EX",
  "supertype": "Pokémon",
  "subtypes": ["Basic", "EX"],
  "hp": "180",
  "types": ["Grass"],
  "evolvesTo": ["M Venusaur-EX"],
  "rules": [
    "Pokémon-EX rule: When a Pokémon-EX has been Knocked Out, your opponent takes 2 Prize cards."
  ],
  "attacks": [
    {
      "name": "Poison Powder",
      "cost": ["Grass", "Colorless", "Colorless"],
      "convertedEnergyCost": 3,
      "damage": "60",
      "text": "Your opponent's Active Pokémon is now Poisoned."
    },
    {
      "name": "Jungle Hammer",
      "cost": ["Grass", "Grass", "Colorless", "Colorless"],
      "convertedEnergyCost": 4,
      "damage": "90",
      "text": "Heal 30 damage from this Pokémon."
    }
  ],
  "weaknesses": [{ "type": "Fire", "value": "×2" }],
  "retreatCost": ["Colorless", "Colorless", "Colorless", "Colorless"],
  "convertedRetreatCost": 4,
  "set": { "id": "xy1", "name": "XY", ... },
  "number": "1",
  "rarity": "Rare Holo EX",
  "nationalPokedexNumbers": [3],
  "images": {
    "small": "https://images.pokemontcg.io/xy1/1.png",
    "large": "https://images.pokemontcg.io/xy1/1_hires.png"
  }
}
```

## Estructura de Respuesta — Carta No-Pokémon (Trainer/Energy)

Los Trainers y Energías también tienen `supertype` pero **no tienen** `hp`, `attacks`, `weaknesses` ni `retreatCost`. Tienen `text` con el efecto de la carta y `subtypes` que distingue el tipo exacto.

---

## Campos Críticos y su Mapeo al Motor

| Campo API | Tipo | Uso en el Engine |
|---|---|---|
| `id` | string `"xy1-1"` | PK en la tabla de caché local |
| `name` | string | Validación del límite de 4 copias por nombre en el mazo |
| `supertype` | `"Pokémon"` / `"Trainer"` / `"Energy"` | Determina el tipo de carta |
| `subtypes` | array | `["Basic"]`, `["Stage 1"]`, `["Item"]`, `["Supporter"]`, `["Stadium"]`, `["EX"]`, `["MEGA"]` |
| `hp` | string numérico | Convertir a `int` al cachear |
| `types` | array | Tipo(s) del Pokémon (`"Fire"`, `"Water"`, etc.) → mapear a nuestro enum `PokemonType` |
| `attacks[].cost` | array de strings | Lista de energías requeridas → mapear a `List<PokemonType>` en nuestro modelo `Attack` |
| `attacks[].convertedEnergyCost` | int | Total de energías requeridas (útil como hint rápido) |
| `attacks[].damage` | string | Puede ser `"60"`, `"90+"`, `"30×"` — el `+` y `×` indican efectos especiales. Parsear con cuidado |
| `weaknesses[].type` | string | Tipo al que es débil. El `value` siempre es `"×2"` en XY1 |
| `retreatCost` | array | Cada elemento es siempre `"Colorless"` en XY1. El `length` del array es el costo de retirada |
| `convertedRetreatCost` | int | Equivale a `retreatCost.length`. Usar este campo directamente |
| `evolvesFrom` | string | Nombre del Pokémon base del que evoluciona |
| `evolvesTo` | array | Pokémon(s) a los que puede evolucionar |
| `rules` | array | Reglas especiales de la carta (EX rule, MEGA rule, etc.) |
| `images.small` | URL | Imagen para el Deck Builder / tablero (85×119 px aprox.) |
| `images.large` | URL | Imagen en alta resolución para zoom o animaciones |

---

## Gotchas Importantes

### 1. `damage` es un String, no un Int
El campo `damage` viene como string: `"60"`, `"90+"`, `"120×"`. El `+` significa "más daño por condición" y el `×` significa "multiplicado por algo". Para el `DamageCalculator`, hay que parsear solo el número base.

```java
// Parseo seguro del daño base
int baseDamage = Integer.parseInt(card.getDamage().replaceAll("[^0-9]", ""));
```

### 2. `hp` también es String
```java
int hp = Integer.parseInt(card.getHp()); // "180" → 180
```

### 3. `retreatCost` siempre es `["Colorless", ...]`
En XY1, el costo de retirada es siempre en energía incolora. El número de cartas en el array es el costo. Usar `convertedRetreatCost` directamente.

### 4. Tipos de energía en `attacks[].cost`
Los valores posibles son: `"Grass"`, `"Fire"`, `"Water"`, `"Lightning"`, `"Psychic"`, `"Fighting"`, `"Darkness"`, `"Metal"`, `"Dragon"`, `"Fairy"`, `"Colorless"`. Mapean 1:1 a nuestro enum `PokemonType`.

### 5. `subtypes` para detectar EX y MEGA
```java
boolean isEx = card.getSubtypes().contains("EX");
boolean isMega = card.getSubtypes().contains("MEGA");
boolean isBasic = card.getSubtypes().contains("Basic");
boolean isStage1 = card.getSubtypes().contains("Stage 1");
boolean isStage2 = card.getSubtypes().contains("Stage 2");
```

### 6. Detectar AS TÁCTICO (ACE SPEC)
En XY1, las cartas AS TÁCTICO tienen el subtipo `"ACE SPEC"` en `subtypes`. Solo puede haber 1 en todo el mazo.

---

## Estrategia de Caché Local (PostgreSQL)

La consigna exige caché obligatoria. Estrategia recomendada:

1. **Al arrancar el servidor por primera vez** (o cuando la tabla esté vacía), hacer una sola llamada `GET /v2/cards?q=set.id:xy1&pageSize=250` y persistir las 146 cartas en la tabla `cards`.
2. **El Deck Builder** jamás llama a la API externa directamente. Lee solo de la tabla local.
3. Las imágenes se sirven desde las URLs de `images.pokemontcg.io` directamente al frontend (no las descargamos). Son CDN públicas y estables.

---

## Respuesta del Endpoint `/v2/sets/xy1`

```json
{
  "data": {
    "id": "xy1",
    "name": "XY",
    "series": "XY",
    "printedTotal": 146,
    "total": 146,
    "legalities": { "unlimited": "Legal", "expanded": "Legal" },
    "ptcgoCode": "XY",
    "releaseDate": "2014/02/05",
    "images": {
      "symbol": "https://images.pokemontcg.io/xy1/symbol.png",
      "logo": "https://images.pokemontcg.io/xy1/logo.png"
    }
  }
}
```
