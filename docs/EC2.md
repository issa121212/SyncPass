# Backend en EC2 para Learner Lab

La configuracion economica usa una unica instancia Amazon Linux 2023 x86_64/t3.micro, volumen gp3 de 8 GiB, creditos CPU Standard y PostgreSQL en esa misma maquina. No crea RDS, ALB ni NAT Gateway. No ofrece alta disponibilidad. Detener la instancia suspende el computo; el volumen EBS sigue existiendo y puede generar cargos. No asumir gratuidad en Learner Lab.

## Preparacion aplicada

Paquetes oficiales: java-21-amazon-corretto-headless, postgresql16-server y postgresql16. El script Backend/deploy/ec2/setup.py prepara una instalacion inicial despues de instalar esos paquetes con dnf. Requiere sudo y recibe --issuer, --pool y --client con identificadores publicos de Cognito. Si encuentra configuracion o un rol SyncPass existente, se detiene para no reemplazar credenciales.

PostgreSQL escucha exclusivamente en localhost, con base/usuario dedicados syncpass y autenticacion SCRAM para la aplicacion. El password se genera en el servidor y se guarda en /etc/syncpass/backend.env, accesible solo por root, fuera de Git. El backend usa perfil prod y un usuario Linux de servicio sin login. No utiliza el .env local del PC ni H2.

El JAR compilado en el PC se instala como /opt/syncpass/backend.jar. El servicio systemd syncpass esta habilitado para arrancar con la instancia, reinicia ante fallo y limita la memoria. Java tiene heap maximo de 256 MiB y la maquina dispone de un archivo swap local de 1 GiB dentro del disco existente. PostgreSQL usa shared_buffers=64MB y max_connections=30; Hikari limita el pool a cinco conexiones. Estas cifras son para una demostracion de poco trafico, no una garantia de capacidad.

## Comprobar desde la sesion SSH

```bash
sudo systemctl status syncpass postgresql --no-pager
curl -fsS http://127.0.0.1:8080/api/health
curl -fsS http://127.0.0.1:8080/actuator/health
sudo journalctl -u syncpass -n 50 --no-pager
```

No compartir el contenido de backend.env ni de la clave SSH. El log y el estado de los servicios bastan para el diagnostico habitual.

## Actualizar el backend

Compilar Backend con gradlew bootJar en el PC y copiar el JAR mediante scp a /tmp/syncpass-backend.jar de la instancia. Usar la clave SSH local, fuera del repositorio. En EC2:

```bash
sudo install -o root -g root -m 0644 /tmp/syncpass-backend.jar /opt/syncpass/backend.jar
sudo systemctl restart syncpass
```

Comprobar health y logs despues de reiniciar. Flyway aplica las migraciones nuevas; respaldar la base antes de cambios de esquema importantes. Conservar los archivos de datos PostgreSQL y el archivo privado de entorno en el volumen, no reconstruirlos al actualizar el JAR.

## Estado comprobado el 28-10-2026

Java 21 y PostgreSQL 16 instalados, servicios syncpass/postgresql activos, migraciones V1 y V2 aplicadas con exito, /api/health y /actuator/health en UP, y /api/proyectos devuelve 401 sin token. El backend ocupaba aproximadamente 349 MiB en la comprobacion inicial, con limite de servicio de 640 MiB.

El backend escucha todavia solo en 127.0.0.1:8080: aun no es una API publica para Android. Falta definir y configurar el acceso HTTPS/API Gateway, verificar Cognito con solicitudes reales y recompilar la app con su URL publica. No se ha abierto PostgreSQL ni el puerto de negocio a Internet.

Al detener/iniciar una instancia con IPv4 publica automatica puede cambiar su IP. Las instrucciones SSH y cualquier integracion que dependa de esa IP deben actualizarse. Una Elastic IP no es necesaria para esta etapa y generaria sus propios cargos mientras permanezca asignada.
