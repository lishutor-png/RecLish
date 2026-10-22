package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sqrt

enum class RecordingStatus {
    IDLE, RECORDING, PAUSED, FINISHED, ERROR
}

enum class AudioCaptureSource {
    BROWSER_INTERNAL, MICROPHONE
}

data class RecorderState(
    val status: RecordingStatus = RecordingStatus.IDLE,
    val durationMs: Long = 0L,
    val amplitude: Float = 0f, // 0.0 to 1.0
    val activeSource: AudioCaptureSource = AudioCaptureSource.BROWSER_INTERNAL,
    val format: String = "MP3",
    val currentFile: File? = null,
    val errorMessage: String? = null
)

class AudioRecordManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val _recorderState = MutableStateFlow(RecorderState())
    val recorderState: StateFlow<RecorderState> = _recorderState.asStateFlow()

    // Internal Audio Capture via AudioRecord
    private var internalAudioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    @Volatile private var isCapturingInternal = false
    @Volatile private var isCapturePaused = false

    // Direct MediaRecorder for Mic
    private var mediaRecorder: MediaRecorder? = null

    private var startTimeMs = 0L
    private var accumulatedDurationMs = 0L
    private var timerJob: Job? = null
    private var targetOutputFile: File? = null
    private var tempPcmFile: File? = null

    companion object {
        var mediaProjection: MediaProjection? = null
    }

    /**
     * Starts audio recording.
     * When source is BROWSER_INTERNAL, STRICTLY captures internal system/browser audio output.
     * NEVER silently falls back to microphone.
     */
    @SuppressLint("MissingPermission")
    suspend fun startRecording(
        outputFile: File,
        format: String = "MP3",
        source: AudioCaptureSource = AudioCaptureSource.BROWSER_INTERNAL
    ): Boolean = withContext(Dispatchers.IO) {
        stopRecording() // ensure any previous session is cleaned up

        targetOutputFile = outputFile

        if (source == AudioCaptureSource.BROWSER_INTERNAL) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                _recorderState.value = RecorderState(
                    status = RecordingStatus.ERROR,
                    errorMessage = "Perekaman audio internal memerlukan Android 10 (API 29) atau lebih baru."
                )
                return@withContext false
            }
            if (mediaProjection == null) {
                _recorderState.value = RecorderState(
                    status = RecordingStatus.ERROR,
                    errorMessage = "Izin audio sistem / browser belum aktif. Silakan izinkan akses capture audio sistem."
                )
                return@withContext false
            }

            _recorderState.value = RecorderState(
                status = RecordingStatus.RECORDING,
                durationMs = 0L,
                amplitude = 0f,
                activeSource = AudioCaptureSource.BROWSER_INTERNAL,
                format = format,
                currentFile = outputFile
            )

            startTimeMs = SystemClock.elapsedRealtime()
            accumulatedDurationMs = 0L
            startTimerAndMeter()

            return@withContext startInternalCapture(outputFile, format)
        } else {
            // Microphone mode
            _recorderState.value = RecorderState(
                status = RecordingStatus.RECORDING,
                durationMs = 0L,
                amplitude = 0f,
                activeSource = AudioCaptureSource.MICROPHONE,
                format = format,
                currentFile = outputFile
            )

            startTimeMs = SystemClock.elapsedRealtime()
            accumulatedDurationMs = 0L
            startTimerAndMeter()

            return@withContext startMediaRecorderCapture(outputFile, format)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startInternalCapture(outputFile: File, format: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val projection = mediaProjection ?: run {
            _recorderState.value = _recorderState.value.copy(
                status = RecordingStatus.ERROR,
                errorMessage = "MediaProjection belum siap untuk merekam audio browser."
            )
            return false
        }

        try {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_STEREO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = minBufferSize.coerceAtLeast(16384)

            // Capture all browser audio usages: media, games, and web audio contexts
            val playbackCaptureConfig = AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val record = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(playbackCaptureConfig)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                _recorderState.value = _recorderState.value.copy(
                    status = RecordingStatus.ERROR,
                    errorMessage = "Gagal menginisialisasi perekam audio internal (STATE_UNINITIALIZED)."
                )
                return false
            }

            internalAudioRecord = record
            tempPcmFile = File(context.cacheDir, "temp_rec_${System.currentTimeMillis()}.pcm")
            record.startRecording()
            isCapturingInternal = true
            isCapturePaused = false

            captureThread = Thread({
                val buffer = ByteArray(bufferSize)
                val out = FileOutputStream(tempPcmFile)
                try {
                    while (isCapturingInternal) {
                        if (isCapturePaused) {
                            Thread.sleep(50)
                            continue
                        }
                        val read = internalAudioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            out.write(buffer, 0, read)
                            // Compute amplitude directly from internal audio stream
                            computePcmAmplitude(buffer, read)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try { out.close() } catch (ignored: Exception) {}
                }
            }, "InternalAudioCaptureThread").apply { start() }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            _recorderState.value = _recorderState.value.copy(
                status = RecordingStatus.ERROR,
                errorMessage = "Gagal merekam audio browser: ${e.message}"
            )
            return false
        }
    }

    private fun computePcmAmplitude(buffer: ByteArray, length: Int) {
        var sum = 0.0
        val numShorts = length / 2
        if (numShorts <= 0) return
        for (i in 0 until length - 1 step 2) {
            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
            sum += (sample * sample).toDouble()
        }
        val rms = sqrt(sum / numShorts)
        val normalized = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
        _recorderState.value = _recorderState.value.copy(amplitude = normalized)
    }

    @SuppressLint("MissingPermission")
    private fun startMediaRecorderCapture(outputFile: File, format: String): Boolean {
        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder = recorder

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(192000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(outputFile.absolutePath)
            recorder.prepare()
            recorder.start()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            _recorderState.value = _recorderState.value.copy(
                status = RecordingStatus.ERROR,
                errorMessage = "Gagal memulai mikrofon: ${e.message}"
            )
            return false
        }
    }

    fun pauseRecording() {
        if (_recorderState.value.status != RecordingStatus.RECORDING) return

        accumulatedDurationMs += SystemClock.elapsedRealtime() - startTimeMs
        if (isCapturingInternal) {
            isCapturePaused = true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _recorderState.value = _recorderState.value.copy(
            status = RecordingStatus.PAUSED,
            amplitude = 0f
        )
    }

    fun resumeRecording() {
        if (_recorderState.value.status != RecordingStatus.PAUSED) return

        startTimeMs = SystemClock.elapsedRealtime()
        if (isCapturingInternal) {
            isCapturePaused = false
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _recorderState.value = _recorderState.value.copy(
            status = RecordingStatus.RECORDING
        )
    }

    suspend fun stopRecording(): File? = withContext(Dispatchers.IO) {
        if (_recorderState.value.status == RecordingStatus.IDLE) return@withContext null

        timerJob?.cancel()
        timerJob = null

        val finalDurationMs = if (_recorderState.value.status == RecordingStatus.RECORDING) {
            accumulatedDurationMs + (SystemClock.elapsedRealtime() - startTimeMs)
        } else {
            accumulatedDurationMs
        }

        // Clean up internal capture
        if (isCapturingInternal) {
            isCapturingInternal = false
            try {
                captureThread?.join(500)
            } catch (ignored: Exception) {}
            try {
                internalAudioRecord?.stop()
                internalAudioRecord?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            internalAudioRecord = null

            // Convert PCM to target file
            val pcm = tempPcmFile
            val target = targetOutputFile
            if (pcm != null && pcm.exists() && target != null) {
                AudioFormatHelper.writeWavHeader(pcm, target, 44100, 2, 16)
                if (target.name.endsWith(".mp3", ignoreCase = true)) {
                    AudioFormatHelper.attachMp3Metadata(target, target.nameWithoutExtension)
                }
                pcm.delete()
            }
        }

        // Clean up MediaRecorder
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }

        val completedFile = targetOutputFile
        if (completedFile != null && completedFile.exists() && completedFile.name.endsWith(".mp3", ignoreCase = true)) {
            AudioFormatHelper.attachMp3Metadata(completedFile, completedFile.nameWithoutExtension)
        }

        _recorderState.value = _recorderState.value.copy(
            status = RecordingStatus.FINISHED,
            durationMs = finalDurationMs,
            amplitude = 0f
        )

        completedFile
    }

    private fun startTimerAndMeter() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                if (_recorderState.value.status == RecordingStatus.RECORDING) {
                    val currentDur = accumulatedDurationMs + (SystemClock.elapsedRealtime() - startTimeMs)
                    val amp = if (mediaRecorder != null) {
                        try {
                            val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                            (maxAmp / 32768f).coerceIn(0f, 1f)
                        } catch (e: Exception) {
                            _recorderState.value.amplitude
                        }
                    } else {
                        _recorderState.value.amplitude
                    }

                    _recorderState.value = _recorderState.value.copy(
                        durationMs = currentDur,
                        amplitude = amp
                    )
                }
                delay(80)
            }
        }
    }

    fun reset() {
        _recorderState.value = RecorderState()
        targetOutputFile = null
        tempPcmFile = null
    }
}
