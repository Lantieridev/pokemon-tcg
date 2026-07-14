# Índice de `docs/`

Este directorio mezcla documentación técnica activa, el planeamiento original del TPI grupal (UTN FRC) y material de referencia. Esta guía existe para saber **cuál leer para qué** — varios archivos tienen alcance parecido pero propósitos distintos.

## Arquitectura — cuál leer primero

Hay dos documentos de arquitectura con nombres parecidos, cada uno con un propósito distinto:

- **[`01_arquitectura_y_flujo.md`](01_arquitectura_y_flujo.md)** — el que hay que leer primero. Está escrito específicamente para que un agente de IA entienda el stack, las reglas de inyección de dependencias y las convenciones antes de tocar código.
- **[`ARCHITECTURE.md`](ARCHITECTURE.md)** — mapa mental del *motor* de juego en sí (Hexagonal, `TurnManager`, patrón Observer). Léelo cuando trabajes específicamente en `engine/`.
- **[`02_sistemas_core.md`](02_sistemas_core.md)** — dónde vive cada sistema (Game Engine, condiciones de victoria, etc.) en el código real.
- **[`03_guia_desarrollo_y_setup.md`](03_guia_desarrollo_y_setup.md)** — cómo levantar el proyecto localmente (prerrequisitos, build, tests).

## Decisiones de arquitectura (ADRs)

- **[`adr/README.md`](adr/README.md)** — índice de las decisiones formales, incluye una que vive en `BE/docs/rankings_and_history/` sin haberse movido.
- [`adr/0001-hexagonal-engine-isolation.md`](adr/0001-hexagonal-engine-isolation.md) — por qué el motor no depende de Spring/JPA/WebSockets.
- [`adr/0002-server-authoritative-websocket-sync.md`](adr/0002-server-authoritative-websocket-sync.md) — por qué el cliente nunca calcula el resultado de una jugada.

## Planeamiento original del TPI (`SDD/`)

Documentos de reparto de módulos del trabajo práctico grupal original (asignación de responsables, objetivos por módulo). Útiles como contexto histórico de por qué el proyecto está dividido así, pero **desactualizados** en cuanto a "Responsables" (ya no aplica, el repo en esta cuenta es propiedad individual) y en algunos detalles de stack (ver `01_arquitectura_y_flujo.md` para el estado real).

- [`SDD/01_GameEngine.md`](SDD/01_GameEngine.md) — motor de reglas.
- [`SDD/02_SessionAndWebSockets.md`](SDD/02_SessionAndWebSockets.md) — sesión de partida y WebSockets.
- [`SDD/03_DeckBuilderAPI.md`](SDD/03_DeckBuilderAPI.md) — integración con `pokemontcg.io` y construcción de mazos.
- [`SDD/04_Persistence.md`](SDD/04_Persistence.md) — modelo de datos y persistencia.
- [`SDD/05_Frontend.md`](SDD/05_Frontend.md) — Angular, componentes smart/dumb.
- [`SDD/06_ApiContracts.md`](SDD/06_ApiContracts.md) — contratos JSON de los mensajes por WebSocket.

## Guías para agentes de IA (`SKILLS/`)

Instrucciones obligatorias por módulo, pensadas para que un agente de IA las siga al tocar esa parte del código.

- [`SKILLS/game-rules-reference.md`](SKILLS/game-rules-reference.md) — solo la lógica dura del reglamento XY1 (fuente de verdad algorítmica, sin lore ni marketing).
- [`SKILLS/pokemon-engine-tdd.md`](SKILLS/pokemon-engine-tdd.md) — TDD estricto para el Game Engine.
- [`SKILLS/pokemon-frontend-strict.md`](SKILLS/pokemon-frontend-strict.md) — convenciones de Angular/Signals/Smart-Dumb.
- [`SKILLS/pokemon-persistence-strict.md`](SKILLS/pokemon-persistence-strict.md) — convenciones de JPA/PostgreSQL.
- [`SKILLS/pokemon-websockets-strict.md`](SKILLS/pokemon-websockets-strict.md) — convenciones de sesión y STOMP.
- [`SKILLS/pokemon-rulebook-auditor.md`](SKILLS/pokemon-rulebook-auditor.md) — cómo auditar que el código cumple el reglamento.

## Features específicas

- [`ACHIEVEMENTS_TECH_DOC.md`](ACHIEVEMENTS_TECH_DOC.md) — sistema de logros y títulos de entrenador.
- [`DECK_BUILDING_SYSTEM_DOCS.md`](DECK_BUILDING_SYSTEM_DOCS.md) — construcción/autocompletado de mazos.

## Diseño visual — dos documentos, dos momentos distintos

- **[`design-guidelines.md`](design-guidelines.md)** — el sistema de diseño **actual e implementado** (glassmorphism, tema oscuro, tokens en `FE/src/styles-theme.css`). Es el que refleja el código real hoy.
- **[`DESIGN_GUIDELINES.md`](DESIGN_GUIDELINES.md)** — visión **aspiracional** para un rediseño total ("Monstra TCG", estética AAA tipo Hearthstone/MTG Arena). Todavía no implementado — no asumas que algo descripto acá ya existe en el código.

## Convenciones de repo

- [`REPO_GUIDELINES.md`](REPO_GUIDELINES.md) — reglas para trabajar en equipo (límites de arquitectura, testing obligatorio, convenciones de commits). Escrito para el equipo original de 5 personas del TPI; en esta cuenta personal el repo tiene autoridad total, pero las reglas técnicas (Strategy pattern en vez de if-chains gigantes, no mezclar el engine con Spring/WebSockets/DB) siguen aplicando.

## Material de referencia (`references/`)

Insumos externos, no documentación propia — no editar salvo que la fuente original cambie.

- [`references/rulebook.txt`](references/rulebook.txt) y [`references/xy1-rulebook-es.pdf`](references/xy1-rulebook-es.pdf) — reglamento oficial XY1 (texto plano y PDF original).
- [`references/pokemontcg-api-reference.md`](references/pokemontcg-api-reference.md) — referencia de la API externa `pokemontcg.io` v2, generada con datos reales del set XY1.
- [`references/Consigna detallada.pdf`](references/Consigna%20detallada.pdf) y [`references/consigna.txt`](references/consigna.txt) — consigna original del TPI universitario.
- [`references/Tablero_referencia.png`](references/Tablero_referencia.png) — imagen de referencia del tablero físico.

## Documentación específica del backend

`BE/docs/` tiene su propio set de documentos (arquitectura del motor en más detalle, contratos de API para agentes externos, historial de auditorías en `rankings_and_history/`, y un sitio Docsify en `app_doc/`) — fuera del alcance de este índice, pero relevante si estás trabajando puntualmente en `BE/`.
