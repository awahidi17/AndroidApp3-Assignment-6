package com.ahmadwahidi.androidapp3.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Singleton Room database so every Activity uses the same saved hunt progress. */
@Database(entities = [TreasurePlace::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun treasurePlaceDao(): TreasurePlaceDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "windsor_treasure_hunt.db"
                ).build().also { instance = it }
            }
        }
    }
}
