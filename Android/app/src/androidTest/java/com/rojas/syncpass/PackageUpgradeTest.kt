package com.rojas.syncpass

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.rojas.syncpass.data.local.SyncPassDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/** Dos ejecuciones explícitas, antes/después de adb install -r; nunca borra datos personales. */
class PackageUpgradeTest {
    @Test fun conservaDatosAlReemplazarApkAnteriorPorLaVersionConHiltYRoomV6() = runBlocking {
        val stage = InstrumentationRegistry.getArguments().getString("upgradeStage")
        org.junit.Assume.assumeTrue(stage == "seed" || stage == "verify")
        val context = ApplicationProvider.getApplicationContext<Context>()
        check(context.packageName == "com.rojas.syncpass.validation")
        val preferences = context.getSharedPreferences("package_upgrade_fixture", Context.MODE_PRIVATE)
        val name = "package-upgrade-fixture.db"
        val hiltAvailable = runCatching { Class.forName("com.rojas.syncpass.di.AccountComponent") }.isSuccess
        if (stage == "seed") {
            assertFalse("La preparación debe ejecutarse sobre el APK anterior de main", hiltAvailable)
            check(!context.getDatabasePath(name).exists()) { "No sobrescribir un ensayo anterior" }
            createLegacyRoomFixture(context, name, 3)
            assertTrue(preferences.edit().putString("nonce", UUID.randomUUID().toString()).commit())
        } else {
            assertTrue("La verificación debe ejecutarse sobre el APK actualizado", hiltAvailable)
            assertNotNull("La actualización debe conservar SharedPreferences", preferences.getString("nonce", null))
            val db = Room.databaseBuilder(context, SyncPassDatabase::class.java, name)
                .addMigrations(SyncPassDatabase.MIGRATION_3_4, SyncPassDatabase.MIGRATION_4_5, SyncPassDatabase.MIGRATION_5_6).build()
            try {
                assertEquals("Proyecto anterior", db.dao().proyectoLocal(7)?.nombre)
                assertEquals("CONFIRMADO", db.dao().proyectoLocal(7)?.syncState)
                assertEquals("2026-11-25T14:30:15", db.dao().eventoLocal(9)?.fechaHora)
                assertEquals("Ana", db.dao().perfilLocal()?.nombre)
                assertEquals("Sede anterior", db.recintoDao().obtener(1)?.nombre)
                assertEquals(8.5, db.dao().operacion("legacy-capture")?.precisionMetros)
                assertNull(db.dao().operacion("legacy-capture")?.ubicacionEn)
                assertEquals(6, db.openHelper.readableDatabase.version)
            } finally { db.close() }
        }
    }
}
