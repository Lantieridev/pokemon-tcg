# SDD Módulo 1: Game Engine (Core Logic)

Este es el corazón del juego. El requerimiento más estricto de la consigna es que **el backend es la única fuente de la verdad**. El Game Engine debe validar todas las reglas del juego y ser agnóstico del transporte (WebSockets o Controladores HTTP) y de la persistencia.

## Objetivo
Implementar un motor de juego de Pokémon TCG basado estrictamente en el Rulebook oficial (XY1) capaz de validar movimientos, calcular el daño y gestionar las condiciones especiales.

## Componentes Principales

1. **`TurnManager` (Gestor de Turnos y Fases)**
   - **Patrón State:** Gestiona los estados del turno `DRAW`, `MAIN`, `ATTACK`, `BETWEEN_TURNS`.
   - Controla de quién es el turno.
   - Aplica el procesamiento automático de la fase `BETWEEN_TURNS` (1: Envenenado, 2: Quemado, 3: Dormido, 4: Paralizado).

2. **`RuleValidator` (Validador de Reglas)**
   - Método principal: `boolean isValidAction(Action action, GameState state)`
   - Valida si un jugador puede realizar una acción en una fase determinada:
     - Jugar Entrenador (solo 1 Partidario por turno, 1 Estadio por turno, Objetos ilimitados).
     - Evolucionar (valida si es el primer turno del jugador o el primer turno del Pokémon en juego).
     - Atacar (valida si tiene las energías necesarias, si está dormido o paralizado).

3. **`DamageCalculator` (Calculadora de Daño)**
   - **Patrón Chain of Responsibility** para resolver el ataque (7 pasos obligatorios de la consigna).
   - Recibe al Atacante, Defensor, y el Daño Base.
   - Aplica Debilidad (x2) y Resistencia (-20, mínimo 0).
   - Valida si el atacante está confundido (lanza moneda: cruz = 3 contadores y fin de ataque).

4. **`StatusEffectManager` (Manejo de Condiciones Especiales)**
   - Lógica de estados alterados: Dormido, Quemado, Confundido, Paralizado, Envenenado.
   - Aplica exclusiones mutuas (Dormido, Confundido y Paralizado se pisan entre sí).
   - Permite superposición (Quemado + Envenenado pueden coexistir con uno de los otros tres).

5. **`VictoryConditionChecker`**
   - Verifica al final de cada turno o acción las condiciones de victoria:
     1. Última carta de premio robada.
     2. Sin Pokémon Activo/Banca del oponente.
     3. Mazo vacío al intentar robar.

## Pruebas (Mandatorias)
La consigna requiere **≥ 90% de coverage** con JUnit/Mockito sobre este módulo de manera aislada. 
- Implementar TDD: escribir los tests de casos como "Atacar estando paralizado lanza error" antes de programar la validación.
