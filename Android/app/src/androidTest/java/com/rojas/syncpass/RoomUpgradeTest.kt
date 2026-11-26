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

class RoomUpgradeTest {
    @Test fun actualizacionDesdeV3ConservaCatalogoPerfilRecintosYPendientesAlReabrir() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        check(context.packageName.endsWith(".validation")) { "Usar instalación de validación separada" }
        val name = "upgrade-v3-${UUID.randomUUID()}.db"
        createLegacyRoomFixture(context, name, 3)
        fun open() = Room.databaseBuilder(context, SyncPassDatabase::class.java, name)
            .addMigrations(SyncPassDatabase.MIGRATION_3_4, SyncPassDatabase.MIGRATION_4_5, SyncPassDatabase.MIGRATION_5_6).build()
        var db = open()
        try {
            val project = requireNotNull(db.dao().proyectoLocal(7))
            val event = requireNotNull(db.dao().eventoLocal(9))
            val profile = requireNotNull(db.dao().perfilLocal())
            val pending = requireNotNull(db.dao().operacion("legacy-capture"))
            val venue = db.recintoDao().observar().first().single()
            assertEquals("Conservar descripción", project.descripcion)
            assertEquals("CONFIRMADO", project.syncState)
            assertEquals("2026-11-25T14:30:15", event.fechaHora)
            assertEquals(8.5, pending.precisionMetros)
            assertNull(pending.ubicacionEn)
            assertFalse(pending.rechazado)
            assertTrue(db.dao().creacionesPendientes().isEmpty())
            db.close(); db = open()
            assertEquals(project, db.dao().proyectoLocal(7))
            assertEquals(event, db.dao().eventoLocal(9))
            assertEquals(profile, db.dao().perfilLocal())
            assertEquals(pending, db.dao().operacion("legacy-capture"))
            assertEquals(venue, db.recintoDao().observar().first().single())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
