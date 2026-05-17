---
name: pokemon-websockets-strict
description: Skill obligatoria para el desarrollo del módulo de Game Session y WebSockets (Spring Boot + STOMP).
---

# Pokémon TCG - WebSockets & Session Guidelines

## 1. Patrón de Comunicación
- **WebSockets + STOMP:** Prohibido usar WebSockets crudos. Se debe usar el broker de STOMP nativo de Spring Boot (`@EnableWebSocketMessageBroker`).
- Los clientes envían acciones a `/app/game.action`.
- Los clientes se suscriben a `/topic/game/{gameId}` para recibir el estado actualizado.

## 2. Regla de Oro: Seguridad (Niebla de Guerra)
- **JAMÁS se envía la mano entera del rival al cliente.** 
- Cuando el servidor serializa el `GameState` para enviarlo por el WebSocket, debe haber un DTO intermedio que "censure" la mano del rival transformándola simplemente en un número (ej: `opponentHandSize: 5`).
- Si el Frontend recibe los IDs o nombres de las cartas que el rival tiene en la mano, el PR debe ser rechazado automáticamente por vulnerabilidad de seguridad (cheating).

## 3. Concurrencia
- Si dos jugadores envían una acción al mismo milisegundo, no debe corromperse el estado del juego.
- Las acciones entrantes deben encolarse o procesarse con bloques sincronizados a nivel de `gameId` (no sincronizar todo el servidor, solo la partida específica). 
- El `GameEngine` procesa la acción de forma atómica y devuelve el nuevo estado.
