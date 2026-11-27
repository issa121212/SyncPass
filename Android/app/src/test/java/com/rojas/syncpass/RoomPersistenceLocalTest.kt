package com.rojas.syncpass

import android.app.Application
import androidx.room.Room
import com.rojas.syncpass.data.local.*
import com.rojas.syncpass.data.repository.RoomRecintosRepository
import com.rojas.syncpass.domain.model.Recinto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RoomPersistenceLocalTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private fun name() = "p-${UUID.randomUUID().toString().take(8)}.db"
    private fun open(name: String) = Room.databaseBuilder(context, SyncPassDatabase::class.java, name)
        .addMigrations(SyncPassDatabase.MIGRATION_1_2, SyncPassDatabase.MIGRATION_2_3, SyncPassDatabase.MIGRATION_3_4,
            SyncPassDatabase.MIGRATION_4_5, SyncPassDatabase.MIGRATION_5_6).build()

    @Test fun migracionesHistoricasConservanCatalogoPerfilCapturasYCreacionesPendientes() = runBlocking {
        for (version in listOf(1, 3, 5)) {
            val name = name()
            createLegacyRoomFixture(context, name, version)
            val db = open(name)
            try {
                val dao = db.dao()
                assertEquals("Proyecto anterior", dao.proyectoLocal(7)?.nombre)
                assertEquals("Ana", dao.perfilLocal()?.nombre)
                assertEquals("2026-11-25T14:30:15", dao.eventoLocal(9)?.fechaHora)
                assertEquals(8.5, dao.operacion("legacy-capture")?.precisionMetros)
                assertNull(dao.proyectoLocal(7)?.codigoPublico)
                assertNull(dao.eventoLocal(9)?.codigoPublico)
                assertEquals(6, db.openHelper.readableDatabase.version)
                if (version == 5) {
                    assertEquals("PENDIENTE", dao.proyectoLocal(-1)?.syncState)
                    assertEquals("550e8400-e29b-41d4-a716-446655440000", dao.creacionesPendientes().single().clientUuid)
                    assertEquals("2026-11-25T14:29:59Z", dao.operacion("legacy-capture")?.ubicacionEn)
                }
            } finally { db.close(); context.deleteDatabase(name) }
        }
    }

    @Test fun recintoValidadoPersisteSeEditaConCoordenadasYSeEliminaSinRed() = runBlocking {
        val name = name()
        var db = open(name)
        try {
            var repo = RoomRecintosRepository(db.recintoDao())
            assertTrue(repo.guardarRecinto(Recinto(nombre = "Sede", direccion = "Calle 100", latitud = -33.45, longitud = -70.66, radioMetros = 150)).isSuccess)
            val venue = repo.recintos.first().single()
            db.close(); db = open(name); repo = RoomRecintosRepository(db.recintoDao())
            assertEquals(-33.45, requireNotNull(db.recintoDao().obtener(venue.id)).latitud)
            assertTrue(repo.guardarRecinto(venue.copy(nombre = "Sede editada", latitud = null, longitud = null)).isSuccess)
            assertEquals("Sede editada", requireNotNull(db.recintoDao().obtener(venue.id)).nombre)
            assertNull(requireNotNull(db.recintoDao().obtener(venue.id)).latitud)
            assertTrue(repo.guardarRecinto(venue.copy(nombre = "")).isFailure)
            assertEquals("Sede editada", repo.recintos.first().single().nombre)
            assertTrue(repo.eliminarRecinto(venue.id).isSuccess)
            assertTrue(repo.recintos.first().isEmpty())
            assertNull(db.recintoDao().obtener(venue.id))
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun basesPorCuentaAislanCatalogoYRecintos() = runBlocking {
        val key = UUID.randomUUID().toString()
        val first = SyncPassDatabase.obtener(context, "issuer|a-$key")
        val second = SyncPassDatabase.obtener(context, "issuer|b-$key")
        try {
            first.dao().guardarPerfil(PerfilEntity(usuarioId = 1, nombre = "Ana", email = "a@example.invalid", roles = "DOCENTE", qrContenido = ""))
            first.recintoDao().crear(RecintoEntity(nombre = "Solo Ana", direccion = "Calle"))
            assertSame(first, SyncPassDatabase.obtener(context, "issuer|a-$key"))
            assertNull(second.dao().perfilLocal())
            assertTrue(second.recintoDao().observar().first().isEmpty())
            assertEquals("Solo Ana", first.recintoDao().observar().first().single().nombre)
        } finally { first.close(); second.close() }
    }
}
