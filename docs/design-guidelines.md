# Directrices de Diseño "Aurora"

El sistema de diseño **Aurora** es la identidad visual principal de nuestra aplicación de cartas Pokémon. Está inspirado en el concepto de *glassmorphism* avanzado combinado con un ecosistema de tema oscuro, iluminación atmosférica (luces de neón simuladas) y micro-animaciones.

## 1. Paleta de Colores (CSS Variables)

El archivo `styles-aurora.css` contiene la fuente de verdad para los tokens de diseño.

- **`--bg` (`#0a1730`)**: Color de fondo principal. Un azul marino muy oscuro y profundo.
- **`--bg2` (`#102242`)**: Color secundario para crear capas de profundidad.
- **`--surface` (`rgba(255,255,255,0.03)`)**: Color de las superficies de cristal (*glass panels*). Se utiliza siempre con `backdrop-filter: blur(10px)` o superior.
- **`--line` (`rgba(255,255,255,0.1)`)**: Para separadores sutiles y bordes de paneles.
- **`--accent` (`#ff3b47`)**: Color de acento primario (Rojo). Usado para insignias, botones de CTA críticos, rachas bajas y llamadas a la acción agresivas.
- **`--accent2` (`#ffce32`)**: Color de acento secundario (Amarillo Pokémon). Usado para rachas altas ("fuego fuerte"), energía y llamadas a la atención positivas.
- **`--txt` (`#ffffff`)**: Texto principal, contraste puro contra `--bg`.
- **`--mut` (`rgba(255,255,255,0.6)`)**: Texto silenciado o secundario (p. ej. "hace 2 d", "Racha").
- **`--dim` (`rgba(255,255,255,0.4)`)**: Texto muy sutil, usado para descripciones o labels sin importancia.

*Nota:* Los gradientes y mallas atmosféricas utilizan `--m1` (rojo) y `--m2` (cyan) rotando lentamente para dar la sensación de "luces de aurora boreales".

## 2. Tipografía

El ecosistema utiliza dos tipografías complementarias cargadas desde Google Fonts:

- **Manrope / Space Grotesk (`sans-serif`)**: Utilizada para la legibilidad del cuerpo del texto, las barras de navegación, los botones de UI y las métricas tabulares. Se enfoca en legibilidad en tamaños pequeños (10px a 14px).
- **Instrument Serif (`serif`)**: Utilizada exclusivamente para "Display", como el MMR, Win Rates de alto impacto, títulos de pantallas o el Nombre de Entrenador. Otorga una vibra muy editorial, limpia y elegante al proyecto.

## 3. Arquitectura y Componentes UI Compartidos

Todos los componentes reutilizables se alojan en `FE/src/app/features/lobby-aurora/ui/aurora-ui.components.ts` (temporalmente, luego a refactorizar a `shared/ui`). Al diseñar una nueva pantalla, SIEMPRE usar estos bloques:

- `<aurora-stat>`: Muestra estadísticas de una forma estándar, como `[v]="1842"` y `k="MMR"`.
- `<aurora-trainer-chip>`: La insignia de entrenador en la barra superior.
- `<aurora-icon>` y `<aurora-ball-icon>`: Íconos SVG estilizados.
- `<aurora-holo-card>`: Para representar las cartas Pokémon individuales con el efecto de rotación 3D en el cursor y brillos volumétricos (`perspective(1000px) rotateX(...)`).

## 4. Disposición y Capas (Z-Index y Fixed)

- Las pantallas mayores (Lobby, Mazos, Perfil) deben renderizarse a pantalla completa cubriendo cualquier menú anterior. Utilizar:
  ```css
  .scene.v-aurora {
    position: fixed;
    inset: 0;
    z-index: 9999;
  }
  ```
- **Atmósfera**: Detrás de todo el contenido, siempre debe incluirse el `<aurora-ambient>` y los divs de malla (`.mesh`, `.bd-noise`, `.bd-vignette`).

## 5. Micro-interacciones

- **Hover States**: Los botones y filas interactivas NUNCA cambian su color de fondo a un color sólido brusco. Usan `rgba(255,255,255,0.05)` o alteran su `border-color` para dar una sensación sutil de encendido.
- **Cartas**: Las cartas interactivas se levantan sutilmente (`translateY(-4px)`) y aumentan la densidad del drop-shadow oscuro.

---
> [!IMPORTANT]
> Nunca utilices TailwindCSS o clases atómicas para colores quemados. Toda estética de Aurora depende de que se utilicen de manera estricta las variables CSS de `styles-aurora.css` para que el renderizado de *glassmorphism* funcione.
