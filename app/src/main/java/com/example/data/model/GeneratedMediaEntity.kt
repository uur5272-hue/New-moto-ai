package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generated_media")
data class GeneratedMediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaType: String, // "image", "video", "edited_photo"
    val prompt: String,
    val localUri: String,
    val mediaStoreUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val resolution: String? = null,
    val fileName: String
)
