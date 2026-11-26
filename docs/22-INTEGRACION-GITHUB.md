# Integración en GitHub · 26-11-2026

Se publicaron las doce ramas locales de esta cadena en origin, sin force-push ni cambios en develop/main. Las referencias a trabajo «solo local» de documentos anteriores describen sus etapas de preparación; esta publicación es posterior y fue autorizada por el equipo.

## Primer paso integrado

[PR #7: migración a Hilt](https://github.com/issa121212/SyncPass/pull/7), desde `feature/inyeccion-hilt` hacia `develop`.

El PR contiene los tres commits de esa etapa. Su descripción distingue sus 48 pruebas unitarias registradas de las 100 pruebas y 45,22 % de cobertura de la entrega completa. No se atribuye a esta rama inicial una cobertura que todavía depende de cambios posteriores.

El usuario fusionó el PR #7 en develop mediante merge commit (10c047c). La integración se verificó por la API de GitHub y por la ascendencia de los commits. La corrección posterior de restauración de sesión debe integrarse también antes de preparar la release. No se presenta este paso intermedio como el APK final.

## Orden de las ramas publicadas

| Orden | Rama | Estado al publicar |
|---|---|---|
| 1 | feature/inyeccion-hilt | PR #7 fusionado en develop |
| 2 | feature/ubicacion-eventos | PR #8 fusionado en develop |
| 3 | feature/geolocalizacion-asistencia | PR #9 fusionado en develop |
| 4 | feature/creacion-offline | PR #10 fusionado en develop |
| 5 | chore/ensayo-migraciones | PR #11 fusionado en develop |
| 6 | test/integracion-android | PR #12 fusionado en develop |
| 7 | fix/validacion-visible-zona | PR #13 abierto hacia develop |
| 8 | fix/restauracion-sesion | Publicada; esperar integración anterior |
| 9 | fix/permiso-camara | Publicada; esperar integración anterior |
| 10 | feature/filtro-estado-eventos | Publicada; esperar integración anterior |
| 11 | feature/codigos-publicos | Publicada; incluye compatibilidad V6 |
| 12 | test/cobertura-minima-android | Publicada; incluye verificación mínima y esta guía |

Cada rama contiene a la anterior como ancestro. Se verificó esa relación antes de publicar. Las ramas locales descartadas de Supabase y de asistencia antigua no forman parte de esta subida.

## Cómo seguir

1. El colega revisa el PR #7 y deja observaciones o una aprobación real con su propia cuenta.
2. Resolver las observaciones antes de fusionar.
3. Usar **Create a merge commit** para integrar esta cadena. Squash/rebase cambiarían la relación entre los commits ya presentes en las ramas siguientes y ampliarían sus comparaciones. Si se elige otra estrategia, hay que reordenar las dependencias antes de continuar.
4. Actualizar la referencia de develop y abrir el siguiente PR hacia develop. Comprobar que la comparación muestra la etapa nueva, sin repetir cambios integrados.
5. Repetir revisión e integración en el orden de la tabla. No abrir doce PR acumulativos hacia develop a la vez.
6. Sobre develop integrado, ejecutar la compilación y `:app:check`, incluida la verificación de 30 % de líneas.
7. Preparar respaldo y despliegue específico de V5/V6 en EC2, manteniendo ENFORCE, y completar las pruebas físicas y de sincronización.
8. Preparar release, PR a main, tag y APK final después de la aprobación.

La publicación de ramas no modifica EC2 ni actualiza el teléfono. No se enviaron comentarios, revisiones ni aprobaciones en nombre del colega.

## Segundo paso abierto

[PR #8: ubicación de eventos](https://github.com/issa121212/SyncPass/pull/8), desde feature/ubicacion-eventos hacia develop. Se verificó que presenta solamente los tres commits de esta etapa, sin repetir la migración a Hilt. El usuario fusionó el PR #8 y se verificó su integración en develop (3959346).

## Tercer paso abierto

[PR #9: geolocalización de asistencia](https://github.com/issa121212/SyncPass/pull/9), desde feature/geolocalizacion-asistencia hacia develop. Incluye cuatro commits nuevos tras las dependencias ya integradas. El usuario fusionó el PR #9 y se verificó su integración en develop (c57a701).

## Cuarto paso abierto

[PR #10: creación offline](https://github.com/issa121212/SyncPass/pull/10), desde feature/creacion-offline hacia develop. Incluye cinco commits nuevos, con cola local y creaciones idempotentes en el backend. El usuario fusionó el PR #10 y se verificó su integración en develop (bc95a5e).

## Quinto paso abierto

[PR #11: respaldo y ensayo de migraciones](https://github.com/issa121212/SyncPass/pull/11), desde chore/ensayo-migraciones hacia develop. Incluye tres commits nuevos y no ejecuta operaciones en EC2 al integrarse. El usuario fusionó el PR #11 y se verificó su integración en develop (0753d1f).

## Sexto paso abierto

[PR #12: integración Android](https://github.com/issa121212/SyncPass/pull/12), desde test/integracion-android hacia develop. Incluye tres commits nuevos con fixtures Room y servidor local de prueba. El usuario fusionó el PR #12 y se verificó su integración en develop (c531be2).

## Séptimo paso abierto

[PR #13: validación de zona visible](https://github.com/issa121212/SyncPass/pull/13), desde fix/validacion-visible-zona hacia develop. Incluye un commit nuevo con la corrección visual, prueba instrumentada y documentación. Queda pendiente de revisión y fusión manual. Después corresponde fix/restauracion-sesion.
