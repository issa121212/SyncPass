# Validacion del backend con PostgreSQL

El backend usa PostgreSQL y Flyway fuera del perfil `local`. El perfil local conserva H2 en memoria para ejecutar la app facilmente desde Android Studio.

Se agrego `spring-boot-starter-flyway`, necesario para activar la integracion de migraciones en Spring Boot 4. Antes solo estaba `flyway-core`: con un PostgreSQL vacio Hibernate encontraba tablas faltantes porque las migraciones no se ejecutaban.

## Pruebas sin Docker

Usar una base **exclusiva de pruebas**, vacia o descartable. Las pruebas crean y modifican proyectos, eventos, usuarios y asistencias; no apuntarlas a una base productiva ni a bases de otras aplicaciones.

En PowerShell, desde `Backend`:

```powershell
$env:TEST_POSTGRES_URL = 'jdbc:postgresql://127.0.0.1:15432/syncpass_validation'
$env:TEST_POSTGRES_USER = 'syncpass_test'
# Definir TEST_POSTGRES_PASSWORD solo si la instancia de pruebas exige password.
.\gradlew.bat test --tests com.duoc.cursos.service.PostgresCrudIntegrationTest --tests com.duoc.cursos.service.PostgresAttendanceIntegrationTest
```

El usuario y la base deben existir en esa instancia. Estas variables solo configuran las pruebas; no cambian el `.env` de la aplicacion. No guardar passwords en Git. Si TEST_POSTGRES_URL no esta definida, esas clases se omiten. La prueba original PostgresIdentityIntegrationTest sigue disponible mediante Testcontainers cuando Docker funciona.

Los tests reutilizan los escenarios de API existentes contra PostgreSQL con Flyway habilitado y `ddl-auto=validate`. No permiten que Hibernate cree las tablas para ocultar migraciones ausentes. La clave idempotente del escenario QR incorpora el evento para poder repetirlo en una base de prueba persistente.

## Resultado del 28-10-2026

Validado con PostgreSQL 16 en una instancia temporal ligada a 127.0.0.1:15432, independiente del servicio PostgreSQL habitual del PC. Migraciones V1 y V2 aplicadas correctamente. Cuatro pruebas PostgreSQL aprobadas: CRUD/propiedad, rechazo por rol, proteccion del historial y asistencia por QR. Comprobacion adicional de regresiones: 32 pruebas aprobadas en total, incluyendo esas cuatro, y bootJar compilado. La clase Testcontainers se excluyo de esa ejecucion porque Docker Desktop no estaba operativo.

Los JWT/Cognito se simulan en estas pruebas. Esta validacion no sustituye probar la configuracion real de Cognito, red, TLS y permisos de AWS en el despliegue. Las pruebas de concurrencia y carga no forman parte de esta comprobacion.
