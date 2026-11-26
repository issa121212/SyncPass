# Selección de ubicación de eventos

## Comportamiento

El tercer paso del formulario compartido de creación y edición permite elegir la zona del evento en un mapa. Tocar el mapa selecciona el centro; el radio se expresa en metros y se dibuja como un contorno geográfico. La selección se incorpora al formulario únicamente al pulsar «Usar esta zona». Volver conserva los datos anteriores del formulario.

«Usar mi ubicación» solicita permiso de ubicación únicamente al pulsar el botón. Se muestra la precisión informada para que la persona revise el punto antes de confirmarlo. Las solicitudes simultáneas se bloquean y seleccionar un punto manualmente cancela la búsqueda pendiente. Se rechazan coordenadas no finitas o fuera de rango, precisión no positiva y posiciones con antigüedad superior a dos minutos. La búsqueda tiene un límite de doce segundos.

Se conservan los recintos locales y la entrada de coordenadas avanzadas. Si se deniega el permiso, todavía puede elegirse un punto en el mapa; si el mapa no carga, pueden introducirse las coordenadas avanzadas. La ubicación sigue siendo opcional para mantener la compatibilidad con eventos existentes. El radio admite enteros entre 1 y 50000 metros, conforme al contrato actual.

El editor conserva sus tres pasos y los campos separados de fecha y hora. El diálogo del mapa limita su altura y permite desplazamiento cuando la pantalla, el teclado o la accesibilidad lo requieren.

## Responsabilidades

- `EventoEditorDialog`: conserva el borrador y recibe la zona confirmada.
- `EventLocationDialog`: presenta controles y solicita permisos.
- `EventZoneMap`: integra el ciclo de vida de MapLibre, la selección y el contorno.
- `EventLocationViewModel`: mantiene el estado, evita solicitudes simultáneas y valida antes de confirmar.
- `LocationSource`: contrato de dominio para obtener la ubicación sin depender de Android.
- `AndroidLocationSource`: consulta los proveedores del dispositivo y cancela la solicitud al abandonar la operación.
- `GeoPoint`, `EventZone` y `DeviceLocation`: validan coordenadas, radio y vigencia de la posición.
- `LocationModule`: enlaza la implementación mediante Hilt e inyecta un reloj reemplazable en pruebas.

La selección temporal se conserva en el ViewModel durante una recreación de la pantalla. No se promete recuperación de esa selección tras la muerte del proceso.

## Proveedor y compatibilidad

Se utiliza MapLibre Native 13.7.1 con el estilo público Liberty de OpenFreeMap. Este servicio no requiere una clave de API; se mantiene la atribución del mapa. La carga requiere conexión y depende de un proveedor externo. No se implementan descargas masivas ni mapas sin conexión. La ubicación seleccionada se envía al backend al guardar el evento mediante los campos existentes de latitud, longitud y radio.

Referencias: [MapLibre Native](https://maplibre.org/maplibre-native/android/examples/getting-started/) y [OpenFreeMap](https://openfreemap.org/quick_start/).

Esta función depende de la migración a Hilt y se desarrolla en `feature/ubicacion-eventos`, creada sobre `feature/inyeccion-hilt`. La integración debe respetar ese orden hacia `develop`. No modifica Room, Flyway, PostgreSQL ni el contrato HTTP compartido con la web. No cambia los permisos de creación o edición de eventos.

## Validación y límites

Las pruebas unitarias utilizan una fuente de ubicación falsa, un reloj fijo y un dispatcher controlado. Comprueban selección y confirmación, radios inválidos, posiciones antiguas o inválidas, errores, permiso denegado, conservación del estado y cancelación de resultados pendientes. El contorno se contrasta mediante distancias calculadas de forma independiente.

La verificación visual del mapa, los permisos reales y los sensores requiere una prueba manual en dispositivo o emulador. Compilar el APK de pruebas no equivale a ejecutarlas.

Comprobación local del 25-11-2026: 58 pruebas unitarias aprobadas, sin fallos ni errores; APK de aplicación y APK de pruebas compilados; lint sin errores. El reporte unitario de AGP cuenta 671 líneas cubiertas de 3986 (16,83 %), incluyendo interfaz y código generado. La meta del 30 % todavía no se alcanza con ese denominador; no se han añadido exclusiones para elevar el porcentaje. Se generó con `:app:createDebugUnitTestCoverageReport` y puede consultarse en `Android/app/build/reports/coverage/test/debug/index.html`.

Este cambio permite definir la zona. La feature de [validación geográfica de asistencia](11-GEOLOCALIZACION-ASISTENCIA.md) incorpora la comprobación de distancia y su activación compatible; [Creación offline](12-CREACION-OFFLINE.md) conserva estas coordenadas en solicitudes pendientes de proyecto y evento.
