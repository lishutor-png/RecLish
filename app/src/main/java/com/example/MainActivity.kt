package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.AudioRecorderService
import com.example.ui.screens.FloatingSettingsScreen
import com.example.ui.screens.RecordingsScreen
import com.example.ui.screens.StudioBrowserScreen
import com.example.ui.theme.StudioDarkSurface
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.WebAudioTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_REQUEST_AUDIO_CAPTURE = "com.example.ACTION_REQUEST_AUDIO_CAPTURE"
    }

    private var viewModelRef: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val checkCaptureRequest = intent?.action == ACTION_REQUEST_AUDIO_CAPTURE

        setContent {
            WebAudioTheme {
                val vm: MainViewModel = viewModel()
                viewModelRef = vm
                if (checkCaptureRequest) {
                    LaunchedEffect(Unit) {
                        vm.requestMediaProjectionCapture()
                    }
                }
                MainScreenRoot(viewModel = vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ACTION_REQUEST_AUDIO_CAPTURE) {
            viewModelRef?.requestMediaProjectionCapture()
        }
    }
}

@Composable
fun MainScreenRoot(
    triggerCapturePromptOnStart: Boolean = false,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val snackMessage by viewModel.snackMessage.collectAsStateWithLifecycle()
    val isMediaProjectionReady by viewModel.isMediaProjectionReady.collectAsStateWithLifecycle()
    val triggerMediaProjectionRequest by viewModel.triggerMediaProjectionRequest.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingRecordAction by remember { mutableStateOf(false) }

    val projectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    // MediaProjection launcher for internal audio playback capture
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.setMediaProjectionData(result.resultCode, result.data!!)
            if (pendingRecordAction) {
                pendingRecordAction = false
                viewModel.startRecording()
            }
        } else {
            pendingRecordAction = false
            viewModel.showSnack("Perekaman audio browser memerlukan izin capture audio sistem.")
        }
    }

    val requestMediaProjectionPrompt: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        } else {
            viewModel.showSnack("Perekaman internal audio memerlukan Android 10 atau lebih baru")
        }
    }

    // Permission launcher for standard runtime permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val audioGranted = perms[Manifest.permission.RECORD_AUDIO] == true
        if (audioGranted && !isMediaProjectionReady) {
            // Automatically prompt for system internal audio capture on first launch
            requestMediaProjectionPrompt()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val notGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        } else if (triggerCapturePromptOnStart && !isMediaProjectionReady) {
            requestMediaProjectionPrompt()
        }
    }

    LaunchedEffect(triggerMediaProjectionRequest) {
        if (triggerMediaProjectionRequest > 0 && !isMediaProjectionReady) {
            requestMediaProjectionPrompt()
            viewModel.onMediaProjectionRequested()
        }
    }

    LaunchedEffect(snackMessage) {
        snackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackMessage()
        }
    }

    // Handle back button on secondary screens
    if (currentScreen != AppScreen.STUDIO_BROWSER) {
        BackHandler {
            viewModel.setScreen(AppScreen.STUDIO_BROWSER)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = StudioDarkSurface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.STUDIO_BROWSER,
                    onClick = { viewModel.setScreen(AppScreen.STUDIO_BROWSER) },
                    icon = { Icon(Icons.Default.Language, contentDescription = "Studio Web") },
                    label = { Text("Studio Web", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F172A),
                        selectedTextColor = StudioPrimary,
                        indicatorColor = StudioPrimary,
                        unselectedIconColor = StudioTextMuted,
                        unselectedTextColor = StudioTextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_studio")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.RECORDINGS,
                    onClick = { viewModel.setScreen(AppScreen.RECORDINGS) },
                    icon = { Icon(Icons.Default.GraphicEq, contentDescription = "Rekaman HP") },
                    label = { Text("Rekaman HP", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F172A),
                        selectedTextColor = StudioPrimary,
                        indicatorColor = StudioPrimary,
                        unselectedIconColor = StudioTextMuted,
                        unselectedTextColor = StudioTextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_recordings")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.FLOATING_OVERLAY,
                    onClick = { viewModel.setScreen(AppScreen.FLOATING_OVERLAY) },
                    icon = { Icon(Icons.Default.Layers, contentDescription = "Floating Mode") },
                    label = { Text("Floating Mode", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F172A),
                        selectedTextColor = StudioPrimary,
                        indicatorColor = StudioPrimary,
                        unselectedIconColor = StudioTextMuted,
                        unselectedTextColor = StudioTextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_overlay")
                )
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentScreen,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { screen ->
            when (screen) {
                AppScreen.STUDIO_BROWSER -> {
                    StudioBrowserScreen(
                        viewModel = viewModel,
                        onRequestMediaProjection = {
                            pendingRecordAction = true
                            requestMediaProjectionPrompt()
                        },
                        onRequestOverlay = {
                            if (!viewModel.hasOverlayPermission()) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                viewModel.toggleFloatingOverlay()
                            }
                        }
                    )
                }
                AppScreen.RECORDINGS -> {
                    RecordingsScreen(
                        viewModel = viewModel
                    )
                }
                AppScreen.FLOATING_OVERLAY -> {
                    FloatingSettingsScreen(
                        viewModel = viewModel,
                        onRequestMediaProjection = {
                            requestMediaProjectionPrompt()
                        }
                    )
                }
            }
        }
    }
}
