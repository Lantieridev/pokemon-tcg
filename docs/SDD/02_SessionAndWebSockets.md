# SDD Módulo 2: Session & WebSockets

Este módulo se encarga de la conectividad en tiempo real de la partida, conectando los comandos emitidos por el FrontEnd con las validaciones del Game Engine (Módulo 1).

## Objetivo
Configurar Spring Boot con WebSockets STOMP para enrutar acciones de los jugadores y emitir de forma segura las actualizaciones del estado del tablero (State Synchronization).

## Arquitectura de Sincronización

### 1. Protocolo y Broker
- Se usará **WebSockets con STOMP** (Simple Text Oriented Messaging Protocol). Spring proporciona abstracciones seguras mediante `@MessageMapping` y `SimpMessagingTemplate`.
- Las partidas tendrán "canales" privados. Ejemplo: `/topic/match/{matchId}`.
- Información confidencial: El mazo del oponente y las cartas exactas en su mano **no** se envían en el payload, solo la longitud (cantidad de cartas) por requerimiento de seguridad de la consigna.

### 2. Flujo de un Comando (Ejemplo: Jugar Energía)
1. **Cliente A** envía payload al canal `/app/match/{matchId}/action`. Payload JSON: `{ type: "ATTACH_ENERGY", energyId: "123", targetPokemonId: "456" }`.
2. **WebSocket Controller** recibe el payload y lo delega al `MatchService`.
3. El `MatchService` delega la acción al `RuleValidator` (Módulo 1).
4. El Game Engine valida, aplica el cambio al `GameState` interno.
5. El servidor envía una notificación a través de `SimpMessagingTemplate` hacia `/topic/match/{matchId}` con el nuevo estado del tablero público y el registro de la acción (`GameLog`).

### 3. Requisitos Críticos de la Consigna (Reconexión)
- "Manejo robusto de reconexiones: si un cliente se desconecta, debe poder reconectarse y recibir el estado actualizado de la partida".
- Se requiere un endpoint HTTP REST auxiliar (ej. `GET /api/matches/{id}/state`) que permita al frontend pedir el state global actual al cargar la pantalla, antes de subscribirse a las notificaciones websocket incrementales (o alternativamente que al suscribirse vía Websocket, el servidor responda enviando todo el state initial de una vez).

## Modelos DTO (Data Transfer Objects)
Definir claramente los `ActionRequestDTO` y `GameStateResponseDTO` para evitar acoplar los objetos de base de datos directamente al cliente.
