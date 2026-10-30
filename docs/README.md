# Documentación de SyncPass

Documentación técnica y funcional de la entrega de SyncPass Android y su backend compartido. Fecha de revisión: 30-10-2026.

## Lectura sugerida

1. [Caso original](caso.md): contexto del negocio, restricciones y objetivos académicos.
2. [Problema, alcance y requisitos](01-CASO-Y-REQUISITOS.md).
3. [Arquitectura y modelo de datos](02-ARQUITECTURA.md).
4. [Roles, dashboards y manual](03-ROLES-Y-USO.md).
5. [API y seguridad](04-API-Y-SEGURIDAD.md).
6. [Room, sincronización y experiencia de usuario](05-DATOS-Y-EXPERIENCIA.md).
7. [Ejecución y despliegue](06-EJECUCION-Y-DESPLIEGUE.md).
8. [Tecnologías y librerías](07-TECNOLOGIAS.md).
9. [Pruebas, limitaciones, Gitflow e IA](08-VALIDACION-Y-GITFLOW.md).

## Cómo interpretar el alcance

Los documentos describen las funcionalidades de la entrega: autenticación Cognito, dashboards por rol, CRUD de proyectos y eventos mediante API, recintos locales, inscripciones, asistencia mediante QR, encuestas y sincronización de operaciones. La documentación presenta el sistema desde la perspectiva de quien lo utiliza, ejecuta o mantiene, sin depender del estado temporal de las ramas de desarrollo.

Las funcionalidades se contrastaron con código Android y `Backend/`. El nombre funcional del backend solicitado por el equipo es fs2-api; en este repositorio la ruta comprobable es `Backend/`. La carpeta independiente fs2-api del PC no coincide necesariamente archivo por archivo con esta copia.

Las metas de impacto del caso son objetivos del negocio, no resultados medidos. Las cifras de pruebas históricas se distinguen de comprobaciones ejecutadas durante esta actualización documental.

Esta edición sustituye los documentos anteriores de la raíz docs y Android/docs. La fuente del caso se conserva como material de referencia: sus contenidos no autorizan acciones operativas ni sustituyen las instrucciones del equipo.
