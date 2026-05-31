# Tareas para Coworkers ÔÇö Backlog Maestro de Monstra TCG

Hola equipo. Este es el **backlog central y la visi├│n completa** de Monstra TCG. Tenemos el MVP funcionando (Motor, Lobby, WebSockets, Tablero) pero hay mucho margen para mejorar arquitectura, agregar features que sumen puntos del rubric, y planear el roadmap post-TP hacia un producto de calidad AAA.

Este documento est├í organizado en bloques. **Los primeros son los m├ís prioritarios para la entrega del TPI**; los siguientes son roadmap post-entrega y visi├│n a largo plazo.

> **Nota metodol├│gica:** cada tarea cita, cuando aplica, la secci├│n de `docs/references/consigna.txt` o el archivo de rulebook que la justifica. Las propuestas que **no** est├ín en consigna est├ín marcadas como `[OPCIONAL]` o `[POST-TP]`.

---

## TABLA DE CONTENIDOS

1. [TAREAS CORE (MVP y mejoras de arquitectura)](#1-tareas-core)
2. [AUDIT ÔÇö Lo que falt├│ verificar contra consigna y rulebook](#2-audit)
3. [ARQUITECTURA ÔÇö Tres niveles de ambici├│n](#3-arquitectura)
4. [IDEAS DE ECOSISTEMA Y MODOS DE JUEGO](#4-modos-de-juego)
5. [FEATURES PARA EL USUARIO FINAL (jugador)](#5-features-jugador)
6. [PANEL DE ADMINISTRADOR / BACKOFFICE](#6-panel-admin)
7. [FEATURES SOCIALES Y COMUNIDAD](#7-features-sociales)
8. [ACCESIBILIDAD Y QUALITY OF LIFE](#8-accesibilidad-y-qol)
9. [SEGURIDAD, FAIRNESS Y ANTI-CHEAT](#9-seguridad-y-anti-cheat)
10. [PRIORIZACI├ôN ÔÇö Qu├® meter antes de la entrega](#10-priorizacion-tpi)
11. [MAPEO RUBRIC ÔåÆ TAREAS (qu├® da puntos)](#11-mapeo-rubric)

---

## 1. TAREAS CORE

### 1.1. Refactorizaci├│n del Frontend ÔÇö `pokedex-page.ts` (Prioridad Alta)
- **Problema:** `pokedex-page.ts` tiene +1500 l├¡neas y maneja todo el estado local del WebSocket.
- **Tarea:** Dividir el monolito en componentes modulares siguiendo el patr├│n Smart/Dumb descripto en `docs/SKILLS/pokemon-frontend-strict.md`.
- **Plan de migraci├│n propuesto (5 sprints):**
  - Sprint 1 ÔÇö Crear `GameStateService` con signals y migrar el estado. El monolito ahora lee del service.
  - Sprint 2 ÔÇö Extraer componentes "hoja" sin interacci├│n: `DeckPileComponent`, `DiscardPileComponent`, `PrizeStackComponent`, `BattleLogComponent`.
  - Sprint 3 ÔÇö Extraer `HandComponent` y `PokemonSlotComponent` (componente reusable para Activo + cada slot de Banca).
  - Sprint 4 ÔÇö Extraer `ActionPanelComponent` y `TargetSelectorOverlay`. Las habilidades (B-04 del audit) viven ac├í.
  - Sprint 5 ÔÇö Limpiar `GameBoardComponent` para que quede solo orquestaci├│n STOMP + composici├│n. Objetivo < 200 l├¡neas.
- **Reglas inviolables (`pokemon-frontend-strict.md`):**
  - Solo Smart components inyectan servicios. Un Dumb que llama a WebSocket o REST = PR rechazado.
  - Prohibido `*ngIf`/`*ngFor`. Solo `@if`/`@for`/`@switch`.
  - Prohibido `.mutate()` en signals. Solo `.set()` y `.update()`.
  - Prohibido RxJS para estado local. RxJS solo para WebSocket/HTTP.
  - `NgOptimizedImage` obligatorio para im├ígenes de cartas.
  - Lazy loading estricto con `loadComponent` en el router.
  - Standalone components 100%.
- **Cita rubric:** RNF-02 (4pts "c├│digo limpio") + Frontend 15pts.

### 1.2. Tolerancia a Fallos y Reconexi├│n WS (Prioridad Alta)
- **Problema:** Si el WebSocket STOMP se cae, el usuario pierde el flujo.
- **Tarea:** Implementar reconexi├│n autom├ítica en el cliente y resincronizaci├│n de estado en el backend al reconectar sin romper la fase del turno.
- **Cita consigna:** RF-06 ÔÇö "Manejo robusto de reconexiones: si un cliente se desconecta, debe poder reconectarse y recibir el estado actualizado de la partida para continuar jugando".
- **Implementaci├│n recomendada:**
  - Endpoint REST auxiliar `GET /api/matches/{matchId}/state` con header `X-Player-Id` (ya recomendado en `docs/SDD/02_SessionAndWebSockets.md`).
  - Backoff exponencial en el cliente STOMP (1s ÔåÆ 2s ÔåÆ 4s ÔåÆ 8s, m├íx 30s).
  - Indicador visual de estado de conexi├│n (verde/amarillo/rojo) en la UI.
  - Abandon timer de 60s (par├ímetro `match.abandon.timeout-seconds` en `application.yml`). Cancelable al reconectar.

### 1.3. Dockerizaci├│n Completa (Prioridad Media)
- **Problema:** Solo la DB est├í en Docker. Backend y frontend se levantan a mano.
- **Tarea:** Crear `Dockerfile` para Backend (JDK 21) y Frontend (Node/Nginx). Orquestar todo en `docker-compose.yml`.
- **Bonus:** profile de docker-compose con un `pgadmin` y un `swagger-ui` para QA y evaluadores.

### 1.4. UI del Creador de Mazos / Deck Builder (Prioridad Media)
- **Problema:** La API de creaci├│n de mazos existe, pero el front usa un JSON hardcodeado.
- **Tarea:** Armar la interfaz visual en Angular con:
  - Grid paginado de cartas del set xy1 (146 cartas).
  - Filtros por tipo, supertipo, subtipo, HP, costo de retiro.
  - Drag & drop hacia la lista del mazo.
  - Validaci├│n visual en vivo (max 4 copias, max 1 ACE SPEC, al menos 1 B├ísico, 60 cartas exactas).
  - Mensajes de error accionables: "Te faltan 5 cartas para completar el mazo", "Ten├®s 5 copias de Charizard, m├íximo 4".
- **Cita consigna:** RF-04 completo.

### 1.5. Optimizaci├│n de Consultas N+1 (Prioridad Media)
- **Tarea:** Revisar `MatchRepository`, `DeckRepository`, `MatchActionRepository` y aplicar `@EntityGraph` o `JOIN FETCH` donde corresponda.
- **Cita rubric:** DB 10pts incluye "queries eficientes con ├¡ndices y sin problemas de N+1" (3pts directos).

### 1.6. Testing E2E (Prioridad Baja)
- **Tarea:** Tests `@SpringBootTest` cubriendo flujos completos. Playwright o Cypress para flujo FE b├ísico (crear mazo ÔåÆ unirse a partida ÔåÆ ejecutar un turno).
- **Cita rubric:** Testing 10pts incluye "Al menos un test E2E cubriendo el flujo b├ísico" (2pts).

---

## 2. AUDIT

Lo siguiente surgi├│ de comparar el estado actual del proyecto con la consigna y el rulebook. Son cosas concretas que **faltan** o est├ín **incompletas** respecto a los requerimientos.

### 2.1. Wiring de `logAction()` en `GameFacade` (CR├ìTICO)
- **Estado:** El log inmutable de acciones existe (Coworker 1 hizo `JpaGameStatePersistence`) pero `GameFacade.apply()` no llama a `gameStatePersistence.logAction()`.
- **Cita:** RF-05 ÔÇö "El registro de acciones (log) debe ser completo e inmutable: cada entrada debe indicar turno, jugador, tipo de acci├│n y resultado".
- **Acci├│n:** Agregar la llamada al final de cada `case` exitoso en `GameFacade.apply()`, **dentro del lock** (ADR-5).

### 2.2. Endpoint REST de rehidrataci├│n
- **Estado:** `docs/SDD/02_SessionAndWebSockets.md` lo cita como necesario para reconexi├│n. Verificar si existe.
- **Acci├│n:** Si no existe, `GET /api/matches/{matchId}/state` con header `X-Player-Id`, retorna `PlayerPerspectiveMapper.toView(session, playerIndex)`.

### 2.3. Broadcast con DTO serializado dentro del lock
- **Estado:** Verificar que `MatchService` construye el `GameStateView` *antes* de soltar el lock (ADR-5 dice "persist dentro del lock, broadcast fuera").
- **Riesgo:** Si la serializaci├│n pasa fuera del lock, dos acciones simult├íneas pueden producir broadcasts cruzados.

### 2.4. Blockers funcionales del engine (audit existente)
- **B-04:** `UseAbilityAction` no tiene resolver. RF-01b y RF-07 piden habilidades. Crear `AbilityEffectResolver` espejo del `TrainerEffectResolver`.
- **B-03 parcial:** Solo RED_CARD y TEAM_FLARE_GRUNT tienen efecto. Implementar al menos:
  - Objetos de robo: `PROFESSOR_SYCAMORE`, `SHAUNA`.
  - Objetos de b├║squeda: `TIERNO`, `POKEBALL`.
  - Objetos de recuperaci├│n: `SUPER_POTION`, `POTION`.
  - Estadios: `POKEMON_RESEARCH_LAB`.
  - Herramientas (attach + efectos continuos).

### 2.5. Warnings activos del audit (no cr├¡ticos pero suman calidad)
- **W-01:** Falta el paso 4 del pipeline de ataque ÔÇö efectos que cancelan el ataque (rulebook ┬º3).
- **W-04:** Diferenciar Energ├¡a Especial de Energ├¡a B├ísica con `EnergyKind { BASIC, SPECIAL }` (RF-02).
- **W-05:** Cambiar `boolean toolAttached` ÔåÆ `Optional<TrainerCard> attachedTool`.
- **W-NEW-02:** `validatePlaceBasicPokemon()` debe verificar que la carta sea de tipo BASIC (rulebook ┬º2).

### 2.6. Conflicto en versiones ÔÇö VERIFICAR
- **Estado:** `CLAUDE.md` dice "Spring Boot 4.0.0" pero consigna y SDDs dicen "Spring Boot 3.x".
- **Acci├│n:** Chequear `BE/pom.xml`. Si est├í en 4.x, downgrade a 3.x (tecnolog├¡a obligatoria). Si est├í en 3.x, corregir `CLAUDE.md`.

---

## 3. ARQUITECTURA ÔÇö Tres niveles de ambici├│n

La consigna pide cumplir RFs/RNFs. Para una demo del TP basta el m├¡nimo. Para ir m├ís all├í hay dos niveles m├ís altos. Elegir seg├║n tiempo restante.

### 3.A ÔÇö Opci├│n "Cumple consigna" (RECOMENDADA para entrega)
**Stack:** Spring Boot 3.x + STOMP `SimpleBroker` + `ReentrantLock` por `gameId` (ya existe) + Tomcat default + PostgreSQL s├¡ncrono dentro del lock.

**Por qu├®:** M├¡nimo c├│digo nuevo, cero infra adicional, cumple consigna al 100%. Soporta c├│modamente las 8-16 conexiones que tendr├ís en evaluaci├│n.

**Pros:** Defendible, simple, testeable.

**Contras:** No escala a m├ís de un nodo. **No te baja puntos del rubric.**

### 3.B ÔÇö Opci├│n "TP con calidad demostrable"
**Stack:** Opci├│n A + Virtual Threads (Java 21 ÔÇö Project Loom) en el executor del WebSocket + outbox liviano + abandon timer.

**Por qu├®:** Demuestra conocimiento del JDK moderno, justifica RNF-01, suma en la defensa oral.

**C├│mo:**
- En `WebSocketMessageBrokerConfigurer`, configurar `clientInboundChannel.taskExecutor()` con `Executors.newVirtualThreadPerTaskExecutor()`.
- Outbox: `BlockingQueue<LogEntry>` en memoria + `@Scheduled(fixedDelay=200ms)` que drena por batch. **Trade-off:** debilita durabilidad ante crash entre queue y flush.
- `ScheduledExecutorService` para abandon timer cancelable.

**Cita:** RNF-01 (200ms p99).

### 3.C ÔÇö Opci├│n "AAA / Long-term vision" [POST-TP]
**Stack:** Opci├│n B + RabbitMQ STOMP broker + consistent hashing por `gameId` + event sourcing como fuente de verdad + Redis para presence.

**No la recomiendo para el TP** ÔÇö overkill, no suma puntos, aumenta riesgo de no entregar.

**Para qu├® sirve mencionarla:** en la defensa oral como "visi├│n de escalabilidad". Para 10.000 partidas concurrentes ser├¡a el target.

---

## 4. MODOS DE JUEGO

### 4.1. Modos competitivos cl├ísicos (algunos ya listados antes)

**Ranked / Sistema de Ligas con MMR**
- Inspiraci├│n: Hearthstone, MTG Arena.
- Implementaci├│n: `RankingService` con algoritmo ELO cl├ísico (K-factor 30). Ligas: Bronce, Plata, Oro, Platino, Diamante, Maestro, Gran Maestro.
- Lobby cambia: matchmaking por MMR ┬▒ 100 puntos.
- Cita rubric: opcional "ranking o historial" (1pt).

**Torneos Suizo**
- `TournamentController`, l├│gica de emparejamiento por puntos acumulados, desempates por buchholz.
- Vista de bracket en el frontend.

**Modo Draft / Arena**
- Inspiraci├│n: Hearthstone Arena.
- Te presentan triples de opciones para armar un mazo de 30 cartas. Jug├ís hasta ganar 7 o perder 3.

### 4.2. Modos basados en el engine existente (BAJO COSTO t├®cnico)

**Aetherlog ÔÇö Replay Viewer**
- El log inmutable de RF-05 es 100% event-sourced. Construir un viewer es leer el log y aplicar snapshots con un slider de tiempo.
- Endpoint: `GET /api/matches/{matchId}/replay`.
- Componente: `<app-replay-viewer>` que reutiliza los componentes Dumb del tablero.
- **Refuerza Arquitectura del rubric** porque demuestra que el log es fuente de verdad real.

**Forge ÔÇö Puzzle del D├¡a**
- Estado inicial pre-fabricado (snapshot JSONB) + objetivo ("logra KO al activo este turno").
- El cliente env├¡a secuencia de acciones a `POST /api/puzzles/{id}/solve`. El backend instancia un `MatchSession` desde el snapshot, aplica acciones, compara resultado con el objetivo.
- Cero WebSockets necesarios. Sub-modo single-player.
- **Demuestra reutilizaci├│n del engine fuera del PvP.**

**Modo Espectador**
- Crear `SpectatorView` an├ílogo a `PlayerPerspectiveMapper.toView()` pero con AMBOS jugadores filtrados (manos como contadores, mazos como contadores, premios boca abajo).
- Canal STOMP `/topic/match/{matchId}/spectator`.
- Endpoint `GET /api/matches/active` listando partidas en curso.

**Spectator Predictions**
- Espectadores apuestan "Insight Points" (moneda cosm├®tica) sobre la pr├│xima jugada.
- Predicciones tipadas: "tomar├í el pr├│ximo premio antes del turno 8", "el ganador es el jugador A", "se descarta una energ├¡a especial".
- Leaderboard de Analystas.

### 4.3. Modos creativos basados en el rulebook XY1

**Ace Spec Showdown**
- Modo competitivo donde s├¡ o s├¡ incluis 1 AS T├üCTICO en el mazo, revelado antes de la partida.
- Aprovecha la validaci├│n "max 1 ACE SPEC" que ya existe.

**Mulligan Madness**
- Modo casual donde los mulligans dan bonus draws extra al oponente (2 en lugar de 1).
- Aprovecha `SetupManager` existente.

**Sudden Death Open**
- Torneo donde toda la primera ronda empieza directo en Sudden Death (1 premio).
- Partidas cortas, ideal para evaluaci├│n r├ípida en demo.
- Aprovecha `MatchCreationService.resetForSuddenDeath()`.

**Status Effect Roulette**
- Solo mazos centrados en condiciones especiales (Veneno/Quemado/Dormido).
- Estresa la l├│gica de coexistencia y exclusi├│n mutua.

### 4.4. Modos cooperativos / PvE

**Asymmetric Raid (2v1)**
- 2 jugadores cooperando contra un "Boss Pok├®mon" controlado por scripting determinista.
- Boss con 200 HP, dos turnos por ronda, ataques con efectos custom ("Inferno Pulse: 80 da├▒o en ├írea a Activo + 1 banca").
- Reutiliza el engine ÔÇö solo agregar un `BossActionStrategy` que reemplaza al `Player` adversario.
- Eventos semanales con bosses tem├íticos.

**Tag Team 2v2**
- 4 jugadores en dos equipos. Dos tableros que comparten zona compartida (Stadium).
- Cada jugador tiene su Activo y Banca. La energ├¡a de un jugador puede asignarse al Pok├®mon del otro.

**Gym Leader Conquest [POST-TP]**
- Career mode contra NPCs tem├íticos (L├¡deres de Gimnasio del XY).
- Cada L├¡der con su mazo y reglas especiales del gimnasio.
- Unlocks: cosm├®ticos al derrotar a cada L├¡der.

**Roguelike Run**
- Career single-player. Empez├ís con un pool m├¡nimo. Cada victoria suma cartas. La primera derrota termina la run.
- Sistema de "relics" (modificadores temporales): "Tu primer ataque del turno cuesta 1 energ├¡a menos", "Empez├ís cada turno con 1 carta extra".

### 4.5. Modos casuales / fun

**Speed Chess Mode**
- Cada turno tiene 30 segundos. Si no actu├ís, se pasa el turno autom├íticamente.
- Para jugadores expertos.

**No Mulligan Mode**
- Tu mano inicial es la que toca, sin oportunidad de re-shuffle aunque no tengas B├ísico.
- Reduce el setup pero introduce variance brutal.

**Mirror Match Challenge**
- Ambos jugadores usan el mismo mazo random.
- Mide pura habilidad de pilotaje.

**Themed Weekly**
- Solo cartas de cierto tipo permitidas esa semana ("solo Fuego y Agua").
- Refresca el meta semana a semana sin tocar cartas.

**King of the Hill**
- Hay una "silla" en el lobby. Quien la ocupa juega contra el siguiente challenger. Si pierde, sale.
- Streak m├ís larga = puntos extra de cosm├®ticos.

**Sealed Evolutionary [POST-TP]**
- Pool aleatorio que crece con victorias, bloqueado por arquetipo.
- Temporadas mensuales con reset.

### 4.6. Modos did├ícticos

**Tutorial Interactivo**
- Onboarding paso a paso: mover una carta, atacar, evolucionar, retirarse.
- Importante para retenci├│n de usuarios nuevos.

**Practice vs Bot**
- IA b├ísica con reglas heur├¡sticas (no ML, solo `if/else` con prioridades).
- Tres niveles: F├ícil (juega random v├ílido), Medio (prioriza ataques), Dif├¡cil (planifica 2 turnos adelante).
- Tambi├®n ├║til para QA ÔÇö testear flujos sin un humano del otro lado.

**Sandbox / Free Setup**
- Modo creador: configur├ís el tablero como quer├®s (estado de manos, banca, premios) y experiment├ís interacciones.
- ├Ütil para entrenar combos.

---

## 5. FEATURES PARA EL USUARIO FINAL (JUGADOR)

### 5.1. Perfil y personalizaci├│n

- **Avatar personalizable** ÔÇö biblioteca inicial de 20-30 sprites de Pok├®mon o trainers.
- **Frames de avatar** ÔÇö desbloqueables por logros.
- **T├¡tulos** ÔÇö debajo del nombre. Ej: "Maestro del Fuego", "Veterano de 100 partidas".
- **Banners de perfil** ÔÇö fondo decorativo.
- **Card sleeves** ÔÇö dise├▒os cosm├®ticos del dorso de las cartas.
- **Playmat / tablero** ÔÇö fondo del tablero personalizable.
- **Card backs animadas** ÔÇö cosm├®tico premium.
- **Insignias visibles** ÔÇö al lado del nombre durante la partida.

### 5.2. Sistema de progresi├│n por mazo / Pok├®mon

- **Mastery de Pok├®mon** ÔÇö cada Pok├®mon que us├ís sube de "nivel de uso". Es solo cosm├®tico (estrellitas al lado del nombre) pero motiva variar mazos.
- **Estad├¡sticas por mazo** ÔÇö win rate, partidas jugadas, oponentes derrotados con ese deck.
- **Logros por Pok├®mon** ÔÇö "Gana 10 partidas con Charizard", "KO 50 Pok├®mon con un solo ataque".

### 5.3. Colecci├│n y cat├ílogo

- **Card Viewer** ÔÇö cat├ílogo navegable de las 146 cartas con info detallada.
- **Search & filter** ÔÇö por tipo, supertipo, HP, costo, da├▒o m├íximo.
- **Favoritos** ÔÇö pin de cartas predilectas al top de b├║squedas.
- **Estad├¡sticas de uso** ÔÇö "Esta carta fue usada en el 47% de los mazos top esta semana".
- **Historial por carta** ÔÇö "Tu primer KO con esta carta fue el 2026-05-20".

### 5.4. Lista de amigos y matchmaking social

- **Friend list** con online/offline status.
- **Invite to play** ÔÇö invitaci├│n directa a partida privada.
- **Match history compartido** ÔÇö ver partidas pasadas contra un amigo.
- **Rivals** ÔÇö sistema autom├ítico que marca a oponentes con los que jugaste 5+ veces.
- **Recently played with** ÔÇö lista de ├║ltimos 10 oponentes para volver a invitar.

### 5.5. Historial y replays personales

- **Match history** filtrable por deck, oponente, resultado, fecha.
- **Replay sharing** ÔÇö link p├║blico a un replay espec├¡fico.
- **Highlights export** ÔÇö generar GIF de un momento puntual de la partida (KO ├®pico, premio decisivo).
- **Match notes** ÔÇö anotaciones personales despu├®s de la partida ("me equivoqu├® en el turno 4, deb├¡ retirar").
- **Win/loss stats por arquetipo** ÔÇö gr├íficos de torta del rendimiento por tipo de deck.

### 5.6. Notificaciones y alertas

- **Sistema de notificaciones in-app** ÔÇö turno comienza, oponente se desconect├│, alguien te desafi├│.
- **Preferencias granulares** ÔÇö activar/desactivar por tipo de evento.
- **Toast configurables** ÔÇö duraci├│n, posici├│n, sonido.
- **Notificaciones push** [POST-TP] ÔÇö torneo arranca, un amigo est├í online.

### 5.7. Pre-game y lobby

- **Quick deck swap** ÔÇö cambiar de mazo mientras esper├ís un match.
- **Deck preview** ÔÇö vista r├ípida de la composici├│n antes de empezar.
- **Match preferences** ÔÇö tipo de match, MMR, idioma del oponente, sin chat, etc.
- **"Listo" / "No listo"** ÔÇö sistema expl├¡cito para empezar la partida.
- **Coin flip animation** ÔÇö animaci├│n visible para determinar qui├®n empieza (consigna RF-01a "Se lanza una moneda").

### 5.8. Durante el partido

- **Auto-pass** ÔÇö pasar de fase autom├íticamente cuando no hay acciones posibles (ej. Pok├®mon dormido + sin banca).
- **Card preview on hover** ÔÇö ver la carta en grande sin clic.
- **Hand sort** ÔÇö por tipo, costo, nombre, fecha de robo.
- **Bench reorder** ÔÇö drag para reordenar (puramente visual, no afecta engine).
- **Quick stats overlay** ÔÇö da├▒o total infligido, energ├¡as unidas, premios tomados.
- **Confirm dialog** para acciones irreversibles ÔÇö "┬┐Atacar con X? Esto termina el turno".
- **Undo limitado** ÔÇö antes de confirmar acciones, una ventana para deshacer. **Una vez confirmada al backend, no hay vuelta atr├ís.**
- **Turn timer visible** ÔÇö countdown con cambio de color (verde ÔåÆ amarillo ÔåÆ rojo).
- **Phase indicator prominente** ÔÇö siempre claro en qu├® fase est├ís.
- **Action log scrolleable** ÔÇö historial completo de la partida, con filtros.
- **Emotes** ÔÇö biblioteca limitada de 10-15 emotes para evitar toxicidad. Mensajes pre-aprobados ("┬íBuena jugada!", "Lo lograste!").
- **Auto-GG** ÔÇö al final de la partida, bot├│n r├ípido para "Good Game" al oponente.

### 5.9. Post-partido

- **Resumen de partida** ÔÇö duraci├│n, turnos, acciones m├ís relevantes, MVP del mazo.
- **Compartir resultado** ÔÇö link copiado al portapapeles para mostrar el match.
- **Rate the opponent** ÔÇö sistema opcional de reputaci├│n (positiva/neutra/negativa).
- **Report player** ÔÇö flujo para denunciar comportamiento inapropiado.
- **Auto-rematch** ÔÇö bot├│n para volver a jugar contra el mismo oponente.

### 5.10. Tutorial y aprendizaje

- **Interactive onboarding** ÔÇö primera vez que entr├ís, 5-10 min de tutorial guiado.
- **In-game glossary** ÔÇö diccionario de t├®rminos accesible en cualquier momento (Pok├®mon-EX, ACE SPEC, mulligan, etc.).
- **Hint system** ÔÇö toggleable, para jugadores nuevos. Sugiere acciones obvias ("Ten├®s energ├¡as sin usar").
- **Replay-driven lessons** ÔÇö peque├▒os tutoriales con replays de jugadas did├ícticas.
- **Rules reference popup** ÔÇö modal con las reglas del rulebook XY1 accesible desde el men├║.

---

## 6. PANEL DE ADMINISTRADOR / BACKOFFICE

Ya est├í listado como tarea 12 en el doc original. Ac├í lo desarrollo en detalle.

### 6.1. Dashboard operativo

- **Partidas activas en vivo** ÔÇö contador real-time, lista con `matchId`, jugadores, fase actual.
- **Usuarios online** ÔÇö count y lista.
- **Latencia promedio** ÔÇö p50, p95, p99 de tiempo de respuesta a acciones.
- **GC pressure** ÔÇö m├®tricas del JVM expuestas v├¡a `/actuator/metrics` (Spring Boot Actuator).
- **DB connections** ÔÇö pool ocupado, en espera, m├íximo.
- **WebSocket sessions** ÔÇö count, conexiones por nodo.
- **Errores por minuto** ÔÇö log de excepciones del backend con b├║squeda.

### 6.2. Gesti├│n de usuarios

- **B├║squeda y filtrado** por nombre, email, fecha de registro, estado.
- **Ver perfil completo** ÔÇö partidas, mazos, estad├¡sticas, IPs usadas.
- **Ban/Suspend** con duraci├│n configurable (1 d├¡a, 7 d├¡as, permanente) + motivo.
- **Audit log** de acciones admin (qui├®n bane├│ a qui├®n, cu├índo, por qu├®).
- **Restablecer contrase├▒a** del usuario manualmente.
- **Resetear MMR** ÔÇö ├║til para corregir errores de ranking.
- **Otorgar cosm├®ticos** ÔÇö premios manuales por concursos, compensaci├│n por bugs.

### 6.3. Gesti├│n de partidas

- **Match audit viewer** ÔÇö leer el log inmutable de cualquier partida.
- **Force end** ÔÇö terminar una partida fantasma (jugadores desconectados sin abandon timer).
- **Override result** ÔÇö corregir manualmente un resultado disputado.
- **Refund prizes** ÔÇö devolver premios mal otorgados.

### 6.4. Gesti├│n de cartas y mazos

- **Cards database editor** ÔÇö corregir errores en el cach├® xy1 (errata, traducci├│n).
- **Reseed XY1 cards** ÔÇö bot├│n para repoblar desde la API si la tabla queda corrupta.
- **Card popularity dashboard** ÔÇö % de mazos que incluyen cada carta.
- **Win-rate by archetype** ÔÇö qu├® arquetipos est├ín dominando.
- **Most banned/reported decks** ÔÇö heur├¡stica para detectar combos abusivos.
- **Deck templates oficiales** ÔÇö administrar los 6 mazos tem├íticos seed.

### 6.5. Tournament management

- **Crear torneo** ÔÇö formato (suizo, single elim, double elim), fecha, premios, l├¡mite de jugadores.
- **Monitor en vivo** ÔÇö bracket, partidas en curso, standings.
- **Bye autom├ítico** y manual.
- **Disqualify player** con motivo.
- **Export resultados** ÔÇö CSV/JSON para registro hist├│rico.

### 6.6. Anti-cheat y fairness

- **Anti-cheat audit log** ÔÇö patrones sospechosos (acciones imposiblemente r├ípidas, mismas IPs en ambos lados de un match para boostear).
- **Network anomaly detection** ÔÇö desconexiones sistem├íticas durante turnos perdedores.
- **Win-rate threshold flags** ÔÇö usuarios con > 95% win rate se marcan para revisi├│n.
- **Replay analysis tool** ÔÇö admin puede ver cualquier replay con anotaciones.
- **Shadow ban** ÔÇö usuario sigue jugando contra otros shadow-banned (sin saberlo).

### 6.7. Comunicaci├│n con usuarios

- **Push announcements** ÔÇö banner global ("Mantenimiento en 2 horas").
- **Maintenance mode** ÔÇö toggle que cierra el lobby y muestra mensaje.
- **Email templates editor** ÔÇö bienvenida, recuperaci├│n de contrase├▒a, suspensi├│n.
- **Notification log** ÔÇö historial de todo lo enviado.
- **Newsletter manager** ÔÇö campa├▒as opt-in con stats de open rate.

### 6.8. Feature flags y configuraci├│n

- **Feature flags toggle** ÔÇö activar/desactivar modos sin redeploy.
- **A/B testing config** ÔÇö porcentaje de usuarios que ven feature X.
- **Rate limits override** ÔÇö por usuario o por endpoint.
- **System config** ÔÇö par├ímetros editables (abandon timeout, max conexiones, max partidas por usuario).

### 6.9. Backup y disaster recovery

- **Manual backup trigger** ÔÇö disparar dump completo.
- **Restore from backup** con flujo de confirmaci├│n.
- **Data export** ÔÇö dump de un usuario completo (GDPR-compliance).
- **Data deletion** ÔÇö wipe completo de un usuario (right to be forgotten).

### 6.10. Anal├¡tica de producto

- **DAU / WAU / MAU** ÔÇö usuarios activos diarios, semanales, mensuales.
- **Cohort retention** ÔÇö % de usuarios que vuelven al d├¡a 1, 7, 30.
- **Funnel** ÔÇö registro ÔåÆ primer mazo ÔåÆ primer match ÔåÆ primera victoria.
- **Tiempo promedio por partida** y distribuci├│n.
- **Heatmap de horarios** ÔÇö cu├índo se juega m├ís.
- **NPS in-app** ÔÇö encuesta opcional post-partida.

---

## 7. FEATURES SOCIALES Y COMUNIDAD

### 7.1. Clanes / Guilds [POST-TP]

- Crear/unirse/dejar clanes (hasta 50 miembros).
- **Clan chat** persistente.
- **Clan vs Clan tournament** ÔÇö competencias inter-clan semanales.
- **Roles internos** ÔÇö L├¡der, Oficial, Miembro.
- **Clan playmat/sleeves** ÔÇö cosm├®tico exclusivo del clan.

### 7.2. Sistema de mentor├¡a

- Jugadores experimentados (>500 partidas, win rate > 60%) opt-in como mentores.
- **Apprentice mode** ÔÇö newbies se emparejan con mentores en partidas casuales.
- **Anotaciones colaborativas** ÔÇö el mentor agrega comentarios sobre el replay del aprendiz.
- **Mentor rating** ÔÇö el aprendiz califica al mentor; mentores con buena reputaci├│n tienen badge especial.

### 7.3. Chat y mensajer├¡a

- **Chat global del lobby** [opcional, +2pts]
- **DMs entre amigos** ÔÇö historial persistente.
- **Channels por inter├®s** ÔÇö `#general`, `#deck-help`, `#tournaments`, `#bug-reports`.
- **Moderaci├│n** ÔÇö palabras prohibidas, sistema de reportes, autobans temporales.
- **Idioma preferido** ÔÇö matchmaking entre hablantes del mismo idioma cuando posible.

### 7.4. Eventos comunitarios

- **Eventos cronometrados** ÔÇö torneos semanales con premios cosm├®ticos.
- **Card of the Day** ÔÇö cartilla destacada con stats y trivia.
- **Weekly meta report** ÔÇö informe automatizado de los arquetipos top.
- **Community decklists** ÔÇö los mejores mazos del momento, votados por la comunidad.

### 7.5. UGC (User-Generated Content)

- **Deck guides** ÔÇö usuarios pueden publicar gu├¡as sobre sus mazos. Mazo + texto + replays anclados.
- **Replays destacados** ÔÇö votados por la comunidad, ranking semanal.
- **Comentarios en replays** ÔÇö discusi├│n as├¡ncrona sobre jugadas concretas.
- **Custom emote submissions** ÔÇö comunidad propone, mods aprueban.

### 7.6. Streaming integration [POST-TP]

- **Twitch overlay** ÔÇö mostrar tu rank, mazo actual, win streak en vivo.
- **Spectator-with-streamer mode** ÔÇö viewers pueden hacer predicciones de los matches en vivo.
- **Stream alerts** ÔÇö notificaci├│n in-game cuando alguien que sigues est├í streameando.

---

## 8. ACCESIBILIDAD Y QUALITY OF LIFE

### 8.1. Accesibilidad visual

- **Color-blind mode** ÔÇö paleta alternativa para tipos de Pok├®mon (que est├ín codificados por color).
- **Card text scaling** ÔÇö slider para agrandar texto en cartas.
- **High contrast mode** ÔÇö fondos m├ís oscuros, bordes m├ís fuertes.
- **Animation toggle** ÔÇö opci├│n de desactivar animaciones (mareo, epilepsia).
- **Reduced motion preference** ÔÇö respetar `prefers-reduced-motion` del SO.
- **Focus indicators visibles** ÔÇö outline de teclado claramente diferenciado.

### 8.2. Accesibilidad de input

- **Mouse-only mode** ÔÇö todas las acciones accesibles solo con mouse (sin atajos teclado).
- **Keyboard-only mode** ÔÇö atajos completos para power users.
- **Touch-optimized mode** ÔÇö controles m├ís grandes, hit areas amplios para tablet.
- **Drag & drop alternativo** ÔÇö bot├│n "Mover a..." para usuarios que no pueden drag.

### 8.3. Accesibilidad cognitiva

- **Modo lento** ÔÇö turn timer extendido para usuarios que necesitan m├ís tiempo.
- **Tutorial siempre accesible** ÔÇö no solo primera vez.
- **Confirmaciones expl├¡citas** ÔÇö para acciones irreversibles (atacar, descartar carta, terminar turno).
- **Resumen del turno del oponente** ÔÇö al empezar tu turno, breve resumen de lo que hizo el rival.

### 8.4. Internacionalizaci├│n (i18n)

- **Espa├▒ol (Argentina, Latam, Espa├▒a)** ÔÇö idioma base.
- **Ingl├®s** ÔÇö segundo idioma.
- **Portugu├®s (Brasil)** ÔÇö comunidad TCG grande en Brasil.
- **Card text** ya viene en ingl├®s desde la API; usar archivos `.json` de traducci├│n para campos cr├¡ticos del UI.

### 8.5. Audio

- **M├║sica de fondo** ÔÇö track relajante en lobby, m├ís intensa en partida.
- **Sound effects** ÔÇö flip de moneda, da├▒o aplicado, KO, robo de premio.
- **Volume sliders** ÔÇö m├║sica y FX independientes.
- **Mute por evento** ÔÇö sin audio para chat global, con audio para alertas de partida.

### 8.6. Performance y QoL t├®cnica

- **Quick reconnect** ÔÇö al reabrir la app, recuperar la ├║ltima partida si segu├¡a activa.
- **Multi-tab safe** ÔÇö detectar si abriste la partida en dos tabs, kickear una.
- **Offline mode parcial** ÔÇö Deck Builder puede usarse sin conexi├│n a Internet (solo cache local).
- **Bandwidth saver mode** ÔÇö im├ígenes en baja resoluci├│n para conexiones lentas.
- **Battery saver mode** ÔÇö animaciones reducidas en dispositivos con bater├¡a baja.

---

## 9. SEGURIDAD, FAIRNESS Y ANTI-CHEAT

### 9.1. Validaci├│n obligatoria

- **Toda acci├│n se valida en backend** (RNF-05). FE solo presenta.
- **Mano del oponente nunca al cliente** ÔÇö solo `handCount`. Si en alg├║n DTO aparece, se rechaza el PR.
- **Orden del mazo oculto** ÔÇö no exponerlo ni cifrado.
- **Cartas de Premio ocultas hasta tomarse** ÔÇö RF-01 + RNF-05.

### 9.2. Anti-cheat

- **Patrones de tiempo an├│malos** ÔÇö acciones a milisegundos imposibles para humanos.
- **Coincidencias de IP/dispositivo** ÔÇö detectar boosting (mismo jugador en ambos lados).
- **Desconexiones sospechosas** ÔÇö disconnect frecuente en momentos de derrota inminente.
- **Replay forensics** ÔÇö admin puede comparar varios replays buscando patrones.
- **Sandbox / Practice mode** ÔÇö separado del ranked. Cero impacto en MMR ni cosm├®ticos.

### 9.3. Privacidad

- **Eliminar cuenta** ÔÇö GDPR compliance. Wipe completo o anonymization (mantener stats agregadas, eliminar PII).
- **Export de datos** ÔÇö usuario puede pedir dump completo.
- **Visibilidad granular** ÔÇö partidas p├║blicas/privadas/solo amigos.
- **Block list** ÔÇö usuarios bloqueados no pueden invitarte ni verte online.

### 9.4. Seguridad de autenticaci├│n

- **JWT con refresh tokens** (opcional seg├║n RNF-05).
- **2FA** [POST-TP] ÔÇö TOTP (Google Authenticator).
- **Password strength meter** durante registro.
- **Rate limiting** en endpoints sensibles (login, registro, recuperaci├│n).
- **HTTPS obligatorio** en producci├│n.

---

## 10. PRIORIZACI├ôN ÔÇö Qu├® meter antes de la entrega

Si ten├®s que elegir qu├® hacer en los d├¡as que quedan, este es el orden ├│ptimo de ROI (puntos / esfuerzo):

### Tier 1 ÔÇö IMPRESCINDIBLE (riesgo de no aprobar)

1. **Wirear `logAction()` en `GameFacade`** ÔÇö RF-05 expl├¡cito.
2. **Endpoint REST de rehidrataci├│n** + reconexi├│n robusta ÔÇö RF-06.
3. **Refactor m├¡nimo de `pokedex-page.ts`** + drag & drop con CDK ÔÇö RF-07 + rubric FE 15pts.
4. **B-04 (abilities) + B-03 (m├ís trainer effects)** ÔÇö RF-01b "usar Habilidades", RF-02 tipos de Trainer.
5. **N+1 queries fix** ÔÇö rubric DB 3pts directos.
6. **Animaciones para condiciones especiales (rotaciones)** ÔÇö RF-07 literal.

### Tier 2 ÔÇö ALTO ROI (suma puntos opcionales)

7. **6 mazos tem├íticos seed** ÔÇö opcional 10pts.
8. **Replay viewer b├ísico** ÔÇö refuerza Arquitectura y FE.
9. **Chat in-game** ÔÇö opcional 2pts.
10. **Animaciones de ataque/KO/evoluci├│n** ÔÇö opcional 2pts.
11. **Ranking b├ísico + historial** ÔÇö opcional 1pt.

### Tier 3 ÔÇö NICE TO HAVE (defensa oral)

12. **Megaevoluci├│n** ÔÇö RF-02 opcional.
13. **Modo Espectador** ÔÇö refuerza Fog of War (RNF-05).
14. **Modo Practice vs Bot** ÔÇö ├║til para QA y demos.
15. **Admin dashboard m├¡nimo** ÔÇö operativo para evaluadores.

### Tier 4 ÔÇö POST-TPI (roadmap)

Todo lo dem├ís de las secciones 4-9.

---

## 11. MAPEO RUBRIC ÔåÆ TAREAS

Cada item del rubric con las tareas que lo cubren.

### Funcionalidad ÔÇö 40pts

| Criterio | Pts | Tareas que lo cubren |
|---|---|---|
| Reglas RF-01 a RF-07 correctas | 10 | Wiring logAction (2.1), B-04 abilities, B-03 trainers, W-01 cancel attack, validate place basic |
| Partida completa jugable | 10 | Toda la cadena del Tier 1 |
| Validaciones backend correctas | 10 | RuleValidator + tests (audit ya tiene 13 puntos validados) |
| Condiciones especiales correctas | 5 | StatusEffectManager (audit OK) + animaciones de rotaci├│n |
| C├ílculo de da├▒o correcto | 5 | DamageCalculator (audit OK) |

### Arquitectura ÔÇö 25pts

| Criterio | Pts | Tareas |
|---|---|---|
| Separaci├│n Controller/Service/Repository/Engine | 6 | Ya est├í. Mantener Fix de SimpleBroker no romperlo |
| Engine con componentes independientes | 6 | Audit ya valida aislamiento Spring. Reforzar tests JaCoCo 90% |
| Patrones de dise├▒o aplicados | 5 | State + Strategy + CoR + Observer + Repo + Facade (todos presentes) |
| C├│digo limpio | 4 | Refactor pokedex-page.ts cumple esto en FE |
| Manejo de errores robusto | 4 | Fail Fast del engine + exception handlers de Spring |

### Base de Datos ÔÇö 10pts

| Criterio | Pts | Tareas |
|---|---|---|
| Modelo de datos correcto | 5 | Flyway schema con matches, match_actions, snapshots JSONB |
| Queries eficientes sin N+1 | 3 | Tarea 1.5 (JOIN FETCH, EntityGraph) |
| Constraints y validaciones DB | 2 | Foreign keys, NOT NULL, ├¡ndices |

### Frontend ÔÇö 15pts

| Criterio | Pts | Tareas |
|---|---|---|
| Drag & drop, panel de acciones, feedback visual | 6 | CDK DragDrop + ActionPanel + rotaciones |
| Estado sincronizado v├¡a WebSocket | 5 | GameStateService con signals + STOMP |
| Dise├▒o claro | 2 | Componentizaci├│n por zonas (RF-07) |
| Funciona en desktop + tablet | 2 | Tailwind responsive + touch-optimized |

### Testing ÔÇö 10pts

| Criterio | Pts | Tareas |
|---|---|---|
| Cobertura ÔëÑ80% global y ÔëÑ90% en cr├¡ticos | 5 | JaCoCo configurado en pom.xml |
| Tests de integraci├│n | 3 | `@SpringBootTest` para partida completa, mulligan, evoluci├│n, KO, victoria |
| Test E2E b├ísico | 2 | Playwright/Cypress para crear mazo + match + un turno |

### Opcionales ÔÇö +15pts bonus

| Opcional | Pts | Tareas |
|---|---|---|
| Mazo tem├ítico seed funcional | 10 | 6 mazos en `V3__seed_themed_decks.sql` |
| Animaciones FE | 2 | Angular Animations API |
| Ranking / historial | 1 | RankingService + endpoints GET |
| Chat in-game | 2 | ChatWebSocketController + componente FE |

### Bonus sin puntos cuantificados

- Megaevoluci├│n (RF-02).
- Expansiones extra al xy1 (RF-04).

---

## CONVENCIONES DE TRABAJO

- **GitFlow:** ramas `feature/BE-<ticket>` o `feature/FE-<ticket>` ÔåÆ PR a `develop`. Nunca push directo a `main`.
- **Conventional Commits.** Sin co-authored-by ni atribuci├│n a IA.
- **Audit obligatorio** (`/audit`) antes de cada commit a `develop`. Si hay blockers ­ƒÜ¿, no commitear.
- **Tests primero** ÔÇö no escribir l├│gica nueva sin un test rojo que la justifique (TDD estricto en engine).
- **Documentaci├│n viva** ÔÇö actualizar `docs/` cuando un m├│dulo cambie significativamente.

---

## CITAS DE REFERENCIA

Todas las recomendaciones de este documento est├ín justificadas por:

- **Consigna:** `docs/references/consigna.txt`
- **Rulebook XY1 (resumen algor├¡tmico):** `docs/SKILLS/game-rules-reference.md`
- **Rulebook XY1 completo:** `docs/references/rulebook.txt` y `docs/references/xy1-rulebook-es (1).pdf`
- **Arquitectura:** `docs/ARCHITECTURE.md`
- **SDDs por m├│dulo:** `docs/SDD/01_GameEngine.md` a `06_ApiContracts.md`
- **Skills estrictas:** `docs/SKILLS/pokemon-engine-tdd.md`, `pokemon-frontend-strict.md`, `pokemon-websockets-strict.md`, `pokemon-persistence-strict.md`, `pokemon-rulebook-auditor.md`
- **API externa:** `docs/references/pokemontcg-api-reference.md`
- **Audit previo del engine:** `docs/AUDIT_ENGINE.md`
- **Plan de implementaci├│n detallado:** `docs/IMPLEMENTATION_PLAN.md`
- **Plan de acci├│n inmediato:** `docs/ACTION_PLAN.md`
- **Gu├¡as de repositorio:** `docs/REPO_GUIDELINES.md`

**Nota final:** este documento es **vivo**. Si encontr├ís errores, contradicciones con la consigna, o features que se nos pasaron, actualizalo directamente y mencion├í el cambio en el commit. La idea es que sea el primer lugar al que cualquier coworker mira para entender el roadmap.

┬í├ëxitos equipo!
