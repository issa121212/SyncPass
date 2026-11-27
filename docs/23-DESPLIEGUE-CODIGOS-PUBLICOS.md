# Despliegue de códigos públicos en EC2 · 26-11-2026

## Origen y respaldo

Después de integrar los PR #7 a #18, se actualizó develop local a `0667534`. Su contenido coincidía exactamente con la rama final previamente probada. `:app:check`, la compilación del APK debug/APK de pruebas y `bootJar` completaron correctamente. La suite local conserva 100 pruebas aprobadas y 45,22 % de líneas; no se reejecutaron pruebas instrumentadas durante este despliegue.

Se desplegó el JAR compilado desde ese develop. Su SHA-256 local, subido y finalmente instalado coincidió:

`af1ace3c3646c88400f77ba231b0c0b77b27745046fc432d3f186891586852b7`

El candidato contiene V5/V6 y no contiene utilidades de ensayo. El procedimiento específico queda en `Backend/deploy/ec2/deploy-public-codes.sh`, separado del procedimiento histórico V2 → V4.

El respaldo definitivo se creó con la recepción externa y el backend detenidos, en `/var/backups/syncpass/pre-migrations-20261127T023741-7995`. El nombre utiliza UTC: corresponde a la noche del 26-11 en Santiago. Conserva dump PostgreSQL, JAR anterior, configuración, unidad systemd y checksums con acceso privado. No se publican estos archivos ni datos de filas en Git.

## Operación

El procedimiento exige esquema inicial V4, servicios activos, hash del candidato verificado y ENFORCE explícito antes de interrumpir el servicio. Detiene temporalmente Nginx y syncpass para impedir escrituras externas durante el respaldo y la comparación.

Instala el JAR mediante sustitución de archivo e inicia syncpass. Flyway aplicó V5 y V6 correctamente, manteniendo V1 a V4 válidas. No se modificaron migraciones aplicadas ni se usó repair.

Se compararon todas las filas anteriores mediante cantidad y hash de su representación JSON, excluyendo únicamente la nueva columna codigo_publico. Se incluyeron las operaciones idempotentes y los campos geográficos anteriores. Las comparaciones antes/después coincidieron:

| Tabla | Filas conservadas |
|---|---:|
| usuarios | 3 |
| usuario_roles | 3 |
| proyectos | 4 |
| eventos | 10 |
| inscripciones | 8 |
| asistencias | 8 |
| encuestas | 3 |
| creaciones_operaciones | 2 |

Estas cantidades son las existentes al comenzar este despliegue, no las de los ensayos o despliegues anteriores. V5 asignó códigos válidos y distintos a los 4 proyectos y 10 eventos, con 14 reservas iniciales.

Se verificó que el archivo completo de configuración mantuvo su hash. El proceso Java en ejecución conserva `ATTENDANCE_GEO_MODE=ENFORCE`. No se cambiaron Nginx, API Gateway ni Cognito como configuración; Nginx se inició de nuevo tras completar la validación local.

## Comprobaciones finales

- Servicios syncpass y nginx activos y salud interna UP.
- `GET /api/health` por API Gateway devolvió 200 UP.
- Eventos y capacidades sin credenciales devolvieron 401: no se abrió el acceso público.
- Preflight CORS de `https://syncpassweb.vercel.app` devolvió 200 y el origen esperado.
- Historial Flyway hasta V6 y códigos migrados válidos/únicos.
- Datos anteriores y configuración conservados.

La validación de códigos de producción se hizo mediante consultas agregadas de solo lectura en PostgreSQL. No se usó una sesión real para consultar los DTO desde Android durante esta operación. La web puede seguir ignorando el campo nuevo; su interfaz no fue modificada en este despliegue.

## Recuperación y límites

El procedimiento conserva el respaldo y no restaura automáticamente la base. Si falla después de sustituir el JAR, intenta recuperar el JAR anterior solo si el esquema sigue en V4 o llegó a V6, versiones compatibles con ese artefacto probado. Si encuentra un estado intermedio V5 exige revisar, porque V5 sin V6 no garantiza inserciones del binario anterior. Se intenta recuperar la recepción por Nginx al finalizar el manejo del fallo.

No se provocó una caída intencional de producción para probar ese manejador. La compatibilidad del JAR anterior con V6 sí se ensayó previamente sobre una copia local, como registra el documento 20.

No hay aún una nueva release o tag y main permanece sin integrar estas mejoras. V6 es la versión del esquema del servidor; no cambia por sí misma el número de Android o del artefacto Java.

## Siguiente validación

El APK principal de develop está compilado y preparado. Al terminar el despliegue no había teléfono ni emulador conectados por adb. Falta instalarlo sobre el teléfono conservando datos, iniciar sesión si expiró, refrescar el catálogo y comprobar los códigos, la sincronización y el rechazo físico fuera de zona/aceptación dentro. La cámara y el GPS ya habían sido comprobados por el usuario en la versión anterior; eso no sustituye este recorrido final.

## Intento de actualización del teléfono

El usuario conectó y autorizó posteriormente su Moto g82. La instalación con `adb install -r` fue rechazada con INSTALL_FAILED_UPDATE_INCOMPATIBLE: el certificado de la app instalada difiere del APK construido con la clave debug disponible en este PC. No se desinstaló, no se borraron datos ni se alteraron permisos.

Se verificaron ambos certificados públicos y la configuración de firma de Gradle. La app instalada conserva Android 1.7.0, versionCode 10 y sus permisos de cámara/ubicación. La actualización principal requiere su clave original o una estrategia de instalación separada elegida por el usuario. No se puede conservar una firma simplemente copiando el certificado público; no se obtuvo ni publicó ninguna clave privada.

El usuario eligió esperar la clave original. No se instaló una variante paralela ni se continuó con cambios en el teléfono. La prueba física final queda pendiente hasta disponer de una actualización firmada con la clave correspondiente.
