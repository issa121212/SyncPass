# Actualización del backend EC2

El 26-11-2026 se desplegó el backend local de esta entrega en EC2, con autorización del equipo. Se conservan PostgreSQL, Nginx, API Gateway, Cognito y la configuración existente. No se publicó ninguna rama en GitHub como parte de esta operación.

## Preparación y respaldo

Se compiló `Backend` mediante `bootJar`. Se comprobó que el JAR contiene las migraciones nuevas y excluye las herramientas de ensayo. El SHA-256 local y remoto coincidió:

`14c9ebc2e4800d38ef842f37db1414ac48db60cb874e5d927d45301828b63cb1`

Con el servicio detenido se creó un respaldo privado de PostgreSQL, JAR anterior, configuración y unidad systemd en `/var/backups/syncpass/pre-migrations-20261126T154531-2977`. El directorio es accesible solo por root. No se copian sus secretos al repositorio.

El procedimiento ejecutado se conserva en `Backend/deploy/ec2/deploy-offline-update.sh`. Sus rutas de archivos preparados corresponden a esta operación; no debe ejecutarse sin preparar y verificar nuevamente el candidato. El respaldo y los ensayos previos se describen en el documento 13.

## Resultado

- Flyway aplicó V3 y V4 correctamente; V1 y V2 permanecen válidas.
- V3 agrega evidencia geográfica opcional a las asistencias; V4 incorpora el registro de creaciones idempotentes.
- Se configuró explícitamente `ATTENDANCE_GEO_MODE=AUDIT` para preservar compatibilidad con clientes anteriores. No se activó ENFORCE.
- La comprobación local de salud respondió correctamente y los servicios syncpass y nginx quedaron activos.
- Se compararon cantidades y hashes de todas las filas de negocio antes y después, excluyendo únicamente los campos geográficos nuevos. Los datos originales coincidieron: 3 usuarios, 3 relaciones usuario/rol, 5 proyectos, 8 eventos, 8 inscripciones, 8 asistencias y 3 encuestas.
- API Gateway devolvió 401 al consultar eventos y capacidades sin credenciales, manteniendo la protección. El preflight CORS de la web publicada devolvió 200 con el origen esperado.

API Gateway no publica `/actuator/health`: esa comprobación se ejecutó dentro de EC2. Un 404 del gateway para esa ruta no significa que el servicio esté caído.

## Comprobación posterior pendiente

La creación guardada en el teléfono permanece en su cola local hasta sincronizar con la sesión correspondiente. Falta confirmar desde el teléfono que se envía y desaparece el estado pendiente, y consultar el evento desde la web. Los controles sin credenciales no sustituyen esta comprobación autenticada.

## Recuperación

El respaldo anterior se conserva en EC2. Ante un fallo se debe revisar el servicio y las migraciones antes de sustituir el JAR. No se restaura automáticamente la base, ya que eso podría perder nuevas escrituras posteriores al despliegue. Las migraciones aplicadas son aditivas; no se eliminan tablas ni filas de negocio para revertir la aplicación.
