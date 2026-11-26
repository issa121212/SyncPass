# Respaldo y ensayo de migraciones

## Resultado local del 25-11-2026

Se ejecutó un ensayo operativo sobre PostgreSQL 16.16 aislado en Docker, con Flyway 12.4.0 y datos ficticios. No utiliza contraseñas ni datos de producción. La tarea es optativa y su código no se incorpora al JAR del backend ni cambia la suite unitaria de Android.

Se creó un esquema hasta V2 y se cargaron dos usuarios, dos roles y un registro de proyecto, evento, inscripción, asistencia y encuesta. Se generó un respaldo en formato custom, se verificó su catálogo y se restauró en otra base. Flyway aplicó V3 y V4 únicamente sobre esa copia.

Comprobaciones aprobadas:

- Todos los campos y filas originales se conservaron después de restaurar y migrar, no solo sus cantidades. Se compararon representaciones JSON de las filas, excluyendo únicamente las tres columnas geográficas nuevas de asistencia.
- Los registros relacionados se restauraron con sus restricciones referenciales.
- La asistencia histórica conservó en null los campos nuevos de evidencia geográfica.
- La restricción por usuario/tipo/UUID rechazó un duplicado con SQLSTATE 23505.
- Flyway validó las cuatro migraciones y sus checksums; una segunda ejecución aplicó cero migraciones.
- La base fuente permaneció en V2 mientras la copia pasó a V4.

Estos resultados no demuestran todavía compatibilidad con la base real de EC2 ni verifican las reglas HTTP, el bloqueo concurrente del servicio o los sensores Android.

## Reproducir el ensayo ficticio

Requiere Docker Desktop con motor Linux y el JDK del backend. La tarea está limitada a un contenedor y dos bases locales con nombres fijos. Aborta si la fuente ya contiene tablas; no sobrescribe una base previa.

```powershell
docker run --detach --name syncpass-migration-rehearsal-20261125 --publish 127.0.0.1:55432:5432 --env POSTGRES_USER=rehearsal --env POSTGRES_PASSWORD=local-rehearsal-only --env POSTGRES_DB=syncpass_rehearsal postgres:16
docker exec syncpass-migration-rehearsal-20261125 pg_isready -U rehearsal -d syncpass_rehearsal
cd Backend
$env:SYNCPASS_MIGRATION_REHEARSAL='YES_LOCAL_SYNTHETIC_ONLY'
.\gradlew.bat migrationRehearsal --console=plain
```

El password mostrado es exclusivamente el valor público del contenedor ficticio. No se debe reutilizar para servicios reales. El puerto se publica solo en 127.0.0.1.

Artefactos locales, excluidos por `build/`:

- `Backend/build/migration-rehearsal/FICTICIO-syncpass-v2.dump`.
- `Backend/build/migration-rehearsal/resultado.txt`.

El respaldo ficticio de esta ejecución tiene SHA-256 `0d3f32b000686d61c8c717a308270738d7787762d64f8b23d5a41f828d362759`. No es una copia de seguridad de EC2.

Al terminar puede detenerse únicamente el contenedor de ensayo mediante `docker stop syncpass-migration-rehearsal-20261125`. Para repetir desde cero, conservar primero los artefactos deseados y retirar únicamente ese contenedor ficticio antes de volver a crearlo. La tarea no elimina contenedores ni bases automáticamente.

## Respaldo real en EC2

`Backend/deploy/ec2/backup-before-migrations.sh` prepara un respaldo privado en `/var/backups/syncpass/`. Está limitado a la base `syncpass`, el JAR `/opt/syncpass/backend.jar` y la configuración `/etc/syncpass/backend.env`. Debe ejecutarse con sudo después de confirmar que se trata de la instancia correcta.

El script genera un dump custom consistente con `pg_dump`, valida que pueda leerse su catálogo y conserva JAR, configuración, unidad systemd y checksums. Los directorios son de root con permisos 0700 y los archivos privados no se muestran en consola. No aplica migraciones, no restaura sobre producción y no reinicia servicios. Que `pg_restore --list` funcione no demuestra por sí solo una restauración completa.

Las copias de configuración contienen secretos y no deben subirse a Git ni compartirse en capturas.

## Resultado con copia real de EC2

También se completó el ensayo sobre una copia del respaldo real. La instancia utiliza PostgreSQL 16.15 y su historial de Flyway registraba V1 y V2 correctamente aplicadas. Se creó el respaldo privado en `/var/backups/syncpass/pre-migrations-20261126T020229-2321/`; el nombre usa fecha UTC. Incluye dump, JAR anterior, configuración y checksums. El directorio pertenece a root con modo 0700 y los archivos privados tienen modo 0600.

Se descargó únicamente el dump a una carpeta local fuera del repositorio, protegida mediante ACL del usuario. Se verificó que su SHA-256 coincide con el archivo remoto: `5c08425d91d67baf7f9db9567d0fe7075b554cd77683818af451110bb3905e83`. No se publican el dump ni el contenido de las filas.

La restauración local usó `--no-owner --no-privileges` para cambiar propietarios al rol aislado del ensayo. La contraseña pública del contenedor ficticio fue sustituida por una contraseña privada antes de continuar con la copia real. El puerto permanece limitado a 127.0.0.1.

Resultados:

- La copia real partió de V2 y aplicó V3/V4 con validación de checksums.
- Se conservaron todos los campos anteriores de 3 usuarios, 3 asignaciones de rol, 5 proyectos, 8 eventos, 8 inscripciones, 8 asistencias y 3 encuestas.
- No se inventaron datos geográficos para las asistencias históricas.
- Una segunda ejecución aplicó cero migraciones.
- El nuevo JAR inició contra la copia migrada con `ddl-auto=validate`: Hibernate aceptó el esquema y `/api/health` respondió HTTP 200 UP. El proceso local se detuvo al terminar.
- La fuente de producción se mantuvo en V2 y no se reinició su servicio.

Los informes se guardan en `Backend/build/migration-rehearsal/resultado-copia-real.txt` y `application-result.txt`; el log de arranque también permanece fuera de Git. El contenedor de ensayo se detuvo al finalizar y conserva la copia para inspección autorizada.

La utilidad admite el modo `SYNCPASS_MIGRATION_REHEARSAL=YES_LOCAL_PRODUCTION_COPY`, exige `SYNCPASS_REAL_BACKUP` y una contraseña local privada en `SYNCPASS_REHEARSAL_PASSWORD` de al menos 24 caracteres. Nunca admite una URL remota. Si una restauración ya se completó y un ensayo se interrumpió antes de migrar, el modo explícito `SYNCPASS_RESUME_REHEARSAL=YES_EXISTING_LOCAL_V2_COPY` permite continuar únicamente si la copia sigue en V2. No elimina ni sobrescribe bases.

Se corrigió una comprobación inicial del ensayo que invocaba `validate()` antes de migrar y trataba V3/V4 pendientes como error. Ahora `migrate()` valida primero las migraciones aplicadas, admite las pendientes y luego se ejecuta `validate()` sobre V4. No se usó `repair`, no se ignoraron checksums ni se modificaron migraciones aplicadas.

Antes del despliegue real siguen pendientes las pruebas de flujos autenticados, Android/Room y compatibilidad de clientes. El ensayo no activa ENFORCE ni demuestra todavía el comportamiento concurrente de la API de creación.

La recuperación debe mantener juntos el dump, el JAR, la configuración y las operaciones idempotentes. Restaurar una base antigua sobre una aplicación que siguió aceptando escrituras puede perder esas escrituras; debe planificarse una ventana y la reconciliación, no sobrescribir producción automáticamente.
