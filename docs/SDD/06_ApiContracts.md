# SDD Módulo 6: Contratos de API (JSON Schemas)

Para garantizar que el Frontend y el Backend puedan trabajar en paralelo, definimos la estructura exacta de los mensajes JSON que transitarán por WebSockets.

## 1. GameState (El Estado del Tablero)
Este JSON es emitido por el Backend hacia el canal `/topic/match/{matchId}` cada vez que cambia algo en el tablero. El Frontend solo lee este JSON y dibuja.

```json
{
  "matchId": "m-12345",
  "status": "ACTIVE",
  "currentTurnPlayerId": "p-001",
  "turnPhase": "MAIN",
  "stadiumCard": null,
  "players": {
    "p-001": {
      "name": "Ash",
      "prizesRemaining": 6,
      "deckCount": 45,
      "handCount": 5, 
      "activePokemon": {
        "id": "card-instance-999",
        "cardId": "xy1-1",
        "name": "Venusaur-EX",
        "currentHp": 120,
        "maxHp": 180,
        "damageCounters": 6,
        "specialConditions": ["POISONED"],
        "attachedEnergies": ["xy1-135", "xy1-135"],
        "attachedTool": null
      },
      "bench": [
        {
          "id": "card-instance-888",
          "cardId": "xy1-4",
          "name": "Weedle",
          "currentHp": 50,
          "maxHp": 50,
          "damageCounters": 0,
          "specialConditions": [],
          "attachedEnergies": [],
          "attachedTool": null
        }
      ]
    },
    "p-002": {
      "name": "Gary",
      "prizesRemaining": 6,
      "deckCount": 42,
      "handCount": 7,
      "activePokemon": { ... },
      "bench": [ ... ]
    }
  },
  "lastActionLog": "Ash unió una Energía Planta a Venusaur-EX."
}
```
*Nota de Seguridad:* La mano del rival (`p-002`) solo envía `handCount`, nunca las cartas reales, cumpliendo RNF-05.

## 2. ActionRequest (Acciones del Jugador)
Enviado por el Frontend hacia `/app/match/{matchId}/action`.

### Jugar una carta desde la mano (Ej: Energía o Entrenador)
```json
{
  "playerId": "p-001",
  "actionType": "PLAY_CARD",
  "payload": {
    "handCardInstanceId": "card-instance-123",
    "targetInstanceId": "card-instance-999" 
  }
}
```

### Atacar
```json
{
  "playerId": "p-001",
  "actionType": "ATTACK",
  "payload": {
    "attackIndex": 0 
  }
}
```

### Retirar Pokémon
```json
{
  "playerId": "p-001",
  "actionType": "RETREAT",
  "payload": {
    "benchPokemonInstanceId": "card-instance-888",
    "discardedEnergyInstanceIds": ["card-instance-101", "card-instance-102"]
  }
}
```
*Nota:* El jugador especifica explícitamente qué energías descarta de su Activo para pagar el costo de retirada.
