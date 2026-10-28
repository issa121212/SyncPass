# 08. Arquitectura Backend (fs2-api)

## Stack

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation, Actuator) |
| Autenticación | OAuth2 Resource Server + JWT de Cognito (JWKS) |
| Base de datos | PostgreSQL (Flyway + `validate`) / H2 en perfil `local` |
| Build | Gradle Wrapper |
| Utilidades | Lombok, java-dotenv, AWS SDK v2 (Cognito) |

## Estructura de Paquetes

```
src/main/java/com/duoc/cursos/
├── CursosApplication.java      # main + carga de .env
├── config/
│   ├── SecurityConfig.java     # JWT, CORS, rutas públicas, @EnableMethodSecurity
│   ├── CorsProperties.java
│   └── CognitoConfig.java
├── controller/
│   ├── HealthController.java   # GET /api/health
│   ├── AuthController.java     # /api/auth/*
│   ├── UsuarioController.java  # /api/usuarios/*
│   ├── ProyectoController.java # /api/proyectos
│   ├── EventoController.java   # /api/eventos
│   └── SyncController.java     # /api/sync
├── dto/ApiDtos.java            # records de request/response
├── exception/GlobalExceptionHandler.java
├── model/                      # entidades JPA + enums
├── repository/                 # Spring Data JPA
├── security/CognitoRoles.java  # normalización de grupos
└── service/                    # reglas de negocio
```

## Capas

| Capa | Responsabilidad |
|---|---|
| **Controller** | Exposición REST, `@Valid`, `@PreAuthorize`, códigos HTTP |
| **Service** | Reglas de negocio: ownership, cupos, idempotencia, transacciones |
| **Repository** | Acceso a datos con Spring Data JPA |
| **Model** | Entidades JPA y enums persistentes |
| **DTO** | Contratos API separados de las entidades |
| **Security** | Validación JWT y mapeo de grupos a autoridades |
| **Config** | Seguridad, CORS, propiedades |

## Seguridad

### Flujo JWT

1. La app envía `Authorization: Bearer <access_token>`.
2. `SecurityConfig` valida la firma con el JWKS de Cognito
   (`NimbusJwtDecoder`) y luego:
   - `iss` (issuer configurado),
   - `exp` (expiración estándar),
   - `token_use = access`,
   - `client_id` contra `aws.cognito.client-id`.
3. El claim `cognito:groups` se normaliza con `CognitoRoles`:
   - `profesor`, `docente`, `teacher`, `coordinador` → `DOCENTE`
   - `alumno`, `estudiante`, `student` → `ESTUDIANTE`
   - `admin` → `ADMIN` y `DOCENTE`
   - sin grupos → `ESTUDIANTE` por defecto
4. Las autoridades quedan como `ROLE_DOCENTE`, `ROLE_ESTUDIANTE`,
   `ROLE_ADMIN` y se usan con `@PreAuthorize`.

### Rutas públicas

- `GET /api/health`
- `POST /api/auth/login`
- `POST /api/auth/registro`
- `POST /api/auth/olvide-contrasena/**`
- Preflight CORS

Todo lo demás exige JWT válido.

### Ownership

`ProyectoService` y `EventoService` verifican que el docente autenticado sea
el responsable del proyecto (o `ADMIN`) antes de modificar, ver
asistentes, escanear QR o listar encuestas.

## Flujo de arranque

1. `CursosApplication.main()` ejecuta `loadDotenv()`: lee `.env` de
   `user.dir` y lo publica como system properties (sin sobrescribir
   variables de entorno reales).
2. `application.yml` activa `spring.profiles.active` desde
   `SPRING_PROFILES_ACTIVE` (default `default`).
3. En perfil `local` se carga `application-local.yml` (H2 en memoria,
   Flyway deshabilitado). Sin perfil, se usa PostgreSQL + Flyway.
4. Se registran los beans de seguridad; el `JwtDecoder` no hace discovery
   al arrancar (solo descarga JWKS al validar el primer token).

## Reglas de negocio

| Regla | Implementación |
|---|---|
| **Auto-provisión de usuario** | `UsuarioService.getCurrentUsuario()` crea el usuario desde los claims del JWT la primera vez |
| **Ownership de proyecto** | Responsable o `ADMIN` |
| **Cupos** | `validarCupo`: si `cupos > 0` y los inscritos alcanzan el límite → `422` |
| **Inscripción idempotente** | Si ya existe `(evento, usuario)` devuelve la inscripción |
| **Inscripción automática al escanear** | El escaneo crea inscripción + asistencia si el estudiante no estaba inscrito |
| **Asistencia idempotente** | Escaneo por `client_uuid`; asistencia propia por `(evento, usuario)` |
| **Encuesta solo con presencia** | Requiere asistencia con `es_presente = true` |
| **Una encuesta por asistencia** | Unique `asistencia_id`; se actualiza si se repite |

## Manejo de errores

`GlobalExceptionHandler` centraliza:

| Excepción | HTTP |
|---|---|
| `MethodArgumentNotValidException` | `400` con `detalles` por campo |
| `ResponseStatusException` | El código indicado (`403`, `404`, `422`, etc.) |
| `AccessDeniedException` | `403` |
| `DataIntegrityViolationException` | `409` |
| `UsernameExistsException`, `UserNotFoundException`, `NotAuthorizedException` (Cognito) | `400`/`404`/`401` |
| `TooManyRequestsException` | `429` |
| Genéricas | `500` |

Formato: `{ error, mensaje, status, detalles }`.

## Base de datos y migraciones

- `src/main/resources/db/migration/V1__syncpass_schema.sql`.
- En PostgreSQL/Flyway, `ddl-auto: validate`: el esquema debe coincidir con
  las entidades.
- En `local`, H2 crea el esquema con `create-drop` y no ejecuta Flyway.

## Variables principales

| Variable | Uso |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` (H2) o perfil con PostgreSQL |
| `SERVER_PORT` | Puerto HTTP (default 8080) |
| `DATABASE_URL` / `DATABASE_USER` / `DATABASE_PASSWORD` | PostgreSQL |
| `COGNITO_ISSUER` | Issuer del User Pool |
| `COGNITO_JWK_SET_URI` | JWKS (derivado del issuer por defecto) |
| `AWS_REGION` | Región de Cognito |
| `AWS_COGNITO_USER_POOL_ID` / `AWS_COGNITO_CLIENT_ID` | Admin API y validación de `client_id` |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | Solo para endpoints que usan la Admin API |
| `CORS_*` | Configuración CORS |
