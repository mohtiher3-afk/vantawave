package com.example.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.TranscriptionProgress
import com.example.data.AudioTrack
import com.example.transcription.KaraokeStyle
import com.example.transcription.LyricsSegment
import com.example.transcription.WordTimestamp
import com.example.ui.theme.VantaBackground
import com.example.ui.theme.VantaPrimary
import com.example.ui.theme.VantaSecondary
import com.example.ui.theme.VantaSurfaceElevated
import com.example.ui.theme.VantaSurfaceVariant
import com.example.ui.theme.VantaTertiary
import com.example.ui.theme.VantaTextMuted
import com.example.ui.theme.VantaTextPrimary
import com.example.ui.theme.VantaTextSecondary

@Composable
fun LyricsTimelineEditor(
    track: AudioTrack,
    segments: List<LyricsSegment>,
    currentPositionMs: Long,
    isPlaying: Boolean,
    transcriptionProgress: TranscriptionProgress,
    selectedKaraokeStyle: KaraokeStyle,
    onKaraokeStyleChange: (KaraokeStyle) -> Unit,
    onSeekTo: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onTranscribeTrack: () -> Unit,
    onNudgeSegment: (segment: LyricsSegment, deltaMs: Long) -> Unit,
    onUpdateSegmentText: (segment: LyricsSegment, newText: String) -> Unit,
    onDeleteSegment: (segment: LyricsSegment) -> Unit,
    onAddSegment: (startTimeMs: Long, text: String) -> Unit,
    onSaveToDatabase: () -> Unit,
    onDismiss: () -> Unit
) {
    var editingSegment by remember { mutableStateOf<LyricsSegment?>(null) }
    var editedText by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var newSegmentText by remember { mutableStateOf("") }
    var newSegmentTimeMs by remember { mutableStateOf(currentPositionMs) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(VantaBackground),
            color = VantaBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = VantaSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "On-Device AI Lyrics Studio",
                                color = VantaTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${track.title} • 100% Offline (Airplane Mode Ready)",
                            color = VantaTextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row {
                        IconButton(
                            onClick = onSaveToDatabase,
                            modifier = Modifier.testTag("save_lyrics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save Lyrics",
                                tint = VantaSecondary
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_lyrics_editor")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = VantaTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Transcription Progress / AI Trigger Banner
                if (transcriptionProgress.isRunning) {
                    Surface(
                        color = VantaSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = transcriptionProgress.stage,
                                    color = VantaSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${transcriptionProgress.percentage}%",
                                    color = VantaTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { transcriptionProgress.percentage / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = VantaSecondary,
                                trackColor = VantaSurfaceVariant
                            )
                        }
                    }
                } else if (segments.isEmpty()) {
                    // Empty state banner
                    Surface(
                        color = VantaSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No lyrics extracted yet for this track",
                                color = VantaTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Run on-device AI transcription to automatically extract words & timestamps",
                                color = VantaTextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onTranscribeTrack,
                                colors = ButtonDefaults.buttonColors(containerColor = VantaPrimary),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("extract_lyrics_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Extract Lyrics with On-Device AI", fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Karaoke Style Selector Row
                Text(
                    text = "Karaoke Animation Style",
                    color = VantaTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KaraokeStyle.values().forEach { style ->
                        val isSelected = style == selectedKaraokeStyle
                        FilterChip(
                            selected = isSelected,
                            onClick = { onKaraokeStyleChange(style) },
                            label = { Text(style.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VantaPrimary.copy(alpha = 0.25f),
                                selectedLabelColor = VantaSecondary,
                                containerColor = VantaSurfaceElevated,
                                labelColor = VantaTextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = VantaSurfaceVariant,
                                selectedBorderColor = VantaSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Timeline Controls & Scrub Bar
                Surface(
                    color = VantaSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPlayPause,
                            modifier = Modifier
                                .size(36.dp)
                                .background(VantaPrimary, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        val curSec = currentPositionMs / 1000
                        val durSec = track.durationMs / 1000
                        Text(
                            text = "%02d:%02d / %02d:%02d".format(curSec / 60, curSec % 60, durSec / 60, durSec % 60),
                            color = VantaTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row {
                            Button(
                                onClick = onTranscribeTrack,
                                colors = ButtonDefaults.buttonColors(containerColor = VantaSurfaceVariant),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VantaSecondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Re-Analyze", color = VantaSecondary, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    newSegmentTimeMs = currentPositionMs
                                    showAddDialog = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Line", tint = VantaSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Timeline List of Lyrics Segments
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(segments) { index, segment ->
                        val isActive = segment.isCurrentlyActive(currentPositionMs)
                        SegmentTimelineCard(
                            segment = segment,
                            isActive = isActive,
                            currentPositionMs = currentPositionMs,
                            onTapToSeek = { onSeekTo(segment.startTimeMs) },
                            onNudge = { delta -> onNudgeSegment(segment, delta) },
                            onEdit = {
                                editingSegment = segment
                                editedText = segment.text
                            },
                            onDelete = { onDeleteSegment(segment) }
                        )
                    }
                }
            }
        }
    }

    // Edit Segment Dialog
    if (editingSegment != null) {
        val seg = editingSegment!!
        Dialog(onDismissRequest = { editingSegment = null }) {
            Surface(
                color = VantaSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Edit Lyric Line",
                        color = VantaTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Timestamp: ${seg.formattedTimestamp} • Direction: ${if (seg.isRtl) "Arabic (RTL)" else "English (LTR)"}",
                        color = VantaTextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    CompositionLocalProvider(
                        LocalLayoutDirection provides (if (seg.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
                    ) {
                        OutlinedTextField(
                            value = editedText,
                            onValueChange = { editedText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_lyric_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VantaSecondary,
                                unfocusedBorderColor = VantaSurfaceVariant,
                                focusedTextColor = VantaTextPrimary,
                                unfocusedTextColor = VantaTextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { editingSegment = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text("Cancel", color = VantaTextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onUpdateSegmentText(seg, editedText)
                                editingSegment = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VantaPrimary)
                        ) {
                            Text("Save", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Add Segment Dialog
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                color = VantaSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Add Lyric Line",
                        color = VantaTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Timestamp: %02d:%02d".format((newSegmentTimeMs / 1000) / 60, (newSegmentTimeMs / 1000) % 60),
                        color = VantaSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newSegmentText,
                        onValueChange = { newSegmentText = it },
                        placeholder = { Text("Enter lyric phrase...", color = VantaTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VantaSecondary,
                            unfocusedBorderColor = VantaSurfaceVariant,
                            focusedTextColor = VantaTextPrimary,
                            unfocusedTextColor = VantaTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showAddDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text("Cancel", color = VantaTextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newSegmentText.isNotBlank()) {
                                    onAddSegment(newSegmentTimeMs, newSegmentText)
                                    newSegmentText = ""
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VantaPrimary)
                        ) {
                            Text("Insert", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentTimelineCard(
    segment: LyricsSegment,
    isActive: Boolean,
    currentPositionMs: Long,
    onTapToSeek: () -> Unit,
    onNudge: (Long) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (isActive) VantaSecondary else Color.Transparent
    val backgroundColor = if (isActive) VantaSurfaceVariant else VantaSurfaceElevated

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onTapToSeek() }
            .testTag("lyric_segment_${segment.lineIndex}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Line Header: Timestamp, RTL/LTR badge, Confidence, Nudge buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = segment.formattedTimestamp,
                        color = if (isActive) VantaSecondary else VantaTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (segment.isRtl) VantaTertiary.copy(alpha = 0.2f) else VantaPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (segment.isRtl) "AR • RTL" else "EN • LTR",
                            color = if (segment.isRtl) VantaTertiary else VantaPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${(segment.confidence * 100).toInt()}% conf",
                        color = VantaTextMuted,
                        fontSize = 10.sp
                    )
                }

                // Actions: Nudge -200ms, Nudge +200ms, Edit, Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onNudge(-250L) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "Nudge Earlier", tint = VantaTextMuted, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { onNudge(250L) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "Nudge Later", tint = VantaTextMuted, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Text", tint = VantaTextMuted, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Line", tint = VantaTextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Text display with proper text direction
            CompositionLocalProvider(
                LocalLayoutDirection provides (if (segment.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
            ) {
                Text(
                    text = segment.text,
                    color = if (isActive) Color.White else VantaTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    textAlign = if (segment.isRtl) TextAlign.Right else TextAlign.Left,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Word-level chips timeline
            if (segment.words.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    segment.words.forEach { word ->
                        val isWordActive = currentPositionMs in word.startTimeMs..word.endTimeMs
                        val wordBg = if (isWordActive) VantaSecondary else VantaSurfaceElevated
                        val wordColor = if (isWordActive) Color.Black else VantaTextSecondary

                        Surface(
                            color = wordBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = word.word,
                                color = wordColor,
                                fontSize = 11.sp,
                                fontWeight = if (isWordActive) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
