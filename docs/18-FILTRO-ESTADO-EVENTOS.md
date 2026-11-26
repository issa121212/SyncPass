# Filtro de eventos por estado

Android incorpora un selector compacto sobre Eventos y talleres con Todos, Programado, En curso, Finalizado y Cancelado. Corresponden a los estados reales de la API. Muestra la cantidad de resultados y distingue un catálogo vacío de un filtro sin coincidencias.

HomeViewModel mantiene la selección en SavedStateHandle y produce eventosVisibles a partir del catálogo local. Cambiar el filtro no llama a la API ni escribe en Room. Las emisiones nuevas del repositorio recalculan los resultados conservando orden, rol y estado de sincronización. Los ID siguen utilizándose solo como referencias internas y claves de la lista.

HomeScreen presenta el selector y los resultados. No se agrega paginación; LazyColumn conserva desplazamiento vertical cuando hace falta. El selector evita una fila horizontal de opciones, pero no pretende eliminar todo desplazamiento de la pantalla.

Las pruebas JUnit FiltroEstadoEventoTest usan un repositorio falso y verifican todos los estados, la actualización del catálogo, los pendientes, la ausencia de nuevas consultas remotas y la selección restaurada con resultado vacío.

La rama feature/filtro-estado-eventos parte de fix/permiso-camara: develop todavía no incluye la cadena de mejoras locales previas. Deben integrarse las dependencias primero, sin perderlas ni publicar main anticipadamente. No se sube a GitHub ni se despliega en EC2 como parte de esta mejora.

Validación: suite unitaria Android aprobada, APK debug compilado y lint sin errores. La comprobación visual se realizará con el conjunto final de ambas mejoras.
