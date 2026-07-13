# Directrices de Diseño

El sistema de diseño es la identidad visual principal de la aplicación de cartas Pokémon. Está inspirado en el concepto de *glassmorphism* avanzado combinado con un ecosistema de tema oscuro, iluminación atmosférica (luces de neón simuladas) y micro-animaciones.

## 1. Paleta de Colores (CSS Variables)

El archivo `FE/src/styles-theme.css` contiene la fuente de verdad para los tokens de diseño (bloque `.v-default`).

- **`--bg` (`#0a1730`)**: Color de fondo principal. Un azul marino muy oscuro y profundo.
- **`--bg2` (`#132342`)**: Color secundario para crear capas de profundidad.
- **`--surface` (`rgba(255,255,255,.05)`)**: Color de las superficies de cristal (*glass panels*). Se utiliza siempre con `backdrop-filter: blur(10px)` o superior.
- **`--line` (`rgba(255,255,255,.10)`)** / **`--line-2` (`rgba(255,255,255,.16)`)**: Para separadores sutiles y bordes de paneles; `--line-2` para bordes con más énfasis.
- **`--accent` (`#ff2e3e`)** / **`--accent-dk` (`#cc2531`)** / **`--accent-soft` (`rgba(255,46,62,.14)`)**: Acento primario (Rojo) y sus variantes oscura/suave. Usado para insignias, botones de CTA críticos, rachas bajas y llamadas a la acción agresivas.
- **`--accent2` (`#ffce32`)**: Acento secundario (Amarillo Pokémon). Usado para rachas altas ("fuego fuerte"), energía y llamadas a la atención positivas.
- **`--txt` (`#eaf0ff`)**: Texto principal (blanco azulado, no blanco puro).
- **`--mut` (`#93a3c4`)**: Texto silenciado o secundario (p. ej. "hace 2 d", "Racha").
- **`--dim` (`#5e6e90`)**: Texto muy sutil, usado para descripciones o labels sin importancia.
- **`--m1`/`--m2`/`--m3`/`--m4`**: Colores de las mallas atmosféricas de fondo (`#1f6f8c`, `#3a2e8c`, `#7a2e6b`, `#1fa89a`).

## 2. Tipografía

El ecosistema utiliza dos tipografías complementarias cargadas desde Google Fonts:

- **Manrope / Space Grotesk (`sans-serif`)**: Utilizada para la legibilidad del cuerpo del texto, las barras de navegación, los botones de UI y las métricas tabulares. Se enfoca en legibilidad en tamaños pequeños (10px a 14px).
- **Instrument Serif (`serif`)**: Utilizada exclusivamente para "Display", como el MMR, Win Rates de alto impacto, títulos de pantallas o el Nombre de Entrenador. Otorga una vibra muy editorial, limpia y elegante al proyecto.

## 3. Arquitectura y Componentes UI Compartidos

Todos los componentes reutilizables se alojan en `FE/src/app/shared/ui/ui-kit.components.ts`. Al diseñar una nueva pantalla, SIEMPRE usar estos bloques:

- `<app-stat>`: Muestra estadísticas de una forma estándar, como `[v]="1842"` y `k="MMR"`.
- `<app-trainer-chip>`: La insignia de entrenador en la barra superior.
- `<app-glyph-icon>` y `<app-ball-icon>`: Íconos SVG estilizados (set reducido, props `n`/`s`). Para el set completo de íconos con nombres semánticos, usar `<app-icon name="..." svgClass="...">` (`FE/src/app/shared/ui/icon/icon.component.ts`) — son dos componentes de ícono distintos, no intercambiables.
- `<app-holo-card>`: Para representar las cartas Pokémon individuales con el efecto de rotación 3D en el cursor y brillos volumétricos (`perspective(1000px) rotateX(...)`), definido en `FE/src/app/shared/ui/holo-card/holo-card.component.ts`.

## 4. Disposición y Capas (Z-Index y Fixed)

- Las pantallas mayores (Lobby, Mazos, Perfil) deben renderizarse a pantalla completa cubriendo cualquier menú anterior. Utilizar:
  ```css
  .scene.v-default {
    position: fixed;
    inset: 0;
    z-index: 9999;
  }
  ```
- **Atmósfera**: Detrás de todo el contenido, siempre debe incluirse el `<app-ambient>` y los divs de malla (`.mesh`, `.bd-noise`, `.bd-vignette`).

## 5. Micro-interacciones

- **Hover States**: Los botones y filas interactivas NUNCA cambian su color de fondo a un color sólido brusco. Usan `rgba(255,255,255,0.05)` o alteran su `border-color` para dar una sensación sutil de encendido.
- **Cartas**: Las cartas interactivas se levantan sutilmente (`translateY(-4px)`) y aumentan la densidad del drop-shadow oscuro.

---
> [!IMPORTANT]
> Nunca utilices TailwindCSS o clases atómicas para colores quemados. Toda la estética depende de que se utilicen de manera estricta las variables CSS de `styles-theme.css` para que el renderizado de *glassmorphism* funcione.

> [!NOTE]
> **2026-07-13**: Este documento estaba desactualizado respecto al código real (ruta de archivo, ubicación de componentes, y varios valores de color no coincidían con `styles-theme.css`). Reconciliado contra el código real en esta fecha, junto con el rename de todos los selectores/clases/archivos que llevaban el nombre en clave "aurora" de la estética a nombres genéricos (`app-*`).
