# pokemon-tcg — Frontend

Angular 21+ (Standalone Components, Signals, control flow nativo `@if`/`@for`). Server-authoritative: este frontend nunca calcula el resultado de una jugada, solo emite intenciones vía STOMP/WebSocket y renderiza el estado que devuelve el backend (ver [`docs/01_arquitectura_y_flujo.md`](../docs/01_arquitectura_y_flujo.md) en la raíz del repo).

## Estructura

```
src/app/
├── core/            # servicios singleton, modelos, guards, interceptors, stores de signals
│   ├── services/    # auth, lobby, match-backend, websocket, profile, ranking, store, pack, battle-pass...
│   ├── store/       # deck.store.ts, match.store.ts — estado compartido vía signals
│   ├── guards/      # auth.guard.ts
│   └── interceptors/# auth.interceptor.ts (adjunta el JWT a cada request)
├── features/        # una carpeta por pantalla/flujo — ver tabla de cobertura abajo
└── shared/          # componentes/UI reutilizables entre features (toast, chat-modal, card-selection-modal...)
```

## Patrón Smart/Dumb

- **Smart components** (en `features/`): inyectan servicios de `core/`, conocen la red y el estado global.
- **Dumb components** (en `shared/`): reciben datos por `@Input()`/`@Output()`, no saben de WebSockets ni HTTP.

## Levantar el entorno local

```bash
npm install
npm start          # http://localhost:4200, apunta a la API en :8080
```

## Testing

Ver [`docs/TESTING_STRATEGY.md`](../docs/TESTING_STRATEGY.md) para el detalle completo de qué cubre cada capa (unit Jasmine/Karma, E2E Cypress) y qué NO está cubierto todavía.

```bash
npm test                # suite unitaria (Jasmine/Karma)
ng test --code-coverage # con reporte de cobertura
npm run cypress:open    # E2E interactivo
npm run cypress:run     # E2E headless (lo que corre en CI)
```
