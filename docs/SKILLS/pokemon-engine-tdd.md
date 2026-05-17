---
name: pokemon-engine-tdd
description: Skill base y obligatoria para el desarrollo del Game Engine del Pokémon TCG usando TDD estricto y Java 21.
---

# Pokémon TCG - Game Engine Guidelines

## 1. Filosofía Core (Aislamiento)
- **Cero Frameworks Web:** Está ESTRICTAMENTE PROHIBIDO importar clases de `org.springframework.*` dentro del paquete del Engine. No se usa `@Service`, `@Component`, ni `@Autowired`. El motor se instancia de forma pura (POJOs) para garantizar que sea 100% agnóstico y ultra rápido para testear.
- **Fail Fast:** El motor no devuelve `null` ni `boolean` silenciosos ante errores. Lanza excepciones de dominio claras (`NotEnoughEnergyException`, `InvalidTurnPhaseException`, `PokemonAsleepException`).

## 2. Strict TDD (Test-Driven Development)
- **Regla de Oro:** NO podés escribir una sola línea de lógica de negocio sin haber escrito antes un test unitario en rojo (failing test) que justifique esa línea.
- La meta es ≥ 90% de coverage.
- Todos los tests deben usar **JUnit 5** y **Mockito** (si hace falta mockear estados, aunque se prefieren state objects puros).
- Nombrar los tests describiendo comportamiento: `shouldThrowExceptionWhenAttackingWhileParalyzed()`.

## 3. Patrones de Diseño Obligatorios
- **State Pattern:** Para gestionar las fases del turno (`DRAW`, `MAIN`, `ATTACK`, `BETWEEN_TURNS`). No usar `if(phase == "MAIN")`.
- **Chain of Responsibility:** Para el cálculo de daño. Los pasos son inmutables: 1. Daño Base -> 2. Modificadores Atacante -> 3. Debilidad -> 4. Resistencia -> 5. Modificadores Defensor.
- **Strategy Pattern:** Para encapsular las reglas de las cartas (Ej: `AttackStrategy`, `AbilityStrategy`).
