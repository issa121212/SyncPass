# 04. Actores, Roles y Permisos

## Actores del Sistema

| Actor | Tipo | Descripción |
|---|---|---|
| **Docente/Coordinador** | Usuario autenticado | Responsable de uno o más proyectos. Gestiona eventos, escanea QRs, visualiza asistencias y encuestas. |
| **Estudiante/Voluntario** | Usuario autenticado | Se inscribe a eventos, marca asistencia, responde encuestas y gestiona su QR personal. |
| **Administrador** | Usuario autenticado | Rol con privilegios ampliados (gestionable vía grupo Cognito). |

## Mapeo Cognito → Roles Internos

El backend mapea los grupos de Cognito a roles Spring Security:

| Grupo Cognito | Rol Spring | Descripción |
|---|---|---|
| `profesor`, `docente`, `teacher`, `coordinador` | `ROLE_DOCENTE` | Docente/coordinador con permisos sobre sus proyectos |
| `estudiante`, `alumno`, `alumnos`, `student`, `monitor` | `ROLE_ESTUDIANTE` | Participante |
| `admin`, `administrador`, `superadmin` | `ROLE_ADMIN` + `ROLE_DOCENTE` | Administrador con permisos globales |

La normalización está en `security/CognitoRoles.java` y lee
`cognito:groups` del JWT. Si el usuario no tiene ningún grupo reconocido,
se le asigna `ESTUDIANTE` por defecto.

## Matriz de Permisos por Funcionalidad

### Proyectos
| Acción | DOCENTE | ESTUDIANTE | ADMIN | Notas |
|---|---|---|---|---|
| Listar proyectos | ✓ | ✓ | ✓ | Según contexto/ownership |
| Crear proyecto | ✓ | ✗ | ✓ | Se asigna como responsable |
| Ver proyecto | ✓ (propio) | ✓ | ✓ | |
| Editar proyecto | ✓ (responsable) | ✗ | ✓ | Validación ownership |
| Eliminar proyecto | ✓ (responsable) | ✗ | ✓ | Validación ownership |

### Eventos
| Acción | DOCENTE | ESTUDIANTE | ADMIN | Notas |
|---|---|---|---|---|
| Listar eventos | ✓ | ✓ | ✓ | |
| Crear evento | ✓ (proyecto propio) | ✗ | ✓ | |
| Ver evento | ✓ | ✓ | ✓ | |
| Ver QR del evento | ✓ (responsable) | ✗ | ✓ | `GET /api/eventos/{id}/qr` |
| Editar evento | ✓ (responsable) | ✗ | ✓ | |
| Eliminar evento | ✓ (responsable) | ✗ | ✓ | |

### Inscripciones
| Acción | DOCENTE | ESTUDIANTE | ADMIN | Notas |
|---|---|---|---|---|
| Inscribirse a evento | ✗ | ✓ | ✗ | `POST /api/eventos/inscribirse` (solo `ESTUDIANTE`) |
| Ver asistentes/inscritos | ✓ (evento propio) | ✗ | ✓ | `GET /api/eventos/{id}/asistentes` |

### Asistencias
| Acción | DOCENTE | ESTUDIANTE | ADMIN | Notas |
|---|---|---|---|---|
| Escanear QR estudiante (marcar) | ✓ | ✗ | ✓ | Puede inscribir si no existe |
| Marcar asistencia propia | ✗ | ✓ | ✗ | Requiere estar inscrito |
| Ver asistencias por evento | ✓ (propio) | ✗ | ✓ | `GET /api/eventos/{id}/asistentes` |
| Sincronizar asistencias escaneadas (batch) | ✓ | ✗ | ✓ | `POST /api/sync/asistencias` |

### Encuestas
| Acción | DOCENTE | ESTUDIANTE | ADMIN | Notas |
|---|---|---|---|---|
| Responder encuesta | ✗ | ✓ | ✗ | Solo si presente (`hasRole('ESTUDIANTE')`) |
| Ver encuestas por evento | ✓ (propio) | ✗ | ✓ | |
| Sincronizar encuestas (batch) | ✗ | ✓ | ✗ | `POST /api/sync/encuestas` |

### Sincronización
| Acción | DOCENTE | ESTUDIANTE | ADMIN | Notas |
|---|---|---|---|---|
| Sincronizar inscripción | ✗ | ✓ | ✗ | `POST /api/eventos/inscribirse` (una por una) |
| Sincronizar asistencia propia | ✗ | ✓ | ✗ | `PUT /api/eventos/{id}/asistencias/me` |

## Reglas de Ownership

- **Proyecto**: el usuario autenticado debe ser el `responsable` del proyecto (o tener rol ADMIN).
- **Evento**: Pertenece a proyecto; se valida ownership vía proyecto responsable.
- **Asistencia**: Docente puede marcar para cualquier estudiante inscrito en evento de su proyecto. Estudiante solo propia.
- **Encuesta**: Solo estudiante que esté marcado como presente puede responder.

## Permisos en App Móvil

La app adapta navegación según rol:

| Rol | Pantallas Principales |
|---|---|
| **DOCENTE** | Inicio Docente, Proyectos, Eventos, Escanear QR Estudiante, Asistencias, Encuestas |
| **ESTUDIANTE** | Inicio Estudiante, Eventos, Mi QR, Detalle con Inscripción/Asistencia/Encuesta |
| **Sin rol/Vacío** | Requiere re-autenticación o asignación de rol |
