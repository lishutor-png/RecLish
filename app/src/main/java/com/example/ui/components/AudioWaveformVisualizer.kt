package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioRecordRed
import com.example.ui.theme.StudioSecondary
import kotlin.random.Random

@Composable
fun AudioWaveformVisualizer(
    amplitude: Float,
    isRecording: Boolean,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 28
) {
    // Generate animated heights for visualizer bars
    val animatedAmplitudes = remember(barCount) {
        List(barCount) { Animatable(0.08f) }
    }

    LaunchedEffect(amplitude, isRecording, isPaused) {
        if (isRecording && !isPaused) {
            animatedAmplitudes.forEachIndexed { index, animatable ->
                val variation = 0.5f + (Random.nextFloat() * 0.9f)
                val target = ((amplitude * variation) + 0.12f).coerceIn(0.1f, 1.0f)
                animatable.animateTo(
                    targetValue = target,
                    animationSpec = tween(
                        durationMillis = 90 + (index % 4) * 20,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        } else if (isPaused) {
            animatedAmplitudes.forEach { animatable ->
                animatable.animateTo(0.18f, tween(150))
            }
        } else {
            animatedAmplitudes.forEach { animatable ->
                animatable.animateTo(0.08f, tween(200))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val totalWidth = size.width
            val height = size.height
            val spacing = 3.dp.toPx()
            val barWidth = ((totalWidth - (spacing * (barCount - 1))) / barCount).coerceAtLeast(2.dp.toPx())

            val gradientBrush = when {
                isRecording && !isPaused -> Brush.verticalGradient(
                    colors = listOf(StudioRecordRed, StudioPrimary)
                )
                isPaused -> Brush.verticalGradient(
                    colors = listOf(Color(0xFFF59E0B), StudioSecondary)
                )
                else -> Brush.verticalGradient(
                    colors = listOf(Color(0xFF475569), Color(0xFF1E293B))
                )
            }

            for (i in 0 until barCount) {
                val barHeightFactor = animatedAmplitudes[i].value
                val barHeight = (height * barHeightFactor).coerceAtLeast(4.dp.toPx())
                val x = i * (barWidth + spacing)
                val y = (height - barHeight) / 2f

                drawRoundRect(
                    brush = gradientBrush,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
}
