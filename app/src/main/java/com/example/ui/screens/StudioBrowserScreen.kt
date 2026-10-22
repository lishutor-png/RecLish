package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioCaptureSource
import com.example.audio.RecordingStatus
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.theme.StudioDarkCard
import com.example.ui.theme.StudioDarkSurface
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioRecordRed
import com.example.ui.theme.StudioSecondary
import com.example.ui.theme.StudioSuccessGreen
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioWarningAmber
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StudioBrowserScreen(
    viewModel: MainViewModel,
    onRequestMediaProjection: () -> Unit,
    onRequestOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recorderState by viewModel.recorderState.collectAsStateWithLifecycle()
    val selectedFormat by viewModel.selectedFormat.collectAsStateWithLifecycle()
    val selectedSource by viewModel.selectedSource.collectAsStateWithLifecycle()
    val isMediaProjectionReady by viewModel.isMediaProjectionReady.collectAsStateWithLifecycle()
    val isOverlayActive by viewModel.isOverlayActive.collectAsStateWithLifecycle()

    var urlInput by remember { mutableStateOf("https://www.soundhelix.com/audio-examples") }
    var currentUrl by remember { mutableStateOf("https://www.soundhelix.com/audio-examples") }
    var pageTitle by remember { mutableStateOf("RecLish Web Studio") }
    var webProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current

    val bookmarks = listOf(
        "🎵 SoundHelix (Audio)" to "https://www.soundhelix.com/audio-examples",
        "🌐 Google Search" to "https://www.google.com",
        "▶️ YouTube Web" to "https://m.youtube.com",
        "🎼 FreeMusicArchive" to "https://freemusicarchive.org",
        "☁️ SoundCloud" to "https://m.soundcloud.com",
        "📻 Radio Garden" to "https://radio.garden"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP ADDRESS & NAVIGATION BAR
        Surface(
            color = StudioDarkSurface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Header Row: App Identity + Quick Google Chrome Shortcut
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = StudioRecordRed,
                            modifier = Modifier.size(14.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RecLish",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StudioPrimary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Audio Tab Studio",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StudioPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // 1-Tap Launch Google Chrome Button
                    Button(
                        onClick = {
                            viewModel.launchChromeAndFloating(context, onRequestMediaProjection)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioRecordRed.copy(alpha = 0.15f),
                            contentColor = StudioRecordRed
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .border(1.dp, StudioRecordRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .testTag("btn_quick_chrome")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buka di Chrome", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Address Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { webViewRef?.let { if (it.canGoBack()) it.goBack() } },
                        enabled = webViewRef?.canGoBack() == true,
                        modifier = Modifier.size(36.dp).testTag("web_btn_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = if (webViewRef?.canGoBack() == true) StudioTextPrimary else StudioTextMuted
                        )
                    }

                    IconButton(
                        onClick = { webViewRef?.let { if (it.canGoForward()) it.goForward() } },
                        enabled = webViewRef?.canGoForward() == true,
                        modifier = Modifier.size(36.dp).testTag("web_btn_forward")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Maju",
                            tint = if (webViewRef?.canGoForward() == true) StudioTextPrimary else StudioTextMuted
                        )
                    }

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .padding(horizontal = 4.dp)
                            .testTag("web_url_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = StudioDarkCard,
                            unfocusedContainerColor = StudioDarkCard,
                            focusedBorderColor = StudioPrimary,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = StudioTextPrimary,
                            unfocusedTextColor = StudioTextPrimary
                        ),
                        placeholder = {
                            Text("Ketik URL atau cari audio...", fontSize = 12.sp, color = StudioTextMuted)
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            keyboardController?.hide()
                            val trimmed = urlInput.trim()
                            val destination = when {
                                trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
                                trimmed.contains(".") -> "https://$trimmed"
                                else -> "https://www.google.com/search?q=$trimmed"
                            }
                            currentUrl = destination
                            webViewRef?.loadUrl(destination)
                        }),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    val trimmed = urlInput.trim()
                                    val destination = when {
                                        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
                                        trimmed.contains(".") -> "https://$trimmed"
                                        else -> "https://www.google.com/search?q=$trimmed"
                                    }
                                    currentUrl = destination
                                    webViewRef?.loadUrl(destination)
                                },
                                modifier = Modifier.size(28.dp).testTag("web_btn_go")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Buka", tint = StudioPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    )

                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier.size(36.dp).testTag("web_btn_reload")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang", tint = StudioPrimary, modifier = Modifier.size(20.dp))
                    }
                }

                // Loading progress bar
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { webProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .padding(top = 2.dp),
                        color = StudioPrimary,
                        trackColor = Color(0xFF1E293B)
                    )
                }

                // Preset Bookmarks Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bookmarks.forEach { (name, target) ->
                        FilterChip(
                            selected = currentUrl == target,
                            onClick = {
                                urlInput = target
                                currentUrl = target
                                webViewRef?.loadUrl(target)
                            },
                            label = { Text(name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = StudioDarkCard,
                                labelColor = StudioTextSecondary,
                                selectedContainerColor = StudioPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = StudioPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = currentUrl == target,
                                borderColor = Color(0xFF334155),
                                selectedBorderColor = StudioPrimary
                            )
                        )
                    }
                }
            }
        }

        // WEBVIEW CONTAINER
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                url?.let { urlInput = it }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                url?.let { urlInput = it }
                                view?.title?.let { pageTitle = it }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                return false
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                webProgress = newProgress
                                if (newProgress == 100) isLoading = false
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                title?.let { pageTitle = it }
                            }
                        }
                        loadUrl(currentUrl)
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // PERSISTENT RECORDING CONTROL DOCK (REVAMPED UI/UX)
        Card(
            colors = CardDefaults.cardColors(containerColor = StudioDarkSurface),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // 1. SOURCE SELECTOR TOGGLE (Tab Browser Internal vs Microphone)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioDarkCard)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isInternal = selectedSource == AudioCaptureSource.BROWSER_INTERNAL
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (isInternal) StudioPrimary.copy(alpha = 0.2f) else Color.Transparent,
                        border = if (isInternal) androidx.compose.foundation.BorderStroke(1.dp, StudioPrimary) else null,
                        modifier = Modifier
                            .weight(1.2f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable {
                                viewModel.setSource(AudioCaptureSource.BROWSER_INTERNAL)
                                if (!isMediaProjectionReady) onRequestMediaProjection()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = if (isInternal) StudioPrimary else StudioTextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🔊 Audio Browser (Jernih)",
                                fontSize = 11.sp,
                                fontWeight = if (isInternal) FontWeight.Bold else FontWeight.Normal,
                                color = if (isInternal) StudioPrimary else StudioTextSecondary
                            )
                        }
                    }

                    val isMic = selectedSource == AudioCaptureSource.MICROPHONE
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (isMic) StudioWarningAmber.copy(alpha = 0.2f) else Color.Transparent,
                        border = if (isMic) androidx.compose.foundation.BorderStroke(1.dp, StudioWarningAmber) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { viewModel.setSource(AudioCaptureSource.MICROPHONE) }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isMic) StudioWarningAmber else StudioTextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🎙 Mikrofon",
                                fontSize = 11.sp,
                                fontWeight = if (isMic) FontWeight.Bold else FontWeight.Normal,
                                color = if (isMic) StudioWarningAmber else StudioTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2. STATUS & PERMISSION BANNER
                if (selectedSource == AudioCaptureSource.BROWSER_INTERNAL) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isMediaProjectionReady) StudioSuccessGreen.copy(alpha = 0.12f) else StudioWarningAmber.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isMediaProjectionReady) StudioSuccessGreen.copy(alpha = 0.4f) else StudioWarningAmber
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!isMediaProjectionReady) onRequestMediaProjection()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isMediaProjectionReady) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = if (isMediaProjectionReady) StudioSuccessGreen else StudioWarningAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMediaProjectionReady)
                                    "Audio browser siap: Merekam langsung audio tab tanpa suara luar/mic."
                                else
                                    "⚠️ Izin audio sistem belum aktif. Ketuk di sini untuk aktifkan!",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isMediaProjectionReady) StudioSuccessGreen else StudioWarningAmber,
                                modifier = Modifier.weight(1f)
                            )
                            if (!isMediaProjectionReady) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = StudioWarningAmber,
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(
                                        text = "Aktifkan",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. WAVEFORM VISUALIZER & DURATION DISPLAY
                val totalSecs = recorderState.durationMs / 1000
                val mins = totalSecs / 60
                val secs = totalSecs % 60
                val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AudioWaveformVisualizer(
                        amplitude = recorderState.amplitude,
                        isRecording = recorderState.status == RecordingStatus.RECORDING,
                        isPaused = recorderState.status == RecordingStatus.PAUSED,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = formattedTime,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = if (recorderState.status == RecordingStatus.RECORDING) StudioRecordRed else StudioTextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // 4. ACTION BAR (Format Selection + Big Record Button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Audio Format selector chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("MP3", "WAV", "M4A").forEach { fmt ->
                            val isSelected = selectedFormat == fmt
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) StudioPrimary else StudioDarkCard,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (recorderState.status == RecordingStatus.IDLE) {
                                            viewModel.setFormat(fmt)
                                        }
                                    }
                                    .testTag("chip_format_$fmt")
                            ) {
                                Text(
                                    text = fmt,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF0F172A) else StudioTextSecondary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Recording Action Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (recorderState.status == RecordingStatus.IDLE) {
                            Button(
                                onClick = {
                                    if (selectedSource == AudioCaptureSource.BROWSER_INTERNAL && !isMediaProjectionReady) {
                                        onRequestMediaProjection()
                                    } else {
                                        viewModel.startRecording()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioRecordRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("btn_start_record")
                            ) {
                                Icon(
                                    Icons.Default.FiberManualRecord,
                                    contentDescription = "Mulai Rekam",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedSource == AudioCaptureSource.BROWSER_INTERNAL) "Rekam Audio Tab" else "Rekam Mikrofon",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // Pause / Resume Button
                            Button(
                                onClick = {
                                    if (recorderState.status == RecordingStatus.RECORDING) {
                                        viewModel.pauseRecording()
                                    } else {
                                        viewModel.resumeRecording()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (recorderState.status == RecordingStatus.RECORDING) StudioWarningAmber else StudioSuccessGreen
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(40.dp).testTag("btn_pause_resume")
                            ) {
                                Icon(
                                    imageVector = if (recorderState.status == RecordingStatus.RECORDING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (recorderState.status == RecordingStatus.RECORDING) "Jeda" else "Lanjut",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (recorderState.status == RecordingStatus.RECORDING) "Jeda" else "Lanjut",
                                    fontSize = 11.sp
                                )
                            }

                            // Stop & Save Button
                            Button(
                                onClick = { viewModel.stopRecording() },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioRecordRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(40.dp).testTag("btn_stop_save")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Simpan", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Simpan MP3", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
