# Room, sincronización y experiencia de usuario

## Qué significa guardar localmente

Room es una capa sobre SQLite dentro de Android. Permite consultar caché y persistir datos/pendientes entre aperturas de la app. No es el backend, no reemplaza PostgreSQL y no comparte automáticamente información con otro teléfono.

`AppContainer` y `SyncPassDatabase` seleccionan una base local según la cuenta (issuer/sub). Al cambiar de usuario no deben mezclarse catálogos ni pendientes de cuentas diferentes. Una desinstalación o borrado de datos puede eliminar información exclusivamente local: no es un respaldo del servidor.

## Capacidades concretas

| Información/acción | Persistencia | Conectividad |
|---|---|---|
| Catálogo de proyectos/eventos y perfil | Caché Room de respuestas remotas | Consulta local después de descarga; actualizar requiere red |
| CRUD de proyectos | API y actualización de caché | Requiere servidor; no existe cola de creación masiva offline |
| Crear/editar/eliminar/cancelar evento | API y caché confirmada | Requiere conexión con el servidor |
| CRUD de recintos | Room | Funciona localmente |
| Inscripción | API; en fallos reintentables, pendiente local | La aceptación remota depende de cupos/estado al sincronizar |
| Escaneo de asistencia | Cola local antes de enviar, luego API | La captura puede quedar pendiente; no equivale a confirmada |
| Encuesta | Cola local y marca local, luego API | Puede quedar pendiente o rechazada |
| Listas de asistentes y encuestas | Consulta API en el detalle | No hay persistencia local completa de esas listas |
| Primer login | Cognito y descarga de perfil | Requiere conexión |

## Actualizar frente a Sincronizar

Actualizar descarga perfil, proyectos y eventos del backend y renueva la caché. Si el servidor falla, conserva lo descargado y muestra el error. No sube supuestos proyectos locales que nunca fueron creados en la API.

Sincronizar procesa la cola de inscripciones, asistencias y encuestas. El repositorio prioriza inscripción, luego asistencia y finalmente encuesta. Usa un Mutex para evitar procesamiento concurrente en las rutas protegidas. Las inscripciones se vuelven a enviar de forma idempotente y la asistencia utiliza clientUuid para reconocer reintentos.

Los errores de red, 408, 429 y 5xx se consideran reintentables. Los errores de negocio se marcan rechazados y se conservan para informar al usuario; una sesión inválida exige resolver la autenticación. Si una operación de un evento falla, no se continúa indiscriminadamente con operaciones dependientes del mismo evento durante ese recorrido.

Después del éxito se limpian los pendientes sincronizados. El backend decide con sus datos actuales: un cupo disponible cuando se capturó algo sin conexión puede haberse ocupado antes del envío. No hay garantía de aceptación de todos los pendientes.

## Trabajo de fondo

`SyncScheduler` programa trabajo periódico de WorkManager cada 15 minutos con red conectada y permite una solicitud inmediata. `SyncWorker` verifica una sesión válida antes de llamar al repositorio.

Android decide cuándo ejecutar el trabajo según batería, restricciones y ciclo del sistema. Quince minutos no es un cronómetro exacto y recuperar la señal no garantiza ejecución instantánea. El usuario puede solicitar Sincronizar; el catálogo se actualiza por la ruta de refresco correspondiente.

## Edición y eliminación de eventos

El repositorio valida los datos de entrada antes de invocar la API, conserva cupos compatibles con los inscritos y evita modificar Room antes del éxito remoto. La eliminación local se realiza después del DELETE y se bloquea si existen operaciones locales pendientes incompatibles para ese evento. El backend puede devolver 409 aunque la caché inicialmente no mostrara historial.

Cancelar refresca el evento y conserva sus datos al cambiar el estado. No equivale a borrar inscripciones ni asistencias. Los estados de guardado y una protección de mutación evitan envíos repetidos en el detalle.

## Diseño de formularios

El editor compartido de creación/edición distribuye la información en tres pasos. El primero ofrece proyectos como opciones visibles, nombre y descripción; el segundo fecha/hora/cupos y estado al editar; el tercero ubicación y recinto local cuando se provee esa selección.

`FechaCampo` y `FormatoFechaHora` mantienen los dígitos y aplican separadores visuales: 20261204 -> 2026-12-04 y 1400 -> 14:00. La validez de calendario y hora se comprueba además del formato. Al editar se conservan segundos/fracciones si el usuario no cambió día ni minuto.

El botón principal permanece fuera del contenido desplazable. La descripción limita su altura visible. Se utiliza rememberSaveable para conservar campos durante recreaciones compatibles. La edición mantiene errores y datos si el servidor falla; la creación conserva el manejo de mensajes globales de Home, por lo que no se debe prometer idéntica recuperación de formulario para ambos casos.

## Scroll, accesibilidad y colores

La meta es reducir el desplazamiento innecesario, no eliminar el scroll. El editor limita la altura del contenido y permite desplazamiento para teclado abierto, texto grande o listas de proyectos extensas. El inicio usa lista diferida y el detalle puede requerir desplazamiento por asistentes/encuestas.

Material 3 proporciona componentes, tema claro/oscuro y tamaños de interacción base. Sigue siendo necesario validar lector de pantalla, contraste y texto ampliado en dispositivos reales. `Theme.kt` habilita color dinámico en Android 12 o superior; los nombres históricos Purple/Pink del fallback no describen una paleta azul fija. La apariencia azul observada depende del esquema seleccionado por el dispositivo. No se cambió ese comportamiento para documentarlo.

## Ubicación

`UbicacionHelper` consulta la última ubicación conocida si existe permiso, incluyendo precisión cuando está disponible. No fuerza una lectura nueva ni garantiza actualidad absoluta. La API almacena coordenadas referenciales; el código revisado de escaneo no aplica una validación de distancia al radio del evento. Esa diferencia con el objetivo del caso debe conservarse en las limitaciones.
