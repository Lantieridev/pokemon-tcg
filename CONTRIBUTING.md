# Contribuir

Este repo empezó como un TPI grupal de UTN FRC y hoy es propiedad individual de [@Lantieridev](https://github.com/Lantieridev), con autoridad total sobre `main`. No hay proceso de revisión de equipo ni ramas obligatorias — este documento describe la convención real que se sigue, no un proceso formal para colaboradores externos.

## Commits

[Conventional Commits](https://www.conventionalcommits.org/): `tipo(ámbito): descripción corta`.

Tipos usados en este repo: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`.

El mensaje explica el **por qué**, no el qué — el diff ya muestra el qué. Ejemplos reales de este repo:

```
fix(persistence): avoid overflow in AsyncPersistenceListener id hashing and narrow caught exception
refactor(engine): resolve PMD field-name-shadowing and dead field in StatusEffectManager
docs: add docs/ index and drop duplicate-download suffix from rulebook PDF
```

## Unidad de commit

Un commit = una unidad de trabajo con un propósito claro (un fix, un refactor, una feature). No se commitea por tipo de archivo ("modelos", después "servicios", después "tests") — el test de una conducta vive en el mismo commit que esa conducta.

## Branching

Cambios chicos van directo a `main`. Para cambios grandes (una feature nueva, una migración), rama de corta vida (`feature/*`, `fix/*`) que se mergea a `main` cuando está lista — sin PR obligatorio, sin revisor externo (no hay a quién asignar).

## Antes de commitear

- Correr la suite completa relevante: `./mvnw clean verify` para cambios de backend, `npm test` / `ng test` para frontend.
- Si tocaste `BE/`, revisar que `BE/docs/api_doc/swagger.json` no se haya regenerado como efecto secundario no deseado (`git checkout -- BE/docs/api_doc/swagger.json` si no estabas tocando la API).
- Checkstyle y PMD corren en `mvn verify` — las reglas de `design` (complejidad, God Class, etc.) son heurísticas, no leyes: usar criterio antes de forzar un refactor que empeora la legibilidad para bajar un número.

## Identidad de Git

Los commits deben ir con `lantieridev@users.noreply.github.com` (GitHub rechaza pushes que expondrían el email personal real vía la protección "would publish a private email" — ver `git config user.email` local del repo).

## Convenciones técnicas

Ver la sección 5 de [`docs/01_arquitectura_y_flujo.md`](docs/01_arquitectura_y_flujo.md#5-convenciones-de-código) para las convenciones de código (Strategy pattern, aislamiento del engine, Angular Signals/Standalone).
