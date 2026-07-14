# Contrato de Integración del Engine

Este documento describe cómo el resto del backend (controllers WebSocket, persistencia) se integra con el Game Engine (`GameFacade`/`RuleValidator`), para quien toque ese límite sin conocer el motor en detalle.

## 1. Entrada al Motor: `GameFacade`

`GameFacade` es un `@Component` de Spring, inyectado por constructor donde se lo necesite (hoy: `MatchService`). No se instancia manualmente ni se obtiene desde `MatchSession` — `MatchSession` es un objeto de dominio puro (sin Spring), no expone el facade.

El flujo real, tal como lo usa `MatchService.executeAction(...)`:

```java
// 1. DTO -> Action del engine
final Action action = facade.toEngineAction(session, playerIndex, dto);

// 2. Validar (RuleValidator, no GameFacade, decide si la acción es legal)
final RuleValidator validator = resolveValidator(session);
final ValidationResult result = validator.validate(action, playerIndex);

if (result instanceof ValidationResult.Invalid invalid) {
    throw new InvalidActionException(invalid.reason());
}

// 3. Aplicar la acción ya validada (muta el estado en memoria de la sesión)
facade.apply(session, action, turnManager);

// 4. Leer el nuevo estado y hacer broadcast
final GameStateView view = ...; // ver PlayerPerspectiveMapper
webSocketService.broadcastBoard(matchId, view);
```

Puntos clave:
- `facade.apply(...)` es `void` — no devuelve `ValidationResult`. La validación es responsabilidad de `RuleValidator`, un paso previo y separado.
- Buscar una sesión activa es `matchSessionRegistry.find(matchId)` (devuelve `Optional<MatchSession>`), no `session.getGameFacade()`.
- `GameFacade.apply` tiene dos overloads: con y sin `TurnManager` — pasar el `TurnManager` cuando la acción puede afectar límites de fase (energía por turno, retiro, etc.).

## 2. Persistencia

El Engine Core **nunca** guarda en base de datos. La serialización de `MatchSession` a JSON (`MatchSessionJsonConverter`) y el guardado en `MatchEntity`/`MatchLogEntity` ocurren en la capa de persistencia (`AsyncPersistenceListener`), fuera del lock de partida, disparados por eventos (`SaveMatchEvent`) una vez que `apply(...)` ya devolvió. Si necesitás guardar algo nuevo derivado del estado de una partida, hacelo desde esa capa leyendo `MatchBoard`/`PlayerRuntime`, no desde dentro del engine.
