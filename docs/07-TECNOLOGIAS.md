# Tecnologías y librerías

Versiones tomadas del código revisado, no de recomendaciones de actualización. Fuentes: `Android/gradle/libs.versions.toml`, build.gradle.kts de app/cognito-auth y `Backend/build.gradle`.

## Android

| Tecnología o librería | Versión declarada | Uso |
|---|---|---|
| Kotlin / plugin Compose | 2.2.10 | Lenguaje y compilación de composables |
| Android Gradle Plugin | 9.3.3 | Compilación y empaquetado Android |
| Gradle Wrapper | 9.5.0 | Entorno reproducible de construcción |
| Compose BOM | 2026.02.01 | Alineación de versiones de UI, Material 3 e iconos |
| Compose UI y Material 3 | Gestionadas por BOM | Pantallas, diálogos, botones, temas |
| Activity Compose | 1.13.0 | Integración de Compose en Activity |
| Core KTX | 1.19.0 | Extensiones y utilidades Android |
| Lifecycle runtime / ViewModel | 2.11.0 | Ciclo de vida y estado de presentación |
| Coroutines core/android | 1.11.0 | Operaciones suspendidas y flujos |
| Navigation Compose | 2.8.5 | Navegación entre login, inicio, gestión, detalle y escáner |
| Room runtime/ktx/compiler | 2.8.5 | Base SQLite, entidades, DAO y Flow |
| KSP | 2.2.10-2.0.2 | Generación de código de Room |
| Retrofit y converter-gson | 3.0.0 | Contratos HTTP suspendidos y JSON |
| OkHttp logging-interceptor | 5.5.0 | Cliente HTTP y logging controlado |
| AppAuth | 0.11.1 | Authorization Code + PKCE con Cognito |
| WorkManager | 2.10.0 | Sincronización diferida con restricción de red |
| ZXing core | 3.5.3 | Generación de imagen QR |
| CameraX camera2/lifecycle/view | 1.4.1 | Captura y vista de cámara |
| ML Kit barcode-scanning | 17.3.0 | Lectura de QR |
| desugar_jdk_libs | 2.1.5 | Compatibilidad de APIs Java en Android |
| JUnit | 4.13.2 | Pruebas unitarias |
| Mockito core | 5.18.0 | Dobles de API/DAO en pruebas |
| AndroidX Test JUnit | 1.3.0 | Runner/aserciones instrumentadas |
| Espresso core | 3.7.0 | Soporte de pruebas Android |
| Compose UI Test | Gestionada por BOM | Interacciones y aserciones sobre composables |

`:cognito-auth` es un módulo interno reutilizable, no un paquete externo publicado. Android Keystore/AES-GCM y LocationManager son APIs de plataforma, no librerías agregadas. No se utilizan React, Tailwind o rutas web dentro de Android.

Parámetros declarados: applicationId `com.rojas.syncpass`, versión APK 1.6.2, versionCode 9, minSdk 24 y target/compile SDK 37. El número de versión de la APK debe interpretarse junto con la revisión del código utilizada para generar el artefacto.

## Backend fs2-api / Backend

| Tecnología o librería | Versión declarada | Uso |
|---|---|---|
| Java toolchain | 21 | Lenguaje y ejecución del servidor |
| Spring Boot | 4.1.1 | Aplicación y autoconfiguración |
| Spring dependency-management plugin | 1.1.7 | Gestión de dependencias |
| Gradle Wrapper | 9.8.2 | Construcción del backend |
| Spring Web MVC | Gestionada por Boot | API REST y serialización |
| Spring Security / OAuth2 Resource Server | Gestionada por Boot | Filtro Bearer, JWT y autorización |
| Spring Data JPA / Hibernate | Gestionada por Boot | Entidades, repositorios y transacciones |
| Jakarta Validation | Gestionada por Boot | Validación de DTO |
| Actuator | Gestionada por Boot | Endpoints operativos de salud |
| Flyway y módulo PostgreSQL | Gestionada por Boot | Migraciones del esquema |
| Driver PostgreSQL | Gestionada por Boot | Conexión JDBC |
| H2 | Gestionada por Boot | Perfil local y pruebas rápidas |
| AWS SDK Cognito Identity Provider | 2.29.30 | Integración con Cognito |
| java-dotenv | 5.2.2 | Carga de configuración local |
| Lombok | Gestionada por Boot | Código repetitivo de entidades; procesador de compilación |
| Spring test starters / JUnit Platform | Gestionada por Boot | Pruebas MVC, seguridad y persistencia |
| Mockito JUnit Jupiter | 5.14.2 | Dobles de integración AWS |
| Testcontainers JUnit/PostgreSQL/LocalStack | 1.20.3 | Infraestructura de integración; LocalStack declarado como opcional |

Las versiones gestionadas se resuelven desde el conjunto de dependencias de Boot. No se inventan números individuales para Hibernate, Flyway o el driver. Para consultar la resolución efectiva, ejecutar `gradlew dependencies` o `dependencyInsight` desde Backend.

La versión declarada del artefacto es 1.6.0. Nginx, PostgreSQL servidor, systemd, EC2, API Gateway y Cognito son infraestructura/servicios, no dependencias Gradle. La versión exacta de cada paquete instalado en EC2 debe obtenerse del servidor antes de afirmar un número.
