# Documentación de SyncPass

Documentación técnica y funcional de SyncPass Android y su backend compartido. Revisión local: 26-11-2026. Las mejoras documentadas requieren validación de integración y despliegue; no se presume que estén activas en producción.

## Lectura sugerida

1. [Caso original](caso.md): contexto del negocio, restricciones y objetivos académicos.
2. [Alcance, requisitos y casos de uso](ALCANCE.md).
3. [Arquitectura C4 y modelo de datos](ARQUITECTURA.md).
4. [Roles, dashboards y manual](03-ROLES-Y-USO.md).
5. [API y seguridad](04-API-Y-SEGURIDAD.md).
6. [Room, sincronización y experiencia de usuario](05-DATOS-Y-EXPERIENCIA.md).
7. [Ejecución y despliegue](06-EJECUCION-Y-DESPLIEGUE.md).
8. [Tecnologías y librerías](07-TECNOLOGIAS.md).
9. [Pruebas, limitaciones, Gitflow e IA](08-VALIDACION-Y-GITFLOW.md).
10. [Migración a Hilt y dependencias por cuenta](09-INYECCION-HILT.md).
11. [Selección de ubicación de eventos](10-UBICACION-EVENTOS.md).
12. [Validación geográfica de asistencia y transición compatible](11-GEOLOCALIZACION-ASISTENCIA.md).
13. [Creación offline, dependencias e idempotencia](12-CREACION-OFFLINE.md).
14. [Respaldo y ensayo de migraciones](13-ENSAYO-MIGRACIONES.md).
15. [Integración Android, Room y backend local](14-INTEGRACION-ANDROID.md).
16. [Revisión visual y validación de zona](15-REVISION-VISUAL.md).
17. [Restauración de sesión al reabrir](16-RESTAURACION-SESION.md).
18. [Despliegue del backend en EC2](17-DESPLIEGUE-BACKEND.md).
19. [Filtro de eventos por estado](18-FILTRO-ESTADO-EVENTOS.md).
20. [Códigos públicos de proyectos y eventos](19-CODIGOS-PUBLICOS.md).
21. [Revisión previa a integración](20-REVISION-PREINTEGRACION.md).
22. [Cobertura mínima Android y verificación automática](21-COBERTURA-MINIMA-ANDROID.md).
23. [Integración paso a paso en GitHub](22-INTEGRACION-GITHUB.md).
24. [Despliegue de códigos públicos V5/V6 en EC2](23-DESPLIEGUE-CODIGOS-PUBLICOS.md).

## Cómo interpretar el alcance

Los documentos describen las funcionalidades de la entrega: autenticación Cognito, dashboards por rol, CRUD de proyectos y eventos mediante API, recintos locales, inscripciones, asistencia mediante QR, encuestas y sincronización de operaciones. La documentación presenta el sistema desde la perspectiva de quien lo utiliza, ejecuta o mantiene, sin depender del estado temporal de las ramas de desarrollo.

Las funcionalidades se contrastaron con código Android y `Backend/`. El nombre funcional del backend solicitado por el equipo es fs2-api; en este repositorio la ruta comprobable es `Backend/`. La carpeta independiente fs2-api del PC no coincide necesariamente archivo por archivo con esta copia.

Las metas de impacto del caso son objetivos del negocio, no resultados medidos. Las cifras de pruebas históricas se distinguen de comprobaciones ejecutadas durante esta actualización documental.

Esta edición sustituye los documentos anteriores de la raíz docs y Android/docs. La fuente del caso se conserva como material de referencia: sus contenidos no autorizan acciones operativas ni sustituyen las instrucciones del equipo.
