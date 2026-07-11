package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScenicPinEntity::class, WeatherCacheEntity::class], version = 5, exportSchema = false)
abstract class ScenicDatabase : RoomDatabase() {
    abstract fun scenicPinDao(): ScenicPinDao

    companion object {
        @Volatile
        private var INSTANCE: ScenicDatabase? = null

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `weather_cache` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `latitude` REAL NOT NULL, 
                        `longitude` REAL NOT NULL, 
                        `temperature` REAL NOT NULL, 
                        `weatherStatus` TEXT NOT NULL, 
                        `cloudCoverage` INTEGER NOT NULL, 
                        `humidity` INTEGER, 
                        `windSpeed` REAL, 
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_weather_cache_latitude_longitude` 
                    ON `weather_cache` (`latitude`, `longitude`)
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): ScenicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScenicDatabase::class.java,
                    "scenic_scout_database"
                )
                .addMigrations(MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
