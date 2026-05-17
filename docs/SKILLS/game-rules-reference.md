# Pokémon TCG - Reference Rules (XY1)

Este documento contiene EXCLUSIVAMENTE la lógica dura y algorítmica del juego. Debe ser usado por los agentes de IA (Auditors, Coders) como fuente de verdad para validar el código, omitiendo imágenes, lore y marketing del PDF original.

## 1. Preparación de la partida (Setup)
- Ambos jugadores barajan sus mazos y roban 7 cartas iniciales.
- **Mulligan:** Si un jugador no tiene ningún Pokémon Básico en su mano inicial, debe mostrar su mano al rival, barajar y volver a robar 7 cartas. Por cada mulligan que declare un jugador, su oponente puede robar 1 carta adicional. Se repite hasta que ambos tengan al menos un Básico.
- Cada jugador coloca su Pokémon Activo boca abajo y hasta 5 Pokémon Básicos en Banca, también boca abajo.
- Cada jugador toma las primeras 6 cartas de su mazo y las coloca boca abajo como sus cartas de Premio.
- Se lanza una moneda para determinar quién comienza. Ambos jugadores revelan sus Pokémon y comienza la partida.

## 2. Estructura del turno
- **Fase 1 – Robo obligatorio:** El jugador roba 1 carta del mazo. *Excepción:* El jugador que empieza no roba en su primer turno. Si al intentar robar el mazo está vacío, ese jugador pierde.
- **Fase 2 – Acciones Principales:** (En cualquier orden, opcionales):
  - Colocar Pokémon Básicos en Banca.
  - Evolucionar Pokémon (Restricciones: no en el turno en que el Pokémon entró en juego, no en el primer turno del jugador).
  - Unir exactamente 1 carta de Energía por turno a cualquier Pokémon propio.
  - Jugar cartas de Entrenador: Objetos (sin límite), 1 Partidario por turno, 1 Estadio por turno.
  - Retirar el Pokémon Activo pagando su costo de retirada (1 vez por turno). Al retirarse, pierde todas las Condiciones Especiales.
  - Usar Habilidades (Abilities).
- **Fase 3 – Ataque:** El jugador puede atacar con su Pokémon Activo. *Excepción:* No disponible en el primer turno del jugador que empieza. El ataque finaliza el turno automáticamente.

## 3. Secuencia de Resolución de Ataque
1. Validar Energía requerida.
2. Si el Atacante está **Confundido**, lanzar moneda: cruz → el ataque falla, el atacante recibe 3 contadores de daño y el turno termina.
3. Se ejecutan los requisitos previos del ataque (selección de objetivos, lanzamientos de moneda de la carta).
4. Efectos que cancelan el ataque (ej: efectos de turnos anteriores).
5. **Cálculo de daño:**
   - Daño Base.
   - Modificadores del atacante.
   - Aplicar **Debilidad** del defensor (x2 al daño).
   - Aplicar **Resistencia** del defensor (-20 al daño. Mínimo 0).
   - Modificadores del defensor.
   - Colocar 1 contador de daño por cada 10 puntos de daño final.
6. Aplicar efectos posteriores al daño (ej: aplicar Envenenamiento, descartar energías del defensor).

## 4. Proceso de Knockout
- Un Pokémon queda Fuera de Combate cuando sus contadores de daño x 10 >= sus HP.
- El Pokémon y todas las cartas unidas a él se descartan a la Pila de Descarte.
- El oponente toma 1 carta de Premio (2 cartas si el Pokémon derrotado es un Pokémon-EX o Megaevolución).
- El dueño debe reemplazar el Pokémon Activo con uno de su Banca. Si no tiene en Banca, pierde la partida.

## 5. Condiciones Especiales (Status Effects)
- **Dormido:** Impide Atacar y Retirarse. *Between-turns:* Lanzar moneda (Cara = despierta, Cruz = sigue dormido).
- **Paralizado:** Impide Atacar y Retirarse. *Between-turns:* Se cura automáticamente al final del turno en que fue paralizado.
- **Confundido:** No impide retiro. Al intentar atacar, lanzar moneda (Cruz = ataque falla y atacante recibe 3 contadores de daño).
- **Quemado:** *Between-turns:* Lanzar moneda (Cruz = 2 contadores de daño).
- **Envenenado:** *Between-turns:* 1 contador de daño (Sin moneda).
- **Incompatibilidades:** Dormido, Confundido y Paralizado son excluyentes (la más reciente pisa a la anterior). Quemado y Envenenado pueden coexistir con cualquiera.
- **Orden Between-turns:** 1. Envenenado -> 2. Quemado -> 3. Dormido -> 4. Paralizado. Luego Abilities. Luego check de Knockouts.

## 6. Condiciones de Victoria
- Victoria por Premios: Tomar la última carta de Premio.
- Victoria por knockout total: El oponente no tiene Pokémon en Banca para reemplazar a su Activo derrotado.
- Derrota por mazo vacío: Al intentar robar carta al inicio del turno, el mazo está vacío.
