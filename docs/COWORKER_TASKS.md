# 📋 Lista de Tareas Unificada (Single Source of Truth)

> 🤖 **INSTRUCCIÓN CRÍTICA PARA EL USO DE IAs:**  
> Cuando deleguen cualquiera de estas tareas a Claude, ChatGPT o cualquier otro agente, **es obligatorio** que incluyan en su prompt la orden estricta de contrastar todo el código generado con `docs/references/consigna.txt` y con cualquier otro documento dentro de la carpeta `docs/references/`. ¡Esa carpeta es nuestra Biblia y no podemos desviarnos de ella!

**Actualizado:** 2026-05-27
**Estado del Backend:** 100% Terminado 🚀 (Todos los blockers del motor fueron resueltos).

Todo el trabajo que falta en este proyecto es principalmente de **Integración (Wiring)** y **Features de Valor**. Tenemos que conectar las maravillosas APIs y el STOMP que ya existen en el backend, con las vistas de Angular, y luego avanzar con los extras.

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

---

## 🎮 TIER 3 — NICE TO HAVE (Defensa Oral y QA)

Para darle volumen y profesionalismo a la aplicación, ideal para mostrar en la evaluación.

- `[ ]` **Modo Practice vs Bot:** | *Equipo: Backend* | Dificultad: Alta 🔴
  - Implementar un servicio `SimpleBotAI` con heurística básica (`if/else`) para testear partidas solos.
- `[ ]` **Admin Dashboard:** | *Equipo: Fullstack* | Dificultad: Media 🟡
  - Armar un panel mínimo para re-popular la BD de cartas (Reseed) y ver estadísticas globales.
- `[ ]` **Modo Espectador:** | *Equipo: Frontend* | Dificultad: Media 🟡
  - Permitir unirse al `/topic/match/{id}` sin enviar comandos, mostrando el tablero general.

---

## 🚀 TIER 4 — ROADMAP POST-TPI

Ideas a futuro que no bloquean la nota pero hacen brillar el producto.

- `[ ]` **Auto-GG / Auto-Rematch:** Botón de revancha o "Buen juego" tras el cartel de victoria.
- `[ ]` **Animaciones y Partículas CSS:** Cuando se ataca, sacar una bola de fuego hacia la carta rival.
- `[ ]` **Megaevolución:** Reglas extra de la expansión XY para megaevolucionar y perder el turno.
