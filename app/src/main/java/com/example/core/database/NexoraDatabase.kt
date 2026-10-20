package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.database.dao.FavoriteTipDao
import com.example.core.database.entity.FavoriteTipEntity

/**
 * Room Database for local persistence of student favorites and off-line study resources.
 */
@Database(
    entities = [
        FavoriteTipEntity::class,
        com.example.core.database.entity.AiChatSessionEntity::class,
        com.example.core.database.entity.AiChatMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NexoraDatabase : RoomDatabase() {

    abstract fun favoriteTipDao(): com.example.core.database.dao.FavoriteTipDao
    abstract fun aiChatDao(): com.example.core.database.dao.AiChatDao

    companion object {
        @Volatile
        private var INSTANCE: NexoraDatabase? = null

        fun getInstance(context: Context): NexoraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexoraDatabase::class.java,
                    "nexora_study_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
