package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class VisualizerMode(val title: String) {
    WAVEFORM_PULSE("Waveform Pulse"),
    FREQUENCY_BARS("Frequency Bars"),
    PARTICLES("Particles"),
    GEOMETRIC_ORBITS("Geometric Orbits"),
    NEON_SPIRAL("Neon Spiral")
}

@Composable
fun VisualizerModeSelector(
    selectedMode: VisualizerMode,
    onModeSelected: (VisualizerMode) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(VisualizerMode.values()) { mode ->
            VisualizerModeItem(
                mode = mode,
                isSelected = (mode == selectedMode),
                onClick = { onModeSelected(mode) }
            )
        }
    }
}

@Composable
private fun VisualizerModeItem(
    mode: VisualizerMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) VantaPrimary else VantaCardBorder
    val backgroundColor = if (isSelected) VantaSurfaceElevated.copy(alpha = 0.5f) else VantaGlassSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .backdropBlur(15f)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Miniature Preview
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(VantaSurfaceVariant)
                .padding(4.dp)
        ) {
            ModePreviewCanvas(mode = mode)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = mode.title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) VantaTextPrimary else VantaTextSecondary
            )
        )
    }
}

@Composable
private fun ModePreviewCanvas(mode: VisualizerMode) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2

        when (mode) {
            VisualizerMode.WAVEFORM_PULSE -> {
                val path = Path()
                for (i in 0..10) {
                    val x = width * i / 10
                    val y = centerY + 10.dp.toPx() * sin(i.toFloat())
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, VantaPrimary, style = Stroke(width = 2.dp.toPx()))
            }
            VisualizerMode.FREQUENCY_BARS -> {
                for (i in 0..6) {
                    val barHeight = (i + 1) * height / 8
                    drawRect(
                        color = VantaSecondary,
                        topLeft = androidx.compose.ui.geometry.Offset(i * width / 7, height - barHeight),
                        size = androidx.compose.ui.geometry.Size(width / 9, barHeight)
                    )
                }
            }
            VisualizerMode.PARTICLES -> {
                for (i in 0..5) {
                    drawCircle(VantaTertiary, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(i * width / 5, centerY))
                }
            }
            VisualizerMode.GEOMETRIC_ORBITS -> {
                drawCircle(VantaPrimary, radius = 10.dp.toPx(), center = androidx.compose.ui.geometry.Offset(centerX, centerY), style = Stroke(2.dp.toPx()))
                drawCircle(VantaSecondary, radius = 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(centerX + 15.dp.toPx(), centerY))
            }
            VisualizerMode.NEON_SPIRAL -> {
                drawCircle(VantaTertiary, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(centerX, centerY))
                drawCircle(VantaPrimary, radius = 5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(centerX + 10.dp.toPx(), centerY + 10.dp.toPx()))
            }
        }
    }
}
