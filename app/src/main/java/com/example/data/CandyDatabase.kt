package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CandyLevel::class], version = 2, exportSchema = false)
abstract class CandyDatabase : RoomDatabase() {
    abstract val levelDao: LevelDao

    companion object {
        @Volatile
        private var INSTANCE: CandyDatabase? = null

        fun getDatabase(context: Context): CandyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CandyDatabase::class.java,
                    "candy_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
