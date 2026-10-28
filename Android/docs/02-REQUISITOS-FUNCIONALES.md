# 02. Requerimientos Funcionales

## Actores

| Actor | Descripción |
|---|---|
| **Docente/Coordinador** | Responsable de proyecto y eventos. Crea proyectos/eventos, escanea QR de estudiantes, visualiza asistencias y encuestas. |
| **Estudiante/Voluntario** | Se inscribe a eventos, marca su asistencia, responde encuestas, gestiona su QR personal. |
| **Administrador** | Gestión general del sistema (opcional, mayoritariamente vía roles). |

## Requerimientos Funcionales por Actor

### RF-DOC-01: Gestión de Proyectos
- **RF-DOC-01.1**: Crear proyecto comunitario con nombre, descripción, área temática, unidad vecinal, fechas y estado.
- **RF-DOC-01.2**: Visualizar lista de proyectos.
- **RF-DOC-01.3**: Solo el responsable del proyecto o ADMIN puede editar/eliminar.
- **RF-DOC-01.4**: Asignación automática de responsable al crear.

### RF-DOC-02: Gestión de Eventos/Talleres
- **RF-DOC-02.1**: Crear evento vinculado a un proyecto con nombre, fecha/hora, cupos, ubicación (recinto/dirección), geolocalización, radio.
- **RF-DOC-02.2**: Visualizar eventos por proyecto.
- **RF-DOC-02.3**: El backend asigna el QR del evento (`EVENTO_{id}`); la app aún no muestra ese QR en pantalla.
- **RF-DOC-02.4**: Solo responsable del proyecto o ADMIN puede crear/editar/eliminar eventos.
- **RF-DOC-02.5**: Cupos = 0 significa cupos abiertos.

### RF-DOC-03: Control de Asistencia
- **RF-DOC-03.1**: Escanear QR personal del estudiante (`ESTUDIANTE_{id}`) para marcar asistencia.
- **RF-DOC-03.2**: Si estudiante no está inscrito, inscribirlo automáticamente al escanear (validando cupos).
- **RF-DOC-03.3**: Visualizar lista de asistentes con estado (inscrito/presente), fecha de marcación y encuesta.
- **RF-DOC-03.4**: Registrar ubicación GPS al escanear (referencial).
- **RF-DOC-03.5**: Sincronización por lotes de asistencias escaneadas offline.

### RF-DOC-04: Visualización de Encuestas
- **RF-DOC-04.1**: Visualizar encuestas respondidas por evento con calificaciones y comentarios.
- **RF-DOC-04.2**: Ver detalle por estudiante.

### RF-EST-01: Inscripción a Eventos
- **RF-EST-01.1**: Visualizar catálogo de eventos.
- **RF-EST-01.2**: Inscribirse desde el detalle del evento (la API acepta el QR `EVENTO_{id}`).
- **RF-EST-01.3**: Inscripción idempotente (evita duplicados).
- **RF-EST-01.4**: Validación de cupos y estado del evento.
- **RF-EST-01.5**: Colocación en cola offline si no hay conexión.

### RF-EST-02: Acreditación de Asistencia
- **RF-EST-02.1**: Marcar asistencia propia desde detalle del evento.
- **RF-EST-02.2**: Captura de ubicación GPS al marcar (con permiso).
- **RF-EST-02.3**: Solo puede marcar si está inscrito.
- **RF-EST-02.4**: Colocación en cola offline si no hay conexión.
- **RF-EST-02.5**: Sincronización automática/manual al recuperar conexión.

### RF-EST-03: Encuestas de Satisfacción
- **RF-EST-03.1**: Responder encuesta solo si está marcado como presente.
- **RF-EST-03.2**: Calificación satisfacción y utilidad (1-5) con slider.
- **RF-EST-03.3**: Comentarios opcionales.
- **RF-EST-03.4**: Una encuesta por asistencia/evento.
- **RF-EST-03.5**: Sincronización por lotes offline.

### RF-EST-04: Mi QR Personal
- **RF-EST-04.1**: Visualizar QR personal (`ESTUDIANTE_{id}`).
- **RF-EST-04.2**: QR generado por backend desde perfil autenticado.

### RF-COM-01: Autenticación y Sesión
- **RF-COM-01.1**: Inicio de sesión con Cognito Hosted UI (Authorization Code + PKCE).
- **RF-COM-01.2**: Persistencia de sesión en SharedPreferences.
- **RF-COM-01.3**: Validación de token JWT (expiración).
- **RF-COM-01.4**: Cierre de sesión (limpieza local + logout Cognito).
- **RF-COM-01.5**: Restauración de sesión al abrir app.

### RF-COM-02: Sincronización Offline
- **RF-COM-02.1**: Todas las acciones se guardan primero en Room.
- **RF-COM-02.2**: Sincronización automática con WorkManager (cada 15 min).
- **RF-COM-02.3**: Sincronización manual desde Home.
- **RF-COM-02.4**: Visualización de operaciones pendientes.
- **RF-COM-02.5**: Al intentar una operación, los `4xx` definitivos no se encolan; red/5xx/408/429 sí.
- **RF-COM-02.6**: Idempotencia con `clientUuid`.

## Criterios de Aceptación

| ID | Criterio |
|---|---|
| CA-01 | Docente puede crear proyecto y evento; el backend genera el QR del evento. |
| CA-02 | Estudiante puede inscribirse desde el detalle del evento. |
| CA-03 | Docente puede escanear QR de estudiante y marcar asistencia (inscribiendo si no existe). |
| CA-04 | Estudiante puede marcar su propia asistencia con ubicación. |
| CA-05 | Estudiante solo puede encuestar si está presente. |
| CA-06 | App funciona offline: acciones se encolan y sincronizan al recuperar conexión. |
| CA-07 | Solo responsable puede gestionar proyecto/evento. |
| CA-08 | Solo estudiante puede inscribirse, marcar asistencia propia y encuestar. |
| CA-09 | Solo docente/admin puede escanear y ver listas de asistentes/encuestas. |
| CA-10 | Tokens expirados redirigen a login. |
