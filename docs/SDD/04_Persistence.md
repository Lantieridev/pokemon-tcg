# SDD Módulo 4: Persistencia y Acceso a Datos

**Responsables:** [Asignar 1 desarrollador]

Para mantener la inmutabilidad y la capacidad de auditoría/reconexión descritas en RF-05, el almacenamiento en base de datos debe diseñarse cuidadosamente. 

## Objetivo
Diseñar un modelo relacional en PostgreSQL (recomendado por soporte a tipos JSON) que guarde usuarios, mazos y el historial (snapshots/logs) de las partidas.

## Modelado de Datos

### 1. Tablas Core
- `users`: Credenciales e información del jugador.
- `cards`: El set `xy1` precargado y cacheado. Debería usar tipos JSONB en Postgres para almacenar los arreglos de ataques (con sus costos) y habilidades, a fin de evitar tablas relacionales masivas para datos estáticos leídos del API.
- `decks`: Lista de IDs de cartas asociados a un usuario.

### 2. Tablas de Partida (Game Session)
- `matches`: ID, estado (`WAITING`, `SETUP`, `ACTIVE`, `FINISHED`), ganador.
- **Opción A (Snapshotting):** Una tabla `match_snapshots` que guarda el JSON global de la partida después de cada turno o acción relevante. Permite recuperar la partida instantáneamente al reconectar (recomendado por simplicidad).
- **Opción B (Event Sourcing):** Una tabla `match_logs` (o `Action Log` según consigna RF-05) inmutable que registre cada evento: `Turno | Jugador | Tipo Acción | Resultado`. Para reconstruir la partida, se cargan todos los eventos.

La consigna exige que "El registro de acciones (log) debe ser completo e inmutable". Se recomienda una tabla `match_logs` para el historial y una columna JSONB en la tabla `matches` llamada `current_state` para la lectura rápida.

### 3. Consideraciones Técnicas
- Spring Data JPA para los Repositories.
- Manejo de transacciones (`@Transactional`) al guardar las acciones para evitar inconsistencias si el Websocket falla.
- Migraciones con Flyway (`V1__init.sql`, `V2__seed_xy1_cards.sql`).
