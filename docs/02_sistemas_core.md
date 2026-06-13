# Sistemas Core y Features (AI-Friendly Context)

## 1. El Game Engine (Motor de Partidas)
Es el núcleo de la aplicación. Maneja las reglas del Pokémon TCG, validación de jugadas y estados.

- **Dónde está:** `BE/src/main/java/ar/edu/utn/frc/tup/piii/services/GameFacade.java` y `MatchService.java`.
- **Estructura del Turno:** Cada turno está dividido en fases (DRAW, MAIN, ATTACK, END).
- **Tipos de Acciones (`MatchActionType`):** `PLAY_POKEMON`, `ATTACH_ENERGY`, `USE_TRAINER`, `ATTACK`, `PASS_TURN`.
- **Daño y KOs:** El cálculo de daño incluye Debilidades y Resistencias. Si un Pokémon llega a 0 HP, se otorga una Carta de Premio al atacante (`GameFacade.resolveKOs()`).
- **Condiciones de Victoria (`MatchStatus`):** `FINISHED_WIN_A`, `FINISHED_WIN_B`, `FINISHED_TIE`. Ocurre si un jugador toma sus 6 premios, si su mazo queda vacío (Deck Out), o si se queda sin Pokémon en Banca y el Activo es KO (Bench Out).

> [!TIP]
> **Modificaciones Futuras (AI Agents):** Si necesitas implementar nuevas cartas (ej: un nuevo ACE SPEC), la lógica no va en el `MatchService`, sino en resolvers específicos (ej: `TrainerEffectResolver.java`) llamados por el `GameFacade`.

## 2. Sistema Competitivo (Rankeds y MMR)
El juego cuenta con un sistema de emparejamiento basado en habilidad.

- **Dónde está:** `MmrCalculationService.java`, `MatchCreationService.java`, `SeasonService.java`.
- **Matchmaking (`LobbyQueue.java`):** Usamos una cola concurrente en RAM. El jugador llama a `POST /api/lobby/join` con `isRanked=true`. El servicio intenta encontrar un rival con `|player1.mmr - player2.mmr| < 100`.
- **Algoritmo ELO (`MmrCalculationService.java`):** K-Factor dinámico. `K=60` para placement (primeras 10 partidas), `K=16` para Expertos (>2000 MMR).
- **Esquema de BD:** El MMR se guarda en `UserEntity.mmr`. Los records de temporada se guardan en `SeasonRecordEntity`.

## 3. Social y Moderación (Tribunal System)
- **FriendshipService:** Gestiona `UserFriendEntity` (estado PENDING, ACCEPTED).
- **ChatService:** WebSockets puros para comunicación. `ProfanityFilterService` reemplaza malas palabras por `***`.
- **PenaltyService:** Cada vez que un usuario es reportado, este servicio lo evalúa. Si junta muchos reportes en poco tiempo, aplica un `MUTE` o ban de rankeds. El `PenaltyService` verifica en el interceptor de WebSockets si el usuario está silenciado.

## 4. Perfiles, Estadísticas y Progresión
- **Dónde está:** `ProfileService.java` y `UserEntity`.
- **Showcase (Deck y Cartas):** El jugador puede mostrar sus cartas favoritas (`UserShowcaseEntity`) usando `cardId` que apunta estáticamente a la tabla pre-cargada.
- **Logros (Achievements):** Cada vez que termina una partida (`MatchService.java`), se llama a `profileService.awardXpAndCheckAchievements()`. Si se cumplen las condiciones, se desbloquean Títulos.
