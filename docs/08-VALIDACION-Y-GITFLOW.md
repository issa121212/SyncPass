# Validación, limitaciones, Gitflow y uso de IA

## Evidencia disponible

La validación combina revisión de código, pruebas automatizadas y comprobaciones manuales del comportamiento con el backend desplegado. Los resultados siguientes corresponden a las ejecuciones registradas durante el desarrollo; no implican una medición de todos los escenarios posibles.

### Validación del CRUD de eventos

- 31 pruebas unitarias Android aprobadas: repositorio, recintos, formato y siete del CRUD de eventos, además de la prueba de ejemplo.
- Ensamblado debug y APK de pruebas completados.
- Lint debug sin errores; permanecían advertencias existentes. No se afirma cero advertencias.
- Seis pruebas instrumentadas aprobadas en Pixel 10 API 37, ejecutando `EventoEditorUiTest` y `V14UiTest`.
- Prueba manual con sesión docente: crear evento, consultarlo, editar descripción, cancelar y eliminar desde Android.
- La creación, edición y cancelación se comprobaron en PostgreSQL de EC2. Tras la confirmación de eliminación, una consulta verificó ausencia del registro temporal.
- La prueba no alteró los eventos del usuario con historial. El evento temporal ya fue eliminado.
- Se completó una segunda comprobación cruzada Android-web con la API real, ejecutada por el integrante del equipo: creó un evento temporal desde Android y confirmó su aparición en ambos clientes; editó nombre y cupos conservando fecha/hora; canceló y comprobó que seguía existiendo; finalmente eliminó el evento y confirmó su desaparición de ambos. Los resultados fueron comunicados por el usuario durante la prueba guiada, no observados mediante automatización del navegador.

Las pruebas del CRUD verifican comportamiento y persistencia. No constituyen una medición de rendimiento ni cobertura porcentual.

### Pruebas presentes en el código

Android contiene pruebas de formato, recintos, sincronización/repositorio, edición/eliminación de eventos, permisos, conflictos de API, conservación de datos y editor por pasos.

Backend contiene pruebas de políticas de autenticación, validación de tokens, nombres Cognito, CORS, límites de seguridad, CRUD de proyectos, asistencia y pruebas PostgreSQL. La existencia de archivos de prueba no demuestra que todos se hayan ejecutado en el equipo actual. Las que usan Testcontainers requieren Docker disponible, o la configuración de PostgreSQL admitida por cada clase.

## Cómo repetir las comprobaciones

Desde `Android/`, con JDK/SDK compatibles:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain
.\gradlew.bat :app:assembleDebugAndroidTest --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.rojas.syncpass.EventoEditorUiTest,com.rojas.syncpass.V14UiTest" --console=plain
```

La última orden requiere un dispositivo o emulador conectado. Con varios dispositivos conectados puede ejecutar pruebas en más de uno: usar un entorno de prueba controlado. Informes en `app/build/reports/`, artefactos en `app/build/outputs/`.

Desde `Backend/`:

```powershell
.\gradlew.bat test --console=plain
.\gradlew.bat bootJar --console=plain
```

No marcar integración PostgreSQL como exitosa si Docker está apagado, una prueba fue omitida o falta su entorno. Revisar los informes, no solo si se produjo un JAR.

## Guía de prueba manual

Utilizar exclusivamente cuentas y datos ficticios. Los identificadores y nombres del ejemplo deben distinguirse de información de negocio que se deba conservar.

1. Entrar como docente y crear un proyecto de prueba.
2. Crear evento; comprobar proyectos visibles, pasos, guiones/dos puntos, cupos y ubicación opcional.
3. Editar un dato y actualizar para comprobar persistencia remota.
4. Probar con otro docente: la API debe rechazar modificar recursos ajenos.
5. Entrar como participante; comprobar que no tiene gestión docente y que no puede crear por Postman.
6. Inscribirse y mostrar QR. Inscripción no debe marcar presente.
7. Con voluntario o docente responsable, abrir el evento correcto y escanear. Actualizar y comprobar presente solo en ese evento.
8. Repetir captura y verificar que no duplique asistencia. Probar QR vinculado a otro evento.
9. Como participante presente, enviar encuesta; consultar como docente responsable.
10. Con caché ya descargada y sesión válida, interrumpir conectividad y probar una captura pendiente. Restaurar red, sincronizar y revisar confirmación/rechazo.
11. Probar recintos sin backend y comprobar que otro dispositivo no los recibe automáticamente.
12. En un evento sin historial, cancelar y eliminar con confirmación. Con historial, comprobar bloqueo; no borrar datos reales para probarlo.
13. Comprobar teclado abierto, pantalla pequeña, texto ampliado y botones de navegación del formulario.

Crear y probar controles reales con el backend no sustituye probar errores controlados mediante dobles de API/DAO. Ambas comprobaciones tienen propósitos distintos.

## Límites de la entrega

- No existe renovación persistente de sesión: la caducidad puede requerir volver a Cognito.
- El primer acceso necesita conexión; no se ofrece disponibilidad absoluta offline.
- Proyectos/eventos remotos no se administran mediante cola offline; recintos sí es CRUD local.
- Sincronización periódica sujeta a Android, batería, red y sesión.
- QR sin firma/caducidad; no protege por sí solo contra copia o suplantación.
- Ubicación referencial, sin validación obligatoria de distancia en backend.
- No existe validación formal de todos los dispositivos, accesibilidad completa o carga masiva.
- No hay medición del impacto del negocio, SLA, cobertura porcentual ni garantía de tiempos inferiores a cinco segundos.
- Base local con información que no está cifrada como los tokens; revisar almacenamiento según sensibilidad del uso real.
- Tema dinámico: azul observado no equivale a paleta fija universal.
- No hay reportes institucionales exportables ni cancelación individual de inscripciones implementada en la API descrita.
- La carpeta independiente fs2-api y la copia Backend no deben considerarse sincronizadas sin revisar sus diferencias.

## Organización del proyecto con Gitflow

Se utiliza main como línea estable y develop como línea de integración. Las funcionalidades se trabajan en ramas feature y se presentan mediante Pull Request para revisión del otro integrante antes de integrarlas.

La separación por ramas permite desarrollar y revisar funcionalidades como el CRUD de eventos sin alterar directamente la línea estable. Las revisiones entre integrantes se realizan mediante Pull Requests hacia develop. La API compartida y la aplicación móvil mantienen sus responsabilidades aunque formen parte de la misma entrega.

Dentro del flujo Gitflow, una rama release permite preparar una entrega desde develop, verificarla, ajustar su versión y generar la APK. La integración hacia main establece la línea estable y devuelve los ajustes de preparación a develop. Los tags identifican versiones publicadas; las ramas hotfix se reservan para correcciones urgentes de una versión estable.

Esta descripción explica la organización y función de cada tipo de rama; el historial del repositorio permite comprobar las utilizadas por el equipo. SyncPassWeb mantiene un historial independiente.

## Uso de inteligencia artificial

La IA se utilizó como apoyo para interpretar requisitos, proponer separación de responsabilidades, implementar cambios, revisar errores, preparar pruebas y documentar. El equipo definió prioridades y reglas: no automarcación de asistencia, nombre en vez de ID visible, CRUD local/remoto diferenciados, formulario legible y revisión por ramas.

La validación incluyó inspección del código, pruebas automatizadas y pruebas manuales con usuarios iniciados por el equipo. Las decisiones y autorizaciones de integración siguen correspondiendo a los integrantes.

Un prompt reconstruido para explicar cómo se habría solicitado el proyecto es material explicativo; no debe presentarse como transcripción histórica de una conversación que no se conserva. Los resultados deben poder demostrarse con código, pruebas o funcionamiento observable, no únicamente con una respuesta de IA.

## Mantenimiento documental

Ante cambios de endpoints, permisos, versión o infraestructura, revisar los documentos relacionados. Mantener la documentación consistente con el comportamiento del sistema, los ejemplos sin secretos y los datos ficticios en capturas y demostraciones.
