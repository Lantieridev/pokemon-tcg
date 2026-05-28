# 📋 Tareas del TPI - Backlog Prioritario

- `[ ]` uncompleted tasks
- `[/]` in progress tasks
- `[x]` completed tasks

Este listado agrupa el trabajo priorizado de los desarrolladores backend, enfocado en cubrir los requerimientos obligatorios de la consigna oficial antes de integrar todo con la interfaz final en Angular.

## 🚀 Prioridad Inmediata: Integración y Motor (Back-End)

Estas tareas corresponden a los requerimientos funcionales más críticos (RF-01, RF-03, RF-06). 

- `[/]` **Implementar Broadcast de Estado en STOMP**
  - Actualmente el WebSocket emite updates parciales. 
  - Al procesar cualquier acción en el motor (atacar, bajar energía, evolucionar), se debe hacer broadcast del DTO completo (`GameStateView`) a `/topic/match/{id}`.
  - El frontend Angular ya está diseñado para consumir el DTO entero y repintar la vista de forma reactiva.
- `[ ]` **Crear un Bot Heurístico (`SimpleBotAI`) para Testeo**
  - El Bot se implementa en Spring Boot como un servicio.
  - Deberá instanciarse e inscribirse como "Player 2" en una partida local.
  - Lógica simple (Heurística básica con `if/else`): Si tiene energías unirlas, si puede atacar, atacar.
  - Este bot permitirá probar el Motor completo, validando estados y flujos sin requerir 2 jugadores físicos conectados y sin chocar con errores de UI.
  - _(MCTS o IA avanzada queda relegada al backlog de post-entrega)_
- `[ ]` **Completar las Validaciones Estrictas**
  - Revisar y pulir el Validator actual para impedir acciones ilícitas (Evolucionar en Turno 1 o en el turno de bajada del Pokémon).
  - Activar los handlers para Efectos de Entrenadores Básicos (Cartas de Robo y Búsqueda).
  - Habilitar los efectos y toma de daño en el `BETWEEN_TURNS` para condiciones (Envenenado y Quemado).

## 💾 Fase 2: Persistencia Crítica (RF-05)

- `[ ]` **Serializar el Estado del Tablero (JSONB)**
  - Dentro del bloqueo atómico (`ReentrantLock`), luego de una acción válida, guardar el estado en la DB en un campo de tipo `jsonb` o similar.
  - Esto garantiza poder reconectarse en caso de perder el WebSocket sin que la partida se rompa (Requisito RF-06: Manejo robusto de reconexiones).
- `[ ]` **Añadir el Log Inmutable de Partida**
  - Conectar el `gameStatePersistence.logAction(...)` al final de cada turno/jugada.

## 💻 Fase 3: Integración Front-End (Angular)

- `[/]` **Perfeccionar Interfaces (Mockeadas)**
  - `[x]` Layout Principal y Tablero (clon 1:1 de `claude design`)
  - `[x]` Solucionar solapamiento/colisiones del mouse (hitbox fix) al hacer hover sobre las cartas en mano (`.fan-card::before`).
  - `[x]` Deshabilitar interacciones ilegales locales (ej. clic en el banco enemigo).
  - `[ ]` Menús Desplegables Condicionales de acción (Evolucionar, Bajar a Banca, etc.) según estado local.
- `[ ]` **Reemplazar Estado Mock por STOMP**
  - Una vez que la "Prioridad Inmediata" del Backend esté lista, borrar los fixtures falsos del `MatchStore` y suscribirse a STOMP real.
