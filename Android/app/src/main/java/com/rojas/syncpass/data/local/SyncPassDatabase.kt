package com.rojas.syncpass.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProyectoEntity::class,
        EventoEntity::class,
        PerfilEntity::class,
        PendienteEntity::class,
        RecintoEntity::class,
        CreationEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class SyncPassDatabase : RoomDatabase() {

    abstract fun dao(): SyncPassDao
    abstract fun recintoDao(): RecintoDao

    companion object {
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                for (table in listOf("proyectos", "eventos")) {
                    db.execSQL("ALTER TABLE $table ADD COLUMN syncState TEXT NOT NULL DEFAULT 'CONFIRMADO'")
                    db.execSQL("ALTER TABLE $table ADD COLUMN syncError TEXT")
                }
                db.execSQL("""CREATE TABLE IF NOT EXISTS creaciones (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, clientUuid TEXT NOT NULL,
                    tipo TEXT NOT NULL, payload TEXT NOT NULL, proyectoId INTEGER, remoteId INTEGER,
                    estado TEXT NOT NULL, error TEXT)""")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_creaciones_clientUuid ON creaciones(clientUuid)")
            }
        }
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pendientes ADD COLUMN ubicacionEn TEXT")
            }
        }
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pendientes ADD COLUMN rechazado INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE proyectos ADD COLUMN responsableId INTEGER")
                db.execSQL("""CREATE TABLE IF NOT EXISTS recintos_locales (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nombre TEXT NOT NULL,
                    direccion TEXT NOT NULL, latitud REAL, longitud REAL, radioMetros INTEGER NOT NULL)""")
            }
        }

        private val instances = mutableMapOf<String, SyncPassDatabase>()

        @Synchronized
        fun obtener(context: Context, accountKey: String): SyncPassDatabase {
            val suffix = java.security.MessageDigest.getInstance("SHA-256")
                .digest(accountKey.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            return instances.getOrPut(suffix) {
                Room.databaseBuilder(context.applicationContext,
                    SyncPassDatabase::class.java, "syncpass-$suffix.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
            }
        }
    }
}
