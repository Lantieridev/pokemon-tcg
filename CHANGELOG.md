# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/). Este repo no usa versionado semántico con tags (proyecto personal, deploy continuo a `main`) — las entradas se agrupan por fecha en vez de por versión.

Retroactivo desde que el repo se independizó del TPI grupal (2026-07-11). Historia previa a esa fecha: ver `docs/SDD/` (planificación original) y el primer commit de este repo.

## [No publicado]

### Added
- `CONTRIBUTING.md`, `CODEOWNERS`, `LICENSE` (MIT), este `CHANGELOG.md`.
- `docs/README.md` — índice de la carpeta `docs/`.
- Sección de convenciones de código migrada a `docs/01_arquitectura_y_flujo.md` (reemplaza `docs/REPO_GUIDELINES.md`, eliminado).

### Fixed
- README raíz: eliminada referencia colgante a un roadmap inexistente (`docs/04_roadmap_y_pendientes.md`).
- `docs/01_arquitectura_y_flujo.md`: versiones de stack corregidas (Spring Boot 4.0.0, Angular 21+), entry-point real de STOMP corregido (`GameWebSocketController.handleAction`), regla de Checkstyle inventada eliminada.
- `docs/SKILLS/game-rules-reference.md`: agregado el efecto de estado faltante (Precisión Baja).
- `BE/docs/03_ENGINE_INTEGRATION_CONTRACT.md` (ex `03_COWORKER_API.md`): reescrito — el ejemplo de código anterior no compilaba contra el código real.
- Metadata de Swagger (`title`/`description`/`contact`) corregida — quedaba con placeholders de Maven sin resolver.
- `openspec/sdd-init.yaml`: atribución de patrones de diseño corregida (Chain of Responsibility es `engine/pipeline/`, no `DamageCalculator`; se agregó Observer; se sacaron Template Method y Command, que no están genuinamente implementados), agregado Spring Security/JWT y PostgreSQL al stack declarado.

### Removed
- `docs/REPO_GUIDELINES.md` — reglas de flujo de trabajo para el equipo de 5 del TPI original, ya no aplican.
- Regla de identidad de Git académica en `docs/03_guia_desarrollo_y_setup.md` y en `application.properties` (contacto placeholder `tuemail@utn.frc.edu.ar`).

## 2026-07-14

### Changed
- Angular actualizado de v20.3 a v21.2 (`ng update`) — 15 componentes/templates migrados a sintaxis nativa `@if`/`@for`. Cierra 13 alertas de Dependabot ancladas a `@angular/build`.
- PMD: `StatusEffectManager` (campo que tapaba a un método público, renombrado) y `AsyncPersistenceListener` (bug de overflow en `Math.abs(Integer.MIN_VALUE)`, corregido).

## 2026-07-13

### Added
- Split de `MatchService.processAction`/`abandonMatch` en pasos nombrados, con tests nuevos de concurrencia real sobre el lock de ADR-5 (no existían antes).
- Re-auditoría independiente de toda la campaña de PMD (4 lentes en paralelo) — 8 hallazgos reales corregidos.

### Fixed
- 2 bugs de datos de juego preexistentes: `CampaignService` usaba Rainbow Energy en vez de Fire Energy en mazos "Fuego"; IDs de Professor Sycamore/Professor's Letter invertidos en `DeckAssistantService`.
- `DeckAssistantService.autocomplete()` no-determinístico (`Random` sin seed) — determinismo restaurado vía inyección de `Random` para tests.
- PMD: `DeckAssistantService` (37→4), `PenaltyServiceImpl` (32→4), y ~10 archivos dispersos más (649→482 violaciones).

### Changed
- Rename completo del codename "aurora" en todo el FE (selectores, clases, CSS, docs).

## 2026-07-12

### Fixed
- PMD corría silenciosamente en la versión vieja (6.55, no la declarada 7.0.0-rc3) — nunca veía switches con pattern-matching de Java 21 en `MatchService`/`GameFacade`.
- `GET /api/decks/user/{userId}` no existía en el backend (404 real) — rompía carga de mazos en lobby/campaña/deck-builder/perfil. Apuntado a `/api/decks/mine`.
- Botón "Autocompletar" del deck builder no esperaba la carga del catálogo de cartas.
- `DeckAssistantService.autocomplete()` armaba mazos de 60 cartas sin ningún Pokémon al empezar de cero.

### Added
- Gate de cobertura FE real en CI (35%→60% piso, tras testear 18 componentes sin tests).
- Suite de Cypress E2E reemplazada (de 1 smoke test con la aserción comentada, a 3 specs/7 tests reales contra el stack completo).

### Changed
- 26 archivos de FE con URL de API hardcodeada consolidados en `environment.ts`.

## 2026-07-11

### Added
- CI por primera vez (BE + FE) — el repo nunca había tenido pipeline pese a exigir `strict_tdd`/90% coverage en su propio `openspec`.
- 2 ADRs nuevos (aislamiento hexagonal del engine, sync server-authoritative vía STOMP) + diagrama de secuencia Mermaid.

### Fixed
- Build de FE completamente roto en `main` desde un rename sin actualizar 11 imports — 0 tests corriendo a 77/89 pasando.
- 17 de 30 alertas de Dependabot parcheadas (`npm audit fix` no-breaking).

### Changed
- Repo renombrado de `pokemon-tcg-tpi` a `pokemon-tcg`, independizado como proyecto de portfolio personal.
