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

La cadena de ejecución está ordenada. ¡**Ya se pueden simular partidas**!. Técnicamente podríamos instanciar un simulador y correr juegos de prueba (hacer jugar bots básicos) ya que todas las reglas obligatorias están soportadas. Sin embargo, para terminar la consola de la API y cerrar el TPI, falta lo siguiente:

### TAREA A: Habilidades (Abilities) ✅ COMPLETADA
El modelo y la resolución de habilidades ya están implementados.

### TAREA B: Acciones Interactivas y Prompts al Jugador ✅ COMPLETADA
El Engine tiene soporte completo para resoluciones de acciones e interactividad, manejado por la API y validado.

### TAREA C: Debugging Profundo de Partida Completa ✅ COMPLETADA
El test `XY1EndToEndIntegrationTest` ya se encarga de jugar y validar partidas completas e-2-e en el motor.

### TAREA D: API de Mazos (Deck Builder) y Frontend ⏳ EN PROGRESO
Actualmente el objetivo es finalizar los endpoints REST para que los jugadores puedan armar sus mazos consumiendo la API externa, y dar inicio al Frontend en Angular 20+.

> [!TIP]
> **A los desarrolladores o IAs entrantes:**  
> Por favor lean `02_ARCHITECTURE_AND_ENGINE.md` antes de escribir una línea de código. La arquitectura está herméticamente sellada. Si necesitan modificar reglas, vayan al `RuleValidator`. Si necesitan modificar flujo de KO, vayan al `KnockoutResolutionHandler`. **No expongan métodos mutadores inseguros en `PlayerRuntime`.**
