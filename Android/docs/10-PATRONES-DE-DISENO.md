# 10. Patrones de Diseño

## Backend (fs2-api)

| Patrón | Aplicación real | Descripción |
|---|---|---|
| **Layered Architecture** | `controller` → `service` → `repository` | Separación de responsabilidades |
| **DTO Pattern** | `dto/ApiDtos.java` (records) | Contratos independientes de las entidades JPA |
| **Repository Pattern** | `repository/*Repository` | Abstracción Spring Data JPA |
| **Dependency Injection** | Constructor injection | Sin field injection |
| **REST/MVC** | `@RestController` | Recursos HTTP |
| **Centralized Exception Handling** | `GlobalExceptionHandler` | Traduce excepciones a `{error, mensaje, status, detalles}` |
| **Transactional** | `@Transactional` en services | Consistencia en inscripción/escaneo/encuesta |
| **Policy/Converter** | `CognitoRoles.normalize` | Grupos Cognito → roles internos |
| **Ownership Guard** | `ProyectoService.requireOwnerOrAdmin` | Autorización a nivel de recurso |
| **Idempotency Key** | `client_uuid` en `Asistencia` | Reintentos seguros |
| **DTO Mapping** | Métodos `toResponse` en services | Ensamblado de respuestas |

## Móvil (FormularioKotlin)

| Patrón | Aplicación real | Descripción |
|---|---|---|
| **MVVM** | `HomeViewModel`, `EventoDetalleViewModel` | UI + `StateFlow` |
| **Repository** | `SyncPassRepository` | Fuente única de verdad (Room + API + cola) |
| **Singleton** | `SyncPassDatabase.obtener`, `AppContainer`, `ApiClient` (object) | Instancias únicas |
| **Observer** | `Flow`/`StateFlow` de Room y ViewModels | UI reactiva |
| **Factory** | `HomeViewModelFactory`, `EventoDetalleViewModelFactory`, `CognitoViewModelFactory` | Creación de ViewModels con dependencias |
| **Interceptor** | Lambda en `ApiClient.crear` | Inyecta `Authorization: Bearer` |
| **Offline-First / Write-Through** | Room primero, luego API | La UI nunca espera a la red |
| **Queue / Outbox** | `PendienteEntity` + `SyncWorker` | Cola de mutaciones |
| **Optimistic UI** | `marcarInscrito`, `marcarPresente`, `marcarEncuesta` | Estado local inmediato |
| **Manual DI** | `AppContainer` | Sin framework de inyección |
| **Facade** | `:cognito-auth` (`CognitoAuthManager`) | Aísla AppAuth del resto de la app |

## Patrones transversales

| Patrón | Dónde | Descripción |
|---|---|---|
| **Idempotency** | `clientUuid` (escaneos), unique constraints | Evita duplicados al reintentar |
| **Error classification** | `esFalloReintentable` al encolar | No encola `4xx` definitivos |
| **RBAC** | `cognito:groups` → `ROLE_*` + `@PreAuthorize` | Autorización por rol |
| **Ownership validation** | Services del backend | Docente solo opera sobre sus proyectos |
| **Fail-fast** | Bean Validation + reglas en service | Errores tempranos y claros |
| **Defensive fallback** | `miQr()` con `runCatching` en `refrescar()` | Un fallo secundario no rompe la carga del catálogo |
| **Configuration as data** | `BuildConfig` desde `.env` | Sin secretos hardcodeados |

## Ejemplos reales

| Patrón | Ubicación |
|---|---|
| Repository | `app/.../data/repository/SyncPassRepository.kt` |
| DI manual | `app/.../SyncPassApplication.kt` (`AppContainer`) |
| Interceptor Bearer | `app/.../data/remote/ApiClient.kt` |
| Cola offline | `app/.../data/local/Entities.kt` (`PendienteEntity`) |
| Worker | `app/.../work/SyncWorker.kt` |
| Normalización de roles | `fs2-api/.../security/CognitoRoles.java` |
| Manejo de errores | `fs2-api/.../exception/GlobalExceptionHandler.java` |
| Reglas de negocio | `fs2-api/.../service/EventoService.java` |
| Sincronización batch | `fs2-api/.../service/SyncService.java` |

## Ventajas

- **Testeable:** capas separadas y servicios con reglas aisladas.
- **Mantenible:** cambios localizados por capa.
- **Extensible:** nuevos tipos de operación offline reutilizan la cola.
- **Resiliente:** la app sigue operando sin red y reconcilia después.
- **Seguro:** JWT validado en servidor + RBAC + ownership.
