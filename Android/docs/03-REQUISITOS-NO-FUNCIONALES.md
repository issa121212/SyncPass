# 03. Requerimientos No Funcionales

## RNF-SEG-01: Seguridad
| ID | Requerimiento | Estado |
|---|---|---|
| RNF-SEG-01.1 | Autenticación con OAuth2/OIDC vía AWS Cognito (Authorization Code + PKCE). | Implementado |
| RNF-SEG-01.2 | Backend valida JWT con JWKS, verifica `iss`, `exp`, `token_use`, `client_id`. | Implementado |
| RNF-SEG-01.3 | Autorización por roles desde `cognito:groups` mapeados a `ROLE_DOCENTE`, `ROLE_ESTUDIANTE`, `ROLE_ADMIN`. | Implementado |
| RNF-SEG-01.4 | Protección de endpoints con `@PreAuthorize`/configuración de seguridad. | Implementado |
| RNF-SEG-01.5 | Validación de ownership: solo responsable del proyecto/evento puede gestionar. | Implementado |
| RNF-SEG-01.6 | Tokens almacenados de forma segura en app móvil (SharedPreferences). | Implementado |
| RNF-SEG-01.7 | Uso de HTTPS en producción (configurable). | Requerido |

## RNF-ARQ-01: Arquitectura
| ID | Requerimiento | Estado |
|---|---|---|
| RNF-ARQ-01.1 | Backend por capas: Controller, Service, Repository, DTO, Entity, Config, Security. | Implementado |
| RNF-ARQ-01.2 | Móvil con arquitectura MVVM: UI (Compose), ViewModel, Repository, DataSource (Remote/Local). | Implementado |
| RNF-ARQ-01.3 | Separación clara entre modelos de red (DTO), dominio/local (Room) y UI. | Implementado |
| RNF-ARQ-01.4 | Inyección de dependencias manual en app móvil (sin Hilt/Dagger). | Implementado |

## RNF-OFF-01: Offline-First y Confiabilidad
| ID | Requerimiento | Estado |
|---|---|---|
| RNF-OFF-01.1 | Todas las operaciones mutantes se registran en cola local antes de sincronizar. | Implementado |
| RNF-OFF-01.2 | App usable sin conexión para visualizar datos cacheados. | Implementado |
| RNF-OFF-01.3 | Sincronización automática con WorkManager (periodic + constraints de red). | Implementado |
| RNF-OFF-01.4 | Diferenciación entre errores reintentables (red, timeout, 5xx, 408, 429) y definitivos (4xx cliente). | Implementado |
| RNF-OFF-01.5 | Idempotencia con `clientUuid` para evitar duplicados en sincronización. | Implementado |
| RNF-OFF-01.6 | Cola persiste entre reinicios de app. | Implementado |

## RNF-PERF-01: Rendimiento
| ID | Requerimiento | Estado |
|---|---|---|
| RNF-PERF-01.1 | Carga de listas con paginación donde aplique. | Parcial |
| RNF-PERF-01.2 | Uso de Room para cache y consultas locales eficientes. | Implementado |
| RNF-PERF-01.3 | Interceptores OkHttp para autenticación y logging. | Implementado |
| RNF-PERF-01.4 | Sincronización por lotes (`/api/sync/asistencias`, `/api/sync/encuestas`). | Implementado |

## RNF-MANT-01: Mantenibilidad
| ID | Requerimiento | Estado |
|---|---|---|
| RNF-MANT-01.1 | Código en español según convención del proyecto. | Implementado |
| RNF-MANT-01.2 | Migraciones con Flyway para esquema de BD. | Implementado |
| RNF-MANT-01.3 | DTOs separados de entidades JPA. | Implementado |
| RNF-MANT-01.4 | Validaciones con Bean Validation. | Implementado |
| RNF-MANT-01.5 | Manejo centralizado de excepciones en backend. | Implementado |

## RNF-USA-01: Usabilidad
| ID | Requerimiento | Estado |
|---|---|---|
| RNF-USA-01.1 | UI con Material 3 y Jetpack Compose. | Implementado |
| RNF-USA-01.2 | Navegación intuitiva por rol. | Implementado |
| RNF-USA-01.3 | Feedback visual para estado offline/sincronización. | Implementado |
| RNF-USA-01.4 | Escaneo QR con permisos de cámara gestionados. | Implementado |
| RNF-USA-01.5 | Ubicación GPS con permisos gestionados. | Implementado |

## RNF-DAT-01: Integridad de Datos
| ID | Requerimiento | Estado |
|---|---|---|
| RNF-DAT-01.1 | Restricciones únicas en BD (inscripción, encuesta). | Implementado |
| RNF-DAT-01.2 | Relaciones con llaves foráneas. | Implementado |
| RNF-DAT-01.3 | Validación de cupos en inscripción/asistencia. | Implementado |
| RNF-DAT-01.4 | Verificación de estado "presente" para responder encuesta. | Implementado |
| RNF-DAT-01.5 | Unicidad por (estudiante_id, evento_id) para inscripción. | Implementado |

## Limitaciones Conocidas
- Sin refresh token automático en cliente móvil (expiración -> logout)
- PostgreSQL/Flyway validación limitada sin Docker
- Sin OpenAPI/Swagger
- Sin notificaciones push
- Sin panel web administrativo
