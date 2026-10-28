# 12. Contrato API

## Información General

| Aspecto | Valor |
|---|---|
| **Base URL** | Configurable (`API_BASE_URL`, ej. `http://10.0.2.2:8080/`) |
| **Prefijo** | Todos los endpoints cuelgan de `/api` |
| **Autenticación** | `Authorization: Bearer <access_token>` (JWT de Cognito) |
| **Formato** | JSON (`application/json`) |
| **Fechas** | ISO-8601 (`LocalDate`: `2026-10-01`; `LocalDateTime`: `2026-10-25T10:00:00`; `Instant`: `2026-10-25T13:00:00Z`) |

El backend valida `iss`, `exp`, `token_use = access` y `client_id` del token
contra el User Pool configurado. La app envía siempre el **access token**.

## Salud

| Método | Endpoint | Roles | Descripción |
|---|---|---|---|
| `GET` | `/api/health` | Público | Health check (`status: UP`) |

## Auth y perfil

| Método | Endpoint | Roles | Descripción |
|---|---|---|---|
| `POST` | `/api/auth/registro` | Público | Registro (siempre crea `ESTUDIANTE`) |
| `POST` | `/api/auth/login` | Público | Login directo contra Cognito (Admin API) |
| `GET` | `/api/auth/me` | Autenticado | Perfil del usuario; auto-provisiona en el primer acceso |
| `POST` | `/api/auth/logout` | Autenticado | Cierra la sesión en Cognito |
| `POST` | `/api/auth/olvide-contrasena/solicitar` | Público | Envía código de recuperación |
| `POST` | `/api/auth/olvide-contrasena/confirmar` | Público | Confirma nueva contraseña |
| `GET` | `/api/usuarios/me` | Autenticado | Alias del perfil actual |
| `PUT` | `/api/usuarios/me` | Autenticado | Actualizar nombre |
| `GET` | `/api/usuarios/me/qr` | `ESTUDIANTE` | QR personal (`ESTUDIANTE_{id}`) |

**Respuesta de perfil:**
```json
{
  "id": 1,
  "cognitoSub": "9f8e7d6c-...",
  "email": "estudiante@test.cl",
  "nombre": "Estudiante",
  "roles": ["ESTUDIANTE"]
}
```

## Proyectos

| Método | Endpoint | Roles | Descripción |
|---|---|---|---|
| `GET` | `/api/proyectos` | Autenticado | Listar proyectos |
| `GET` | `/api/proyectos/{id}` | Autenticado | Obtener proyecto |
| `POST` | `/api/proyectos` | `DOCENTE`/`ADMIN` | Crear proyecto |
| `PUT` | `/api/proyectos/{id}` | `DOCENTE`/`ADMIN` (owner) | Actualizar |
| `DELETE` | `/api/proyectos/{id}` | `DOCENTE`/`ADMIN` (owner) | Eliminar |

**Request:**
```json
{
  "nombre": "Huertas Comunitarias",
  "descripcion": "Proyecto de huertas",
  "areaTematica": "Medio Ambiente",
  "unidadVecinal": "Villa Esperanza",
  "fechaInicio": "2026-10-01",
  "fechaFin": "2026-12-31",
  "estado": "PLANIFICADO"
}
```

`estado` ∈ `PLANIFICADO | EN_CURSO | FINALIZADO | CANCELADO`.

**Response:** incluye `id`, `responsable { id, nombre, email }`,
`creadoEn`, `actualizadoEn`.

## Eventos

| Método | Endpoint | Roles | Descripción |
|---|---|---|---|
| `GET` | `/api/eventos?proyectoId={id}` | Autenticado | Listar (filtro opcional) |
| `GET` | `/api/eventos/{id}` | Autenticado | Obtener evento |
| `POST` | `/api/eventos` | `DOCENTE`/`ADMIN` (owner) | Crear |
| `PUT` | `/api/eventos/{id}` | `DOCENTE`/`ADMIN` (owner) | Actualizar |
| `DELETE` | `/api/eventos/{id}` | `DOCENTE`/`ADMIN` (owner) | Eliminar |
| `GET` | `/api/eventos/{id}/qr` | `DOCENTE`/`ADMIN` (owner) | QR del evento |
| `POST` | `/api/eventos/inscribirse` | `ESTUDIANTE` | Inscribirse con QR |
| `GET` | `/api/eventos/{id}/asistentes` | `DOCENTE`/`ADMIN` (owner) | Lista de asistentes |
| `POST` | `/api/eventos/{id}/asistencias/escanear` | `DOCENTE`/`ADMIN` (owner) | Escanear QR de estudiante |
| `GET` | `/api/eventos/{id}/asistencias/me` | `ESTUDIANTE` | Mi asistencia |
| `PUT` | `/api/eventos/{id}/asistencias/me` | `ESTUDIANTE` | Marcar/actualizar mi asistencia |
| `PUT` | `/api/eventos/{id}/encuestas/me` | `ESTUDIANTE` | Responder encuesta |
| `GET` | `/api/eventos/{id}/encuestas` | `DOCENTE`/`ADMIN` (owner) | Encuestas del evento |

**Request de evento:**
```json
{
  "proyectoId": 1,
  "nombre": "Taller de Compostaje",
  "descripcion": "Taller práctico",
  "fechaHora": "2026-10-25T10:00:00",
  "cupos": 30,
  "recinto": "Centro Comunitario",
  "direccion": "Calle 123",
  "latitud": -33.4489,
  "longitud": -70.6693,
  "radioMetros": 200,
  "estado": "PROGRAMADO"
}
```

`estado` ∈ `PROGRAMADO | EN_CURSO | FINALIZADO | CANCELADO`.
`cupos = 0` significa cupos abiertos.

**Response de evento:** incluye además `proyectoNombre`, `inscritos`,
`presentes`, `qrContenido`, `inscrito`, `esPresente`,
`encuestaRespondida`.

## Inscripción

`POST /api/eventos/inscribirse`
```json
{ "codigoQr": "EVENTO_1" }
```

- Es idempotente: si el estudiante ya está inscrito, devuelve el evento.
- Valida cupos (si `cupos > 0`) y agrega al estudiante a la lista.
- Crea una `Asistencia` con `esPresente = false`.

## Asistencias

**Escanear (docente):**
`POST /api/eventos/{id}/asistencias/escanear`
```json
{
  "codigoQr": "ESTUDIANTE_7",
  "esPresente": true,
  "marcadoEn": "2026-10-25T13:00:00Z",
  "latitud": -33.4489,
  "longitud": -70.6693,
  "precisionMetros": 12.5,
  "clientUuid": "uuid-v4"
}
```

- Inscribe al estudiante si no lo estaba.
- Idempotente por `clientUuid`.
- Requiere que el docente sea responsable del proyecto o `ADMIN`.

**Asistencia propia (estudiante):**
`PUT /api/eventos/{id}/asistencias/me`
```json
{
  "esPresente": true,
  "marcadoEn": "2026-10-25T13:00:00Z",
  "latitud": -33.4489,
  "longitud": -70.6693,
  "precisionMetros": 12.5,
  "clientUuid": "uuid-v4"
}
```

- Requiere estar inscrito (`403` si no).
- Idempotente por `(eventoId, usuarioId)`.

## Encuestas

`PUT /api/eventos/{id}/encuestas/me`
```json
{
  "satisfaccion": 5,
  "utilidad": 4,
  "comentarios": "Excelente taller",
  "enviadoEn": "2026-10-25T13:05:00Z"
}
```

- `satisfaccion` y `utilidad` ∈ 1..5.
- Requiere una asistencia existente y `esPresente = true`
  (`404` si no hay asistencia, `422` si no está presente).
- Una encuesta por asistencia (se actualiza si se repite).

## Sincronización offline

Los endpoints de sync reciben **un array JSON directo** (no un objeto con
`items`):

| Método | Endpoint | Roles | Body |
|---|---|---|---|
| `POST` | `/api/sync/asistencias` | `DOCENTE`/`ADMIN` | `AsistenciaSyncItem[]` |
| `POST` | `/api/sync/encuestas` | `ESTUDIANTE` | `EncuestaSyncItem[]` |

**AsistenciaSyncItem:**
```json
[
  {
    "clientUuid": "uuid-v4",
    "eventoId": 1,
    "codigoQr": "ESTUDIANTE_7",
    "marcadoEn": "2026-10-25T13:00:00Z",
    "latitud": -33.4489,
    "longitud": -70.6693,
    "precisionMetros": 12.5
  }
]
```

**EncuestaSyncItem:**
```json
[
  {
    "eventoId": 1,
    "satisfaccion": 5,
    "utilidad": 4,
    "comentarios": "Excelente taller",
    "enviadoEn": "2026-10-25T13:05:00Z"
  }
]
```

**Respuesta de sync (`SyncResult`):**
```json
{
  "procesados": 1,
  "errores": 0,
  "mensajes": []
}
```

Cada item se procesa de forma independiente; si uno falla, se informa en
`mensajes` y `errores` sin abortar el resto.

> Las inscripciones y las asistencias propias no tienen endpoint batch:
> la app las sincroniza de a una (`POST /api/eventos/inscribirse`,
> `PUT /api/eventos/{id}/asistencias/me`).

## Respuestas de error

Formato:
```json
{
  "error": "VALIDATION_ERROR",
  "mensaje": "Datos de entrada inválidos",
  "status": 400,
  "detalles": { "nombre": "no debe estar vacío" }
}
```

| Código | Cuándo |
|---|---|
| `400` | Validación de DTO, QR con formato inválido, contraseña inválida |
| `401` | Token ausente/inválido/expirado |
| `403` | Sin rol, sin ownership o sin inscripción |
| `404` | Recurso inexistente |
| `409` | Conflicto/dato duplicado |
| `422` | Regla de negocio: sin cupos, QR no estudiantil, encuesta sin presencia |
| `429` | Límite de solicitudes de Cognito |
| `500` | Error interno |

## Formatos de QR

| Tipo | Formato | Uso |
|---|---|---|
| **Evento** | `EVENTO_{eventoId}` | El estudiante se inscribe |
| **Estudiante** | `ESTUDIANTE_{usuarioId}` | El docente marca asistencia escaneándolo |

El backend valida el prefijo y que el ID sea numérico.

## Notas

- El ownership se valida en la capa de servicio: un docente solo opera
  sobre sus propios proyectos/eventos (salvo `ADMIN`).
- `clientUuid` es la clave de idempotencia de los escaneos.
- El cliente móvil no implementa refresh token: al expirar el access token
  hay que volver a iniciar sesión.
