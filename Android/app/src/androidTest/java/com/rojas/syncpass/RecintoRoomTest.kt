package com.rojas.syncpass

import android.content.Context
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
            .addMigrations(SyncPassDatabase.MIGRATION_1_2, SyncPassDatabase.MIGRATION_2_3, SyncPassDatabase.MIGRATION_3_4, SyncPassDatabase.MIGRATION_4_5).build()
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
        createLegacyRoomFixture(context, name, 1)
        val db = Room.databaseBuilder(context, SyncPassDatabase::class.java, name)
            .addMigrations(SyncPassDatabase.MIGRATION_1_2, SyncPassDatabase.MIGRATION_2_3,
                SyncPassDatabase.MIGRATION_3_4, SyncPassDatabase.MIGRATION_4_5).build()
        try {
            assertEquals("Proyecto anterior", db.dao().observarProyectos().first().single().nombre)
            assertEquals("Ana", db.dao().observarPerfil().first()?.nombre)
            assertTrue(db.recintoDao().observar().first().isEmpty())
            assertTrue(db.recintoDao().crear(RecintoEntity(nombre = "Sede nueva", direccion = "Calle")) > 0)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
