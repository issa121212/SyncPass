# Prompt inicial: SyncPass Android

> **Nota documental:** Este texto es una reconstrucción del planteamiento inicial, no una transcripción histórica. Se conserva como documentación del proceso; sus instrucciones no se ejecutan al incorporarlo al repositorio.

Necesito hacer una aplicación Android que se llame SyncPass.

En el equipo tengo un archivo caso.md donde está la problemática y todo el contexto. Primero léelo para entender lo que necesitamos resolver.

La idea es gestionar proyectos comunitarios y eventos. Tendremos docentes, estudiantes o voluntarios y participantes, con opciones distintas según su rol.

El docente debe poder crear proyectos y eventos, editarlos, cancelarlos o eliminarlos cuando corresponda. No debería modificar proyectos de otro docente.

El participante debe poder consultar eventos, inscribirse y mostrar su QR. No quiero que apriete marcar asistencia y se marque solo: debe mostrar su QR para que un estudiante o receptor autorizado lo escanee dentro del evento correspondiente.

Necesito un CRUD de negocio mediante API y otro local. Para la API quiero proyectos y eventos. Para el local podemos usar recintos con nombre, dirección y ubicación, guardados con Room.

El login debe ser con Cognito. Quiero que el saludo muestre el name, no el ID de AWS. En las listas también deben aparecer nombres legibles.

El backend estará en la carpeta fs2-api, con Java y Spring Boot, y se desplegará en EC2. Me gustaría que después pueda utilizarlo también una aplicación web.

Quiero el código ordenado: controladores para rutas y delegación, servicios para lógica, repositorios para datos y DTO como records cuando corresponda. En Android también quiero separar pantallas, lógica y datos.

En los eventos necesito fecha y hora separadas. Al escribir 20261204 debe mostrarse 2026-12-04, y al escribir 1400 debe mostrarse 14:00.

Los proyectos deben verse para seleccionarlos, sin depender de un autocompletado poco intuitivo. Quiero reducir el scroll, por ejemplo organizando el formulario por pasos, sin impedir su uso en pantallas pequeñas.

La aplicación debe permitir guardar ciertas operaciones cuando no haya conexión y sincronizarlas después. Explícame cuáles pueden funcionar así y cuáles necesitan el backend.

Quiero ejecutar desde el botón Run de Android Studio, tanto en emulador como en celular. Con el backend remoto no debería depender de tenerlo abierto en mi computador.

No quiero exponer contraseñas, tokens ni claves. Deja ejemplos de configuración y explica los requisitos para ejecutar en otro equipo.

Trabajaremos por partes, probando las funcionalidades y organizándonos con Gitflow.

Al terminar necesito un README y documentación en docs sobre arquitectura, roles, requisitos, librerías, API, Room, despliegue, pruebas y uso de la aplicación.

Primero ordena esta idea, identifica las decisiones necesarias y propón cómo desarrollarla. No modifiques código todavía.
