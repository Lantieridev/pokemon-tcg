# 🗺️ Plan de Acción — Engine Game Logic

**Fecha:** 2026-05-21  
**Basado en:** `docs/AUDIT_ENGINE.md`  
**Responsable:** nuestro equipo (excluye módulos de Coworkers 1-4)

---

## Cómo leer este documento

Las tareas están ordenadas por **dependencias y criticidad**. Una tarea no puede empezarse hasta que las marcadas como prerequisito estén completas. Cada tarea incluye los archivos exactos a tocar y el criterio de "done".

---

## TAREA 1 — Sistema de Premios + Proceso de KO completo
> **Prerequisito de TODO lo demás.** Sin esto la victoria por premios es imposible.  
> **Blockers resueltos:** B-05, B-06  
> **Estimado:** Alto esfuerzo

### Qué hacer

**1.1 — Crear el contrato de `PrizeManager`** (nuevo archivo en `engine/manager/`)

Responsabilidades:
- Decrementar el contador de premios del jugador ganador.
- Mover las cartas de Premio a la mano de ese jugador.
- Devolver cuántos premios quedan.

```java
// engine/manager/PrizeManager.java
public interface PrizeManager {
    int takePrizes(int playerIndex, int count); // retorna premios restantes
}
```

Implementarlo en `PlayerRuntime` o como colaborador separado que conoce el prize pile del jugador.

**1.2 — Conectar el KO al descarte y al PrizeManager**

En `KnockoutManager` (o en el coordinador que llama al `KnockoutHandler`), después del `handler.onKnockout(knocked, prizes)`:

- Remover el Pokémon del campo activo: `playerRuntime.setActivePokemon(null)`.
- Mover sus `attachedEnergies` a la `DiscardPile` del dueño.
- Si tenía `toolAttached`, descartar la herramienta también.
- Llamar a `prizeManager.takePrizes(attackerIndex, prizesToTake)`.

**1.3 — Actualizar `VictoryConditionChecker`**

El chequeo `prizeProvider.getRemainingPrizes(attacker) == 0` solo funcionará cuando el PrizeManager realmente decremente el contador. Verificar que `PrizeStateProvider` lea del mismo lugar donde escribe el PrizeManager.

**1.4 — Prompt al dueño del KO para elegir reemplazo**

Después del KO y antes de que `VictoryConditionChecker` chequee bench-out, el dueño debe poder elegir qué Pokémon de banca pasa al activo. Esta decisión tiene que orquestrarse en `MatchService` / `GameWebSocketController`:

- Enviar evento `POKEMON_KO` al cliente del dueño.
- Esperar acción `PROMOTE_FROM_BENCH` antes de continuar.
- Si no hay banca → bench-out victory.

### Criterio de done
- [ ] Un test de integración donde un Pokémon muere, el oponente toma 1 premio, el dueño elige reemplazo y la partida continúa.
- [ ] Un test donde al tomar el último premio se dispara `VictoryResult.PrizeVictory`.
- [ ] Un test donde el KO deja banca vacía y se dispara `VictoryResult.BenchOutVictory`.

---

## TAREA 2 — `AttachEnergy` a cualquier Pokémon (Activo o Banca)
> **Prerequisito:** ninguno (independiente)  
> **Blocker resuelto:** B-02  
> **Estimado:** Bajo esfuerzo

### Qué hacer

**2.1 — Agregar `target` a `AttachEnergyAction`**

```java
// engine/model/AttachEnergyAction.java
public record AttachEnergyAction(PokemonType energyType, BattlePokemonState target) implements Action {}
```

**2.2 — Actualizar `GameFacade.applyAttachEnergy()`**

```java
private void applyAttachEnergy(final AttachEnergyAction action, final PlayerRuntime runtime) {
    final EnergyCard energyCard = findEnergyInHand(runtime, action.energyType());
    runtime.getHand().removeCard(energyCard.getCardId());
    action.target().attachEnergy(action.energyType()); // ← usar el target, no getActivePokemon()
}
```

**2.3 — Actualizar `GameFacade.toEngineAction()`**

En el case `ATTACH_ENERGY`, resolver el target a partir del `dto.targetIndex()`:
- Si `targetIndex == null` o `-1` → adjuntar al Activo.
- Si `targetIndex >= 0` → adjuntar al Pokémon de banca en esa posición.

**2.4 — Actualizar `ActionRequestDTO`**

Verificar que `targetIndex` exista en el DTO (ya parece estar, validar).

### Criterio de done
- [ ] Test: adjuntar energía al Pokémon activo → pasa.
- [ ] Test: adjuntar energía al Pokémon de banca en índice 0 → pasa.
- [ ] Test: intentar adjuntar una segunda energía en el mismo turno → `ENERGY_ALREADY_ATTACHED`.

---

## TAREA 3 — Evolución real del Pokémon en el campo
> **Prerequisito:** ninguno (independiente)  
> **Blocker resuelto:** B-01  
> **Estimado:** Medio esfuerzo

### Qué hacer

**3.1 — Refactorizar `InPlayPokemon` para soportar swap de carta**

`InPlayPokemon` tiene `private final PokemonCard card`. Cambiar a no-final y agregar:

```java
public void evolve(final PokemonCard newCard) {
    this.card = Objects.requireNonNull(newCard, "newCard must not be null");
    // damageCounters y attachedEnergies se preservan automáticamente
}
```

**3.2 — Actualizar `EvolveAction`**

Agregar la `PokemonCard` de evolución al record para que el executor la tenga:

```java
public record EvolveAction(BattlePokemonState target, PokemonCard evolution) implements Action {}
```

(Nota: ya existe un campo `evolution` de tipo `PokemonCard`, verificar si ya es `PokemonCard` o `BattlePokemonState`.)

**3.3 — Actualizar `EvolveExecutor.executeEvolve()`**

```java
public void executeEvolve(final BattlePokemonState target, final PokemonCard evolution) {
    if (target instanceof InPlayPokemon inPlay && evolution != null) {
        inPlay.evolve(evolution);
    }
    statusEffectManager.clearAll();
}
```

**3.4 — Actualizar `GameFacade.applyEvolve()`**

```java
private void applyEvolve(final EvolveAction action, final PlayerRuntime runtime) {
    if (action.evolution() != null) {
        runtime.getHand().removeCard(action.evolution().getCardId());
        new EvolveExecutor(runtime.getStatusEffectManager())
            .executeEvolve(action.target(), action.evolution());
    }
}
```

**3.5 — Actualizar `GameFacade.toEngineAction()` para el case `EVOLVE`**

Resolver la `PokemonCard` de evolución a partir del `dto.cardId()` buscando en la mano del jugador.

### Criterio de done
- [ ] Test: Charmander evoluciona a Charmeleon → el InPlayPokemon ahora tiene el HP y ataques de Charmeleon.
- [ ] Test: Los damage counters y energías adjuntas se preservan después de evolucionar.
- [ ] Test: Las condiciones especiales se limpian después de evolucionar.

---

## TAREA 4 — Orquestación del `processBetweenTurns`
> **Prerequisito:** TAREA 1 (necesita el flujo completo de KO para testear)  
> **Warning resuelto:** W-06  
> **Estimado:** Bajo-Medio esfuerzo

### Qué hacer

En `MatchService` (o el coordinador de turno), el flujo del between-turns debe ser explícito y en el orden correcto:

```java
// Pseudocódigo del coordinador en MatchService al procesar END_TURN:

// 1. Procesar status effects de ambos jugadores activos
for (PlayerRuntime pr : session.getPlayerRuntimes()) {
    statusEffectManager(pr).processBetweenTurns(pr.getActivePokemon());
}

// 2. Verificar KOs provocados por los status effects (veneno/quemadura mataron)
// → KnockoutManager escucha PhaseExited(BetweenTurns), se dispara en paso 3

// 3. Transicionar la fase
turnManager.endBetweenTurns();
```

El contrato del `KnockoutManager` dice que sus listeners sobre `PhaseExited(BetweenTurnsPhase)` **se ejecutan** cuando `endBetweenTurns()` es llamado. Esto está bien — solo falta que `processBetweenTurns` se llame *antes* de ese punto.

Agregar un test de integración que valide este orden:
- Pokémon envenenado → entre turnos → recibe 1 counter → si llega a HP máx → KO detectado.

### Criterio de done
- [ ] Test: Pokémon con 10 HP restantes y Envenenado → después del between-turns, el KO se dispara.
- [ ] Test: Pokémon Dormido → entre turnos saca cara → se despierta.
- [ ] Test: Pokémon Paralizado → no puede atacar en su turno → se cura entre turnos.

---

## TAREA 5 — Inyectar `CoinFlipper` en `GameFacade`
> **Prerequisito:** ninguno (independiente, bajo riesgo)  
> **Warning resuelto:** W-02  
> **Estimado:** Bajo esfuerzo

### Qué hacer

**5.1 — Eliminar `private static final Random RANDOM`** de `GameFacade`.

**5.2 — Agregar `CoinFlipper` como dependencia inyectada:**

```java
@Component
public final class GameFacade {

    private final AttackPipeline attackPipeline;
    private final CoinFlipper coinFlipper;

    public GameFacade(final CoinFlipper coinFlipper) {
        this.coinFlipper = Objects.requireNonNull(coinFlipper);
        this.attackPipeline = new AttackPipeline(List.of(
            new ValidationStep(),
            new PreDamageEffectsStep(),
            new DamageCalculationStep(new DamageCalculator()),
            new DamageApplicationStep(),
            new PostDamageEffectsStep(new AttackEffectResolver()),
            new KnockoutCheckStep()
        ));
    }
}
```

**5.3 — En `MatchEngineConfig`**, registrar `RandomCoinFlipper` como bean.

**5.4 — Pasar `this.coinFlipper` al `AttackContext.Builder`** en `applyDeclareAttack()`.

### Criterio de done
- [ ] `GameFacade` no tiene ninguna referencia directa a `java.util.Random`.
- [ ] Los tests de ataque pueden usar un `CoinFlipper` determinístico (siempre cara / siempre cruz).

---

## TAREA 6 — Wiring del log de acciones (coordinación con Coworker 1)
> **Prerequisito:** esperar a que Coworker 1 termine la firma de `GameStatePersistence.logAction()`  
> **Warning resuelto:** W-06  
> **Estimado:** Bajo esfuerzo (solo wiring)

### Qué hacer

Una vez que el Coworker 1 tenga definida la firma del método `logAction()` en `GameStatePersistence`, agregar la llamada al final de cada `case` exitoso en `GameFacade.apply()`:

```java
// Al final de cada acción aplicada exitosamente:
gameStatePersistence.logAction(session.getMatchId(), playerIndex, action, result);
```

El `GameStatePersistence` ya está diseñado para ser asíncrono (via `@Async` del Coworker 1), así que la llamada no bloqueará el hilo del juego.

**Acordar con Coworker 1** el tipo del parámetro `result` (String descriptivo vs. objeto).

### Criterio de done
- [ ] Cada acción (ATTACK, EVOLVE, RETREAT, etc.) genera una entrada en `match_logs`.
- [ ] El test de `GameFacadeApplyTest` mockea `GameStatePersistence` y verifica que `logAction()` fue llamado.

---

## TAREA 7 — Modelo de `TrainerCard` con campo de efecto
> **Prerequisito:** ninguno (es refactor de modelo)  
> **Warning resuelto:** W-03  
> **Habilitador de:** TAREA 8  
> **Estimado:** Bajo esfuerzo

### Qué hacer

**7.1 — Agregar `effectText` al builder de `TrainerCard`:**

```java
public static final class Builder {
    // ... campos existentes ...
    private String effectText = "";

    public Builder effectText(final String text) {
        this.effectText = text;
        return this;
    }
}
```

**7.2 — Agregar getter `getEffectText()` a `TrainerCard`.**

**7.3 — Actualizar `CardMapper`** para mapear el campo `description` (o equivalente) de la API/persistencia al `effectText`.

### Criterio de done
- [ ] `TrainerCard.Builder` acepta `effectText`.
- [ ] `CardMapper` llena `effectText` desde la fuente de datos.
- [ ] Tests de `CardMapper` actualizados.

---

## TAREA 8 — Efectos básicos de Trainer (ITEM, SUPPORTER)
> **Prerequisito:** TAREA 7  
> **Blocker resuelto:** B-03  
> **Estimado:** Alto esfuerzo (requiere implementar efectos carta por carta)

### Qué hacer

**8.1 — Crear `TrainerEffectResolver`** análogo al `AttackEffectResolver`:

Mapear keywords del `effectText` a handlers. Ejemplos del set XY1:
- `"draw:N"` → robar N cartas del mazo.
- `"search_basic"` → buscar un Pokémon Básico del mazo y ponerlo en mano.
- `"heal:N"` → curar N daño del Pokémon Activo.
- `"discard_energy_from_opponent"` → descartar energía del rival.

**8.2 — Integrar en `GameFacade.applyPlayTrainer()`:**

```java
case ITEM, SUPPORTER -> trainerEffectResolver.apply(action, session, runtime);
```

**8.3 — Priorizar los Trainers más comunes del set XY1** (Potion, Professor's Letter, Cheren, Shauna, etc.) antes de los más complejos.

### Criterio de done
- [ ] Al menos 5 cartas Trainer del set XY1 tienen efecto implementado y testeado.
- [ ] `GameFacade` no tiene más el comentario `/* ITEM and SUPPORTER effects are... */`.

---

## TAREA 9 — Muerte Súbita (Sudden Death) básico
> **Prerequisito:** TAREA 1 (requiere el sistema de premios funcionando)  
> **Blocker resuelto:** B-07  
> **Estimado:** Medio esfuerzo

### Qué hacer

En el handler de `VictoryResult.SuddenDeath` (en `MatchService`):

1. Marcar la partida como `SUDDEN_DEATH` (nuevo sub-estado, o re-usar `ACTIVE`).
2. Re-ejecutar el `SetupManager` con decks barajeados y **1 carta de Premio** en lugar de 6.
3. Resetear `TurnManager` y `StatusEffectManager` de ambos jugadores.
4. Notificar a ambos clientes del inicio de Muerte Súbita via WebSocket.
5. Repetir hasta que haya un ganador no-simultáneo.

### Criterio de done
- [ ] Test: `SuddenDeathTest` existente valida que si se dispara `SuddenDeath`, la partida continúa con 1 carta de Premio.
- [ ] (El test `SuddenDeathTest` ya existe en el repo — revisar si está completo o solo es un stub.)

---

## Orden de ejecución recomendado

```
TAREA 2 (AttachEnergy)       ← empezar ya, bajo riesgo, independiente
TAREA 3 (Evolve)             ← empezar ya, independiente  
TAREA 5 (CoinFlipper)        ← empezar ya, bajo riesgo

TAREA 1 (KO + Premios)       ← crítica, en paralelo con 2/3/5
TAREA 4 (BetweenTurns)       ← después de TAREA 1

TAREA 7 (TrainerCard model)  ← después de que 1/2/3 estén listos
TAREA 8 (Trainer effects)    ← después de TAREA 7
TAREA 6 (log wiring)         ← cuando Coworker 1 defina la firma

TAREA 9 (Sudden Death)       ← al final, después de TAREA 1
```

---

## Interfaz con coworkers — qué necesitamos de ellos

| Coworker | Qué nos bloquea | Cuándo |
|----------|----------------|--------|
| Coworker 1 (Módulo 4) | Firma definitiva de `GameStatePersistence.logAction()` | Antes de TAREA 6 |
| Coworker 4 (Chat/Replay) | Ninguno — ellos dependen de nuestro log, no al revés | — |
| Coworker 2 (JWT) | El `playerId` validado debe llegar al `GameWebSocketController` | Antes de integrar acciones |
