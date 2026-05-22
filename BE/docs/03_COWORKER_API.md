# 🤝 API para Coworkers (Módulos 4 y externos)

Este documento va dirigido a los desarrolladores de otros módulos (Logs, Controllers WebSocket, Persistence) que necesitan integrarse con el Engine Core.

## 1. Entrada al Motor: `GameFacade`
**La única clase que deben inyectar o instanciar es `MatchSession`.** 
`MatchSession` contiene `getGameFacade()` que es el punto de entrada oficial para todas las acciones de los jugadores.

### Ejemplo de uso desde un Controller WS:
```java
public void onPlayerAction(String sessionId, ActionRequestDTO dto) {
    MatchSession session = registry.getSession(sessionId);
    GameFacade facade = session.getGameFacade();
    
    // El facade devuelve un ValidationResult. Valid o Invalid.
    ValidationResult result = facade.processAction(dto);
    
    if (result instanceof ValidationResult.Invalid invalid) {
        // Enviar error al cliente WebSocket
        webSocketService.sendError(invalid.reason());
    } else {
        // La acción se aplicó con éxito.
        // Leer el nuevo estado de MatchBoard y hacer broadcast a los clientes
        MatchBoard board = session.getMatchBoard();
        webSocketService.broadcastBoard(board);
    }
}
```

## 2. Lo que falta por cablear (Tareas Pendientes de Integración)

### 2.1. Inyección de Logs
El Módulo 4 proveerá un Logger. Actualmente el Engine Core emite acciones y notificaciones.
- **Tu Tarea:** Cuando el Módulo 4 exponga la interfaz del Logger, inyéctenla en `GameFacade` o en el `PhaseListener` para registrar cada paso (por ejemplo: "El Jugador 1 robó una carta", "Pikachu usó Impactrueno por 20 de daño").

### 2.2. Manejo de Fin de Partida
El Engine dispara eventos de victoria (DeckOut, Knockout, Prizes). 
- **Tu Tarea:** El `VictoryHandler` provisto por fuera del engine debe ser implementado para actualizar la BD (tabla de matches históricos), calcular ELO (si aplica) y notificar por WS el fin del partido.

> [!WARNING]
> **RECORDATORIO CONSTANTE:** El Engine Core NUNCA debe guardar en base de datos. Si ustedes necesitan guardar un log en SQL o un estado de partida en Redis, deben hacerlo DESDE SUS CAPAS EXTERNAS leyendo el estado del `MatchBoard`. El Engine no toca la BD.
