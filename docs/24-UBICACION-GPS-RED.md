# Ubicación actual: GPS y red

En el Moto g82 los permisos estaban concedidos, pero SyncPass consultaba únicamente GPS y vencía a los 12 segundos sin recibir posición.

Android consulta ahora GPS y red disponibles en paralelo. Acepta la primera ubicación válida y cancela las otras solicitudes. Espera un máximo de 20 segundos. Si un proveedor falla o devuelve datos inválidos, espera al otro. Salir de la pantalla también cancela las solicitudes.

La caché se filtra antes de elegir la posición más reciente. Se mantienen las validaciones de coordenadas, precisión positiva y antigüedad máxima de dos minutos. Sin permiso preciso solo se consulta red. El backend y ENFORCE se mantienen: una ubicación de red no garantiza cumplir la precisión ni el radio para registrar asistencia.

JUnit con fuentes falsas verifica red sin GPS, GPS sin red, datos antiguos o inválidos, vencimiento, cancelación y resultados nulos. Verificación física: se instaló la APK conservando los datos en el Moto g82. El usuario confirmó que «Ubicación actual» obtiene correctamente su posición. Esta comprobación no valida por sí sola el registro de asistencia ni el rechazo fuera del radio.

Validación local: `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` completados correctamente; 106 pruebas locales aprobadas, incluidas seis pruebas nuevas de selección y cancelación de fuentes.
