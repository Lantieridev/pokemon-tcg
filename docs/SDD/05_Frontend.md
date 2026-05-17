# SDD Módulo 5: Frontend (Angular 21+)

**Responsables:** [Asignar 1 o 2 desarrolladores]

Este módulo provee la interfaz gráfica interactiva, incluyendo el creador de mazos, el lobby de partidas, y el tablero del juego en tiempo real (Game Board).

## Objetivo
Desarrollar una aplicación "dumb" (sin toma de decisiones de negocio) que actúe exclusivamente como capa de presentación del backend, siguiendo RNF-06 (feedback visual, animaciones, drag & drop) usando las mejores prácticas de Angular establecidas en `AGENTS.md`.

## Estructura y Flujo

### 1. Gestión de Estado Global y Local
- Uso exclusivo de **Signals**.
- El `GameState` recibido por WebSockets debe ser mapeado a Signals reactivos (`WritableSignal` para el estado crudo, `computed` para estados derivados como "cartas en mano", "energías disponibles", "estado de botones").
- Evitar mutar signals (`.mutate` está prohibido según `AGENTS.md`), usar `set()` o `update()`.

### 2. Tablero de Juego (Game Board UI)
La interfaz debe dividirse visualmente según el cliente oficial de Pokémon TCG Live:
- **Oponente (Arriba):** Su Banca (hasta 5 espacios), su Activo, Premios restantes, Cartas en Mano (boca abajo).
- **Compartido (Centro):** Carta de Estadio Activa.
- **Jugador (Abajo):** Su Activo, su Banca, su Mano, sus Premios, Mazo, Pila de Descarte.
- **Panel de Acciones (Lateral/Flotante):** Botones contextuales habilitados según la fase del turno:
  - Evolucionar, Unir Energía, Jugar Entrenador, Retirar, Atacar, Finalizar Turno.
  - El botón "Atacar" abre un modal o menú de ataques disponibles del Pokémon Activo, que valida si hay energía suficiente (UI side complementario, ya que el back lo rechazará de todos modos).

### 3. Drag & Drop Interactivo
- Mover cartas desde la Mano al espacio de Banca (para colocar Básicos).
- Mover una carta de Energía desde la Mano hacia un Pokémon Activo/Banca específico.
- Mover una Herramienta Pokémon hacia un objetivo.

### 4. Feedback y Notificaciones
- Rotar físicamente la tarjeta del Pokémon Activo si sufre una Condición Especial:
  - Dormido: Girado a la Izquierda.
  - Confundido: Invertido (180 grados).
  - Paralizado: Girado a la Derecha.
- Uso de Toasters / Snackbars (ej. con Angular Material o un componente custom) para eventos que vienen por Websocket ("Tu oponente ha robado 3 premios", "Ataque falla por confusión").

### 5. Reglas y Estilos
- Standalone components y control flow nativo de Angular (`@if`, `@for`).
- UI Responsive para Desktop y Tablets.
- RxStomp o WebSocket nativo integrado con el ecosistema inyectable de Angular (`inject()`).
