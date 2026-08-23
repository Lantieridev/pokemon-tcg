# 0004 — Dominio Store migrado a Clean/Hexagonal Architecture (slice de prueba de concepto)

## Estado
Aceptada

## Contexto
El backend entero usa package-by-layer (`controllers/`, `services/`, `services/impl/`, `persistence/entity`, `persistence/repository`) fuera del `engine/` (ver [ADR 0001](./0001-hexagonal-engine-isolation.md), que aísla solo el motor de reglas, no el resto de la aplicación). El issue #12 propone migrar gradualmente el resto del backend a Clean/Hexagonal Architecture, pero migrar todo de una vez es de alto riesgo — hay que probar el patrón en un dominio chico y autocontenido antes de tocar dominios grandes o el propio `engine/`.

Se evaluaron dos candidatos: **Store** (`StoreController` de 36 líneas → `StoreService`/`StoreServiceImpl` de 96 líneas → `StoreItemEntity`/`StoreItemRepository`, dos endpoints, un solo colaborador externo a su propio agregado) y **Profile** (`ProfileServiceImpl`, 943 líneas, muchos más colaboradores). Store es la porción más chica y autocontenida, por lo que se eligió como slice de prueba de concepto — `engine/`, `GameFacade`, `MatchService` y `MatchSession` quedan explícitamente fuera de esta primera migración por ser las piezas más centrales y riesgosas.

## Decisión
Se creó el paquete `ar.edu.utn.frc.tup.piii.store` (al lado de, no dentro de, `controllers`/`services`/`persistence`), estructurado como ports & adapters:

```
store/
  domain/                    — lógica de negocio sin dependencias a frameworks
    StoreItem, StoreItemType (enum propio, independiente del enum de persistencia)
    UserStoreAccount         (inmutable; alreadyOwns/canAfford/purchase viven acá)
    exception/                — jerarquía StoreException (reemplaza IllegalArgumentException)
  application/
    port/in/                  — ListAvailableStoreItemsUseCase, PurchaseStoreItemUseCase
    port/out/                 — StoreItemRepositoryPort, UserStoreAccountPort
    service/                  — StoreCatalogService, PurchaseStoreItemService (los casos de uso)
  adapter/
    in/web/                   — StoreController + DTOs de request/response
    out/persistence/          — adapters JPA + mappers hacia/desde StoreItemEntity/UserEntity
```

Se eliminaron `controllers/StoreController.java`, `services/StoreService(Impl).java`, `dtos/StoreItemDTO.java` y `dtos/BuyRequestDTO.java`.

`UserEntity` (agregado compartido por auth, perfil, amistades, honor, etc.) **no** fue migrado — `UserStoreAccountPort` es un puerto angosto, definido por el consumidor: expone solo lo que una compra necesita (balance, títulos/avatares desbloqueados, inventario de sobres), y el adapter de persistencia lee/escribe únicamente esos campos sobre el `UserEntity`/`UserRepository` existente. Es una técnica hexagonal legítima, pero implica que `UserEntity` en sí sigue siendo un god-object acoplado a JPA hasta que un futuro slice de "User/Profile" lo aborde directamente.

## Consecuencias
- **El resto del backend sigue plano.** No asumas que otro dominio (`services/impl/ProfileServiceImpl`, `FriendshipServiceImpl`, etc.) ya sigue el patrón hexagonal solo porque `store/` lo hace — ver la nota en la sección 5 de [`01_arquitectura_y_flujo.md`](../01_arquitectura_y_flujo.md#5-convenciones-de-código).
- El contrato JSON de `/api/store/items` y `/api/store/buy` no cambió (mismos nombres de campo), solo los nombres de los DTOs (`StoreItemDTO`→`StoreItemResponse`, `BuyRequestDTO`→`PurchaseItemRequest`) y su paquete.
- Se encontró y corrigió una regresión real durante la migración: `springdoc.packages-to-scan` (en `application.properties` y `src/test/resources/application.properties`) tenía hardcodeado solo `ar.edu.utn.frc.tup.piii.controllers`, así que mover `StoreController` fuera de ese paquete hacía desaparecer `/api/store/*` de `/v3/api-docs` y `docs/api_doc/swagger.json` silenciosamente (la app y la seguridad seguían funcionando, solo Swagger dejaba de listar los endpoints). Cada futuro slice que agregue un nuevo paquete `adapter.in.web` tiene que sumarse a ambos `packages-to-scan` o sus endpoints desaparecen de la documentación sin error visible.
- Plan de slices futuros propuesto (orden de menor a mayor riesgo, ninguno arrancado todavía): Pack → Friendship → BattlePass → Chat (primer adapter WebSocket además de REST) → History/Replay/Ranking → Profile (una vez probado el patrón de puerto angosto en 2-3 slices chicos) → `engine/`/`GameFacade`/`MatchService`/`MatchSession` (al final, a propósito, y probablemente con su propia discusión de diseño).
