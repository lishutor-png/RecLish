package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StudioPrimary
import kotlin.random.Random

@Composable
fun PlaybackWaveformScrubber(
    progress: Float, // 0.0 to 1.0
    durationMs: Long,
    onSeek: (Float) -> Unit,
    seed: Long = 42L,
    modifier: Modifier = Modifier,
    barCount: Int = 36
) {
    // Generate pseudo-waveform based on recording id/seed for consistent look
    val heights = remember(seed, barCount) {
        val rand = Random(seed)
        FloatArray(barCount) {
            0.2f + (rand.nextFloat() * 0.75f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(fraction)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val totalWidth = size.width
            val height = size.height
            val spacing = 2.5.dp.toPx()
            val barWidth = ((totalWidth - (spacing * (barCount - 1))) / barCount).coerceAtLeast(2.dp.toPx())

            val activeColor = StudioPrimary
            val inactiveColor = Color(0xFF334155)

            for (i in 0 until barCount) {
                val barFraction = i.toFloat() / barCount
                val isPlayed = barFraction <= progress
                val barHeight = (height * heights[i]).coerceAtLeast(3.dp.toPx())
                val x = i * (barWidth + spacing)
                val y = (height - barHeight) / 2f

                drawRoundRect(
                    color = if (isPlayed) activeColor else inactiveColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}
