package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.db.dao.EntryRowDao
import com.example.data.db.dao.JournalDao
import com.example.data.db.dao.PropertyDao
import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.JournalEntity
import com.example.data.db.entity.PropertyEntity

@Database(
    entities = [JournalEntity::class, PropertyEntity::class, EntryRowEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
    abstract fun propertyDao(): PropertyDao
    abstract fun entryRowDao(): EntryRowDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Ajoute les colonnes de synchronisation (v1 -> v2).
         * Les lignes existantes sont rétro-remplies : updated_at = created_at pour journaux et
         * lignes, 0 pour les propriétés. Une valeur de 0 marque la ligne comme jamais poussée,
         * ce qui garantit que les données hors-ligne existantes partent au premier envoi.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("journals", "properties", "entry_rows").forEach { table ->
                    db.execSQL("ALTER TABLE $table ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE $table ADD COLUMN deleted_at INTEGER DEFAULT NULL")
                    db.execSQL("ALTER TABLE $table ADD COLUMN user_id TEXT DEFAULT NULL")
                }
                db.execSQL("UPDATE journals SET updated_at = created_at WHERE updated_at = 0")
                db.execSQL("UPDATE entry_rows SET updated_at = created_at WHERE updated_at = 0")
            }
        }

        /**
         * Ajoute le sous-format des propriétés numériques (v2 -> v3).
         * Colonne nullable sans valeur de remplissage : les lignes existantes valent NULL,
         * ce que le code traite comme "plain". Aucune donnée utilisateur n'est réécrite.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE properties ADD COLUMN format TEXT DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myapp.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
