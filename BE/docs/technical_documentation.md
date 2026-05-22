# PokeTCG - Documentación Técnica: Chat, Reportes y Replay (Backend)

Este documento detalla la arquitectura técnica, seguridad, persistencia y endpoints implementados para el chat en vivo, reportes de comportamiento, replay de partidas, sistema de silencios, honores y penalizaciones avanzadas. Todo el código ha sido verificado localmente y se encuentra en la rama `feature/BE-chat-replay-auditory`.

---

## 1. Tabla Resumen de Commits

| Hash de Commit | Mensaje de Commit (Convencional) | Componente Principal | Archivos Creados / Modificados |
| :--- | :--- | :--- | :--- |
| **`82dcb49`** | `feat(persistence): add chat reports flyway migration` | Base de Datos (Flyway) | - `V3__chat_reports.sql` |
| **`86521d0`** | `test(auth): add auth integration tests and dashboard` | Dashboard Auxiliar (Test) | - `index.html` (dashboard de pruebas)<br>- Tests de integración de seguridad |
| **`c471d3a`** | `feat(auth): add websocket connection jwt interceptor` | Seguridad & WebSocket | - `WebSocketConfig.java` (Interceptor JWT)<br>- `WebSocketConfigTest.java` |
| **`9ee1d06`** | `feat(chat): implement chat service and in-memory cache` | Negocio de Chat (FIFO) | - `ChatMessageResponse.java`<br>- `ChatService.java`<br>- `ChatServiceImpl.java` |
| **`915b99e`** | `feat(chat): add chat websocket controller & REST history` | Comunicación WebSocket | - `ChatMessageRequest.java`<br>- `ChatWebSocketController.java`<br>- `ReplayController.java` (GET chat history) |
| **`c0620af`** | `feat(reports): implement behavior reports endpoint & persistence` | Reportes de Toxicidad (JSONB) | - `ChatReportRequest.java`<br>- `ChatReportEntity.java`<br>- `ChatReportRepository.java`<br>- `ReplayController.java` (POST report)<br>- Tests de servicio y controlador |
| **`c6f4311`** | `feat(replay): implement match replay & action logging` | Auditoría y Historial de Partida | - `ReplayEventDTO.java`<br>- `ReplayResponseDTO.java`<br>- `ReplayService.java`<br>- `ReplayServiceImpl.java`<br>- `ReplayController.java` (GET replay)<br>- `ReplayServiceImplTest.java` |
| **`54d27fc`** | `feat(chat): complete profanity filter logic and fix controller tests` | Moderación de Chat | - `ProfanityFilterService.java`<br>- `ProfanityFilterServiceImpl.java`<br>- `ProfanityFilterServiceTest.java` |
| **`dbb0c66`** | `feat(users): implement user muting system and rest endpoints` | Bloqueo de Usuarios | - `MuteService.java`<br>- `MuteServiceImpl.java`<br>- `UserController.java` (mute/unmute)<br>- `ReplayController.java` (filtros)<br>- Tests de silenciado |
| **`82f8f27`** | `feat(users): implement player honor system and REST endpoints` | Reconocimiento / Honor | - `HonorType.java`<br>- `HonorRequest.java`<br>- `HonorService.java`<br>- `HonorServiceImpl.java`<br>- `UserController.java` (endpoints honor)<br>- Tests de honores |
| **`f12e72e`** | `feat(replay): implement user profile replay library` | Biblioteca de Replays | - `UserMatchHistoryDTO.java`<br>- `MatchRepository.java` (query)<br>- `ReplayService/ServiceImpl.java`<br>- `ReplayController.java` (endpoint)<br>- Tests de replays por perfil |
| **`0771ce2`** | `feat(persistence): add isValidated flag to chat reports and database migration` | Persistencia y BD | - `V4__add_is_validated_to_chat_reports.sql`<br>- `ChatReportEntity.java` (isValidated)<br>- `ChatReportRepository.java` |
| **`cd6de62`** | `feat(reports): implement automated chat report validation logic` | Validación de Reportes | - `ChatServiceImpl.java` (validaciones automáticas de spam y profanidad) |
| **`e281e73`** | `feat(users): implement advanced penalty service with levels and menu notifications` | Penalizaciones Avanzadas | - `PenaltyService.java`<br>- `PenaltyServiceImpl.java`<br>- `UserStatusResponse.java`<br>- `UserController.java` |
| **`70ff1cd`** | `feat(match): integrate penalty deduction rules and web socket checks` | Integración del Juego | - `MatchService.java` (turn tracking y registerMatchFinished) |

---

## 2. Arquitectura de los Módulos

### A. Seguridad en WebSockets y Prevención de Impersonación
Para asegurar el canal de WebSockets sin alterar la base de datos ni los controladores de seguridad REST:
1. **Interceptor STOMP (JWT):** Implementado en `WebSocketConfig.java`. Captura el comando `CONNECT`, extrae el encabezado `Authorization: Bearer <token>`, valida el JWT vía `JwtUtil` y registra el token de autenticación en la sesión del cliente (`accessor.setUser(authentication)`).
2. **Controlador Seguro:** En `ChatWebSocketController`, el emisor de los mensajes no se extrae del payload de la petición HTTP/WebSocket (que podría ser falsificado). Se obtiene directamente inyectando `Principal principal` (`principal.getName()`), previniendo que un jugador envíe mensajes en nombre de otro.

### B. Caché en Memoria FIFO para el Chat
Para evitar consultar la base de datos de manera constante en cada mensaje de chat enviado en tiempo real:
- Implementado en `ChatServiceImpl` mediante un `ConcurrentHashMap` donde cada llave es un `matchId` y su valor es un `ConcurrentLinkedQueue<ChatMessageResponse>`.
- Posee un límite físico estricto de **50 mensajes** (FIFO). Al insertar el mensaje número 51, se deshecha el más antiguo, optimizando el consumo de memoria RAM del servidor.

### C. Almacenamiento en Base de Datos de Reportes (JSONB)
Cuando un jugador denuncia comportamiento inapropiado:
1. **Mapeo:** La entidad `ChatReportEntity.java` define la tabla `chat_reports`.
2. **Serialización JSONB:** El campo `chat_history` está anotado con `@JdbcTypeCode(SqlTypes.JSON)`. Esto permite que Hibernate guarde la lista completa de mensajes capturados como JSONB nativo en PostgreSQL.
3. **Flujo:** Al ejecutarse el reporte, se extrae el snapshot actual del chat de la cola en memoria RAM y se persiste en base de datos.

### D. Replay de Partidas (Auditoría)
- Cada acción significativa de la partida ya es persistida por el motor del juego en la tabla `match_logs`.
- `ReplayServiceImpl.java` recupera cronológicamente estas acciones utilizando la consulta ordenada por tiempo (`findByMatchIdOrderByCreatedAtAsc`).
- Transforma los datos crudos del log de base de datos en un listado limpio (`ReplayEventDTO`) listo para ser consumido por un reproductor de replay visual.

### E. Filtro de Palabras Ofensivas (Profanity Filter)
- Implementado en `ProfanityFilterServiceImpl.java` usando una lista negra predefinida de términos inapropiados.
- Utiliza expresiones regulares (`Regex`) con búsqueda insensible a mayúsculas/minúsculas y límites de palabra (`\b`) para evitar falsos positivos y reemplazar las palabras ofensivas por asteriscos (`***`).

### F. Sistema de Silenciado (Mute System)
- Permite a los usuarios silenciar/bloquear de forma local el chat de otros jugadores molestos.
- Almacenado en memoria mediante un `ConcurrentHashMap<String, Set<String>>`.
- El filtrado de mensajes se aplica en el controlador REST `ReplayController` al recuperar el historial del chat, quitando los mensajes de usuarios silenciados.

### G. Sistema de Honores Post-Partida (Honor System)
- Los jugadores pueden honrar a sus oponentes al finalizar las partidas según tres categorías: `GOOD_SPORTSMAN`, `FRIENDLY` y `GREAT_STRATEGIST`.
- Almacenado en memoria mediante un `ConcurrentHashMap<String, Map<HonorType, Integer>>` para registrar los conteos acumulados por usuario de forma eficiente y concurrente.

### H. Biblioteca de Replays por Perfil (User Replays)
- Permite recuperar la lista de todas las partidas jugadas por un usuario específico.
- Realiza una consulta personalizada (`MatchRepository.findMatchesByUsername`) buscando partidas donde el usuario sea tanto `player1` como `player2`, ordenadas de forma descendente por fecha de creación (`createdAt`).

### I. Validación Automatizada de Reportes de Chat (Evidencia)
Para evitar denuncias falsas o abusivas (rage reporting), cada reporte creado pasa por filtros automatizados:
1. **Filtro de Profanidad:** El historial de chat del denunciado se escanea contra el `ProfanityFilterService`. Si se detecta algún insulto, el reporte se marca como justificado (`isValidated = true`).
2. **Detección de Spam:** Si el denunciado envió **10 o más mensajes en un rango de 30 segundos**, se clasifica como spam/flooding y se valida.
3. Si no cumple ninguna de estas condiciones, el reporte se guarda como inválido (`isValidated = false`) y no afecta a las penalizaciones del denunciado.

### J. Sistema de Penalizaciones Avanzado por Niveles e Integración
Implementa penalizaciones incrementales en memoria en base al número de **reportes justificados** (`isValidated = true`) de un usuario:
* **Niveles de Penalización:**
  * **3 reportes:** Silenciado (Mute) del chat por **1 partida** completa.
  * **5 reportes:** Silenciado (Mute) del chat por **3 partidas** completas.
  * **8 reportes:** Suspensión completa de la cuenta por **3 días**.
  * **12 reportes:** Suspensión completa de la cuenta por **7 días**.
  * **20+ reportes:** Suspensión permanente (`PERMA_BANNED`).
* **Lógica del Descuento de Partidas Silenciadas:**
  * Para que una partida cuente y reste 1 al contador de silencio del infractor, debe finalizar de forma legítima:
    - Victoria o derrota normal.
    - O rendición del oponente **únicamente si** ambos jugadores ya habían jugado al menos **5 turnos cada uno**.
    - Si el jugador silenciado abandona o se desconecta voluntariamente antes, **no cuenta** para el descuento.
  * El conteo de turnos se realiza mediante un Turn Tracker en memoria en `MatchService.java` que monitorea las acciones y cambios de actor.

### K. Notificaciones y Advertencias Diferidas al Lobby (Menú)
Para no interrumpir la experiencia de juego en medio de una partida activa:
1. **Diferimiento:** Si un usuario es sancionado a raíz de reportes generados en una partida, la sanción queda como **pendiente** y no interrumpe el juego.
2. **Consolidación:** Se hace efectiva en el backend inmediatamente cuando finaliza la partida (`registerMatchFinished`).
3. **Notificación en el Lobby:** Cuando el usuario regresa al menú principal, el frontend llama a `/status`. El endpoint devuelve los mensajes de baneo pendientes en `pendingNotifications` y los limpia del servidor para evitar repeticiones.
4. **Advertencia de Reincidencia:** Al expirar una suspensión o silencio, el estado vuelve a `ACTIVE`. Al consultar el lobby, se activa la advertencia `showRecidivismWarning = true` mostrando el cartel: *"Si continúas con esta actitud, la penalización seguirá escalando hasta el baneo permanente."*, la cual también se limpia una vez leída.

---

## 3. Contratos de la API REST (Endpoints Implementados)

### 1. Obtener Historial de Chat
* **Ruta:** `GET /api/matches/{matchId}/chat`

### 2. Crear Reporte de Comportamiento
* **Ruta:** `POST /api/matches/{matchId}/reports`

### 3. Obtener Replay de Partida
* **Ruta:** `GET /api/matches/{matchId}/replay`

### 4. Silenciar / Desilenciar Usuario
* **Silenciar:** `POST /api/users/mute/{targetUsername}`
* **Desilenciar:** `DELETE /api/users/mute/{targetUsername}`
* **Obtener Silenciados:** `GET /api/users/mute`

### 5. Otorgar y Obtener Honores
* **Otorgar Honor:** `POST /api/users/{username}/honor`
* **Obtener Honores:** `GET /api/users/{username}/honor`

### 6. Historial de Replays de un Usuario
* **Ruta:** `GET /api/users/{username}/replays`

### 7. Consultar Estado de Penalización de Usuario (Lobby / Menú Principal)
* **Ruta:** `GET /api/users/{username}/status`
* **Seguridad:** Requiere JWT.
* **Respuesta (200 OK):**
  ```json
  {
    "status": "PENALIZED",
    "penaltyType": "MUTE",
    "matchesPenalizedRemaining": 3,
    "penaltyExpiration": null,
    "pendingNotifications": [
      "Has sido silenciado del chat por las próximas 3 partidas debido a comportamiento antideportivo."
    ],
    "showRecidivismWarning": false
  }
  ```
  *(Nota: Al consultar, las notificaciones y el flag de advertencia son limpiados en el backend para próximas consultas).*
