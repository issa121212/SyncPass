# Cobertura mínima Android · 26-11-2026

## Resultado actual

La suite local JUnit del módulo Android `:app` supera el mínimo de 30 % de líneas. Se ejecutó sin emulador, teléfono, API remota ni base de datos de EC2.

| Medición | Resultado |
|---|---|
| Líneas | **45,22 % · 2.248 de 4.971** |
| Ramas | 28,96 % · 711 de 2.455 |
| Métodos | 35,72 % · 584 de 1.635 |
| Pruebas locales JUnit | 100 aprobadas; 0 fallos, errores u omisiones |
| Verificación mínima | Aprobada: 30 % de líneas |
| Lint | 0 errores; conserva advertencias |

La meta se refiere a líneas, no a todos los contadores. La cobertura de ramas todavía es inferior a 30 %. El porcentaje no significa que todos los flujos, dispositivos o condiciones de red estén comprobados.

## Qué se agregó y por qué

Se conservaron las 95 pruebas unitarias anteriores y se agregaron cinco pruebas de componentes locales con JUnit y Robolectric 4.18. Están en `Android/app/src/test/java/com/rojas/syncpass/RoomOfflineLocalTest.kt` y ejercitan comportamiento de persistencia que un DAO simulado no verifica:

1. Confirmación de proyecto/evento dependiente: reemplazo de referencias temporales, conservación del payload y códigos públicos y recuperación tras reabrir la base.
2. Actualización del catálogo: conserva creaciones locales y reaplica inscripción/encuesta pendientes sin inventar asistencia personal.
3. Errores temporales y rechazos: conserva datos y distingue qué operaciones deben volver a enviarse y cuáles quedan rechazadas.
4. UUID duplicado: no reemplaza el registro original ni genera otra tarjeta u operación.
5. Limpieza de sincronizados: elimina solo los confirmados y conserva pendientes aunque el servidor ya no publique el evento en su catálogo.

Son pruebas locales de componentes Room/SQLite sobre Android simulado, no cinco pruebas unitarias puras de una función aislada. Se ejecutan mediante `testDebugUnitTest`, separadas de `connectedDebugAndroidTest`. No se mezclan datos de ejecución de pruebas instrumentadas ni se presenta este resultado como cobertura integral del backend.

Cada prueba usa una base temporal aislada, la cierra y elimina sus propios archivos. Los nombres se mantienen cortos para evitar el límite de rutas de SQLite en Windows. Se simula Android API 35 mediante una Application simple: no se inicializa la aplicación de producción con Hilt, no se autentica un usuario real y no se usa ningún token real.

## Medición y alcance

Se mantiene el reporte estándar de AGP `createDebugUnitTestCoverageReport` sobre el módulo completo. No se agregaron exclusiones de archivos de aplicación, UI, Room o Hilt para superar el mínimo. El total de líneas sigue siendo 4.971, igual que en la medición anterior de 19,71 % (980 líneas cubiertas).

El aumento incluye código generado de Room que ahora se ejecuta al comprobar consultas y transacciones reales. Eso forma parte del reporte completo; no debe presentarse como si todas esas líneas fueran lógica de negocio escrita manualmente.

Robolectric carga clases mediante su propio classloader y JaCoCo no las contabilizaba inicialmente. Se habilitó `isIncludeNoLocationClasses = true` para instrumentarlas. No se alteró el código productivo para simular ejecución ni se fusionaron reportes antiguos. Robolectric es una dependencia `testImplementation`, no una librería del APK de producción.

Los reportes generados están excluidos de Git:

- HTML: `Android/app/build/reports/coverage/test/debug/index.html`.
- XML: `Android/app/build/reports/coverage/test/debug/report.xml`.
- JUnit: `Android/app/build/test-results/testDebugUnitTest/`.

## Verificación automática

Se agregó `verifyDebugUnitTestCoverage`, dependiente del reporte unitario. Lee el contador LINE del módulo completo y falla si cubiertas/total es menor a 30 %, sin redondear para decidir el cumplimiento. También falla si el reporte no tiene líneas válidas.

La tarea `:app:check` depende de esta verificación. Se comprobó esa dependencia mediante dry-run; la ejecución real de la verificación y lint aprobó con las 100 pruebas locales.

Desde `Android/`:

```powershell
.\gradlew.bat :app:verifyDebugUnitTestCoverage :app:lintDebug -PsyncpassTestInstall=false
```

Para ejecutar las verificaciones habituales junto con el mínimo:

```powershell
.\gradlew.bat :app:check -PsyncpassTestInstall=false
```

Los argumentos JVM y recursos Android de prueba siguen la [configuración oficial de Robolectric](https://robolectric.org/getting-started/). La captura de clases sin ubicación utiliza la [opción documentada de JaCoCo en Gradle](https://docs.gradle.org/current/javadoc/org/gradle/testing/jacoco/plugins/JacocoTaskExtension.html).

## Git y despliegue

Trabajo local en `test/cobertura-minima-android`, dependiente de la cadena de mejoras de `feature/codigos-publicos`. No se publican ramas ni se fusiona develop/main automáticamente. Esta mejora cambia configuración de pruebas, pruebas y documentación; no requiere actualizar EC2 ni reinstalar la app del usuario.
