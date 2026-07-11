# 0002 — Sincronización server-authoritative vía STOMP/WebSockets

## Estado
Aceptada

## Contexto
Pokémon TCG es un juego por turnos con estado compartido entre dos jugadores en tiempo real. Si el frontend calculara el resultado de una acción (daño, KO, cambio de fase) localmente y luego lo sincronizara, un cliente modificado o con un bug de cálculo podría hacer trampa o desincronizar la partida entre ambos jugadores — inaceptable incluso en un TPI donde la corrección del motor es lo que se evalúa.

## Decisión
Arquitectura **server-authoritative** de punta a punta (ver `docs/01_arquitectura_y_flujo.md` para el detalle paso a paso):
- El frontend (Angular + Signals) **nunca** muta el estado del juego localmente — solo emite intenciones (`stompClient.send("/app/match/{matchId}/action", ...)`).
- Todo el cálculo (validación de reglas, daño, KOs, cambio de turno) ocurre en el backend, protegido por un `ReentrantLock` por `matchId` (`MatchSessionRegistry`) para serializar acciones concurrentes sobre la misma partida.
- El backend hace *broadcast* de una `GameStateView` — una vista sanitizada que oculta las cartas del oponente — a `/topic/match/{matchId}`, y todos los clientes (incluido el que originó la acción) actualizan su estado a partir de esa respuesta, no de un cálculo propio.

## Consecuencias
- Ningún cliente puede alterar el resultado de una acción de juego — el servidor es la única fuente de verdad, lo cual también simplifica el frontend (solo renderiza el estado que recibe, patrón Smart/Dumb components).
- El `ReentrantLock` por partida es un cuello de botella deliberado y aceptado: dentro del lock no puede haber I/O lento (ya documentado como advertencia para agentes de IA en `01_arquitectura_y_flujo.md`) — cualquier feature nueva que toque el Game Engine tiene que respetar ese bloque `try/finally`.
- Escalar a múltiples instancias del backend requeriría un mecanismo de lock distribuido (hoy es un `ReentrantLock` en memoria, válido para una sola instancia) — no es un problema actual, pero es la primera cosa a revisar si el TPI necesitara escalar horizontalmente.
