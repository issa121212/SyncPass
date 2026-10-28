# Despliegue AWS

Guardar Android y Backend en un unico repositorio no implica desplegar ambas aplicaciones en EC2. Android se instala en los dispositivos y Room permanece dentro de ellos. EC2 ejecuta solo el backend Java.

Arquitectura prevista: Android -> HTTPS API Gateway HTTP API con JWT authorizer Cognito -> VPC Link -> ALB interno -> EC2 con Java 21/Spring Boot -> PostgreSQL privado (preferiblemente RDS). Mantener las validaciones de access token, roles y propiedad de recursos en Spring. Configurar issuer y audience/client_id del authorizer, preservar las rutas /api y restringir el puerto interno del backend.

## Configuracion productiva

No copiar el .env local a EC2: activa H2 en memoria. Configurar SPRING_PROFILES_ACTIVE=prod, DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD, AWS_REGION, AWS_COGNITO_USER_POOL_ID, AWS_COGNITO_CLIENT_ID y COGNITO_ISSUER fuera del repositorio. Las propiedades base activan PostgreSQL/Flyway; validar migraciones, transacciones y concurrencia con PostgreSQL real antes de produccion.

Compilar desde Backend con ./gradlew bootJar y ejecutar el JAR generado con Java 21 como servicio con reinicio y logs. Configurar credenciales de base mediante un mecanismo seguro fuera de Git. Para llamadas administrativas Cognito usar el rol IAM de EC2 con permisos minimos, sin access keys estaticas en Android ni en el repositorio. EC2 necesita conectividad saliente hacia Cognito, JWKS y userInfo.

Cuando exista el endpoint HTTPS publico, compilar Android con API_BASE_URL apuntando a el. PREPARAR_RUN.ps1 omite el arranque local cuando detecta una URL remota/LAN. Cognito y sus callbacks siguen configurados para la app. API Gateway no sustituye las reglas de negocio del backend.

El despliegue no esta creado ni automatizado. GitHub Actions puede incorporarse despues para compilar/probar y actualizar solo Backend en EC2, preferiblemente autenticando con AWS mediante OIDC y permisos limitados. No se han creado recursos AWS ni comprobado permisos/costos de una cuenta o laboratorio.

Referencias: https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-integrations-private.html y https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_use_switch-role-ec2.html
