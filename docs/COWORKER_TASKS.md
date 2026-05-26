# Tareas para Coworkers (Actualizado)

Hola! Si llegaste hasta acá, bienvenido al proyecto **Monstra TCG**. Hemos avanzado un montón y pasamos de un simple prototipo a un MVP con partidas funcionales, Lobby, y WebSockets conectados al motor en Java. 

Acá tenés una lista de tareas reales que nos están haciendo falta para llegar a la versión final:

## 1. Refactorización del Frontend (Prioridad Alta)
El frontend ha crecido un montón. Si te fijás, `pokedex-page.ts` y `pokedex-page.html` tienen +800 y +1000 líneas de código respectivamente.
**Tu tarea:**
- Dividir `PokedexPage` en componentes más pequeños dentro de Angular (ej. `<app-battle-board>`, `<app-lobby>`, `<app-hand>`, etc.).
- Asegurarte de que la reactividad con `signal` se comparta correctamente entre componentes usando inyección de dependencias o Inputs/Outputs.

## 2. Implementar Torneos (Prioridad Media)
Si lees la `consigna.txt`, vas a ver que el sistema pide creación de torneos (Formato Suizo / Eliminación Directa). 
**Tu tarea:**
- Crear en el Backend un `TournamentController` (o similar) para la creación y visualización de torneos.
- Implementar la lógica de emparejamiento (Matchmaking) automático para un torneo activo.
- (Opcional) Hacer una vista en el Frontend para unirse a un torneo.

## 3. Dockerizar el Backend (Prioridad Media)
Actualmente el `docker-compose.yml` levanta la base de datos (PostgreSQL) y Adminer. Pero la API (Spring Boot) y el Front (Angular) se levantan a mano.
**Tu tarea:**
- Crear el `Dockerfile` en la carpeta `BE/` (usando una imagen de JDK 21).
- (Opcional) Crear el `Dockerfile` para el Frontend.
- Actualizar el `docker-compose.yml` en la raíz del proyecto para que con un simple `docker compose up -d` se levante TODO el entorno (DB, Backend, Frontend).

## 4. UI del Creador de Mazos (Deck Builder) (Prioridad Baja/Media)
En el backend los endpoints de Deck ya existen (creación y lectura), e incluso hay un `create_deck.json` en el front a modo de prueba.
**Tu tarea:**
- Armar una vista piola de "Deck Builder" en Angular, donde el usuario pueda ver todas las cartas disponibles (Paginación o Grid).
- Permitir arrastrar cartas hacia una "lista del mazo" hasta tener 60 cartas.
- Guardar el mazo creado pegándole a la API `POST /api/decks`.

## 5. Testing E2E o de Integración (Prioridad Baja)
Hemos hecho bastantes tests unitarios del engine, pero nos faltan tests funcionales end-to-end.
**Tu tarea:**
- Escribir tests de integración en Spring Boot utilizando `@SpringBootTest` para simular todo un flujo de partida, desde el login hasta el final de un duelo, verificando cómo responden los sockets.
- (Opcional) Sumar Playwright o Cypress en el frontend para testear la UI de la batalla.

---

**Nota:** Por favor, cualquier desarrollo hacelo utilizando **GitFlow**. Creá tu rama (ej: `feature/refactor-fe`), programá, verificá que los tests pasen y hacé un Pull Request a `develop`. ¡Éxitos!
