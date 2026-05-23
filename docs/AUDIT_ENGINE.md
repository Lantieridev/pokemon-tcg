# 🔍 AUDIT COMPLETO — Game Engine vs. Consigna + Rulebook XY1

**Fecha última actualización:** 2026-05-22  
**Archivos revisados:** todo el paquete `engine/**`, `services/GameFacade`, `services/MatchService`, `services/MatchCreationService`, `services/deck/DeckBuilderValidator`  
**Referencias:** `docs/references/consigna.txt`, `docs/SKILLS/game-rules-reference.md`

> **Nota sobre división de trabajo:**  
> Los siguientes módulos están siendo implementados por otros integrantes del equipo y se excluyen de este audit:
> - **Módulo 4 (Coworker 1):** `JpaGameStatePersistence`, `MatchLogEntity`, guardado asíncrono de estado.
> - **Seguridad/JWT (Coworker 2):** `UserEntity`, Spring Security, registro y login.
> - **Ranking/Historial (Coworker 3):** `RankingService`, `HistoryService`, endpoints GET.
> - **Chat + Replay (Coworker 4):** `ChatWebSocketController`, `ReplayService`, `ReplayController`.
>
> Lo que **sí nos corresponde** en coordinación con esos módulos: **wiring de las llamadas** a `GameStatePersistence.logAction()` desde `GameFacade` (ver W-06 abajo).

---

## Historial de auditorías

| Fecha | Correctos | Warnings | Blockers |
|-------|-----------|----------|----------|
| 2026-05-21 | 15 | 6 | 7 |
| 2026-05-22 | 20 | 5 | 2 |

---

## Resumen ejecutivo

```
🔍 AUDIT COMPLETE — ✅ 20 puntos correctos | ⚠️ 5 warnings | 🚨 2 blockers
```

La arquitectura del engine es sólida. En la segunda sesión de auditoría + corrección se resolvieron 5 blockers (B-NEW-01 a B-NEW-04 + B-07) y 1 warning. Los 2 blockers restantes son funcionales (abilities pendientes + efectos de trainer parciales), no críticos para la demo de flujo completo.

---

## 🚨 BLOCKERS RESTANTES

### B-04: `UseAbilityAction` no tiene implementación — TODO en producción

**Archivo:** `services/GameFacade.java` (~línea 112)  
**Regla:** RF-01b — "Usar Habilidades de Pokémon propios"; RF-07 — "Visualizar y permitir el uso de Habilidades"

```java
case UseAbilityAction ignored -> { /* FR-TODO: ability effects not yet implemented */ }
```

Las Habilidades son parte del flujo obligatorio de MainPhase. El RF-07 también requiere visualizarlas desde el tablero. Requiere un `AbilityEffectResolver` similar al `AttackEffectResolver`.

---

### B-03 (parcial): Efectos de cartas Entrenador — solo RED_CARD y TEAM_FLARE_GRUNT implementados

**Archivo:** `services/GameFacade.java` (~línea 185)  
**Regla:** RF-01b, RF-02

El `TrainerEffectResolver` resuelve efectos por `TrainerEffectId`. Las cartas con `effectId = null` o con IDs no mapeados caen silenciosamente sin efecto. El set XY1 tiene 146 cartas con 30+ efectos únicos de Entrenador; la mayoría no está implementada.

---

## ⚠️ WARNINGS ACTIVOS

### W-01: Pipeline de ataque — falta el paso 4 del Rulebook §3 (cancelación de ataque)

**Archivos:** `engine/pipeline/`  
**Regla:** RF-01c §3

El pipeline actual: Validation → PreDamage → PokemonTool → Stadium → Calc → Apply → PostDamage → KnockoutCheck.  
El rulebook §3 requiere además: efectos que **cancelan el ataque** (ataques anteriores del rival) entre PreDamage y Calc. Completamente ausente.

---

### W-04: Energía Especial no diferenciada del modelo

**Regla:** RF-02 — "Energía Especial: máximo 4 copias; con efectos adicionales"

No hay distinción entre Energía Básica y Energía Especial en el modelo del engine. La validación de "máximo 4 copias" excluye las energías básicas correctamente, pero la Energía Especial no tiene `effectText` ni se distingue durante el juego.

---

### W-05: Herramienta Pokémon solo rastrea si hay una herramienta, no cuál

**Archivo:** `engine/model/InPlayPokemon.java`  
**Regla:** RF-02

```java
private boolean toolAttached;  // solo true/false, no cardId ni efectos
```

Sin saber qué herramienta está equipada, sus efectos continuos (reducir daño, etc.) no pueden implementarse correctamente (aunque `PokemonToolStep` en el pipeline sí lee la Tool via `getAttachedTool()`).

---

### W-06: Wiring de `logAction()` — coordinación con Coworker 1 (Módulo 4)

**Archivo:** `services/GameFacade.java` — falta la llamada  
**Regla:** RF-03, RF-05

`GameFacade.apply()` nunca llama al puerto `GameStatePersistence.logAction()`. El log de acciones existe pero nadie escribe en él. Corresponde a nuestro equipo wiring las llamadas desde `GameFacade`.

---

### W-NEW-02: `validatePlaceBasicPokemon()` no verifica que la carta sea de tipo BASIC

**Archivo:** `engine/manager/RuleValidator.java`  
**Regla:** RF-01b — "Colocar Pokémon Básicos en la Banca"

Si el cliente envía un `cardId` que corresponde a un Pokémon Stage-1/Stage-2 o incluso una carta Energía, el validator devuelve `Valid` y `applyPlacePokemon` hace `(PokemonCard) card` que puede pasar, pero `new InPlayPokemon(card)` recibiría una carta no-Básica. Requiere acceso al runtime en el validator para inspeccionar el tipo de la carta.

---

## ✅ Lo que está correcto (20 puntos)

1. **Engine aislado de Spring** — `EngineSpringIsolationTest` lo valida. Cero imports `org.springframework.*` en `engine/**`. ✅
2. **Orden between-turns** — `BETWEEN_TURNS_ORDER`: ENVENENADO → QUEMADO → DORMIDO → PARALIZADO. ✅ Rulebook §5.
3. **Exclusión mutua Dormido/Confundido/Paralizado** — `isRotationSlot()` correcto en las tres. ✅
4. **Quemado y Envenenado coexisten** — `isRotationSlot() = false` para ambos. ✅
5. **Fórmula de daño en orden correcto** — base → attackerMods → Debilidad x2 → Resistencia -20 (mín. 0) → defenderMods. ✅ Rulebook §3.
6. **Primera jugada no puede atacar** — `FirstTurnAttackException` en `TurnManager.declareAttack()`. ✅ RF-01b.
7. **Robo del primer jugador incluido** — `DrawPhaseExecutor` roba en turno 1; solo la restricción de ataque aplica. ✅ RF-01b (corregido B-NEW-03).
8. **DrawPhase auto-avanza a MainPhase** — `DrawPhaseExecutor` llama `turnManager.endDraw()` después de robar. ✅ (corregido B-NEW-03).
9. **Mulligan** — `SetupManager` implementa el bucle con bonus draws opcionales para el oponente. ✅ RF-01a.
10. **Retreat limpia condiciones especiales** — `RetreatExecutor` llama `clearAll()` después de quitar energías. ✅ Rulebook §2.
11. **Evolución limpia condiciones especiales** — `EvolveExecutor` llama `clearAll()`. ✅ Rulebook §2.
12. **EX Pokémon dan 2 premios** — `KnockoutCheckStep` y `KnockoutManager` chequean `isEx()`. ✅ RF-01d.
13. **Validación de mazo en DeckBuilderValidator** — 60 cartas, max 4 copias, al menos 1 Básico, max 1 ACE SPEC. ✅ RF-04.
14. **Estado de la partida WAITING → SETUP → ACTIVE → FINISHED** — transiciones validadas en `MatchSession`. ✅ RF-03.
15. **Fog of War** — `PlayerPerspectiveMapper` existe y nunca expone la mano del rival. ✅ RNF-05.
16. **ReentrantLock por partida** — `MatchSession.getLock()` provee lock por gameId, no global. ✅ ADR-5.
17. **MatchBoard lee desde runtimes vivos** — `getActivePokemon()`, `getBenchSize()`, `getBenchedPokemon()`, `getHandOf()`, `getActiveAttacks()` leen de `boundRuntimes`. ✅ (corregido B-NEW-02).
18. **turnsInPlay tracking** — `PlayerRuntime` mantiene mapa de turnos en juego; `TurnInPlayTracker` lo incrementa en cada `TurnEnded`. ✅ (corregido B-NEW-01).
19. **SuddenDeath reinicia la partida** — `MatchCreationService.handleVictory()` llama `resetForSuddenDeath()` en lugar de `finish()`. ✅ (corregido B-07).
20. **KO-replacement (PROMOTE_ACTIVE)** — `PromoteActiveAction` + gate en `processAction()` + `checkForPendingPromotion()` pausan la fase hasta que el defensor promueve. ✅ (corregido B-NEW-04).

---

## Blockers ya resueltos (en sesiones anteriores)

| ID | Descripción | Fix |
|----|-------------|-----|
| B-01 | EvolveExecutor no reemplazaba el Pokémon | `InPlayPokemon.evolveInto()` + `applyEvolve()` corregido |
| B-02 | AttachEnergyAction sin campo target | Agregado `target: BattlePokemonState` al record |
| B-05/B-06 | KO no descartaba ni tomaba premios | `KnockoutResolutionHandler` implementado completo |
| B-07 | SuddenDeath no reiniciaba la partida | `handleVictory()` llama `resetForSuddenDeath()` |
| B-NEW-01 | turnsInPlay nunca se incrementaba | `PlayerRuntime.turnsInPlay` map + `TurnInPlayTracker` |
| B-NEW-02 | MatchBoard leía snapshots estáticos | Métodos corregidos para leer de `boundRuntimes` |
| B-NEW-03 | DrawPhase nunca avanzaba a MainPhase | `DrawPhaseExecutor.executeDraw()` llama `endDraw()` |
| B-NEW-04 | No había acción de reemplazo tras KO | `PromoteActiveAction` + gate + `checkForPendingPromotion()` |
| W-NEW-01 | energyType nulo en AttachEnergy silencioso | Validado en `RuleValidator.validateAttachEnergy()` |
