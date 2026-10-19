package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AudioVisualizerCanvas(
    mode: VisualizerMode,
    audioData: FloatArray, // Normalized 0.0f - 1.0f
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        when (mode) {
            VisualizerMode.WAVEFORM_PULSE -> {
                val path = Path()
                audioData.forEachIndexed { index, value ->
                    val x = width * index / audioData.size
                    val y = height / 2 + (value * height / 2 * sin(index.toFloat() * 0.1f))
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, VantaPrimary, style = Stroke(width = 4.dp.toPx()))
            }
            VisualizerMode.FREQUENCY_BARS -> {
                val barWidth = width / (audioData.size * 1.5f)
                audioData.forEachIndexed { index, value ->
                    val barHeight = value * height
                    drawRect(
                        color = VantaSecondary,
                        topLeft = androidx.compose.ui.geometry.Offset(index * (barWidth * 1.5f), height - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                    )
                }
            }
            VisualizerMode.PARTICLES -> {
                audioData.forEachIndexed { index, value ->
                    val x = (index * width / audioData.size)
                    val y = height - (value * height)
                    drawCircle(
                        color = VantaTertiary,
                        radius = value * 20.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                }
            }
            VisualizerMode.GEOMETRIC_ORBITS -> {
                val centerX = width / 2
                val centerY = height / 2
                audioData.forEachIndexed { index, value ->
                    val angle = index * 2 * PI / audioData.size
                    val radius = value * (height / 3)
                    drawCircle(
                        color = if (index % 2 == 0) VantaPrimary else VantaSecondary,
                        radius = 5.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(
                            centerX + radius * cos(angle.toFloat()),
                            centerY + radius * sin(angle.toFloat())
                        )
                    )
                }
            }
            VisualizerMode.NEON_SPIRAL -> {
                val centerX = width / 2
                val centerY = height / 2
                audioData.forEachIndexed { index, value ->
                    val angle = index * 0.1f
                    val radius = index * 2.0f * value
                    drawCircle(
                        color = if (index % 3 == 0) VantaTertiary else VantaPrimary,
                        radius = 3.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(
                            centerX + radius * cos(angle),
                            centerY + radius * sin(angle)
                        )
                    )
                }
            }
        }
    }
}
