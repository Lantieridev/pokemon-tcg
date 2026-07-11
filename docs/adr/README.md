# Architecture Decision Records

Registro formal de decisiones de arquitectura del TPI. El proyecto ya tenía análisis de este tipo dispersos en `BE/docs/rankings_and_history/` — esta carpeta los centraliza y les da un índice, sin mover ni romper links a los documentos originales.

| # | Título | Estado |
|---|---|---|
| [0001](./0001-hexagonal-engine-isolation.md) | Motor de juego aislado con Clean/Hexagonal Architecture | Aceptada |
| [0002](./0002-server-authoritative-websocket-sync.md) | Sincronización server-authoritative vía STOMP/WebSockets | Aceptada |
| [0003](../../BE/docs/rankings_and_history/diseno_persistencia_ganador.md) | Persistencia atómica del ganador en una única transacción | Aceptada (documento preexistente) |

## Cuándo agregar un ADR nuevo
Cuando se tome una decisión de arquitectura que otro integrante del equipo podría cuestionar o revertir sin este contexto — no hace falta uno por cada PR. Seguir el mismo formato: Contexto → Decisión → Consecuencias.
