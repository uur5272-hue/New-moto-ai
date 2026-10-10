package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.GeneratedMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneratedMediaDao {
    @Query("SELECT * FROM generated_media ORDER BY timestamp DESC")
    fun getAllMediaFlow(): Flow<List<GeneratedMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: GeneratedMediaEntity): Long

    @Query("DELETE FROM generated_media WHERE id = :id")
    suspend fun deleteMediaById(id: Long)

    @Query("DELETE FROM generated_media")
    suspend fun clearAllMedia()
}
