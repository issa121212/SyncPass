# 13. Offline-First y Sincronización

## Filosofía Offline-First

La app móvil está diseñada para funcionar sin conectividad:

1. **Cache local primero**: el catálogo (proyectos, eventos, perfil) se guarda en Room.
2. **Escritura inmediata**: las mutaciones se guardan en una cola local y la UI
   se actualiza de forma optimista.
3. **Sincronización diferida**: la cola se envía cuando hay conectividad.
4. **Resiliencia**: los errores de red se reintentan; los errores de negocio
   quedan visibles para el usuario.

## Cola de Pendientes (`pendientes`)

### Estructura real (Room)

| Campo | Tipo | Descripción |
|---|---|---|
| `clientUuid` | String (PK) | UUID v4 generado al encolar; clave de idempotencia |
| `tipo` | String | `INSCRIPCION`, `ASISTENCIA_PROPIA`, `ASISTENCIA_ESCANEO`, `ENCUESTA` |
| `eventoId` | Long | Evento al que pertenece la operación |
| `codigoQr` | String? | QR usado (inscripción o escaneo) |
| `marcadoEn` | String | Instante ISO-8601 de la operación |
| `latitud` / `longitud` / `precisionMetros` | Double? | GPS capturado en terreno |
| `satisfaccion` / `utilidad` | Int? | Solo encuestas (1–5) |
| `comentarios` | String? | Solo encuestas |
| `creadoEn` | Long | Timestamp local de creación (ordena la cola) |
| `sincronizado` | Boolean | `true` cuando el backend confirmó la operación |
| `error` | String? | Último mensaje de error (se muestra/registra) |

No existe un enum de estado ni un contador de reintentos: la cola se
consulta con `WHERE sincronizado = 0 ORDER BY creadoEn` y las operaciones
fallidas simplemente permanecen pendientes. Los sincronizados se eliminan
al final de cada corrida con `limpiarSincronizados()`.

### Tipos de operación y endpoint real

| Tipo | Descripción | Endpoint |
|---|---|---|
| `INSCRIPCION` | Estudiante se inscribe a un evento | `POST /api/eventos/inscribirse` |
| `ASISTENCIA_PROPIA` | Estudiante marca su asistencia | `PUT /api/eventos/{id}/asistencias/me` |
| `ASISTENCIA_ESCANEO` | Docente escanea el QR del estudiante | `POST /api/sync/asistencias` (batch) |
| `ENCUESTA` | Estudiante responde la encuesta | `POST /api/sync/encuestas` (batch) |

> Matiz de implementación: la **inscripción** intenta primero la API (para
> mostrar de inmediato errores de negocio como "sin cupos") y solo encola si
> el fallo es reintentable. **Escaneos, asistencia propia y encuestas**
> siempre se guardan primero en `pendientes` y luego se intenta sincronizar.

## Orden de sincronización

`SyncPassRepository.sincronizar()` procesa la cola en este orden por
dependencias del backend:

1. **Inscripciones** — crean la `Inscripcion` que necesitan los pasos siguientes.
2. **Asistencias escaneadas** — el QR del docente inscribe al estudiante si
   hacía falta; no dependen del paso 1.
3. **Asistencias propias** — el backend exige estar inscrito.
4. **Encuestas** — el backend exige una asistencia marcada como presente.

Sin este orden, un estudiante que hizo todo offline (inscribirse → marcar
asistencia → encuesta) necesitaría varias corridas de sincronización para
que la encuesta fuera aceptada.

## Clasificación de Errores

Se aplica al decidir si una operación se encola (por ejemplo, al
inscribirse) y al interpretar fallos:

| Tipo de Error | Clasificación | Acción |
|---|---|---|
| **IOException / timeout / sin red** | Reintentable | Se encola (o permanece en cola) |
| **5xx (500, 502, 503, 504)** | Reintentable | Se encola / se reintenta en el próximo sync |
| **408 Request Timeout** | Reintentable | Se reintenta |
| **429 Too Many Requests** | Reintentable | Se reintenta |
| **400 Bad Request** | Definitivo | No se encola; se muestra el error al usuario |
| **401 Unauthorized** | Definitivo | No se encola; requiere volver a iniciar sesión |
| **403 Forbidden** | Definitivo | No se encola (sin permisos/ownership) |
| **404 Not Found** | Definitivo | No se encola (recurso inexistente) |
| **409 Conflict** | Definitivo | No se encola (duplicado/regla de negocio) |

> Una operación ya encolada se reintenta en cada sync. Si el backend
> responde con un error de negocio, el mensaje queda en `error` y la
> operación sigue pendiente (no se descarta automáticamente).

## Sincronización

### WorkManager

- `PeriodicWorkRequest` cada **15 minutos**, con constraint
  `NetworkType.CONNECTED`.
- `enqueueUniquePeriodicWork(..., KEEP)` para no duplicar el worker.
- Si no hay sesión válida, el worker termina con éxito sin hacer nada.
- El botón **Sincronizar** de Home llama directamente a
  `repository.sincronizar()` (no encola un `OneTimeWorkRequest`).
  `SyncScheduler.sincronizarAhora()` existe como alternativa.

### Proceso

1. Leer pendientes (`sincronizado = 0`, orden FIFO por `creadoEn`).
2. Procesar en el orden de dependencias descrito arriba.
3. Enviar cada operación con su `clientUuid` cuando el endpoint lo soporta.
4. Marcar `sincronizado = 1` o guardar `error`.
5. Eliminar las operaciones sincronizadas.

## Idempotencia

- **`clientUuid`**: generado al encolar (UUID v4).
- **Escaneos**: el backend busca por `clientUuid`; si ya existe, devuelve la
  asistencia existente sin duplicar.
- **Asistencia propia**: el backend es idempotente por
  `(eventoId, usuarioId)`; el `clientUuid` se envía pero no se usa como
  clave.
- **Inscripción**: el backend devuelve la inscripción existente si el
  estudiante ya está inscrito.
- **Encuesta**: el backend actualiza la encuesta de la asistencia
  (una por asistencia).

## Estrategias

| Estrategia | Descripción |
|---|---|
| **Write-Through Local** | Guardar en Room primero y confirmar en la UI |
| **Queue-Based Sync** | Las mutaciones se encolan si falla la red |
| **Batch Sync** | Escaneos y encuestas se envían en lote |
| **Optimistic UI** | `marcarInscrito`, `marcarPresente`, `marcarEncuesta` actualizan Room al instante |
| **Error Classification** | Evita encolar errores de negocio definitivos |

## Casos de Uso

### Inscripción Offline
1. Sin red, el estudiante pulsa **Inscribirme**.
2. Se crea un pendiente `INSCRIPCION` y se marca el evento como inscrito.
3. Al sincronizar, `POST /api/eventos/inscribirse` valida cupos y duplicados.
4. Si el backend confirma, el pendiente se elimina.

### Escaneo Docente Offline
1. El docente escanea el QR `ESTUDIANTE_<id>` sin conexión.
2. Se encola `ASISTENCIA_ESCANEO` con `eventoId`, `codigoQr` y GPS.
3. Al sincronizar, `POST /api/sync/asistencias` inscribe (si hace falta) y
   registra la asistencia usando `clientUuid` para idempotencia.

### Asistencia Propia Offline
1. El estudiante pulsa **Marcar mi asistencia**.
2. Se encola `ASISTENCIA_PROPIA` con GPS y se marca presente en Room.
3. Al sincronizar, `PUT /api/eventos/{id}/asistencias/me` actualiza la
   asistencia y el estado de la inscripción.

### Encuesta Offline
1. El estudiante responde la encuesta sin conexión.
2. Se encola `ENCUESTA` y se marca `encuestaRespondida` en Room.
3. Al sincronizar, el backend exige que la asistencia exista y esté presente.

## Monitoreo y UI

- **Pendientes**: contador en Home (`COUNT(*) WHERE sincronizado = 0`).
- **Errores**: el mensaje del último fallo queda en `pendientes.error`.
- **Sincronización manual**: botón **Sincronizar** en Home.
- **Worker periódico**: reintenta cada 15 minutos con red disponible.

## Limitaciones

- Sin refresh token automático: al expirar el access token hay que volver a
  iniciar sesión.
- Sin resolución de conflictos compleja (last-write-wins o validación servidor).
- Sin cola prioritaria avanzada.
- Sin sincronización en tiempo real (WebSocket).
- La encuesta depende de que la asistencia se haya sincronizado antes; el
  orden de la cola lo garantiza, pero un fallo de negocio persistente deja
  la operación pendiente de forma indefinida.
