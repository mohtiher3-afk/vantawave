package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text

// ... (in MusicPlayerControls function)
@Composable
fun MusicPlayerControls(
    isPlaying: Boolean,
    currentTime: Float,
    duration: Float,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Seekbar
        Slider(
            value = currentTime,
            onValueChange = onSeek,
            valueRange = 0f..duration,
            colors = SliderDefaults.colors(
                thumbColor = VantaPrimary,
                activeTrackColor = VantaPrimary,
                inactiveTrackColor = VantaSurfaceVariant
            )
        )
        
        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeonControlButton(
                icon = Icons.Default.SkipPrevious,
                onClick = onPrevious,
                size = 48.dp
            )
            NeonControlButton(
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                onClick = onPlayPause,
                size = 72.dp,
                isPrimary = true
            )
            NeonControlButton(
                icon = Icons.Default.SkipNext,
                onClick = onNext,
                size = 48.dp
            )
        }
    }
}

@Composable
private fun NeonControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    isPrimary: Boolean = false
) {
    val backgroundColor = if (isPrimary) VantaPrimary else VantaSurfaceElevated
    val iconColor = VantaTextPrimary
    val glowColor = if (isPrimary) VantaGlow else VantaCardBorder

    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = if (isPrimary) 20.dp else 10.dp,
                shape = CircleShape,
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .clip(CircleShape)
            .background(backgroundColor)
            .border(2.dp, if (isPrimary) VantaSecondary else VantaCardBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(size / 2)
        )
    }
}
