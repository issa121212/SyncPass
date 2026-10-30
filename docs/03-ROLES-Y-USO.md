# Roles, dashboards y manual de uso

## Identidad y roles

Cognito autentica a la persona. `CognitoRoles` transforma los grupos en roles internos: docente/profesor/coordinador -> DOCENTE; estudiante/alumno/monitor y recepción -> VOLUNTARIO; beneficiario/asistente -> PARTICIPANTE; administrador -> ADMIN y DOCENTE. Grupos desconocidos se ignoran y, sin coincidencias, el backend asigna PARTICIPANTE. La constante ESTUDIANTE se conserva por compatibilidad; no representa automáticamente al grupo actual estudiante, que se normaliza como voluntario.

La interfaz se adapta al perfil recibido del servidor. Tener un botón oculto no es la barrera de seguridad: `@PreAuthorize` y los servicios validan de nuevo cada solicitud.

## Matriz de permisos

| Acción | Participante | Estudiante/voluntario | Docente | ADMIN |
|---|---|---|---|---|
| Consultar proyectos y eventos | Sí | Sí | Sí | Sí |
| Crear proyecto | No | No | Sí, queda responsable | Sí |
| Editar/eliminar proyecto | No | No | Solo responsable | Cualquiera, sujeto a reglas |
| Crear evento | No | No | En proyecto propio | Sí |
| Editar/cancelar/eliminar evento | No | No | Responsable del proyecto | Sí |
| Inscribirse | Sí | No con VOLUNTARIO únicamente | No con DOCENTE únicamente | No con ADMIN únicamente |
| Mostrar credencial QR propia de participante | Sí | No con VOLUNTARIO únicamente | No con DOCENTE únicamente | No con ADMIN únicamente |
| Escanear asistencia | No | Sí | Solo proyecto propio | Sí |
| Ver lista de asistentes | No | Sí | Solo proyecto propio | Sí |
| Ver asistencia propia/responder encuesta | Sí; encuesta exige presente | No con VOLUNTARIO únicamente | No con DOCENTE únicamente | No con ADMIN únicamente |
| Ver encuestas del evento | No | No | Solo proyecto propio | Sí |
| Administrar recintos locales desde gestión | No en la UI | No en la UI | Sí | Sí |

Los permisos se combinan cuando una cuenta tiene varios roles. Los endpoints de inscripción/encuesta también contemplan ESTUDIANTE legacy; comprobar la normalización antes de asumir que el grupo estudiante obtiene esos permisos. Recintos es una función local: su restricción no proviene de un endpoint HTTP.

## Inicio de sesión

1. Abrir SyncPass y pulsar Continuar con Cognito.
2. Autenticarse en la página administrada de Cognito con una cuenta de prueba.
3. Regresar a la app mediante `com.rojas.syncpass://cognito/callback`.
4. Esperar la descarga del perfil y catálogo.
5. Comprobar el saludo y el rol. El saludo lee `name` del ID token de la misma identidad; no se utiliza el sub como nombre.

Una sesión ya iniciada en el navegador puede evitar que Cognito vuelva a pedir contraseña. Eso no significa que se haya omitido el flujo de autenticación. Cerrar sesión para cambiar de cuenta. Si expira el token, iniciar sesión nuevamente: no hay renovación persistente mediante refresh token.

## Dashboard docente

Muestra encabezado de coordinación, proyectos y eventos, conteos de inscritos/presentes, Actualizar, Sincronizar y Salir. Ofrece Nuevo proyecto, Nuevo evento y Administrar proyectos y recintos.

### Proyectos

1. Crear un proyecto con nombre y datos del negocio.
2. Abrir administración para consultar, editar o eliminar los proyectos permitidos.
3. Si hay eventos asociados, el servidor rechaza la eliminación con 409. Resolver las relaciones o conservar el proyecto según la necesidad del negocio.

Los nombres de responsables son legibles; los identificadores siguen existiendo en las relaciones y contratos HTTP.

### Eventos

1. Pulsar Nuevo evento.
2. Paso 1: seleccionar un proyecto permitido mediante opciones visibles; ingresar nombre y descripción.
3. Paso 2: ingresar fecha, hora y cupos. Cero significa sin límite. Los separadores se agregan visualmente.
4. Paso 3: completar ubicación opcional o copiar un recinto local. Si se ingresan coordenadas deben completarse ambas.
5. Guardar y comprobar el resultado del servidor.
6. En el detalle, abrir Gestionar para editar, cancelar o eliminar según permisos.

Editar conserva los datos y mantiene el formulario si la API falla. Cancelar cambia a CANCELADO sin borrar historial. Eliminar pide confirmación y solo se permite sin historial incompatible; en Android se bloquea cuando se conocen inscritos o presentes. El servidor conserva la decisión final si los datos cambiaron en otro dispositivo.

El menú Gestionar agrupa las acciones del evento para mantener el detalle legible y evitar formularios extensos en una sola vista.

### Recintos

Abrir Administrar proyectos y recintos. Crear, editar o eliminar un recinto. No requiere servidor y pertenece a los datos locales de esa cuenta/dispositivo. Al usarlo en un evento se copian sus valores; no se crea un vínculo remoto con una entidad recinto.

## Dashboard estudiante/voluntario

Muestra catálogo y acceso a eventos para recepción. Abrir el evento correcto antes de escanear. La cuenta con VOLUNTARIO no obtiene por ese solo rol los controles docentes de creación o gestión.

1. Abrir el evento donde se registrará asistencia.
2. Pulsar Escanear QR de participante.
3. Autorizar cámara si el sistema lo solicita.
4. Enfocar el QR que muestra el participante.
5. Leer el resultado: confirmado en servidor o captura pendiente.
6. Revisar pendientes y rechazadas antes de considerar completo el registro.

Docente responsable y ADMIN también pueden utilizar recepción; no es una capacidad exclusiva del estudiante.

## Dashboard participante

Muestra eventos y sus indicadores personales de inscripción, asistencia y encuesta. Permite consultar Mi QR y abrir detalles.

1. Abrir un evento disponible.
2. Inscribirse cuando corresponda. Inscribirse no equivale a estar presente.
3. Seleccionar Mostrar QR para asistencia.
4. Presentarlo al receptor autorizado.
5. Actualizar para consultar la asistencia confirmada.
6. Si está presente y la opción está disponible, responder la encuesta.

La encuesta tiene satisfacción y utilidad de 1 a 5 y comentario opcional. La API guarda o actualiza una encuesta por asistencia; no debe documentarse como rechazo obligatorio de toda segunda respuesta.

## Flujo entre PC y celular

Para demostrarlo con un solo dispositivo con cámara, el PC puede mostrar el QR del participante en la web y el teléfono Android, con voluntario o docente responsable, escanearlo dentro del evento. Ambos deben apuntar a la misma API y al mismo evento.

El QR del detalle Android incluye `SYNC_PASS:ASISTENCIA:<eventoId>:<usuarioId>`, por lo que un evento distinto se rechaza. La credencial personal compatible `ESTUDIANTE_<id>` se aplica al evento que seleccionó recepción. El prefijo histórico no cambia que el titular deba ser PARTICIPANTE para registrar asistencia.

El backend puede inscribir al participante durante el escaneo si no estaba inscrito y hay cupo. No es necesario afirmar que toda asistencia exige inscripción manual previa.

## Incidencias frecuentes

- Sin conexión: se muestra la caché y solo las acciones habilitadas localmente pueden continuar.
- Pendiente: aún no existe confirmación remota; no presentar como asistencia definitiva.
- 401: volver a autenticarse y comprobar token/cliente permitido.
- 403: comprobar rol o responsable; reinstalar no concede permisos.
- 409: conflicto de historial, relaciones o capacidad.
- 422: regla del negocio, como evento cerrado, sin cupos o encuesta sin presencia.
- Nombre no visible: comprobar atributo name y permisos de lectura en el App Client; no reemplazarlo por el sub.
