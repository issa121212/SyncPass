# 14. Compilación, Pruebas y Limitaciones

## Backend (`fs2-api`)

### Requisitos
- JDK 21 (toolchain configurado en `build.gradle`)
- Gradle Wrapper (`gradlew` / `gradlew.bat`)
- **Para el perfil `local`:** nada más (H2 en memoria)
- **Para el perfil por defecto:** PostgreSQL 13+ con Flyway

### Perfiles de ejecución

| Perfil | Base de datos | Esquema | Uso |
|---|---|---|---|
| `local` | H2 en memoria | `ddl-auto: create-drop` | Probar el MVP sin Docker ni PostgreSQL. Los datos se pierden al detener la app. |
| por defecto (`dev`/`prod`) | PostgreSQL | `ddl-auto: validate` + Flyway | Desarrollo persistente y despliegue. |

El perfil se activa con `SPRING_PROFILES_ACTIVE` en `.env`
(por defecto `local` en `.env.example`) o con
`--spring.profiles.active=local`.

### Compilación y ejecución

```bash
# Compilar
./gradlew compileJava

# Ejecutar tests (usan H2)
./gradlew test

# Empaquetar
./gradlew bootJar

# Ejecutar en local (H2 en memoria, sin Docker)
./gradlew bootRun
```

Al arrancar, la aplicación carga `.env` de la raíz del proyecto
(`CursosApplication.loadDotenv()`), por lo que no hay que exportar
las variables manualmente.

Verificación rápida:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Respuesta esperada:

```json
{ "status": "UP", "service": "cursos-api" }
```

### Estado actual

- ✅ Compila correctamente con JDK 21.
- ✅ Tests pasan con H2.
- ✅ Perfil `local` con H2 permite levantar la API sin Docker/PostgreSQL.
- ⚠️ Migraciones Flyway no validadas contra un PostgreSQL real (Docker no disponible en el ambiente).
- ⚠️ Sin OpenAPI/Swagger.

## Móvil (`FormularioKotlin`)

### Requisitos
- Android Studio (recomendado) o JDK 17+
- Android SDK instalado y `local.properties` con `sdk.dir`
- Emulador o dispositivo con Android 8.0+ (según `minSdk`)

### Compilación

```bash
# Compilar Kotlin debug
./gradlew :app:compileDebugKotlin

# Generar APK debug
./gradlew :app:assembleDebug

# Limpiar
./gradlew clean
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

### Configuración

`app/build.gradle.kts` lee el archivo `.env` de la raíz del proyecto y
lo inyecta en `BuildConfig` (sin valores hardcodeados en Kotlin):

| Variable | Uso |
|---|---|
| `API_BASE_URL` | URL base del backend. Emulador: `http://10.0.2.2:8080/`. Dispositivo: `http://<IP-LAN>:8080/`. |
| `COGNITO_ISSUER` | Issuer del User Pool. |
| `COGNITO_CLIENT_ID` | App Client ID (sin secret). |
| `COGNITO_REDIRECT_URI` | Debe calzar con el `intent-filter` y con Cognito. |
| `COGNITO_LOGOUT_URI` | URL de cierre de sesión del Hosted UI. |
| `COGNITO_SCOPES` | Scopes solicitados (`email openid phone`). |

El tráfico HTTP plano para probar el backend local solo se permite en
builds **debug** (`app/src/debug/AndroidManifest.xml`). Los builds
release exigen HTTPS.

### Estado actual

- ✅ Compila con `:app:compileDebugKotlin`.
- ✅ APK debug generado.
- ✅ Room, Retrofit, WorkManager, CameraX/ML Kit/ZXing y AppAuth integrados.
- ⚠️ Pruebas end-to-end pendientes de un User Pool de Cognito real.
- ⚠️ Sin suite de tests automatizados en el módulo móvil.

## Pruebas

### Backend
- Tests unitarios/integración con H2 (`./gradlew test`).
- Perfil `local` para pruebas manuales sin infraestructura.
- Prueba de contrato JWT: los endpoints protegidos exigen
  `Authorization: Bearer <access_token>` y devuelven `401` sin token.

### Móvil
- Pruebas manuales guiadas por [16-GUIA-PRUEBA-MVP.md](16-GUIA-PRUEBA-MVP.md).
- Sin suite de tests automatizados visible.

## Despliegue

### Backend
- JAR ejecutable con Spring Boot (`./gradlew bootJar`).
- En producción: PostgreSQL + Flyway + variables de entorno reales.
- Cognito: issuer, JWKS y client ID deben corresponder al User Pool.

### Móvil
- APK debug/release.
- AppAuth con redirect URI registrado en Cognito.
- Permisos `CAMERA` y `ACCESS_FINE_LOCATION`.

## Limitaciones Conocidas

### Funcionales
| Limitación | Impacto | Estado |
|---|---|---|
| **Sin refresh token automático** | Al expirar el access token la sesión termina y requiere login | Conocido |
| **Sin panel web administrativo** | Gestión solo vía API/app | Fuera de alcance |
| **Sin notificaciones push** | Sin alertas de eventos/sincronización | Fuera de alcance |
| **Sin reportes PDF/Excel** | Solo visualización en app | Fuera de alcance |
| **Multi-tenant no soportado** | Una instancia por contexto | Fuera de alcance |

### Técnicas
| Limitación | Impacto | Estado |
|---|---|---|
| **PostgreSQL/Flyway sin validación completa** | No se validaron migraciones con Docker | Bloqueado (ambiente) |
| **Sin OpenAPI/Swagger** | Documentación API manual | Pendiente |
| **Sin tests automatizados móviles** | Mayor riesgo en regresiones | Pendiente |
| **DI manual** | Más boilerplate, sin Hilt | Decisión técnica |
| **SharedPreferences para sesión** | Menos seguro que Keystore, pero funcional | Decisión técnica |
| **Sin resolución de conflictos compleja** | Posibles inconsistencias en edge cases | Aceptado |

### Seguridad
| Limitación | Impacto | Estado |
|---|---|---|
| **Tokens en SharedPreferences** | Vulnerable si el dispositivo está comprometido | Aceptado para MVP |
| **Sin certificate pinning** | Vulnerable a MITM en redes no confiables | Pendiente |
| **Sin ofuscación avanzada** | APK release sin R8 revisado en detalle | Pendiente |

## Checklist de Verificación

### Backend
- [x] Compila con Gradle
- [x] Tests pasan con H2
- [x] Estructura por capas
- [x] Seguridad JWT configurada
- [x] Migraciones Flyway presentes
- [x] Perfil `local` con H2 operativo (sin Docker)
- [ ] Validación PostgreSQL completa
- [ ] OpenAPI/Swagger

### Móvil
- [x] Compila con Gradle
- [x] APK generado
- [x] Arquitectura MVVM
- [x] Room + Retrofit
- [x] WorkManager
- [x] AppAuth + PKCE
- [x] Offline-first implementado
- [ ] Pruebas en dispositivo
- [ ] Configuración de producción

### Integración
- [ ] Autenticación Cognito verificada end-to-end
- [ ] QR de estudiante y escaneo por docente verificados
- [ ] Sincronización offline verificada contra la API
- [ ] Roles y permisos verificados con usuarios reales

> Para el paso a paso completo de la prueba end-to-end ver
> [16-GUIA-PRUEBA-MVP.md](16-GUIA-PRUEBA-MVP.md).
