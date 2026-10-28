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
    ],
    version = 3,
    exportSchema = false,
)
abstract class SyncPassDatabase : RoomDatabase() {

    abstract fun dao(): SyncPassDao
    abstract fun recintoDao(): RecintoDao

    companion object {
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
            }
        }
    }
}
