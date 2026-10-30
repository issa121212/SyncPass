# CRUD de eventos en Android

La creación y consulta existentes se completan con edición, eliminación y cancelación desde el detalle del evento. El menú **Gestionar** está disponible para el docente responsable del proyecto y para administración. El backend siempre vuelve a comprobar permisos, cupos e historial.

## Uso

- Crear: **Nuevo evento**, formulario de tres pasos (datos, fecha/cupos, ubicación).
- Editar: abrir un evento y seleccionar **Gestionar > Editar**. El mismo formulario permite cambiar el estado y moverlo a otro proyecto gestionable.
- Eliminar: **Gestionar > Eliminar**, con confirmación. Si hay inscritos o presentes se deshabilita esta opción; un conflicto concurrente del servidor mantiene el evento y muestra el error.
- Cancelar: **Gestionar > Cancelar evento**, con confirmación. Se conservan los registros asociados; se consulta la versión actual antes de cambiar únicamente su estado.

El formulario muestra nombre de proyecto y fecha/hora separadas con separadores automáticos. Los proyectos siguen visibles como opciones de selección. Las acciones de navegación del formulario quedan fuera del contenido desplazable; solo se desplaza el paso actual si la altura, el teclado o el tamaño del texto lo requieren. No se suprime el desplazamiento necesario para acceder a campos en pantallas pequeñas.

## Capas y persistencia

`SyncPassApi` usa POST y GET existentes y agrega PUT/DELETE `/api/eventos/{id}`. `SyncPassOperations` expone las operaciones al ViewModel; `SyncPassRepository` realiza la escritura remota y actualiza Room únicamente después de la confirmación. Las operaciones se serializan con la actualización del catálogo para evitar que una respuesta antigua reemplace una escritura reciente.

Las modificaciones del evento requieren conexión; no se encolan como escrituras offline. Una edición respeta las operaciones locales pendientes al actualizar la copia. Se impide eliminar si el dispositivo tiene operaciones pendientes del evento. El formulario de edición permanece abierto ante errores y se bloquean envíos repetidos durante una escritura.

`EventoRules` valida el contrato de negocio y los permisos de presentación. El ViewModel coordina las operaciones y su estado, sin llamadas Retrofit desde la pantalla. La validación incluye cupos, calendario, límites de texto, coordenadas completas y radio de 1 a 50000 metros. Al editar otros campos se conservan los segundos y fracciones de la fecha original.

## Gitflow

Primero se creó `develop` desde el `main` existente, sin reescribir el historial. El trabajo se implementa en `feature/crud-eventos-android`, con commits por capa, interfaz y validación. La integración debe hacerse mediante un PR a `develop`; la versión de Android se incrementará al preparar una release real.

## Comprobaciones

Pruebas unitarias: respuesta confirmada antes de guardar en Room, fallo de red, eliminación ordenada, conflicto, pendientes locales, permisos y validación. Pruebas Compose: formulario por pasos, conservación de fecha, cupo mínimo y bloqueo durante escritura.

Revisión manual con API: entrar como docente responsable, crear un evento de prueba, editarlo y comprobarlo en la web, eliminar uno sin historial, cancelar otro con historial y comprobar que recepción conserva su consulta. Probar otro docente, participante y recepción: no deben recibir el menú de gestión si no corresponde. Probar sin red y verificar que no se anuncie una escritura exitosa.
