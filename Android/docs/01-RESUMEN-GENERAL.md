# 01. Resumen General

## Objetivo

**SyncPass** es una solución integral para la acreditación de asistencia en jornadas comunitarias mediante códigos QR. Consiste en un backend (`fs2-api`) y una aplicación móvil Android (`FormularioKotlin`) que permite a docentes y estudiantes gestionar proyectos, eventos, inscripciones, asistencias y encuestas de satisfacción, con soporte **offline-first**.

## Alcance

### Incluido
- Autenticación con **AWS Cognito** (Hosted UI + Authorization Code + PKCE)
- Gestión de proyectos comunitarios y eventos
- Inscripción de estudiantes mediante QR de evento (`EVENTO_{id}`)
- Acreditación de asistencia por escaneo QR de estudiante (`ESTUDIANTE_{id}`) o marcado propio con geolocalización
- Encuestas de satisfacción/utilidad (1-5) post-asistencia
- Sincronización offline con cola de pendientes y WorkManager
- Validación de roles y ownership por docente responsable
- Persistencia local con Room y remota con PostgreSQL

### Fuera del alcance
- Panel web administrativo
- Notificaciones push
- Múltiples instituciones (multi-tenant)
- Firma digital de QR
- Reportes PDF/Excel avanzados
- Refresh token automático en cliente móvil

## Estado Actual

| Componente | Estado | Observaciones |
|---|---|---|
| **Backend `fs2-api`** | Implementado | Compila y pasa tests con H2. El perfil `local` levanta la API con H2 en memoria; PostgreSQL + Flyway queda para despliegue. |
| **App Móvil `FormularioKotlin`** | Implementado | Compila y genera APK. UI completa, lógica offline-first implementada. Pendiente pruebas en dispositivo/emulador. |
| **Autenticación Cognito** | Implementado | Hosted UI funcional con AppAuth. Validación JWT en backend. |
| **Sincronización Offline** | Implementado | Cola con Room, WorkManager cada 15 min, distinción 4xx vs red/5xx. |

## Tecnologías

### Backend
| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje base |
| Spring Boot | 4.1.1 | Framework principal |
| Spring Security + OAuth2 Resource Server | - | Autenticación y autorización JWT |
| Spring Data JPA | - | Persistencia |
| PostgreSQL | 16 | Base de datos |
| Flyway | - | Migraciones |
| AWS SDK Cognito | 2.29.30 | Integración Cognito |
| Lombok | - | Reducción boilerplate |

### Móvil
| Tecnología | Versión | Uso |
|---|---|---|
| Kotlin | 2.2.10 | Lenguaje base |
| Jetpack Compose | - | UI declarativa |
| Material 3 | - | Design system |
| Navigation Compose | - | Navegación |
| Room | 2.7.1 | Base de datos local |
| Retrofit | - | Cliente HTTP |
| OkHttp | - | HTTP client + interceptors |
| WorkManager | 2.10.1 | Sincronización en background |
| CameraX | 1.4.1 | Escaneo QR |
| ML Kit Barcode Scanning | 17.3.0 | Detección QR |
| ZXing | 3.5.4 | Generación QR |
| AppAuth | - | Cognito Hosted UI |

## Arquitectura General

```
┌─────────────────┐     ┌──────────────┐     ┌─────────────┐
│ FormularioKotlin│────▶│   Cognito    │     │   fs2-api    │
│  (Android App)  │     │ Hosted UI    │     │ (Spring Boot)│
└─────────────────┘     └──────────────┘     └─────────────┘
         │                                           │
         │                                           │
    ┌────▼────┐                                 ┌────▼────┐
    │  Room   │                                 │PostgreSQL│
    │ (Cache) │                                 │   16    │
    └─────────┘                                 └─────────┘
```

La app móvil se autentica vía Cognito Hosted UI, obtiene tokens JWT y los envía al backend `fs2-api` en todas las peticiones protegidas. El backend valida los tokens, aplica reglas de negocio y persiste en PostgreSQL. La app mantiene cache en Room y cola offline para operaciones sin conectividad.
