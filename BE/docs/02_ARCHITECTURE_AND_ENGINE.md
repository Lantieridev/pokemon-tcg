# 🏗️ Arquitectura del Engine Core

Este documento es la **ÚNICA VERDAD ARQUITECTÓNICA**. Detalla cómo está estructurado el motor del juego. Toda la lógica aquí es stateful, en memoria, pura (sin dependencias de frameworks) y estrictamente guiada por eventos.

> [!CAUTION]  
> **RULEBOOK XY1 Y LA CONSIGNA SON LA BIBLIA INQUEBRANTABLE.**  
> Ninguna IA, compañero de equipo, o refactorización está autorizada a romper la fidelidad con las reglas base. Si una lógica difiere del Rulebook XY1, **está mal**. Siempre prioriza la pureza del motor por sobre atajos rápidos.

---

## 1. El Patrón Facade (`GameFacade`)
El único punto de entrada para mutar el estado del juego en curso desde el exterior es la clase `GameFacade`. 
- **NO DEBES** invocar a los Executors o Pipelines directamente desde un Controller o un REST Service.
- La fachada recibe DTOs (e.g., `ActionRequestDTO`), traduce esos DTOs a acciones de dominio (e.g., `AttackAction`, `PlayTrainerAction`), resuelve los `BattlePokemonState` objetivos (Activo vs Banca) y orquesta la aplicación.
- **Inyección de Dependencias Limpia:** `GameFacade` carece de estado (es un `@Service` o Singleton lógico) e invoca mutaciones sobre `MatchSession`, la cual contiene todo el runtime. No usa instancias de `new Random()` dispersas; utiliza abstracciones puras inyectadas al crear la partida.

## 2. Modelos en Memoria (`PlayerRuntime` y `MatchSession`)
A diferencia de un juego donde la base de datos es la fuente de la verdad, aquí **la memoria ram de la JVM lo es**.
- `MatchSession`: El estado general de la partida (WAITING, SETUP, ACTIVE, FINISHED). Mantiene la concurrencia (Locks) y provee acceso al `CoinFlipper` (para evitar randoms indeseables en los tests) y al `TurnManager`.
- `PlayerRuntime`: Representa todo lo físico que tiene el jugador. 
  - Contiene: `Deck`, `Hand`, `Bench`, `DiscardPile`, y `prizePile` (Premios Ocultos).
  - Los **Premios** son extraídos como `Card` directamente desde el deck.
  - Cuando un Pokémon muere, el engine (a través del `KnockoutResolutionHandler`) toma el `BaseCard` (la carta base subyacente), las evoluciones y las energías adjuntas y las traslada enteramente a la `DiscardPile`. ¡Nada se borra, las cartas físicas persisten viajando por los distintos pilones!

## 3. The Pipeline Pattern (Ataques y Efectos)
Los procesos complejos y secuenciales están modelados en **Pipelines** o **Resolvers**.
- **`AttackPipeline`**: Subdivide el ataque en Steps:
  1. `ValidationStep`: Verifica si puede atacar.
  2. `PreDamageEffectsStep`: Efectos a aplicar antes del daño.
  3. `DamageCalculationStep`: Calcula daño con debilidades, resistencias y el multiplicador XY1.
  4. `DamageApplicationStep`: Aplica el daño al HP (reduciendo o sumando damage counters).
  5. `PostDamageEffectsStep`: Efectos adicionales al ataque.
  6. `KnockoutCheckStep`: Chequea si el HP llegó a 0 y despacha el evento al KnockoutManager.
- **`TrainerEffectResolver`**: Un switch-case o Map de funciones que mapea el ID / tipo del efecto del Trainer (e.g. `Professor Sycamore`, `Switch`, `Heal`) a una función que muta el `PlayerRuntime` en cuestión.

## 4. The Event Listener Pattern (Ciclo de Vida)
El motor evita el "spaghetti code" mediante eventos y suscripciones a través del `TurnManager`.
- `PhaseListener`: Componentes como `DrawPhaseExecutor` o `VictoryConditionChecker` se suscriben a las fases del turno.
  - Ejemplo: `DrawPhaseExecutor` roba una carta automáticamente al inicio del turno. Si no hay deck, `VictoryConditionChecker` detecta el Deck-Out y otorga victoria al rival.
- `KnockoutHandler`: Escucha el evento de un Pokémon derrotado. `KnockoutResolutionHandler` limpia la mesa y hace que el atacante robe premios. `VictoryConditionChecker` evalúa si se acabaron los premios o si la banca rival está vacía.
- **Sudden Death (Muerte Súbita):** Si hay un KO mutuo y la validación otorga victoria a ambos (empate), `MatchSession.resetForSuddenDeath()` barajará todo a los Decks y llamará al `SetupManager` **con 1 sola carta de premio** por jugador.

## 5. Validación Centralizada (`RuleValidator`)
El `RuleValidator` es un componente de **solo lectura**. No muta el estado.
Su responsabilidad es asegurar que la acción solicitada cumple con el Rulebook XY1. 
- Verifica: ¿Es el turno del jugador?, ¿El Pokémon está dormido o paralizado y no puede atacar/retirarse?, ¿Hay energía suficiente?, ¿Está evolucionando en su primer turno o en el turno que fue bajado?
- **Extensibilidad:** Si agregas una regla al juego, agrégala como un check en `RuleValidator`. **NO LA METAS EN LA FACHADA**.

## 6. Determinismo y Testing
Cualquier clase que dependa del azar (monedas, barajar mazos) recibe una abstracción (e.g., `CoinFlipper` o un `Consumer<Deck>` para barajar). 
Esto asegura que las pruebas unitarias pasen el 100% de las veces. **Bajo ningún concepto agregues un `new Random().nextBoolean()` dentro de una clase core del engine.**
