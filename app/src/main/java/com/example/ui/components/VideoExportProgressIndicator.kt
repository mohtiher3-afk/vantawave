package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.export.ExportProgress
import com.example.export.ExportStage
import com.example.ui.theme.NeonPurpleGradient
import com.example.ui.theme.VantaCardBorder
import com.example.ui.theme.VantaGlassSurface
import com.example.ui.theme.VantaGlow
import com.example.ui.theme.VantaPrimary
import com.example.ui.theme.VantaSecondary
import com.example.ui.theme.VantaSurfaceElevated
import com.example.ui.theme.VantaSurfaceVariant
import com.example.ui.theme.VantaTertiary
import com.example.ui.theme.VantaTextMuted
import com.example.ui.theme.VantaTextPrimary
import com.example.ui.theme.VantaTextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-tech visual progress indicator for MP4 hardware encoding pipeline.
 * Features an animated glowing radial progress gauge, multi-stage pipeline stepper,
 * real-time frame telemetry, and scanline progress bar.
 */
@Composable
fun VideoExportProgressIndicator(
    exportProgress: ExportProgress,
    onCancelExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = exportProgress.progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "export_progress_anim"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val scanOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_offset"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_export_progress_indicator"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Futuristic Glowing Radial Progress Gauge
        Box(
            modifier = Modifier
                .size(140.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(124.dp)) {
                val strokeWidth = 9.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // Background Track Arc
                drawCircle(
                    color = VantaSurfaceVariant.copy(alpha = 0.6f),
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth)
                )

                // Outer ambient glow shadow arc
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(VantaPrimary, VantaSecondary, VantaTertiary, VantaPrimary),
                        center = centerOffset
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth * 1.6f, cap = StrokeCap.Round),
                    alpha = 0.25f * pulseAlpha
                )

                // Foreground Glowing Gradient Progress Arc
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(VantaPrimary, VantaSecondary, VantaTertiary, VantaPrimary),
                        center = centerOffset
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Leading glowing head particle dot
                if (animatedProgress > 0.01f) {
                    val angleRad = (-90f + animatedProgress * 360f) * (PI / 180f)
                    val headX = centerOffset.x + radius * cos(angleRad).toFloat()
                    val headY = centerOffset.y + radius * sin(angleRad).toFloat()

                    drawCircle(
                        color = Color.White,
                        radius = strokeWidth * 0.55f,
                        center = Offset(headX, headY)
                    )
                    drawCircle(
                        color = VantaSecondary,
                        radius = strokeWidth * 0.9f,
                        center = Offset(headX, headY),
                        alpha = pulseAlpha
                    )
                }
            }

            // Radial Center Information
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = VantaTextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = when (exportProgress.stage) {
                        ExportStage.PREPARING -> "PREPARING"
                        ExportStage.ANALYZING_AUDIO -> "ANALYZING"
                        ExportStage.ENCODING_VIDEO -> "ENCODING"
                        ExportStage.MUXING_AUDIO -> "MUXING"
                        ExportStage.COMPLETED -> "READY"
                        else -> "ACTIVE"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = VantaSecondary,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Multi-Stage Pipeline Stepper
        PipelineStageStepper(currentStage = exportProgress.stage)

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Real-Time Telemetry & Frame Metrics Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(VantaGlassSurface)
                .backdropBlur(10f)
                .border(1.dp, VantaCardBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(VantaSecondary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STATUS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VantaTextMuted,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (exportProgress.totalFrames > 0) {
                        Text(
                            text = "Frame ${exportProgress.currentFrame} / ${exportProgress.totalFrames}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VantaSecondary
                        )
                    }
                }

                Text(
                    text = if (exportProgress.message.isNotBlank()) exportProgress.message else "Rendering MP4 video...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = VantaTextPrimary
                )

                // Scanline Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(VantaSurfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(VantaPrimary, VantaSecondary, VantaTertiary)
                                )
                            )
                    )

                    // Moving scanline beam
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(8.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.45f),
                                        Color.Transparent
                                    ),
                                    startX = scanOffset * 500f,
                                    endX = scanOffset * 500f + 120f
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Hardware H.264 Encoder",
                        fontSize = 11.sp,
                        color = VantaTextMuted
                    )
                    Text(
                        text = "AAC Audio Sync",
                        fontSize = 11.sp,
                        color = VantaTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Cancel Button
        OutlinedButton(
            onClick = onCancelExport,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VantaTertiary),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.horizontalGradient(listOf(VantaTertiary.copy(alpha = 0.5f), VantaTertiary))
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("export_cancel_button")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Cancel Export",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 4-step pipeline stepper showing progress across Preparation, Audio Analysis,
 * Video Rendering, and Muxing.
 */
@Composable
private fun PipelineStageStepper(currentStage: ExportStage) {
    val stages = listOf(
        PipelineStep("Prep", Icons.Default.Settings, ExportStage.PREPARING),
        PipelineStep("Audio", Icons.Default.GraphicEq, ExportStage.ANALYZING_AUDIO),
        PipelineStep("Render", Icons.Default.Videocam, ExportStage.ENCODING_VIDEO),
        PipelineStep("Mux", Icons.Default.Movie, ExportStage.MUXING_AUDIO)
    )

    val currentStageIndex = when (currentStage) {
        ExportStage.PREPARING -> 0
        ExportStage.ANALYZING_AUDIO -> 1
        ExportStage.ENCODING_VIDEO -> 2
        ExportStage.MUXING_AUDIO -> 3
        ExportStage.COMPLETED -> 4
        else -> 0
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        stages.forEachIndexed { index, step ->
            val isCompleted = index < currentStageIndex
            val isCurrent = index == currentStageIndex
            val isPending = index > currentStageIndex

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(60.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> VantaSecondary
                                isCurrent -> VantaPrimary
                                else -> VantaSurfaceVariant
                            }
                        )
                        .border(
                            1.5.dp,
                            when {
                                isCompleted -> VantaSecondary
                                isCurrent -> Color.White
                                else -> VantaCardBorder
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.Check else step.icon,
                        contentDescription = step.title,
                        tint = when {
                            isCompleted -> Color.Black
                            isCurrent -> Color.White
                            else -> VantaTextMuted
                        },
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = step.title,
                    fontSize = 11.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isCompleted -> VantaSecondary
                        isCurrent -> VantaTextPrimary
                        else -> VantaTextMuted
                    }
                )
            }

            if (index < stages.lastIndex) {
                // Connecting bar between steps
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 2.dp)
                        .background(
                            if (index < currentStageIndex) VantaSecondary
                            else VantaSurfaceVariant
                        )
                )
            }
        }
    }
}

private data class PipelineStep(
    val title: String,
    val icon: ImageVector,
    val stage: ExportStage
)
