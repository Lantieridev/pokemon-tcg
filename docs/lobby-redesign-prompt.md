# Prompt — Rediseño del Lobby (Pokémon TCG Digital)

Usá este prompt con Claude Design Web o cualquier herramienta de diseño generativo.

---

## PROMPT

Diseñá el **lobby principal** de una aplicación web de Pokémon TCG (juego de cartas digital, licenciado). Es un TPI universitario con stack Angular 20 + Tailwind CSS. El objetivo es que la pantalla tenga **vida, emoción y esencia Pokémon**, no que parezca un dashboard genérico.

---

### CONTEXTO DE LA APP

- Juego competitivo 1v1 en tiempo real con mazos de 60 cartas del set XY1.
- El set de diseño ya existe y tiene una identidad fuerte: **estética "3DS Pokémon X/Y era"** — paneles sólidos con bordes oscuros (`#1a1010`), hard shadows (box-shadow hacia abajo, sin difuminar), tipografía chunky (`Russo One`, `Bowlby One`), paleta `--p-red: #ee1515`, `--p-yellow: #ffcb05`, `--p-blue: #3b5ba7`, fondos nocturnos `#0d1521 → #1c2a44`.
- Fondo oscuro con textura de puntos radiales (hex pattern, 36×36px) muy sutil.
- Motivo decorativo: **Pokéball en CSS puro** como marca de agua (ya existe en el DS, clase `.pokeball-watermark`).
- Los botones importantes usan **hard shadow** (ej: `box-shadow: 0 6px 0 #1a1010`) y animación press: `translateY(3px)` al hacer click.
- Hay una clase `.battle-cta` para el botón principal de batalla: fondo rojo o amarillo, tipografía `Bowlby One`, tamaño grande, sombra dramática.

---

### PANTALLA A DISEÑAR: LOBBY PRINCIPAL

Esta es la primera pantalla que ve el jugador luego del login. Necesita:

1. **Sentir que es un juego de Pokémon**, no una app SaaS. Inspiración: menú principal de Pokémon X/Y, Pokémon TCG Online, Legends Arceus.
2. **Jerarquía clara**: la acción principal (buscar partida ranked) debe dominar la pantalla.
3. **Dinamismo**: animaciones CSS que den vida sin ser molestas — cartas flotando, brillos, partículas sutiles, transiciones de entrada.
4. **Información útil pero no invasiva**: rango del jugador, estadísticas de temporada, mazos guardados.

---

### ELEMENTOS QUE DEBE INCLUIR

#### Hero / CTA principal
- Zona grande (>40% de la pantalla) con el botón **"BATALLAR"** como protagonista absoluto.
- Fondo: playmat oscuro con gradiente animado sutil (shimmer o aurora) que evoque la arena de batalla.
- El nombre del jugador mostrado con estilo "Trainer Card" — blanco con sombra roja estilo texto Pokémon.
- Indicador visual del rango actual (ícono de insignia/medalla con glow del color del rango).
- **Animación**: 2-3 cartas Pokémon flotando suavemente en perspectiva en el fondo del hero, con rotación lenta y brillo. Efecto: `transform: rotateY(Xdeg)` + `animation: float` con `translateY`.

#### Panel de rango / temporada
- Tarjeta compacta estilo "Trainer ID Card" del DS: fondo crema `#fff8e8`, borde rojo, texto oscuro.
- Contiene: nombre del jugador, rango (Liga Oro III, etc.), MMR, barra de progreso hacia siguiente división (segmentos sólidos, no barra continua — 5 cuadraditos tipo DS).
- Una marca de agua Pokéball en CSS detrás del contenido.

#### Modos de juego secundarios
- **NO una grilla genérica de 4 tarjetas iguales**. En cambio, una fila de "cartuchos" o "chapas" estilo 3DS:
  - Casual: fondo azul oscuro, ícono Pokéball simple.
  - Tutorial: fondo verde, ícono libro.
  - Deck Builder: fondo dorado/amarillo, ícono de cartas apiladas.
  - Cada uno con hover que levanta la chapa (+3px en Y, shadow más larga) y un efecto de "brillo" que cruza la tarjeta (CSS `::after` con gradiente diagonal animado).

#### Mazos del jugador
- Sección compacta debajo o lateral: lista de hasta 3 mazos guardados como miniaturas.
- Cada mazo muestra la imagen de la carta principal (Pokémon portada) con un efecto de perspectiva 3D sutil al hover (`perspective: 500px; rotateX/Y`).
- Botón "+ Nuevo mazo" como chip sólido amarillo.

#### Actividad reciente / rivales online
- Sección pequeña lateral o inferior: muestra "3 rivales en cola" con avatares animados (dot online pulsando).
- Historial de las últimas 3 partidas: resultado W/L con badge de color y nombre del rival.

---

### ANIMACIONES ESPECÍFICAS REQUERIDAS

1. **Float cards (hero)**: `@keyframes float { 0%,100% { transform: translateY(0) rotateY(-8deg) } 50% { transform: translateY(-18px) rotateY(-8deg) } }` — cartas en el fondo del hero flotando con delay escalonado.
2. **Shimmer aurora (hero background)**: gradiente animado `conic-gradient` o `linear-gradient` que cambia sutilmente de posición con `animation: aurora 8s ease-in-out infinite alternate`.
3. **Botón BATALLAR — pulso de ready**: cuando el jugador está en cola, el botón hace `animation: pulse-ring` — un ring que se expande y desvanece cada 2s (como el efecto `.ranked-cta` ya definido en el DS).
4. **Entrada de elementos**: `@keyframes slide-up { from { opacity:0; transform:translateY(20px) } to { opacity:1; transform:translateY(0) } }` con `animation-delay` escalonado para que los paneles entren uno tras otro al cargar.
5. **Card hover 3D**: las tarjetas de modos y mazos responden al mouse con `rotateX/Y` usando `transform-style: preserve-3d` y `perspective`. CSS puro con `:hover`.

---

### LO QUE HAY QUE EVITAR

- **Misiones/dailies en el lobby** — no encajan, quitarlas del diseño.
- Grillas de tarjetas todas iguales sin jerarquía — parece un dashboard de SaaS, no un juego.
- Glassmorphism con `backdrop-filter` excesivo — la identidad es sólida y chunky, no frosted glass.
- Colores hardcodeados — usar las variables CSS del DS.
- Más de 3 niveles de profundidad visual — mantenerlo legible.
- Scroll en la pantalla principal — todo debe caber en `100vh`.

---

### OUTPUT ESPERADO

Quiero ver:
1. **Mockup visual completo** del lobby (puede ser HTML/CSS estático, Figma frame, o SVG).
2. **Las animaciones CSS** descritas arriba implementadas o al menos esbozadas con código.
3. Una versión con el botón en estado "buscando partida" (spinner + tiempo transcurrido + botón cancelar).
4. Anotaciones breves explicando las decisiones de jerarquía y por qué cada elemento está donde está.

El resultado tiene que sentirse como **entrar a un juego de Pokémon**, no como abrir una app de gestión.
