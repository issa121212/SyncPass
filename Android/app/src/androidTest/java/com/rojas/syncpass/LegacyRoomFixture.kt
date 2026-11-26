package com.rojas.syncpass

import android.content.Context
import android.database.sqlite.SQLiteDatabase

/** Esquemas históricos explícitos: no se simula una versión vieja alterando una base nueva. */
internal fun createLegacyRoomFixture(context: Context, name: String, version: Int) {
    require(version == 1 || version == 3)
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
        db.version = version
    }
}
