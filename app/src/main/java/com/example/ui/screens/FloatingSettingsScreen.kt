package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioCaptureSource
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
import com.example.ui.viewmodel.MainViewModel

@Composable
fun FloatingSettingsScreen(
    viewModel: MainViewModel,
    onRequestMediaProjection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOverlayActive by viewModel.isOverlayActive.collectAsStateWithLifecycle()
    val isMediaProjectionReady by viewModel.isMediaProjectionReady.collectAsStateWithLifecycle()
    val selectedFormat by viewModel.selectedFormat.collectAsStateWithLifecycle()
    val selectedSource by viewModel.selectedSource.collectAsStateWithLifecycle()
    val hasOverlayPermission = viewModel.hasOverlayPermission()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP HEADER
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = StudioRecordRed,
                    modifier = Modifier.size(10.dp)
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mode Rekam Google Chrome",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Merekam audio tab browser secara otomatis di latar belakang dengan kontrol melayang (bubble)",
                fontSize = 12.sp,
                color = StudioTextSecondary
            )
        }

        // 1. HERO 1-TAP CHROME LAUNCHER & PERMISSION CHECKLIST CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = StudioDarkSurface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, StudioPrimary.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .testTag("card_chrome_hero")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioRecordRed.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = StudioRecordRed, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mulai Cepat: Rekam di Chrome",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary
                        )
                        Text(
                            text = "Buka Google Chrome dengan bubble pengontrol siap pakai",
                            fontSize = 11.5.sp,
                            color = StudioTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Readiness Checklist
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioDarkCard)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Item 1: Audio Capture System Permission
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { if (!isMediaProjectionReady) onRequestMediaProjection() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isMediaProjectionReady) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (isMediaProjectionReady) StudioSuccessGreen else StudioWarningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "1. Tangkap Audio Tab Browser",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioTextPrimary
                            )
                            Text(
                                text = if (isMediaProjectionReady) "Suara tab akan terekam jernih tanpa mic" else "Ketuk di sini untuk aktifkan izin sistem",
                                fontSize = 10.5.sp,
                                color = if (isMediaProjectionReady) StudioSuccessGreen else StudioWarningAmber
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isMediaProjectionReady) StudioSuccessGreen.copy(alpha = 0.2f) else StudioWarningAmber.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isMediaProjectionReady) "Siap" else "Aktifkan",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMediaProjectionReady) StudioSuccessGreen else StudioWarningAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Item 2: Floating Overlay Permission
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (!hasOverlayPermission) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                    context.startActivity(intent)
                                } else {
                                    viewModel.toggleFloatingOverlay()
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (hasOverlayPermission) StudioSuccessGreen else StudioWarningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "2. Bubble di Atas Aplikasi Lain",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioTextPrimary
                            )
                            Text(
                                text = if (hasOverlayPermission) "Bubble dapat mengapung di atas Chrome" else "Ketuk untuk izinkan overlay di pengaturan HP",
                                fontSize = 10.5.sp,
                                color = if (hasOverlayPermission) StudioSuccessGreen else StudioWarningAmber
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (hasOverlayPermission) StudioSuccessGreen.copy(alpha = 0.2f) else StudioWarningAmber.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (hasOverlayPermission) "Siap" else "Aktifkan",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasOverlayPermission) StudioSuccessGreen else StudioWarningAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Giant Launch Button
                Button(
                    onClick = {
                        viewModel.launchChromeAndFloating(context, onRequestMediaProjection)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioRecordRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_launch_chrome_primary")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Buka Google Chrome & Mulai Rekam",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // 2. FLOATING BUBBLE STATUS & MANUAL SWITCH
        Card(
            colors = CardDefaults.cardColors(containerColor = StudioDarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF26334D), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = StudioPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = StudioPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Status Bubble Mengambang",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary
                        )
                        Text(
                            text = if (isOverlayActive) "🟢 Aktif di layar HP" else "⚪ Nonaktif",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOverlayActive) StudioSuccessGreen else StudioTextMuted
                        )
                    }

                    Switch(
                        checked = isOverlayActive,
                        onCheckedChange = {
                            if (!hasOverlayPermission) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                context.startActivity(intent)
                            } else {
                                viewModel.toggleFloatingOverlay()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StudioPrimary,
                            checkedTrackColor = StudioPrimary.copy(alpha = 0.4f),
                            uncheckedThumbColor = StudioTextMuted,
                            uncheckedTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("switch_floating_overlay")
                    )
                }

                // Visual Preview of the Bubble
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mini preview bubble pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioPrimary),
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, color = StudioRecordRed, modifier = Modifier.size(8.dp)) {}
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("00:42", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("🔊 Rec", fontSize = 9.sp, color = StudioPrimary)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tampilan Bubble di Layar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioTextPrimary
                            )
                            Text(
                                text = "Bisa digeser ke mana saja. Ketuk bubble untuk Jeda atau Simpan rekaman MP3.",
                                fontSize = 10.sp,
                                color = StudioTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 3. AUDIO SOURCE SELECTION (TAB BROWSER vs MIC)
        Card(
            colors = CardDefaults.cardColors(containerColor = StudioDarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF26334D), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Pilihan Sumber Suara",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tentukan sumber audio sebelum merekam:",
                    fontSize = 12.sp,
                    color = StudioTextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 1: Browser Tab Audio (Internal Capture)
                val isInternal = selectedSource == AudioCaptureSource.BROWSER_INTERNAL
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isInternal) StudioPrimary.copy(alpha = 0.12f) else StudioDarkCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isInternal) StudioPrimary else Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.setSource(AudioCaptureSource.BROWSER_INTERNAL)
                            if (!isMediaProjectionReady) onRequestMediaProjection()
                        }
                        .testTag("source_browser_internal")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isInternal) StudioPrimary.copy(alpha = 0.2f) else Color(0xFF1E293B),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = if (isInternal) StudioPrimary else StudioTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🔊 Audio Tab Browser (Jernih)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isInternal) StudioPrimary else StudioTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = StudioSuccessGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Rekomendasi",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioSuccessGreen,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Menangkap suara langsung dari sistem Android. Hasil rekaman murni suara video/musik tanpa suara napas atau kebisingan ruangan.",
                                fontSize = 11.sp,
                                color = StudioTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: Microphone
                val isMic = selectedSource == AudioCaptureSource.MICROPHONE
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isMic) StudioWarningAmber.copy(alpha = 0.12f) else StudioDarkCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isMic) StudioWarningAmber else Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.setSource(AudioCaptureSource.MICROPHONE) }
                        .testTag("source_mic")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isMic) StudioWarningAmber.copy(alpha = 0.2f) else Color(0xFF1E293B),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isMic) StudioWarningAmber else StudioTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🎙 Mikrofon HP",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMic) StudioWarningAmber else StudioTextPrimary
                            )
                            Text(
                                text = "Merekam suara yang keluar dari speaker luar dan suara Anda.",
                                fontSize = 11.sp,
                                color = StudioTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 4. FORMAT SELECTOR (MP3, WAV, M4A)
        Card(
            colors = CardDefaults.cardColors(containerColor = StudioDarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF26334D), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Format File Penyimpanan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "File disimpan langsung ke memori internal HP:",
                    fontSize = 12.sp,
                    color = StudioTextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("MP3", "Rekomendasi", "Paling kompatibel & ringan"),
                        Triple("WAV", "Studio PCM", "Kualitas tanpa kompresi"),
                        Triple("M4A", "AAC Modern", "Efisien & jernih")
                    ).forEach { (fmt, tag, desc) ->
                        val isSelected = selectedFormat == fmt
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) StudioPrimary.copy(alpha = 0.15f) else StudioDarkCard,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) StudioPrimary else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setFormat(fmt) }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = fmt,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) StudioPrimary else StudioTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tag,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) StudioPrimary else StudioTextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. STORAGE FOLDER LOCATION BANNER
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lokasi File di Memori HP",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                    Text(
                        text = "Penyimpanan Internal / Music / BrowserRecordings",
                        fontSize = 11.sp,
                        color = StudioTextSecondary
                    )
                    Text(
                        text = "Dapat diputar langsung di pemutar musik apa saja atau dikirim via WhatsApp.",
                        fontSize = 10.sp,
                        color = StudioTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
