# Documentación Técnica: Sistema de Logros y Títulos de Entrenador

Este documento detalla la arquitectura, el diseño lógico y el historial de cambios (commits) para la expansión del sistema de logros y títulos del entrenador en el backend.

---

## 1. Arquitectura y Modelo de Datos

El sistema de logros se evalúa de manera **dinámica** sin requerir un estado persistente de "logro desbloqueado por ID" en tablas separadas. En su lugar, el backend utiliza el conjunto de **títulos desbloqueados** (`unlockedTitles` en `UserEntity`) mapeado a la tabla `user_unlocked_titles` como el indicador del estado del logro.

### Atributos Utilizados en `users`:
*   `level`, `xp`: Para logros de progresión de nivel.
*   `mmr`: Para los logros del modo competitivo.
*   `pokecoins`, `battle_points`: Monedas acumuladas de la economía.
*   `total_damage_dealt`: Daño acumulado infligido.
*   `total_kos`: Cantidad acumulada de KOs realizados.
*   `perfect_wins`: Victorias perfectas (sin perder Pokémon/KOs sufridos).
*   `comeback_wins`: Victorias de remontada.
*   `trainer_cards_played`: Cartas de Entrenador jugadas.

### Tablas de Estadísticas Adicionales:
*   `user_card_stats` (`UserCardStatEntity`): Utilizada para calcular la versatilidad (número de cartas distintas jugadas) y la lealtad de uso de Pokémon específicos (Pikachu, Charizard, Blastoise, Venusaur, Mewtwo).
*   `user_energy_stats` (`UserEnergyStatEntity`): Utilizada para contabilizar el uso acumulado de energías de tipos elementales (Fuego, Agua, Planta, Rayo, Psíquico, Lucha, Incoloro).

---

## 2. Puntos de Entrada de Lógica (BE)

Toda la lógica del sistema reside en [ProfileServiceImpl.java](../BE/src/main/java/ar/edu/utn/frc/tup/piii/services/impl/ProfileServiceImpl.java):

1.  `checkAndUnlockTitles(UserEntity user, ...)`: Se ejecuta al ganar XP tras finalizar una partida o al consultar el perfil. Evalúa los requisitos de los 88 logros y añade los títulos correspondientes al conjunto `unlockedTitles`.
2.  `getAchievementsProgress(String username)`: Retorna el listado completo de logros con sus respectivos niveles de progreso actual y objetivos (`progress` y `target`) para el renderizado de la interfaz en el frontend.

---

## 3. Registro de Commits y Cambios

### Commit 1: Implementación de la Lógica del Backend
*   **Mensaje del commit:** `feat(BE): expand achievements and titles system to 88 total achievements`
*   **Descripción de cambios:**
    *   Se implementó la recolección de estadísticas de uso de cartas (`UserCardStatEntity`) y energías unidas (`UserEnergyStatEntity`) en `ProfileServiceImpl`.
    *   Se ampliaron las evaluaciones en `checkAndUnlockTitles` para abarcar las nuevas categorías: Combate (Daño, KOs, Victorias Perfectas, Remontadas, Entrenadores), Resiliencia (Derrotas), MMR Competitivo, Versatilidad, Economía, Colección de Títulos, Lealtad de Pokémon y Elementos.
    *   Se amplió `getAchievementsProgress` para mapear los 88 logros con su progreso actual de forma que el frontend los recupere dinámicamente.

### Commit 2: Ampliación de Pruebas Unitarias
*   **Mensaje del commit:** `test(BE): update ProfileServiceTest for expanded achievements and verify combat progress`
*   **Descripción de cambios:**
	*   Se añadieron imports requeridos y se actualizó `testExtendedStatsAndAchievementsProgress` para asertar el tamaño total de la lista (88 logros).
	*   Se agregaron verificaciones explícitas de progreso para logros de daño acumulado (`Poder Eléctrico`) y KOs realizados (`Derribador`).

### Commit 3: Colores para Nuevas Categorías en Frontend
*   **Mensaje del commit:** `style(FE): add color mapping for new achievement categories in profile page`
*   **Descripción de cambios:**
	*   Se añadieron los mapeos de color en `getCategoryColor` en `profile.component.ts` para las nuevas categorías de logros (`RESILIENCIA`, `COMPETITIVO`, `VERSATILIDAD`, `TITULOS`, `ECONOMIA`, `COMBATE`, `ELEMENTAL`, `LEALTAD`).

### Commit 4: Unificación del Menú de Usuario en Navbar
*   **Mensaje del commit:** `fix(FE): remove duplicate overlapping profile dropdown and delegate menu to TrainerChipComponent`
*   **Descripción de cambios:**
	*   Se removió el menú superpuesto redundante de `navbar.html` y el manejador `toggleUserMenu()` de `navbar.component.ts`.
	*   Se integró el enlace "Mi Perfil" directamente dentro del componente de menú estilizado `TrainerChipComponent` en `ui-kit.components.ts` e importó `RouterModule` para permitir navegación.

### Commit 5: Actualización del DTO de Logros y Vista del Navbar en Frontend
*   **Mensaje del commit:** `feat(FE): extend UserAchievementProgressDTO and link avatar icon to navbar trainer chip`
*   **Descripción de cambios:**
	*   Se añadieron los campos opcionales `rewardType` y `rewardValue` al DTO `UserAchievementProgressDTO` en `profile.service.ts`.
	*   Se modificó el componente `TrainerChipComponent` para recibir el parámetro `avatarIcon` y renderizar dinámicamente la imagen del avatar si es personalizado (`avatar_`) o el emoji si es por defecto.
	*   Se actualizó `navbar.html` para enlazar y pasar `profileData()?.avatarIcon` al chip.

### Commit 6: Procesamiento e Integración de Activos Gráficos Originales
*   **Mensaje del commit:** `feat(assets): generate and slice 25 medals and 31 avatars from master grids`
*   **Descripción de cambios:**
	*   Se generaron 4 grids maestros de imágenes (estilos Cute/Flat y Epic 3D Holographic) utilizando herramientas de inteligencia artificial.
	*   Se escribió y ejecutó un script en Node.js (`slice_assets.js`) utilizando `jimp` para recortar y redimensionar de forma óptima a 192x192 cada medalla y avatar.
	*   Los activos resultantes con fondo transparente se ubicaron en `FE/public/assets/achievements/medals` y `FE/public/assets/achievements/avatars`.

### Commit 7: Medallero, Avatares y Filtrado de Títulos en Perfil
*   **Mensaje del commit:** `feat(FE): integrate Medallero, custom avatars list, and active titles dropdown filtering`
*   **Descripción de cambios:**
	*   Se implementó el componente visual **Medallero de Logros** con tooltips descriptivos animados (CSS puro) que reflejan el estado bloqueado/desbloqueado de cada una de las 25 medallas.
	*   Se actualizó el listado de logros para mostrar tags de tipo de recompensa (Medalla, Título, Avatar) y un preview gráfico al lado de cada logro.
	*   Se filtró el dropdown de selección de títulos activos en el modal de edición de perfil para mostrar únicamente logros desbloqueados que otorgan un Título.
	*   Se implementó un grid con scroll para la selección de avatares en el modal que carga los avatares por defecto y los personalizados una vez que se desbloquean.

