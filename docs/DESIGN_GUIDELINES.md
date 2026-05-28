# Monstra TCG - Design & UI Guidelines

Este documento establece las bases visuales y de experiencia de usuario (UX) para el rediseño total del Frontend de Monstra TCG. El objetivo es alejarnos de interfaces web genéricas y adoptar un estándar de juego de cartas coleccionables AAA (como Hearthstone, MTG Arena o PTCG Online/Live).

## 1. Visión Estética (AAA TCG)
- **El Arte es Rey:** Las cartas no deben estar encapsuladas en contenedores HTML con bordes duros. El límite visual de la carta es la propia imagen de la carta (`<img>`) con un `border-radius` adecuado (típicamente 4-5%) y un `box-shadow` que brinde profundidad tridimensional.
- **Inmersión Física:** El fondo de la partida no es un color plano, es un "Tapete de Juego" (Playmat). Los elementos interactivos deben sentirse táctiles (hover states que acercan la carta a la cámara, sonidos de arrastre de papel).
- **HUD Minimalista:** Evitar textos en pantalla para estadísticas de vida o botones enormes. Los indicadores (Daño, Veneno, Energías atachadas) deben ser íconos superpuestos sutilmente sobre el arte de la carta.

## 2. Sistema de Cosméticos y Diseños Desbloqueables (Monetización / Retención)
Como todos los jugadores tienen acceso a todas las cartas por diseño (sandbox), el sistema de progresión y recompensas se basará 100% en **desbloquear estéticas**:

- **Fundas de Cartas (Sleeves):** Diseños para el dorso de las cartas. Ejemplos: "Funda de Pikachu 8-bit" (retro), "Funda Legendaria de Mewtwo".
- **Playmats (Tapetes de Mesa):** El fondo del tablero. Cambia completamente la atmósfera.
  - *Tapete Clásico:* El diseño de las capturas de pantalla de referencia (azul/rojo clásico).
  - *Tapete de Gimnasio:* Desbloqueado al vencer a la IA de líderes de gimnasio.
  - *Tapete Neón/Cyberpunk:* Recompensa de torneo Ranked.
- **Monedas (Coins):** Monedas 3D customizables que se usan en las animaciones de lanzamiento de moneda (Coin Flip).
- **Temas de la Interfaz (UI Themes):** Afectan a los menús del Lobby y Perfil. 
  - *Tema Light/Classic:* UI blanca/roja al estilo Pokedex antigua.
  - *Tema Dark/Glassmorphism:* UI moderna, translúcida, tonos oscuros.
  - *Tema Gameboy:* UI pixel art para eventos retro.

## 3. Disposición del Tablero (Inspirado en PTCGO)
1. **Activo y Banca:** El Pokémon Activo va al centro. La banca va en una fila justo detrás/debajo del Activo.
2. **Energías (Cascada):** Las energías asignadas a un Pokémon se apilan **detrás** de la carta del Pokémon, asomando hacia la izquierda o hacia arriba.
3. **Mazo y Descartes:** El Mazo a la derecha (boca abajo, mostrando la funda elegida). La Pila de Descartes debajo del mazo (boca arriba).
4. **Premios:** 6 cartas agrupadas a la izquierda (boca abajo).
5. **La Mano:** Ubicada en la parte inferior de la pantalla, las cartas se superponen formando un arco o abanico.

## 4. Interacción Radial / Contextual
- **No más "Barra de Acciones Global":** Cuando es tu turno, hacer clic en una carta de tu lado del campo despliega un menú contextual pequeño (un modal al lado de la carta o un menú circular).
- **Ejemplo Activo:** Clic en tu Activo -> Muestra "Atacar (Lanzallamas)", "Retirar", "Usar Habilidad".
- **Animaciones obligatorias:** Drag & Drop para jugar cartas de la mano, mover a la banca, o atachar energías.
