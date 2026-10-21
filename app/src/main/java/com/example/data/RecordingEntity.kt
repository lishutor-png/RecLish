package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val fileName: String,
    val filePath: String,
    val contentUri: String? = null,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val format: String = "MP3",
    val sampleRate: Int = 44100,
    val audioSource: String = "Browser Audio",
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
