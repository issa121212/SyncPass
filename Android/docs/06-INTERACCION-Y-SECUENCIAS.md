# 06. Interacción y Secuencias

Los diagramas reflejan la implementación real (backend `fs2-api` + app
`FormularioKotlin`). Todos los endpoints cuelgan de `/api`.

## SEQ-01: Autenticación Cognito Hosted UI + PKCE

```mermaid
sequenceDiagram
    participant U as Usuario
    participant App as App Móvil
    participant Auth as AppAuth
    participant Cog as Cognito Hosted UI
    participant Store as SessionStore (SharedPreferences)
    participant API as fs2-api

    U->>App: Abrir app
    App->>Store: hasValidSession()
    Store-->>App: Sin sesión / access token expirado
    App->>Auth: Authorization Code + PKCE
    Auth->>Cog: GET /oauth2/authorize (client_id, redirect_uri, code_challenge, scope)
    Cog->>U: Formulario de login
    U->>Cog: Credenciales
    Cog->>Auth: Redirect al callback con code
    Auth->>Cog: POST /oauth2/token (code, code_verifier, client_id)
    Cog-->>Auth: access_token, id_token, refresh_token
    Auth->>Store: Guardar tokens y expiración
    App->>API: GET /api/auth/me (Bearer access_token)
    API->>API: Validar firma (JWKS), issuer, token_use y client_id
    API->>API: Auto-provisionar usuario local si es la primera vez
    API-->>App: Perfil + roles normalizados
    App-->>U: Home según rol (DOCENTE / ESTUDIANTE)
```

Notas:

- El backend valida el **access token**; la app no usa el id token para las
  llamadas.
- No hay refresh token automático: si el access token expira, la app vuelve
  al login.
- Los roles provienen de los grupos de Cognito normalizados por el backend
  (`CognitoRoles`).

## SEQ-02: Inscripción a Evento (Online)

```mermaid
sequenceDiagram
    participant U as Estudiante
    participant App as App Móvil
    participant API as fs2-api
    participant DB as Base de datos

    U->>App: Abrir evento -> "Inscribirme en el evento"
    App->>API: POST /api/eventos/inscribirse {codigoQr: "EVENTO_{id}"}
    API->>API: Validar rol ESTUDIANTE y formato del QR
    API->>DB: Buscar inscripción existente (evento, usuario)
    alt Ya inscrito
        API-->>App: 200 Evento (idempotente)
    else No inscrito y hay cupos
        API->>DB: INSERT inscripción (INSCRITO)
        API->>DB: INSERT asistencia (esPresente = false)
        API-->>App: 200 Evento
    else Sin cupos
        API-->>App: 422 "El evento no tiene cupos disponibles"
    end
    App->>App: Room: marcar inscrito
    App-->>U: Confirmación
```

## SEQ-03: Marcar Asistencia Propia (Online)

```mermaid
sequenceDiagram
    participant U as Estudiante
    participant App as App Móvil
    participant GPS as Location Service
    participant API as fs2-api
    participant DB as Base de datos

    U->>App: Evento inscrito -> "Marcar mi asistencia"
    App->>GPS: Solicitar permiso y última ubicación
    GPS-->>App: lat, lng, precisión (o null)
    App->>API: PUT /api/eventos/{id}/asistencias/me
    API->>DB: Verificar inscripción (evento, usuario)
    alt Inscrito
        API->>DB: UPSERT asistencia (esPresente = true)
        API->>DB: Actualizar inscripción (ASISTIO)
        API-->>App: 200 Asistencia
        App->>App: Room: marcar presente
        App-->>U: Asistencia registrada
    else No inscrito
        API-->>App: 403 "No estás inscrito en este evento"
        App-->>U: Debes inscribirte primero
    end
```

La operación es idempotente por `(eventoId, usuarioId)`.

## SEQ-04: Escaneo QR de Estudiante por Docente (Online)

```mermaid
sequenceDiagram
    participant D as Docente
    participant App as App Móvil
    participant Cam as CameraX + ML Kit
    participant API as fs2-api
    participant DB as Base de datos

    D->>App: Evento -> "Escanear QR de estudiante"
    App->>Cam: Inicializar cámara (permiso CAMERA)
    D->>Cam: Escanear QR ESTUDIANTE_{id}
    Cam-->>App: contenido del QR
    App->>API: POST /api/eventos/{id}/asistencias/escanear
    API->>API: Validar ownership del evento (responsable o ADMIN)
    API->>DB: Buscar asistencia por clientUuid
    alt Ya sincronizada
        API-->>App: 200 Asistencia existente (idempotente)
    else Nueva
        API->>API: Parsear ESTUDIANTE_{id} y validar rol del usuario
        API->>DB: Buscar inscripción (evento, estudiante)
        alt No inscrito
            API->>DB: Verificar cupos
            API->>DB: INSERT inscripción + asistencia presente
        else Inscrito
            API->>DB: UPDATE asistencia (presente, GPS, origen QR)
        end
        API-->>App: 200 Asistencia
    end
    App->>App: Room: marcar presente / encolar si no hay red
    App-->>D: Resultado (puede seguir escaneando)
```

## SEQ-05: Sincronización Offline

```mermaid
sequenceDiagram
    participant App as App Móvil
    participant Room as Room (pendientes)
    participant WM as WorkManager
    participant API as fs2-api

    Note over App: Sin conexión: las mutaciones se encolan y la UI se actualiza
    App->>Room: INSERT pendiente (clientUuid, tipo, datos)
    Note over WM: Cada 15 min con red, o botón "Sincronizar"
    WM->>App: SyncWorker.doWork()
    App->>Room: SELECT * FROM pendientes WHERE sincronizado = 0 ORDER BY creadoEn
    Room-->>App: Lista FIFO

    App->>API: 1) POST /api/eventos/inscribirse (una por una)
    App->>API: 2) POST /api/sync/asistencias (batch de escaneos)
    App->>API: 3) PUT /api/eventos/{id}/asistencias/me (una por una)
    App->>API: 4) POST /api/sync/encuestas (batch)

    alt Operación OK
        API-->>App: 2xx
        App->>Room: sincronizado = 1
    else Error de red / 5xx / 408 / 429
        API-->>App: Error
        App->>Room: se mantiene pendiente + mensaje en error
    else Error de negocio (4xx)
        API-->>App: Error
        App->>Room: se mantiene pendiente + mensaje visible
    end

    App->>Room: DELETE FROM pendientes WHERE sincronizado = 1
    App-->>App: Actualizar contador de pendientes
```

El orden (inscripción → escaneos → asistencia propia → encuesta) respeta
las dependencias del backend. Ver
[13-OFFLINE-FIRST-Y-SINCRONIZACION.md](13-OFFLINE-FIRST-Y-SINCRONIZACION.md).

## SEQ-06: Responder Encuesta (Estudiante)

```mermaid
sequenceDiagram
    participant U as Estudiante
    participant App as App Móvil
    participant API as fs2-api
    participant DB as Base de datos

    U->>App: Evento con asistencia presente -> Responder encuesta
    App->>API: PUT /api/eventos/{id}/encuestas/me {satisfaccion, utilidad, comentarios}
    API->>DB: Buscar asistencia (evento, usuario)
    alt Sin asistencia
        API-->>App: 404
        App-->>U: Debes marcar asistencia primero
    else No presente
        API-->>App: 422 "No puedes encuestar sin asistencia"
        App-->>U: Debes marcar asistencia primero
    else Presente
        API->>DB: INSERT o UPDATE encuesta (una por asistencia)
        API-->>App: 200 Encuesta
        App->>App: Room: marcar encuesta respondida
        App-->>U: Encuesta enviada
    end
```

Si no hay red, la encuesta se encola como `ENCUESTA` y se sincroniza
después de que la asistencia haya sido confirmada por el backend.
