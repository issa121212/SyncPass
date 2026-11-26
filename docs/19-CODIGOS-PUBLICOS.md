# Códigos públicos de proyectos y eventos

## Comportamiento

Cada proyecto recibe un código como `PRY-8C36F1A420DB` y cada evento uno como `EVT-B49E27D18A63`. Los doce caracteres hexadecimales se obtienen de UUID aleatorios, no de los ID internos ni de información personal. Los nombres pueden repetirse; cada recurso recibe su propio código.

El ID de PostgreSQL sigue siendo la clave interna y las operaciones de la API continúan utilizándolo. La interfaz Android presenta el código público como información secundaria en las tarjetas, detalles y selección de proyecto, sin reemplazar fecha, responsable o recinto. Los QR conservan exactamente su formato y validación anteriores. Ocultar un ID en la interfaz no equivale a retirarlo del contrato HTTP ni reemplaza la autorización.

## Generación y unicidad

`PublicCodeAllocator` genera un candidato y llama a `CodigoPublicoRepository.reservar`. PostgreSQL reserva el valor mediante `INSERT ... ON CONFLICT DO NOTHING` en `codigos_publicos`. Si hay colisión, la transacción sigue siendo utilizable y el generador prueba otro candidato; limita el proceso a 20 intentos y revierte la creación si no puede reservar uno.

La reserva, la creación y el registro idempotente pertenecen a la misma transacción. Las restricciones UNIQUE de proyectos/eventos y sus claves foráneas a las reservas refuerzan la integridad. Las reservas confirmadas no se eliminan al borrar recursos: un código público no vuelve a asignarse.

Las solicitudes no incorporan un campo editable de código. Solo las respuestas añaden `codigoPublico`. La asignación sucede al crear y el campo JPA es `updatable=false`; editar o cancelar conserva el código. Un reintento con el mismo UUID recupera el mismo recurso y su código, sin reservar uno nuevo.

## Migraciones y clientes

- Flyway V5 agrega las reservas y las columnas, asigna códigos aleatorios a los registros existentes con reintento de colisiones, y luego establece NOT NULL, UNIQUE y claves foráneas. No modifica las migraciones ya aplicadas ni los datos anteriores.
- Flyway V6 agrega generación por defecto para INSERT de un backend anterior, permitiendo recuperar el JAR V4 probado sin perder la capacidad de crear recursos. No modifica V5.
- Room pasa de V5 a V6 con dos columnas TEXT opcionales. No destruye tablas, caché, sesiones ni colas. Las cadenas desde V1/V3 se completan con la migración nueva.
- DTO, entidades, modelos y mappers Android conservan el código de extremo a extremo.
- Una creación sin conexión no inventa un código: muestra «Código pendiente de sincronización» hasta recibir la respuesta del servidor. Una creación rechazada sin código muestra «Código no asignado».
- Un registro confirmado recibido de un backend anterior puede tener código null: muestra «Código no disponible». Al actualizar el backend y refrescar el catálogo recibirá el código migrado.
- Los clientes anteriores pueden ignorar el campo nuevo. No se modifica el repositorio web; este contrato permite que lo incorpore después sin romper las consultas existentes.

## Validación ejecutada

Se aprobaron 95 pruebas unitarias Android. Las cuatro pruebas nuevas comprueban filtrado/estado restaurado y lectura/mapeo de códigos, con dependencias falsas. Se ampliaron las pruebas existentes de creación offline para comprobar ausencia de código local, recepción del código y estabilidad ante respuesta perdida, en vez de duplicarlas.

Se aprobaron ocho pruebas instrumentadas en Pixel 10 emulado, mediante la instalación `.validation` separada:

- Migración explícita V5 → V6 conservando catálogo, cola de creación y evidencia de asistencia; persistencia del código tras reabrir.
- Migración desde V3 y catálogo desde V1; CRUD local de recintos.
- Tres escenarios HTTP/Room contra el servidor local: creación offline padre/hijo con respuesta perdida y código persistido, concurrencia idempotente y regresión de geocerca ENFORCE/permisos.
- Uso visible del selector y del código público en Compose sin mostrar el ID interno.

El ensayo `publicCodesRehearsal` ejecutó V4 → V5 en la copia PostgreSQL local previamente preparada. Comparó todas las filas y campos anteriores, incluidas operaciones idempotentes y QR. Verificó formato, reservas y unicidad, rechazo SQLSTATE 23505, recuperación de una reserva repetida y segunda migración sin cambios. El reporte está en `Backend/build/migration-rehearsal/codigos-publicos.txt` y no contiene datos personales.

En la API local se comprobó que proyectos y eventos homónimos reciben códigos distintos, que un código enviado por el cliente no controla la respuesta y que editar conserva el código y el QR. Dos solicitudes concurrentes con el mismo UUID recuperaron el mismo ID y código. Esto complementa las pruebas Android; no se presenta la suite Android como prueba de las restricciones de PostgreSQL. El procedimiento HTTP se conserva en `Backend/deploy/rehearsal/check-public-codes-api.py`; se ejecuta desde Backend con el servidor `androidRehearsalServer` activo y utiliza únicamente sus direcciones localhost e identidades ficticias.

También pasaron las 12 regresiones existentes seleccionadas de backend (CRUD de proyectos y asistencia). El perfil local H2 y el entorno H2 de esas pruebas usan modo PostgreSQL para admitir la sentencia de reserva, sin sustituir el ensayo sobre PostgreSQL real.

## Revisión e integración

La rama `feature/codigos-publicos` depende de `feature/filtro-estado-eventos`, que a su vez depende de la cadena local de mejoras anterior. Deben integrarse esas dependencias en develop antes de preparar la entrega a main. No se reescribe la historia ni se publica ninguna rama automáticamente.

No se despliegan V5/V6 en EC2 ni se actualiza el teléfono como parte de esta implementación. EC2 permanece con el backend anterior de esta sesión, Flyway V4 y geolocalización ENFORCE. El respaldo y despliegue de V5/V6 requieren una operación posterior, tras revisar el resultado local. El APK principal actualizado se instaló en el emulador conservando sus datos. La sesión estaba cerrada al inspeccionarlo: la revisión interactiva con Cognito requiere login manual. La interfaz con datos ficticios se comprobó mediante la prueba Compose. La validación física de esta nueva interfaz y una prueba de carga masiva de códigos no se han ejecutado.

## Revisión posterior

La revisión interactiva, las correcciones del despliegue histórico y la comprobación de recuperación del JAR anterior con V6 se describen en [Revisión previa a integración](20-REVISION-PREINTEGRACION.md). La inspección posterior encontró una sesión activa de estudiante en el emulador y permitió verificar el filtro con el catálogo real; las referencias anteriores a sesión cerrada describen el momento de instalación.
