# Estrategia de Testing

Qué cubre cada capa de testing en este repo, y qué NO está cubierto todavía — para que los huecos sean una decisión consciente, no algo invisible.

## Backend (`BE/`)

- **Unit + integración (JUnit 5 + Mockito):** 1.181 tests a la fecha (`./mvnw test`). Cubre el engine (reglas, efectos, pipeline de ataque), servicios de aplicación (matchmaking, deck builder, logros, economía) y la capa de persistencia.
- **Gate de cobertura (JaCoCo, en `BE/pom.xml`):** 3 niveles, no uno solo:
  - 90% línea + 85% rama, escopeado a `engine/**`.
  - 90% instrucción, escopeado a 3 clases específicas: `RuleValidator`, `DamageCalculator`, `StatusEffectManager`.
  - 75% instrucción, piso agregado para todo el proyecto.
- **Regla del engine:** tests de `engine/` no levantan contexto de Spring — son POJOs puros, para que corran rápido y no acoplen la lógica de reglas a JPA/WebSockets.
- **Checkstyle + PMD:** corren en `mvn verify`. PMD tiene reglas `bestpractices`/`errorprone` (se persiguen a 0, casi siempre bugs reales) y reglas `design` (heurísticas de complejidad — se evalúan caso a caso, no se persiguen ciegamente a 0).

## Frontend (`FE/`)

- **Unit (Jasmine + Karma):** 476 tests a la fecha (`ng test --code-coverage`). Gate de cobertura en CI.
- **E2E (Cypress):** 3 specs, 7 tests — cubren **3 de 11 áreas de feature**:
  - ✅ `auth.cy.ts` — registro + login.
  - ✅ `deck-builder.cy.ts` — construcción de mazo de 60 cartas vía autocompletar + guardado.
  - ✅ `bot-battle.cy.ts` — partida vs. bot desde el lobby hasta tablero interactivo.
  - ❌ **Sin E2E:** batalla en vivo PvP, campaña, tienda, sobres, pase de batalla, social/chat/amigos, perfil, ranking, simulador. La más notable: la pantalla de batalla en vivo (`battle.component.ts`, 1.515 líneas) no tiene ningún E2E, solo unit tests del componente.

## Cómo correr todo

```bash
# Backend
cd BE && ./mvnw clean verify

# Frontend
cd FE && npm test
cd FE && npm run cypress:run
```

## Próximos pasos (no atacados en esta pasada)

- Sumar al menos 1 spec E2E para el flujo de batalla PvP en vivo (el hueco de mayor riesgo).
- Evaluar mutation testing (Pitest para BE, Stryker para FE) — decidido como backlog en la campaña de PMD: perseguir 100% de cobertura literal es un antipatrón, pero cobertura alta sin verificar que los tests realmente detectan bugs tampoco alcanza.
