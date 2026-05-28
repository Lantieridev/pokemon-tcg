# 📋 Lista de Tareas Unificada (Single Source of Truth)

**Actualizado:** 2026-05-27
**Estado del Backend:** 100% Terminado 🚀 (Todos los blockers del motor fueron resueltos).

Todo el trabajo que falta en este proyecto es **100% de Integración (Wiring)**. Tenemos que conectar las maravillosas APIs y el STOMP que ya existen en el backend, con las vistas simuladas (mock) que armamos en Angular.

---

## 🔗 TIER 1 — IMPRESCINDIBLE (Conexión Crítica)

Estas son las tareas obligatorias para que el flujo básico funcione y se pueda evaluar el TPI.

- `[ ]` **Wirear el Tablero (WebSocket STOMP)** | *Equipo: Frontend / Fullstack* | Dificultad: Alta 🔴
  - Borrar la data falsa de `MatchStore.ts`.
  - Conectar el store de Angular al `/topic/match/{id}` real que emite el DTO del tablero.
  - Implementar que al hacer clic en las cartas se dispare un mensaje a STOMP para atacar, bajar a la banca o evolucionar.
- `[ ]` **Conectar Auth & Login** | *Equipo: Fullstack* | Dificultad: Media 🟡
  - Conectar el formulario de `login.html` a `AuthController` (`POST /api/auth/login` y `register`).
  - Guardar el JWT en `localStorage`.
- `[ ]` **Deck Builder (Creación de Mazos)** | *Equipo: Fullstack* | Dificultad: Media 🟡
  - Armar el servicio Angular que haga el GET a `api.pokemontcg.io/v2/cards?q=set.id:xy1` y guarde el resultado en `localStorage` (para no hacer fetching infinito).
  - Conectar el botón "Guardar" de la vista para que dispare un POST a `DeckController` en el backend.

---

## ⭐ TIER 2 — HIGH ROI (Puntos Extra)

Estas funcionalidades ya están armadas enteras en el backend (controladores listos). Solo falta conectarlas al frontend para ganar todos los puntos extra de la rúbrica.

- `[ ]` **Chat In-Game (+2 pts)** | *Equipo: Frontend* | Dificultad: Baja 🟢
  - El backend ya expone `ChatWebSocketController` (con filtro de malas palabras).
  - Armar un componente visual lateral en el tablero y conectarlo al canal `/topic/chat/{id}`.
- `[ ]` **Perfil y Ranking (+1 pt)** | *Equipo: Frontend* | Dificultad: Baja 🟢
  - El backend ya cuenta con `RankingController` y `HistoryController`.
  - Conectar la pantalla del Perfil para que consuma el Rango (MMR) y el Historial en vez de usar mocks.
- `[ ]` **Visor de Replays** | *Equipo: Frontend* | Dificultad: Media 🟡
  - El backend expone `ReplayController`.
  - Armar una vista donde puedas darle "Play" y ver las acciones pasadas.
