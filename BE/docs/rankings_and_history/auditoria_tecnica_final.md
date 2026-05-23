# Auditoría Técnica Final: Rankings, Historial, Persistencia y Replay

Este documento presenta la verificación técnica de extremo a extremo del estado del backend tras la refactorización atómica de la persistencia del ganador. Se analizan el flujo end-to-end, la serialización y la evaluación de deudas técnicas asociadas a otros componentes.

---

## 1. Persistencia End-to-End

El pipeline unificado garantiza que el ganador (`winner_id`) llegue a la base de datos a través de una sola transacción:

1. **Victoria Normal:** En `MatchCreationService.handleVictory`, el ganador se determina dinámicamente desde el `VictoryResult` (`PrizeVictory`, `BenchOutVictory`, `DeckOutVictory`) mediante su `winnerPlayerIndex`. Se asocia al `winnerId` de `MatchSession` y se llama a `saveMatch(session)`.
2. **Abandono:** En `MatchService.abandonMatch`, se calcula quién abandonó, se establece al rival como ganador en la sesión y se invoca a `saveMatch(session)`.
3. **Persistencia Async:** `saveMatch` publica un `SaveMatchEvent`. El listener asíncrono `onSaveMatch` lee el `winnerId`, resuelve/crea la entidad `UserEntity` mediante `getOrCreateUser` y la asigna a la propiedad `winner` de `MatchEntity` antes de hacer el único `matchRepository.save(entity)`.

### Rankings e Historial
* **Ranking:** La consulta `getGlobalRanking` funciona correctamente sumando los registros finalizados donde `winner IS NOT NULL`. Al persistirse adecuadamente el ganador tanto en abandonos como en victorias naturales, el ranking ahora refleja datos reales del juego de manera coherente.
* **Historial:** En `HistoryServiceImpl.mapToDto`, si la partida está en estado `FINISHED`:
  * Si el `winnerUsername` de la DB coincide con el del usuario autenticado $\rightarrow$ **`VICTORY`**.
  * Si el `winnerUsername` de la DB es diferente y no nulo $\rightarrow$ **`DEFEAT`**.
  * Si el `winnerUsername` de la DB es nulo $\rightarrow$ **`TIE`** (empate real).
* **Consistencia:** No quedan partidas finalizadas con ganador nulo a menos que el juego termine en un empate real.

---

## 2. Serialización JSON (`MatchSessionJsonConverter`)

* **Serialización del `winnerId`:** Confirmado. `MatchSessionSerializer` incluye la propiedad `winnerId` en el JSON generado para la columna `current_state` de la base de datos.
* **Deserialización:** `MatchSessionDeserializer` extrae `"winnerId"` y utiliza reflexión para inyectarlo en el campo privado de la sesión restaurada.
* **Compatibilidad con Sesiones Viejas:** Si se deserializa un JSON antiguo que carece del campo `"winnerId"`, el lector lo evalúa como `null` y la reflexión setea el campo de la sesión como `null`. No se producen fallos de deserialización ni incompatibilidades.
* **Riesgos de Reflexión:** La excepción de reflexión está envuelta en un bloque `catch` que lanza un `IOException` descriptivo. No hay riesgo de fallos silenciosos.

---

## 3. Código Muerto / Legacy

Al finalizar este refactor, las siguientes piezas de código han sido eliminadas o desactivadas:

1. **`MatchWinnerEvent.java`:** Eliminado del proyecto.
2. **`matchRepository.updateWinnerIfNull(...)`:** Removida de `MatchRepository.java`.
3. **`persistence.declareWinner(...)` (Deprecated):** Queda temporalmente como un **no-op** vacío exclusivamente para no romper stubs y aserciones de verificación Mockito en tests antiguos (como `MatchServiceAbandonTest.java`).

---

## 4. Auditoría de Replays (`ReplayServiceImpl`)

Se detectó una **deuda técnica de alta severidad** en la implementación legacy de replays (`ReplayServiceImpl`):

1. **Método `getReplay(matchId)`:** Usa `matchRepository.findById(matchId)` solo para verificar que la partida existe, lo que obliga a Hibernate a leer y deserializar el JSON de `currentState` de 100KB innecesariamente. Se recomienda migrar a `matchRepository.existsById(matchId)`.
2. **Método `getUserMatchHistory(username)`:** Ejecuta `matchRepository.findMatchesByUsername(username)` y carga una lista completa de entidades `MatchEntity`, deserializando el JSON de `currentState` de todos los matches históricos del usuario en memoria del servidor.
   * **Impacto:** Si un usuario tiene 50 partidas registradas, se deserializarán en memoria 50 JSONs gigantes (~5MB), a pesar de que el DTO de respuesta `UserMatchHistoryDTO` solo necesita campos primitivos.
   * **Recomendación:** Migrar a proyecciones JPQL directas (DTO projections) de la misma forma que el historial de la Fase 4 para evitar degradaciones y consumo excesivo de memoria en producción.
