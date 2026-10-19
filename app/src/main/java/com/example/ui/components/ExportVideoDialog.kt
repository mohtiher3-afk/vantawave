package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.AudioTrack
import com.example.export.AspectRatioPreset
import com.example.export.ExportProgress
import com.example.export.ExportStage
import com.example.export.VideoExportSettings
import com.example.ui.theme.VantaBackground
import com.example.ui.theme.VantaPrimary
import com.example.ui.theme.VantaSecondary
import com.example.ui.theme.VantaSurface
import com.example.ui.theme.VantaSurfaceElevated
import com.example.ui.theme.VantaSurfaceVariant
import com.example.ui.theme.VantaTertiary
import com.example.ui.theme.VantaTextMuted
import com.example.ui.theme.VantaTextPrimary
import com.example.ui.theme.VantaTextSecondary
import com.example.visualizer.VisualizerRenderConfig
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportVideoDialog(
    track: AudioTrack,
    renderConfig: VisualizerRenderConfig,
    exportProgress: ExportProgress,
    savedExportSettings: com.example.data.ExportSettingsEntity? = null,
    onStartExport: (VideoExportSettings) -> Unit,
    onCancelExport: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initialRatio = remember(savedExportSettings) {
        VideoExportSettings.ALL_RATIOS.find { it.name == savedExportSettings?.aspectRatioName }
            ?: VideoExportSettings.RATIO_9_16_HD
    }
    var selectedRatio by remember(initialRatio) { mutableStateOf(initialRatio) }
    var selectedDurationOption by remember(savedExportSettings) {
        mutableIntStateOf(
            if (savedExportSettings?.isFullTrack == true) -1
            else (savedExportSettings?.durationSeconds ?: 15)
        )
    }
    var selectedFps by remember(savedExportSettings) {
        mutableIntStateOf(savedExportSettings?.fps ?: 30)
    }

    val isExporting = exportProgress.stage in listOf(
        ExportStage.PREPARING,
        ExportStage.ANALYZING_AUDIO,
        ExportStage.ENCODING_VIDEO,
        ExportStage.MUXING_AUDIO
    )
    val isCompleted = exportProgress.stage == ExportStage.COMPLETED
    val isFailed = exportProgress.stage == ExportStage.FAILED

    Dialog(
        onDismissRequest = {
            if (!isExporting) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, VantaPrimary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .testTag("export_video_dialog"),
            color = VantaSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = VantaSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export Music Video",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = VantaTextPrimary
                        )
                    }

                    if (!isExporting) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("export_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = VantaTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isExporting) {
                    // Modern Multi-Stage Video Export Progress Indicator
                    VideoExportProgressIndicator(
                        exportProgress = exportProgress,
                        onCancelExport = onCancelExport,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else if (isCompleted) {
                    // Success View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(56.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Video Saved Successfully!",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = VantaTextPrimary
                        )

                        Text(
                            text = "Saved to Movies/VantaWave",
                            fontSize = 13.sp,
                            color = VantaTextSecondary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            exportProgress.outputFile?.let { file ->
                                Button(
                                    onClick = { playExportedVideo(context, file) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_play_video_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = VantaPrimary)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play")
                                }

                                Button(
                                    onClick = { shareExportedVideo(context, file) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_share_video_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = VantaSecondary)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done", color = VantaTextPrimary)
                        }
                    }
                } else {
                    // Export Configuration View
                    if (isFailed) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(VantaTertiary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = VantaTertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = exportProgress.error ?: "Encoding error",
                                color = VantaTertiary,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Track info preview
                    Text(
                        text = "Source Track: ${track.title} • ${track.artist}",
                        fontSize = 13.sp,
                        color = VantaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Aspect Ratio Selector
                    Text(
                        text = "Aspect Ratio",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = VantaTextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VideoExportSettings.ALL_RATIOS.forEach { ratio ->
                            val isSelected = selectedRatio.name == ratio.name
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) VantaPrimary else VantaSurfaceVariant)
                                    .border(
                                        1.dp,
                                        if (isSelected) VantaSecondary else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedRatio = ratio }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = ratio.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else VantaTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Clip Duration Selector
                    Text(
                        text = "Duration / Clip Trimming",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = VantaTextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val durationOptions = listOf(
                        15 to "15s (Reels/TikTok)",
                        30 to "30s (Shorts)",
                        60 to "60s Clip",
                        -1 to "Full Track (${track.formattedDuration})"
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durationOptions.forEach { (seconds, label) ->
                            val isSelected = selectedDurationOption == seconds
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) VantaSecondary else VantaSurfaceVariant)
                                    .clickable { selectedDurationOption = seconds }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else VantaTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Frame Rate Selector
                    Text(
                        text = "Frame Rate",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = VantaTextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30, 24).forEach { fps ->
                            val isSelected = selectedFps == fps
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) VantaPrimary else VantaSurfaceVariant)
                                    .clickable { selectedFps = fps }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$fps FPS",
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else VantaTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Export CTA Button
                    Button(
                        onClick = {
                            val isFull = selectedDurationOption == -1
                            val durationSec = if (isFull) (track.durationMs / 1000).toInt() else selectedDurationOption
                            val settings = VideoExportSettings(
                                track = track,
                                title = track.title,
                                aspectRatioName = selectedRatio.name,
                                width = selectedRatio.width,
                                height = selectedRatio.height,
                                fps = selectedFps,
                                durationSeconds = durationSec,
                                isFullTrack = isFull,
                                renderConfig = renderConfig.copy(
                                    titleText = track.title,
                                    artistText = track.artist
                                )
                            )
                            onStartExport(settings)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("export_start_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VantaPrimary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Start Video Export",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun playExportedVideo(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(file), "video/mp4")
        }
        context.startActivity(Intent.createChooser(fallbackIntent, "Play Video"))
    }
}

private fun shareExportedVideo(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Music Video"))
    } catch (e: Exception) {
        // Fallback
    }
}
