# TPI Pokémon TCG - Documentación Técnica y Arquitectura

Bienvenido a la documentación oficial del proyecto. Esta documentación ha sido diseñada **tanto para desarrolladores humanos como para Agentes de IA (LLMs)**. Cada archivo detalla clases, flujos de datos y convenciones de diseño estrictas que deben respetarse.

## Directorio de Documentación

### 1. [Arquitectura y Flujo de Datos](docs/01_arquitectura_y_flujo.md)
Detalla la separación entre el Cliente (Angular) y el Servidor (Spring Boot), el uso intensivo de WebSockets (STOMP) para el Game Engine, y cómo se gestiona la concurrencia (`ReentrantLock`) y la persistencia (`MatchSessionJsonConverter`).

### 2. [Sistemas Core y Reglas de Negocio](docs/02_sistemas_core.md)
Contiene la lógica de dominio del proyecto:
- **Game Engine:** Fases del turno, mecánicas de daño, tipos de cartas (Pokémon, Entrenadores, Energía) y condiciones de victoria.
- **Matchmaking y MMR (Rankeds):** Implementación de la fórmula Elo, Ligas y el reset de temporadas.
- **Sistemas Sociales:** Sistema de amigos, chat en vivo, reportes de toxicidad y penalizaciones automáticas (`PenaltyService`).

### 3. [Guía de Desarrollo, CI y Setup](docs/03_guia_desarrollo_y_setup.md)
Instrucciones para levantar el proyecto localmente, comandos de Maven y NPM, convenciones de código (Checkstyle) y Testing.

### 4. [Architecture Decision Records](docs/adr/README.md)
Por qué el motor está aislado con Clean/Hexagonal Architecture, por qué la sincronización es server-authoritative vía STOMP, y el diseño de persistencia atómica del ganador.

Listado de todo lo que queda por hacer (Sistema Económico, Sobres, Pase de Batalla). **Los agentes de IA deben consultar este archivo para entender el contexto futuro y las dependencias de features.**
