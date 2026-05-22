# 📖 La Biblia: Rulebook XY1 y Consigna Oficial

> [!CAUTION]
> **ESTE DOCUMENTO ES LA LEY SUPREMA DEL PROYECTO.**
> Cualquier IA, desarrollador o coworker que intente modificar el motor debe verificar que sus cambios no violen NADA de lo que está escrito aquí. **NO SE ACEPTAN DELIRIOS NI INVENTOS.**

## 1. El Core: Separación de Responsabilidades (La Consigna)
Nuestra consigna oficial nos prohíbe absolutamente mezclar responsabilidades de base de datos dentro de la lógica del juego puro.
- El paquete `ar.edu.utn.frc.tup.piii.engine` **NUNCA** debe importar clases de JPA, Repositorios, ni anotaciones de Spring (`@Service`, `@Autowired`, etc.).
- Todo el motor debe operar sobre POJOs (Plain Old Java Objects).
- La base de datos es solo para persistir estados *antes* y *después* del match. El juego corre en memoria.

## 2. Reglas Estrictas del TCG (Basado en Rulebook XY1)

### 2.1. El Primer Turno
- **Lanzamiento de Moneda:** El jugador que gana elige si va primero o segundo.
- **Robo de Cartas:** Al iniciar su turno, el jugador DEBE robar una carta. **El jugador que va primero SÍ roba una carta en su primer turno** (esta regla fue modificada en BW y se mantiene en XY).
- **Ataque:** El jugador que va primero **NO PUEDE ATACAR** en su primer turno.
- **Evolución:** Ningún jugador puede evolucionar un Pokémon en su primer turno, ni a un Pokémon que acaba de ser bajado en ese mismo turno.

### 2.2. Mulligans (Mano sin Básicos)
- Si un jugador roba sus 7 cartas iniciales y no tiene un Pokémon Básico, debe mostrar su mano, barajar y robar 7 cartas de nuevo.
- Por cada Mulligan que hace un jugador, su oponente tiene el derecho de robar 1 carta adicional.
- **Cross-Mulligan (Resolución Neta):** Si ambos jugadores hacen Mulligan a la vez, se anulan mutuamente 1 a 1. Solo la "diferencia neta" de Mulligans otorga robo al jugador con menor cantidad de Mulligans. (Implementado estrictamente en `SetupManager`).

### 2.3. La Banca y el Activo
- **Límite de Banca:** Máximo 5 Pokémon en la banca.
- **Condiciones Especiales (Status Effects):** Las Condiciones Especiales (Veneno, Quemadura, Confusión, Dormido, Paralizado) **SOLO AFECTAN AL POKÉMON ACTIVO**.
- **Limpieza de Estados:** Si el Pokémon Activo evoluciona o se retira a la Banca, pierde TODAS las Condiciones Especiales.
- **Evolución en la Banca:** Se puede evolucionar un Pokémon que está en la Banca. **Esto NO limpia los estados del Pokémon Activo**.

### 2.4. Coste de Retirada (Retreat Cost)
- Para retirarse, el jugador debe descartar Cartas de Energía adjuntas al Pokémon Activo hasta igualar el coste de retirada.
- **ELECCIÓN DEL JUGADOR:** El jugador *elige exactamente* qué cartas de energía descartar. El motor no debe borrar energías arbitrariamente.

### 2.5. Victoria
- **Take all Prize Cards:** Tomar las 6 cartas de premio.
- **Knock Out:** Si el oponente no tiene Pokémon Activo ni Pokémon en la Banca para reemplazarlo.
- **Deck Out:** Si el oponente debe robar una carta al comienzo de su turno y no le quedan cartas en el mazo.
