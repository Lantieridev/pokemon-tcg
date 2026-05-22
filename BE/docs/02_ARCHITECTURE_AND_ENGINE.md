# 🏗️ Arquitectura del Engine Core

Este documento detalla cómo está estructurado el motor del juego. **Toda la lógica aquí es stateful, en memoria, pura (sin dependencias de frameworks) y guiada por eventos.**

## El Patrón Facade (`GameFacade`)
El único punto de entrada para mutar el estado del juego desde el exterior es la clase `GameFacade`. 
- **NO DEBES** invocar a los Executors o Pipelines directamente desde un Controller o Service.
- La fachada recibe DTOs limpios (RequestAction), los despacha al motor interno y orquesta las validaciones.

## The Pipeline Pattern
Los procesos complejos (como atacar) están divididos en `Steps` secuenciales que conforman un Pipeline. Esto facilita probar cada parte del ataque por separado y asegura un flujo claro.
- **`AttackPipeline`**: Compuesto por:
  1. `ValidationStep`
  2. `PreDamageEffectsStep`
  3. `DamageCalculationStep` (Aquí corre el DamageCalculator con debilidades, resistencias y el multiplier XY1)
  4. `DamageApplicationStep`
  5. `PostDamageEffectsStep`
  6. `KnockoutCheckStep`

## The Event Listener Pattern
Para resolver el "Turn Phase lifecycle" sin crear espagueti, usamos `PhaseListener`.
Módulos que necesitan reaccionar a cambios de fase se registran en `TurnManager`.
Ejemplos actuales:
- `DrawPhaseExecutor`: Automáticamente roba carta al entrar a la `DrawPhase`.
- `MatchActionLogger`: (A implementar) Escucha eventos para persistir logs.

## The Rule Validator
El `RuleValidator` es un componente de **solo lectura**. No muta el estado. Su única responsabilidad es recibir una `Action`, evaluar el estado actual del `PlayerRuntime` (o del oponente) y devolver `ValidationResult.Valid` o `ValidationResult.Invalid`. 

> [!TIP]
> **Extensibilidad:** Si agregas una regla nueva al juego, **NO LA METAS EN LA FACHADA**. Agrégala al `RuleValidator` y luego asegúrate de que el estado que necesitas validar esté expuesto en los getters correspondientes.
