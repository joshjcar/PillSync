package com.tally.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Medicine::class, ScheduleItem::class, DoseLog::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PillSyncDatabase : RoomDatabase() {
    abstract fun dao(): PillSyncDao

    companion object {
        @Volatile private var INSTANCE: PillSyncDatabase? = null

        /** v1 -> v2 adds stock-tracking columns to medicines without wiping data. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN stockEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE medicines ADD COLUMN stockCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE medicines ADD COLUMN lowStockThreshold INTEGER NOT NULL DEFAULT 5")
            }
        }

        fun get(context: Context): PillSyncDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PillSyncDatabase::class.java,
                    "tally.db"
                ).addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
