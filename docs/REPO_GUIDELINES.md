# Guía de Uso del Repositorio y Convenciones (TPI Pokémon TCG)

Dado que somos un equipo de 5 personas desarrollando en paralelo, es **crítico** seguir estas reglas para no generar conflictos, no romper el código del compañero y mantener la sanidad mental del equipo.

## 1. Arquitectura General y Límites Estrictos
- **NUNCA** se mezcla la lógica del `GameEngine` con clases de WebSockets, HTTP Controllers o Repositories de Base de datos. El motor debe ser testeable sin levantar un contexto de Spring.
- **Backend (Java 21 + Spring Boot):** 
  - Usar patrones SOLID. Si hay un `if/else` gigante para efectos de cartas, está mal diseñado (usar Strategy Pattern).
  - El testing es obligatorio (JUnit + Mockito). No se sube código sin tests al menos para los validadores de reglas (90% coverage requerido por consigna).
- **Frontend (Angular 21+):**
  - Componentes **Standalone** exclusivamente.
  - Usar **Signals** (`signal()`, `computed()`) y el nuevo Control Flow (`@if`, `@for`). Prohibido usar `*ngIf`.
  - Interfaces estrictas para los payloads que vienen del backend.

## 2. Flujo de Git (GitFlow Simplificado)
Trabajaremos con un esquema basado en ramas de features (Feature Branch Workflow).

- **`main`:** Código de producción, estable y evaluable. **Prohibido pushear directamente a main.**
- **`develop`:** Rama de integración. Aquí converge el trabajo de todos.

### Cómo trabajar en un ticket/feature:
1. Asegurate de estar en la última versión de develop:
   ```bash
   git checkout develop
   git pull origin develop
   ```
2. Crea tu rama descriptiva:
   ```bash
   # Prefijos: feature/ (nuevas cosas), fix/ (errores), docs/ (documentación), test/ (pruebas)
   git checkout -b feature/BE-engine-damage-calculator
   ```
3. Trabaja en tu código. Haz commits atómicos (ver punto 3).
4. Sincroniza con develop ANTES de hacer el PR (para resolver conflictos localmente):
   ```bash
   git checkout develop
   git pull origin develop
   git checkout feature/BE-engine-damage-calculator
   git rebase develop # o merge si prefieren
   ```
5. Pushea y crea un Pull Request (PR) hacia `develop`.
6. **Regla de Oro:** Todo PR debe ser revisado y aprobado por al menos **1 compañero (ideal 2)** antes de mergear. Nunca auto-mergear.

## 3. Convenciones de Commits (Conventional Commits)
Los mensajes de commit deben decir QUÉ se hizo y POR QUÉ. No sirve "cambios" o "fix bug".
Formato: `tipo(ámbito): descripción corta`

Tipos permitidos:
- `feat`: Nueva funcionalidad.
- `fix`: Resolución de un bug.
- `docs`: Cambios en la documentación.
- `style`: Formateo, falta de punto y coma, etc; sin cambios de código.
- `refactor`: Refactorización de código de producción.
- `test`: Añadir o modificar pruebas.
- `chore`: Actualizar tareas de construcción, gestor de paquetes, etc; sin cambios de código.

**Ejemplos correctos:**
- `feat(engine): implementar cálculo de debilidad y resistencia`
- `fix(websocket): corregir desincronización de estado tras retiro de pokemon`
- `test(rule-validator): añadir tests para condición especial 'Dormido'`

## 4. Manejo de la Base de Datos
- **Prohibido modificar el esquema de DB a mano**. 
- Usaremos Flyway o Liquibase para migraciones.
- Los seeders (cartas iniciales del set xy1) se guardarán en un `V2__seed_xy1_cards.sql`.

## 5. Prácticas de Equipo e IA
- Las IAs (como este asistente) asumen convenciones basadas en el prompt. Siempre pegá la especificación del módulo que vas a desarrollar en el prompt de la IA.
- Antes de que la IA genere 300 líneas de código, pedile que te explique el **enfoque arquitectónico**. 
- Vos (humano) sos el responsable del código que hace merge a `develop`, no la IA. Leé lo que empujás.
