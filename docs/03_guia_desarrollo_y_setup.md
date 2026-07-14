# Guía de Desarrollo y Setup

Este documento contiene las instrucciones fundamentales para compilar, probar y ejecutar el proyecto localmente.

## 1. Prerrequisitos
- **Java:** JDK 21
- **Node.js:** v20+
- **Base de datos:** PostgreSQL (usado en producción) o H2 (usado por defecto en perfiles de test).
- **Herramientas de Build:** Maven (Backend) y Angular CLI (Frontend). El repo ya incluye el wrapper de Maven (`mvnw`).

## 2. Compilar y Ejecutar el Backend

El backend es una aplicación Spring Boot.

### Levantar el entorno de desarrollo
1. Abrir una terminal en la carpeta `BE/`.
2. Ejecutar el wrapper de maven para limpiar y compilar:
   ```bash
   ./mvnw clean compile
   ```
3. Ejecutar la aplicación:
   ```bash
   ./mvnw spring-boot:run
   ```
La base de datos (por defecto configurada a memoria/H2 en dev) se poblará automáticamente mediante Flyway con el set de cartas inicial (`V2__seed_xy1_cards.sql`).

### Ejecutar los Tests y CI
Para correr toda la suite de tests (1.181 tests a la fecha) y verificar métricas de calidad (Jacoco Coverage y Checkstyle), ejecutar:
```bash
./mvnw clean verify
```
> **Aviso:** Si este comando falla, el pipeline de GitHub Actions también fallará. Siempre asegurate de que `verify` corra exitosamente antes de pushear código a `main`.

## 3. Compilar y Ejecutar el Frontend

El frontend está construido en Angular 21+.

### Levantar el entorno
1. Abrir una terminal en la carpeta `FE/`.
2. Instalar dependencias (solo la primera vez):
   ```bash
   npm install
   ```
3. Ejecutar el servidor de desarrollo:
   ```bash
   npm start
   ```
El Frontend levantará por defecto en `http://localhost:4200` y estará configurado para golpear la API local del Backend en el puerto 8080.

## 4. Troubleshooting
- **NullPointer en Tests de MatchService:** Si agregás una nueva dependencia al Game Engine o al MatchService, actualizá **todos** los tests unitarios vinculados para mockear la inyección de esa dependencia.
- **`swagger.json` aparece modificado tras correr los tests:** es un efecto secundario esperado de `SpringDocConfigTest` (regenera `BE/docs/api_doc/swagger.json` en cada corrida). Si no estabas tocando la API, hacé `git checkout -- BE/docs/api_doc/swagger.json` antes de commitear para no ensuciar el diff.
