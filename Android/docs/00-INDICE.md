# Índice General - Documentación Unificada SyncPass

Toda la documentación se encuentra unificada en este directorio, cubriendo tanto el backend `fs2-api` como la aplicación móvil `FormularioKotlin`. Ambos componentes conforman la solución **SyncPass** para la acreditación de asistencia mediante QR en jornadas comunitarias.

## Documentación

| Archivo | Descripción |
|---|---|
| [01-RESUMEN-GENERAL.md](01-RESUMEN-GENERAL.md) | Resumen ejecutivo, objetivo, alcance, estado actual y tecnologías utilizadas. |
| [02-REQUISITOS-FUNCIONALES.md](02-REQUISITOS-FUNCIONALES.md) | Requerimientos funcionales por actor (Docente, Estudiante, Administrador) con criterios de aceptación. |
| [03-REQUISITOS-NO-FUNCIONALES.md](03-REQUISITOS-NO-FUNCIONALES.md) | Requerimientos no funcionales: seguridad, rendimiento, confiabilidad, mantenibilidad, usabilidad, offline-first. |
| [04-ACTORES-ROLES-Y-PERMISOS.md](04-ACTORES-ROLES-Y-PERMISOS.md) | Actores, mapeo Cognito ↔ roles internos, matriz de permisos por endpoint y pantalla. |
| [05-FLUJOS-PRINCIPALES.md](05-FLUJOS-PRINCIPALES.md) | Flujos detallados paso a paso con actores, acciones y validaciones. |
| [06-INTERACCION-Y-SECUENCIAS.md](06-INTERACCION-Y-SECUENCIAS.md) | Diagramas de secuencia (Mermaid) entre App, Cognito, API, Room, WorkManager y PostgreSQL. |
| [07-ARQUITECTURA-GENERAL.md](07-ARQUITECTURA-GENERAL.md) | Vista unificada backend + móvil, comunicación HTTP, enfoque offline-first. |
| [08-ARQUITECTURA-BACKEND.md](08-ARQUITECTURA-BACKEND.md) | Capas backend, configuración, validación JWT, ownership, manejo de errores, migraciones. |
| [09-ARQUITECTURA-MOVIL.md](09-ARQUITECTURA-MOVIL.md) | Arquitectura FormularioKotlin: MVVM, Navigation, Room, Retrofit, DI manual, permisos. |
| [10-PATRONES-DE-DISENO.md](10-PATRONES-DE-DISENO.md) | Patrones aplicados en backend y móvil con ejemplos de clases reales. |
| [11-MODELO-DATOS.md](11-MODELO-DATOS.md) | Modelo unificado: entidades PostgreSQL y Room, relaciones, restricciones. |
| [12-CONTRATO-API.md](12-CONTRATO-API.md) | Contrato HTTP consolidado usado por la app móvil con requests/responses. |
| [13-OFFLINE-FIRST-Y-SINCRONIZACION.md](13-OFFLINE-FIRST-Y-SINCRONIZACION.md) | Cola de pendientes, tipos, idempotencia, SyncWorker, manejo de errores. |
| [14-COMPILACION-PRUEBAS-Y-LIMITACIONES.md](14-COMPILACION-PRUEBAS-Y-LIMITACIONES.md) | Compilación, tests, despliegue, limitaciones y checklist. |
| [15-DECISIONES-TECNICAS.md](15-DECISIONES-TECNICAS.md) | Decisiones técnicas justificadas y alternativas consideradas. |
| [16-GUIA-PRUEBA-MVP.md](16-GUIA-PRUEBA-MVP.md) | Guía paso a paso para probar el MVP: backend local con H2, configuración de Cognito, build de la app y smoke test end-to-end. |

## Documentación histórica

Los documentos anteriores a la unificación (API.md, ARQUITECTURA.md,
MODELO_DATOS.md, REQUERIMIENTOS.md, PLAN.md, etc.) se conservan en
[`legacy/`](legacy/README.md) solo como referencia. **No están
actualizados**: describen el modelo anterior de "clases" y endpoints que
ya no existen. Para el estado real del sistema usa siempre los archivos
numerados de este índice.
