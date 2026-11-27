# Revisión y ampliación de pruebas de Android

## Objetivo y plan

Trabajo desde develop, en feature/cobertura-android-70, sin modificar main ni desplegar backend. La meta es 70 % de líneas del módulo Android :app en el reporte estándar de pruebas locales. Se conserva el denominador completo, incluida UI y código generado de Room/Hilt; no se mezcla cobertura instrumentada ni de backend.

1. Ejecutar check, compilación de APK y compilación de pruebas instrumentadas sobre la base integrada.
2. Comprobar persistencia, seguridad de sesión y pantallas en una instalación separada del teléfono.
3. Identificar comportamientos sin cubrir y agregar pruebas JUnit con fuentes falsas, Room/SQLite local y componentes Compose simulados.
4. Corregir los problemas del entorno de prueba, repetir los escenarios afectados y medir la suite completa.
5. Registrar métricas, límites y comprobaciones pendientes. Actualizar el mínimo automático solo si el reporte completo alcanza la meta.

## Verificación inicial

La base develop 131f16c aprobó :app:check, :app:assembleDebug y :app:assembleDebugAndroidTest. La cobertura inicial tras la corrección GPS/red era 45,54 % de líneas (2.277 de 5.000), con 106 pruebas locales aprobadas.

El 27-11-2026 se ejecutaron 16 pruebas instrumentadas en el Moto g82 (Android 13), mediante los paquetes separados com.rojas.syncpass.validation y com.rojas.syncpass.validation.test. Se aprobaron persistencia y migraciones de Room, códigos públicos, cifrado de sesión con Keystore real, descarte del refresh token, eliminación de credenciales antiguas, editor por pasos, conservación de segundos, cupos respecto de inscritos, errores de guardado, filtros, QR sin automarcación, formato de fecha/hora, CRUD de recintos sin API y visualización completa del error de radio.

El primer intento obtuvo 10 fallos de interfaz porque el teléfono se bloqueó. El usuario lo desbloqueó y se repitieron las 16 pruebas: OK, 0 fallos. Durante la repetición se mantuvo encendida la pantalla con alimentación USB; se restauró el ajuste anterior al finalizar. La instalación personal y sus datos no fueron reemplazados.

## Entornos de pruebas locales

- Pruebas unitarias JUnit de reglas, ViewModels y coordinación de corrutinas con fuentes falsas.
- Robolectric API 35 para componentes Android, persistencia SQLite y pantallas Compose. Son pruebas de componentes locales; no deben describirse todas como unitarias puras.
- Sesión con cifrador simulado para verificar persistencia y coherencia entre cuentas. El cifrado real se comprueba por separado en el teléfono.
- HTTP real de loopback con respuestas sintéticas para verificar Retrofit, serialización, autorización y bloqueo de una cuenta inválida; no conecta con AWS.
- Actividad y grafo Hilt reales bajo una Application de prueba sin tareas programadas. Las pruebas de navegación autenticada sustituyen sesión y repositorio por entornos falsos.

Las dependencias Compose de prueba son testImplementation y no agregan bibliotecas a producción. La utilidad que crea esquemas Room históricos se comparte entre pruebas JVM e instrumentadas en src/sharedTest; no se duplican casos para aumentar la cantidad.

Para evitar un problema conocido de Robolectric con diálogos y campos de texto, las pruebas locales conservan el ancho virtual predeterminado y solo fijan la altura. No se modifica la interfaz de producción para acomodar el simulador. Referencia: https://github.com/robolectric/robolectric/issues/8460.

## Resultado final

| Medición de Android :app | Resultado |
|---|---|
| Cobertura de líneas | **85,38 %: 4.269 de 5.000** |
| Cobertura de ramas | 65,53 %: 1.610 de 2.457 |
| Cobertura de métodos | 78,66 %: 1.290 de 1.640 |
| Suite local JUnit | **141 aprobadas; 0 fallos, errores u omisiones** |
| Tipo de pruebas locales | 104 de lógica sin Robolectric; 36 de componentes con Robolectric y 1 de contrato HTTP en loopback |
| Pruebas instrumentadas en Moto | **16 aprobadas**, separadas del porcentaje |
| Lint de :app | 0 errores; 36 advertencias existentes |
| Lint de :cognito-auth | 0 errores; 3 advertencias |
| Control automático | 70 % de líneas, aprobado sin redondear para decidir |

No se agregaron exclusiones ni se redujo el denominador de 5.000 líneas para alcanzar la meta. El reporte incluye código generado: no equivale a que 4.269 líneas de lógica escrita manualmente estén cubiertas, ni a que todos los comportamientos reales estén validados. El 70 % corresponde al contador de líneas de :app; no se atribuye ese porcentaje al backend, al módulo :cognito-auth ni a la cobertura de ramas.

Se retiraron las dos pruebas de plantilla ExampleUnitTest y ExampleInstrumentedTest: la primera comprobaba 2 + 2 y la segunda fijaba el nombre del paquete, incompatible con una instalación de validación. No acreditaban funciones de negocio. La suite final conserva 141 escenarios útiles y no cuenta repeticiones de ejecución como casos nuevos.

Comprobación completa desde Android/:

```powershell
.\gradlew.bat :app:check :app:assembleDebug :app:assembleDebugAndroidTest :cognito-auth:check
```

El control verifyDebugUnitTestCoverage lee el contador LINE del reporte estándar y falla si no hay líneas o si cubiertas/total es inferior a 70 %. :app:check depende de ese control. No se suman archivos .ec de instrumentación ni reportes de ejecuciones anteriores.

Artefactos locales excluidos de Git:

- app/build/reports/coverage/test/debug/index.html y report.xml.
- app/build/test-results/testDebugUnitTest/ y app/build/reports/tests/testDebugUnitTest/.
- build/revision-android-70.json: resumen de esta verificación.
- build/instrumentadas-moto-reintento-70.log: ejecución física completa, OK (16 tests).
- build/resultado-final-70.log: comprobación final y mínimo de 70 %.

La prueba de navegación verifica inicio → administración → inicio → detalle → recepción → regreso con Hilt y ViewModels reales, sesión y repositorio simulados. No inicia sesión en Cognito ni registra asistencia. Las pruebas del editor comprueban reuso de recintos, zona, fechas, segundos y validaciones antes de enviar. Las pruebas de repositorio usan Room real con respuestas de API falsas para verificar la cola y sus confirmaciones.

Este cambio modifica únicamente configuración de pruebas, pruebas y documentación. No requiere actualizar la app personal ni EC2. Las instalaciones temporales de validación se retiraron del teléfono al terminar; el emulador temporal se cerró.

## Pendientes de validación real

La API Gateway respondió 503 a /api/health, /api/eventos, /api/sync/capabilities y al preflight CORS durante esta revisión. No se acredita como aprobada una prueba de conexión contra EC2.

Falta ejecutar con el servicio disponible:

- Escaneo de asistencia dentro y fuera del radio en ENFORCE, con ubicación física real y precisión suficiente.
- Confirmación en web de las creaciones offline, pérdida de respuesta y reintento sin duplicados contra la API desplegada.
- Validación de autorización por rol con sesiones reales, incluyendo 401/403 según corresponda.
- Recorrido visual manual con teclado, texto grande, rotación y pérdida real de conectividad. Los asserts automáticos no sustituyen esta revisión.

La cámara y Ubicación actual habían sido confirmadas por el usuario en el teléfono; eso no demuestra por sí solo los rechazos geográficos del backend. Este trabajo no cambia el backend ni registra asistencias de negocio.

## Comprobación tras encender el backend

El usuario confirmó que el backend estaba apagado durante la primera revisión. Al repetir la comprobación el 27-11-2026 (05:31 UTC), API Gateway respondió:

- GET /api/health: 200, status UP.
- GET /api/eventos sin token: 401.
- GET /api/sync/capabilities sin token: 401.
- OPTIONS /api/eventos desde https://syncpassweb.vercel.app: 200 con ese origen permitido.

Esto confirma disponibilidad, rechazo de esas consultas sin autenticación y respuesta del preflight para ese origen. No acredita aún autorización con sesiones reales, sincronización entre clientes ni asistencia dentro/fuera del radio. Las comprobaciones funcionales anteriores siguen pendientes; el bloqueo de disponibilidad quedó resuelto.
