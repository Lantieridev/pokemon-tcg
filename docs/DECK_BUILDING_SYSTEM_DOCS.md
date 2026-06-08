# Documentación del Sistema de Mazos (Deck Building System) - Backend

Esta documentación detalla todas las funcionalidades, endpoints y cambios a nivel de arquitectura y base de datos que se implementaron para el **Sistema de Construcción de Mazos**. El objetivo de este sistema es permitir a los usuarios crear, guardar, clonar y autocompletar mazos de Pokémon TCG de manera inteligente.

---

## 1. Nuevas Funcionalidades Principales

### 1.1. Soporte para Mazos Incompletos (Drafts)
Se añadió el soporte para guardar mazos que no tienen exactamente 60 cartas. Para ello:
- Se creó el enum `DeckStatus` (`VALID` y `DRAFT`).
- Los mazos con menos de 60 cartas se guardan como `DRAFT` para que el usuario pueda continuarlos en otro momento.
- Los mazos que cumplen con las 60 cartas pasan a estado `VALID`.

### 1.2. Plantillas de Mazos (Templates)
Se implementó un sistema de plantillas para que los nuevos jugadores no tengan que empezar desde cero. 
- Existen plantillas preconstruidas de 60 cartas (Ej: Mazo de Fuego, Mazo de Agua).
- El usuario puede **clonar** una plantilla, lo que automáticamente guarda una copia en su cuenta con el estado `VALID`.

### 1.3. Asistente Inteligente de Mazos
Se creó un nuevo servicio (`DeckAssistantService`) que provee lógica "inteligente" para ayudar al usuario a armar su mazo:
1. **Autocompletado**: Toma una lista de cartas incompleta (ej. 4 Charmanders) y rellena las cartas faltantes hasta llegar a 60. Se asegura de agregar Cartas de Entrenador esenciales y rellena el resto con el tipo de Energía que corresponda a los Pokémon ingresados.
2. **Sugerencias Inteligentes**: Analiza las cartas que el jugador ya agregó al mazo y devuelve hasta 3 sugerencias de cartas que tienen buena sinergia (por ejemplo, evoluciones o soporte del mismo tipo elemental).
3. **Mago Creador (Wizard)**: Permite generar un mazo de 60 cartas perfectamente balanceado (Pokémon, Entrenadores y Energías) y totalmente legal, basado únicamente en una temática elegida (ej. "fuego", "agua").

---

## 2. Endpoints de la API Creados

### `DeckController`
- `GET /api/decks/templates`
  - Devuelve la lista de plantillas de mazos preconstruidas disponibles en el sistema.
- `POST /api/decks/users/{userId}/clone/{templateId}`
  - Clona una plantilla existente a la cuenta del usuario ingresado.

### `DeckAssistantController`
- `POST /api/decks/assistant/autocomplete`
  - **Body**: Lista de `DeckCardRequestDTO` (cartas actuales).
  - **Response**: Lista completa de 60 cartas rellenada por el asistente.
- `POST /api/decks/assistant/suggestions`
  - **Body**: Lista de `DeckCardRequestDTO` (cartas actuales).
  - **Response**: Hasta 3 `DeckCardResponseDTO` sugeridas por el sistema.
- `POST /api/decks/assistant/wizard`
  - **Body**: `{"theme": "water"}` (temática deseada).
  - **Response**: Un mazo completo y balanceado de 60 cartas basado en el elemento solicitado.

---

## 3. Cambios a Nivel Técnico

### 3.1. Migración de Base de Datos
- Se creó el script de migración en Flyway `V16__add_status_to_decks.sql` para agregar la columna `status` (`VARCHAR(255) DEFAULT 'VALID'`) a la tabla `decks`. Esto permite guardar el estado de completitud del mazo de forma persistente.

### 3.2. Configuración de Seguridad
- Se actualizó el archivo `SecurityConfig.java` para exponer temporalmente todas las rutas bajo `/api/decks/**` (usando `.permitAll()`). Esto se hizo con el objetivo de facilitar el desarrollo y las pruebas desde Swagger y el Frontend sin necesidad de configurar y arrastrar el token JWT constantemente.

### 3.3. Refactorización del Seeder
- Se renombró el `LantieriDeckSeeder` a `ExampleDeckSeeder` para generalizar su uso en la carga inicial de datos de la aplicación y se lo actualizó para que incluya el nuevo parámetro requerido `DeckStatus.VALID`.

---

## 4. Próximos Pasos (Frontend)
Con el backend 100% funcional y probado, la siguiente etapa consistirá en consumir estos endpoints desde el Frontend para construir la interfaz de usuario (UI), incluyendo:
- La vista de selección y clonación de plantillas.
- El constructor de mazos interactivo (arrastrar y soltar cartas).
- La integración de los botones mágicos de "Autocompletar" y "Sugerir Carta".
- El wizard modal para armar un mazo desde cero eligiendo la temática.
