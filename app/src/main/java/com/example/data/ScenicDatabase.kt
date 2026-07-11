package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScenicPinEntity::class], version = 4, exportSchema = false)
abstract class ScenicDatabase : RoomDatabase() {
    abstract fun scenicPinDao(): ScenicPinDao

    companion object {
        @Volatile
        private var INSTANCE: ScenicDatabase? = null

        fun getDatabase(context: Context): ScenicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScenicDatabase::class.java,
                    "scenic_scout_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
