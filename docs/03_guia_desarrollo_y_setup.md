# Guía de Desarrollo y Setup

Este documento contiene las instrucciones fundamentales para compilar, probar y ejecutar el proyecto localmente.

## 1. Prerrequisitos
- **Java:** JDK 21
- **Node.js:** v20+
- **Base de datos:** PostgreSQL (usado en producción) o H2 (usado por defecto en perfiles de test).
- **Herramientas de Build:** Maven (Backend) y Angular CLI (Frontend). El repo ya incluye el wrapper de Maven (`mvnw`).

> [!CAUTION]
> **REGLA ESTRICTA DE GIT:** Todos los commits en este repositorio DEBEN realizarse bajo la cuenta de la facultad. Antes de hacer tu primer commit, ejecuta localmente en la carpeta del proyecto:
> `git config user.name "Lantieri-Martin"`
> `git config user.email "421312@tecnicatura.frc.utn.edu.ar"`
> (Reemplazar con los datos del integrante del equipo correspondiente). Nunca utilices correos personales (`@gmail.com`) porque anula la validez académica del commit.

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
Para correr toda la suite de tests (870+ tests) y verificar métricas de calidad (Jacoco Coverage y Checkstyle), ejecutar:
```bash
./mvnw clean verify
```
> **Aviso:** Si este comando falla, el pipeline de GitHub Actions también fallará. Siempre asegurate de que `verify` corra exitosamente antes de pushear código a `develop`.

## 3. Compilar y Ejecutar el Frontend

El frontend está construido en Angular 18+.

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
- **Fallas de Checkstyle (HiddenField):** Asegurate de que los parámetros de constructores no tengan exactamente el mismo nombre que los atributos de clase si no usás el prefijo `this.atributo = parametro` directamente.
- **NullPointer en Tests de MatchService:** Si agregás una nueva dependencia al Game Engine o al MatchService, actualizá **todos** los tests unitarios vinculados para mockear la inyección de esa dependencia.
