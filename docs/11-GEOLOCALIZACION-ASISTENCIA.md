# Validación geográfica de asistencia

## Alcance y flujo

La ubicación corresponde al teléfono de quien recibe al participante y escanea su QR. Se mantienen los permisos existentes: estudiante voluntario de recepción, docente responsable o administrador. El participante presenta su QR y no se marca a sí mismo.

En un evento con zona configurada, Android detiene la cámara después de detectar el QR, solicita permiso de ubicación y obtiene una posición reciente. Conserva el QR para permitir reintentar sin volver a leerlo. Si falla el proveedor, se deniega el permiso o la posición no cumple las reglas, muestra el motivo y no encola una asistencia inválida. El acceso a los ajustes permite recuperar permisos denegados permanentemente o habilitar ubicación precisa.

Android guarda la captura completa en Room antes de intentar enviarla. Confirmada significa que la API aceptó la operación; pendiente significa que falta la confirmación remota; rechazada conserva el motivo y no se reenvía automáticamente. Capturar otra vez es una nueva operación; sincronizar una captura pendiente conserva su UUID, coordenadas, precisión y horas originales.

Los eventos con ambas coordenadas vacías mantienen el flujo anterior y no exigen ubicación. Una zona incompleta se considera inválida, no un evento sin zona. La caché puede estar desactualizada: el backend toma la decisión definitiva con los datos actuales del evento.

## Reglas

Para una zona definida se exige:

- Latitud finita entre -90 y 90; longitud finita entre -180 y 180.
- Radio del evento entre 1 y 50000 metros.
- Precisión positiva, como máximo 100 metros y como máximo la mitad del radio.
- Posición tomada como máximo dos minutos antes de la captura y nunca después de ella.
- Distancia de Haversine más precisión menor o igual al radio. Se exige que todo el margen de incertidumbre esté dentro de la zona; una precisión peor no amplía el recinto.

El backend acepta una captura sincronizada hasta 24 horas después de su hora de captura y tolera hasta 30 segundos de adelanto respecto del servidor. La antigüedad de la posición se compara con la captura original, por lo que sincronizar más tarde no convierte automáticamente una posición válida en antigua. Un evento cancelado o finalizado sigue rechazando nuevas asistencias conforme a la regla existente.

Los reintentos de una asistencia ya confirmada conservan la respuesta idempotente antes de volver a aplicar la regla temporal. Las validaciones no dependen de la fecha programada del evento: se mantiene el control por su estado actual.

## Contrato y persistencia

`marcadoEn` es la hora de captura del QR, registrada al obtener una posición aceptable y antes de guardar; `ubicacionEn` es la hora de la posición proporcionada por Android. Ambos se envían como instantes ISO 8601 UTC. Se mantienen `latitud`, `longitud`, `precisionMetros` y `clientUuid`.

Se incorpora `ubicacionEn` opcional a `AsistenciaRequest` y `AsistenciaSyncItem`. El endpoint directo y la sincronización por lotes pasan por `EventoService.escanear`, que consulta `AttendanceLocationPolicy` antes de modificar inscripciones o asistencias. Se conservan las comprobaciones de rol, participante, evento, cupos y duplicados.

Room pasa de versión 3 a 4 mediante `ALTER TABLE pendientes ADD COLUMN ubicacionEn TEXT`. No se eliminan tablas, datos ni pendientes antiguos. `RoomAsistenciasRepository` se encarga de persistir la captura y consultar su resultado; `SyncPassRepository` conserva la coordinación de la sincronización. Se utiliza la base separada por cuenta existente.

Flyway agrega `V3__attendance_location.sql` sin modificar V1 o V2. Añade a `asistencias` tres columnas opcionales: `ubicacion_en`, `validacion_geografica` y `motivo_geografico`. Las asistencias históricas conservan sus valores y quedan con estos campos en null; eso significa sin evaluación histórica, no validación aprobada.

## Activación compatible

`ATTENDANCE_GEO_MODE` admite dos valores:

| Modo | Comportamiento |
|---|---|
| `AUDIT` (predeterminado) | Evalúa y registra el resultado, pero conserva la aceptación de clientes antiguos cuando falla la comprobación geográfica. No garantiza geocerca obligatoria. |
| `ENFORCE` | En eventos con zona rechaza las capturas que incumplan las reglas mediante HTTP 422. Los eventos sin zona mantienen la compatibilidad. |

En auditoría se almacena `VALIDADA`, `AUDITORIA_RECHAZADA` o `SIN_ZONA`, junto con el código del motivo cuando corresponda. Android traduce únicamente códigos geográficos conocidos a mensajes comprensibles. Los rechazos permanentes permanecen visibles en la cola rechazada.

Después de desplegar y verificar el backend, pueden revisarse los resultados sin modificar datos:

```sql
SELECT validacion_geografica, motivo_geografico, COUNT(*)
FROM asistencias
GROUP BY validacion_geografica, motivo_geografico;
```

La web actual y versiones anteriores de Android no envían necesariamente `ubicacionEn`. Por eso no debe activarse `ENFORCE` hasta actualizar y verificar todos los clientes que escanean eventos con zona. Los pendientes antiguos tampoco deben recibir una hora de ubicación inventada: se conservan para revisión, y en modo obligatorio pueden ser rechazados con `GEO_DATOS_AUSENTES`.

Orden previsto: respaldar PostgreSQL y configuración, ensayar V3 en una copia, desplegar backend en `AUDIT`, actualizar Android, adaptar/verificar la recepción web, revisar resultados de auditoría y activar `ENFORCE` después de aprobar la integración. Volver a `AUDIT` es la recuperación de configuración; no revierte automáticamente asistencias escritas ni requiere eliminar las nuevas columnas. Este desarrollo no despliega ni modifica EC2.

## Pruebas y límites

Las pruebas JUnit de Android utilizan reloj fijo, ubicación falsa, repositorio falso y una fuente local en memoria. Verifican la zona y su incertidumbre, límite temporal, permiso denegado, doble detección, cancelación, compatibilidad sin zona, estados y reenvío exacto tras una respuesta perdida. También verifican la traducción de un rechazo geográfico y que una operación rechazada no se vuelva a enviar.

Comprobación local del 25-11-2026: 71 pruebas unitarias aprobadas, sin fallos ni errores. Aplicación y APK de pruebas compilados; lint sin errores. La cobertura unitaria de AGP registra 767 líneas cubiertas de 4170 (18,39 %), con el mismo alcance que incluye interfaz y código generado. La meta de 30 % sigue pendiente; no se modificaron las exclusiones. Comandos reproducibles desde `Android/`:

```text
.\gradlew.bat :app:assembleDebug :app:createDebugUnitTestCoverageReport :app:lintDebug :app:assembleDebugAndroidTest
```

El reporte se genera en `Android/app/build/reports/coverage/test/debug/index.html`. Desde `Backend/`, `gradlew.bat compileJava compileTestJava` compiló las fuentes y los tests existentes; no los ejecutó.

La compilación del backend comprueba compatibilidad de fuentes. No se añaden pruebas del backend en esta etapa centrada en Android. Quedan para integración: ejecutar las migraciones sobre Room y PostgreSQL reales, comprobar sensores y permisos, contrastar ambos modos del servidor y verificar Android/web contra la API real. No se afirma que estas comprobaciones se hayan ejecutado.

Las coordenadas y las horas son declaraciones del cliente: esta validación no certifica presencia física ni impide GPS simulado, manipulación del reloj o copia del QR. La ubicación almacenada corresponde al receptor y no prueba la posición individual del participante. No se incorporan nuevas librerías en esta feature.

La rama `feature/geolocalizacion-asistencia` depende de las features locales de Hilt y ubicación. Se integrarán en ese orden hacia `develop`. Los diagramas C4 se actualizarán al finalizar las mejoras restantes.
