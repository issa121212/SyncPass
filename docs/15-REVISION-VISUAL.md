# Revisión visual de Android

Se revisó el inicio visible de docente en Pixel 10 emulado. No se compartieron contraseñas y no se guardaron proyectos, eventos ni asistencias de prueba en producción. La comprobación de sesión vigente en el paquete principal se distingue de la inspección visual, pues coexistían una instalación principal y una de validación con el mismo nombre.

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

### Cámara sin permiso

Se abrió el lector desde el detalle de un evento con una cuenta docente. Con el permiso de cámara no concedido, la app mostró «Se necesita acceso a la cámara para leer el código QR» y el botón «Permitir cámara», manteniendo la navegación para volver. No se leyó ningún QR ni se registró una nueva asistencia. La vista previa tras aceptar el permiso y la cámara física siguen pendientes de comprobación; la respuesta al diálogo de privacidad se realiza manualmente.

Se corrigió la [restauración de sesiones vigentes](16-RESTAURACION-SESION.md) y se comprobó mediante pruebas unitarias y una actualización del APK principal después de un login real: la app recuperó el inicio de docente sin pedir otra autenticación. Siguen pendientes logout real, permiso y ubicación del dispositivo, vista previa tras conceder cámara y lectura QR en teléfono. La geocerca del servidor ya fue verificada en el entorno local descrito en [Integración Android](14-INTEGRACION-ANDROID.md).
