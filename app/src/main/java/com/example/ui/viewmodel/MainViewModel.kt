package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioCaptureSource
import com.example.audio.AudioPlayerManager
import com.example.audio.PlaybackState
import com.example.audio.RecorderState
import com.example.audio.RecordingStatus
import com.example.data.RecordingEntity
import com.example.data.RecordingRepository
import com.example.service.AudioRecorderService
import com.example.service.FloatingOverlayService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen {
    STUDIO_BROWSER,
    RECORDINGS,
    FLOATING_OVERLAY
}

enum class SortOption {
    DATE_DESC,
    DATE_ASC,
    TITLE_AZ,
    DURATION_DESC,
    SIZE_DESC
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    val repository = RecordingRepository(context)
    val playerManager = AudioPlayerManager(context)

    // Current navigation tab
    private val _currentScreen = MutableStateFlow(AppScreen.STUDIO_BROWSER)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Recorder configuration
    private val _selectedFormat = MutableStateFlow("MP3") // "MP3", "WAV", "M4A"
    val selectedFormat: StateFlow<String> = _selectedFormat.asStateFlow()

    private val _selectedSource = MutableStateFlow(AudioCaptureSource.BROWSER_INTERNAL)
    val selectedSource: StateFlow<AudioCaptureSource> = _selectedSource.asStateFlow()

    // Recording status polling from service
    private val _recorderState = MutableStateFlow(RecorderState())
    val recorderState: StateFlow<RecorderState> = _recorderState.asStateFlow()

    // Search and sort for file manager
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    // Overlay status
    private val _isOverlayActive = MutableStateFlow(false)
    val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

    // MediaProjection internal audio capture status
    private val _isMediaProjectionReady = MutableStateFlow(AudioRecorderService.isMediaProjectionReady)
    val isMediaProjectionReady: StateFlow<Boolean> = _isMediaProjectionReady.asStateFlow()

    private val _triggerMediaProjectionRequest = MutableStateFlow(0)
    val triggerMediaProjectionRequest: StateFlow<Int> = _triggerMediaProjectionRequest.asStateFlow()

    // Status snackbar message
    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    // Playback state
    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState

    // Recordings list from database
    val recordings: StateFlow<List<RecordingEntity>> = combine(
        repository.allRecordings,
        _searchQuery,
        _sortOption
    ) { all, query, sort ->
        val filtered = if (query.isBlank()) {
            all
        } else {
            all.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.fileName.contains(query, ignoreCase = true)
            }
        }
        when (sort) {
            SortOption.DATE_DESC -> filtered.sortedByDescending { it.createdAt }
            SortOption.DATE_ASC -> filtered.sortedBy { it.createdAt }
            SortOption.TITLE_AZ -> filtered.sortedBy { it.title.lowercase() }
            SortOption.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
            SortOption.SIZE_DESC -> filtered.sortedByDescending { it.fileSizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Poll recorder service state
        viewModelScope.launch {
            while (isActive) {
                val service = AudioRecorderService.instance
                if (service != null) {
                    _recorderState.value = service.getRecorderState().value
                } else if (_recorderState.value.status != RecordingStatus.IDLE) {
                    _recorderState.value = RecorderState(status = RecordingStatus.IDLE)
                }
                _isOverlayActive.value = FloatingOverlayService.isOverlayRunning
                _isMediaProjectionReady.value = AudioRecorderService.isMediaProjectionReady
                delay(120)
            }
        }
    }

    fun setMediaProjectionData(resultCode: Int, data: Intent) {
        AudioRecorderService.mediaProjectionResultCode = resultCode
        AudioRecorderService.mediaProjectionResultData = data
        _isMediaProjectionReady.value = true
        showSnack("✅ Audio Browser Aktif! Suara jernih dari tab akan direkam.")
    }

    fun requestMediaProjectionCapture() {
        _triggerMediaProjectionRequest.value += 1
    }

    fun onMediaProjectionRequested() {
        // acknowledged
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setFormat(format: String) {
        _selectedFormat.value = format
    }

    fun setSource(source: AudioCaptureSource) {
        _selectedSource.value = source
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(sort: SortOption) {
        _sortOption.value = sort
    }

    fun clearSnackMessage() {
        _snackMessage.value = null
    }

    fun showSnack(msg: String) {
        _snackMessage.value = msg
    }

    // --- Recording Actions ---

    fun startRecording(customTitle: String? = null) {
        val intent = Intent(context, AudioRecorderService::class.java).apply {
            action = AudioRecorderService.ACTION_START
            putExtra(AudioRecorderService.EXTRA_FORMAT, _selectedFormat.value)
            putExtra(AudioRecorderService.EXTRA_SOURCE, _selectedSource.value.name)
            if (!customTitle.isNullOrBlank()) {
                putExtra(AudioRecorderService.EXTRA_TITLE, customTitle)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun pauseRecording() {
        val intent = Intent(context, AudioRecorderService::class.java).apply {
            action = AudioRecorderService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resumeRecording() {
        val intent = Intent(context, AudioRecorderService::class.java).apply {
            action = AudioRecorderService.ACTION_RESUME
        }
        context.startService(intent)
    }

    fun stopRecording() {
        val intent = Intent(context, AudioRecorderService::class.java).apply {
            action = AudioRecorderService.ACTION_STOP
        }
        context.startService(intent)
        showSnack("Rekaman disimpan ke memori!")
    }

    // --- Floating Overlay Controls ---

    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun toggleFloatingOverlay(): Boolean {
        if (!hasOverlayPermission()) {
            return false
        }
        if (_isOverlayActive.value) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = FloatingOverlayService.ACTION_HIDE
            }
            context.startService(intent)
            _isOverlayActive.value = false
            showSnack("Floating overlay dinonaktifkan")
        } else {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = FloatingOverlayService.ACTION_SHOW
            }
            context.startService(intent)
            _isOverlayActive.value = true
            showSnack("Floating overlay aktif di atas aplikasi lain!")
        }
        return true
    }

    fun launchChromeAndFloating(context: Context, onRequestMediaProjection: () -> Unit) {
        if (_selectedSource.value == AudioCaptureSource.BROWSER_INTERNAL && !_isMediaProjectionReady.value) {
            onRequestMediaProjection()
            showSnack("Izinkan penangkapan audio sistem terlebih dahulu agar suara tab terekam jernih.")
            return
        }

        if (!hasOverlayPermission()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            showSnack("Aktifkan izin 'Tampilkan di atas aplikasi lain' untuk bubble rekam.")
            return
        }

        // Activate overlay bubble if not running
        if (!_isOverlayActive.value) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = FloatingOverlayService.ACTION_SHOW
            }
            context.startService(intent)
            _isOverlayActive.value = true
        }

        // Launch Google Chrome app
        val chromeIntent = context.packageManager.getLaunchIntentForPackage("com.android.chrome")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        chromeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chromeIntent)
            showSnack("🚀 Membuka Google Chrome dengan Bubble RecLish!")
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            showSnack("Membuka browser...")
        }
    }

    // --- Player Actions ---

    fun playRecording(recording: RecordingEntity) {
        playerManager.playRecording(recording)
    }

    fun pausePlayback() {
        playerManager.pause()
    }

    fun resumePlayback() {
        playerManager.resume()
    }

    fun seekPlayback(fraction: Float) {
        val state = playerManager.playbackState.value
        val pos = (state.totalDurationMs * fraction).toLong()
        playerManager.seekTo(pos)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setSpeed(speed)
    }

    fun stopPlayback() {
        playerManager.stop()
    }

    // --- File Management Actions ---

    fun renameRecording(recording: RecordingEntity, newTitle: String) {
        viewModelScope.launch {
            val success = repository.renameRecording(recording, newTitle)
            if (success) {
                showSnack("Nama file berhasil diubah!")
            } else {
                showSnack("Gagal mengubah nama file")
            }
        }
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            if (playerManager.playbackState.value.currentRecording?.id == recording.id) {
                playerManager.stop()
            }
            val success = repository.deleteRecording(recording)
            if (success) {
                showSnack("File rekaman berhasil dihapus")
            } else {
                showSnack("Gagal menghapus file")
            }
        }
    }

    fun toggleFavorite(recording: RecordingEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(recording)
        }
    }

    fun getShareIntent(recording: RecordingEntity): Intent? {
        return repository.getShareIntent(recording)
    }

    fun exportToDownloads(recording: RecordingEntity) {
        viewModelScope.launch {
            val uri = repository.exportToDownloads(recording)
            if (uri != null) {
                showSnack("Berhasil diekspor ke folder Download!")
            } else {
                showSnack("Gagal mengekspor file")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
