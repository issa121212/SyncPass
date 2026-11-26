package com.rojas.syncpass

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rojas.syncpass.data.local.SyncPassDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class PublicCodeRoomMigrationTest {
    @Test fun migracionV5AV6ConservaColasYCatalogoYGuardaCodigosAlReabrir() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        check(context.packageName.endsWith(".validation"))
        val name = "public-code-migration-${UUID.randomUUID()}.db"
        createLegacyRoomFixture(context, name, 5)
        fun open() = Room.databaseBuilder(context, SyncPassDatabase::class.java, name)
            .addMigrations(SyncPassDatabase.MIGRATION_5_6).build()
        var db = open()
        try {
            val dao = db.dao()
            val project = requireNotNull(dao.proyectoLocal(7))
            val event = requireNotNull(dao.eventoLocal(9))
            val pending = requireNotNull(dao.operacion("legacy-capture"))
            val creation = requireNotNull(dao.creacion(1))
            assertEquals(6, db.openHelper.readableDatabase.version)
            assertNull(project.codigoPublico)
            assertNull(event.codigoPublico)
            assertEquals("PENDIENTE", dao.proyectoLocal(-1)?.syncState)
            assertEquals("2026-11-25T14:29:59Z", pending.ubicacionEn)
            val coded = event.copy(codigoPublico = "EVT-ABCDEF123456")
            dao.guardarEventoConPendientes(coded)
            db.close(); db = open()
            assertEquals(project, db.dao().proyectoLocal(7))
            assertEquals(coded, db.dao().eventoLocal(9))
            assertEquals(pending, db.dao().operacion("legacy-capture"))
            assertEquals(creation, db.dao().creacion(1))
            assertEquals("Proyecto pendiente", db.dao().proyectoLocal(-1)?.nombre)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
