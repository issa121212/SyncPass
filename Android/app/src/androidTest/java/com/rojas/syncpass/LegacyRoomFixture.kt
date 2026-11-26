package com.rojas.syncpass

import android.content.Context
import android.database.sqlite.SQLiteDatabase

/** Esquemas históricos explícitos: no se simula una versión vieja alterando una base nueva. */
internal fun createLegacyRoomFixture(context: Context, name: String, version: Int) {
    require(version == 1 || version == 3 || version == 5)
    context.getDatabasePath(name).parentFile?.mkdirs()
    SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
        db.execSQL("""CREATE TABLE proyectos(id INTEGER NOT NULL PRIMARY KEY, nombre TEXT NOT NULL, descripcion TEXT,
            areaTematica TEXT, unidadVecinal TEXT, fechaInicio TEXT, fechaFin TEXT, estado TEXT NOT NULL,
            responsableNombre TEXT, responsableEmail TEXT${if (version >= 2) ", responsableId INTEGER" else ""})""")
        db.execSQL("""CREATE TABLE eventos(id INTEGER NOT NULL PRIMARY KEY, proyectoId INTEGER NOT NULL,
            proyectoNombre TEXT NOT NULL, nombre TEXT NOT NULL, descripcion TEXT, fechaHora TEXT NOT NULL,
            cupos INTEGER NOT NULL, inscritos INTEGER NOT NULL, presentes INTEGER NOT NULL, recinto TEXT,
            direccion TEXT, latitud REAL, longitud REAL, radioMetros INTEGER NOT NULL, qrContenido TEXT,
            estado TEXT NOT NULL, inscrito INTEGER NOT NULL, esPresente INTEGER NOT NULL,
            encuestaRespondida INTEGER NOT NULL, actualizadoEn TEXT)""")
        db.execSQL("""CREATE TABLE perfil(id INTEGER NOT NULL PRIMARY KEY, usuarioId INTEGER NOT NULL,
            nombre TEXT NOT NULL, email TEXT NOT NULL, roles TEXT NOT NULL, qrContenido TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE pendientes(clientUuid TEXT NOT NULL PRIMARY KEY, tipo TEXT NOT NULL,
            eventoId INTEGER NOT NULL, codigoQr TEXT, marcadoEn TEXT NOT NULL, latitud REAL, longitud REAL,
            precisionMetros REAL, satisfaccion INTEGER, utilidad INTEGER, comentarios TEXT, creadoEn INTEGER NOT NULL,
            sincronizado INTEGER NOT NULL, error TEXT${if (version >= 3) ", rechazado INTEGER NOT NULL DEFAULT 0" else ""})""")
        if (version >= 2) {
            db.execSQL("""CREATE TABLE recintos_locales(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nombre TEXT NOT NULL, direccion TEXT NOT NULL, latitud REAL, longitud REAL, radioMetros INTEGER NOT NULL)""")
            db.execSQL("INSERT INTO recintos_locales(nombre,direccion,latitud,longitud,radioMetros) VALUES('Sede anterior','Calle 100',-33.45,-70.66,200)")
        }
        db.execSQL("INSERT INTO proyectos(id,nombre,descripcion,estado,responsableNombre) VALUES(7,'Proyecto anterior','Conservar descripción','PLANIFICADO','Ana')")
        db.execSQL("INSERT INTO perfil VALUES(1,3,'Ana','ana@example.invalid','DOCENTE','ESTUDIANTE_3')")
        db.execSQL("""INSERT INTO eventos(id,proyectoId,proyectoNombre,nombre,fechaHora,cupos,inscritos,presentes,
            latitud,longitud,radioMetros,estado,inscrito,esPresente,encuestaRespondida)
            VALUES(9,7,'Proyecto anterior','Evento anterior','2026-11-25T14:30:15',12,2,1,-33.45,-70.66,200,'PROGRAMADO',0,0,0)""")
        db.execSQL("""INSERT INTO pendientes(clientUuid,tipo,eventoId,codigoQr,marcadoEn,latitud,longitud,precisionMetros,creadoEn,sincronizado)
            VALUES('legacy-capture','ASISTENCIA_ESCANEO',9,'ESTUDIANTE_4','2026-11-25T14:30:00Z',-33.45,-70.66,8.5,1234,0)""")
        if (version == 5) {
            db.execSQL("ALTER TABLE pendientes ADD COLUMN ubicacionEn TEXT")
            for (table in listOf("proyectos", "eventos")) {
                db.execSQL("ALTER TABLE $table ADD COLUMN syncState TEXT NOT NULL DEFAULT 'CONFIRMADO'")
                db.execSQL("ALTER TABLE $table ADD COLUMN syncError TEXT")
            }
            db.execSQL("""CREATE TABLE creaciones(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                clientUuid TEXT NOT NULL, tipo TEXT NOT NULL, payload TEXT NOT NULL, proyectoId INTEGER,
                remoteId INTEGER, estado TEXT NOT NULL, error TEXT)""")
            db.execSQL("CREATE UNIQUE INDEX index_creaciones_clientUuid ON creaciones(clientUuid)")
            db.execSQL("""INSERT INTO creaciones(id,clientUuid,tipo,payload,estado) VALUES
                (1,'550e8400-e29b-41d4-a716-446655440000','PROYECTO','{"nombre":"Proyecto pendiente"}','PENDIENTE')""")
            db.execSQL("""INSERT INTO proyectos(id,nombre,estado,syncState) VALUES(-1,'Proyecto pendiente','PLANIFICADO','PENDIENTE')""")
            db.execSQL("UPDATE pendientes SET ubicacionEn='2026-11-25T14:29:59Z' WHERE clientUuid='legacy-capture'")
        }
        db.version = version
    }
}
