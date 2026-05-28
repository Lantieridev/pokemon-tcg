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

## 🌐 Fase 3: APIs REST y Conexión en Angular (Fullstack)

Para darle más libertad y ownership al equipo, ustedes no solo van a armar los endpoints en el Backend, sino que **también van a realizar la integración (wiring) en los servicios de Angular**. Nosotros les dejamos las vistas y componentes visuales listos, ustedes hacen las peticiones HTTP y conectan los datos:

- `[ ]` **Auth & Login**
  - **Backend:** `POST /api/auth/login` y `register`.
  - **Frontend:** Conectar el formulario de `login.html` usando `HttpClient`.
- `[ ]` **Perfil y Estadísticas**
  - **Backend:** `GET /api/users/{id}/profile`.
  - **Frontend:** Llenar los datos mockeados en `profile.html` con los datos reales del usuario.
- `[ ]` **Gestión de Mazos (RF-04)**
  - **Frontend (API Externa):** Implementar el fetch a `pokemontcg.io (v2)` para buscar las cartas del set `xy1` y cachearlas en el navegador (localStorage).
  - **Backend:** `GET /api/decks`, `POST /api/decks`, y re-validación de 60 cartas.
  - **Frontend:** Conectar el botón "Guardar" del Deck Builder a sus propios endpoints.

## 💻 Fase 4: Integración del Tablero (WebSocket)

- `[/]` **Perfeccionar Interfaces (Completado por UI Team)**
  - `[x]` Tablero reparado y funcional visualmente.
- `[ ]` **Reemplazar Estado Mock por STOMP**
  - Una vez que el Broadcast esté listo, borrar los fixtures del `MatchStore` y suscribirse a STOMP real en Angular.

## ⭐ Fase 5: Features de Valor Agregado (Puntos Extra)

Para darle mucho más "cuerpo" a la aplicación y lucirse en la defensa (además de sumar puntos extra explícitos en la rúbrica) sin meterse en mecánicas demasiado raras, sugerimos agregar esto cuando las fases anteriores estén estables:

- `[ ]` **Chat In-Game (+2 pts)**
  - Un canal STOMP adicional (ej: `/topic/match/{id}/chat`) paralelo al del tablero.
  - Componente visual flotante o lateral para mandarse mensajes durante la partida.
- `[ ]` **Ranking y Ligas (+1 pt)**
  - Cuando una partida termina en el estado `FINISHED`, actualizar un puntaje MMR en la tabla de usuarios.
  - Mostrar el Rango (Bronce, Plata, Oro) en el Perfil y en el Lobby.
- `[ ]` **Modo Espectador**
  - Permitir a un tercer cliente suscribirse al `/topic/match/{id}` pero sin permisos para enviar acciones.
  - Como el backend ya emite el DTO filtrado para la "perspectiva" de cada jugador (ocultando la mano del rival), crear una "Spectator Perspective" que oculte ambas manos pero muestre el tablero completo.
- `[ ]` **Selección de Mazos Temáticos Base (+10 pts)**
  - Ya que el backend precargó las cartas, armar 6 mazos "Pre-construidos" en la base de datos que los usuarios puedan elegir directamente en el Lobby sin necesidad de armar uno propio en el Deck Builder. Ideal para testear el juego rápido.
