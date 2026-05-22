# 🔍 AUDIT COMPLETO — Game Engine vs. Consigna + Rulebook XY1

**Fecha:** 2026-05-21  
**Archivos revisados:** todo el paquete `engine/**`, `services/GameFacade`, `services/deck/DeckBuilderValidator`  
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

## Resumen ejecutivo

```
🔍 AUDIT COMPLETE — ✅ 15 puntos correctos | ⚠️ 6 warnings | 🚨 7 blockers
```

El engine tiene una base arquitectónica sólida (patrones State, Chain of Responsibility, Strategy, Observer todos presentes y correctos), pero hay **7 bugs bloqueantes** que hacen que la partida no pueda completarse. Lo más crítico: la evolución de Pokémon no funciona, nadie puede ganar por premios (el contador nunca se decrementa), y ninguna carta Entrenador tiene efecto real.

---

## 🚨 BLOCKERS — Deben resolverse antes de commitear

### B-01: `EvolveExecutor` no evoluciona el Pokémon — solo limpia status effects

**Archivo:** `engine/manager/EvolveExecutor.java`, `services/GameFacade.java` (~línea 129)  
**Regla:** RF-02, Rulebook §2

`EvolveExecutor.executeEvolve()` solo hace `statusEffectManager.clearAll()`. El `GameFacade.applyEvolve()` saca la carta de la mano y llama al executor, pero el `InPlayPokemon` en el campo **nunca es reemplazado** por la forma evolucionada. Después de evolucionar, el Pokémon sigue con el HP, los ataques y el tipo del pre-evolucionado. La evolución es una no-op completa en términos de datos de carta.

**Fix necesario:** `applyEvolve` debe reemplazar el `InPlayPokemon` activo (o el de banca correspondiente) con uno nuevo construido desde la `PokemonCard` de evolución, preservando los `damageCounters` y las `attachedEnergies`.

---

### B-02: `AttachEnergyAction` no tiene campo `target` — solo adjunta al Activo

**Archivo:** `engine/model/AttachEnergyAction.java`, `services/GameFacade.java` (~línea 123)  
**Regla:** RF-01b — "Unir 1 carta de Energía por turno a **cualquier Pokémon propio**"

```java
// AttachEnergyAction solo tiene:
public record AttachEnergyAction(PokemonType energyType) implements Action {}

// GameFacade siempre adjunta al Activo:
runtime.getActivePokemon().attachEnergy(action.energyType());
```

Es imposible adjuntar energía a un Pokémon de banca. Rulebook y consigna son explícitos: la energía va a cualquier Pokémon propio en juego.

**Fix necesario:** Agregar `BattlePokemonState target` al record `AttachEnergyAction` y actualizar `GameFacade`, `RuleValidator`, el DTO y el `toEngineAction()`.

---

### B-03: Efectos de cartas Entrenador (ITEM, SUPPORTER) no implementados

**Archivo:** `services/GameFacade.java` (~línea 185)  
**Regla:** RF-01b, RF-02

```java
default -> { /* ITEM and SUPPORTER effects are applied by RuleValidator/EffectResolver */ }
```

`RuleValidator` solo valida, no aplica efectos. No existe ningún `EffectResolver` para Trainers. Las cartas ITEM y SUPPORTER se sacan de la mano y **no pasa nada más**. Además, `TrainerCard` ni siquiera tiene un campo `effectText`, haciendo imposible implementar efectos sin refactorizar el modelo (ver W-03).

**Fix necesario:** Agregar `effectText` a `TrainerCard.Builder`, crear un `TrainerEffectResolver` análogo al `AttackEffectResolver`, e invocarlo desde `GameFacade.applyPlayTrainer()`.

---

### B-04: `UseAbilityAction` no tiene implementación — TODO en producción

**Archivo:** `services/GameFacade.java` (~línea 112)  
**Regla:** RF-01b — "Usar Habilidades de Pokémon propios"; RF-07 — "Visualizar y permitir el uso de Habilidades"

```java
case UseAbilityAction ignored -> { /* FR-TODO: ability effects not yet implemented */ }
```

Las Habilidades son parte del flujo obligatorio de MainPhase. El RF-07 también requiere visualizarlas desde el tablero.

---

### B-05 + B-06: El KO no descarta el Pokémon ni toma cartas de Premio → victoria imposible

**Archivos:** `engine/manager/KnockoutManager.java`, `engine/pipeline/KnockoutCheckStep.java`, `engine/manager/VictoryConditionChecker.java`  
**Regla:** RF-01d, RF-01f

El `KnockoutManager` detecta el KO y llama `handler.onKnockout(knocked, prizes)`. El `VictoryConditionChecker` recibe esto y revisa si ya quedan 0 premios. Pero **nadie ejecuta**:

- Remover el Pokémon KO'd del campo (`setActivePokemon(null)` o equivalente)
- Descartar energías y herramientas adjuntas a la `DiscardPile`
- **Decrementar el contador de premios** (`prizeProvider`)
- Mover cartas del prize pile a la mano del ganador

Como consecuencia, `prizeProvider.getRemainingPrizes(attacker)` **nunca llega a 0** (siempre son 6), haciendo que `VictoryResult.PrizeVictory` nunca se dispare. La victoria por premios es imposible en una partida real.

---

### B-07: Muerte Súbita (Sudden Death) — solo el tipo, sin lógica de reinicio

**Archivo:** `engine/model/VictoryResult.java`, `engine/manager/VictoryConditionChecker.java`  
**Regla:** RF-01f

`VictoryResult.SuddenDeath` existe como sealed record y puede dispararse si ambos jugadores simultáneamente quedan sin premios o sin banca. Pero **no hay código** que maneje `SuddenDeath` en el orquestador — nadie reinicia la partida en modo 1-carta-de-Premio.

---

## ⚠️ WARNINGS — Incompletos o incorrectos, a resolver antes de la entrega

### W-01: Pipeline de ataque — faltan los pasos 3 y 4 del Rulebook §3

**Archivos:** `engine/pipeline/`, `services/GameFacade.java`  
**Regla:** RF-01c §3

El pipeline actual: Validation → PreDamage → Calc → Apply → PostDamage → KnockoutCheck.  
El rulebook §3 en orden estricto:

1. ✅ Validar energía (ValidationStep)
2. ✅ Confusión (ValidationStep via onAttackAttempt)
3. ⚠️ **Prerequisitos del ataque** (selección de targets, monedas del texto de carta) — `PreDamageEffectsStep` solo maneja `coin_flip_extra`
4. ❌ **Efectos que cancelan el ataque** (de ataques anteriores del rival) — completamente ausente
5. ✅ Cálculo de daño
6. ✅ Aplicar daño
7. ✅ Efectos post-daño

---

### W-02: `GameFacade` usa `new Random()` directo — rompe el patrón CoinFlipper

**Archivo:** `services/GameFacade.java` (~líneas 50, 158)  
**Regla:** RNF-03 (tests determinísticos)

```java
private static final Random RANDOM = new Random();
// ...
RANDOM::nextBoolean   // ← no inyectable, no testeable
```

Todo el engine usa `CoinFlipper` para permitir tests determinísticos. El `GameFacade` rompe esta convención. Los tests de ataque no pueden controlar el resultado de las monedas.

---

### W-03: `TrainerCard` no tiene campo de efecto — modelo incompleto

**Archivo:** `engine/model/TrainerCard.java`  
**Regla:** RF-02

`TrainerCard` solo tiene `cardId`, `name`, `trainerType`, `aceSpec`. Sin un campo `effectText` (o similar), es imposible implementar los efectos de los Entrenadores. Este problema habilita el B-03.

---

### W-04: Energía Especial no diferenciada del modelo

**Regla:** RF-02 — "Energía Especial: máximo 4 copias; con efectos adicionales"

No hay distinción entre Energía Básica y Energía Especial en el modelo del engine. La validación de "máximo 4 copias" en `DeckBuilderValidator` excluye las energías básicas correctamente, pero la Energía Especial no tiene `effectText` ni se distingue durante el juego.

---

### W-05: Herramienta Pokémon solo rastrea si hay una herramienta, no cuál

**Archivo:** `engine/model/InPlayPokemon.java`, `services/GameFacade.java`  
**Regla:** RF-02

```java
private boolean toolAttached;  // solo true/false, no cardId ni efectos
```

Sin saber qué herramienta está equipada, sus efectos continuos (reducir daño, etc.) no pueden implementarse.

---

### W-06: Wiring de `logAction()` — coordinación con Coworker 1 (Módulo 4)

**Archivo:** `services/GameFacade.java` — falta la llamada  
**Regla:** RF-03, RF-05

El Coworker 1 implementa `JpaGameStatePersistence` con el método `logAction()`. Sin embargo, `GameFacade.apply()` **nunca llama** a este puerto. El log de acciones existe en la base de datos pero nadie escribe en él. Corresponde a nuestro equipo wiring las llamadas desde `GameFacade` hacia el puerto `GameStatePersistence`.

---

## ✅ Lo que está correcto

1. **Engine aislado de Spring** — `EngineSpringIsolationTest` lo valida. Cero imports `org.springframework.*` en `engine/**`. ✅
2. **Orden between-turns** — `BETWEEN_TURNS_ORDER`: ENVENENADO → QUEMADO → DORMIDO → PARALIZADO. ✅ Rulebook §5.
3. **Exclusión mutua Dormido/Confundido/Paralizado** — `isRotationSlot()` correcto en las tres. ✅
4. **Quemado y Envenenado coexisten** — `isRotationSlot() = false` para ambos. ✅
5. **Fórmula de daño en orden correcto** — base → attackerMods → Debilidad x2 → Resistencia -20 (mín. 0) → defenderMods. ✅ Rulebook §3.
6. **Primera jugada no puede atacar** — `FirstTurnAttackException` en `TurnManager.declareAttack()`. ✅ RF-01b.
7. **Robo del primer jugador omitido** — `DrawPhaseExecutor` salta si `isStartingPlayer && isFirstTurn`. ✅ RF-01b.
8. **Mulligan** — `SetupManager` implementa el bucle con bonus draws opcionales para el oponente. ✅ RF-01a.
9. **Retreat limpia condiciones especiales** — `RetreatExecutor` llama `clearAll()` después de quitar energías. ✅ Rulebook §2.
10. **Evolución limpia condiciones especiales** — `EvolveExecutor` llama `clearAll()`. ✅ (Pero falta el reemplazo del Pokémon — B-01).
11. **EX Pokémon dan 2 premios** — `KnockoutCheckStep` y `KnockoutManager` chequean `isEx()`. ✅ RF-01d.
12. **Validación de mazo en DeckBuilderValidator** — 60 cartas, max 4 copias, al menos 1 Básico, max 1 ACE SPEC. ✅ RF-04.
13. **Estado de la partida WAITING → SETUP → ACTIVE → FINISHED** — transiciones validadas en `MatchSession`. ✅ RF-03.
14. **Fog of War** — `PlayerPerspectiveMapper` existe y nunca expone la mano del rival. ✅ RNF-05.
15. **ReentrantLock por partida** — `MatchSession.getLock()` provee lock por gameId, no global. ✅ ADR-5.
