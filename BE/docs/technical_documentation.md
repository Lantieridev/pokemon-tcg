# PokeTCG - Documentación Técnica: Chat, Reportes y Replay (Backend)

Este documento detalla la arquitectura técnica, seguridad, persistencia y endpoints implementados para el chat en vivo, reportes de comportamiento, replay de partidas, sistema de silencios, honores y penalizaciones automáticas. Todo el código ha sido verificado localmente y se encuentra en la rama `feature/BE-chat-replay-auditory`.

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
| **`da89ef0`** | `feat(users): implement automatic penalty system based on chat reports` | Sanciones Automáticas | - `UserStatusResponse.java`<br>- `PenaltyService.java`<br>- `PenaltyServiceImpl.java`<br>- `UserController.java` (endpoint estado)<br>- `ChatWebSocketController.java`<br>- Tests de penalizaciones |

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
2. **Serialización JSONB:** El campo `chat_history` está anotado con `@JdbcTypeCode(SqlTypes.JSON)`. Esto permite que Hibernate guarde la lista completa de mensajes capturados como JSONB nativo en PostgreSQL (y texto/JSON formateado transparente en H2 para tests de integración).
3. **Flujo:** Al ejecutarse el reporte, se extrae el snapshot actual del chat de la cola en memoria RAM y se persiste en base de datos.

### D. Replay de Partidas (Auditoría)
- Cada acción significativa de la partida ya es persistida por el motor del juego en la tabla `match_logs`.
- `ReplayServiceImpl.java` recupera cronológicamente estas acciones utilizando la consulta ordenada por tiempo (`findByMatchIdOrderByCreatedAtAsc`).
- Transforma los datos crudos del log de base de datos en un listado limpio (`ReplayEventDTO`) listo para ser consumido por un reproductor de replay visual.

### E. Filtro de Palabras Ofensivas (Profanity Filter)
- Implementado en `ProfanityFilterServiceImpl.java` usando una lista negra predefinida de términos inapropiados (insultos comunes, etc.).
- Utiliza expresiones regulares (`Regex`) con búsqueda insensible a mayúsculas/minúsculas y límites de palabra (`\b`) para evitar falsos positivos y reemplazar las palabras ofensivas por asteriscos (`***`).
- Integrado de forma transparente en el controlador del WebSocket `ChatWebSocketController` para filtrar los mensajes en tiempo real antes de guardarlos y transmitirlos.

### F. Sistema de Silenciado (Mute System)
- Permite a los usuarios silenciar/bloquear de forma local el chat de otros jugadores molestos.
- Almacenado en memoria mediante un `ConcurrentHashMap<String, Set<String>>` para mapear de manera eficiente qué usuario ha bloqueado a cuáles otros.
- El filtrado de mensajes se aplica en el controlador REST `ReplayController` al recuperar el historial del chat (`GET /api/matches/{matchId}/chat`), quitando los mensajes de usuarios silenciados basándose en la identidad del usuario solicitante extraída del token JWT (`Principal`).

### G. Sistema de Honores Post-Partida (Honor System)
- Los jugadores pueden honrar a sus oponentes/compañeros al finalizar las partidas según tres categorías (`HonorType`): `GOOD_SPORTSMAN` (Buen deportista), `FRIENDLY` (Amigable) y `GREAT_STRATEGIST` (Gran estratega).
- Almacenado en memoria mediante un `ConcurrentHashMap<String, Map<HonorType, Integer>>` para registrar los conteos acumulados por usuario de forma eficiente y concurrente.
- Incluye validación de negocio para prevenir que un usuario se honre a sí mismo.

### H. Biblioteca de Replays por Perfil (User Replays)
- Permite recuperar la lista de todas las partidas jugadas por un usuario específico (historial de partidas).
- Realiza una consulta personalizada (`MatchRepository.findMatchesByUsername`) buscando partidas donde el usuario sea tanto `player1` como `player2`, ordenadas de forma descendente por fecha de creación (`createdAt`).
- Expone los detalles mediante un DTO limpio (`UserMatchHistoryDTO`).

### I. Sistema de Penalización Automática y Estado de Usuario
- Penaliza de forma automatizada a aquellos usuarios que acumulan comportamiento inapropiado.
- Al consultar el estado del usuario o intentar chatear, el sistema verifica en la base de datos si el usuario tiene **3 o más reportes acumulados**.
- Si el usuario cumple con esta condición de toxicidad y no posee una penalización activa en memoria, se le aplica una **penalización temporal de 5 minutos** en memoria y se registra para evitar penalizaciones redundantes si no llegan nuevos reportes.
- Durante la penalización:
  1. Su endpoint de estado (`GET /api/users/{username}/status`) retornará `"PENALIZED"` con la fecha y hora de expiración de la sanción.
  2. En el controlador de chat websocket (`ChatWebSocketController`), si el usuario intenta enviar un mensaje, este es bloqueado y descartado sin ser difundido, retornándole de vuelta un mensaje de advertencia del sistema (`SYSTEM`).

---

## 3. Contratos de la API REST (Endpoints Implementados)

### 1. Obtener Historial de Chat
* **Ruta:** `GET /api/matches/{matchId}/chat`
* **Seguridad:** Requiere JWT. Filtra de forma automática los mensajes provenientes de usuarios silenciados por el usuario autenticado.
* **Respuesta (200 OK):**
  ```json
  [
    {
      "sender": "ash_ketchum",
      "message": "¡Buena jugada!",
      "timestamp": "2026-05-22T19:30:15"
    }
  ]
  ```

### 2. Crear Reporte de Comportamiento
* **Ruta:** `POST /api/matches/{matchId}/reports`
* **Seguridad:** Requiere JWT.
* **Cuerpo de Petición (`ChatReportRequest`):**
  ```json
  {
    "reporterId": 1,
    "reportedId": 2,
    "reason": "Lenguaje ofensivo continuo"
  }
  ```
* **Respuesta:** `200 OK` (Vacío).

### 3. Obtener Replay de Partida
* **Ruta:** `GET /api/matches/{matchId}/replay`
* **Seguridad:** Requiere JWT.
* **Respuesta (200 OK):**
  ```json
  {
    "matchId": 123,
    "events": [
      {
        "turn": 1,
        "player": "ash_ketchum",
        "action": "DRAW_CARD",
        "result": "Drew Pikachu",
        "timestamp": "2026-05-22T19:28:01"
      }
    ]
  }
  ```

### 4. Silenciar / Desilenciar Usuario
* **Silenciar:** `POST /api/users/mute/{targetUsername}`
* **Desilenciar:** `DELETE /api/users/mute/{targetUsername}`
* **Obtener Silenciados:** `GET /api/users/mute`
* **Seguridad:** Requiere JWT.
* **Respuesta (200 OK):** Vacío (en POST/DELETE) o listado de nombres de usuario silenciados en GET.

### 5. Otorgar y Obtener Honores
* **Otorgar Honor:** `POST /api/users/{username}/honor`
  * **Cuerpo de Petición (`HonorRequest`):**
    ```json
    {
      "honorType": "GOOD_SPORTSMAN"
    }
    ```
* **Obtener Honores:** `GET /api/users/{username}/honor`
* **Seguridad:** Requiere JWT.
* **Respuesta (200 OK) en GET:**
  ```json
  {
    "GOOD_SPORTSMAN": 2,
    "FRIENDLY": 5,
    "GREAT_STRATEGIST": 1
  }
  ```

### 6. Historial de Replays de un Usuario
* **Ruta:** `GET /api/users/{username}/replays`
* **Seguridad:** Requiere JWT.
* **Respuesta (200 OK):**
  ```json
  [
    {
      "matchId": 10,
      "player1": "ash_ketchum",
      "player2": "gary_oak",
      "winner": "ash_ketchum",
      "createdAt": "2026-05-22T20:00:00"
    }
  ]
  ```

### 7. Consultar Estado de Penalización de Usuario
* **Ruta:** `GET /api/users/{username}/status`
* **Seguridad:** Requiere JWT.
* **Respuesta (200 OK - Activo):**
  ```json
  {
    "status": "ACTIVE",
    "penaltyExpiration": null
  }
  ```
* **Respuesta (200 OK - Penalizado):**
  ```json
  {
    "status": "PENALIZED",
    "penaltyExpiration": "2026-05-22T23:15:00"
  }
  ```
