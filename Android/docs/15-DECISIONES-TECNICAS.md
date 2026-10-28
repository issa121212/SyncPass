# 15. Decisiones Técnicas

## Decisiones por Componente

### Backend (fs2-api)

| Decisión | Alternativas Consideradas | Justificación | Estado |
|---|---|---|---|
| **Spring Boot 4.1.1 + Java 21** | Spring Boot 3.x | Versión utilizada en proyecto, compatibilidad con stack actual | Adoptado |
| **OAuth2 Resource Server + JWT (Cognito)** | Sesiones propias, JWT propio | Integración con AWS Cognito, estandar OIDC, delegación de auth | Adoptado |
| **JWKS validation** | Verificación con secret | Más seguro, rotación de claves automática | Adoptado |
| **Mapeo cognito:groups → roles** | Claims personalizados | Aprovecha grupos de Cognito, flexible | Adoptado |
| **Flyway para migraciones** | Liquibase, manual | Simple, versionado, integrado con Spring Boot | Adoptado |
| **PostgreSQL como BD principal** | MySQL, H2 | Robusto, adecuado para producción | Adoptado |
| **H2 para tests y perfil `local`** | Testcontainers | Rápido y permite levantar el MVP sin Docker/PostgreSQL | Adoptado |
| **DTOs separados de entidades** | Exponer entidades JPA | Evita lazy loading issues, controla contrato API | Adoptado |
| **Validación ownership en Service** | Solo en Security | Reglas de negocio complejas (proyecto-evento) requieren lógica | Adoptado |
| **Inscripción automática al escanear** | Requerir inscripción previa | Mejora UX para docente, valida cupos | Adoptado |

### Móvil (FormularioKotlin)

| Decisión | Alternativas Consideradas | Justificación | Estado |
|---|---|---|---|
| **Kotlin + Jetpack Compose** | XML Views | Moderno, declarativo, recomendado por Google | Adoptado |
| **Navigation Compose** | Fragment Navigation | Integrado con Compose | Adoptado |
| **Room para persistencia local** | DataStore, SQLite directo | ORM robusto, DAOs, migraciones, integrado | Adoptado |
| **Retrofit + OkHttp** | Ktor, Volley | Estándar, interceptors, maduro | Adoptado |
| **WorkManager para sync** | AlarmManager, JobScheduler | Garantiza ejecución con constraints, compatible | Adoptado |
| **AppAuth para Cognito** | Implementación manual OAuth | Seguro, maneja PKCE, redirect, tokens | Adoptado |
| **SharedPreferences para sesión** | DataStore, EncryptedSharedPreferences | Simple, funcional para MVP | Adoptado |
| **DI manual (AppContainer)** | Hilt, Koin, Dagger | Sin dependencias extra, explícito, control total | Adoptado |
| **CameraX + ML Kit + ZXing** | ZBar, ZXing solo | CameraX moderno, ML Kit eficiente, ZXing para generación | Adoptado |
| **MVVM** | MVI, MVP | Estándar Android, simple con Compose | Adoptado |

### Arquitectura Offline-First

| Decisión | Alternativas Consideradas | Justificación | Estado |
|---|---|---|---|
| **Cola de pendientes en Room** | SharedPreferences, archivo | Persistente, relacional, fácil consultar/actualizar | Adoptado |
| **clientUuid para idempotencia** | Timestamp, hash | Único, generado cliente, evita duplicados | Adoptado |
| **Clasificación 4xx vs 5xx/red** | Reintentar todo | Evita reintentos infinitos de errores definitivos | Adoptado |
| **Batch endpoints (`/api/sync/*`)** | Sincronización uno a uno | Eficiente para escaneos y encuestas; inscripciones/asistencias propias van una a una | Adoptado |
| **Write-first local** | Solo remoto | Permite uso offline completo | Adoptado |

### Seguridad

| Decisión | Alternativas Consideradas | Justificación | Estado |
|---|---|---|---|
| **Authorization Code + PKCE** | Implicit Flow | Más seguro, recomendado por OAuth 2.1 | Adoptado |
| **JWT validation server-side** | Solo confiar en cliente | Crítico, valida firma/claims/expiración | Adoptado |
| **Role-based + ownership** | Solo roles | Necesario para multi-docente (solo su proyecto) | Adoptado |
| **Bearer token** | Cookies | Compatible con REST API, app móvil | Adoptado |

## Trade-offs Aceptados

| Trade-off | Razón |
|---|---|
| **Sin refresh token automático** | Simplifica MVP, usuario re-autentica si expira |
| **DI manual vs Hilt** | Menor curva/setup, sin anotaciones complejas |
| **SharedPreferences vs Keystore** | Más simple, aceptable para MVP con tokens Cognito |
| **Sin tests automatizados móviles** | Prioridad en funcionalidad offline-first |
| **Sin OpenAPI** | Documentación manual suficiente por ahora |

## Lecciones y Consideraciones Futuras

- **Agregar refresh token**: Mejorar UX evitando re-login
- **EncryptedSharedPreferences**: Mejorar seguridad de tokens
- **Certificate pinning**: Protección contra MITM
- **OpenAPI/Swagger**: Auto-documentación API
- **Tests E2E**: Cobertura crítica (auth, QR, sync)
- **Room migrations**: Preparar para evolución esquema
- **Logging/Analytics**: Monitoreo en producción
- **Notificaciones push**: Alertas de eventos
- **Panel web**: Administración más completa
