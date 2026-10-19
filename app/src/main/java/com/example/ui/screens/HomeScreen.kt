package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items as itemsGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AudioTrack
import com.example.data.ExportRecordEntity
import com.example.data.VisualizerPreset
import com.example.ui.components.backdropBlur
import com.example.ui.theme.VantaCardBorder
import com.example.ui.theme.VantaPrimary
import com.example.ui.theme.VantaSecondary
import com.example.ui.theme.VantaSurface
import com.example.ui.theme.VantaSurfaceElevated
import com.example.ui.theme.VantaSurfaceVariant
import com.example.ui.theme.VantaTertiary
import com.example.ui.theme.VantaTextMuted
import com.example.ui.theme.VantaTextPrimary
import com.example.ui.theme.VantaTextSecondary
import com.example.ui.theme.VantaGlassSurface


@Composable
fun HomeScreen(
    tracks: List<AudioTrack>,
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    recentProjects: List<com.example.data.VisualizerProjectEntity> = emptyList(),
    recentExports: List<ExportRecordEntity> = emptyList(),
    onTrackSelect: (AudioTrack) -> Unit,
    onPresetSelect: (VisualizerPreset) -> Unit,
    onProjectSelect: (com.example.data.VisualizerProjectEntity) -> Unit = {},
    onCreateVideoClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQueryState = remember { mutableStateOf("") }
    val searchQuery by searchQueryState

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // App Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(listOf(VantaPrimary, VantaSecondary))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "VantaWave AI",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = VantaTextPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Music • Visuals • AI Lyrics",
                            fontSize = 12.sp,
                            color = VantaSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = VantaTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // App Header (already exists earlier)

        // Search Bar (Full width)
        item {
            androidx.compose.material3.OutlinedTextField(
                value = searchQueryState.value,
                onValueChange = { searchQueryState.value = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Search visualizer modes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Hero "Create Music Video" Action Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, VantaCardBorder, RoundedCornerShape(20.dp))
                    .testTag("hero_create_video_card"),
                color = VantaSurfaceElevated
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(VantaPrimary.copy(alpha = 0.2f))
                                        .border(1.dp, VantaPrimary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "STUDIO PRO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VantaSecondary,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Offline AI Audio Workstation",
                                    fontSize = 12.sp,
                                    color = VantaTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Audio Visualizer &\nAI Lyric Video Studio",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = VantaTextPrimary,
                            lineHeight = 28.sp,
                            letterSpacing = (-0.3).sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Real-time FFT audio spectrum analysis, customizable particle dynamics, and on-device offline AI karaoke lyrics.",
                            fontSize = 13.sp,
                            color = VantaTextSecondary,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = onCreateVideoClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("home_create_video_cta"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VantaPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open Visualizer Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Recent Projects Snapshots
        if (recentProjects.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Projects",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = VantaTextPrimary
                    )
                    Text(
                        text = "${recentProjects.size} Saved",
                        fontSize = 12.sp,
                        color = VantaSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentProjects) { project ->
                        val prim = try {
                            Color(android.graphics.Color.parseColor(project.primaryColorHex))
                        } catch (e: Exception) {
                            VantaPrimary
                        }
                        Surface(
                            modifier = Modifier
                                .width(150.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, prim.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .clickable { onProjectSelect(project) }
                                .testTag("home_recent_project_${project.id}"),
                            color = VantaSurfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(prim.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = prim,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = project.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VantaTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = project.mode,
                                    fontSize = 11.sp,
                                    color = VantaTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Visualizer Presets Carousel
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Visualizer Presets",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VantaTextPrimary
                )
                Text(
                    text = "${VisualizerPreset.ALL_PRESETS.size} Modes",
                    fontSize = 12.sp,
                    color = VantaSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val filteredPresets = VisualizerPreset.ALL_PRESETS.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
            
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsGrid(filteredPresets) { preset ->
                    val primaryColor = Color(android.graphics.Color.parseColor(preset.primaryColorHex))
                    val secondaryColor = Color(android.graphics.Color.parseColor(preset.secondaryColorHex))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable { onPresetSelect(preset) }
                            .testTag("preset_card_${preset.name}"),
                        color = VantaSurfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(listOf(primaryColor, secondaryColor))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                            // Instead of Icon
                            AsyncImage(
                                model = when(preset.id) {
                                    "preset_cyberwave" -> R.drawable.preset_cyberwave
                                    "preset_neon_pulse" -> R.drawable.preset_neon_pulse
                                    "preset_dark_spectrum" -> R.drawable.preset_dark_spectrum
                                    "preset_aurora" -> R.drawable.preset_aurora
                                    "preset_minimal_wave" -> R.drawable.preset_minimal_wave
                                    "preset_radial_bass" -> R.drawable.preset_radial_bass
                                    else -> R.drawable.preset_cyberwave
                                },
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = preset.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = VantaTextPrimary,
                                maxLines = 1
                            )

                            Text(
                                text = preset.mode.displayName,
                                fontSize = 11.sp,
                                color = VantaTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Quick Tracks (Top 4 / Demo Tracks)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Play & Visualize",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VantaTextPrimary
                )
                Text(
                    text = "See All",
                    fontSize = 13.sp,
                    color = VantaSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onOpenLibrary() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (tracks.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    color = VantaSurfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = VantaSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Loading audio tracks...",
                            fontSize = 14.sp,
                            color = VantaTextPrimary
                        )
                    }
                }
            }
        } else {
            items(tracks.take(4)) { track ->
                val isCurrent = currentTrack?.id == track.id

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTrackSelect(track) }
                        .testTag("home_track_item_${track.id}"),
                    color = if (isCurrent) VantaSurfaceElevated else VantaSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrent) VantaPrimary else VantaSurfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCurrent && isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (isCurrent) Color.White else VantaSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                fontSize = 14.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) VantaSecondary else VantaTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${track.artist} • ${track.formattedDuration}",
                                fontSize = 12.sp,
                                color = VantaTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (track.isDemo) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(VantaTertiary.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DEMO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VantaTertiary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Video Exports section
        if (recentExports.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Exports",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = VantaTextPrimary
                    )
                    Text(
                        text = "${recentExports.size} Videos",
                        fontSize = 12.sp,
                        color = VantaSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentExports.take(5)) { export ->
                        Surface(
                            modifier = Modifier
                                .width(130.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            color = VantaSurfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(VantaSurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = VantaSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = export.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VantaTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${export.aspectRatio} • ${export.resolution}",
                                    fontSize = 10.sp,
                                    color = VantaTextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom spacing for mini player
        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}
