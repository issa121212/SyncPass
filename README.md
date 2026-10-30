# SyncPass

Versión Android: **1.7.0**, versionCode **10**.

Aplicación Android para organizar proyectos comunitarios y eventos, inscribir participantes, registrar asistencia mediante QR y recoger encuestas. Proyecto académico basado en la problemática de [caso.md](docs/caso.md).

El frontend móvil utiliza Kotlin y Jetpack Compose; el backend utiliza Java, Spring Boot y PostgreSQL, y está desplegado en Amazon EC2. La misma API sirve a Android y a SyncPassWeb, que tiene su propio repositorio y despliegue.

## Funcionalidades

- Inicio y cierre de sesión con Amazon Cognito; saludo con el nombre de la persona.
- Inicio adaptado a docente, estudiante/voluntario y participante.
- CRUD de proyectos mediante la API, con autorización por rol y responsable.
- CRUD de eventos mediante la API y cancelación conservando historial.
- CRUD de recintos exclusivamente local con Room.
- Inscripciones, QR del participante, escaneo por recepción y encuestas.
- Catálogo local y cola de operaciones concretas para trabajo con conectividad intermitente.
- Fecha y hora separadas con formato automático; editor de eventos en tres pasos.

## Alcance de esta documentación

Describe la entrega de SyncPass Android y su backend compartido, incluyendo el CRUD de proyectos y eventos por API, el CRUD local de recintos, las inscripciones, la asistencia mediante QR y las encuestas. Incluye arquitectura, operación, pruebas y límites del MVP académico.

## Organización

```text
Android/           Aplicación :app y módulo :cognito-auth
Backend/           Copia del backend compartido en este repositorio
docs/              Documentación única del sistema
PREPARAR_RUN.ps1   Preparación del entorno de ejecución local
```

El backend también se identifica en el trabajo como **fs2-api**, carpeta independiente del equipo. Aquí su código está en `Backend/`: los comandos y enlaces usan ese nombre real. No se presupone que cualquier copia de fs2-api tenga exactamente la misma revisión.

## Arranque rápido

1. Instalar Android Studio, SDK Android 37 y un JDK compatible; para las comprobaciones se utilizó JDK 21.
2. Abrir la carpeta `Android/`, no la raíz completa como proyecto Android.
3. Crear `Android/.env` a partir de `Android/.env.example` si se necesita personalizar la configuración. El build puede leer el ejemplo cuando no existe `.env`.
4. Comprobar Cognito y `API_BASE_URL`. La URL debe terminar en `/`.
5. Sincronizar Gradle, seleccionar `app` o la configuración `SyncPass Local`, elegir dispositivo y pulsar Run.
6. Iniciar sesión con una cuenta de prueba y su grupo configurado en Cognito.

Con el backend remoto no es necesario ejecutar Java o PostgreSQL en el PC. EC2 y el Learner Lab deben estar activos. Para el backend local, Postgres/H2 y la conexión de dispositivos, consultar [ejecución](docs/06-EJECUCION-Y-DESPLIEGUE.md).

## Documentación

El [índice](docs/README.md) reúne arquitectura, requisitos, permisos, API, sincronización, despliegue, manual, dependencias, pruebas y Gitflow.

No publicar `.env`, claves PEM, contraseñas ni tokens. Utilizar datos ficticios o anonimizados en las demostraciones y entregas.
