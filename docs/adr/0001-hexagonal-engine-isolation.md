# 0001 — Motor de juego aislado con Clean/Hexagonal Architecture

## Estado
Aceptada

## Contexto
El motor de reglas del TCG (`ar.edu.utn.frc.tup.piii.engine`) tiene lógica compleja y crítica (turnos, daño, KOs, condiciones de victoria) que necesita testearse exhaustivamente y rápido (`strict_tdd: true`, `coverage_target: 90%` en `openspec/sdd-init.yaml`). Si el motor dependiera de Spring, JPA o WebSockets, cada test unitario requeriría levantar contexto de Spring o mocks pesados, y el equipo no podría iterar reglas de juego a la velocidad que necesita un TPI con fecha de entrega.

## Decisión
El motor es 100% POJOs puros, sin conocer:
- Base de datos (ninguna dependencia a JPA/Hibernate).
- API REST ni WebSockets.
- Librerías externas fuera de Java 21 estándar.

Se comunica con el resto del sistema exclusivamente a través de interfaces "Provider" (`BattlefieldStateProvider`, `BenchStateProvider`, `DeckStateProvider`, `PrizeStateProvider`) que actúan como puertos (Ports) en el sentido de Clean/Hexagonal Architecture — el motor solo lee a través de ellos, nunca conoce su implementación real. La capa de persistencia (JPA) implementa esos puertos en producción; los tests usan fakes.

`openspec/sdd-init.yaml` codifica esto como regla dura: `engine_isolation: true`, y `testing_constraints` prohíbe explícitamente contexto de Spring en tests del engine.

## Consecuencias
- Los tests del motor corren en milisegundos, sin Spring context — esto es lo que hace viable `strict_tdd` con `coverage_target: 90%` en el tiempo de un TPI.
- Cualquier PR que agregue un `import` de Spring/JPA/WebSocket dentro de `engine/` rompe esta regla — vale la pena que el reviewer lo chequee explícitamente, Checkstyle/PMD no lo detectan automáticamente.
- Agregar un dato nuevo que el motor necesite leer (ej. un nuevo efecto de estado) requiere sumar o extender un Provider, no agregar una dependencia directa a persistencia.
