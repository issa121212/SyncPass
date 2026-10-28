# 07. Arquitectura General

## Visión Unificada

SyncPass está conformado por dos componentes que trabajan de forma integrada con un enfoque **offline-first**:

| Componente | Rol | Tecnología |
|---|---|---|
| **fs2-api** | Backend REST, lógica de negocio, autorización, validación y persistencia. | Spring Boot 4.1.1 (Java 21) |
| **FormularioKotlin** | Cliente móvil Android, UI, captura QR/GPS, cache local y cola offline. | Kotlin 2.2.10 + Jetpack Compose |

## Diagrama de Arquitectura Unificada

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                CLIENTE MÓVIL                                │
│                            FormularioKotlin (Android)                        │
├─────────────────────────────────────────────────────────────────────────────┤
│ UI (Compose) │ ViewModel │ Repository │ Local (Room) │ Remote (Retrofit)     │
│              │           │            │ pendientes   │ OkHttp interceptor    │
└──────────────┴───────────┴────────────┴──────────────┴─────────────────────┘
                │                │                        │
                │                │                        │ HTTP (REST/JSON)
                │                │                        │ Authorization: Bearer JWT
                ▼                ▼                        ▼
            ┌─────────┐      ┌─────────────┐        ┌─────────────────────────┐
            │ WorkMgr │      │SessionStore  │        │      AWS Cognito        │
            │(Sync)   │      │(SharedPrefs) │        │   Hosted UI + PKCE      │
            └─────────┘      └─────────────┘        └─────────────────────────┘
                                                              │ (Tokens)
                                                              ▼
                                                      ┌─────────────────────────┐
                                                      │        fs2-api          │
                                                      │      Spring Boot        │
                                                      ├─────────────────────────┤
                                                      │ Security(JWT/JWKS)      │
                                                      │ Controllers/Services    │
                                                      │ Repositories (JPA)      │
                                                      └─────────────┬───────────┘
                                                                    │
                                                                    ▼
                                                              ┌─────────────┐
                                                              │ PostgreSQL  │
                                                              │ (o H2 local)│
                                                              └─────────────┘
```

## Principios Arquitectónicos

| Principio | Aplicación |
|---|---|
| **Separation of Concerns** | Capas bien definidas en backend y móvil. |
| **Offline-First** | La app prioriza persistencia local (Room) y encola operaciones para sincronizar. |
| **API-First** | Comunicación REST con contrato definido en [12-CONTRATO-API.md](12-CONTRATO-API.md). |
| **Role-Based Access Control** | Autorización por roles Cognito mapeados a `ROLE_*`. |
| **Idempotencia** | Uso de `clientUuid` en operaciones offline para evitar duplicados. |
| **Fail-Safe Sync** | Distinción entre errores reintentables y definitivos. |
| **Least Privilege** | Validación de ownership en backend para recursos sensibles. |

## Comunicación

| Aspecto | Detalle |
|---|---|
| **Protocolo** | HTTP/HTTPS |
| **Formato** | JSON |
| **Autenticación** | JWT Bearer Token (obtenido desde Cognito) |
| **Sincronización** | Batch para escaneos/encuestas (`/api/sync/*`); el resto una a una |
| **Idempotencia** | `clientUuid` (escaneos) y constraints únicos |
| **Errores** | Respuestas HTTP con mensajes descriptivos |

## Flujo de Datos

1. **Autenticación**: App → Cognito Hosted UI → Tokens → SessionStore
2. **Lectura**: App (Remote) → API → DB → Respuesta cacheada/mostrada
3. **Escritura Online**: App → API (validado) → DB → Respuesta
4. **Escritura Offline**: App → Room (`pendientes`) → UI confirmación local
5. **Sincronización**: WorkManager → Lee `pendientes` → Llama API → Marca sincronizado
