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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Layers
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.RecordingStatus
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.theme.StudioDarkCard
import com.example.ui.theme.StudioDarkSurface
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioRecordRed
import com.example.ui.theme.StudioSuccessGreen
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioWarningAmber
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StudioBrowserScreen(
    viewModel: MainViewModel,
    onRequestOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recorderState by viewModel.recorderState.collectAsStateWithLifecycle()
    val selectedFormat by viewModel.selectedFormat.collectAsStateWithLifecycle()
    val isOverlayActive by viewModel.isOverlayActive.collectAsStateWithLifecycle()

    var urlInput by remember { mutableStateOf("https://www.soundhelix.com/audio-examples") }
    var currentUrl by remember { mutableStateOf("https://www.soundhelix.com/audio-examples") }
    var pageTitle by remember { mutableStateOf("Audio Browser Studio") }
    var webProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val bookmarks = listOf(
        "Google Search" to "https://www.google.com",
        "YouTube Web" to "https://m.youtube.com",
        "SoundHelix (Audio)" to "https://www.soundhelix.com/audio-examples",
        "FreeMusicArchive" to "https://freemusicarchive.org",
        "SoundCloud" to "https://m.soundcloud.com",
        "Radio Garden" to "https://radio.garden"
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
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
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
                            .height(48.dp)
                            .padding(horizontal = 4.dp)
                            .testTag("web_url_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = StudioDarkCard,
                            unfocusedContainerColor = StudioDarkCard,
                            focusedBorderColor = StudioPrimary,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = StudioTextPrimary,
                            unfocusedTextColor = StudioTextPrimary
                        ),
                        placeholder = {
                            Text("Ketik URL atau cari web...", fontSize = 12.sp, color = StudioTextMuted)
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
                                modifier = Modifier.size(32.dp).testTag("web_btn_go")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Buka", tint = StudioPrimary)
                            }
                        }
                    )

                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier.size(36.dp).testTag("web_btn_reload")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang", tint = StudioPrimary)
                    }
                }

                // Loading progress bar
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { webProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .padding(top = 4.dp),
                        color = StudioPrimary,
                        trackColor = Color(0xFF1E293B)
                    )
                }

                // Preset Audio Bookmarks chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Google Chrome App Shortcut Chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioRecordRed),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (!isOverlayActive && viewModel.hasOverlayPermission()) {
                                    viewModel.toggleFloatingOverlay()
                                }
                                val intent = context.packageManager.getLaunchIntentForPackage("com.android.chrome")
                                    ?: android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))
                                context.startActivity(intent)
                                viewModel.showSnack("Membuka Google Chrome dengan RecLish!")
                            }
                            .testTag("chip_open_chrome")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("🌐 Buka Google Chrome", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioRecordRed)
                        }
                    }

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

        // PERSISTENT RECORDING CONTROL DOCK
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Dock Header: Status & Duration & Floating Overlay Launcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusDotColor = when (recorderState.status) {
                            RecordingStatus.RECORDING -> StudioRecordRed
                            RecordingStatus.PAUSED -> StudioWarningAmber
                            else -> Color(0xFF64748B)
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusDotColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val statusLabel = when (recorderState.status) {
                            RecordingStatus.RECORDING -> "Merekam Audio Web"
                            RecordingStatus.PAUSED -> "Perekaman Dijeda"
                            else -> "Siap Merekam Suara"
                        }
                        Text(
                            text = statusLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StudioTextPrimary
                        )
                    }

                    // Floating overlay toggle button
                    Button(
                        onClick = onRequestOverlay,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOverlayActive) StudioPrimary.copy(alpha = 0.2f) else StudioDarkCard,
                            contentColor = if (isOverlayActive) StudioPrimary else StudioTextSecondary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("btn_toggle_floating")
                    ) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = "Floating Overlay",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOverlayActive) "Overlay Aktif" else "Buka di Atas Chrome",
                            fontSize = 11.sp
                        )
                    }
                }

                // Waveform visualizer & Timer display
                val totalSecs = recorderState.durationMs / 1000
                val mins = totalSecs / 60
                val secs = totalSecs % 60
                val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AudioWaveformVisualizer(
                        amplitude = recorderState.amplitude,
                        isRecording = recorderState.status == RecordingStatus.RECORDING,
                        isPaused = recorderState.status == RecordingStatus.PAUSED,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = formattedTime,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = if (recorderState.status == RecordingStatus.RECORDING) StudioRecordRed else StudioTextPrimary
                    )
                }

                // Format & Action Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Format selector tags
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("MP3", "WAV", "M4A").forEach { fmt ->
                            val isSelected = selectedFormat == fmt
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) StudioPrimary else StudioDarkCard,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .testTag("chip_format_$fmt"),
                                onClick = {
                                    if (recorderState.status == RecordingStatus.IDLE) {
                                        viewModel.setFormat(fmt)
                                    }
                                }
                            ) {
                                Text(
                                    text = fmt,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF0F172A) else StudioTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Recording Action Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (recorderState.status == RecordingStatus.IDLE) {
                            Button(
                                onClick = { viewModel.startRecording() },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioRecordRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(40.dp).testTag("btn_start_record")
                            ) {
                                Icon(
                                    Icons.Default.FiberManualRecord,
                                    contentDescription = "Mulai Rekam",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rekam Suara", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(40.dp).testTag("btn_pause_resume")
                            ) {
                                Icon(
                                    imageVector = if (recorderState.status == RecordingStatus.RECORDING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (recorderState.status == RecordingStatus.RECORDING) "Jeda" else "Lanjut",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (recorderState.status == RecordingStatus.RECORDING) "Jeda" else "Lanjut",
                                    fontSize = 12.sp
                                )
                            }

                            // Stop & Save Button
                            Button(
                                onClick = { viewModel.stopRecording() },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioRecordRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(40.dp).testTag("btn_stop_save")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Simpan", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Simpan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
