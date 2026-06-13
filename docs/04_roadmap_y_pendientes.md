# Propuesta de Implementación: Tareas Secundarias (Features)

Este documento detalla la implementación técnica y de diseño para las tareas secundarias propuestas para el juego de Pokémon.

## 1. Agente de Ayuda (Tutorial Dinámico)
**Descripción:** Un asistente visual que guía al jugador explicando mecánicas, presentando un Pokémon en la interfaz junto a globos de texto.
**Implementación Técnica:**
- **UI/UX:** Un componente flotante de Frontend (ej. `HelpAgentComponent`) posicionado generalmente en la esquina inferior izquierda.
- **Gráficos:** Un sprite de un Pokémon mascota con diferentes estados/animaciones (feliz, pensando, alertando) según el contexto, acompañado de un componente `SpeechBubble` para los textos.
- **Lógica:**
  - Un estado global/contexto que escuche los eventos de la aplicación (ej. primer ingreso a la tienda, inicio del primer turno).
  - Almacenamiento en base de datos o local (ej. localStorage) del progreso del tutorial para evitar repetir diálogos ya vistos.

## 2. Monedas (Sistema Económico)
**Descripción:** Divisa virtual (`currency`) para adquirir elementos en el juego.
**Implementación Técnica:**
- **Backend:** Agregar un campo numérico (ej. `coins`) en la tabla de la base de datos del usuario.
- **Lógica de obtención:**
  - **Partidas ganadas:** El endpoint que procesa el resultado de la partida inyecta la lógica de recompensa, sumando monedas al ganador (+X monedas, con multiplicadores opcionales).
  - **Misiones/Pase:** Endpoints dedicados para el reclamo de recompensas que validan transaccionalmente los requisitos y suman las monedas de forma atómica.
- **Seguridad:** Toda validación de saldo y agregado de monedas debe ser firmemente calculada y validada del lado del servidor (Server-Authoritative).

## 3. Tienda
**Descripción:** Interfaz para gastar monedas en diferentes artículos.
**Implementación Técnica:**
- **Backend:**
  - Modelo `StoreItem` con campos como `id`, `name`, `type` (huevo, perfil, carta), `price`, y `isActive`.
  - Endpoint `POST /api/store/buy` que reciba el ítem, verifique el saldo del usuario, debite las monedas y otorgue el artículo de inventario, todo en una única transacción en la base de datos (ACID).
- **Frontend:**
  - Vista modularizada con pestañas por categoría.
  - Confirmación de compra y visualización reactiva del saldo del usuario.

## 4. Sobres (Card Packs)
**Descripción:** Sobres (boosters) obtenibles en la tienda y pase de batalla, que se pueden abrir para sacar cartas.
**Implementación Técnica:**
- **Lógica de Apertura (RNG/Gacha):**
  - Implementación en el servidor de una tabla de probabilidades (Drop Rates). Por ejemplo: 70% común, 25% rara, 5% full-art.
  - Endpoint `POST /api/packs/open` que consuma un sobre del usuario, genere aleatoriamente las cartas basadas en los *drop rates*, y devuelva el resultado para añadirlas a la colección.
- **Frontend:**
  - Una animación interactiva y gratificante (usando CSS o librerías de animación) que simule romper o rasgar el sobre, revelando las cartas progresivamente.

## 5. Pase de Batalla
**Descripción:** Sistema de progresión lineal con niveles y recompensas por jugar y ganar.
**Implementación Técnica:**
- **Backend:**
  - Tablas para configurar la temporada (`Season`) y los niveles (`BattlePassLevel`), incluyendo XP requerida y qué premio da.
  - Al usuario se le vincula su progreso (`current_xp`, `current_level`) y un historial de las recompensas ya reclamadas (`ClaimedRewards`).
  - Cada partida ganada invoca un cálculo de XP extra a sumar al usuario.
- **Frontend:**
  - Componente de barra de progresión (`ProgressBar`) para la pista del pase.
  - Indicadores claros de los niveles alcanzados y botones para "Reclamar" las recompensas pendientes.

## 6. Batallas Clasificatorias (Ranked) y MMR
**Descripción:** Modo competitivo diferenciado del casual, con rangos y tabla de clasificación (Leaderboard).
**Implementación Técnica:**
- **Sistema MMR (Matchmaking Rating):**
  - Al iniciar una partida Ranked, se utiliza un sistema similar a Elo. Si el jugador gana, su MMR sube; si pierde, baja.
- **Escala de Rangos:**
  - Definición de "Tiers" por rangos de puntuación en el servidor (ej. Novato: 0-500, Entrenador: 501-1000, Líder de Gimnasio: 1001-1500, Alto Mando: 1501-2000, Campeón: 2001-2500, Maestro Pokémon: 2500+).
- **Matchmaking:**
  - El sistema de colas (Queue) intentará emparejar jugadores con MMR muy similar.
- **Leaderboard:**
  - Una vista y endpoint de los mejores jugadores globales. Requiere indexación en la columna `mmr` de la base de datos para responder de manera óptima y escalable.

## 7. Cuidar Pokémon (Minijuego "Tamagotchi")
**Descripción:** Espacio tipo hábitat donde los huevos eclosionan y los Pokémon pueden ser alimentados y cuidados.
**Implementación Técnica:**
- **Mecánica de Incubación:**
  - Los huevos requieren de tiempo real o de "pasos" (partidas jugadas) para abrirse.
- **Lógica de Mascotas (Pets):**
  - Modelo `Pet` vinculado al usuario, con estadísticas que decrecen en el tiempo (Hambre, Afecto, Higiene).
  - En lugar de cronjobs constantes, el decaimiento se calcula en el backend mediante la diferencia de tiempo ("timestamp" del último cuidado contra el "timestamp" actual).
- **Frontend (Hábitat):**
  - Uso de HTML5 Canvas o componentes interactivos superpuestos donde se pueden "arrastrar" objetos como bayas (comida) o hacer clic sobre el Pokémon (para acariciarlo).

## 8. Clubes y Eventos Comunitarios (Raids)
**Descripción:** Gremios creados por la comunidad y batallas asíncronas masivas.
**Implementación Técnica:**
- **Clubes/Clanes:**
  - Estructura relacional con tabla `Club` y `ClubMembers` (para los roles). Funcionalidad de crear, buscar y unirse (con invitaciones o público).
- **Raid Boss (Evento Mundial):**
  - Entidad centralizada (`WorldBossInstance`) con puntos de vida masivos (HP).
  - Los jugadores entran a luchar; el motor resuelve la batalla y suma el daño que el usuario le provocó. Luego, de manera atómica (evitando condiciones de carrera), ese daño se resta al HP global del Jefe.
  - Al bajar el HP del Jefe a 0, se dispara una rutina que reparte recompensas por el evento.

## 9. Modo Historia (Campaña / PvE)
**Descripción:** Mapa geográfico de gimnasios, progresión de la campaña contra bots (IA).
**Implementación Técnica:**
- **Mapa de UI:**
  - Un layout visual tipo grafo o camino, con hitos fijos que el jugador desbloquea secuencialmente. 
  - La interfaz se centra automáticamente en el hito activo y se permite "viajar rápido" o interactuar con los anteriores ya vencidos.
- **Gestión de IA (Bots):**
  - **Dificultad Progresiva:** Bots iniciales que juegan cartas al azar usando su energía disponible, escalando a bots avanzados que priorizan ataques fuertes. Los "Líderes de Gimnasio" tendrán mazos estructurados (temáticos por tipo, por ejemplo, agua o fuego) y rutinas heurísticas que buscan el combo de mayor daño.
- **Persistencia:**
  - Guardado en la tabla de progresión del jugador (ej. `highest_story_node_cleared`).

## 10. Torneos Suizos Automatizados (Swiss Brackets)
**Descripción:** Eventos competitivos programados donde el servidor organiza llaves de torneo en vivo para los participantes inscritos.
**Implementación Técnica:**
- **Backend:**
  - Motor de emparejamiento suizo que agrupa jugadores por puntuación/victorias dentro del mismo torneo.
  - Gestión de estado del torneo (Inscripción, Ronda N, Finalizado).
- **Frontend:**
  - Vista gráfica de las llaves del torneo generada dinámicamente según avancen las rondas.

## 11. "Puzzles" del Día
**Descripción:** Retos diarios donde el jugador se enfrenta a un tablero pre-configurado y debe ganar en exactamente 1 turno utilizando la mano provista.
**Implementación Técnica:**
- **Backend:**
  - Endpoint que envíe un `GameState` inicial estático y valide si en la simulación del turno el oponente llega a 0 HP o se quedan sin premios.
- **Mecánica:**
  - Recompensa única diaria (PokéCoins) almacenada en base de datos para prevenir reclamos duplicados.

## 12. Draft Mode / Arena
**Descripción:** Modo de juego sin colecciones previas donde se elige 1 carta entre 3 (draft) hasta completar un mazo de 20 cartas. Se juega hasta obtener 3 derrotas o llegar a 12 victorias.
**Implementación Técnica:**
- **Backend:**
  - Lógica de "picks": el servidor genera opciones pseudo-aleatorias balanceadas (cartas básicas y evoluciones garantizadas) por cada ronda de elección.
  - Tracking del estado de la arena por usuario (Victorias, Derrotas, Mazo Draft).
- **Frontend:**
  - Pantalla interactiva de selección tipo ruleta o "pick" de sobres para elegir cartas e ir visualizando la curva de energía del mazo en tiempo real.

## 13. Logros Reactivos en Vivo (Achievements)
**Descripción:** Logros in-game detectados por el servidor de WebSockets durante la partida y notificados en pantalla en tiempo real.
**Implementación Técnica:**
- **Game Engine:**
  - Sistema de heurística en la resolución de acciones (e.g. ganar con 10 HP, infligir más de 200 de daño en 1 ataque, evolucionar 3 veces en un turno).
- **Notificación:**
  - Evento STOMP que envía un push asíncrono al cliente disparando una animación de trofeo in-game.
