package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.AudioCaptureSource
import com.example.audio.AudioRecordManager
import com.example.audio.RecorderState
import com.example.audio.RecordingStatus
import com.example.data.RecordingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class AudioRecorderService : Service() {
    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var audioRecordManager: AudioRecordManager
    private lateinit var repository: RecordingRepository

    companion object {
        const val CHANNEL_ID = "channel_browser_audio_recorder"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        const val EXTRA_FORMAT = "extra_format"
        const val EXTRA_SOURCE = "extra_source"
        const val EXTRA_TITLE = "extra_title"

        // Singleton reference for overlay and viewmodel communication
        var instance: AudioRecorderService? = null
            private set
    }

    inner class LocalBinder : Binder() {
        fun getService(): AudioRecorderService = this@AudioRecorderService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        instance = this
        audioRecordManager = AudioRecordManager(applicationContext)
        repository = RecordingRepository(applicationContext)
        createNotificationChannel()

        // Observe recorder state to update ongoing notification
        scope.launch {
            audioRecordManager.recorderState.collect { state ->
                if (state.status == RecordingStatus.RECORDING || state.status == RecordingStatus.PAUSED) {
                    updateNotification(state)
                }
            }
        }
    }

    fun getRecorderState(): StateFlow<RecorderState> = audioRecordManager.recorderState

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val format = intent.getStringExtra(EXTRA_FORMAT) ?: "MP3"
                val sourceStr = intent.getStringExtra(EXTRA_SOURCE) ?: "BROWSER_INTERNAL"
                val source = try {
                    AudioCaptureSource.valueOf(sourceStr)
                } catch (e: Exception) {
                    AudioCaptureSource.BROWSER_INTERNAL
                }
                val customTitle = intent.getStringExtra(EXTRA_TITLE)
                startRecordingSession(format, source, customTitle)
            }
            ACTION_PAUSE -> {
                audioRecordManager.pauseRecording()
            }
            ACTION_RESUME -> {
                audioRecordManager.resumeRecording()
            }
            ACTION_STOP -> {
                stopRecordingSession()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecordingSession(format: String, source: AudioCaptureSource, customTitle: String?) {
        val file = repository.createNewRecordingFile(format)
        val initialNotification = buildNotification("Mempersiapkan rekaman...", isPaused = false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundType = if (source == AudioCaptureSource.BROWSER_INTERNAL && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(NOTIFICATION_ID, initialNotification, foregroundType)
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        scope.launch {
            val started = audioRecordManager.startRecording(file, format, source)
            if (!started) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun stopRecordingSession() {
        scope.launch {
            val completedFile = audioRecordManager.stopRecording()
            val state = audioRecordManager.recorderState.value
            if (completedFile != null && completedFile.exists() && state.durationMs > 500) {
                val defaultTitle = "Rekaman Browser ${System.currentTimeMillis() % 10000}"
                val sourceLabel = if (state.activeSource == AudioCaptureSource.BROWSER_INTERNAL) {
                    "Browser Audio"
                } else {
                    "Mikrofon"
                }
                repository.saveCompletedRecording(
                    file = completedFile,
                    title = defaultTitle,
                    durationMs = state.durationMs,
                    format = state.format,
                    audioSource = sourceLabel
                )
            }
            audioRecordManager.reset()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Perekam Audio Browser",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi status perekaman audio di latar belakang tanpa mengganggu"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String, isPaused: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause or Resume action
        val pauseResumeIntent = Intent(this, AudioRecorderService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pauseResumePendingIntent = PendingIntent.getService(
            this,
            1,
            pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pauseResumeActionText = if (isPaused) "Lanjutkan" else "Jeda"

        // Stop action
        val stopIntent = Intent(this, AudioRecorderService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RecLish")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_reclish_logo)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, pauseResumeActionText, pauseResumePendingIntent)
            .addAction(0, "Selesai & Simpan", stopPendingIntent)
            .build()
    }

    private fun updateNotification(state: RecorderState) {
        val totalSecs = state.durationMs / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        val sourceLabel = if (state.activeSource == AudioCaptureSource.BROWSER_INTERNAL) "Audio Browser" else "Mikrofon"
        val statusText = if (state.status == RecordingStatus.PAUSED) {
            "Dijeda ($timeStr) - $sourceLabel"
        } else {
            "Merekam... ($timeStr) - $sourceLabel"
        }

        val notification = buildNotification(statusText, isPaused = state.status == RecordingStatus.PAUSED)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}
