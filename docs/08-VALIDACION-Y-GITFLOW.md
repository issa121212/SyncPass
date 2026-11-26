# Validación, limitaciones, Gitflow y uso de IA

## Medición local actual (26-11-2026)

La suite local JUnit de Android :app registra 100 pruebas aprobadas: 95 unitarias previas y cinco de componentes Room con Android simulado mediante Robolectric. El reporte exclusivo de esa suite mide **2.248 de 4.971 líneas (45,22 %)**, sin sumar las pruebas instrumentadas ni excluir archivos de aplicación. Se supera el mínimo de 30 % de líneas; ramas registra 28,96 % y no alcanza ese porcentaje.

La tarea verifyDebugUnitTestCoverage exige el mínimo y está conectada a :app:check. Alcance, casos nuevos, comandos y límites se detallan en [Cobertura mínima Android](21-COBERTURA-MINIMA-ANDROID.md). Las mediciones siguientes son históricas y no sustituyen esta cifra.

## Validación histórica de las mejoras locales (25-11-2026)

La suite unitaria de esa etapa registró 88 pruebas aprobadas. El reporte exclusivo de unitarias cubre 940 de 4825 líneas (19,48 %), incluyendo código generado y UI; no alcanza la meta del 30 %. Se verificaron dependencias Hilt, sesión por cuenta, selección de zona, captura geográfica, creaciones offline, reintentos y sincronización concurrente con fuentes falsas. No se ejecutaron nuevas pruebas instrumentadas ni integraciones de producción. Los resultados combinados anteriores se conservan más abajo como evidencia histórica y no sustituyen esta medición.

La app y el APK de pruebas compilan; lint registra cero errores. Los resultados detallados, comandos y pendientes están en [Creación offline](12-CREACION-OFFLINE.md). Las features locales dependen en orden de Hilt, ubicación, geolocalización de asistencia y creación offline; su integración hacia `develop` y posterior release hacia `main` requiere revisión. No se han publicado estas ramas ni desplegado el backend en esta etapa.

Posteriormente se ejecutaron [seis pruebas instrumentadas y un escenario de actualización de APK en dos fases](14-INTEGRACION-ANDROID.md) sobre Room real y el backend local. Aprobaron sin omisiones; no se mezcló esa evidencia con la cobertura unitaria. La prueba local utilizó identidad ficticia firmada y una copia de PostgreSQL, no el login real de Cognito ni producción.

## Evidencia disponible

Para preparar Android 1.7.0 (versionCode 10) se ejecutaron testDebugUnitTest, lintDebug, assembleDebug y assembleRelease con resultado correcto: 31 pruebas unitarias, sin fallos ni omisiones. Lint conserva advertencias existentes. Se generó una APK debug firmada con la clave de desarrollo y un artefacto release sin firma de distribución; este último debe firmarse antes de instalarlo como release. Las seis pruebas instrumentadas indicadas más abajo corresponden a la validación anterior del CRUD, no a una repetición durante esta preparación.

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

Las pruebas del CRUD verifican comportamiento y persistencia. La cobertura se mide por separado con JaCoCo.

## Cobertura histórica combinada Android con JaCoCo

El reporte combinado mide la variante `debug` del módulo Android `:app`. Reúne las pruebas unitarias y las pruebas instrumentadas de interfaz, Room y sesión. La tarea usa los datos de ejecución de ambas suites para contar cada línea cubierta una sola vez.

La primera medición con 31 pruebas unitarias cubrió 336 de 3.388 líneas (9,92 %). El informe combinado se generó con 35 pruebas unitarias y 12 instrumentadas, todas aprobadas en su entorno correspondiente. Las instrumentadas se ejecutaron en un Pixel 10 emulado con Android 17. El informe incluye las clases generadas por Room del módulo `:app`.

| Métrica | Cubiertas | Total | Cobertura |
|---|---:|---:|---:|
| Líneas | 1.492 | 3.390 | 44,01 % |
| Instrucciones | 17.600 | 37.367 | 47,10 % |
| Ramas | 639 | 1.683 | 37,97 % |

Desde `Android/`, con un emulador conectado y seleccionado por `ANDROID_SERIAL`, ejecutar en PowerShell:

```powershell
$env:ANDROID_SERIAL = 'emulator-5554'
$clases = 'EventoEditorUiTest','HomeScreenRegressionTest','LocalCrudUiTest','RecintoRoomTest','SessionSecurityTest','V14UiTest'
$suite = ($clases | ForEach-Object { "com.rojas.syncpass.$_" }) -join ','
.\gradlew.bat :app:syncPassDebugCoverageReport -PsyncpassInstrumentedCoverage=true -PsyncpassTestInstall=true "-Pandroid.testInstrumentationRunnerArguments.class=$suite"
```

El informe HTML se genera en `app/build/reports/jacoco/syncPassDebugCoverageReport/html/index.html`. El sufijo `.validation` aísla la instalación utilizada para las pruebas. Esta selección puede ejecutarse con datos controlados y sin depender de la disponibilidad de EC2.

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
- La creación de proyectos/eventos utiliza cola offline; su edición y eliminación remotas siguen requiriendo conexión. Recintos mantiene CRUD local.
- Sincronización periódica sujeta a Android, batería, red y sesión.
- QR sin firma/caducidad; no protege por sí solo contra copia o suplantación.
- Validación geográfica incorporada con modo inicial `AUDIT`; la activación obligatoria `ENFORCE` y las pruebas reales de clientes/migraciones siguen pendientes. Véase [la feature geográfica](11-GEOLOCALIZACION-ASISTENCIA.md).
- No existe validación formal de todos los dispositivos, accesibilidad completa o carga masiva.
- No hay medición del impacto del negocio, SLA ni garantía de tiempos inferiores a cinco segundos.
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
