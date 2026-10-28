package com.rojas.syncpass

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rojas.syncpass.data.local.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class RecintoRoomTest {
    @Test fun localCrudPersistsAfterReopeningDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "crud-test-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, SyncPassDatabase::class.java, name)
            .addMigrations(SyncPassDatabase.MIGRATION_1_2, SyncPassDatabase.MIGRATION_2_3).build()
        var db = open()
        try {
            val id = db.recintoDao().crear(RecintoEntity(nombre = "Sede", direccion = "Calle 100"))
            assertEquals("Sede", db.recintoDao().obtener(id)?.nombre)
            db.close(); db = open()
            val original = requireNotNull(db.recintoDao().obtener(id))
            assertEquals("Calle 100", original.direccion)
            assertEquals(1, db.recintoDao().editar(original.copy(nombre = "Sede editada")))
            assertEquals("Sede editada", db.recintoDao().observar().first().single().nombre)
            assertEquals(1, db.recintoDao().eliminar(id))
            assertNull(db.recintoDao().obtener(id))
            assertTrue(db.recintoDao().observar().first().isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun migrationPreservesExistingCatalogAndProfile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, SyncPassDatabase::class.java, name).build()
        try {
            db.dao().guardarProyectos(listOf(ProyectoEntity(7, "Proyecto anterior", null, null, null,
                null, null, "PLANIFICADO", "Ana", null)))
            db.dao().guardarPerfil(PerfilEntity(usuarioId = 3, nombre = "Ana", email = "ana@example.com",
                roles = "DOCENTE", qrContenido = "ESTUDIANTE_3"))
            db.close()
            // Recreate the precise schema difference of v1; Room validates v2 after migration.
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("DROP TABLE recintos_locales")
                it.execSQL("ALTER TABLE pendientes DROP COLUMN rechazado")
                it.execSQL("ALTER TABLE proyectos DROP COLUMN responsableId")
                it.execSQL("UPDATE room_master_table SET identity_hash = 'legacy-v1'")
                it.version = 1
            }
            db = Room.databaseBuilder(context, SyncPassDatabase::class.java, name)
                .addMigrations(SyncPassDatabase.MIGRATION_1_2, SyncPassDatabase.MIGRATION_2_3).build()
            assertEquals("Proyecto anterior", db.dao().observarProyectos().first().single().nombre)
            assertEquals("Ana", db.dao().observarPerfil().first()?.nombre)
            assertTrue(db.recintoDao().observar().first().isEmpty())
            assertTrue(db.recintoDao().crear(RecintoEntity(nombre = "Sede nueva", direccion = "Calle")) > 0)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
