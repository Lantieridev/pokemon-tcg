# Plan de Implementación Completo y Detallado - Set Flashfire (XY2)

Este plan describe la arquitectura y pasos necesarios para implementar todas las habilidades, ataques, pasivas y cartas de entrenador del set **Flashfire (XY2)** que actualmente no están soportadas, están implementadas a medias o presentan errores en el motor de juego de Pokémon TCG.

El desarrollo está organizado en **12 bloques de desarrollo progresivos, concisos y probados** para asegurar que no quede ningún efecto del set fuera del motor de reglas.

---

## Directrices Generales de Desarrollo

1. **Ramas y Commits**: Cada bloque debe ser desarrollado en su propia sub-rama (ej. `feature/flashfire-bloque-X`) y contar con commits atómicos.
2. **Métodos de Testeo**: No se dará por finalizado un bloque sin haber escrito los tests unitarios correspondientes en:
   * [AttackEffectResolverTest.java](file:///c:/Users/lucas/.gemini/antigravity/scratch/tpi-pokemon-2w1-15/BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/pipeline/AttackEffectResolverTest.java) (efectos de ataques).
   * [PokemonAbilitiesTest.java](file:///c:/Users/lucas/.gemini/antigravity/scratch/tpi-pokemon-2w1-15/BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/manager/PokemonAbilitiesTest.java) (habilidades y pasivas).
   * [RuleValidatorTest.java](file:///c:/Users/lucas/.gemini/antigravity/scratch/tpi-pokemon-2w1-15/BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/manager/RuleValidatorTest.java) (restricciones).
3. **Mapeo Robusto**: Todos los efectos nuevos deben ser analizados gramaticalmente en [CardMapper.java](file:///c:/Users/lucas/.gemini/antigravity/scratch/tpi-pokemon-2w1-15/BE/src/main/java/ar/edu/utn/frc/tup/piii/persistence/mapper/CardMapper.java) para evitar colisiones con cartas de otros sets.

---

## 📦 Bloque 1: Correcciones Críticas de Mapeo (Sueño y Curación Propios)
*   **Snorlax (*Sleepy Press*) y Sealeo (*Rest*)**:
    *   *Problema:* Contienen la frase `"is now Asleep"` y el parser genérico los mapea erróneamente como `"sleep"` (duerme al oponente e ignora la curación propia).
    *   *Solución:* Modificar la condición de `"sleep"` en `CardMapper` para que si contiene `"this pokémon is now asleep"` y `"heal"`, retorne `"heal_and_sleep:AMOUNT"`.
    *   *Resolver:* Registrar `HEAL_SELF_AND_SLEEP` en `AttackEffectType` y `AttackEffectResolver`. El manejador curará al atacante y aplicará `StatusEffectType.DORMIDO` a su propio runtime.
*   **M Charizard-EX (*Wild Blaze*)**:
    *   *Solución:* Mapear `"discard the top \\d+ cards of your deck"` -> `"discard_deck_self:5"`. El manejador en `AttackEffectResolver` descartará las 5 cartas superiores de su propio mazo.
*   **Torkoal (*Flamethrower*)**:
    *   *Solución:* Mapear a `"coin_flip_discard_energy:1"`. Implementar en el resolvedor que lance una moneda y, si sale cruz (tails), descarte 1 energía unida al atacante.

---

## 📦 Bloque 2: Lanzamiento de Moneda hasta Cruz (Until Tails)
*   **Roserade (*Whiplash*)**:
    *   *Solución:* Mapear `"flip a coin until you get tails"` combinada con `"discard an energy attached to your opponent's Active"` -> `"coin_flips_until_tails_discard_opponent_energy"`.
    *   *Resolver:* Lanzar monedas hasta obtener cruz (tails). Por cada cara, lanzar un request de selección de energía al rival para descartar.
*   **M Kangaskhan-EX (*Wham Bam Punch*)**:
    *   *Solución:* Mapear `"flip a coin until you get tails"` con `"does \\d+ more damage"` -> `"coin_flips_until_tails_extra:30"`.
    *   *Pipeline:* En `PreDamageEffectsStep`, lanzar monedas hasta obtener cruz, acumulando `+30` de daño por cada cara.

---

## 📦 Bloque 3: Multiplicadores de Daño Basados en el Campo y Energía
*   **Miltank (*Powerful Friends*)**:
    *   *Solución:* Mapear el nombre `"powerful friends"` -> `"powerful_friends:70"`. En `PreDamageEffectsStep`, comprobar si en la banca del atacante hay un Pokémon en fase `STAGE2`. Si es así, sumar `+70` de daño.
*   **Carbink (*Wonder Blast*)**:
    *   *Solución:* Mapear `"does \\d+ more damage for each [Type] energy attached"` -> `"damage_per_energy_type:fairy:20"`. En `PreDamageEffectsStep`, contar las energías Hada unidas al atacante y añadir `cantidad * 20` de daño.

---

## 📦 Bloque 4: Multiplicadores de Daño Basados en Daño y Premios
*   **Weavile (*Claw Rend*)**:
    *   *Solución:* Mapear `"already has any damage counters"` -> `"damage_if_target_damaged:30"`. Si el defensor tiene contadores, sumar `+30`.
*   **Walrein (*Big Tusk*)**:
    *   *Solución:* Mapear `"minus \\d+ damage for each damage counter"` -> `"damage_minus_per_counter:10"`. Restar `contadores_del_atacante * 10` del daño base.
*   **Druddigon (*Revenge*)**:
    *   *Solución:* Rastrear en `PlayerRuntime` si algún Pokémon aliado fue debilitado el turno anterior. Mapear `"if any of your pokémon were knocked out... does \\d+ more damage"` -> `"revenge_damage:70"`. Sumar `+70` si se cumple la condición.
*   **Luxio (*Electricounter*)**:
    *   *Solución:* Mapear `"does \\d+ damage times the number of prize cards your opponent has taken"` -> `"damage_per_opponent_prize:40"`. En `PreDamageEffectsStep`, calcular premios tomados por el rival y multiplicar por 40.

---

## 📦 Bloque 5: Habilidades Pasivas (HP, Daño y Retirada)
*   **Qwilfish (*Counterattack Quills*)**:
    *   *Solución:* Registrar `COUNTERATTACK_QUILLS` en `AbilityEffectId`. En `ReactiveAbilityHandler.onDamageDealt`, si el defensor tiene esta habilidad y es el activo, aplicar 2 contadores de daño al atacante.
*   **Floette (*Flower Veil*)**:
    *   *Solución:* Registrar `FLOWER_VEIL` en `AbilityEffectId`. En `BattlePokemonState.getMaxHp`, verificar si hay alguna Floette aliada en juego y sumar `+20` HP si el Pokémon actual es tipo `GRASS`.
*   **Dragalge (*Poison Barrier*)**:
    *   *Solución:* Registrar `POISON_BARRIER` en `AbilityEffectId`. En `RuleValidator.validateRetreat`, bloquear la retirada del Pokémon activo del jugador si el rival posee esta habilidad y el activo está envenenado.

---

## 📦 Bloque 6: Habilidades Activas y Efectos de Entrada
*   **Snorlax (*Stir and Snooze*)**:
    *   *Solución:* Registrar `STIR_AND_SNOOZE` en `AbilityEffectId`. En `processBetweenTurns`, si Snorlax está dormido, lanzar 2 monedas; solo despierta si ambas salen caras.
*   **Forretress (*Thorn Tempest*)**:
    *   *Solución:* Registrar `THORN_TEMPEST` en `AbilityEffectId`. En `GameFacade.evolvePokemon`, al evolucionar a Forretress, aplicar automáticamente 1 contador de daño a todos los Pokémon rivales.
*   **Lopunny (*Big Jump*) y Goodra (*Gooey Regeneration*)**:
    *   *Solución:* Registrar `BIG_JUMP` y `GOOEY_REGENERATION`. Implementar `BigJumpStrategy` (regresa Lopunny y adjuntos a la mano) y `GooeyRegenerationStrategy` (descarta energía propia para curar 60) en el resolver de habilidades.

---

## 📦 Bloque 7: Estados Especiales de Precisión (Smokescreen / Sand-Attack)
*   **Stunky / Pidgeotto (*Smokescreen* / *Sand-Attack*)**:
    *   *Solución:* Mapear el efecto a `"smokescreen"`. Registrar `StatusEffectType.PRECISION_BAJA` y aplicar al rival al atacar.
    *   *Pipeline:* En `AttackCancellationStep`, si el atacante tiene `PRECISION_BAJA`, tirar una moneda. Si sale cruz, el ataque falla y consume el turno.
*   **Lopunny (*Sitdown Bounce*)**:
    *   *Solución:* Mapear a `"coin_flip_self_disable"`. Si sale cruz, setear `selfDisabledNextTurn` en el atacante. En `RuleValidator`, impedir declarar ataques el próximo turno.

---

## 📦 Bloque 8: Efectos de Soporte y Herramientas (Frost Barrier, Shatter, Peck Off)
*   **Avalugg (*Frost Barrier*)**:
    *   *Solución:* Mapear a `"prevent_damage_20"`. Guardar en el estado del Pokémon y restar 20 a los ataques entrantes en `PreDamageEffectsStep` el siguiente turno.
*   **Avalugg (*Shatter*)**:
    *   *Solución:* Mapear a `"discard_stadium"`. Si hay un estadio activo en `MatchSession`, removerlo.
*   **Pidgey (*Peck Off*)**:
    *   *Solución:* Mapear a `"discard_opponent_tool"`. Remover todas las herramientas unidas al defensor en `PreDamageEffectsStep` antes de calcular daño.

---

## 📦 Bloque 9: Cambios y Descartes Interactivos (Bounce, Flash Claw, Rock Rush)
*   **Buneary (*Bounce*)**:
    *   *Solución:* Mapear a `"switch_self"`. Intercambiar al Pokémon activo con uno de la banca inmediatamente después del daño.
*   **Sneasel (*Flash Claw*)**:
    *   *Solución:* Mapear a `"discard_opponent_hand:1"`. Crear un request interactivo para que el oponente elija una carta de su mano y la descarte.
*   **Barbaracle (*Rock Rush*)**:
    *   *Solución:* Mapear a `"discard_hand_energy_multiply_damage:fighting:30"`. Lanza un request interactivo para descartar energías Lucha de la mano y sumar `30 * cantidad` al daño en `PreDamageEffectsStep`.

---

## 📦 Bloque 10: Ataques de Área y Búsquedas del Mazo
*   **Floette / Florges (*Petal Blizzard*)**:
    *   *Solución:* Mapear `"does \\d+ damage to each of your opponent's pokémon"` -> `"damage_all_opponents:10"`. Aplicar daño normal al activo y daño sin debilidad/resistencia a toda la banca.
*   **Florges (*Brilliant Search*)**:
    *   *Solución:* Mapear a `"search_deck_any:3"`. Lanzar un request de selección de hasta 3 cartas cualesquiera del mazo del jugador para llevar a la mano, luego barajar.
*   **Furret (*Buried Treasure Hunt*)**:
    *   *Solución:* Mapear a `"look_top_4_take_2_discard_rest"`. Request interactivo que muestra las top 4 del mazo, el jugador elige 2 para la mano y las otras 2 se descartan.

---

## 📦 Bloque 11: Entrenadores de Descarte y Búsqueda
*   **Fiery Torch (`xy2-89`)**:
    *   *Solución:* Request interactivo para descartar 1 energía Fuego de la mano; si se descarta con éxito, el jugador roba 2 cartas.
*   **Pokémon Fan Club (`xy2-94` / `xy2-106`)**:
    *   *Solución:* Request interactivo de búsqueda de deck de hasta 2 Pokémon Básicos para llevar a la mano.
*   **Sacred Ash (`xy2-96`) y Pal Pad (`xy2-92`)**:
    *   *Solución:* Requests interactivos de selección del descarte (hasta 5 Pokémon o 2 Partidarios, respectivamente) para barajarlos de vuelta al mazo.

---

## 📦 Bloque 12: Entrenadores de Control y Efectos Globales
*   **Startling Megaphone (`xy2-97`)**:
    *   *Solución:* Remover inmediatamente todas las herramientas Pokémon unidas a los Pokémon en juego del oponente (activo + banca).
*   **Trick Shovel (`xy2-98`)**:
    *   *Solución:* Mostrar interactivamente la carta superior del mazo seleccionado (tuyo o del oponente) y dar la opción de descartarla o dejarla en el tope.
*   **Magnetic Storm (`xy2-91`)**:
    *   *Solución:* Registrar efecto de estadio. Mientras esté activo en juego, el paso de cálculo de daño ignorará por completo la Resistencia de todos los Pokémon en juego.
