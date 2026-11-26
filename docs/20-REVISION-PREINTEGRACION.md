# Revisión previa a integración · 26-11-2026

## Alcance y resultado

Se revisaron el filtro Android, el flujo de códigos públicos, las migraciones, sus reportes de pruebas, el estado de Git y la salud del backend desplegado. No se publicó ninguna rama, no se fusionó develop/main y no se desplegaron las migraciones nuevas en EC2. La revisión de un colega sigue siendo independiente de esta comprobación técnica.

La revisión encontró dos problemas operativos y los corrigió localmente antes de proponer el despliegue.

## Correcciones

### Reutilización del despliegue histórico

`deploy-offline-update.sh` correspondía a V2 → V4 y fijaba AUDIT. Reutilizarlo podía deshacer la elección posterior de ENFORCE y sus comparaciones no contemplan las columnas/tablas nuevas de códigos.

Ahora rechaza cualquier esquema inicial distinto de V2 antes de detener servicios, y conserva la configuración geográfica en vez de sobrescribirla. Se comprobó su sintaxis con bash, sin ejecutarlo contra producción. El procedimiento para desplegar V5/V6 debe prepararse específicamente; este script histórico no es ese procedimiento.

### Recuperación de la versión anterior del backend

La V5 original agregaba columnas NOT NULL sin defaults. Se confirmó en PostgreSQL local que un INSERT del contrato anterior, sin codigo_publico, producía SQLSTATE 23502. Esto impedía recuperar el JAR anterior manteniendo la creación de recursos.

Se agregó V6 sin modificar las migraciones ya aplicadas. V6 establece defaults que generan y reservan códigos válidos cuando un INSERT anterior omite la columna. Usa prefijos permitidos, reserva única y hasta 20 intentos ante colisiones. El backend actual continúa enviando su reserva explícita; no se generan dos códigos para una creación.

La tarea `publicCodeCompatibilityRehearsal` verificó V5 → V6, conservación de todas las filas y códigos anteriores, INSERT antiguos de proyecto/evento y rollback que revierte también sus reservas. Una segunda migración no cambió el esquema. Los ensayos históricos se fijaron a sus versiones objetivo para no aplicar inadvertidamente migraciones posteriores.

Además se descargó una copia del JAR público de `/opt/syncpass/backend.jar` a la carpeta de build local, sin descargar configuración ni credenciales de EC2. Su SHA-256 coincidió con el artefacto V4 desplegado. Se ejecutó contra la copia PostgreSQL V6 e identidad ficticia local: arrancó, creó proyecto/evento y editó el proyecto. La API nueva pudo consultar sus códigos, que permanecieron estables. Se repitió la comprobación HTTP local de nombres repetidos, edición e idempotencia sobre V6 y aprobó.

Reportes operativos, excluidos de Git y sin tokens ni coordenadas personales:

- `Backend/build/migration-rehearsal/compatibilidad-codigos.txt`.
- `Backend/build/migration-rehearsal/rollback-binario-v4.txt`.
- `Backend/build/migration-rehearsal/codigos-publicos-api.txt`.

Esta comprobación de recuperación corresponde al JAR V4 exacto probado, no a cualquier versión histórica del sistema. Recuperar ese JAR no quita los defaults ni restaura una copia antigua de los datos; su API simplemente no presenta el campo nuevo.

## Interfaz real en el emulador

Se encontró una sesión de estudiante/recepción activa y se comprobó el selector contra el catálogo real:

| Selección | Resultado observado |
|---|---|
| Todos | 9 eventos |
| Programado | 5 eventos |
| Cancelado | 4 eventos |
| En curso | 0 eventos y mensaje explicativo |

Los proyectos siguen visibles aunque un estado no tenga eventos. Se dejó el filtro en Todos. No se guardaron proyectos, eventos, inscripciones ni asistencias durante esta inspección. No se comprobó físicamente la rotación en esta revisión; la selección restaurada está cubierta por las pruebas unitarias existentes.

La interfaz muestra «Código no disponible» porque EC2 aún está en V4. Esto confirma la tolerancia al backend anterior, no la visualización de códigos reales de producción. La presentación de códigos sí está cubierta por la prueba Compose con datos ficticios y su transporte por las comprobaciones locales HTTP/Room.

## Evidencia de pruebas

Se revisaron los reportes de la implementación: 95 pruebas unitarias Android, 8 instrumentadas y 12 regresiones existentes seleccionadas de backend; cero fallos, errores o pruebas omitidas. No se sumaron los ensayos operativos a esas cantidades ni se reejecutaron suites sin cambios Android para aumentar cifras.

Lint informa 0 errores y 35 advertencias; no se presenta como una ejecución sin advertencias. Los ensayos nuevos de esta revisión corresponden a la compatibilidad V6 y al JAR anterior, no a una repetición de toda la batería. `git diff --check` y la compilación del backend con V6 completaron correctamente.

## Git e infraestructura

Se actualizó la referencia remota. `origin/develop` es ancestro de la rama revisada y estaba 36 commits por detrás al comenzar esta revisión, incluidas las mejoras anteriores. No existen commits remotos divergentes en esa comparación. Esto no reemplaza las políticas ni la aprobación del PR.

Orden de dependencias locales: Hilt → ubicación de eventos → geolocalización de asistencia → creación offline → ensayo de migraciones → integración Android → correcciones de validación/sesión/cámara → filtro por estado → códigos públicos y estas correcciones. No se debe integrar solamente el último bloque omitiendo sus dependencias.

No se verificó el listado actual de PR: la API pública de GitHub devolvió 403 por límite de solicitudes. La comprobación de referencias mediante git fetch sí se completó. No se enviaron comentarios ni aprobaciones en nombre del colega.

EC2 respondió con servicio activo, salud UP y `ATTENDANCE_GEO_MODE=ENFORCE`. El teléfono no estaba conectado: solo estaba disponible el emulador. Los servidores y PostgreSQL locales de ensayo se detuvieron al finalizar.

## Pendientes que no se presentan como aprobados

- Revisión del colega y organización de PR hacia develop, considerando todas las dependencias.
- Respaldo actualizado y despliegue específico de V5/V6 en EC2, preservando ENFORCE.
- Actualización del teléfono y revisión visual de esta nueva versión en hardware real.
- Prueba física de rechazo fuera del radio y aceptación dentro, con un evento y una cuenta de prueba.
- Confirmación de códigos tras sincronizar desde el teléfono con el backend actualizado.
- Entrega a main y tag después de la integración aprobada.

El filtro y los códigos visibles en SyncPassWeb siguen fuera de este cambio; la compatibilidad del contrato compartido no implica que su interfaz los implemente.
