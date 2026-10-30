# Arquitectura y modelo de datos

## Vista general

```text
Android: Compose -> ViewModel -> contrato de repositorio
                                  |-> Room / SQLite
                                  |-> Retrofit / OkHttp
                                              |
                                          HTTPS API Gateway
                                              |
                                          HTTPS Nginx (EC2)
                                              |
                                     Spring Boot (127.0.0.1:8080)
                                              |
                                     PostgreSQL (127.0.0.1:5432)

Cognito Hosted UI -> callback Android -> sesión local
Cognito JWKS -> validación del access token en Spring Security
```

Es un cliente móvil con backend por capas y base de datos relacional. API Gateway enruta; no contiene las reglas de negocio. Cognito gestiona la autenticación, mientras Spring autoriza cada operación. SyncPassWeb consume la misma API desde otro repositorio.

## Frontend Android: MVVM con repositorios

Paquete base: `com.rojas.syncpass`, dentro de `Android/app/src/main/java/`.

| Ubicación | Responsabilidad | Ejemplos |
|---|---|---|
| `ui/` | Composición visual, navegación y eventos del usuario | HomeScreen, GestionScreen, EventoDetalleScreen, QrScannerScreen |
| `ui/*/*ViewModel` | Coordinar operaciones y construir estado observable | HomeViewModel, GestionViewModel, ScannerViewModel |
| `domain/` | Contratos, modelos y errores del negocio | Repositories.kt, Models.kt, AppException |
| `data/repository/` | Acceso coordinado a fuentes remotas/locales | SyncPassRepository, ApiProyectosRepository, RoomRecintosRepository |
| `data/remote/` | Contratos Retrofit, DTO HTTP y conversión de errores | SyncPassApi, ApiModels, ApiClient, ResultBoundary |
| `data/local/` | Tablas SQLite, consultas y transacciones Room | SyncPassDatabase, SyncPassDao, RecintoDao, Entities |
| `data/mapper/` | Conversión entre DTO, entidades y dominio | ModelMappers |
| `data/session/` | Sesión y cifrado de tokens | SessionStore, TokenVault |
| `core/` | Configuración, formato, lectura JWT y ubicación | AppConfig, FormatoFechaHora, UbicacionHelper |
| `work/` | Ejecución diferida de la sincronización | SyncWorker, SyncScheduler |

`SyncPassApplication` crea `AppContainer`. La composición de dependencias es manual; no se utiliza Hilt/Dagger. El contenedor crea API, Room y repositorios por cuenta. El módulo `:cognito-auth`, paquete `com.cognitoauth`, encapsula AppAuth, descubrimiento OIDC, solicitudes y canje de código. La app conserva la navegación y persistencia de sesión.

Los ViewModels usan `viewModelScope`, Flow/StateFlow y estados inmutables. Compose recibe estado y callbacks; los composables no acceden directamente a SQL o Retrofit. Room emite los cambios de datos que alimentan la UI. `stateIn` y `WhileSubscribed` permiten compartir flujos durante la observación.

Se utiliza MVVM con separación presentación/dominio/datos, no una implementación estricta de Clean Architecture: no existe una clase de caso de uso para cada acción y `SyncPassRepository` sigue coordinando varias capacidades. La delegación a repositorios de proyectos y recintos reduce parte de esa responsabilidad.

### Dashboards

Android utiliza `HomeScreen` compartida con secciones y acciones condicionales de `HomeUiState`. No son tres Activities ni tres pages React separadas. Cambian el encabezado, los botones, las métricas y las acciones del detalle según rol. La navegación se centraliza en `ui/navigation/SyncPassApp.kt`.

### Ejemplo: editar evento

`EventoEditorDialog` valida los pasos y genera `EventoInput`. `EventoDetalleViewModel` verifica que se pueda gestionar el proyecto y llama a `SyncPassOperations.editarEvento`. El repositorio envía PUT mediante Retrofit; Spring valida DTO, rol y propiedad; el servicio actualiza la entidad en una transacción. Solo con respuesta exitosa se actualiza Room. El flujo emite el nuevo evento y la interfaz cierra la edición tras la confirmación. Un error conserva el formulario.

### Ejemplo: recinto local

`GestionScreen` -> `GestionViewModel` -> contrato de recintos -> `RoomRecintosRepository` -> `RecintoDao` -> SQLite. No existe solicitud HTTP para guardar el recinto. Elegirlo en el formulario copia sus campos al evento; guardar ese evento sí envía esos valores a la API.

## Backend fs2-api: Spring por capas

Ruta comprobada en este repositorio: `Backend/src/main/java/com/duoc/cursos/`. El nombre histórico del paquete y servicio sigue siendo `cursos`; el producto es SyncPass.

| Paquete | Función |
|---|---|
| `controller` | Rutas, validación de entrada, permisos de método y delegación; devuelve DTO |
| `service` | Reglas del negocio, propiedad, transacciones, inscripciones, asistencia y encuestas |
| `repository` | Interfaces Spring Data JPA y consultas/bloqueos de persistencia |
| `model` | Entidades y enumeraciones; relaciones de datos |
| `dto` | `ApiDtos`, records de solicitudes, respuestas y errores |
| `security` | Normalización de grupos, validación de access tokens y sincronización del usuario |
| `integration` | Comunicación con Cognito mediante AWS SDK |
| `config` | Spring Security, CORS y clientes AWS |
| `exception` | Traducción centralizada de excepciones HTTP y validación |

La inyección del backend se realiza por constructor. Spring Security configura un Resource Server JWT sin sesión HTTP. `UsuarioSincronizadoFilter`, `SesionUsuarioService` y `PerfilUsuarioWriter` relacionan la identidad Cognito con el usuario local. Los servicios usan `@Transactional`. JPA mantiene entidades; los controladores retornan records y no las entidades completas.

No existe un mapper independiente para todo: conversiones como `toResponse` están en servicios. Esto es una decisión del código actual, no una omisión que deba ocultarse. El controlador health construye una respuesta simple; no ejecuta lógica del negocio.

## Modelo de datos

- Usuario: identidad local, vínculo Cognito, nombre y roles. Un usuario participa en inscripciones y puede ser responsable de proyectos.
- Proyecto: nombre, descripción, área temática, unidad vecinal, fechas, estado y responsable. Tiene varios eventos.
- Evento: proyecto, datos de actividad, LocalDateTime, cupos, ubicación, radio, QR y estado.
- Inscripción: une usuario y evento, con estado y fecha de creación.
- Asistencia: une usuario y evento, presente, timestamp, origen, ubicación opcional y clientUuid.
- Encuesta: asociada a asistencia; satisfacción, utilidad, comentarios y fecha.
- Recinto local: nombre, dirección, coordenadas y radio; no es una entidad de la API.
- Pendiente local: tipo de operación, evento, UUID, contenido necesario y estado/error de envío.

Los conteos de inscritos/presentes se calculan en el servidor; no son una autorización aportada por el cliente. Las fechas de eventos usan `LocalDateTime` sin zona; los timestamps de asistencia/envío usan `Instant`.

Room mantiene caché de perfil, proyectos y eventos, además de recintos y pendientes. PostgreSQL mantiene las entidades del negocio. Las migraciones del servidor están en `Backend/src/main/resources/db/migration/`; Flyway aplica el esquema y Hibernate lo valida. El perfil local H2 crea y descarta el esquema para desarrollo.

## Límites arquitectónicos

La base local no es una réplica completa de todas las tablas del servidor. Las listas de asistentes y encuestas se consultan remotamente. Las consultas de conteos por evento pueden aumentar el número de accesos JPA con catálogos grandes; no hay benchmark de carga ni paginación completa que justifique prometer escalabilidad ilimitada.
