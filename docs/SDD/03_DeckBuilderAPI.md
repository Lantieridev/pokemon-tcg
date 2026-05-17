# SDD Módulo 3: Deck Builder & API Integration

**Responsables:** [Asignar 1 desarrollador]

Este módulo provee la integración con la API externa requerida (pokemontcg.io) y valida la creación de mazos para los usuarios, asegurando que se cumplan las reglas de torneo antes de iniciar una partida.

## Objetivo
Implementar la lógica de creación de mazos consumiendo la información de cartas del set oficial XY1, cacheadas en la base de datos local para cumplir con RNF-01 (< 500ms response time) y evitar abuso de API externa durante el juego.

## Características

### 1. Seeder o Poblamiento Inicial
- La consigna indica el uso obligatorio del **Set xy1** (146 cartas).
- El backend no debe consultar la API externa en medio de una partida.
- El módulo se encargará de ejecutar un script inicial (o tener un `data.sql` pre-generado) que consulte `https://api.pokemontcg.io/v2/cards?q=set.id:xy1` y almacene un JSON estructurado de cada carta (HP, tipo, debilidades, costo de ataques) en la base de datos relacional.

### 2. Constructor de Mazos (Deck Builder)
Permite al usuario crear, modificar y guardar sus mazos de 60 cartas.

**Validaciones estrictas (Implementadas en backend, reflejadas en UI):**
1. El mazo debe contener **exactamente 60 cartas**.
2. **Máximo 4 copias** de una carta con el mismo nombre.
   - *Excepción:* Las cartas de **Energía Básica** (Fuego, Agua, Planta, Eléctrico, Psíquico, Lucha, Oscuridad, Metálico, Hada) no tienen límite.
3. El mazo debe tener **al menos 1 Pokémon Básico**.
4. **Máximo 1 carta AS TÁCTICO** en todo el mazo, independientemente del nombre.

### 3. Modelo de Mazo
- Entidad `Deck` que pertenece a un `User`/`Player`.
- Relación `@OneToMany` o almacenamiento de JSON Array con los IDs de las cartas.
- Un controlador REST clásico para que el FrontEnd arme el drag & drop del builder.
