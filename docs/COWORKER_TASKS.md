# 📋 Plan de Tareas Detallado: Pokémon TCG

Este archivo contiene el plan de tareas detallado y las especificaciones técnicas para implementar las cuatro funcionalidades pendientes en el proyecto Pokémon TCG.

---

## 🛡️ TAREA 1: Modo Espectador (Modo Espectador)

Permite que usuarios ajenos a la partida se conecten al WebSocket, observen el tablero con Niebla de Guerra (sin revelar cartas de la mano) y sin poder realizar ninguna acción interactiva.

### Backend (Java)
- `[ ]` **Implementar el mapeador neutral en [PlayerPerspectiveMapper.java](file:///c:/Users/Ornella/Documents/TUP/3%C2%B0%20Cuatrimestre/Progra%203/TPI/BE/src/main/java/ar/edu/utn/frc/tup/piii/services/PlayerPerspectiveMapper.java)**:
  - Crear un método `public GameStateResponseDTO toSpectatorResponse(final MatchSession session)`.
  - Construir la vista de jugador (`PlayerView`) para el Jugador A (índice 0) pero ofuscar el array `hand` reemplazando los IDs de las cartas por strings vacíos `""` manteniendo la misma cantidad.
  - Construir la vista de oponente (`OpponentView`) para el Jugador B (índice 1) que de forma nativa solo expone `handSize`.
- `[ ]` **Wirear el broadcast de espectador en [MatchService.java](file:///c:/Users/Ornella/Documents/TUP/3%C2%B0%20Cuatrimestre/Progra%203/TPI/BE/src/main/java/ar/edu/utn/frc/tup/piii/services/MatchService.java)**:
  - En los métodos `broadcastState` y `abandonMatch`, enviar la vista de espectador al tópico `/topic/match/{matchId}/spectator`.
- `[ ]` **Wirear el broadcast de espectador en [MatchCreationService.java](file:///c:/Users/Ornella/Documents/TUP/3%C2%B0%20Cuatrimestre/Progra%203/TPI/BE/src/main/java/ar/edu/utn/frc/tup/piii/services/MatchCreationService.java)**:
  - En los métodos `handleVictory` y `createMatch`, transmitir el estado de espectador a `/topic/match/{matchId}/spectator` al iniciar o finalizar el encuentro.

### Frontend (Angular)
- `[ ]` **Actualizar conexión de WebSocket en [websocket.service.ts](file:///c:/Users/Ornella/Documents/TUP/3%C2%B0%20Cuatrimestre/Progra%203/TPI/FE/src/app/core/services/websocket.service.ts)**:
  - Modificar `connect(matchId: string, isSpectator: boolean = false)` para que si `isSpectator` es `true`, se suscriba al canal `/topic/match/${matchId}/spectator` en vez de `/topic/match/${matchId}/player/${username}`.
- `[ ]` **Configurar la vista de batalla en [battle.component.ts](file:///c:/Users/Ornella/Documents/TUP/3%C2%B0%20Cuatrimestre/Progra%203/TPI/FE/src/app/features/battle/battle.component.ts)**:
  - Leer el query parameter `spectator === 'true'` en `ngOnInit()`.
  - De ser así, invocar `wsService.connect(this.matchId, true)` y establecer una señal/señalador `isSpectator = true`.
- `[ ]` **Adaptar la interfaz de usuario en [battle.html](file:///c:/Users/Ornella/Documents/TUP/3%C2%B0%20Cuatrimestre/Progra%203/TPI/FE/src/app/features/battle/battle.html)**:
  - Deshabilitar interacción para espectadores: ocultar botón de "FIN TURNO", deshabilitar drag-and-drop, doble click, click derecho y menú de acciones sobre las cartas.
  - Mostrar un banner flotante arriba que diga `🛡️ MODO ESPECTADOR`.

---

## 🎨 TAREA 2: Animaciones y Efectos Visuales a las Cartas

Añadir micro-animaciones premium a la interfaz del juego para cuando una carta sufre daño, evoluciona o recibe una energía.

### Frontend (Angular & CSS)
- `[ ]` **Agregar animaciones CSS en [battle.css](file:///c:/Users/Ornella/Documents/TUP/3%C2%B0%20Cuatrimestre/Progra%203/TPI/FE/src/app/features/battle/battle.css)**:
  - Diseñar `@keyframes damage-shake`: Movimiento rápido lateral para simular golpe de daño.
  - Diseñar `@keyframes evolution-glow`: Destello brillante (cyan/oro) con escala aumentada momentáneamente.
  - Diseñar `@keyframes energy-glow`: Pulso de energía que emana del marco de la carta.
  - Declarar clases utilitarias correspondientes: `.damage-shake`, `.evolution-glow`, `.energy-glow`.
- `[ ]` **Lógica de detección de cambios en [battle.component.ts](file:///c:/Users/Ornella/Documents/TUP/3%C2%B0%20Cuatrimestre/Progra%203/TPI/FE/src/app/features/battle/battle.component.ts)**:
  - Escuchar cambios en la suscripción a `gameState$` y comparar con el estado anterior:
    - **Daño recibido**: Si los contadores de daño de un Pokémon aumentan, activar la clase `.damage-shake` en su ID de carta.
    - **Evolución**: Si el ID del Pokémon en un slot cambia de una carta básica a una de evolución, activar la clase `.evolution-glow` en ese slot.
    - **Energía unida**: Si la lista de energías unidas a un Pokémon crece, activar la clase `.energy-glow` en el slot.
  - Usar un `setTimeout` de 500ms para limpiar la clase animada de la señal de animaciones activas (`activeCardAnimations`) para permitir ejecuciones consecutivas.

---

## 💀 TAREA 3: Derrota por Mazo Vacío (Deck Out)

Garantizar que las reglas de derrota por falta de cartas se apliquen estrictamente y que no se produzcan excepciones que interrumpan la partida cuando se roba mediante efectos.

### Backend (Java)
- `[x]` **Evitar errores en efectos de robo en [Deck.java](file:///c:/Users/Ornella/Documents/TUP/3%C2%B0%20Cuatrimestre/Progra%203/TPI/BE/src/main/java/ar/edu/utn/frc/tup/piii/engine/model/Deck.java)**:
  - La derrota por robar al inicio del turno ya está implementada en `DrawPhaseExecutor.java` y `VictoryConditionChecker.java`.
  - **Corrección**: Modificar el método `drawMultiple(final int n)` para que retorne `Math.min(n, cards.size())` cartas en lugar de lanzar una excepción `DeckEmptyException` cuando `n > cards.size()`. Las reglas oficiales establecen que el jugador roba tantas cartas como queden en su mazo y no pierde inmediatamente sino hasta el inicio de su próximo turno.
- `[x]` **Validar tests unitarios**:
  - Verificar que el test `shouldFireDeckOutVictoryWhenDeckIsEmpty` en `DrawPhaseExecutorTest` pase correctamente.
  - Escribir un test unitario en `DeckTest` que verifique que el mazo puede vaciarse por robo múltiple sin lanzar excepciones.

---

## 🃏 TAREA 4: Efectos Secundarios de Cartas de Entrenadores

Agregar las reglas de validación en el backend para evitar que cartas de Entrenador de uso complejo se jueguen de manera ilegal o sin efecto válido.

### Backend (Java)
- `[x]` **Implementar validaciones detalladas en [RuleValidator.java](file:///c:/Users/Ornella/Documents/TUP/3%C2%B0%20Cuatrimestre/Progra%203/TPI/BE/src/main/java/ar/edu/utn/frc/tup/piii/engine/manager/RuleValidator.java)**:
  - Agregar reglas de validación específicas en el método `validatePlayTrainer` para los siguientes efectos:
    - **Super Poción (`SUPER_POTION`)**: Validar que el Pokémon objetivo tenga daño y al menos una energía unida.
    - **Cassius (`CASSIUS`)**: Validar que el Pokémon objetivo esté en juego.
    - **Evosoda (`EVOSODA`)**: Validar que el Pokémon objetivo esté en juego, pueda evolucionar y no haya sido bajado en este turno.
    - **Max Revive (`MAX_REVIVE`)**: Validar que haya al menos una carta de Pokémon Básico en la pila de descarte.
    - **Carta del Profesor (`PROFESSORS_LETTER`)** y **Super Bola (`GREAT_BALL`)**: Validar que el mazo del jugador tenga al menos una carta.
    - **Recluta del Team Flare (`TEAM_FLARE_GRUNT`)**: Validar que el Pokémon activo del oponente tenga al menos una energía unida.
    - **Carta Roja (`RED_CARD`)**: Validar que la mano del oponente tenga al menos una carta.
- `[x]` **Pruebas Unitarias**:
  - Escribir pruebas unitarias correspondientes en `RuleValidatorTest.java` para asegurar la correcta validación y rechazo de acciones inválidas.
