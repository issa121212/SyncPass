# 05. Flujos Principales

## FLUJO-01: Autenticación (Cognito Hosted UI + PKCE)

**Actores:** Usuario, App Móvil, Cognito Hosted UI, fs2-api

**Precondiciones:** App instalada, configuración Cognito válida

**Pasos:**
1. Usuario abre app → verifica sesión en `SessionStore`
2. Sin sesión → navega a Login → abre Cognito Hosted UI con Authorization Code + PKCE
3. Usuario se autentica en Cognito
4. Cognito redirige con `code` a `redirect_uri` de app
5. App intercambia `code` por tokens (id_token, access_token, refresh_token según configuración)
6. App guarda los tokens en `SessionStore` (SharedPreferences)
7. App llama `GET /api/auth/me` con el access token para obtener perfil y roles
8. Backend valida JWT (firma, issuer, `token_use`, `client_id`) en cada petición

**Postcondiciones:** Sesión establecida, navegación según rol

## FLUJO-02: Crear Proyecto y Evento (Docente)

**Actores:** Docente, App, fs2-api, PostgreSQL

**Pasos:**
1. Docente selecciona "Nuevo proyecto"
2. Completa formulario (nombre, descripción, área, unidad vecinal, fechas)
3. App envía `POST /api/proyectos` con token
4. Backend valida rol DOCENTE/ADMIN y crea el proyecto con
   `responsable = usuario autenticado`
5. Guarda en BD
6. Docente crea evento desde el proyecto: `POST /api/eventos` con `proyectoId`
7. Backend valida ownership del proyecto
8. Crea evento con cupos, ubicación y geolocalización
9. El evento queda con `qrContenido = EVENTO_{id}`

**Postcondiciones:** Proyecto y evento creados, QR disponible

## FLUJO-03: Inscripción a Evento (Estudiante)

**Actores:** Estudiante, App, fs2-api

**Modo Online:**
1. Estudiante abre el detalle del evento y pulsa "Inscribirme en el evento"
2. App envía `POST /api/eventos/inscribirse` con `{ "codigoQr": "EVENTO_{id}" }`
3. Backend valida rol ESTUDIANTE, cupos y duplicado
4. Crea la inscripción (`INSCRITO`) y una asistencia con `esPresente = false`
5. App marca el evento como inscrito en Room

**Modo Offline:**
1. Si falla la red (IOException/5xx/408/429), la app guarda un pendiente
   tipo `INSCRIPCION` con `clientUuid` y `codigoQr`
2. Muestra "Guardado local, se sincronizará"
3. El `SyncWorker` (cada 15 min) o el botón **Sincronizar** envían
   `POST /api/eventos/inscribirse` al recuperar conexión

**Postcondiciones:** Inscripción creada o encolada

## FLUJO-04: Marcar Asistencia Propia (Estudiante)

**Actores:** Estudiante, App, GPS, fs2-api

**Online:**
1. Estudiante va a detalle de evento inscrito
2. Selecciona "Marcar mi asistencia"
3. App solicita permiso de ubicación (si es necesario)
4. Obtiene la última ubicación conocida (GPS/red)
5. Envía `PUT /api/eventos/{id}/asistencias/me` con `lat`, `lng`, `precisionMetros`
6. Backend valida la inscripción y marca la asistencia con `presente = true`
7. Actualiza la UI y Room

**Offline:**
1. La app guarda primero en la cola `pendientes` tipo `ASISTENCIA_PROPIA`
   y marca el evento como presente en Room
2. Sincroniza con `PUT /api/eventos/{id}/asistencias/me` incluyendo `clientUuid`

**Postcondiciones:** Asistencia marcada

## FLUJO-05: Escanear QR Estudiante y Marcar Asistencia (Docente)

**Actores:** Docente, App, Cámara, fs2-api

**Online:**
1. Docente abre "Escanear QR de estudiante"
2. Escanea el QR con formato `ESTUDIANTE_{id}`
3. App obtiene la última ubicación (si hay permiso)
4. Envía `POST /api/eventos/{id}/asistencias/escanear` con `codigoQr`,
   `lat`, `lng` y `clientUuid`
5. Backend:
   - Valida ownership del evento (docente responsable o ADMIN)
   - Verifica si existe la inscripción estudiante-evento
   - Si NO existe: verifica cupos y crea inscripción + asistencia
   - Si existe: marca la asistencia como presente
6. Responde con la asistencia y el docente puede seguir escaneando

**Offline:**
1. La app encola en `pendientes` tipo `ASISTENCIA_ESCANEO` con
   `eventoId`, `codigoQr` y `clientUuid`
2. Sincroniza con `POST /api/sync/asistencias` (array de items)

**Postcondiciones:** Asistencia registrada (inscribiendo si aplica)

## FLUJO-06: Responder Encuesta (Estudiante)

**Actores:** Estudiante, App, fs2-api

**Requisitos:** Estudiante debe estar marcado como **presente**

**Pasos:**
1. Estudiante ve evento con asistencia presente
2. Selecciona "Responder encuesta"
3. Completa satisfacción (1-5), utilidad (1-5) y comentario opcional
4. App envía `PUT /api/eventos/{id}/encuestas/me` o la encola offline
5. Backend valida que exista una asistencia con `esPresente = true`
6. Crea o actualiza la encuesta (una por asistencia)
7. Actualiza `encuestaRespondida` en Room

**Postcondiciones:** Encuesta guardada, evita duplicados

## FLUJO-07: Sincronización Offline

**Actores:** App, WorkManager, fs2-api

**Trigger:** WorkManager periódico (15 min) con `NetworkType.CONNECTED`, o sincronización manual

**Pasos:**
1. `SyncWorker` lee las operaciones con `sincronizado = 0` de la tabla
   `pendientes`, ordenadas por `creadoEn`
2. Procesa en orden: inscripciones → escaneos → asistencias propias → encuestas
3. Para cada operación:
   - Envía la petición HTTP con el access token
   - Si éxito (2xx): marca `sincronizado = 1`
   - Si falla (red o negocio): guarda el mensaje en `error` y la deja pendiente
4. Al final elimina las filas sincronizadas y actualiza el contador de Home
5. El worker periódico reintenta cada 15 minutos; el botón **Sincronizar**
   dispara el proceso de inmediato

**Postcondiciones:** Pendientes sincronizados o marcados con estado
