# Integración Android con Room y backend de ensayo

## Entorno

Pruebas ejecutadas en Pixel 10 emulado, Android 17/API 37. Se utiliza el paquete separado `com.rojas.syncpass.validation`, habilitado con `-PsyncpassTestInstall=true`; las pruebas que guardan sesiones o simulan la actualización comprueban ese identificador antes de operar. No se sobrescribe la instalación personal `com.rojas.syncpass`.

El servidor de ensayo corre únicamente en 127.0.0.1:18081 y utiliza la copia local migrada de PostgreSQL, nunca la base de EC2. Una utilidad de identidad en 127.0.0.1:18082 genera claves RSA efímeras y tokens ficticios de docente y participante. El backend conserva su validación normal de firma, issuer, expiración, cliente y rol, con parámetros locales explícitos. No se modifica la configuración de seguridad de producción ni se usa una cuenta Cognito real.

El emulador accede mediante `adb reverse` a esos dos puertos. Las pruebas usan URLs locales constantes en lugar de `BuildConfig.API_BASE_URL`. La simulación de desconexión intercepta la comprobación de capacidades y genera un fallo de red antes de enviar creaciones; no apaga la conectividad del usuario.

## Resultados de las seis pruebas instrumentadas

1. CRUD real de recintos y reapertura de Room.
2. Migración explícita de esquema V1 a V5, conservando catálogo y perfil.
3. Migración explícita de V3 a V5, conservando proyecto, evento, perfil, recinto y pendiente de asistencia. La fecha/hora original, UUID y precisión se conservan; la hora de ubicación nueva permanece vacía para no inventar evidencia.
4. Creación de proyecto/evento offline en Room, cierre y reapertura de la base, respuesta perdida después de guardar en servidor y reintento. Se confirma un único proyecto y evento, en ese orden, con sus relaciones e IDs remotos correctos.
5. Dos POST concurrentes con el mismo UUID devuelven el mismo ID; cambiar el cuerpo conservando el UUID recibe HTTP 409.
6. Participante sin permisos recibe 403 al crear proyectos. En el servidor local con geografía ENFORCE, la captura sin ubicación o fuera del radio recibe 422; una captura válida se registra una sola vez en el evento seleccionado.

Las seis pruebas aprobaron sin fallos ni omisiones. La prueba antigua que construía una falsa V1 borrando columnas de una base actual se reemplazó por esquemas históricos explícitos; al agregar V4/V5, el método anterior ya no representaba la versión original.

Lint de la variante de validación terminó sin errores. El JAR de producción se reconstruyó y se comprobó que no contiene los helpers del ensayo ni el servidor de identidad ficticia.

La geocerca se comprueba mediante coordenadas enviadas a la API. Estas pruebas no activan la cámara ni verifican la ubicación física del emulador. Cerrar y reabrir la base comprueba persistencia de SQLite, pero no equivale a simular toda muerte del proceso Android.

## Comandos

Con el contenedor de copia local disponible, el modo de ensayo activado y `SYNCPASS_REHEARSAL_PASSWORD` cargado desde su archivo privado, ejecutar desde `Backend/`:

```powershell
.\gradlew.bat androidRehearsalServer --console=plain
```

El servidor solo admite la base local `syncpass_real_rehearsal_20261125`; se deben seguir las precauciones de [Respaldo y ensayo de migraciones](13-ENSAYO-MIGRACIONES.md). La clave de firma y los tokens ficticios son efímeros y no se publican en Git.

Desde `Android/`, ajustando el serial al dispositivo autorizado:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 reverse tcp:18081 tcp:18081
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 reverse tcp:18082 tcp:18082
.\gradlew.bat :app:connectedDebugAndroidTest '-PsyncpassTestInstall=true' '-Pandroid.testInstrumentationRunnerArguments.syncpassRehearsal=true' '-Pandroid.testInstrumentationRunnerArguments.class=com.rojas.syncpass.RoomUpgradeTest,com.rojas.syncpass.RecintoRoomTest,com.rojas.syncpass.OfflineBackendIntegrationTest'
```

En PowerShell los argumentos `-P` con puntos se escriben entre comillas para evitar que se interpreten como nombres de tarea. Los reportes están en `Android/app/build/reports/androidTests/connected/debug/` y sus XML en `Android/app/build/outputs/androidTest-results/connected/debug/`.

## Ensayo de reemplazo de APK

`PackageUpgradeTest` permite dos ejecuciones explícitas, `upgradeStage=seed` y `upgradeStage=verify`. La primera requiere el APK anterior de `main` sin `AccountComponent` de Hilt, crea una base V3 ficticia y un marcador de preferencias. La segunda requiere el APK actualizado, verifica que el marcador sobrevivió y abre la base mediante las migraciones hasta V5.

El reemplazo se realiza con `adb install -r` sobre el paquete de validación. No se desinstala ni se ejecuta `pm clear` entre ambas fases. La base y el marcador del ensayo permanecen en esa instalación separada para inspección. El código no acepta ejecutar este procedimiento sobre el paquete personal.

Resultado: ambas fases aprobaron sobre `main` en `2bad889` y el APK local actualizado. La verificación constató preferencias conservadas, catálogo/perfil/recinto/pending anteriores y versión de Room 5. Es un escenario de actualización en dos fases, no dos comportamientos independientes para aumentar el conteo. La salida final está en `Android/app/build/reports/package-upgrade/verify.txt`.

Secuencia, después de compilar el APK de main y el APK nuevo con `-PsyncpassTestInstall=true`, y comprobar `applicationId` en sus `output-metadata.json`:

```text
adb -s emulator-5554 install -r <apk-anterior-de-validacion>
adb -s emulator-5554 install -r <apk-instrumentado-de-validacion>
adb -s emulator-5554 shell am instrument -w -r -e class com.rojas.syncpass.PackageUpgradeTest -e upgradeStage seed com.rojas.syncpass.validation.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5554 install -r <apk-nuevo-de-validacion>
adb -s emulator-5554 shell am instrument -w -r -e class com.rojas.syncpass.PackageUpgradeTest -e upgradeStage verify com.rojas.syncpass.validation.test/androidx.test.runner.AndroidJUnitRunner
```

No repetir `seed` sobre una base ya preparada: la prueba aborta para no sobrescribirla. La compilación de main se realizó en un worktree temporal separado, sin cambiar la rama de desarrollo ni incorporar cambios anteriores descartados.

## Alcance y pendientes

Las 88 pruebas unitarias y su cobertura de 19,48 % se mantienen como medición separada; estas pruebas instrumentadas no se suman para afirmar que se alcanzó el 30 % unitario. No hubo cambios de producción Android en esta etapa.

Las escrituras de ensayo se limitaron a registros ficticios agregados a la copia local. Producción permanece en V2, con su JAR anterior, y no recibió estos registros. Quedan la revisión visual de mapas, permisos/cámara/ubicación en teléfono, continuidad de una sesión real de Cognito al actualizar y el recorrido Android/web a través de API Gateway después de aprobar el despliegue.

Al finalizar se detuvieron el servidor de ensayo y el contenedor de PostgreSQL, y se retiraron únicamente los reenvíos adb de los puertos 18081/18082. El emulador y la instalación separada quedan disponibles. Los helpers de identidad y servidor están en el source set operativo; no forman parte del JAR de producción.
