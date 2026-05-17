---
name: pokemon-persistence-strict
description: Skill obligatoria para el desarrollo del módulo de Base de Datos y Persistencia (PostgreSQL + Spring Data JPA).
---

# Pokémon TCG - Persistence Guidelines

## 1. Patrón de Diseño: Inmutabilidad (Event Sourcing Light)
- **Prohibido hacer UPDATE al GameState mutándolo.** 
- La tabla de `Game` debe guardar un log de acciones inmutables (ej: Jugador 1 ataca, Jugador 2 roba carta).
- Para el estado actual del tablero, se debe guardar un Snapshot usando el tipo de dato **JSONB** nativo de PostgreSQL. No intentes mapear las cartas de la banca a tablas relacionales (`Banca`, `CartaEnBanca`, etc.), es un anti-patrón para juegos de cartas.

## 2. Herramientas Obligatorias
- **Flyway:** No se debe usar `hibernate.hbm2ddl.auto=update`. Todo cambio en la base de datos debe ser un script `.sql` en la carpeta de migraciones de Flyway.
- **Spring Data JPA:** Se debe usar con el driver de PostgreSQL. 

## 3. Consultas (Queries)
- Prevenir el problema de N+1 queries. Si necesitas cargar un mazo de cartas de un usuario, usa `JOIN FETCH` o EntityGraphs en el repositorio.
- El Seed Data (cartas iniciales) debe cargarse vía scripts V2__ insert.sql de Flyway, asegurando que las 146 cartas del Set XY1 estén siempre disponibles sin llamar a una API externa durante el runtime del juego.
