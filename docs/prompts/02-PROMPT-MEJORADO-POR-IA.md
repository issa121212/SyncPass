# Prompt de desarrollo: SyncPass Android

> **Nota documental:** Este texto es una reconstrucción de la organización y mejora del planteamiento inicial con ayuda de IA, no una transcripción histórica. Se conserva como documentación del proceso; sus instrucciones no se ejecutan al incorporarlo al repositorio.

Quiero desarrollar SyncPass, una aplicación Android para gestionar proyectos comunitarios, eventos, inscripciones, asistencia y encuestas.

Esta solicitud organiza el planteamiento inicial con ayuda de IA.

Primero lee caso.md y utiliza su problemática, actores y restricciones como contexto. No inventes necesidades ni presentes las metas del caso como resultados ya comprobados.

Antes de implementar, presenta la arquitectura, el modelo de datos y un plan por etapas.

## 1. Tecnologías

Utiliza:

- Kotlin.
- Jetpack Compose y Material 3.
- ViewModel.
- Corrutinas y Flow/StateFlow.
- Room para persistencia local.
- Retrofit y OkHttp para consumir la API.
- AppAuth para autenticación con Cognito.
- WorkManager para sincronización diferida.
- Bibliotecas apropiadas para generar y escanear QR.

El backend estará en fs2-api y utilizará Java, Spring Boot, Spring Security, PostgreSQL y migraciones.

Selecciona versiones compatibles y justifica las dependencias principales.

## 2. Arquitectura Android

Utiliza MVVM con repositorios y separación entre presentación, dominio y datos.

Presentación:

- Pantallas Compose.
- Componentes reutilizables.
- Navegación.
- ViewModels.
- Estados de carga, guardado, datos y errores.

Dominio:

- Modelos de negocio.
- Contratos de repositorios.
- Validaciones y errores de aplicación.

Datos:

- Repositorios.
- Cliente HTTP.
- DTO de solicitudes y respuestas.
- Mapeadores.
- Entidades Room.
- DAO.
- Base de datos y migraciones.
- Sesión y operaciones pendientes.

Las pantallas no deben ejecutar SQL ni solicitudes HTTP directamente.

Los ViewModels deben coordinar las acciones de la pantalla. Los repositorios deben gestionar el acceso a las fuentes de datos.

Evita capas o archivos que no tengan una responsabilidad clara.

## 3. Arquitectura del backend

Organiza fs2-api mediante:

- Controladores para rutas, validación de entrada y delegación.
- Servicios para reglas de negocio y transacciones.
- Repositorios para acceso a datos.
- Entidades de persistencia.
- DTO definidos como records cuando corresponda.
- Seguridad.
- Configuración.
- Manejo centralizado de errores.
- Migraciones.

No concentres las reglas del negocio en los controladores.

El servidor debe validar permisos, propiedad, cupos, estados y relaciones. No confíes únicamente en la interfaz Android.

Prepara el backend para que pueda compartirlo posteriormente una aplicación web.

## 4. Autenticación y sesión

Implementa login con Cognito mediante código de autorización y PKCE.

La pantalla inicial debe ser sencilla, con Continuar con Cognito.

Gestiona:

- Apertura de la página de autenticación.
- Retorno a Android.
- Sesión.
- Expiración.
- Errores.
- Cierre de sesión.

Utiliza el atributo name para el saludo. No muestres el identificador técnico como nombre.

Envía el access token a la API. No muestres tokens ni contraseñas en la interfaz o los registros.

Protege la sesión almacenada y explica la política de renovación o nuevo inicio de sesión.

## 5. Roles y dashboards

Adapta el inicio y las acciones según el rol.

### Docente

Debe poder:

- Consultar proyectos y eventos.
- Crear proyectos.
- Editar y eliminar proyectos que administra.
- Crear eventos en sus proyectos.
- Editar, cancelar y eliminar eventos cuando las reglas lo permitan.
- Consultar inscritos, asistentes y encuestas.
- Administrar recintos locales.

### Estudiante o voluntario

Debe poder:

- Consultar eventos.
- Seleccionar el evento de recepción.
- Escanear el QR de participantes.
- Registrar asistencia según sus permisos.

### Participante

Debe poder:

- Consultar eventos.
- Inscribirse.
- Consultar su estado.
- Mostrar su QR.
- Consultar su asistencia.
- Responder encuestas cuando corresponda.

El participante no debe confirmar su propia asistencia.

Define las equivalencias entre grupos Cognito y roles internos. Si existe ADMIN, diferencia sus permisos.

## 6. CRUD de proyectos mediante API

Implementa crear, consultar, editar y eliminar.

Utiliza datos del negocio como nombre, descripción, área temática, unidad vecinal, fechas, estado y responsable.

Muestra nombres legibles en la interfaz.

Comprueba la propiedad antes de modificar y define qué ocurre al eliminar un proyecto con eventos asociados.

## 7. CRUD de eventos mediante API

Implementa:

- Crear.
- Consultar.
- Editar.
- Eliminar.
- Cancelar mediante cambio de estado.

Cada evento tendrá proyecto, nombre, descripción, fecha y hora, cupos, estado y ubicación opcional.

Valida campos obligatorios, longitudes, fechas, horas, cupos, coordenadas y radio.

No permitas reducir cupos por debajo de las inscripciones existentes cuando corresponda.

Verifica permisos sobre el proyecto original y el de destino si se cambia la asociación.

Cancelar debe conservar el historial.

Eliminar debe pedir confirmación y respetar las restricciones de inscripciones, asistencias y operaciones locales pendientes.

Actualiza la copia local después de recibir confirmación del servidor en edición y eliminación. Si la API falla, no presentes éxito ni borres información local como si hubiera funcionado.

## 8. CRUD local con Room

Implementa un CRUD de recintos con:

- Nombre.
- Dirección.
- Coordenadas opcionales.
- Radio.

Utiliza Entity, DAO, RoomDatabase, repositorio y migraciones.

Debe permitir crear, consultar, editar y eliminar sin depender del backend.

Los recintos podrán utilizarse para completar la ubicación de un evento.

Diferencia este catálogo local de la información compartida por API. No debe aparecer automáticamente en otro dispositivo.

Separa los datos locales por cuenta para evitar mezclar información entre usuarios.

## 9. Inscripciones y asistencia

Implementa inscripción con validación de cupos, estado y duplicados.

Inscribirse no debe marcar presente.

El flujo de asistencia debe ser:

1. El participante muestra su QR.
2. Un receptor autorizado abre el evento correspondiente.
3. Escanea el QR.
4. La aplicación guarda y envía la operación según el flujo definido.
5. El backend valida y registra.
6. La interfaz distingue confirmación de operación pendiente.

La asistencia debe registrarse únicamente en el evento seleccionado.

Controla duplicados y reintentos mediante identificadores de operación y validaciones del servidor.

El QR no debe contener contraseñas ni tokens.

Gestiona permisos de cámara y libera los recursos al abandonar el escáner.

La ubicación será referencial. No presentes una validación de distancia como implementada si no existe.

## 10. Encuestas

Permite responder encuestas cuando el participante cumpla las condiciones del backend.

Incluye satisfacción, utilidad y comentarios.

Valida rangos y longitudes.

El docente responsable debe poder consultar los resultados disponibles.

Respeta las reglas definidas para guardar o actualizar respuestas.

## 11. Sin conexión y sincronización

Conserva el catálogo necesario en Room.

Implementa una cola para las operaciones que lo requieran, como inscripción, asistencia y encuestas.

Distingue:

- Pendiente.
- Confirmada.
- Rechazada.

Evita duplicados y separa errores transitorios de rechazos de negocio.

Define el orden de envío de operaciones dependientes.

Utiliza WorkManager con restricciones adecuadas y permite sincronización solicitada por el usuario.

Diferencia Actualizar de Sincronizar.

No afirmes que todos los CRUD funcionan offline. La administración remota de proyectos y eventos puede requerir conexión.

No muestres una captura pendiente como asistencia confirmada por el servidor.

## 12. Formularios y experiencia de usuario

Mantén una interfaz clara y consistente, con colores azules y fondos claros como referencia visual.

Separa fecha y hora.

Al ingresar 20261204, muestra 2026-12-04. Al ingresar 1400, muestra 14:00.

Mantén correctamente el cursor y el borrado. Valida el calendario y la hora además del formato.

Muestra los proyectos disponibles como opciones visibles.

Organiza el formulario de eventos en tres pasos:

1. Proyecto y datos generales.
2. Fecha, hora, cupos y estado cuando corresponda.
3. Ubicación.

Mantén accesibles Atrás, Siguiente, Cancelar y Guardar.

Reduce el scroll innecesario sin ocultar contenido en pantallas pequeñas, con teclado abierto o texto ampliado.

Incluye mensajes claros, estados de carga y prevención de envíos repetidos. Conserva el formulario de edición cuando falle la API.

## 13. Seguridad y errores

El backend debe diferenciar:

- 400: datos inválidos.
- 401: autenticación ausente o inválida.
- 403: permisos insuficientes.
- 404: recurso inexistente.
- 409: conflicto.
- 422: reglas de negocio cuando corresponda.
- Errores transitorios del servidor.

No expongas trazas ni secretos en las respuestas.

Mantén fuera de Git contraseñas, tokens, claves privadas y archivos de entorno sensibles.

Incluye .env.example y explica qué valores son públicos y cuáles son secretos.

## 14. Ejecución y despliegue

Permite ejecutar desde Run de Android Studio en emulador o teléfono físico.

Explica la conexión con:

- Backend local.
- Emulador.
- Teléfono por USB.
- Backend remoto.

Si se necesita automatización local, centralízala en PREPARAR_RUN.ps1.

Con el backend remoto no debe ser necesario mantener un servidor abierto en el computador.

Despliega el backend en EC2 con PostgreSQL, servicio del sistema y HTTPS, utilizando API Gateway y Nginx según la arquitectura acordada.

Considera AWS Academy y el presupuesto disponible. No crees recursos con costo sin autorización.

No agregues APK generadas a la raíz del código fuente.

## 15. Pruebas y Gitflow

Implementa pruebas de validación, repositorios, errores, permisos y formularios.

Comprueba Room, el CRUD API, los reintentos y la conservación de información cuando falla el servidor.

Ejecuta compilación, pruebas y análisis estático.

Realiza pruebas manuales con datos ficticios y comprueba que los cambios remotos sean visibles desde otro cliente cuando corresponda.

Organiza el trabajo con main, develop, ramas feature y Pull Requests para revisión entre integrantes. Utiliza release y tags para las entregas.

No publiques ni fusiones sin indicación. Quiero revisar localmente antes.

## 16. Documentación final

Al terminar, revisa el código y genera un README principal y documentación dentro de docs.

Incluye:

- Contexto de caso.md.
- Requisitos funcionales y no funcionales.
- Arquitectura Android y backend.
- Responsabilidad de cada capa.
- Modelos de datos.
- Roles y dashboards.
- CRUD local y CRUD API.
- Flujo QR y encuestas.
- Room y sincronización.
- Tecnologías, versiones y justificación.
- Ejecución y despliegue.
- Pruebas y resultados reales.
- Organización Gitflow.
- Apoyo de IA y validación del equipo.
- Límites de la entrega.

Redacta la documentación para el sistema terminado, sin notas temporales sobre fusiones pendientes.

No inventes funcionalidades, resultados de pruebas ni mediciones de impacto.

Trabajemos por etapas. En cada una explica qué se implementó, qué cambió y cómo se comprobó.
