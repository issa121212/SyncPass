# 09. Arquitectura Móvil (FormularioKotlin)

## Módulos

| Módulo | Contenido |
|---|---|
| `:app` | UI Compose, navegación, Room, Retrofit, repositorio, WorkManager, configuración |
| `:cognito-auth` | Fachada reutilizable sobre AppAuth: `CognitoConfig`, `CognitoAuthManager`, `CognitoAuthViewModel`, `CognitoTokens` |

## Stack

| Componente | Tecnología | Uso |
|---|---|---|
| **UI** | Jetpack Compose + Material 3 | Interfaz declarativa |
| **Navegación** | Navigation Compose | Rutas `home`, `evento/{id}`, `scanner/{id}` |
| **Arquitectura** | MVVM + StateFlow | Estado y lógica por pantalla |
| **Local DB** | Room | Cache del catálogo y cola `pendientes` |
| **Red** | Retrofit + OkHttp + Gson | Cliente HTTP con interceptor Bearer |
| **Background** | WorkManager | Sincronización periódica (15 min) |
| **Auth** | AppAuth (módulo `:cognito-auth`) | Cognito Hosted UI + Authorization Code + PKCE |
| **QR** | CameraX + ML Kit (escaneo) + ZXing (generación) | Escaneo y render de QR |
| **DI** | Manual (`AppContainer`) | Sin Hilt/Dagger |
| **Sesión** | SharedPreferences (`SessionStore`) | Tokens Cognito |

## Estructura de Paquetes (`:app`)

```
app/src/main/java/com/rojas/syncpass/
├── MainActivity.kt          # entry point, launchers de login/logout
├── SyncPassApplication.kt   # Application + AppContainer
├── core/
│   ├── config/              # AppConfig (API_BASE_URL), CognitoAppConfig
│   ├── jwt/                 # JwtDecoder, SessionUser
│   └── util/                # FechaUtils, UbicacionHelper
├── data/
│   ├── local/               # Entities, SyncPassDao, SyncPassDatabase
│   ├── remote/              # SyncPassApi, ApiModels, ApiClient
│   ├── repository/          # SyncPassRepository
│   └── session/             # SessionStore
├── ui/
│   ├── components/          # AuthCard, QrCode
│   ├── event/               # EventoDetalleScreen + ViewModel
│   ├── home/                # HomeScreen, CreateDialogs, ViewModel, UiState
│   ├── login/               # LoginScreen, CognitoViewModelFactory
│   ├── navigation/          # SyncPassApp (NavHost)
│   ├── scanner/             # QrScannerScreen
│   └── theme/               # Material 3
└── work/                    # SyncWorker + SyncScheduler
```

## Capas

| Capa | Responsabilidad |
|---|---|
| **UI** | Composables, estado visual, permisos runtime |
| **ViewModel** | `StateFlow` de UI, orquestación del repositorio |
| **Repository** | Fuente única de verdad; coordina Room, Retrofit y la cola offline |
| **Remote** | Contrato Retrofit y DTOs |
| **Local** | Room: proyectos, eventos, perfil, pendientes |
| **Session** | Tokens y expiración |
| **Worker** | Sincronización en background |

## Inyección de dependencias manual

`SyncPassApplication` crea `AppContainer` en `onCreate()`:

```kotlin
class AppContainer(application: Application) {
    val sessionStore = SessionStore(application)
    private val database = SyncPassDatabase.obtener(application)
    private val api = ApiClient.crear(AppConfig.apiBaseUrl, sessionStore)
    val repository = SyncPassRepository(api, database.dao(), sessionStore)
}
```

`SyncScheduler.programar(this)` registra el worker periódico al arrancar.
Las pantallas obtienen el repositorio desde `(application as SyncPassApplication).container`.

## Configuración desde `.env`

`app/build.gradle.kts` lee `.env` y genera `BuildConfig`:

```
API_BASE_URL, COGNITO_ISSUER, COGNITO_CLIENT_ID,
COGNITO_REDIRECT_URI, COGNITO_LOGOUT_URI, COGNITO_SCOPES
```

- `AppConfig.apiBaseUrl` normaliza la barra final.
- `CognitoAppConfig.isConfigured` es `false` si faltan issuer o client ID;
  en ese caso `MainActivity` muestra un aviso en vez de crear el manager.
- No hay valores hardcodeados en archivos `.kt`.

## Sesión

- `SessionStore` guarda `access_token`, `id_token`, `refresh_token`,
  `expires_in` y `saved_at` en SharedPreferences.
- `hasValidSession()` decodifica el access token y verifica `exp`.
- `ApiClient` agrega `Authorization: Bearer <access_token>` si existe.
- No se implementa refresh automático: token expirado → login.
- Logout: `CognitoAuthManager` abre el end-session endpoint y
  `SessionStore.clear()`.

## Navegación por rol

| Ruta | Pantalla | Visible para |
|---|---|---|
| `home` | Home con eventos, proyectos y pendientes | Todos |
| `evento/{id}` | Detalle del evento | Todos |
| `scanner/{id}` | Escáner QR de estudiante | Docente (botón) |

El rol se obtiene de `/api/auth/me` (`roles`) y se guarda en `perfil`.
`HomeUiState.esDocente` / `esEstudiante` habilitan acciones:

- **Docente:** "Nuevo proyecto", "Nuevo evento", "Escanear QR", asistentes,
  encuestas.
- **Estudiante:** "Mi QR", "Inscribirme", "Marcar mi asistencia",
  "Responder encuesta".

## Base de datos local

Entidades reales (ver [11-MODELO-DATOS.md](11-MODELO-DATOS.md)):

- `ProyectoEntity`
- `EventoEntity`
- `PerfilEntity` (fila única)
- `PendienteEntity` (cola offline)

Un único DAO (`SyncPassDao`) expone `Flow` para observación reactiva.

## Permisos

| Permiso | Uso | Gestión |
|---|---|---|
| `INTERNET` | Llamadas HTTP | Manifest |
| `CAMERA` | Escaneo QR | Runtime (`QrScannerScreen`) |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | GPS al marcar asistencia | Runtime (`EventoDetalleScreen`); si se niega, se marca sin GPS |

## WorkManager

- `SyncWorker`: si no hay sesión válida termina con éxito; si
  `repository.sincronizar()` falla, devuelve `Result.retry()`.
- `SyncScheduler`: `PeriodicWorkRequest` de 15 min con
  `NetworkType.CONNECTED` y política `KEEP`.
- El botón **Sincronizar** de Home llama directamente al repositorio.

## UI/UX

- Material 3, estados de carga/error/vacío.
- Indicador de operaciones pendientes en Home.
- Formularios validan fechas antes de enviar (`FechaUtils.aIsoLocal`).
- Feedback offline: los cambios se reflejan de inmediato en Room.
