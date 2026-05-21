# Backlog de Tareas para el Equipo (Coworkers)

Este documento contiene un catálogo extenso de funcionalidades periféricas y de infraestructura. Están pensadas para que cualquier integrante del equipo pueda tomar una tarea, sumar contribuciones importantes al proyecto y ganar puntos para la evaluación final, **sin interferir ni romper el motor de reglas (Game Engine) que ya está construido.**

Por favor, al elegir una tarea, respeta estrictamente los lineamientos de arquitectura: Cero lógica de base de datos dentro del paquete `engine`, uso de *Conventional Commits* y TDD donde corresponda.

---

## 1. Seguridad, Auth y JWT (Módulo de Usuarios)
**Complejidad:** Alta
**Resumen:** Sistema de registro y login para identificar unívocamente a cada jugador.
**Cómo implementarlo:**
1. Usar Spring Boot Starter Security y JWT (`jjwt`).
2. Mapear `UserEntity` a la tabla `users` (ya existe en Flyway).
3. Configurar un `SecurityFilterChain` en `STATELESS` deshabilitando CSRF.
4. Crear un endpoint `POST /api/auth/register` (hasheando con `BCryptPasswordEncoder`) y `POST /api/auth/login`.
5. Crear un `JwtAuthenticationFilter` que valide el token en el header `Authorization` de todas las llamadas `/api/**`.
**Mejores prácticas:** Nunca devolver el password en un JSON. Manejar las excepciones de credenciales inválidas tirando un `BadCredentialsException` y mapeándolo con `@ControllerAdvice` a un `HTTP 401`.

## 2. Lobby y Matchmaking
**Complejidad:** Media
**Resumen:** Salas de espera para encontrar oponente antes de iniciar una partida.
**Cómo implementarlo:**
1. Crear `LobbyController` con soporte HTTP.
2. `POST /api/lobby` crea una partida en estado `WAITING` (tabla `matches`).
3. `GET /api/lobby` lista todas las partidas `WAITING`.
4. `POST /api/lobby/{id}/join` permite a un segundo jugador unirse, pasando el estado a `SETUP` y notificando a ambos por WebSocket.
**Mejores prácticas:** Validar que un jugador no pueda unirse a su propia sala, y que no pueda unirse si no tiene un mazo válido creado.

## 3. Chat en Vivo (In-Game)
**Complejidad:** Media
**Resumen:** Comunicación por mensajes de texto durante la partida. Cumple con el opcional de la consigna.
**Cómo implementarlo:**
1. Usar WebSockets con STOMP (`@EnableWebSocketMessageBroker`).
2. Crear un `ChatController` con `@MessageMapping("/chat/{matchId}")`.
3. Re-transmitir el mensaje decorado con un *timestamp* hacia el canal `@SendTo("/topic/chat/{matchId}")`.
**Mejores prácticas:** No guardar el chat en la base de datos para no saturar el I/O, mantenerlo efímero en la sesión de WebSockets. 

## 4. Ranking Global y Estadísticas (Leaderboard)
**Complejidad:** Baja
**Resumen:** Exponer los mejores jugadores del sistema basados en porcentaje de victorias. Cumple requisito opcional.
**Cómo implementarlo:**
1. Crear una consulta en `MatchRepository` (JPQL) que agrupe por `winner_id` y cuente la cantidad de registros donde `status = 'FINISHED'`.
2. Exponer un `GET /api/rankings?limit=10`.
**Mejores prácticas:** Usar una proyección (`Interface` o `DTO` directo en el `@Query` de JPA) para no cargar la entidad completa en memoria.

## 5. Auditoría y Sistema de Replay
**Complejidad:** Media
**Resumen:** Transformar los logs puros de la base de datos en un "Replay" para ver partidas finalizadas. (Exigido en RF-05).
**Cómo implementarlo:**
1. Exponer `GET /api/matches/{id}/replay`.
2. Leer todos los `MatchLogEntity` ordenados por `turn_number` y `created_at`.
3. Transformar los comandos duros (ej. `ATTACK`) en strings amigables para el front ("Ash atacó con Pikachu").
**Mejores prácticas:** Paginación de Spring Data (`Pageable`) si los logs exceden las 200 acciones.

## 6. Sincronizador Automático de Expansiones
**Complejidad:** Media / Alta
**Resumen:** Sincronizar nuevas cartas desde `pokemontcg.io` periódicamente para cumplir con el bonus track de "Soporte para múltiples expansiones".
**Cómo implementarlo:**
1. Usar `@EnableScheduling`.
2. Crear un `CardSyncJob` con `@Scheduled(cron = "0 0 3 * * ?")` (todos los días a las 3 AM).
3. Consultar la API oficial iterando sobre las páginas, descargar las cartas nuevas y hacer upsert (update o insert) en la tabla `cards`.
**Mejores prácticas:** Respetar los *Rate Limits* de la API oficial (usar Thread.sleep si no se tiene un token premium). Usar `@Transactional` en bloques lógicos.

## 7. CI/CD: Integración y Entrega Continua
**Complejidad:** Media
**Resumen:** Automatizar los tests, el análisis de código y facilitar la revisión para los profesores.
**Cómo implementarlo:**
1. Crear archivo `.github/workflows/build.yml`.
2. Configurar pasos para instalar Java 21, ejecutar `mvn clean verify` (corriendo JaCoCo para la cobertura).
3. Romper el build automáticamente si Checkstyle o PMD tiran warnings.
**Mejores prácticas:** Es la mejor forma de asegurar que nadie en el equipo rompa código existente con un Pull Request descuidado.

## 8. Dockerización del Entorno
**Complejidad:** Baja
**Resumen:** Empaquetar todo para levantar el entorno con un solo clic. Ideal para la defensa del TPI.
**Cómo implementarlo:**
1. Escribir un `Dockerfile` multietapa para compilar con Maven y empaquetar con `eclipse-temurin:21-jre`.
2. Crear un `docker-compose.yml` que defina un servicio para PostgreSQL (configurado con los mismos usuarios del `application-dev.properties`) y un servicio para el backend.
**Mejores prácticas:** Depender del `healthcheck` de PostgreSQL para que el backend de Spring no levante antes de que la BD esté lista.

## 9. Modo Espectador (Observer Pattern)
**Complejidad:** Baja
**Resumen:** Permitir a usuarios de la plataforma ver partidas ajenas en tiempo real.
**Cómo implementarlo:**
1. Permitir que usuarios que no son `player1_id` ni `player2_id` se suscriban al topic STOMP `/topic/match/{id}`.
2. Validar a nivel de `@MessageMapping` que cualquier comando de juego entrante desde ese usuario devuelva un `Unauthorized`.
**Mejores prácticas:** Es un subproducto gratuito de la arquitectura de WebSockets, sólo requiere blindar la entrada de comandos.

## 10. Swagger / OpenAPI Documentation
**Complejidad:** Baja
**Resumen:** Documentar interactivamente todos los endpoints para que el desarrollador del Front End (y el profe) sepa cómo consumirlos.
**Cómo implementarlo:**
1. Añadir dependencia `springdoc-openapi-starter-webmvc-ui`.
2. Anotar todos los controladores (DeckBuilder, Auth, Lobby, Ranking) con `@Operation` y `@ApiResponse`.
3. Configurar globalmente la seguridad JWT en Swagger para que el candadito funcione en la UI.
**Mejores prácticas:** Usar descripciones detalladas de los DTOs y evitar fugas de información interna en los esquemas.

## 11. Recuperación de Contraseña con Spring Mail
**Complejidad:** Baja / Media
**Resumen:** Feature clásica de producción.
**Cómo implementarlo:**
1. Crear `POST /api/auth/forgot-password`.
2. Generar un UUID, guardarlo asociado al usuario con fecha de expiración.
3. Usar `JavaMailSender` para enviar un email por SMTP (ej. Mailtrap para desarrollo) con un link de reseteo.
4. Crear `POST /api/auth/reset-password` para validar el UUID y cambiar la clave.
**Mejores prácticas:** Guardar el token hasheado en BD, no en texto plano. Nunca decir "el correo no existe" por temas de enumeración de usuarios (devolver HTTP 200 siempre).

---

**NOTA:** Antes de arrancar cualquier tarea, crear una rama tipo `feature/BE-auth`, y realizar commits granulares (Convencionales). Al finalizar, abrir un PR (Pull Request) para revisión de al menos un compañero antes del merge a `develop`.
