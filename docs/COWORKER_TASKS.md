# Tareas para Coworkers — Backlog Maestro de Monstra TCG

Hola equipo. Este es el **backlog central y la visión completa** de Monstra TCG. Tenemos el MVP funcionando (Motor, Lobby, WebSockets, Tablero) pero hay mucho margen para mejorar arquitectura, agregar features que sumen puntos del rubric, y planear el roadmap post-TP hacia un producto de calidad AAA.

Este documento está organizado en bloques. **Los primeros son los más prioritarios para la entrega del TPI**; los siguientes son roadmap post-entrega y visión a largo plazo.

> **Nota metodológica:** cada tarea cita, cuando aplica, la sección de `docs/references/consigna.txt` o el archivo de rulebook que la justifica. Las propuestas que **no** están en consigna están marcadas como `[OPCIONAL]` o `[POST-TP]`.

---

## TABLA DE CONTENIDOS

1. [TAREAS CORE (MVP y mejoras de arquitectura)](#1-tareas-core)
2. [AUDIT — Lo que faltó verificar contra consigna y rulebook](#2-audit)
3. [ARQUITECTURA — Tres niveles de ambición](#3-arquitectura)
4. [IDEAS DE ECOSISTEMA Y MODOS DE JUEGO](#4-modos-de-juego)
5. [FEATURES PARA EL USUARIO FINAL (jugador)](#5-features-jugador)
6. [PANEL DE ADMINISTRADOR / BACKOFFICE](#6-panel-admin)
7. [FEATURES SOCIALES Y COMUNIDAD](#7-features-sociales)
8. [ACCESIBILIDAD Y QUALITY OF LIFE](#8-accesibilidad-y-qol)
9. [SEGURIDAD, FAIRNESS Y ANTI-CHEAT](#9-seguridad-y-anti-cheat)
10. [PRIORIZACIÓN — Qué meter antes de la entrega](#10-priorizacion-tpi)
11. [MAPEO RUBRIC → TAREAS (qué da puntos)](#11-mapeo-rubric)

---

## 1. TAREAS CORE

### 1.1. Refactorización del Frontend — `pokedex-page.ts` (Prioridad Alta)
- **Problema:** `pokedex-page.ts` tiene +1500 líneas y maneja todo el estado local del WebSocket.
- **Tarea:** Dividir el monolito en componentes modulares siguiendo el patrón Smart/Dumb descripto en `docs/SKILLS/pokemon-frontend-strict.md`.
- **Plan de migración propuesto (5 sprints):**
  - Sprint 1 — Crear `GameStateService` con signals y migrar el estado. El monolito ahora lee del service.
  - Sprint 2 — Extraer componentes "hoja" sin interacción: `DeckPileComponent`, `DiscardPileComponent`, `PrizeStackComponent`, `BattleLogComponent`.
  - Sprint 3 — Extraer `HandComponent` y `PokemonSlotComponent` (componente reusable para Activo + cada slot de Banca).
  - Sprint 4 — Extraer `ActionPanelComponent` y `TargetSelectorOverlay`. Las habilidades (B-04 del audit) viven acá.
  - Sprint 5 — Limpiar `GameBoardComponent` para que quede solo orquestación STOMP + composición. Objetivo < 200 líneas.
- **Reglas inviolables (`pokemon-frontend-strict.md`):**
  - Solo Smart components inyectan servicios. Un Dumb que llama a WebSocket o REST = PR rechazado.
  - Prohibido `*ngIf`/`*ngFor`. Solo `@if`/`@for`/`@switch`.
  - Prohibido `.mutate()` en signals. Solo `.set()` y `.update()`.
  - Prohibido RxJS para estado local. RxJS solo para WebSocket/HTTP.
  - `NgOptimizedImage` obligatorio para imágenes de cartas.
  - Lazy loading estricto con `loadComponent` en el router.
  - Standalone components 100%.
- **Cita rubric:** RNF-02 (4pts "código limpio") + Frontend 15pts.

### 1.2. Tolerancia a Fallos y Reconexión WS (Prioridad Alta)
- **Problema:** Si el WebSocket STOMP se cae, el usuario pierde el flujo.
- **Tarea:** Implementar reconexión automática en el cliente y resincronización de estado en el backend al reconectar sin romper la fase del turno.
- **Cita consigna:** RF-06 — "Manejo robusto de reconexiones: si un cliente se desconecta, debe poder reconectarse y recibir el estado actualizado de la partida para continuar jugando".
- **Implementación recomendada:**
  - Endpoint REST auxiliar `GET /api/matches/{matchId}/state` con header `X-Player-Id` (ya recomendado en `docs/SDD/02_SessionAndWebSockets.md`).
  - Backoff exponencial en el cliente STOMP (1s → 2s → 4s → 8s, máx 30s).
  - Indicador visual de estado de conexión (verde/amarillo/rojo) en la UI.
  - Abandon timer de 60s (parámetro `match.abandon.timeout-seconds` en `application.yml`). Cancelable al reconectar.

### 1.3. Dockerización Completa (Prioridad Media)
- **Problema:** Solo la DB está en Docker. Backend y frontend se levantan a mano.
- **Tarea:** Crear `Dockerfile` para Backend (JDK 21) y Frontend (Node/Nginx). Orquestar todo en `docker-compose.yml`.
- **Bonus:** profile de docker-compose con un `pgadmin` y un `swagger-ui` para QA y evaluadores.

### 1.4. UI del Creador de Mazos / Deck Builder (Prioridad Media)
- **Problema:** La API de creación de mazos existe, pero el front usa un JSON hardcodeado.
- **Tarea:** Armar la interfaz visual en Angular con:
  - Grid paginado de cartas del set xy1 (146 cartas).
  - Filtros por tipo, supertipo, subtipo, HP, costo de retiro.
  - Drag & drop hacia la lista del mazo.
  - Validación visual en vivo (max 4 copias, max 1 ACE SPEC, al menos 1 Básico, 60 cartas exactas).
  - Mensajes de error accionables: "Te faltan 5 cartas para completar el mazo", "Tenés 5 copias de Charizard, máximo 4".
- **Cita consigna:** RF-04 completo.

### 1.5. Optimización de Consultas N+1 (Prioridad Media)
- **Tarea:** Revisar `MatchRepository`, `DeckRepository`, `MatchActionRepository` y aplicar `@EntityGraph` o `JOIN FETCH` donde corresponda.
- **Cita rubric:** DB 10pts incluye "queries eficientes con índices y sin problemas de N+1" (3pts directos).

### 1.6. Testing E2E (Prioridad Baja)
- **Tarea:** Tests `@SpringBootTest` cubriendo flujos completos. Playwright o Cypress para flujo FE básico (crear mazo → unirse a partida → ejecutar un turno).
- **Cita rubric:** Testing 10pts incluye "Al menos un test E2E cubriendo el flujo básico" (2pts).

---

## 2. AUDIT

Lo siguiente surgió de comparar el estado actual del proyecto con la consigna y el rulebook. Son cosas concretas que **faltan** o están **incompletas** respecto a los requerimientos.

### 2.1. Wiring de `logAction()` en `GameFacade` (CRÍTICO)
- **Estado:** El log inmutable de acciones existe (Coworker 1 hizo `JpaGameStatePersistence`) pero `GameFacade.apply()` no llama a `gameStatePersistence.logAction()`.
- **Cita:** RF-05 — "El registro de acciones (log) debe ser completo e inmutable: cada entrada debe indicar turno, jugador, tipo de acción y resultado".
- **Acción:** Agregar la llamada al final de cada `case` exitoso en `GameFacade.apply()`, **dentro del lock** (ADR-5).

### 2.2. Endpoint REST de rehidratación
- **Estado:** `docs/SDD/02_SessionAndWebSockets.md` lo cita como necesario para reconexión. Verificar si existe.
- **Acción:** Si no existe, `GET /api/matches/{matchId}/state` con header `X-Player-Id`, retorna `PlayerPerspectiveMapper.toView(session, playerIndex)`.

### 2.3. Broadcast con DTO serializado dentro del lock
- **Estado:** Verificar que `MatchService` construye el `GameStateView` *antes* de soltar el lock (ADR-5 dice "persist dentro del lock, broadcast fuera").
- **Riesgo:** Si la serialización pasa fuera del lock, dos acciones simultáneas pueden producir broadcasts cruzados.

### 2.4. Blockers funcionales del engine (audit existente)
- **B-04:** `UseAbilityAction` no tiene resolver. RF-01b y RF-07 piden habilidades. Crear `AbilityEffectResolver` espejo del `TrainerEffectResolver`.
- **B-03 parcial:** Solo RED_CARD y TEAM_FLARE_GRUNT tienen efecto. Implementar al menos:
  - Objetos de robo: `PROFESSOR_SYCAMORE`, `SHAUNA`.
  - Objetos de búsqueda: `TIERNO`, `POKEBALL`.
  - Objetos de recuperación: `SUPER_POTION`, `POTION`.
  - Estadios: `POKEMON_RESEARCH_LAB`.
  - Herramientas (attach + efectos continuos).

### 2.5. Warnings activos del audit (no críticos pero suman calidad)
- **W-01:** Falta el paso 4 del pipeline de ataque — efectos que cancelan el ataque (rulebook §3).
- **W-04:** Diferenciar Energía Especial de Energía Básica con `EnergyKind { BASIC, SPECIAL }` (RF-02).
- **W-05:** Cambiar `boolean toolAttached` → `Optional<TrainerCard> attachedTool`.
- **W-NEW-02:** `validatePlaceBasicPokemon()` debe verificar que la carta sea de tipo BASIC (rulebook §2).

### 2.6. Conflicto en versiones — VERIFICAR
- **Estado:** `CLAUDE.md` dice "Spring Boot 4.0.0" pero consigna y SDDs dicen "Spring Boot 3.x".
- **Acción:** Chequear `BE/pom.xml`. Si está en 4.x, downgrade a 3.x (tecnología obligatoria). Si está en 3.x, corregir `CLAUDE.md`.

---

## 3. ARQUITECTURA — Tres niveles de ambición

La consigna pide cumplir RFs/RNFs. Para una demo del TP basta el mínimo. Para ir más allá hay dos niveles más altos. Elegir según tiempo restante.

### 3.A — Opción "Cumple consigna" (RECOMENDADA para entrega)
**Stack:** Spring Boot 3.x + STOMP `SimpleBroker` + `ReentrantLock` por `gameId` (ya existe) + Tomcat default + PostgreSQL síncrono dentro del lock.

**Por qué:** Mínimo código nuevo, cero infra adicional, cumple consigna al 100%. Soporta cómodamente las 8-16 conexiones que tendrás en evaluación.

**Pros:** Defendible, simple, testeable.

**Contras:** No escala a más de un nodo. **No te baja puntos del rubric.**

### 3.B — Opción "TP con calidad demostrable"
**Stack:** Opción A + Virtual Threads (Java 21 — Project Loom) en el executor del WebSocket + outbox liviano + abandon timer.

**Por qué:** Demuestra conocimiento del JDK moderno, justifica RNF-01, suma en la defensa oral.

**Cómo:**
- En `WebSocketMessageBrokerConfigurer`, configurar `clientInboundChannel.taskExecutor()` con `Executors.newVirtualThreadPerTaskExecutor()`.
- Outbox: `BlockingQueue<LogEntry>` en memoria + `@Scheduled(fixedDelay=200ms)` que drena por batch. **Trade-off:** debilita durabilidad ante crash entre queue y flush.
- `ScheduledExecutorService` para abandon timer cancelable.

**Cita:** RNF-01 (200ms p99).

### 3.C — Opción "AAA / Long-term vision" [POST-TP]
**Stack:** Opción B + RabbitMQ STOMP broker + consistent hashing por `gameId` + event sourcing como fuente de verdad + Redis para presence.

**No la recomiendo para el TP** — overkill, no suma puntos, aumenta riesgo de no entregar.

**Para qué sirve mencionarla:** en la defensa oral como "visión de escalabilidad". Para 10.000 partidas concurrentes sería el target.

---

## 4. MODOS DE JUEGO

### 4.1. Modos competitivos clásicos (algunos ya listados antes)

**Ranked / Sistema de Ligas con MMR**
- Inspiración: Hearthstone, MTG Arena.
- Implementación: `RankingService` con algoritmo ELO clásico (K-factor 30). Ligas: Bronce, Plata, Oro, Platino, Diamante, Maestro, Gran Maestro.
- Lobby cambia: matchmaking por MMR ± 100 puntos.
- Cita rubric: opcional "ranking o historial" (1pt).

**Torneos Suizo**
- `TournamentController`, lógica de emparejamiento por puntos acumulados, desempates por buchholz.
- Vista de bracket en el frontend.

**Modo Draft / Arena**
- Inspiración: Hearthstone Arena.
- Te presentan triples de opciones para armar un mazo de 30 cartas. Jugás hasta ganar 7 o perder 3.

### 4.2. Modos basados en el engine existente (BAJO COSTO técnico)

**Aetherlog — Replay Viewer**
- El log inmutable de RF-05 es 100% event-sourced. Construir un viewer es leer el log y aplicar snapshots con un slider de tiempo.
- Endpoint: `GET /api/matches/{matchId}/replay`.
- Componente: `<app-replay-viewer>` que reutiliza los componentes Dumb del tablero.
- **Refuerza Arquitectura del rubric** porque demuestra que el log es fuente de verdad real.

**Forge — Puzzle del Día**
- Estado inicial pre-fabricado (snapshot JSONB) + objetivo ("logra KO al activo este turno").
- El cliente envía secuencia de acciones a `POST /api/puzzles/{id}/solve`. El backend instancia un `MatchSession` desde el snapshot, aplica acciones, compara resultado con el objetivo.
- Cero WebSockets necesarios. Sub-modo single-player.
- **Demuestra reutilización del engine fuera del PvP.**

**Modo Espectador**
- Crear `SpectatorView` análogo a `PlayerPerspectiveMapper.toView()` pero con AMBOS jugadores filtrados (manos como contadores, mazos como contadores, premios boca abajo).
- Canal STOMP `/topic/match/{matchId}/spectator`.
- Endpoint `GET /api/matches/active` listando partidas en curso.

**Spectator Predictions**
- Espectadores apuestan "Insight Points" (moneda cosmética) sobre la próxima jugada.
- Predicciones tipadas: "tomará el próximo premio antes del turno 8", "el ganador es el jugador A", "se descarta una energía especial".
- Leaderboard de Analystas.

### 4.3. Modos creativos basados en el rulebook XY1

**Ace Spec Showdown**
- Modo competitivo donde sí o sí incluis 1 AS TÁCTICO en el mazo, revelado antes de la partida.
- Aprovecha la validación "max 1 ACE SPEC" que ya existe.

**Mulligan Madness**
- Modo casual donde los mulligans dan bonus draws extra al oponente (2 en lugar de 1).
- Aprovecha `SetupManager` existente.

**Sudden Death Open**
- Torneo donde toda la primera ronda empieza directo en Sudden Death (1 premio).
- Partidas cortas, ideal para evaluación rápida en demo.
- Aprovecha `MatchCreationService.resetForSuddenDeath()`.

**Status Effect Roulette**
- Solo mazos centrados en condiciones especiales (Veneno/Quemado/Dormido).
- Estresa la lógica de coexistencia y exclusión mutua.

### 4.4. Modos cooperativos / PvE

**Asymmetric Raid (2v1)**
- 2 jugadores cooperando contra un "Boss Pokémon" controlado por scripting determinista.
- Boss con 200 HP, dos turnos por ronda, ataques con efectos custom ("Inferno Pulse: 80 daño en área a Activo + 1 banca").
- Reutiliza el engine — solo agregar un `BossActionStrategy` que reemplaza al `Player` adversario.
- Eventos semanales con bosses temáticos.

**Tag Team 2v2**
- 4 jugadores en dos equipos. Dos tableros que comparten zona compartida (Stadium).
- Cada jugador tiene su Activo y Banca. La energía de un jugador puede asignarse al Pokémon del otro.

**Gym Leader Conquest [POST-TP]**
- Career mode contra NPCs temáticos (Líderes de Gimnasio del XY).
- Cada Líder con su mazo y reglas especiales del gimnasio.
- Unlocks: cosméticos al derrotar a cada Líder.

**Roguelike Run**
- Career single-player. Empezás con un pool mínimo. Cada victoria suma cartas. La primera derrota termina la run.
- Sistema de "relics" (modificadores temporales): "Tu primer ataque del turno cuesta 1 energía menos", "Empezás cada turno con 1 carta extra".

### 4.5. Modos casuales / fun

**Speed Chess Mode**
- Cada turno tiene 30 segundos. Si no actuás, se pasa el turno automáticamente.
- Para jugadores expertos.

**No Mulligan Mode**
- Tu mano inicial es la que toca, sin oportunidad de re-shuffle aunque no tengas Básico.
- Reduce el setup pero introduce variance brutal.

**Mirror Match Challenge**
- Ambos jugadores usan el mismo mazo random.
- Mide pura habilidad de pilotaje.

**Themed Weekly**
- Solo cartas de cierto tipo permitidas esa semana ("solo Fuego y Agua").
- Refresca el meta semana a semana sin tocar cartas.

**King of the Hill**
- Hay una "silla" en el lobby. Quien la ocupa juega contra el siguiente challenger. Si pierde, sale.
- Streak más larga = puntos extra de cosméticos.

**Sealed Evolutionary [POST-TP]**
- Pool aleatorio que crece con victorias, bloqueado por arquetipo.
- Temporadas mensuales con reset.

### 4.6. Modos didácticos

**Tutorial Interactivo**
- Onboarding paso a paso: mover una carta, atacar, evolucionar, retirarse.
- Importante para retención de usuarios nuevos.

**Practice vs Bot**
- IA básica con reglas heurísticas (no ML, solo `if/else` con prioridades).
- Tres niveles: Fácil (juega random válido), Medio (prioriza ataques), Difícil (planifica 2 turnos adelante).
- También útil para QA — testear flujos sin un humano del otro lado.

**Sandbox / Free Setup**
- Modo creador: configurás el tablero como querés (estado de manos, banca, premios) y experimentás interacciones.
- Útil para entrenar combos.

---

## 5. FEATURES PARA EL USUARIO FINAL (JUGADOR)

### 5.1. Perfil y personalización

- **Avatar personalizable** — biblioteca inicial de 20-30 sprites de Pokémon o trainers.
- **Frames de avatar** — desbloqueables por logros.
- **Títulos** — debajo del nombre. Ej: "Maestro del Fuego", "Veterano de 100 partidas".
- **Banners de perfil** — fondo decorativo.
- **Card sleeves** — diseños cosméticos del dorso de las cartas.
- **Playmat / tablero** — fondo del tablero personalizable.
- **Card backs animadas** — cosmético premium.
- **Insignias visibles** — al lado del nombre durante la partida.

### 5.2. Sistema de progresión por mazo / Pokémon

- **Mastery de Pokémon** — cada Pokémon que usás sube de "nivel de uso". Es solo cosmético (estrellitas al lado del nombre) pero motiva variar mazos.
- **Estadísticas por mazo** — win rate, partidas jugadas, oponentes derrotados con ese deck.
- **Logros por Pokémon** — "Gana 10 partidas con Charizard", "KO 50 Pokémon con un solo ataque".

### 5.3. Colección y catálogo

- **Card Viewer** — catálogo navegable de las 146 cartas con info detallada.
- **Search & filter** — por tipo, supertipo, HP, costo, daño máximo.
- **Favoritos** — pin de cartas predilectas al top de búsquedas.
- **Estadísticas de uso** — "Esta carta fue usada en el 47% de los mazos top esta semana".
- **Historial por carta** — "Tu primer KO con esta carta fue el 2026-05-20".

### 5.4. Lista de amigos y matchmaking social

- **Friend list** con online/offline status.
- **Invite to play** — invitación directa a partida privada.
- **Match history compartido** — ver partidas pasadas contra un amigo.
- **Rivals** — sistema automático que marca a oponentes con los que jugaste 5+ veces.
- **Recently played with** — lista de últimos 10 oponentes para volver a invitar.

### 5.5. Historial y replays personales

- **Match history** filtrable por deck, oponente, resultado, fecha.
- **Replay sharing** — link público a un replay específico.
- **Highlights export** — generar GIF de un momento puntual de la partida (KO épico, premio decisivo).
- **Match notes** — anotaciones personales después de la partida ("me equivoqué en el turno 4, debí retirar").
- **Win/loss stats por arquetipo** — gráficos de torta del rendimiento por tipo de deck.

### 5.6. Notificaciones y alertas

- **Sistema de notificaciones in-app** — turno comienza, oponente se desconectó, alguien te desafió.
- **Preferencias granulares** — activar/desactivar por tipo de evento.
- **Toast configurables** — duración, posición, sonido.
- **Notificaciones push** [POST-TP] — torneo arranca, un amigo está online.

### 5.7. Pre-game y lobby

- **Quick deck swap** — cambiar de mazo mientras esperás un match.
- **Deck preview** — vista rápida de la composición antes de empezar.
- **Match preferences** — tipo de match, MMR, idioma del oponente, sin chat, etc.
- **"Listo" / "No listo"** — sistema explícito para empezar la partida.
- **Coin flip animation** — animación visible para determinar quién empieza (consigna RF-01a "Se lanza una moneda").

### 5.8. Durante el partido

- **Auto-pass** — pasar de fase automáticamente cuando no hay acciones posibles (ej. Pokémon dormido + sin banca).
- **Card preview on hover** — ver la carta en grande sin clic.
- **Hand sort** — por tipo, costo, nombre, fecha de robo.
- **Bench reorder** — drag para reordenar (puramente visual, no afecta engine).
- **Quick stats overlay** — daño total infligido, energías unidas, premios tomados.
- **Confirm dialog** para acciones irreversibles — "¿Atacar con X? Esto termina el turno".
- **Undo limitado** — antes de confirmar acciones, una ventana para deshacer. **Una vez confirmada al backend, no hay vuelta atrás.**
- **Turn timer visible** — countdown con cambio de color (verde → amarillo → rojo).
- **Phase indicator prominente** — siempre claro en qué fase estás.
- **Action log scrolleable** — historial completo de la partida, con filtros.
- **Emotes** — biblioteca limitada de 10-15 emotes para evitar toxicidad. Mensajes pre-aprobados ("¡Buena jugada!", "Lo lograste!").
- **Auto-GG** — al final de la partida, botón rápido para "Good Game" al oponente.

### 5.9. Post-partido

- **Resumen de partida** — duración, turnos, acciones más relevantes, MVP del mazo.
- **Compartir resultado** — link copiado al portapapeles para mostrar el match.
- **Rate the opponent** — sistema opcional de reputación (positiva/neutra/negativa).
- **Report player** — flujo para denunciar comportamiento inapropiado.
- **Auto-rematch** — botón para volver a jugar contra el mismo oponente.

### 5.10. Tutorial y aprendizaje

- **Interactive onboarding** — primera vez que entrás, 5-10 min de tutorial guiado.
- **In-game glossary** — diccionario de términos accesible en cualquier momento (Pokémon-EX, ACE SPEC, mulligan, etc.).
- **Hint system** — toggleable, para jugadores nuevos. Sugiere acciones obvias ("Tenés energías sin usar").
- **Replay-driven lessons** — pequeños tutoriales con replays de jugadas didácticas.
- **Rules reference popup** — modal con las reglas del rulebook XY1 accesible desde el menú.

---

## 6. PANEL DE ADMINISTRADOR / BACKOFFICE

Ya está listado como tarea 12 en el doc original. Acá lo desarrollo en detalle.

### 6.1. Dashboard operativo

- **Partidas activas en vivo** — contador real-time, lista con `matchId`, jugadores, fase actual.
- **Usuarios online** — count y lista.
- **Latencia promedio** — p50, p95, p99 de tiempo de respuesta a acciones.
- **GC pressure** — métricas del JVM expuestas vía `/actuator/metrics` (Spring Boot Actuator).
- **DB connections** — pool ocupado, en espera, máximo.
- **WebSocket sessions** — count, conexiones por nodo.
- **Errores por minuto** — log de excepciones del backend con búsqueda.

### 6.2. Gestión de usuarios

- **Búsqueda y filtrado** por nombre, email, fecha de registro, estado.
- **Ver perfil completo** — partidas, mazos, estadísticas, IPs usadas.
- **Ban/Suspend** con duración configurable (1 día, 7 días, permanente) + motivo.
- **Audit log** de acciones admin (quién baneó a quién, cuándo, por qué).
- **Restablecer contraseña** del usuario manualmente.
- **Resetear MMR** — útil para corregir errores de ranking.
- **Otorgar cosméticos** — premios manuales por concursos, compensación por bugs.

### 6.3. Gestión de partidas

- **Match audit viewer** — leer el log inmutable de cualquier partida.
- **Force end** — terminar una partida fantasma (jugadores desconectados sin abandon timer).
- **Override result** — corregir manualmente un resultado disputado.
- **Refund prizes** — devolver premios mal otorgados.

### 6.4. Gestión de cartas y mazos

- **Cards database editor** — corregir errores en el caché xy1 (errata, traducción).
- **Reseed XY1 cards** — botón para repoblar desde la API si la tabla queda corrupta.
- **Card popularity dashboard** — % de mazos que incluyen cada carta.
- **Win-rate by archetype** — qué arquetipos están dominando.
- **Most banned/reported decks** — heurística para detectar combos abusivos.
- **Deck templates oficiales** — administrar los 6 mazos temáticos seed.

### 6.5. Tournament management

- **Crear torneo** — formato (suizo, single elim, double elim), fecha, premios, límite de jugadores.
- **Monitor en vivo** — bracket, partidas en curso, standings.
- **Bye automático** y manual.
- **Disqualify player** con motivo.
- **Export resultados** — CSV/JSON para registro histórico.

### 6.6. Anti-cheat y fairness

- **Anti-cheat audit log** — patrones sospechosos (acciones imposiblemente rápidas, mismas IPs en ambos lados de un match para boostear).
- **Network anomaly detection** — desconexiones sistemáticas durante turnos perdedores.
- **Win-rate threshold flags** — usuarios con > 95% win rate se marcan para revisión.
- **Replay analysis tool** — admin puede ver cualquier replay con anotaciones.
- **Shadow ban** — usuario sigue jugando contra otros shadow-banned (sin saberlo).

### 6.7. Comunicación con usuarios

- **Push announcements** — banner global ("Mantenimiento en 2 horas").
- **Maintenance mode** — toggle que cierra el lobby y muestra mensaje.
- **Email templates editor** — bienvenida, recuperación de contraseña, suspensión.
- **Notification log** — historial de todo lo enviado.
- **Newsletter manager** — campañas opt-in con stats de open rate.

### 6.8. Feature flags y configuración

- **Feature flags toggle** — activar/desactivar modos sin redeploy.
- **A/B testing config** — porcentaje de usuarios que ven feature X.
- **Rate limits override** — por usuario o por endpoint.
- **System config** — parámetros editables (abandon timeout, max conexiones, max partidas por usuario).

### 6.9. Backup y disaster recovery

- **Manual backup trigger** — disparar dump completo.
- **Restore from backup** con flujo de confirmación.
- **Data export** — dump de un usuario completo (GDPR-compliance).
- **Data deletion** — wipe completo de un usuario (right to be forgotten).

### 6.10. Analítica de producto

- **DAU / WAU / MAU** — usuarios activos diarios, semanales, mensuales.
- **Cohort retention** — % de usuarios que vuelven al día 1, 7, 30.
- **Funnel** — registro → primer mazo → primer match → primera victoria.
- **Tiempo promedio por partida** y distribución.
- **Heatmap de horarios** — cuándo se juega más.
- **NPS in-app** — encuesta opcional post-partida.

---

## 7. FEATURES SOCIALES Y COMUNIDAD

### 7.1. Clanes / Guilds [POST-TP]

- Crear/unirse/dejar clanes (hasta 50 miembros).
- **Clan chat** persistente.
- **Clan vs Clan tournament** — competencias inter-clan semanales.
- **Roles internos** — Líder, Oficial, Miembro.
- **Clan playmat/sleeves** — cosmético exclusivo del clan.

### 7.2. Sistema de mentoría

- Jugadores experimentados (>500 partidas, win rate > 60%) opt-in como mentores.
- **Apprentice mode** — newbies se emparejan con mentores en partidas casuales.
- **Anotaciones colaborativas** — el mentor agrega comentarios sobre el replay del aprendiz.
- **Mentor rating** — el aprendiz califica al mentor; mentores con buena reputación tienen badge especial.

### 7.3. Chat y mensajería

- **Chat global del lobby** [opcional, +2pts]
- **DMs entre amigos** — historial persistente.
- **Channels por interés** — `#general`, `#deck-help`, `#tournaments`, `#bug-reports`.
- **Moderación** — palabras prohibidas, sistema de reportes, autobans temporales.
- **Idioma preferido** — matchmaking entre hablantes del mismo idioma cuando posible.

### 7.4. Eventos comunitarios

- **Eventos cronometrados** — torneos semanales con premios cosméticos.
- **Card of the Day** — cartilla destacada con stats y trivia.
- **Weekly meta report** — informe automatizado de los arquetipos top.
- **Community decklists** — los mejores mazos del momento, votados por la comunidad.

### 7.5. UGC (User-Generated Content)

- **Deck guides** — usuarios pueden publicar guías sobre sus mazos. Mazo + texto + replays anclados.
- **Replays destacados** — votados por la comunidad, ranking semanal.
- **Comentarios en replays** — discusión asíncrona sobre jugadas concretas.
- **Custom emote submissions** — comunidad propone, mods aprueban.

### 7.6. Streaming integration [POST-TP]

- **Twitch overlay** — mostrar tu rank, mazo actual, win streak en vivo.
- **Spectator-with-streamer mode** — viewers pueden hacer predicciones de los matches en vivo.
- **Stream alerts** — notificación in-game cuando alguien que sigues está streameando.

---

## 8. ACCESIBILIDAD Y QUALITY OF LIFE

### 8.1. Accesibilidad visual

- **Color-blind mode** — paleta alternativa para tipos de Pokémon (que están codificados por color).
- **Card text scaling** — slider para agrandar texto en cartas.
- **High contrast mode** — fondos más oscuros, bordes más fuertes.
- **Animation toggle** — opción de desactivar animaciones (mareo, epilepsia).
- **Reduced motion preference** — respetar `prefers-reduced-motion` del SO.
- **Focus indicators visibles** — outline de teclado claramente diferenciado.

### 8.2. Accesibilidad de input

- **Mouse-only mode** — todas las acciones accesibles solo con mouse (sin atajos teclado).
- **Keyboard-only mode** — atajos completos para power users.
- **Touch-optimized mode** — controles más grandes, hit areas amplios para tablet.
- **Drag & drop alternativo** — botón "Mover a..." para usuarios que no pueden drag.

### 8.3. Accesibilidad cognitiva

- **Modo lento** — turn timer extendido para usuarios que necesitan más tiempo.
- **Tutorial siempre accesible** — no solo primera vez.
- **Confirmaciones explícitas** — para acciones irreversibles (atacar, descartar carta, terminar turno).
- **Resumen del turno del oponente** — al empezar tu turno, breve resumen de lo que hizo el rival.

### 8.4. Internacionalización (i18n)

- **Español (Argentina, Latam, España)** — idioma base.
- **Inglés** — segundo idioma.
- **Portugués (Brasil)** — comunidad TCG grande en Brasil.
- **Card text** ya viene en inglés desde la API; usar archivos `.json` de traducción para campos críticos del UI.

### 8.5. Audio

- **Música de fondo** — track relajante en lobby, más intensa en partida.
- **Sound effects** — flip de moneda, daño aplicado, KO, robo de premio.
- **Volume sliders** — música y FX independientes.
- **Mute por evento** — sin audio para chat global, con audio para alertas de partida.

### 8.6. Performance y QoL técnica

- **Quick reconnect** — al reabrir la app, recuperar la última partida si seguía activa.
- **Multi-tab safe** — detectar si abriste la partida en dos tabs, kickear una.
- **Offline mode parcial** — Deck Builder puede usarse sin conexión a Internet (solo cache local).
- **Bandwidth saver mode** — imágenes en baja resolución para conexiones lentas.
- **Battery saver mode** — animaciones reducidas en dispositivos con batería baja.

---

## 9. SEGURIDAD, FAIRNESS Y ANTI-CHEAT

### 9.1. Validación obligatoria

- **Toda acción se valida en backend** (RNF-05). FE solo presenta.
- **Mano del oponente nunca al cliente** — solo `handCount`. Si en algún DTO aparece, se rechaza el PR.
- **Orden del mazo oculto** — no exponerlo ni cifrado.
- **Cartas de Premio ocultas hasta tomarse** — RF-01 + RNF-05.

### 9.2. Anti-cheat

- **Patrones de tiempo anómalos** — acciones a milisegundos imposibles para humanos.
- **Coincidencias de IP/dispositivo** — detectar boosting (mismo jugador en ambos lados).
- **Desconexiones sospechosas** — disconnect frecuente en momentos de derrota inminente.
- **Replay forensics** — admin puede comparar varios replays buscando patrones.
- **Sandbox / Practice mode** — separado del ranked. Cero impacto en MMR ni cosméticos.

### 9.3. Privacidad

- **Eliminar cuenta** — GDPR compliance. Wipe completo o anonymization (mantener stats agregadas, eliminar PII).
- **Export de datos** — usuario puede pedir dump completo.
- **Visibilidad granular** — partidas públicas/privadas/solo amigos.
- **Block list** — usuarios bloqueados no pueden invitarte ni verte online.

### 9.4. Seguridad de autenticación

- **JWT con refresh tokens** (opcional según RNF-05).
- **2FA** [POST-TP] — TOTP (Google Authenticator).
- **Password strength meter** durante registro.
- **Rate limiting** en endpoints sensibles (login, registro, recuperación).
- **HTTPS obligatorio** en producción.

---

## 10. PRIORIZACIÓN — Qué meter antes de la entrega

Si tenés que elegir qué hacer en los días que quedan, este es el orden óptimo de ROI (puntos / esfuerzo):

### Tier 1 — IMPRESCINDIBLE (riesgo de no aprobar)

1. **Wirear `logAction()` en `GameFacade`** — RF-05 explícito.
2. **Endpoint REST de rehidratación** + reconexión robusta — RF-06.
3. **Refactor mínimo de `pokedex-page.ts`** + drag & drop con CDK — RF-07 + rubric FE 15pts.
4. **B-04 (abilities) + B-03 (más trainer effects)** — RF-01b "usar Habilidades", RF-02 tipos de Trainer.
5. **N+1 queries fix** — rubric DB 3pts directos.
6. **Animaciones para condiciones especiales (rotaciones)** — RF-07 literal.

### Tier 2 — ALTO ROI (suma puntos opcionales)

7. **6 mazos temáticos seed** — opcional 10pts.
8. **Replay viewer básico** — refuerza Arquitectura y FE.
9. **Chat in-game** — opcional 2pts.
10. **Animaciones de ataque/KO/evolución** — opcional 2pts.
11. **Ranking básico + historial** — opcional 1pt.

### Tier 3 — NICE TO HAVE (defensa oral)

12. **Megaevolución** — RF-02 opcional.
13. **Modo Espectador** — refuerza Fog of War (RNF-05).
14. **Modo Practice vs Bot** — útil para QA y demos.
15. **Admin dashboard mínimo** — operativo para evaluadores.

### Tier 4 — POST-TPI (roadmap)

Todo lo demás de las secciones 4-9.

---

## 11. MAPEO RUBRIC → TAREAS

Cada item del rubric con las tareas que lo cubren.

### Funcionalidad — 40pts

| Criterio | Pts | Tareas que lo cubren |
|---|---|---|
| Reglas RF-01 a RF-07 correctas | 10 | Wiring logAction (2.1), B-04 abilities, B-03 trainers, W-01 cancel attack, validate place basic |
| Partida completa jugable | 10 | Toda la cadena del Tier 1 |
| Validaciones backend correctas | 10 | RuleValidator + tests (audit ya tiene 13 puntos validados) |
| Condiciones especiales correctas | 5 | StatusEffectManager (audit OK) + animaciones de rotación |
| Cálculo de daño correcto | 5 | DamageCalculator (audit OK) |

### Arquitectura — 25pts

| Criterio | Pts | Tareas |
|---|---|---|
| Separación Controller/Service/Repository/Engine | 6 | Ya está. Mantener Fix de SimpleBroker no romperlo |
| Engine con componentes independientes | 6 | Audit ya valida aislamiento Spring. Reforzar tests JaCoCo 90% |
| Patrones de diseño aplicados | 5 | State + Strategy + CoR + Observer + Repo + Facade (todos presentes) |
| Código limpio | 4 | Refactor pokedex-page.ts cumple esto en FE |
| Manejo de errores robusto | 4 | Fail Fast del engine + exception handlers de Spring |

### Base de Datos — 10pts

| Criterio | Pts | Tareas |
|---|---|---|
| Modelo de datos correcto | 5 | Flyway schema con matches, match_actions, snapshots JSONB |
| Queries eficientes sin N+1 | 3 | Tarea 1.5 (JOIN FETCH, EntityGraph) |
| Constraints y validaciones DB | 2 | Foreign keys, NOT NULL, índices |

### Frontend — 15pts

| Criterio | Pts | Tareas |
|---|---|---|
| Drag & drop, panel de acciones, feedback visual | 6 | CDK DragDrop + ActionPanel + rotaciones |
| Estado sincronizado vía WebSocket | 5 | GameStateService con signals + STOMP |
| Diseño claro | 2 | Componentización por zonas (RF-07) |
| Funciona en desktop + tablet | 2 | Tailwind responsive + touch-optimized |

### Testing — 10pts

| Criterio | Pts | Tareas |
|---|---|---|
| Cobertura ≥80% global y ≥90% en críticos | 5 | JaCoCo configurado en pom.xml |
| Tests de integración | 3 | `@SpringBootTest` para partida completa, mulligan, evolución, KO, victoria |
| Test E2E básico | 2 | Playwright/Cypress para crear mazo + match + un turno |

### Opcionales — +15pts bonus

| Opcional | Pts | Tareas |
|---|---|---|
| Mazo temático seed funcional | 10 | 6 mazos en `V3__seed_themed_decks.sql` |
| Animaciones FE | 2 | Angular Animations API |
| Ranking / historial | 1 | RankingService + endpoints GET |
| Chat in-game | 2 | ChatWebSocketController + componente FE |

### Bonus sin puntos cuantificados

- Megaevolución (RF-02).
- Expansiones extra al xy1 (RF-04).

---

## CONVENCIONES DE TRABAJO

- **GitFlow:** ramas `feature/BE-<ticket>` o `feature/FE-<ticket>` → PR a `develop`. Nunca push directo a `main`.
- **Conventional Commits.** Sin co-authored-by ni atribución a IA.
- **Audit obligatorio** (`/audit`) antes de cada commit a `develop`. Si hay blockers 🚨, no commitear.
- **Tests primero** — no escribir lógica nueva sin un test rojo que la justifique (TDD estricto en engine).
- **Documentación viva** — actualizar `docs/` cuando un módulo cambie significativamente.

---

## CITAS DE REFERENCIA

Todas las recomendaciones de este documento están justificadas por:

- **Consigna:** `docs/references/consigna.txt`
- **Rulebook XY1 (resumen algorítmico):** `docs/SKILLS/game-rules-reference.md`
- **Rulebook XY1 completo:** `docs/references/rulebook.txt` y `docs/references/xy1-rulebook-es (1).pdf`
- **Arquitectura:** `docs/ARCHITECTURE.md`
- **SDDs por módulo:** `docs/SDD/01_GameEngine.md` a `06_ApiContracts.md`
- **Skills estrictas:** `docs/SKILLS/pokemon-engine-tdd.md`, `pokemon-frontend-strict.md`, `pokemon-websockets-strict.md`, `pokemon-persistence-strict.md`, `pokemon-rulebook-auditor.md`
- **API externa:** `docs/references/pokemontcg-api-reference.md`
- **Audit previo del engine:** `docs/AUDIT_ENGINE.md`
- **Plan de implementación detallado:** `docs/IMPLEMENTATION_PLAN.md`
- **Plan de acción inmediato:** `docs/ACTION_PLAN.md`
- **Guías de repositorio:** `docs/REPO_GUIDELINES.md`

**Nota final:** este documento es **vivo**. Si encontrás errores, contradicciones con la consigna, o features que se nos pasaron, actualizalo directamente y mencioná el cambio en el commit. La idea es que sea el primer lugar al que cualquier coworker mira para entender el roadmap.

¡Éxitos equipo!
