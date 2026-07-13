# Pokémon TCG Engine - Arquitectura Core

Este documento sirve como **mapa mental** para entender cómo está diseñado el motor central (Domain) del juego. Está escrito para que cualquier IA o desarrollador que se sume al proyecto entienda los cimientos del sistema antes de tocar código.

## 1. Filosofía: Clean / Hexagonal Architecture

El motor (`ar.edu.utn.frc.tup.piii.engine`) es **100% puro**. 
- **NO** sabe qué es una Base de Datos.
- **NO** sabe qué es una API REST.
- **NO** sabe qué es un WebSocket.
- **NO** tiene dependencias a librerías externas (solo Java 21).

Todo esto se diseñó así para que las reglas de negocio (calcular daño, validar KOs, manejar turnos) se puedan testear unitariamente a una velocidad ridícula (milisegundos) sin tener que levantar contextos de Spring Boot ni contenedores de bases de datos.

## 2. El Flujo de Turnos (TurnManager)

El corazón del juego es el `TurnManager`. Funciona como una máquina de estados determinista. En Pokémon TCG un turno tiene un ciclo de vida muy estricto.

### Fases Principales
1. **Draw Phase:** Robo de carta inicial. Se dispara el `TurnStarted`.
2. **Main Phase:** El jugador juega cartas, asigna energías, evoluciona, etc. Es libre.
3. **Attack Phase:** Si el jugador decide atacar, el turno termina automáticamente.
4. **Between Turns Phase:** Fase crítica que ocurre *entre* el turno de un jugador y el del otro. Acá suceden los daños por Envenenamiento (Poison) y Quemadura (Burn).

## 3. Patrón Observer (Eventos y Listeners)

El `TurnManager` no resuelve todo solo. Delega responsabilidades mediante eventos. Cada vez que el TurnManager avanza de fase, emite un `PhaseEvent` a sus `PhaseListener`s registrados.

Los listeners más críticos son:
- **`KnockoutManager`:** Escucha el final de la fase de ataque y el final de la fase "entre turnos". Cuando escucha esto, barre el campo (Activo y Banca) de ambos jugadores revisando si algún Pokémon llegó a 0 HP.
- **`VictoryConditionChecker`:** Escucha a `KnockoutManager` (cuando muere alguien) y al `TurnManager` (cuando a alguien se le acaba el mazo en el Draw Phase). Dictamina si alguien ganó la partida (por llevarse 6 premios, por Bench-Out, o por Deck-Out).

## 4. Patrón Proveedor (Providers)

Dado que el Engine no tiene base de datos, necesita una forma de "preguntar" cómo está la mesa. Para esto usamos interfaces Proveedoras (Ports en Clean Architecture).
- **`BattlefieldStateProvider`:** ¿Quién es el Pokémon Activo del jugador X?
- **`BenchStateProvider`:** ¿Cuántos y cuáles Pokémon tiene el jugador X en su banca?
- **`DeckStateProvider`:** ¿Cuántas cartas le quedan en el mazo?
- **`PrizeStateProvider`:** ¿Cuántos premios le quedan por tomar?

El Engine **solo lee** a través de estas interfaces. En los tests usamos fakes/mocks, pero en producción, la capa de persistencia (Módulo 4) implementará estas interfaces y las conectará con la Base de Datos.

## 5. Validación de Reglas (`RuleValidator`)

Todas las acciones que el usuario quiere hacer desde el frontend llegan modeladas como un `Action` (record o sealed interface en Java 21).
Antes de ejecutarse, pasan por el `RuleValidator.validate(action)`.

Este validador es estricto y retorna un `ValidationResult`:
- Puede ser `Valid`.
- Puede ser `Invalid(reason)`.

Ejemplo de validación rigurosa: Para retirarse (`RetreatAction`), el validador verifica:
1. Que no esté Dormido o Paralizado (consultando al `StatusEffectManager`).
2. Que haya al menos un Pokémon en la Banca para reemplazarlo (consultando al `BenchStateProvider`).
3. Que no se haya retirado ya este mismo turno.
4. Que tenga las energías suficientes para pagar el costo de retirada.

## 6. Modelado de Daño (`DamageCalculator`)

Calcula el daño final de un ataque evaluando:
1. Daño base.
2. Debilidad (x2 si el tipo del ataque matchea con la debilidad del defensor).
3. Resistencia (-20 si matchea).
4. Efectos adicionales.

## 7. Capa de Aplicación (fuera del Engine puro)

El Engine descrito arriba es intencionalmente ciego a infraestructura. La capa que lo conecta con WebSocket/DB vive en `ar.edu.utn.frc.tup.piii.services` y `ar.edu.utn.frc.tup.piii.controllers`:

- **`GameFacade`:** traduce los DTOs de acción del frontend (`ActionRequestDTO`) a `Action`s del Engine (`toEngineAction`) y aplica cada tipo de acción sobre el `MatchSession` (`apply`). Es la única clase que conoce tanto los DTOs de transporte como el modelo del Engine.
- **`MatchService`:** orquesta una partida completa — mantiene el `MatchSessionRegistry` en memoria, delega en `GameFacade` para aplicar acciones, persiste el estado vía `GameStatePersistence`, calcula MMR (`MmrCalculationService`), aplica penalizaciones por abandono, decide jugadas del bot (`BotDecisionService`) y notifica a los clientes por STOMP (`SimpMessagingTemplate`).
- **`GameWebSocketController`:** único punto de entrada STOMP para acciones de partida (`/app/match/{matchId}/action`). Verifica que el `Principal` autenticado coincida con el `playerId` del payload y delega todo en `MatchService.processAction`.

## Resumen del Flujo de Ejecución Real

1. El usuario aprieta "Atacar" en la UI. El cliente Angular publica sobre STOMP a `/app/match/{matchId}/action`.
2. `GameWebSocketController.handleAction` recibe el mensaje, valida que el `Principal` autenticado coincida con el `playerId` del header, y delega en `MatchService.processAction`.
3. `MatchService` recupera la `MatchSession` en memoria desde `MatchSessionRegistry`.
4. `GameFacade.toEngineAction` traduce el DTO a un `DeclareAttackAction` del Engine; `GameFacade.apply` lo ejecuta contra el `RuleValidator` **(Engine)**.
5. Si el validador dice `Invalid`, `MatchService` propaga el error al tópico `/topic/match/{matchId}/player/{playerId}/errors` sin tocar el estado.
6. Si es `Valid`, el `AttackPipeline` **(Engine)** corre sus steps (`ValidationStep` → `DamageCalculationStep` → `DamageApplicationStep` → `KnockoutCheckStep` → ...) usando `DamageCalculator` para la matemática del daño.
7. El `KnockoutManager` **(Engine)** detecta HP en 0 y emite el evento de KO; `VictoryConditionChecker` **(Engine)** evalúa si la partida terminó.
8. `MatchService` persiste el nuevo estado vía `GameStatePersistence` y actualiza MMR/estadísticas si la partida terminó (`MmrCalculationService`, `MatchStatisticsTracker`).
9. `MatchService` proyecta el estado por jugador con `PlayerPerspectiveMapper` (cada jugador solo ve lo que le corresponde: no la mano rival, por ejemplo) y lo publica por WebSocket a ambos clientes.
