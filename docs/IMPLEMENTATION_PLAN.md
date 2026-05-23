# Lista de Implementación — Pokémon TCG TPI

**Fecha:** 2026-05-22  
**Prioridad:** SEGUIR REGLAS > BUENAS PRÁCTICAS > PERFORMANCE

Cada ítem cita el RF/RNF exacto y la sección del rulebook que lo justifica.  
El orden dentro de cada bloque es secuencial (los ítems superiores desbloquean a los siguientes).

---

## BLOQUE 1 — SEGUIR REGLAS (Módulo 1, correcciones críticas)

Estos ítems son **blockers de corrección**: el engine ya existe pero viola reglas activas del rulebook o la consigna.

---

### 1.1 — W-NEW-02: `validatePlaceBasicPokemon()` debe verificar que la carta sea de tipo BASIC

**Archivo:** `engine/manager/RuleValidator.java`  
**Regla:** RF-01b — "Colocar Pokémon Básicos en Banca"; RF-02 — "Evolución: no se puede evolucionar en el primer turno"  
**Rulebook §2:** "Colocar Pokémon Básicos en Banca"

**¿Por qué es regla antes que práctica?**  
Si un cliente envía un `cardId` de Stage-1, Stage-2 o Energía, el validator actual devuelve `Valid` y `applyPlacePokemon` crea un `InPlayPokemon` con una carta que no es Básica. Esto viola directamente el rulebook — colocar evoluciones en Banca sin Básico previo es movimiento ilegal. Es un bug de corrección de reglas, no de calidad de código.

**Implementación correcta:**
```java
private ValidationResult validatePlaceBasicPokemon(final PlaceBasicPokemonAction action,
                                                    final PlayerRuntime runtime) {
    final Card card = runtime.getHand().findById(action.cardId())
        .orElseThrow(() -> new CardNotFoundException(action.cardId()));
    if (!(card instanceof PokemonCard pokemon) || pokemon.getStage() != Stage.BASIC) {
        return new ValidationResult.Invalid("card_not_basic_pokemon");
    }
    if (runtime.getBench().isFull()) {
        return new ValidationResult.Invalid("bench_full");
    }
    return new ValidationResult.Valid();
}
```

**Por qué esta implementación y no otra:**  
- `instanceof` + pattern variable es el único lugar donde se inspecciona el tipo concreto de carta — es el único sitio donde el uso de `instanceof` está justificado por la jerarquía sellada de `Card`.  
- Lanzar `CardNotFoundException` en vez de devolver `Invalid` es intencional: si la carta no está en la mano del jugador es un error de protocolo del cliente, no una jugada inválida.

---

### 1.2 — W-01: Paso 4 del pipeline de ataque — efectos que cancelan el ataque

**Archivo:** `engine/pipeline/` — nuevo `AttackCancellationStep`  
**Regla:** RF-01c paso 5 — "Se aplican efectos que puedan modificar o cancelar el ataque"  
**Rulebook §3, paso 4**

**¿Por qué es regla antes que práctica?**  
El pipeline actual salta del paso 3 (PreDamage) al paso 5 (DamageCalculation). El paso 4 es parte obligatoria de la secuencia de resolución de ataque definida en la consigna. Sin él, efectos de cartas que cancelan ataques (presentes en el set XY1) nunca se aplican.

**Implementación correcta:**  
Agregar `AttackCancellationStep` en posición 4 de la cadena (después de `PreDamageStep`, antes de `DamageCalcStep`). El step consulta un `AttackCancellationRegistry` que mapea efectos activos (persistidos en `PlayerRuntime.activeEffects`) a predicados de cancelación. Si se cancela, lanza `AttackCanceledException` que el `AttackPhaseExecutor` captura para terminar el turno sin daño.

**Por qué esta implementación y no otra:**  
- Sigue el patrón Chain of Responsibility ya establecido — agregar un eslabón no rompe la cadena existente.  
- El registry vacío por defecto significa que sin efectos activos el comportamiento es idéntico al actual (ninguna regresión).  
- Separar el registro de efectos activos en `PlayerRuntime` (no en el step) permite persistirlos en Módulo 4 sin tocar el pipeline.

---

### 1.3 — B-04: `UseAbilityAction` — implementar `AbilityEffectResolver`

**Archivo:** `services/GameFacade.java`, nuevo `engine/resolver/AbilityEffectResolver.java`  
**Regla:** RF-01b — "Usar Habilidades de Pokémon propios"; RF-07 — "Visualizar y permitir el uso de Habilidades"  
**Rulebook §2:** Habilidades listadas como acción de la Fase Principal

**¿Por qué es regla antes que práctica?**  
El `GameFacade` tiene `/* FR-TODO: ability effects not yet implemented */` en producción. RF-07 requiere explícitamente que las habilidades sean visibles y usables desde el tablero. Una acción que el cliente puede enviar y que el servidor descarta en silencio es una violación funcional directa.

**Implementación correcta:**  
1. Crear `AbilityEffectResolver` con la misma firma que `TrainerEffectResolver`: recibe un `AbilityEffectId` + contexto de runtime, despacha al handler correcto.  
2. Las abilities XY1 más comunes a implementar primero: `ENERGY_ACCELERATION` (mover energía), `DRAW_ABILITY` (robar cartas), `HEALING_ABILITY` (curar daño).  
3. En `GameFacade.apply()`: `case UseAbilityAction a -> applyUseAbility(a, runtime)` delegando al resolver.
4. Agregar validación en `RuleValidator`: una ability solo puede usarse una vez por turno por Pokémon (flag `abilityUsedThisTurn` en `BattlePokemonState`).

**Por qué esta implementación y no otra:**  
- Mirror del `TrainerEffectResolver` — reutiliza el patrón Strategy ya establecido (RNF-04).  
- El flag por Pokémon es más correcto que un flag global, ya que en Banca con múltiples Pokémon cada uno puede tener abilities distintas.

---

### 1.4 — B-03 parcial: `TrainerEffectResolver` — ampliar efectos implementados

**Archivo:** `engine/resolver/TrainerEffectResolver.java`  
**Regla:** RF-01b — "Jugar cartas de Entrenador"; RF-02 — tipos de Entrenador con sus restricciones  
**Rulebook §2:** Objetos sin límite; 1 Partidario por turno; 1 Estadio por turno

**¿Por qué es regla antes que práctica?**  
Actualmente solo RED_CARD y TEAM_FLARE_GRUNT tienen implementación. El set XY1 tiene 30+ efectos únicos de Entrenador. Jugar una carta Entrenador que no tiene handler no genera error — cae silenciosamente, lo que viola el principio Fail Fast del engine y deja acciones sin efecto en partidas reales.

**Orden de implementación (por frecuencia en XY1 y complejidad creciente):**
1. **Objetos de robo** (`PROFESSOR_SYCAMORE`, `SHAUNA`): descarte/robo de cartas — sin dependencias de runtime complejas.
2. **Objetos de búsqueda** (`TIERNO`, `POKEBALL`): buscar carta específica del mazo — requiere acceso a `DeckStateProvider`.
3. **Objetos de recuperación** (`SUPER_POTION`, `POTION`): curar daño — ya existe el modelo de contadores.
4. **Estadios** (`POKEMON_CENTER_LADY` actúa como Objeto; estadios como `POKEMON_RESEARCH_LAB`): agregar zona de Estadio activo en `MatchSession` si no existe.
5. **Herramientas** (attach logic): el modelo `toolAttached` ya existe, solo falta el resolver de efectos continuos.

**Por qué este orden:**  
Los de robo/búsqueda son los más jugados estadísticamente en XY1 y desbloquean partidas funcionales completas. Los estadios requieren estado adicional (zona compartida) por lo que van después de los Objetos.

---

### 1.5 — W-04: Diferenciar Energía Especial de Energía Básica en el modelo

**Archivo:** `engine/model/EnergyCard.java`  
**Regla:** RF-02 — "Energía Especial: máximo 4 copias; con efectos adicionales según el texto de la carta"

**¿Por qué es regla antes que práctica?**  
La consigna diferencia explícitamente Básica y Especial. La validación de deck ya distingue correctamente el límite de copias, pero durante el juego no hay distinción: una Energía Especial se trata igual que una Básica. Esto hace imposible implementar sus efectos adicionales (parte obligatoria de RF-02).

**Implementación correcta:**  
Agregar `boolean isSpecial()` en `EnergyCard` (o un enum `EnergyKind { BASIC, SPECIAL }`). El `AttachEnergyAction` aplica el efecto del `EnergyEffectResolver` cuando `isSpecial() == true`. Las energías básicas no tienen resolver — son solo proveedoras de tipo.

---

### 1.6 — W-05: `toolAttached` — persistir qué herramienta está equipada

**Archivo:** `engine/model/InPlayPokemon.java` / `BattlePokemonState`  
**Regla:** RF-02 — "Herramienta Pokémon: máximo 1 por Pokémon; permanece hasta que el Pokémon sea descartado"  
**Rulebook §2:** efectos continuos de herramientas

**¿Por qué es regla antes que práctica?**  
`toolAttached: boolean` no permite implementar el efecto de ninguna herramienta. El `PokemonToolStep` del pipeline ya llama `getAttachedTool()`, lo que prueba que la arquitectura lo contempla pero el modelo no lo soporta todavía.

**Implementación correcta:**  
Cambiar `boolean toolAttached` → `Optional<TrainerCard> attachedTool`. El `PokemonToolStep` ya puede leer el `effectId` de la herramienta y delegar al `PokemonToolEffectResolver`. Al hacer KO y descartar el Pokémon, la herramienta se descarta junto con él (lógica ya en `KnockoutResolutionHandler` — solo necesita `attachedTool` para añadirlo a la pila de descarte).

---

## BLOQUE 2 — SEGUIR REGLAS (Módulos pendientes — funcionalidad nueva)

Estos ítems implementan módulos enteros que la consigna requiere y que aún no existen.

---

### 2.1 — Módulo 3: Deck Builder API

**Regla:** RF-04 completo — construcción, validación y persistencia de mazos  
**Tecnología obligatoria:** pokemontcg.io v2, solo en Deck Builder, nunca durante partida

**Secuencia de implementación:**

**2.1.1 — Seed de cartas XY1 (Flyway V2__seed_xy1_cards.sql)**  
Antes de cualquier endpoint, las 146 cartas del set xy1 deben existir en la BD. Consultar pokemontcg.io una sola vez (durante desarrollo), serializar a SQL, commitear como migration. El motor del juego lee de BD — nunca de la API en runtime (RF-03). Esta es la base de todo lo demás.

**2.1.2 — CardRepository + CardService**  
`GET /api/cards?set=xy1&name={query}` — búsqueda paginada contra la tabla de cartas cacheadas. Respuesta < 500ms (RNF-01). Índice en `(set_id, name)`.

**2.1.3 — DeckRepository + DeckService**  
CRUD completo de mazos por jugador: crear, listar, editar, eliminar. Persistencia en PostgreSQL. Un mazo guarda `[{cardId, quantity}]` — no duplicar rows por carta sino un jsonb con el conteo.

**2.1.4 — DeckBuilderValidator (ya existe) conectado a los endpoints**  
El validator (`DeckBuilderValidator`) ya valida las 4 reglas (60 cartas, max 4 copias, max 1 ACE SPEC, al menos 1 Básico). Solo necesita wiring al endpoint `POST /api/decks/validate`.

**2.1.5 — Tests de integración**  
El `DeckBuilderValidator` ya tiene tests unitarios. Agregar tests de integración para los endpoints REST con `@SpringBootTest` + testcontainer PostgreSQL.

---

### 2.2 — Módulo 4: Persistencia PostgreSQL

**Regla:** RF-05 — estado completo persistido después de cada acción; RF-03 — log inmutable; RNF-02 — Flyway obligatorio

**Secuencia de implementación:**

**2.2.1 — Flyway V1__schema.sql**  
Tablas: `matches`, `match_actions` (log inmutable, append-only), `match_state` (JSONB snapshot). Nunca `hbm2ddl.auto=update`. El schema debe permitir reconstruir la partida completa (RF-05).

**2.2.2 — `JpaGameStatePersistence` implementando `GameStatePersistence`**  
Reemplaza `NoOpGameStatePersistence`. Serializa el `MatchSession` completo a JSONB. Se llama dentro del lock en `GameFacade` (ADR-5). La interface ya existe — solo implementar.

**2.2.3 — Wiring de `logAction()` en `GameFacade`** (W-06)  
`GameFacade.apply()` ya conoce el turno, el jugador y la acción. Agregar llamada a `gameStatePersistence.logAction(matchId, turn, playerIndex, action, result)` al final de cada acción exitosa, dentro del lock. Es coordinación con Coworker 1 según el audit.

**2.2.4 — Reconexión desde snapshot**  
Al reconectar (`GET /api/matches/{matchId}/state`), si `MatchSession` no está en memoria, cargar desde el último snapshot JSONB. Requiere deserializador de `MatchSession` (inverse del serializer del 2.2.2).

**Por qué JSONB y no tablas relacionales:**  
El estado del tablero Pokémon tiene profundidad variable (banca 0-5 Pokémon, cada uno con 0-N energías, condiciones opcionales). Mapear esto a tablas relacionales requiere joins en caliente en cada acción — más lento y más frágil que un snapshot atómico. RF-05 solo requiere que el estado sea reconstruible, no que esté normalizado (RNF-01 justifica JSONB).

---

### 2.3 — Módulo 2 completar: WebSocket STOMP + reconexión + abandon timer

**Regla:** RF-06 — comunicación bidireccional, reconexión, notificaciones de eventos

**Lo que falta (la infraestructura base ya existe):**

**2.3.1 — Broadcast completo post-acción**  
Después de cada acción validada, enviar `PlayerPerspectiveMapper.toView(session, playerIndex)` a `/topic/match/{matchId}/player/{playerIndex}` para ambos jugadores. Actualmente el broadcast puede ser parcial.

**2.3.2 — Evento de notificación de KO**  
Cuando `KnockoutResolutionHandler` procesa un KO, emitir evento `KnockoutEvent` que `MatchService` convierte en mensaje STOMP a ambos clientes (RF-06: "notificar knockout").

**2.3.3 — Abandon timer (60 segundos)**  
`@Scheduled` o `ScheduledExecutorService` por sesión. Al desconectarse (`@EventListener(SessionDisconnectEvent.class)`), iniciar timer. Al reconectar, cancelarlo. Al expirar, llamar `handleVictory(abandonment)`. El timeout es configurable (`match.abandon.timeout-seconds` en `application.yml`).

**2.3.4 — Reconexión con estado actualizado**  
`GET /api/matches/{matchId}/state` con header `X-Player-Id` devuelve la vista filtrada por fog of war. Si la sesión no está en memoria, cargarla desde el snapshot (depende de 2.2.4).

---

## BLOQUE 3 — BUENAS PRÁCTICAS

Estos ítems no bloquean funcionalidad pero son exigidos por RNF-02, RNF-03, RNF-04 y pueden afectar la nota.

---

### 3.1 — Cobertura JaCoCo al 90% en `engine/**`

**Regla:** RNF-03 — "≥ 90% líneas y ≥ 85% branches en paquetes `engine/**`"

Con los ítems 1.1–1.6 implementados, los nuevos paths de código deben tener tests antes de escribirse (TDD estricto per CLAUDE.md). Puntos concretos que necesitan tests nuevos:
- `validatePlaceBasicPokemon()` con carta Stage-1 y con carta de Energía.
- `AttackCancellationStep` con efecto activo y sin efecto activo.
- `AbilityEffectResolver` por cada tipo de ability implementada.
- `TrainerEffectResolver` por cada nuevo efecto añadido en 1.4.
- `InPlayPokemon.attachedTool` — attach, discard on KO.

**Por qué TDD y no tests al final:**  
El threshold JaCoCo bloquea `mvn verify`. Si se escribe código sin tests, el build falla en CI. Además, escribir el test primero fuerza a definir la API del objeto antes de su implementación, lo que alinea con RNF-04 (diseño por interfaces).

---

### 3.2 — Completar Javadoc en clases públicas del engine

**Regla:** RNF-02 — "legibilidad, mantenibilidad y escalabilidad"

Las clases nuevas de 1.1–1.6 deben documentar: contratos de sus constructores (`@param`, `@throws`), invariantes de clase y referencias a la sección del rulebook (`// XY1 §N`). Patrón ya establecido en `DrawPhaseExecutor`, `DamageCalculator`, etc.

---

### 3.3 — Checkstyle y PMD sin violations en código nuevo

**Regla:** RNF-02 — calidad de código; configuración en `BE/.code_quality/`

Antes de cada commit: `mvn verify` limpio. Los violations más frecuentes a vigilar con el código nuevo:
- `final` en todos los parámetros y variables locales (ya es convención en el proyecto).
- Sin `if/else` para comportamiento polimórfico (usar Strategy/switch exhaustivo).
- Nombres de tests descriptivos: `shouldRejectStage1PokemonInPlaceBasicAction()`.

---

### 3.4 — `MatchSession` thread-safety audit

**Regla:** RNF-02; ADR-5 (lock protocol)

Con la adición de `awaitingPromotion` + `promotingPlayerIndex` en sesiones anteriores, verificar que todos los campos mutables de `MatchSession` solo se acceden dentro del `ReentrantLock` de la sesión. Un campo mutable leído fuera del lock es una race condition potencial en partidas con dos conexiones simultáneas.

**Check concreto:** grep de `session.isAwaitingPromotion()` y `session.getPromotingPlayerIndex()` — deben aparecer únicamente dentro del bloque `lock.lock()` / `lock.unlock()` en `MatchService`.

---

## BLOQUE 4 — MÓDULO 5: FRONTEND ANGULAR

**Regla:** RF-07 completo — tablero interactivo, drag & drop, panel de acciones, fog of war

El frontend es el módulo más visible para la evaluación. Se implementa completamente después de que el backend WebSocket esté estable (Bloque 2).

---

### 4.1 — Arquitectura base y WebSocket STOMP client

**Regla:** RF-06, RF-07; RNF-02 — "mejores prácticas Angular 21+"

**Estructura de directorios:**
```
FE/src/app/
  core/
    services/
      game-state.service.ts   ← Signals + STOMP client
      deck.service.ts
      auth.service.ts
  features/
    lobby/                    ← Smart component
    board/                    ← Smart component
      pokemon-zone/           ← Dumb
      hand/                   ← Dumb
      action-panel/           ← Dumb
    deck-builder/             ← Smart component
```

`GameStateService` mantiene el estado como `signal<GameView>`. El STOMP subscription escribe en el signal. Nada de RxJS para estado local (CLAUDE.md).

---

### 4.2 — Tablero con fog of war

**Regla:** RF-07 — "cantidad de cartas en la mano del oponente (sin revelar el contenido)"; RNF-05

El backend ya filtra via `PlayerPerspectiveMapper`. El frontend debe:
- Mostrar `opponentHandSize` como N cartas boca abajo — nunca los IDs de cartas rivales.
- `NgOptimizedImage` para imágenes de cartas (obligatorio per CLAUDE.md).
- Zona de oponente con Activo, Banca (boca arriba después del setup), mazo (número), Premio (número), descarte.

---

### 4.3 — Drag & drop

**Regla:** RF-07 — "Sistema de drag & drop para: colocar Pokémon Básicos en Banca, unir Energías, equipar Herramientas, jugar Entrenadores"

Usar Angular CDK DragDrop (`@angular/cdk/drag-drop`). Cada zona del tablero es un `cdkDropList`. El drop emite la acción correspondiente al `GameStateService`, que la envía via STOMP. La validación la hace el backend — el frontend no decide si la acción es legal, solo la envía.

---

### 4.4 — Panel de acciones con botones habilitados/deshabilitados por fase

**Regla:** RF-07 — "botones habilitados o deshabilitados según la fase del turno y las acciones ya realizadas"

`computed(() => ...)` sobre el `signal<GameView>` para derivar qué acciones están disponibles:
- Atacar: solo en `ATTACK` phase, no en turno 1.
- Retirar: solo en `MAIN` phase, no si ya se retiró este turno, no si Dormido/Paralizado.
- Evolucionar: solo en `MAIN` phase, no en turno 1, no si el Pokémon entró este turno.
- Unir Energía: solo en `MAIN` phase, no si ya se unió este turno.

Estos `computed()` no duplican lógica de negocio — solo leen los flags del `GameView` que el backend ya calcula y serializa en el estado.

---

### 4.5 — Notificaciones visuales y log de acciones

**Regla:** RF-07 — "Notificaciones visuales para eventos relevantes"; "Log de acciones visible"

Toast/overlay para: inicio de turno, KO, toma de Premio, condición especial aplicada, fin de partida.  
Log de acciones: lista scrolleable, actualizada en tiempo real desde el estado del servidor. Solo display — sin lógica.

---

## BLOQUE 5 — PERFORMANCE

Estos ítems solo se abordan después de que la funcionalidad esté completa. Optimizar antes es prematuro.

---

### 5.1 — Índices PostgreSQL para queries frecuentes

**Regla:** RNF-01 — "búsqueda de cartas < 500ms"

Índices: `(set_id, name)` en tabla de cartas, `(match_id, created_at DESC)` en `match_actions`. Medir con `EXPLAIN ANALYZE` antes de indexar — no agregar índices sin evidencia de lentitud.

---

### 5.2 — Cache de cartas en memoria con Caffeine

**Regla:** RNF-01 — "tiempo de respuesta < 200ms en acciones de juego"; RF-03 — "datos de cartas del caché local"

El motor del juego ya carga cartas desde BD durante el setup de la partida. Con 146 cartas XY1, el volumen es pequeño — un `@Cacheable` con Caffeine sobre `CardRepository.findBySetId("xy1")` evita queries repetidas en partidas concurrentes. Implementar solo si el profiler muestra que las queries de cartas son el cuello de botella.

---

### 5.3 — `PlayerPerspectiveMapper` — evitar copias innecesarias

**Regla:** RNF-01 — "tiempo de respuesta < 200ms"

El mapper construye una `GameView` completa en cada broadcast. Con el `MatchBoard` ya leyendo de `boundRuntimes` (corrección B-NEW-02), verificar que no haya copias defensivas innecesarias de listas grandes (ej: mano de 7+ cartas). Si el profiler lo muestra, cambiar a views inmutables `List.copyOf()` solo donde sea necesario por contrato de API.

---

## Resumen de orden de ejecución

```
[HOY]      1.1 W-NEW-02 validatePlaceBasicPokemon tipo BASIC
           1.2 W-01 AttackCancellationStep (pipeline paso 4)
           1.3 B-04 AbilityEffectResolver
           1.4 B-03 TrainerEffectResolver ampliar
           1.5 W-04 EnergyCard isSpecial()
           1.6 W-05 attachedTool Optional<TrainerCard>

[MÓDULO 3] 2.1.1 Flyway seed XY1
           2.1.2 CardRepository + endpoint búsqueda
           2.1.3 DeckRepository CRUD
           2.1.4 DeckBuilderValidator wiring endpoints
           2.1.5 Tests integración Deck Builder

[MÓDULO 4] 2.2.1 Flyway schema (matches, log, snapshot)
           2.2.2 JpaGameStatePersistence
           2.2.3 Wiring logAction() en GameFacade
           2.2.4 Reconexión desde snapshot

[MÓDULO 2] 2.3.1 Broadcast completo post-acción
           2.3.2 Evento KO via STOMP
           2.3.3 Abandon timer 60s
           2.3.4 Reconexión con estado actualizado

[PRÁCTICAS] 3.1 JaCoCo ≥90% en engine/**
            3.2 Javadoc clases nuevas
            3.3 Checkstyle/PMD limpio
            3.4 MatchSession thread-safety audit

[MÓDULO 5] 4.1 → 4.5 Frontend (después de backend estable)

[PERF]     5.1 → 5.3 (solo si hay evidencia de lentitud)
```
