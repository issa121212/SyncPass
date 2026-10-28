# SyncPass

Aplicacion Android y API Java/Spring Boot para proyectos comunitarios, eventos, inscripciones, asistencia mediante QR y encuestas. Login con Amazon Cognito, CRUD de proyectos por API y CRUD de recintos local con Room.

## Ejecutar en Windows

1. Instalar Android Studio, SDK Android/API 37 y JDK 21. La primera compilacion necesita Internet.
2. Clonar el repositorio y abrir **la carpeta Android** en Android Studio. La carpeta contenedora no es el proyecto Gradle de Android.
3. Esperar Gradle Sync, abrir el emulador o conectar el telefono con depuracion USB autorizada.
4. Seleccionar **SyncPass Local** y pulsar Run. El script PREPARAR_RUN.ps1 crea los .env ausentes desde las plantillas, inicia/reutiliza el backend y configura adb reverse en los dispositivos detectados.
5. Para usar celular y emulador simultaneamente, instalar la app ejecutando Run una vez para cada dispositivo. Ambos comparten el backend del PC y pueden usar distintas cuentas Cognito.

Android usa .env.example si aun no existe .env al configurar Gradle. Las plantillas incluyen identificadores PUBLICOS del entorno Cognito de desarrollo; no contrasenas ni claves AWS. El login requiere una cuenta valida y que ese entorno siga disponible. Un .env existente tiene prioridad y nunca se sobrescribe. Si cambia la configuracion Android hay que recompilar con Run.

Si se reinicia ADB o se reconecta el USB, ejecutar SyncPass Local otra vez para restaurar adb reverse. No hay reconexion continua. Run instala solo en el dispositivo seleccionado. El backend permanece en segundo plano cuando se cierra la app; su log esta en .local/backend.log.

## Estructura

- Android/: app, autenticacion Cognito, contratos de dominio, adaptadores Retrofit y persistencia Room.
- Backend/: controladores REST, servicios de negocio, DTO records, entidades JPA, repositorios, integracion Cognito, validacion JWT y migraciones Flyway.
- PREPARAR_RUN.ps1: preparacion local usada por la configuracion compartida Android/.run/SyncPass_Local.run.xml.
- Android/docs/: documentacion funcional y de arquitectura de referencia.
- docs/DESPLIEGUE_AWS.md: separacion entre desarrollo local y despliegue productivo.

## Datos y limites

El perfil local del backend usa H2 en memoria: pierde los datos al detener el proceso. Room conserva datos por dispositivo/cuenta y los recintos locales no se sincronizan al servidor. La asistencia pendiente se confirma al sincronizar con la API.

El formato QR actual conserva compatibilidad con identificadores existentes; no implementa una credencial firmada de corta duracion ni prueba antifraude de presencia. Las pruebas con PostgreSQL real y la infraestructura productiva deben validarse antes de desplegar.

La version interna Android se mantiene para permitir actualizaciones sobre instalaciones existentes. El nombre del repositorio no cambia el applicationId, los callbacks Cognito ni el nombre de la aplicacion.

## Git

No subir .env, tokens, passwords, credenciales AWS, claves privadas, keystores, APK/JAR de aplicacion, caches ni builds. Se incluyen los wrappers Gradle y .env.example. La URL API, issuer y client ID de una app movil son configuracion publica. La documentacion permanece dentro de carpetas docs.
