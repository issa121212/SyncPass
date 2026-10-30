# API y seguridad

Fuente del contrato: `Backend/src/main/java/com/duoc/cursos/controller/`, `dto/ApiDtos.java` y servicios. Base pública configurada: `https://yw4llkw0lb.execute-api.us-east-1.amazonaws.com/`.

## Autenticación

Android utiliza Hosted UI con Authorization Code + PKCE mediante AppAuth. El issuer tiene formato `https://cognito-idp.<region>.amazonaws.com/<poolId>`. El dominio de login es distinto del issuer. Callback y cierre de sesión deben coincidir con Cognito y AndroidManifest.

El access token se envía como `Authorization: Bearer <access_token>`. El ID token se utiliza para identidad visible; no sustituye al access token ante la API. `SecurityConfig` valida firma/JWKS, issuer y expiración, y `CognitoAccessTokenValidator` verifica el uso de access token y el client_id permitido. La decodificación local en Android no reemplaza esa comprobación criptográfica del servidor.

La API usa política stateless y permisos de método, más comprobaciones de propiedad en servicios. API Gateway no tiene un autorizador configurado en el despliegue descrito; la protección de rutas de negocio está en Spring.

`SessionStore` conserva access/ID token cifrados con AES-GCM; la clave está en Android Keystore. No conserva refresh token y una sesión expirada requiere autenticación nueva. Room está separada por cuenta, pero no está cifrada por ese mecanismo.

## Endpoints de negocio

Todas las rutas siguientes requieren autenticación, salvo health. Los roles y propiedad se detallan en [roles y uso](03-ROLES-Y-USO.md).

| Método y ruta | Resultado | Permiso principal |
|---|---|---|
| GET `/api/health` | Estado simple UP, timestamp y nombre histórico cursos-api | Público |
| GET `/api/usuarios/me` | Perfil actual | Autenticado |
| PUT `/api/usuarios/me` | Actualizar nombre de perfil | Autenticado |
| GET `/api/usuarios/me/qr` | Contenido QR personal | PARTICIPANTE |
| GET `/api/proyectos` | Lista | Autenticado |
| GET `/api/proyectos/{id}` | Detalle | Autenticado |
| POST `/api/proyectos` | Crear, 201 | DOCENTE/ADMIN |
| PUT `/api/proyectos/{id}` | Actualizar | Responsable/ADMIN |
| DELETE `/api/proyectos/{id}` | Eliminar, 204 | Responsable/ADMIN; sin eventos |
| GET `/api/eventos?proyectoId={id}` | Lista, filtro opcional | Autenticado |
| GET `/api/eventos/{id}` | Detalle con indicadores del usuario | Autenticado |
| POST `/api/eventos` | Crear, 201 | Responsable del proyecto/ADMIN |
| PUT `/api/eventos/{id}` | Actualizar o cambiar estado | Responsable/ADMIN, origen y destino |
| DELETE `/api/eventos/{id}` | Eliminar, 204 | Responsable/ADMIN; sin historial incompatible |
| GET `/api/eventos/{id}/qr` | QR del evento | Responsable/ADMIN |
| POST `/api/eventos/inscribirse` | Inscribir usuario actual | PARTICIPANTE/ESTUDIANTE legacy |
| GET `/api/eventos/{id}/asistentes` | Lista de inscritos y presencia | Receptor autorizado |
| POST `/api/eventos/{id}/asistencias/escanear` | Registrar asistencia | Receptor autorizado |
| GET `/api/eventos/{id}/asistencias/me` | Asistencia propia | PARTICIPANTE/ESTUDIANTE legacy |
| PUT `/api/eventos/{id}/asistencias/me` | Operación denegada | `denyAll`: no automarcación |
| PUT `/api/eventos/{id}/encuestas/me` | Guardar/actualizar encuesta propia | PARTICIPANTE presente/legacy |
| GET `/api/eventos/{id}/encuestas` | Encuestas del evento | Responsable/ADMIN |
| POST `/api/sync/asistencias` | Procesar lote | Receptor autorizado; 1 a 100 elementos |
| POST `/api/sync/encuestas` | Procesar lote | PARTICIPANTE/legacy; 1 a 100 elementos |

La app Android actual procesa su cola enviando las solicitudes individuales; que exista una API batch no significa que el cliente la esté utilizando.

## Endpoints de autenticación conservados

`/api/auth` incluye POST login, registro, logout, recuperación de contraseña y GET me. Android inicia sesión por Hosted UI, no enviando usuario/contraseña a `/api/auth/login`. El registro público asigna PARTICIPANTE y no permite elegir roles privilegiados. El login por contraseña requiere habilitar ese flujo tanto en la configuración de la aplicación como en Cognito; no es necesario para el flujo móvil principal.

## Ejemplos de Postman

Crear variables `baseUrl`, `accessToken`, `proyectoId`, `eventoId` y `participanteId`. Los IDs deben corresponder a recursos de prueba reales; no asumir que 1 siempre existe. En Authorization elegir Bearer Token e ingresar `{{accessToken}}`. En cuerpos JSON utilizar Content-Type application/json. La URL contiene solo la dirección; seleccionar POST/PUT/etc. en el selector del método.

### Proyecto: crear y editar

POST `{{baseUrl}}/api/proyectos`. Para editar usar PUT `{{baseUrl}}/api/proyectos/{{proyectoId}}` con la representación completa:

```json
{
  "nombre": "Proyecto de prueba",
  "descripcion": "Actividad académica con datos ficticios",
  "areaTematica": "Tecnología",
  "unidadVecinal": "Unidad de demostración",
  "fechaInicio": "2026-12-01",
  "fechaFin": "2026-12-31",
  "estado": "PLANIFICADO"
}
```

Consultar con GET de lista/detalle; eliminar con DELETE de detalle solo cuando no tenga eventos.

### Evento: crear, editar y cancelar

POST `{{baseUrl}}/api/eventos`; para editar, PUT `{{baseUrl}}/api/eventos/{{eventoId}}`:

```json
{
  "proyectoId": {{proyectoId}},
  "nombre": "Taller de prueba",
  "descripcion": "Demostración del CRUD",
  "fechaHora": "2026-12-04T14:00:00",
  "cupos": 30,
  "recinto": "Recinto ficticio",
  "direccion": "Dirección de demostración",
  "latitud": null,
  "longitud": null,
  "radioMetros": 200,
  "estado": "PROGRAMADO"
}
```

Las variables numéricas sin comillas del ejemplo son plantillas de Postman: deben resolverse antes de enviarlo como JSON. Cancelar usa el mismo PUT completo cambiando estado a CANCELADO; no existe un endpoint `/cancelar`. Estados: PROGRAMADO, EN_CURSO, FINALIZADO y CANCELADO. Cupos 0 significa sin límite. fechaHora es LocalDateTime, sin Z ni conversión automática de zona horaria.

### Inscripción y asistencia

Con participante, POST `{{baseUrl}}/api/eventos/inscribirse`:

```json
{"codigoQr":"EVENTO_{{eventoId}}"}
```

Con receptor autorizado, POST `{{baseUrl}}/api/eventos/{{eventoId}}/asistencias/escanear`:

```json
{
  "codigoQr": "SYNC_PASS:ASISTENCIA:{{eventoId}}:{{participanteId}}",
  "clientUuid": "{{operationUuid}}",
  "latitud": null,
  "longitud": null,
  "precisionMetros": null
}
```

Definir operationUuid como UUID nuevo por operación lógica y conservarlo para reintentar la misma captura. No reutilizarlo para otro evento/persona. marcadoEn es opcional: si se omite, se utiliza el instante del servidor. La credencial compatible `ESTUDIANTE_<id>` también se acepta y se aplica al evento de la URL.

### Encuesta

Con participante presente, PUT `{{baseUrl}}/api/eventos/{{eventoId}}/encuestas/me`:

```json
{"satisfaccion":5,"utilidad":4,"comentarios":"Prueba académica"}
```

El cuerpo de actualización de perfil es `{"nombre":"Participante de prueba"}`. Los lotes de sincronización reciben arreglos de DTO `AsistenciaSyncItem` o `EncuestaSyncItem`, definidos en ApiDtos; el contrato incluye conteo de procesados, errores y mensajes. No son un CRUD adicional.

## Errores y reglas

| Código | Ejemplo real |
|---|---|
| 400 | DTO inválido, JSON incorrecto, QR mal formado |
| 401 | Access token ausente, inválido, expirado o cliente no permitido |
| 403 | Rol insuficiente, docente ajeno, automarcación |
| 404 | Recurso o asistencia propia inexistente |
| 409 | Proyecto con eventos, evento con historial, cupos menores que inscritos, UUID conflictivo |
| 422 | Evento cerrado, sin cupo, coordenadas incompletas, encuesta sin presencia, QR de otro evento |
| 429 | Límites o exceso de solicitudes de Cognito |
| 500 | Error interno inesperado, sin detalle técnico en respuesta |

Los errores del negocio y validación usan `ErrorResponse`: error, mensaje, status y detalles. Los errores previos de autenticación pueden ser resueltos directamente por Spring Security: no asumir que siempre tendrán idéntico cuerpo JSON.

## Protección y límites

No registrar Authorization ni cuerpos con tokens. El logging HTTP Android es BASIC en debug y NONE en release. No incluir secretos en ejemplos. El perfil propio expone identificadores como id/cognitoSub para relación técnica; la decisión de no mostrarlos como nombre no significa que se hayan eliminado del contrato.

CORS restringe solicitudes de navegadores, no es autenticación ni restringe Postman. Los QR no son credenciales criptográficas: pueden copiarse y dependen de que un receptor autorizado confirme la persona/evento. La versión académica no implementa QR rotativo firmado ni verificación biométrica.
