# Restauración de sesión al reabrir Android

## Problema y solución

`SessionStore` ya guardaba los tokens cifrados, pero `SessionViewModel` solo abría el área privada cuando recibía una autenticación nueva en memoria. Tras recrear el proceso o actualizar el APK, ese estado de autenticación no existía y el inicio no recuperaba por sí mismo una sesión persistida vigente.

El ViewModel ahora consulta la validez de la sesión persistida al construirse. Si es válida, crea un nuevo almacén de pantallas para la misma cuenta sin reescribir los tokens ni reiniciar su vigencia. Si está vencida o ausente, mantiene el login. No se agregan refresh tokens ni se prolonga artificialmente la expiración de Cognito.

Al destruir el ViewModel se liberan las pantallas, pero se conservan las dependencias de la cuenta para que la sincronización de fondo y una nueva Activity compartan el mismo coordinador. Cerrar sesión o recibir otra autenticación sí descarta las dependencias correspondientes, conforme al aislamiento existente.

Para cerrar una sesión restaurada, `MainActivity` obtiene el ID token persistido antes de limpiar la sesión y lo pasa a `CognitoAuthViewModel.startLogout`. El módulo de autenticación mantiene su llamada Kotlin sin argumentos como opción compatible para los consumidores que ya tenían tokens en memoria.

## Validación

Se agregaron pruebas unitarias de arranque con sesión vigente, recreación sin borrar ni reescribir tokens, liberación de pantallas al cerrar sesión y prevención de reapertura de una sesión cerrada o vencida. No son pruebas de getters: verifican el ciclo de sesión, su persistencia y el acceso al área privada.

Suite Android: 91 pruebas aprobadas, sin fallos ni errores. Cobertura unitaria: 948 de 4839 líneas (19,59 %), conservando el alcance anterior con UI y fuentes generadas. La meta de 30 % sigue pendiente. Lint y compilación terminaron correctamente; el APK se instaló sobre la app principal del emulador sin desinstalarla ni borrar sus datos.

La comprobación manual debe distinguir la app principal `com.rojas.syncpass` de la instalación separada de validación. Se detectó que la principal conservaba una sesión guardada el 21-11-2026, ya vencida; no corresponde restaurarla como autenticación vigente. Un nuevo login manual y posterior reapertura de la app principal permitirán verificar el caso real de Cognito. No se extrajeron ni mostraron tokens o contraseñas para diagnosticarlo; solo se consultó presencia de sesión cifrada y metadatos de vigencia.

La corrección no cambia esquemas Room, migraciones de PostgreSQL, contratos HTTP ni seguridad del servidor. Producción sigue con su despliegue anterior.
