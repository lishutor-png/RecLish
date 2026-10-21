package com.example.ui.screens

import android.content.Context
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.RecordingEntity
import com.example.ui.components.PlaybackWaveformScrubber
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
import com.example.ui.viewmodel.SortOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Semua") }

    // Dialog states
    var recordingToRename by remember { mutableStateOf<RecordingEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var recordingToDelete by remember { mutableStateOf<RecordingEntity?>(null) }

    // Filter by format or favorite
    val displayedRecordings = remember(recordings, selectedCategory) {
        when (selectedCategory) {
            "MP3" -> recordings.filter { it.format.equals("MP3", ignoreCase = true) }
            "WAV" -> recordings.filter { it.format.equals("WAV", ignoreCase = true) }
            "M4A" -> recordings.filter { it.format.equals("M4A", ignoreCase = true) }
            "Favorit" -> recordings.filter { it.isFavorite }
            else -> recordings
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP HEADER & SEARCH BAR
        Surface(
            color = StudioDarkSurface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Manajemen Rekaman",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary
                        )
                        Text(
                            text = "${recordings.size} file tersimpan di memori HP",
                            fontSize = 12.sp,
                            color = StudioTextSecondary
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("btn_sort_menu")
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Urutkan", tint = StudioPrimary)
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(StudioDarkCard)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Terbaru", color = StudioTextPrimary) },
                                onClick = {
                                    viewModel.setSortOption(SortOption.DATE_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Terlama", color = StudioTextPrimary) },
                                onClick = {
                                    viewModel.setSortOption(SortOption.DATE_ASC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Nama (A-Z)", color = StudioTextPrimary) },
                                onClick = {
                                    viewModel.setSortOption(SortOption.TITLE_AZ)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Durasi Terpanjang", color = StudioTextPrimary) },
                                onClick = {
                                    viewModel.setSortOption(SortOption.DURATION_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ukuran Terbesar", color = StudioTextPrimary) },
                                onClick = {
                                    viewModel.setSortOption(SortOption.SIZE_DESC)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("search_recordings_input"),
                    placeholder = { Text("Cari nama rekaman audio...", fontSize = 12.sp, color = StudioTextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = StudioPrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus", tint = StudioTextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = StudioDarkCard,
                        unfocusedContainerColor = StudioDarkCard,
                        focusedBorderColor = StudioPrimary,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = StudioTextPrimary,
                        unfocusedTextColor = StudioTextPrimary
                    )
                )

                // Category filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Semua", "MP3", "WAV", "M4A", "Favorit").forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = StudioDarkCard,
                                labelColor = StudioTextSecondary,
                                selectedContainerColor = StudioPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = StudioPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedCategory == cat,
                                borderColor = Color(0xFF334155),
                                selectedBorderColor = StudioPrimary
                            )
                        )
                    }
                }
            }
        }

        // STORAGE PATH BANNER
        Surface(
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = StudioPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Lokasi: Memori HP / Music / BrowserRecordings",
                    fontSize = 11.sp,
                    color = StudioTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // RECORDINGS LIST OR EMPTY STATE
        if (displayedRecordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Tidak ada rekaman yang cocok" else "Belum Ada Rekaman",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Mulai rekam suara dari tab browser di halaman Studio atau gunakan Floating Bubble di atas Chrome!",
                        fontSize = 12.sp,
                        color = StudioTextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayedRecordings, key = { it.id }) { rec ->
                    RecordingItemCard(
                        recording = rec,
                        isPlaying = playbackState.currentRecording?.id == rec.id && playbackState.isPlaying,
                        isCurrentTrack = playbackState.currentRecording?.id == rec.id,
                        onPlayClick = {
                            if (playbackState.currentRecording?.id == rec.id && playbackState.isPlaying) {
                                viewModel.pausePlayback()
                            } else {
                                viewModel.playRecording(rec)
                            }
                        },
                        onRenameClick = {
                            recordingToRename = rec
                            renameInput = rec.title
                        },
                        onDeleteClick = {
                            recordingToDelete = rec
                        },
                        onShareClick = {
                            val shareIntent = viewModel.getShareIntent(rec)
                            shareIntent?.let {
                                context.startActivity(android.content.Intent.createChooser(it, "Bagikan Rekaman Audio"))
                            }
                        },
                        onExportClick = {
                            viewModel.exportToDownloads(rec)
                        },
                        onFavoriteClick = {
                            viewModel.toggleFavorite(rec)
                        }
                    )
                }
            }
        }

        // FLOATING AUDIO PLAYER BOTTOM BAR
        AnimatedVisibility(
            visible = playbackState.currentRecording != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            playbackState.currentRecording?.let { track ->
                PlayerBottomBar(
                    track = track,
                    playbackState = playbackState,
                    onTogglePlay = {
                        if (playbackState.isPlaying) viewModel.pausePlayback() else viewModel.resumePlayback()
                    },
                    onSeek = { fraction -> viewModel.seekPlayback(fraction) },
                    onSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
                    onClose = { viewModel.stopPlayback() }
                )
            }
        }
    }

    // RENAME DIALOG
    recordingToRename?.let { rec ->
        AlertDialog(
            onDismissRequest = { recordingToRename = null },
            title = { Text("Ganti Nama Rekaman", color = StudioTextPrimary) },
            text = {
                Column {
                    Text("Ketik nama baru untuk file audio ini:", fontSize = 12.sp, color = StudioTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rename_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = StudioDarkCard,
                            unfocusedContainerColor = StudioDarkCard,
                            focusedBorderColor = StudioPrimary,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = StudioTextPrimary,
                            unfocusedTextColor = StudioTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renameRecording(rec, renameInput.trim())
                            recordingToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary)
                ) {
                    Text("Simpan", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToRename = null }) {
                    Text("Batal", color = StudioTextSecondary)
                }
            },
            containerColor = StudioDarkSurface
        )
    }

    // DELETE CONFIRMATION DIALOG
    recordingToDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            title = { Text("Hapus Rekaman?", color = StudioRecordRed, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus '${rec.title}' (${rec.fileName}) secara permanen dari memori HP?",
                    color = StudioTextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecording(rec)
                        recordingToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioRecordRed)
                ) {
                    Text("Hapus", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToDelete = null }) {
                    Text("Batal", color = StudioTextSecondary)
                }
            },
            containerColor = StudioDarkSurface
        )
    }
}

@Composable
fun RecordingItemCard(
    recording: RecordingEntity,
    isPlaying: Boolean,
    isCurrentTrack: Boolean,
    onPlayClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    onExportClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val formattedDuration = remember(recording.durationMs) {
        val totalSecs = recording.durationMs / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    val formattedSize = remember(recording.fileSizeBytes) {
        Formatter.formatFileSize(context, recording.fileSizeBytes)
    }

    val formattedDate = remember(recording.createdAt) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(recording.createdAt))
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTrack) Color(0xFF162035) else StudioDarkCard
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isCurrentTrack) StudioPrimary.copy(alpha = 0.5f) else Color(0xFF26334D),
                RoundedCornerShape(14.dp)
            )
            .testTag("recording_item_${recording.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause round button
                Surface(
                    shape = CircleShape,
                    color = if (isPlaying) StudioRecordRed else StudioPrimary,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onPlayClick() }
                        .testTag("play_btn_${recording.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Meta Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = recording.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Format Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (recording.format == "MP3") StudioPrimary.copy(alpha = 0.2f) else StudioSecondary.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = recording.format,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (recording.format == "MP3") StudioPrimary else StudioSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = recording.fileName,
                        fontSize = 11.sp,
                        color = StudioTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = formattedDuration, fontSize = 11.sp, color = StudioPrimary, fontWeight = FontWeight.SemiBold)
                        Text(text = "•", fontSize = 10.sp, color = StudioTextMuted)
                        Text(text = formattedSize, fontSize = 11.sp, color = StudioTextSecondary)
                        Text(text = "•", fontSize = 10.sp, color = StudioTextMuted)
                        Text(text = formattedDate, fontSize = 10.sp, color = StudioTextMuted)
                    }
                }

                // Favorite and Overflow actions
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(32.dp).testTag("fav_btn_${recording.id}")
                ) {
                    Icon(
                        imageVector = if (recording.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorit",
                        tint = if (recording.isFavorite) StudioRecordRed else StudioTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp).testTag("more_btn_${recording.id}")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = StudioTextSecondary, modifier = Modifier.size(20.dp))
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(StudioDarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ganti Nama", color = StudioTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = StudioPrimary) },
                            onClick = {
                                showMenu = false
                                onRenameClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bagikan Audio", color = StudioTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = StudioPrimary) },
                            onClick = {
                                showMenu = false
                                onShareClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ekspor ke Download", color = StudioTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = StudioSuccessGreen) },
                            onClick = {
                                showMenu = false
                                onExportClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus File", color = StudioRecordRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StudioRecordRed) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerBottomBar(
    track: RecordingEntity,
    playbackState: com.example.audio.PlaybackState,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = Color(0xFF131D31),
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF26334D), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("player_bottom_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Track Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = StudioPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = track.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val totalSecs = playbackState.currentPositionMs / 1000
                        val mins = totalSecs / 60
                        val secs = totalSecs % 60
                        val curStr = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

                        val totalDurSecs = playbackState.totalDurationMs / 1000
                        val totalMins = totalDurSecs / 60
                        val totalSecsRem = totalDurSecs % 60
                        val totStr = String.format(Locale.getDefault(), "%02d:%02d", totalMins, totalSecsRem)

                        Text(
                            text = "$curStr / $totStr • ${track.format}",
                            fontSize = 11.sp,
                            color = StudioTextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Play/Pause button
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier.size(36.dp).testTag("bottom_play_pause")
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = StudioPrimary
                        )
                    }

                    // Close player button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp).testTag("bottom_close_player")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = StudioTextMuted)
                    }
                }
            }

            // Interactive Waveform Scrubber
            val progress = if (playbackState.totalDurationMs > 0) {
                (playbackState.currentPositionMs.toFloat() / playbackState.totalDurationMs).coerceIn(0f, 1f)
            } else 0f

            PlaybackWaveformScrubber(
                progress = progress,
                durationMs = playbackState.totalDurationMs,
                onSeek = onSeek,
                seed = track.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            // Speed chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Kecepatan Putar:", fontSize = 11.sp, color = StudioTextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                        val isSelected = playbackState.playbackSpeed == spd
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) StudioPrimary else Color(0xFF1E293B),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onSpeedChange(spd) }
                        ) {
                            Text(
                                text = "${spd}x",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF0F172A) else StudioTextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
