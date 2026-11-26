# Inyección de dependencias con Hilt

## Base de trabajo

La feature parte de `develop` en `f772d38`. Su código funcional coincide con `main` en `2bad889`; las diferencias publicadas previamente corresponden a documentación, configuración de cobertura y pruebas. Las features locales descartadas permanecen fuera de esta entrega.

## Responsabilidades y alcance

La aplicación utiliza Hilt 2.60.1 y la integración de ViewModels Compose de AndroidX Hilt 1.4.0. Los plugins y procesadores utilizan KSP. `SyncPassApplication` tiene `@HiltAndroidApp` y `MainActivity` tiene `@AndroidEntryPoint`.

Los ViewModels de inicio, gestión, detalle, escáner, sesión y Cognito utilizan `@HiltViewModel` e inyección por constructor. El detalle recibe el identificador de ruta mediante `SavedStateHandle`. La navegación obtiene sus ViewModels mediante `hiltViewModel()`.

`SessionModule` proporciona la sesión cifrada y la configuración de Cognito. `CognitoModule` crea un administrador AppAuth por ViewModel de autenticación. El administrador se libera con ese ViewModel.

## Dependencias por cuenta

`AccountComponent` es un componente Hilt hijo del componente de aplicación, con alcance `AccountScoped`. Recibe la clave `issuer|sub` y proporciona la base Room de esa cuenta, la API y las operaciones del repositorio.

`AccountDependencies` mantiene el conjunto activo mediante `AccountScopeRegistry`. El acceso comprueba que exista una sesión válida. Al cambiar de cuenta se construye otro conjunto. Al cerrar la sesión se descarta la referencia activa.

`SessionViewModel` mantiene un `ViewModelStore` para el área privada. Al recrearse la Activity por una configuración, conserva el store. Al cerrar sesión o recibir otra autenticación, libera las pantallas anteriores y sus scopes. El acceso HTTP utiliza un snapshot del token validado para la cuenta solicitante mediante `SessionStore.accessTokenFor`.

Cada ViewModel recibe las operaciones de su cuenta al construirse. `SyncWorker` obtiene la sesión y el gestor de cuentas mediante un entry point de Hilt antes de procesar la cola. El interceptor bloquea solicitudes cuando el token disponible corresponde a otra cuenta o ya no es válido.

## Persistencia y compatibilidad

Se conservan la versión 3 de Room, los nombres de las bases derivados de la cuenta y las migraciones existentes. El contrato HTTP y el esquema PostgreSQL se mantienen. La feature no requiere un despliegue del backend.

Se actualizó la construcción del ViewModel en el test instrumentado de gestión para conservar la compilación de la suite existente. La ejecución de pruebas instrumentadas y de login con dos cuentas se reserva para la etapa de integración.

## Pruebas unitarias

Después de implementar el cambio, las pruebas agregadas comprueban:

- Reutilización y descarte de dependencias al cambiar de cuenta o cerrar sesión.
- Creación concurrente de un único conjunto por cuenta.
- Conservación de ViewModels ante la misma autenticación y liberación al cambiarla.
- Autorización HTTP ligada a la cuenta.
- Estado de inicio con datos locales cuando falla una actualización.
- Mensajes de sincronización y conservación de pendientes.
- Prevención de envíos simultáneos desde gestión.
- Conservación del formulario ante errores.
- Lectura del identificador de ruta en el detalle.

Se utilizan JUnit, Mockito y `kotlinx-coroutines-test` con datos ficticios y respuestas controladas. La cobertura unitaria se informa por separado de los resultados instrumentados históricos.

Desde `Android/`, ejecutar:

```powershell
.\gradlew.bat :app:createDebugUnitTestCoverageReport :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

El informe unitario se genera en `app/build/reports/coverage/test/debug/index.html`. Los diagramas C4 se revisarán al terminar las features aprobadas.

## Resultado de esta etapa

- Pasaron 48 pruebas unitarias, incluidas 13 nuevas sobre sesión, aislamiento y ViewModels.
- La APK debug y el APK de pruebas instrumentadas se compilaron correctamente.
- Lint terminó con 0 errores y 33 advertencias.
- El informe unitario completo registró 584 de 3.720 líneas cubiertas, un 15,70 %. Incluye las pantallas y las clases generadas que recopila AGP. Se conserva este alcance para comparar las siguientes etapas.

La cobertura unitaria todavía queda por debajo del objetivo del 30 %. Las siguientes features incorporarán pruebas de ubicación, cola de creación y errores. Los porcentajes combinados de pruebas instrumentadas de la entrega anterior se conservan como resultados históricos.

Quedan pendientes las verificaciones de integración de login con dos cuentas y de navegación en dispositivo. El armado del grafo de Hilt se valida durante la compilación; los tests de esta etapa utilizan dependencias falsas para comprobar el comportamiento de los consumidores.
