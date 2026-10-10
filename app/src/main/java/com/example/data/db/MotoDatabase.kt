package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.GeneratedMediaEntity

@Database(
    entities = [ChatMessageEntity::class, GeneratedMediaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MotoDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun generatedMediaDao(): GeneratedMediaDao

    companion object {
        @Volatile
        private var INSTANCE: MotoDatabase? = null

        fun getInstance(context: Context): MotoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MotoDatabase::class.java,
                    "moto_ai_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
