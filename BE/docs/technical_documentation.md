# PokeTCG - Documentación Técnica: Chat, Reportes y Replay (Backend)

Este documento detalla la arquitectura técnica, seguridad, persistencia y endpoints implementados para el chat en vivo, reportes de comportamiento y replay de partidas. Todo el código ha sido verificado localmente y se encuentra en la rama `feature/BE-chat-replay-auditory`.

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

---

## 3. Contratos de la API REST (Endpoints Implementados)

### 1. Obtener Historial de Chat
* **Ruta:** `GET /api/matches/{matchId}/chat`
* **Seguridad:** Requiere JWT.
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
* **Respuesta:** `200 OK` (Vacio).

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
      },
      {
        "turn": 1,
        "player": "ash_ketchum",
        "action": "PLAY_POKEMON",
        "result": "Played Pikachu to active bench",
        "timestamp": "2026-05-22T19:28:10"
      }
    ]
  }
  ```
