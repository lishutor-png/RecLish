package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
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
        // HEADER TITLE
        Column {
            Text(
                text = "Pengaturan & Floating Mode",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = StudioTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Merekam suara di atas Chrome / browser lain di latar belakang",
                fontSize = 12.sp,
                color = StudioTextSecondary
            )
        }

        // 1. FLOATING OVERLAY TOGGLE CARD ("Berjalan di atas aplikasi lain")
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
                            text = "Bubble Mengambang",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary
                        )
                        Text(
                            text = if (isOverlayActive) "Aktif di atas semua aplikasi" else "Tidak aktif",
                            fontSize = 12.sp,
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
                                )
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

                Spacer(modifier = Modifier.height(12.dp))

                // Permission Warning or Success
                if (!hasOverlayPermission) {
                    Surface(
                        color = StudioWarningAmber.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = StudioWarningAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Izin 'Tampilkan di atas aplikasi lain' diperlukan",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioWarningAmber
                                )
                                Text(
                                    text = "Ketuk untuk membuka pengaturan Android dan aktifkan izin.",
                                    fontSize = 11.sp,
                                    color = StudioTextSecondary
                                )
                            }
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioWarningAmber),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Buka", fontSize = 11.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Surface(
                        color = StudioSuccessGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioSuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Izin overlay aktif. Bubble dapat digeser ke posisi mana saja.",
                                fontSize = 11.sp,
                                color = StudioSuccessGreen
                            )
                        }
                    }
                }
            }
        }

        // 2. AUDIO SOURCE SELECTOR CARD
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
                    text = "Sumber Audio Perekaman",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pilih dari mana suara akan direkam saat browsing:",
                    fontSize = 12.sp,
                    color = StudioTextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 1: Browser Internal Audio Playback Capture
                val isInternal = selectedSource == AudioCaptureSource.BROWSER_INTERNAL
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isInternal) StudioPrimary.copy(alpha = 0.12f) else StudioDarkCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isInternal) StudioPrimary else Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.setSource(AudioCaptureSource.BROWSER_INTERNAL)
                            onRequestMediaProjection()
                        }
                        .testTag("source_browser_internal")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (isInternal) StudioPrimary else StudioTextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Audio Tab Browser (Internal)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isInternal) StudioPrimary else StudioTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = StudioSuccessGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Jernih Tanpa Noise",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioSuccessGreen,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Menangkap suara langsung dari sistem Android tanpa suara bising ruangan.",
                                fontSize = 11.sp,
                                color = StudioTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: Microphone Capture
                val isMic = selectedSource == AudioCaptureSource.MICROPHONE
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isMic) StudioPrimary.copy(alpha = 0.12f) else StudioDarkCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isMic) StudioPrimary else Color(0xFF334155)
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
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isMic) StudioPrimary else StudioTextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mikrofon (Speaker & Suara Luar)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMic) StudioPrimary else StudioTextPrimary
                            )
                            Text(
                                text = "Merekam suara speaker HP serta komentar suara pengguna.",
                                fontSize = 11.sp,
                                color = StudioTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 3. AUDIO FORMAT & QUALITY CARD
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
                    text = "Format & Kualitas Audio",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dukungan format file audio yang disimpan ke memori HP:",
                    fontSize = 12.sp,
                    color = StudioTextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("MP3", "Standar & Ringan", "Kompatibel semua pemutar"),
                        Triple("WAV", "Studio PCM", "Kualitas tanpa kompresi"),
                        Triple("M4A", "High Efficiency", "Format AAC modern")
                    ).forEach { (fmt, sub, desc) ->
                        val isSelected = selectedFormat == fmt
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) StudioPrimary.copy(alpha = 0.15f) else StudioDarkCard,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
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
                                    text = sub,
                                    fontSize = 10.sp,
                                    color = if (isSelected) StudioPrimary else StudioTextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. STEP-BY-STEP TUTORIAL CARD
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cara Merekam Suara dari Chrome / Browser",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                listOf(
                    "1" to "Nyalakan sakelar 'Bubble Mengambang' di atas.",
                    "2" to "Buka Google Chrome atau browser favorit Anda.",
                    "3" to "Buka tab web yang memutar musik, podcast, atau audio.",
                    "4" to "Ketuk tombol 'Mulai' pada bubble mengambang di pojok layar.",
                    "5" to "Aplikasi merekam di latar belakang tanpa mengganggu browsing Anda.",
                    "6" to "Ketuk 'Simpan' saat selesai. Hasil rekaman MP3 otomatis tersimpan di memori HP Anda!"
                ).forEach { (step, text) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = StudioSecondary.copy(alpha = 0.2f),
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = step, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = text, fontSize = 12.sp, color = StudioTextSecondary, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (!isOverlayActive && hasOverlayPermission) {
                            viewModel.toggleFloatingOverlay()
                        }
                        val chromeIntent = context.packageManager.getLaunchIntentForPackage("com.android.chrome")
                            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                        context.startActivity(chromeIntent)
                        viewModel.showSnack("Membuka Google Chrome dengan RecLish Overlay!")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioRecordRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_launch_chrome")
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buka Google Chrome Sekarang", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 5. STORAGE LOCATION INFO
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF162035)),
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
                        text = "Penyimpanan di Memori HP",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                    Text(
                        text = "Penyimpanan Internal/Music/BrowserRecordings",
                        fontSize = 11.sp,
                        color = StudioTextSecondary
                    )
                    Text(
                        text = "File dapat diputar di pemutar musik mana saja & dapat dibagikan langsung.",
                        fontSize = 10.sp,
                        color = StudioTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
