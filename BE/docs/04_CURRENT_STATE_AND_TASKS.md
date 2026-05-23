# 📊 Estado Actual del Engine y Tareas Pendientes

Este documento es un snapshot del avance actual del motor de reglas, qué capacidades tiene desbloqueadas, y qué tareas de desarrollo quedan pendientes para lograr el MVP completo de Pokémon TCG XY1.

## 🚀 Lo que YA funciona (Engine Core)

1. **Gestión de Fases y Turnos:** El `TurnManager` orquesta perfectamente el paso entre `DrawPhase`, `MainPhase`, `AttackPhase`, y `BetweenTurnsPhase`.
2. **Setup y Mulligans:** El `SetupManager` reparte las 7 cartas iniciales, detecta ausencia de Básicos, ejecuta Mulligans, recompensa con robos extra, setea 6 premios ocultos, y elige quién empieza tirando una moneda.
3. **Mecánica de Cartas Físicas:** El motor ya no destruye cartas. Un Pokémon debilitado toma su carta Base, sus evoluciones superpuestas y sus energías adjuntas y las manda a la `DiscardPile`.
4. **Condiciones de Victoria Completas:**
   - **Por Premios:** El jugador roba todas sus cartas de premio.
   - **Por Bench-Out:** El Pokémon Activo es debilitado y el rival no tiene Pokémon en la Banca para reemplazarlo.
   - **Por Deck-Out:** Al iniciar el turno, el jugador debe robar pero no tiene cartas en el Deck.
   - **Empate / Muerte Súbita:** Choque simultáneo de KOs o premios. El engine expone `MatchSession.resetForSuddenDeath()` para resetear todo y jugar a 1 solo premio.
5. **Efectos de Entrenadores:** Los Trainer Cards ya no son pasivos. A través del `TrainerEffectResolver` en la `GameFacade`, se pueden disparar efectos inyectando el runtime del jugador. Ya están armadas las bases (ej: *Potion*, *Professor Sycamore*).
6. **Validación Exhaustiva:** El `RuleValidator` intercepta movimientos ilegales (retirarse dormido, evolucionar el mismo turno, falta de energía).
7. **Pipeline de Ataque:** Completo con debilidades, resistencias y estados alterados.

---

## 🛠️ Lo que falta (Lista de Tareas Pendientes)

La cadena de ejecución está ordenada. ¡**Ya se pueden simular partidas**!. El Game Engine (Módulo 1), WebSockets (Módulo 2) y Persistencia (Módulo 4) están funcionales. A continuación se detallan las tareas necesarias para completar el TPI con excelencia.

### MÓDULO 3: Deck Builder y Caché de Cartas (Backend)
*(Ref: `consigna.txt` RF-04, pág 9 / `pokemontcg-api-reference.md`)*

- **TAREA 1: Servicio de Sincronización de Cartas (Caché Local)**
  - Consumir la API `api.pokemontcg.io/v2/cards?q=set.id:xy1&pageSize=250`.
  - Guardar las 146 cartas del set XY1 en PostgreSQL al iniciar la aplicación si la base está vacía. **(Crucial: el motor no debe llamar a la API externa durante el juego, ref: RF-03 pág 8).**
  - Parsear los datos críticos (`damage`, `hp` como int, mapear tipos de energía). Ver sección "Gotchas Importantes" en `pokemontcg-api-reference.md`.
- **TAREA 2: Validación Estricta de Mazos**
  - Crear el endpoint de validación de mazo: debe garantizar exactamente 60 cartas, al menos 1 Pokémon Básico, máximo 4 copias de la misma carta (excepto Energía Básica) y **máximo 1 carta AS TÁCTICO** en todo el mazo.
- **TAREA 3: CRUD de Mazos Persistidos**
  - Endpoints REST (`GET`, `POST`, `PUT`, `DELETE` sobre `/api/decks`) para guardar y cargar mazos asociados a un usuario.

### MÓDULO 5: Frontend Angular 21+ (Arquitectura y WebSockets)
*(Ref: `consigna.txt` RNF-02, pág 12 / `CLAUDE.md`)*

- **TAREA 4: Setup de Arquitectura Angular Standalone**
  - Configurar Angular con 100% Standalone Components, uso de **Signals** (prohibido RxJS para estado local), y Control Flow nativo (`@if`, `@for`).
  - Separar componentes en Smart (Containers, inyectan servicios) y Dumb (Presentational, usan `input()` y `output()`).
- **TAREA 5: Cliente STOMP y Sincronización de Estado**
  - Desarrollar `MatchWebSocketService` para conectarse al canal `/topic/match/{matchId}/player/{playerId}`.
  - El frontend **nunca** debe modificar su estado local directamente tras una acción de usuario, solo debe renderizar el `GameStateResponseDTO` que llega desde el backend. (Ref: RF-06 pág 10).

### MÓDULO 5: Pantalla de Juego y UX Interactivo
*(Ref: `consigna.txt` RF-07, pág 11)*

- **TAREA 6: Layout del Tablero (Zonas de Juego)**
  - Implementar visualmente las zonas: Pokémon Activo, Banca (hasta 5 espacios), Mazo, Cartas de Premio (6 reversos), Pila de Descarte, Zona de Estadio (compartida) y la Mano del jugador.
  - Ocultar la mano del rival (solo mostrar cantidad de cartas) para respetar la Niebla de Guerra.
- **TAREA 7: Drag & Drop Funcional**
  - Añadir soporte nativo de Drag & Drop para:
    1. Bajar Pokémon Básicos a la banca.
    2. Unir cartas de Energía a un Pokémon específico.
    3. Equipar Herramientas Pokémon.
  - Asegurar el feedback visual (highlight de dropzones válidas).
- **TAREA 8: Visualización de Estados y Daño**
  - Mostrar contadores de daño (ej: `[30 / 180 HP]`) y Energías unidas sobre la carta.
  - **Rotación de cartas:** Girar la carta 90° para Dormido, 180° para Confundido, etc. O usar marcadores visuales para Quemado/Envenenado.
  - Utilizar `NgOptimizedImage` para renderizar imágenes (`images.small` de la API de cartas).

### TAREAS OPCIONALES (Puntos Extra)
*(Ref: `consigna.txt` Evaluación, pág 16 y 17)*

- **TAREA 9: Mazos Temáticos Pre-armados (Seed Data)**
  - Armar 2 mazos 100% legales y funcionales del set XY1 y dejarlos como seeders (`V2__seed_xy1_cards.sql`) para arrancar a probar rápido. Otorga hasta 10 puntos bonus.
- **TAREA 10: Animaciones (CSS/Angular Animations)**
  - Efectos visuales de transición para evoluciones, ataques y knockouts.
- **TAREA 11: Chat In-Game**
  - Un componente de chat acoplado a la partida, mandando mensajes vía WebSockets.

> [!TIP]
> **A los desarrolladores o IAs entrantes:**  
> Por favor lean `02_ARCHITECTURE_AND_ENGINE.md` antes de tocar el backend. Si abordan el Módulo 3, apoyénse fuerte en `pokemontcg-api-reference.md`. Para el Frontend, respeten las reglas de Signals y Dumb/Smart components obligatorias de `CLAUDE.md`.
