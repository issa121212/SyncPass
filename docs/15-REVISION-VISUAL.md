# Revisión visual de Android

Se revisó la aplicación actualizada en Pixel 10 emulado con una sesión real de docente iniciada manualmente por el usuario. No se compartieron contraseñas y no se guardaron proyectos, eventos ni asistencias de prueba en producción.

## Comprobaciones

- El inicio muestra nombre, rol docente, acciones y catálogo.
- El editor conserva tres pasos con acciones principales visibles en el tamaño normal del emulador.
- Escribir `20261126` presenta `2026-11-26`; escribir `1430` presenta `14:30`.
- El mapa cargó cartografía OpenFreeMap, permitió seleccionar un punto y mostró centro y contorno de radio de 200 metros.
- Un radio cero impidió confirmar la zona.

## Error de validación visible

La revisión detectó que el mensaje de radio inválido quedaba parcialmente cortado al final del contenido desplazable del diálogo. Se movió el mensaje fuera de ese contenido y se agregó una región de accesibilidad `Polite` para anunciarlo sin interrumpir al lector de pantalla.

La prueba instrumentada `EventLocationDialogUiTest` comprueba que el alto visible del mensaje coincide con su alto completo, que un radio inválido no confirma y que puede corregirse a 250 metros. La prueba aprobó en el emulador. No se agregó una prueba de constructor/getter ni se mezcló esta medición con cobertura unitaria.

Se conserva el desplazamiento cuando el tamaño de pantalla, el teclado o la accesibilidad lo necesitan; los botones de confirmar y volver permanecen fuera del contenido desplazable. No se promete ausencia absoluta de scroll.

## Comprobaciones pendientes

La revisión de reapertura tras actualizar detectó que el inicio no recuperaba la sesión persistida. Se revisará la restauración en `SessionViewModel` antes de pedir un nuevo login. También quedan los diálogos reales de permisos, cámara física y lectura QR en teléfono. La geocerca del servidor ya fue verificada en el entorno local descrito en [Integración Android](14-INTEGRACION-ANDROID.md).
