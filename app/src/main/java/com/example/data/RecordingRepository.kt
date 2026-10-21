package com.example.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.recordingDao()

    val allRecordings: Flow<List<RecordingEntity>> = dao.getAllRecordings()

    fun searchRecordings(query: String): Flow<List<RecordingEntity>> = dao.searchRecordings(query)

    suspend fun getRecordingById(id: Long): RecordingEntity? = dao.getRecordingById(id)

    /**
     * Resolves the target directory on device storage for recordings.
     * Uses public Music/BrowserRecordings or standard external files dir.
     */
    fun getRecordingsDirectory(): File {
        val publicMusic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val appFolder = File(publicMusic, "BrowserRecordings")
        if (!appFolder.exists()) {
            appFolder.mkdirs()
        }
        if (appFolder.canWrite()) {
            return appFolder
        }
        // Fallback to external files directory
        val extFiles = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
        val fallback = File(extFiles, "BrowserRecordings")
        if (!fallback.exists()) {
            fallback.mkdirs()
        }
        return fallback
    }

    /**
     * Creates a new destination file for an upcoming recording.
     */
    fun createNewRecordingFile(format: String): File {
        val dir = getRecordingsDirectory()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val extension = when (format.uppercase()) {
            "WAV" -> "wav"
            "M4A" -> "m4a"
            else -> "mp3"
        }
        return File(dir, "BrowserRec_$timeStamp.$extension")
    }

    /**
     * Registers a completed recording file to MediaStore so it appears in device's phone memory / music apps,
     * and persists the record into the Room database.
     */
    suspend fun saveCompletedRecording(
        file: File,
        title: String,
        durationMs: Long,
        format: String,
        audioSource: String
    ): RecordingEntity = withContext(Dispatchers.IO) {
        val sizeBytes = if (file.exists()) file.length() else 0L
        var contentUriString: String? = null

        // Add to Android MediaStore for system-wide accessibility in phone memory
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.DURATION, durationMs)
                put(MediaStore.Audio.Media.SIZE, sizeBytes)
                val mimeType = when (format.uppercase()) {
                    "WAV" -> "audio/wav"
                    "M4A" -> "audio/mp4"
                    else -> "audio/mpeg"
                }
                put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/BrowserRecordings")
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
            }
            val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
            contentUriString = uri?.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val entity = RecordingEntity(
            title = title,
            fileName = file.name,
            filePath = file.absolutePath,
            contentUri = contentUriString,
            durationMs = durationMs,
            fileSizeBytes = sizeBytes,
            format = format.uppercase(),
            audioSource = audioSource,
            createdAt = System.currentTimeMillis()
        )

        val id = dao.insert(entity)
        entity.copy(id = id)
    }

    suspend fun renameRecording(recording: RecordingEntity, newTitle: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val oldFile = File(recording.filePath)
            val ext = oldFile.extension
            val sanitized = newTitle.replace(Regex("[^a-zA-Z0-9._ -]"), "_").trim()
            val newFileName = if (sanitized.endsWith(".$ext", ignoreCase = true)) sanitized else "$sanitized.$ext"
            val newFile = File(oldFile.parentFile, newFileName)

            if (oldFile.exists() && oldFile.renameTo(newFile)) {
                dao.rename(
                    id = recording.id,
                    newTitle = newTitle,
                    newFileName = newFileName,
                    newPath = newFile.absolutePath
                )
                // Update media store if possible
                recording.contentUri?.let { uriStr ->
                    try {
                        val uri = Uri.parse(uriStr)
                        val cv = ContentValues().apply {
                            put(MediaStore.Audio.Media.TITLE, newTitle)
                            put(MediaStore.Audio.Media.DISPLAY_NAME, newFileName)
                        }
                        context.contentResolver.update(uri, cv, null, null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                true
            } else {
                // If rename failed, still update title in DB
                dao.rename(
                    id = recording.id,
                    newTitle = newTitle,
                    newFileName = recording.fileName,
                    newPath = recording.filePath
                )
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteRecording(recording: RecordingEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
            recording.contentUri?.let { uriStr ->
                try {
                    context.contentResolver.delete(Uri.parse(uriStr), null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            dao.deleteById(recording.id)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun toggleFavorite(recording: RecordingEntity) {
        dao.toggleFavorite(recording.id, !recording.isFavorite)
    }

    fun getShareIntent(recording: RecordingEntity): Intent? {
        val file = File(recording.filePath)
        if (!file.exists()) return null

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = when (recording.format.uppercase()) {
            "WAV" -> "audio/wav"
            "M4A" -> "audio/mp4"
            else -> "audio/mpeg"
        }
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, recording.title)
            putExtra(Intent.EXTRA_TEXT, "Rekaman audio dari browser: ${recording.title}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun exportToDownloads(recording: RecordingEntity): Uri? = withContext(Dispatchers.IO) {
        try {
            val srcFile = File(recording.filePath)
            if (!srcFile.exists()) return@withContext null

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, srcFile.name)
                put(MediaStore.Downloads.MIME_TYPE, if (recording.format == "WAV") "audio/wav" else "audio/mpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext null

            context.contentResolver.openOutputStream(uri)?.use { out ->
                FileInputStream(srcFile).use { input ->
                    input.copyTo(out)
                }
            }
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
