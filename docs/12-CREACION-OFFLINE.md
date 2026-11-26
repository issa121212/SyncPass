# Creación offline de proyectos y eventos

## Comportamiento

Una cuenta docente o administradora con perfil descargado puede crear proyectos y eventos sin conexión. Android valida los campos y guarda, en una transacción de Room, la solicitud con UUID y el registro visible del catálogo. Una cuenta que nunca descargó su perfil necesita conectarse primero; esta función no sustituye el login de Cognito.

Cada creación obtiene un ID local negativo. El evento puede seleccionar un proyecto pendiente de la misma cuenta. La aplicación muestra pendiente o rechazado con su motivo, y el encabezado diferencia pendientes de rechazos. Hasta la confirmación no se habilitan escaneo, inscripción ni edición remota para ese evento. Las creaciones locales no proporcionan un QR de evento confirmado.

Guardar intenta sincronizar. Si falla la conexión o el servicio, la solicitud permanece guardada y puede enviarse con el botón Sincronizar o con el trabajo existente de WorkManager. El refresco del catálogo conserva los registros locales negativos. Los mensajes de guardado no afirman que una solicitud pendiente ya exista en el servidor.

## Orden, reintentos y dependencias

`OfflineCreations` procesa proyectos antes que eventos. Tras confirmar un proyecto, una transacción local agrega el registro remoto, reasigna sus eventos y solicitudes dependientes y sustituye la referencia temporal. Los eventos se envían con el ID remoto del proyecto. La confirmación de un evento reemplaza su registro local por la respuesta remota.

Las confirmaciones conservan la correspondencia entre ID local y remoto. Esto también permite resolver un formulario abierto con el ID temporal de un proyecto que se confirmó mientras se completaba el evento.

Un fallo temporal del proyecto mantiene sus eventos esperando. Un rechazo definitivo del proyecto conserva los datos y rechaza sus hijos sin enviarlos. Una creación rechazada no bloquea otras independientes. Los errores de sesión conservan las solicitudes y exigen recuperar la autenticación. La cancelación de coroutines se propaga; no se convierte en rechazo.

La sincronización manual y `SyncWorker` utilizan el mismo repositorio por cuenta y su Mutex. Las creaciones se procesan antes que la cola existente de inscripciones, asistencias y encuestas. Un proceso interrumpido puede retomar la solicitud almacenada; no se obtiene otro UUID por un simple reintento.

## Contrato compatible e idempotencia

Se agrega `clientUuid` opcional a los cuerpos de creación de proyecto y evento. Los clientes anteriores que omiten el campo conservan su funcionamiento, pero no reciben protección nueva contra reintentos sin UUID.

Antes de enviar creaciones pendientes, Android consulta `GET /api/sync/capabilities`, disponible para DOCENTE y ADMIN. La respuesta debe incluir `creacionIdempotente: true`. Si la ruta no existe o el backend todavía no anuncia esa capacidad, las creaciones quedan pendientes y no se realiza ningún POST de creación. Así se evita que un backend anterior ignore el UUID y cree duplicados durante un reintento.

`CreationIdempotency` conserva el recurso por usuario, tipo de creación y UUID. Calcula una huella SHA-256 de los datos tipados, diferenciando null y valores mediante longitudes explícitas. El mismo UUID con los mismos datos devuelve el mismo recurso; con otros datos devuelve HTTP 409. La identidad está vinculada al usuario autenticado, no a un identificador suministrado por Android.

La creación del recurso y de su operación se realiza en la misma transacción del servidor. Un bloqueo pesimista de la fila del usuario serializa las creaciones concurrentes de esa cuenta, incluso con varias instancias del backend. Una restricción única protege la identidad persistida de la operación. Al devolver un recurso existente se vuelven a comprobar los permisos actuales del recurso.

Si el servidor guardó y se perdió la respuesta, Android reenvía el mismo UUID y cuerpo. Si llegó la respuesta pero falló la confirmación local, conserva la operación como pendiente para recuperar la correspondencia. Si el recurso fue eliminado posteriormente, reutilizar ese UUID no recrea silenciosamente el registro.

## Capas y migraciones

- View y ViewModel muestran los estados y bloquean envíos simultáneos de creación.
- `ProyectoRules` y las reglas de evento validan campos; un ID negativo de proyecto solo es utilizable cuando existe un proyecto local autorizado.
- `OfflineCreations` coordina persistencia, orden de envío, errores y confirmaciones.
- `CreationStore` define la frontera local sustituible por una fuente falsa; `RoomCreationStore` usa las transacciones de `SyncPassDao`.
- `SyncPassRepository` mantiene la coordinación con el resto de operaciones y la exclusión mutua por cuenta.
- La fuente remota utiliza Retrofit y los DTO existentes ampliados con UUID y capacidades.
- Los servicios del backend mantienen autorización y reglas de negocio; `CreationIdempotency` concentra la deduplicación.

Room pasa de versión 4 a 5 con una migración explícita. Agrega `syncState` y `syncError` a proyectos y eventos, inicializa el estado de los registros existentes como CONFIRMADO y crea `creaciones` con índice único de UUID. Se conservan todas las migraciones anteriores y la separación de bases por cuenta. No se usa migración destructiva.

Flyway V4 crea `creaciones_operaciones`; no modifica las migraciones V1 a V3 ni elimina datos de negocio. La tabla conserva usuario, tipo, UUID, huella e ID del recurso. No se agrega una clave foránea al recurso porque los IDs pueden corresponder a dos tablas y el registro de idempotencia debe sobrevivir a una eliminación.

## Rechazos y límites

Los datos rechazados permanecen visibles para revisar el motivo y preparar una solicitud corregida. En esta versión las creaciones locales son de solo lectura: no se modifica un cuerpo pendiente cuyo resultado remoto podría ser incierto. Editar, eliminar o cancelar registros confirmados sigue requiriendo API. No se implementa un editor de reparación de la cola ni una eliminación automática de rechazos.

La lista global de operaciones rechazadas de asistencia/inscripción/encuestas conserva su alcance; los rechazos de creación aparecen en sus tarjetas y en el contador del encabezado. Los registros confirmados de correspondencia se conservan para resolver referencias y reintentos; su limpieza futura requerirá una política explícita.

La app no garantiza que una creación offline vaya a ser aceptada: el backend vuelve a comprobar permisos y datos actuales, y el proyecto remoto de un evento puede haber cambiado o sido eliminado. Desinstalar o borrar datos puede perder solicitudes exclusivamente locales. La cola no es un respaldo.

## Validación y despliegue

Las pruebas unitarias de Android emplean una fuente local en memoria y una API falsa. Cubren la secuencia padre/hijo, IDs remotos, pérdida de respuesta, fallo de confirmación local, backend anterior, permisos, rechazo del padre, independencia de otras creaciones, cancelación y sincronizaciones concurrentes. La prueba de reanudación conserva la fuente en memoria y reconstruye el coordinador; no demuestra por sí sola persistencia física tras cerrar el proceso.

Resultado local del 25-11-2026: 88 pruebas unitarias aprobadas, sin fallos ni errores. La app, el APK de pruebas y las fuentes/tests del backend compilan. Lint: cero errores, 32 advertencias y 2 sugerencias. El APK de pruebas se compiló; las pruebas instrumentadas no se ejecutaron.

Cobertura unitaria global: 940 líneas cubiertas de 4825 (19,48 %). Se mantuvo el alcance de AGP, incluidas UI y fuentes generadas. La revisión detectó 1685 líneas sin cubrir en el paquete local, que incluye código generado de Room; también quedan pantallas Compose sin ejercitar. Se ampliaron pruebas de dependencias, errores, concurrencia, estado de guardado y validación de proyectos. La meta de 30 % no está cumplida; no se sustituyó por una cifra filtrada ni se probaron constructores/getters para elevarla. Las fuentes locales falsas comprueban coordinación, pero no ejecutan las implementaciones Room ni la interfaz Android real. Esos comportamientos requieren la fase de integración o un entorno de UI adecuado.

Desde `Android/`:

```text
.\gradlew.bat :app:assembleDebug :app:createDebugUnitTestCoverageReport :app:lintDebug :app:assembleDebugAndroidTest
```

Reporte: `Android/app/build/reports/coverage/test/debug/index.html`. Después de agregar las últimas pruebas se ejecutó nuevamente `:app:createDebugUnitTestCoverageReport`; no hubo cambios de producción posteriores. Desde `Backend/`, `gradlew.bat compileJava compileTestJava` comprobó compilación sin ejecutar pruebas ni acceder a producción.

Quedan para integración la migración real de Room, cierre/reapertura del proceso, transacciones y bloqueos de PostgreSQL, dos solicitudes HTTP concurrentes con el mismo UUID y el recorrido Android/web contra el backend desplegado. En esta etapa no se añaden pruebas del backend; se compilan sus fuentes y tests existentes.

Orden de despliegue: respaldar base y configuración; ensayar V3 y V4 sobre una copia de PostgreSQL y verificar conteos/relaciones; desplegar el backend compatible conservando geografía en AUDIT; comprobar capacidades; instalar Android y ensayar Room 3→4→5 con datos/pendientes anteriores. Verificar un proyecto offline, su evento dependiente y un reintento tras respuesta perdida.

No retirar ni degradar el contrato idempotente mientras existan clientes nuevos enviando creaciones. Una recuperación debe conservar la tabla y los registros de operaciones junto a los recursos; restaurar solo una parte puede perder la protección contra duplicados. No se aplicaron migraciones ni se desplegaron cambios en EC2 durante este desarrollo.
