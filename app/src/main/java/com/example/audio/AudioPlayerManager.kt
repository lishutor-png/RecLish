package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import com.example.data.RecordingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackState(
    val currentRecording: RecordingEntity? = null,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f
)

class AudioPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun playRecording(recording: RecordingEntity) {
        val file = File(recording.filePath)
        if (!file.exists()) return

        // If same recording is paused, simply resume
        if (_playbackState.value.currentRecording?.id == recording.id && _playbackState.value.isPaused) {
            resume()
            return
        }

        stop()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(recording.filePath)
                prepare()
                val speed = _playbackState.value.playbackSpeed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && speed != 1.0f) {
                    playbackParams = PlaybackParams().apply { this.speed = speed }
                }
                start()
                setOnCompletionListener {
                    stopProgressTracking()
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        isPaused = false,
                        currentPositionMs = 0L
                    )
                }
            }

            _playbackState.value = _playbackState.value.copy(
                currentRecording = recording,
                isPlaying = true,
                isPaused = false,
                currentPositionMs = 0L,
                totalDurationMs = mediaPlayer?.duration?.toLong() ?: recording.durationMs
            )

            startProgressTracking()
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                stopProgressTracking()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    isPaused = true,
                    currentPositionMs = player.currentPosition.toLong()
                )
            }
        }
    }

    fun resume() {
        mediaPlayer?.let { player ->
            player.start()
            _playbackState.value = _playbackState.value.copy(
                isPlaying = true,
                isPaused = false
            )
            startProgressTracking()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            player.seekTo(positionMs.toInt())
            _playbackState.value = _playbackState.value.copy(
                currentPositionMs = positionMs
            )
        }
    }

    fun setSpeed(speed: Float) {
        _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { player ->
                    player.playbackParams = PlaybackParams().apply { this.speed = speed }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        stopProgressTracking()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }
        _playbackState.value = PlaybackState(playbackSpeed = _playbackState.value.playbackSpeed)
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _playbackState.value = _playbackState.value.copy(
                            currentPositionMs = player.currentPosition.toLong(),
                            totalDurationMs = player.duration.toLong().coerceAtLeast(1L)
                        )
                    }
                }
                delay(100)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stop()
    }
}
