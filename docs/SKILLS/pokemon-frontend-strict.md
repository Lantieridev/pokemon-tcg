---
name: pokemon-frontend-strict
description: Skill base y obligatoria para el desarrollo del Frontend del Pokémon TCG usando Angular 21+, Signals y Smart/Dumb Components.
---

# Pokémon TCG - Frontend Guidelines

## 1. Patrón Arquitectónico: Smart & Dumb Components
- **Container Components (Smart):** Son los únicos que pueden inyectar servicios (`MatchService`, `WebSocketService`). Se encargan de la lógica pesada, mantener el estado global y despachar eventos. Ej: `GameBoardComponent`.
- **Presentational Components (Dumb):** 100% aislados. SOLO reciben datos a través de `input()` y emiten eventos a través de `output()`. No inyectan servicios. Ej: `PlayerBenchComponent`, `ActivePokemonCardComponent`. 
- **Regla:** Si un componente visual (Dumb) está llamando a un servicio REST o WebSocket, está mal diseñado y el PR debe ser rechazado.

## 2. Gestión de Estado y Angular 21+
- Uso **obligatorio** de Signals. Prohibido el uso de RxJS para el estado local del componente (solo se permite RxJS para el manejo de los WebSockets o llamadas HTTP).
- Usar `computed()` para valores derivados (ej. "cantidad de energía total unida al Pokémon").
- **Control Flow Nativo:** Obligatorio usar `@if`, `@for`, `@switch`. Está terminantemente prohibido usar las directivas legacy `*ngIf` o `*ngFor`.
- Componentes 100% **Standalone**.

## 3. Estilos y Animaciones (Híbrido)
- Se permite Tailwind CSS para la maquetación estructural y posicionamiento (flex, grid, espaciados).
- **SCSS Puro para Animaciones:** Las animaciones complejas (rotar cartas para estados como Dormido/Confundido, efectos de hover premium inspirados en repos públicos) deben aislarse en archivos `.scss` encapsulados por componente, usando transformaciones de hardware acelerado (`transform: translate3d`, `rotate`, `scale`).

## 4. Performance y Ruteo (Lazy Loading)
- **Lazy Loading estricto:** Todas las rutas principales (`/lobby`, `/deck-builder`, `/match/:id`) deben cargarse mediante `loadComponent`. No se debe importar el componente directamente en el router principal para evitar inflar el bundle inicial.
- **Optimización de imágenes:** Usar obligatoriamente la directiva `NgOptimizedImage` (`ngSrc`) para las imágenes de las cartas, previniendo recargas innecesarias y layout shifts.
