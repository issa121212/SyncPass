# Problema, alcance y requisitos

## Problemática

Según [caso.md](caso.md), la consultora Ticdgm plantea digitalizar la gestión de vinculación comunitaria: planificación en herramientas aisladas, asistencia en papel, encuestas tardías y consolidación manual. Los problemas son pérdida de evidencia, filas, baja respuesta de encuestas y conectividad inestable en terreno.

SyncPass centraliza proyectos y eventos, permite acreditar participantes mediante QR y conserva operaciones locales para sincronizarlas después. Los actores son docente/coordinación, estudiante voluntario/recepción y participante/beneficiario. ADMIN es un rol técnico adicional.

El alcance es un MVP académico Android con backend REST. El caso exige información ficticia o anonimizada. La eliminación total del papel, acreditación menor a cinco segundos, respuesta de encuestas superior al 65%, disponibilidad del 100% y ahorro administrativo del 80% son metas del caso: no existen mediciones del proyecto que demuestren esos resultados.

## Requisitos funcionales

Los requisitos siguientes corresponden a las funcionalidades de la entrega. La última columna relaciona cada requisito con sus componentes de implementación.

| ID | Actor y condición | Resultado esperado | Evidencia de implementación |
|---|---|---|---|
| RF-01 | Usuario con cuenta Cognito | Inicia sesión mediante Hosted UI y regresa a Android | `cognito-auth`, `LoginScreen` |
| RF-02 | Usuario con sesión | Ve nombre y opciones según roles, y puede cerrar sesión | `SessionStore`, `HomeUiState`, navegación |
| RF-03 | Docente o ADMIN | Crea y consulta proyectos de negocio mediante HTTP | `ApiProyectosRepository`, `ProyectoService` |
| RF-04 | Responsable o ADMIN | Edita proyectos; elimina solo sin eventos asociados | `GestionScreen`, `ProyectoService` |
| RF-05 | Usuario autenticado | Consulta catálogo y detalle de eventos | `SyncPassRepository`, `EventoController` |
| RF-06 | Docente responsable o ADMIN | Crea evento asociado a un proyecto permitido | `EventoEditorDialog`, `EventoService` |
| RF-07 | Responsable o ADMIN | Edita evento mediante PUT, con validación y copia local confirmada | `EventoDetalleViewModel`, `EventoRules` |
| RF-08 | Responsable o ADMIN | Cancela conservando historial; elimina sin historial incompatible | `EventoDetalleViewModel`, `EventoService` |
| RF-09 | Docente desde gestión | Crea, consulta, edita y elimina recintos en Room | `RoomRecintosRepository`, `RecintoDao` |
| RF-10 | Participante con cupo y evento habilitado | Se inscribe sin duplicar la inscripción | `EventoService.inscribirse` |
| RF-11 | Participante | Muestra credencial personal o QR vinculado al evento | `QrCode`, `EventoDetalleScreen` |
| RF-12 | Receptor autorizado | Escanea y registra asistencia solo en el evento seleccionado | `ScannerViewModel`, `EventoService.escanear` |
| RF-13 | Responsable, ADMIN o voluntario autorizado | Consulta inscritos y presentes del evento | asistentes y comprobación de permisos |
| RF-14 | Participante presente | Envía o actualiza encuesta con satisfacción/utilidad de 1 a 5 | `EncuestaService.guardarPropia` |
| RF-15 | Responsable o ADMIN | Consulta encuestas del evento administrado | `EncuestaService.listarPorEvento` |
| RF-16 | Usuario con datos ya descargados | Consulta copia local; encola inscripción, asistencia y encuesta según operación | Room y `PendienteEntity` |
| RF-17 | Usuario con conexión y sesión válida | Envía pendientes y distingue errores transitorios de rechazos | `sincronizar`, `SyncWorker` |
| RF-18 | Usuario que crea o edita evento | Introduce fecha/hora separadas y ve guiones/dos puntos automáticos | `FechaCampo`, `FormatoFechaHora`, `EventoEditorDialog` |

La cancelación de una inscripción individual, reportes exportables y administración completa de usuarios no se presentan como funcionalidades entregadas por Android.

## Requisitos no funcionales

| ID | Requisito | Evidencia y límite |
|---|---|---|
| RNF-01 | Autenticación verificable en servidor | JWT, firma, issuer, expiración, token_use y client_id; no basta decodificarlo en Android |
| RNF-02 | Autorización por rol y propiedad | `@PreAuthorize` y servicios; ver matriz de permisos |
| RNF-03 | Protección de sesión y secretos | AES-GCM/Keystore para tokens, `.gitignore`, configuración externa; no equivale a cifrar toda Room |
| RNF-04 | Transporte HTTPS remoto | Entrada TLS por API Gateway y Nginx; HTTP local permitido solo en debug |
| RNF-05 | Mantenibilidad | MVVM y repositorios en Android; capas en Spring; DTO record y pruebas |
| RNF-06 | Trabajo con conectividad intermitente | Catálogo y cola local; login y CRUD remoto requieren red; sin garantía absoluta offline |
| RNF-07 | Rendimiento y uso de recursos | Corrutinas, Flow, listas diferidas, trabajo de fondo con restricciones; sin benchmark publicado |
| RNF-08 | Usabilidad y accesibilidad | Material 3, mensajes, editor por pasos y scroll cuando hace falta; la validación no abarca todas las combinaciones de pantalla y texto |
| RNF-09 | Consistencia de operaciones | Transacciones, UUID para asistencia, restricciones de cupos, confirmación remota al editar/eliminar |
| RNF-10 | Portabilidad | Gradle Wrapper y ejemplos de entorno; requiere SDK/JDK, Cognito y servidor accesible |
| RNF-11 | Disponibilidad | Servicio systemd en EC2; depende del laboratorio, instancia, base de datos y certificados; sin SLA |
| RNF-12 | Verificabilidad | Pruebas unitarias e instrumentadas. La variante debug de Android alcanza 44,01 % de cobertura combinada de líneas. Ver [reproducción del informe](08-VALIDACION-Y-GITFLOW.md#cobertura-android-con-jacoco). |
| RNF-13 | Separación por cuenta | Room y repositorios ligados a issuer/sub; recintos no compartidos automáticamente |
| RNF-14 | Compatibilidad | minSdk 24, target/compile 37; el caso pide Android 8/API 26 o superior, incluido en ese rango; no se ha validado cada versión |

## Casos de uso y flujos

### CU-01 · Iniciar sesión y abrir el inicio

- **Actores:** docente, estudiante voluntario y participante. Se requiere una cuenta Cognito habilitada.
- **Flujo:** la persona elige continuar con Cognito, se autentica en el navegador y retorna a Android. La app guarda la sesión y presenta las acciones según el perfil.
- **Alternativa:** si se cancela el acceso, se mantiene la pantalla de inicio de sesión. Si expira la sesión, hay que autenticarse de nuevo.
- **Trazabilidad:** RF-01, RF-02, RNF-01 a RNF-03.

### CU-02 · Gestionar proyectos y eventos

- **Actor:** docente responsable o administrador autorizado. Se requiere conexión y, para el evento, un proyecto permitido.
- **Flujo:** el docente elige el proyecto, completa los pasos del formulario y guarda. La API valida los datos y permisos. Con la respuesta exitosa, Android actualiza su copia en Room.
- **Alternativas:** se puede cancelar el evento y conservar su historial. El servidor rechaza la eliminación si hay historial incompatible. Si falla la red o la validación, se conserva la última versión confirmada en Room.
- **Trazabilidad:** RF-03 a RF-08, RF-18, RNF-02 y RNF-09.

### CU-03 · Gestionar recintos locales

- **Actor:** docente desde la pantalla de gestión. Puede trabajar con los datos de su cuenta en el dispositivo sin conexión.
- **Flujo:** abre la sección de recintos, completa los campos y guarda. Room actualiza la lista. Al elegir el recinto en el editor de eventos, se copian sus campos al formulario.
- **Alternativas:** puede editar o eliminar un recinto local. La creación del evento asociado requiere conexión.
- **Trazabilidad:** RF-09, RNF-06, RNF-13.

### CU-04 · Inscribirse y presentar el QR

- **Actor:** participante. El evento debe estar habilitado y contar con cupo.
- **Flujo:** abre el evento, solicita la inscripción y muestra su credencial QR para recepción.
- **Alternativa:** ante un error transitorio, Android puede guardar la solicitud en la cola local. El servidor verifica las condiciones de inscripción cuando recibe la solicitud.
- **Trazabilidad:** RF-05, RF-10 y RF-11.

### CU-05 · Registrar y sincronizar asistencia

- **Actores:** participante y estudiante voluntario de recepción. También pueden escanear el docente responsable y el administrador según sus permisos.
- **Flujo:** recepción abre el evento correcto y escanea el QR. Android guarda la captura con un UUID y envía el contenido leído a la API. El servidor valida el evento, los permisos y el registro de asistencia.
- **Alternativa sin red:** la captura queda en Room. Al reconectar, el operador pulsa **Sincronizar** o espera al trabajo programado. El reintento conserva el UUID. Los rechazos definitivos se registran para consulta.
- **Verificación:** comprobar que la captura se registre una sola vez y en el evento seleccionado.
- **Trazabilidad:** RF-12, RF-13, RF-16, RF-17, RNF-06 y RNF-09.

### CU-06 · Responder y consultar encuestas

- **Actores:** participante con asistencia registrada y docente responsable o administrador para la consulta.
- **Flujo:** el participante asigna valores de satisfacción y utilidad de 1 a 5 y puede agregar un comentario. El backend verifica la asistencia. El docente consulta las encuestas desde el evento que administra.
- **Alternativa:** Android conserva el envío en su cola si todavía no puede sincronizarlo.
- **Trazabilidad:** RF-14, RF-15, RF-17.

## Diferencias respecto a la visión del caso

- El caso menciona QR dinámico; el MVP usa identificadores QR legibles y no firmados. No hay caducidad criptográfica ni protección completa contra copia de credenciales.
- La recepción incorpora comprobación de ubicación, precisión y distancia en Android y backend. El backend inicia en modo `AUDIT` compatible; la geocerca obligatoria requiere activar `ENFORCE` después de actualizar y verificar los clientes. Véase [la política geográfica](11-GEOLOCALIZACION-ASISTENCIA.md).
- La cola no cubre todo el CRUD y el trabajo periódico no garantiza envío inmediato al volver la señal.
- No se entrega un motor de informes institucionales ni una medición formal del impacto.
- La paleta puede variar por los colores dinámicos de Android; la consistencia azul observada no es una garantía de color fijo en todos los equipos.
