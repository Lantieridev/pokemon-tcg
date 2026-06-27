# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

TPI universitario (UTN-FRC, Programación III): versión digital funcional del Pokémon TCG basada en el reglamento XY1.
Dos jugadores compiten en tiempo real con mazos de 60 cartas. El backend es la ÚNICA fuente de verdad del estado del juego.

**Stack obligatorio:**
- **Backend:** Java 21 + Spring Boot 4.0.0 + Maven — raíz del paquete `ar.edu.utn.frc.tup.piii`, código en `BE/`
- **Frontend:** Angular 20+ (Standalone Components, Signals, `@if`/`@for`) — código en `FE/`
- **Base de datos:** PostgreSQL (Flyway — NUNCA `hbm2ddl.auto=update`) — pendiente Módulo 4
- **Tiempo real:** WebSockets STOMP (`@EnableWebSocketMessageBroker`)
- **API externa:** pokemontcg.io v2 — solo Deck Builder, nunca durante una partida
- **Testing:** JUnit 5 + Mockito + JaCoCo

## Documentación de Referencia (LEER ANTES DE CODEAR)

| Archivo | Uso |
|---------|-----|
| `docs/references/consigna.txt` | RFs y RNFs completos — fuente de verdad de los requisitos |
| `docs/SKILLS/game-rules-reference.md` | Reglas algorítmicas XY1 — fuente de verdad para el engine |
| `docs/ARCHITECTURE.md` | Mapa mental de la arquitectura del Game Engine |
| `docs/references/pokemontcg-api-reference.md` | Estructura real de la API y gotchas |
| `docs/SDD/` | Documentos de diseño por módulo |

## Comandos de Build y Test

Todos los comandos desde el directorio `BE/`:

```bash
mvn compile                                        # Compilar (verificar sintaxis rápido)
mvn test                                           # Tests + reporte JaCoCo
mvn verify                                         # Build completo con Checkstyle + PMD + JaCoCo
mvn test -Dtest=RuleValidatorTest                  # Clase de test individual
mvn test -Dtest=RuleValidatorTest#shouldThrow...   # Método de test individual
mvn spring-boot:run                                # Levantar la aplicación
```

Frontend desde `FE/`:
```bash
npm install && npm start   # ng serve
npm test                   # Karma
npm run build
```

## Thresholds JaCoCo (bloquean `mvn verify`)

- Bundle global: ≥ 80% instrucciones
- Paquetes `engine/**`: ≥ 90% líneas, ≥ 85% branches
- `RuleValidator`, `DamageCalculator`, `StatusEffectManager`: ≥ 90% instrucciones cada uno

## Estado de los Módulos

| Módulo | Estado |
|--------|--------|
| 1 — Game Engine | COMPLETADO |
| 2 — WebSockets / Sesión | COMPLETADO |
| 3 — Deck Builder API | EN PROGRESO |
| 4 — Persistencia PostgreSQL | COMPLETADO |
| 5 — Frontend | PENDIENTE |

---

## Reglas del Game Engine (Módulo 1 — COMPLETADO)

- **Cero frameworks web:** Prohibido importar `org.springframework.*` dentro de `engine/**`. 100% POJOs, instanciable sin DI. Validado por `EngineSpringIsolationTest`.
- **Fail Fast:** El motor lanza excepciones de dominio claras; nunca devuelve `null` ni `boolean` silenciosos.
- **TDD estricto:** No se escribe lógica de negocio sin un test en rojo previo que la justifique.
- **Patrones obligatorios:** State (fases del turno), Chain of Responsibility (pipeline de daño), Strategy (efectos de cartas), Observer (`PhaseEvent` → `KnockoutManager`, `VictoryConditionChecker`).
- **Providers:** El engine lee el estado de la mesa a través de interfaces (`BattlefieldStateProvider`, `BenchStateProvider`, `DeckStateProvider`, `PrizeStateProvider`). Nunca accede a DB directamente.

### Flujo de Fases del Turno

`TurnManager` sigue estrictamente el rulebook XY1 (`docs/SKILLS/game-rules-reference.md` §2):
1. **DrawPhase** — roba 1 carta. Excepción: el primer jugador no roba en turno 1. Deck vacío = derrota.
2. **MainPhase** — colocar Básicos, evolucionar (no en turno que entró ni en turno 1), 1 energía por turno, cartas Entrenador, retiro (1 vez/turno).
3. **AttackPhase** — ataque opcional; finaliza el turno automáticamente. No disponible en turno 1 del primer jugador.
4. **BetweenTurnsPhase** — efectos de estado en orden estricto: Envenenado → Quemado → Dormido → Paralizado → check KO.

### Pipeline de Daño (§3 rulebook)

1. Validar energías requeridas.
2. Si Confundido: moneda → cruz = falla el ataque, 3 contadores al atacante.
3. Prerrequisitos del ataque (selección de targets, monedas).
4. Efectos que cancelan el ataque.
5. Cálculo: daño base → modificadores atacante → Debilidad x2 → Resistencia -20 (mínimo 0) → modificadores defensor.
6. Efectos post-daño (aplicar Envenenamiento, descartar energías).

### Condiciones Especiales (§5 rulebook)

- **Dormido / Paralizado / Confundido** son mutuamente excluyentes (la más reciente reemplaza a la anterior).
- **Quemado / Envenenado** pueden coexistir con cualquiera.
- Al retirarse el Pokémon Activo, **todas** las condiciones especiales se eliminan.

### Condiciones de Victoria (§6 rulebook)

- Tomar la última carta de Premio.
- Bench-Out: el oponente no tiene Pokémon en Banca para reemplazar al Activo derrotado.
- Deck-Out: el oponente intenta robar con el mazo vacío.
- EX/Megaevolución derrotado: el oponente toma 2 cartas de Premio.

---

## Reglas de WebSockets (Módulo 2 — EN PROGRESO)

- **STOMP obligatorio.** Prohibido WebSockets crudos.
- **Canales:**
  - Clientes publican acciones a `/app/match/{matchId}/action` con header `playerId`.
  - Clientes se suscriben a `/topic/match/{matchId}/player/{playerId}`.
  - Polling REST disponible: `GET /api/matches/{matchId}/state` con header `X-Player-Id`.
- **Niebla de Guerra (CRÍTICO):** `PlayerPerspectiveMapper` es el único lugar donde se construye el `OpponentView`. JAMÁS enviar mano del rival al cliente — solo `opponentHandSize: int`. Leakear IDs o nombres de cartas del rival es una vulnerabilidad de seguridad.
- **Protocolo de lock (ADR-5):** Acquire lock → validar → apply → persist (dentro del lock) → release → broadcast (fuera del lock). `ReentrantLock` por `gameId`, nunca lock global.
- **Timeout de abandono:** Desconexión inicia timer de 60 segundos (`match.abandon.timeout-seconds`). Sin reconexión → derrota por abandono. Timer cancelable al reconectar.
- **Puerto de persistencia:** `GameStatePersistence` (actualmente no-op `NoOpGameStatePersistence`) se llama dentro del lock después de cada acción exitosa. El hook existe para el Módulo 4.

---

## Reglas de Persistencia (Módulo 4 — PENDIENTE)

- **JSONB para snapshots:** Estado del tablero como JSONB en PostgreSQL. No mapear cartas de banca a tablas relacionales individuales.
- **Log inmutable:** Tabla de acciones append-only (turno, jugador, acción, resultado).
- **Flyway:** Todo cambio de schema es un script `.sql` versionado.
- **Seed Data:** 146 cartas del set XY1 cargadas vía scripts Flyway (`V2__seed_xy1_cards.sql`), nunca llamando a la API en runtime.
- **N+1:** Prevenir con `JOIN FETCH` o EntityGraphs.

---

## Reglas del Frontend (Módulo 5 — PENDIENTE)

- **Smart/Dumb Components:** Solo los Container (Smart) inyectan servicios. Los Presentational (Dumb) solo reciben `input()` y emiten `output()`.
- **Signals obligatorios:** Prohibido RxJS para estado local (solo para WebSocket/HTTP). Usar `computed()` para valores derivados.
- **Control Flow nativo:** `@if`, `@for`, `@switch`. Prohibido `*ngIf`, `*ngFor`.
- **Standalone 100%.** Lazy Loading estricto con `loadComponent`.
- **`NgOptimizedImage`** obligatorio para imágenes de cartas.
- **Drag & drop** requerido en el tablero (RF-07 de la consigna).

---

## Reglas de Auditoría del Rulebook (OBLIGATORIO)

Cada vez que se trabaje sobre el Game Engine (explore, propose, apply, verify), leer `docs/SKILLS/game-rules-reference.md` y verificar:
1. El código refleja exactamente el orden de resolución de las reglas.
2. Los estados especiales se gestionan correctamente (exclusión mutua, orden between-turns).
3. Reportar violaciones como blockers con la regla exacta citada.

---

## Convenciones de Código

- `final` en parámetros y variables locales.
- `Objects.requireNonNull()` en constructores.
- Sin `if/else`, `switch`, ni `instanceof` para comportamiento polimórfico — usar Strategy/State.
- Colecciones declaradas como interfaces: `List<T>`, `Map<K,V>`, `Set<T>`.
- Nombres de tests descriptivos: `shouldThrowExceptionWhenAttackingWhileParalyzed()`.
- Conventional Commits (sin co-authored-by ni atribución a IA).
- GitFlow: ramas `feature/BE-<ticket>` o `feature/FE-<ticket>` → PR a `develop`. Nunca push directo a `main`.
- Reglas Checkstyle: `BE/.code_quality/checkstyle_rules.xml` | PMD: `BE/.code_quality/pmd_rules.xml`

---

## Auditoría Automática (OBLIGATORIO)

Después de completar cada batch de cambios (PR o sdd-apply), ejecutar `/audit` ANTES de commitear.
```
🔍 AUDIT COMPLETE — ✅ X passed | ⚠️ Y warnings | 🚨 Z blockers
```
Si hay blockers (🚨), NO commitear hasta resolverlos.
