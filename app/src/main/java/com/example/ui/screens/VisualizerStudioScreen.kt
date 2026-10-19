package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AudioTrack
import com.example.data.VisualizerMode
import com.example.data.VisualizerPreset
import com.example.ui.components.backdropBlur
import com.example.ui.theme.VantaBackground
import com.example.ui.theme.VantaPrimary
import com.example.ui.theme.VantaSecondary
import com.example.ui.theme.VantaSurfaceElevated
import com.example.ui.theme.VantaSurfaceVariant
import com.example.ui.theme.VantaTertiary
import com.example.ui.theme.VantaTextMuted
import com.example.ui.theme.VantaTextPrimary
import com.example.ui.theme.VantaTextSecondary
import com.example.ui.theme.VantaGlassSurface
import com.example.ui.theme.VantaCardBorder

import com.example.visualizer.ParticleSimulation
import com.example.visualizer.VisualizerDrawers
import com.example.visualizer.VisualizerRenderConfig

@Composable
fun VisualizerStudioScreen(
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    fftData: FloatArray,
    waveformPcm: FloatArray,
    renderConfig: VisualizerRenderConfig,
    onConfigChange: (VisualizerRenderConfig) -> Unit,
    onPlayPause: () -> Unit,
    onOpenExport: () -> Unit,
    onSaveProject: () -> Unit,
    onOpenLyricsEditor: () -> Unit = {},
    onOpenModelManager: () -> Unit = {},
    onGenerateLyrics: (song: String, artist: String, lang: String) -> Unit = { _, _, _ -> },
    isSectionLoopEnabled: Boolean = false,
    loopStartMs: Long = 0L,
    loopEndMs: Long = 15000L,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    onToggleSectionLoop: (Boolean) -> Unit = {},
    onSetLoopRange: (startMs: Long, endMs: Long) -> Unit = { _, _ -> },
    onSetLoopStartCurrent: () -> Unit = {},
    onSetLoopEndCurrent: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isControlsExpanded by remember { mutableStateOf(false) }
    var frameTimeSeconds by remember { mutableFloatStateOf(0f) }
    var showLyricGenDialog by remember { mutableStateOf(false) }

    val particleSim = remember { ParticleSimulation(renderConfig.particleCount) }

    // Modern Android Photo Picker for zero-permission custom background image selection
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            onConfigChange(renderConfig.copy(customImageUri = it.toString()))
        }
    }

    // Animation frame tick for continuous fluid visualizer rendering
    LaunchedEffect(Unit) {
        var lastTime = withFrameMillis { it }
        while (true) {
            val currentTime = withFrameMillis { it }
            val dt = (currentTime - lastTime) / 1000f
            lastTime = currentTime
            frameTimeSeconds += dt
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(renderConfig.backgroundColor)
            .testTag("visualizer_studio_screen")
    ) {
    // Background Asset Layer (Image/Video/GIF)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (renderConfig.backgroundConfig.type) {
            com.example.visualizer.BackgroundType.IMAGE, 
            com.example.visualizer.BackgroundType.ANIMATED_IMAGE -> {
                if (renderConfig.backgroundConfig.assetPath != null) {
                    androidx.compose.foundation.Image(
                        bitmap = // This would be the loaded bitmap
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        alpha = renderConfig.backgroundConfig.opacity
                    )
                }
            }
            com.example.visualizer.BackgroundType.VIDEO -> {
                if (renderConfig.backgroundConfig.assetPath != null) {
                    // Video rendering would typically happen via AndroidView(factory = { ExoPlayer... })
                    // For this high-level implemention, we'll use a placeholder or the designated assetPath
                }
            }
            com.example.visualizer.BackgroundType.COLOR -> {
                Box(modifier = Modifier.fillMaxSize().background(renderConfig.backgroundColor))
            }
        }
    }

    // Main Visualizer Canvas
    Canvas(

            modifier = Modifier
                .fillMaxSize()
                .testTag("visualizer_canvas")
        ) {
            val particles = particleSim.update(
                size.width, size.height,
                fftData, renderConfig.sensitivity, 1f / 60f
            )

            VisualizerDrawers.drawVisualizer(
                scope = this,
                config = renderConfig,
                fftData = fftData,
                waveformData = waveformPcm,
                particles = particles,
                timeSeconds = frameTimeSeconds
            )
        }

        // Overlay Song Title & Artist if enabled
        if (renderConfig.showTextOverlay && currentTrack != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = if (isControlsExpanded) 280.dp else 120.dp)
            ) {
                Text(
                    text = currentTrack.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = currentTrack.artist,
                    fontSize = 14.sp,
                    color = VantaTextSecondary
                )
            }
        }

        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mode Indicator Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VantaGlassSurface,
                    modifier = Modifier
                        .backdropBlur(15f)
                        .border(1.dp, VantaPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = VantaSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = renderConfig.mode.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VantaTextPrimary
                        )
                    }
                }

                // Loop Active Glowing Badge
                if (isSectionLoopEnabled) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = VantaPrimary.copy(alpha = 0.25f),
                        modifier = Modifier
                            .backdropBlur(15f)
                            .border(1.dp, VantaSecondary, RoundedCornerShape(12.dp))
                            .clickable { onToggleSectionLoop(false) }
                            .testTag("studio_loop_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AllInclusive,
                                contentDescription = "Looping Active",
                                tint = VantaSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${formatStudioTime(loopStartMs)} - ${formatStudioTime(loopEndMs)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = VantaSecondary
                            )
                        }
                    }
                }
            }

            // Top Action Buttons: Loop Toggle, Play/Pause, Save Project, Export Video
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Loop Visualization Quick Toggle button
                IconButton(
                    onClick = { onToggleSectionLoop(!isSectionLoopEnabled) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSectionLoopEnabled) VantaPrimary.copy(alpha = 0.85f)
                            else VantaSurfaceElevated.copy(alpha = 0.85f)
                        )
                        .border(
                            1.dp,
                            if (isSectionLoopEnabled) VantaSecondary else Color.Transparent,
                            CircleShape
                        )
                        .testTag("studio_loop_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AllInclusive,
                        contentDescription = if (isSectionLoopEnabled) "Disable Loop Visualization" else "Enable Loop Visualization",
                        tint = if (isSectionLoopEnabled) VantaSecondary else VantaTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Play/Pause button
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(VantaSurfaceElevated.copy(alpha = 0.85f))
                        .testTag("studio_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = VantaSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Save Project button
                IconButton(
                    onClick = onSaveProject,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(VantaSurfaceElevated.copy(alpha = 0.85f))
                        .testTag("studio_save_project_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Save Project",
                        tint = VantaTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // AI Lyrics Studio button
                IconButton(
                    onClick = onOpenLyricsEditor,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(VantaSurfaceElevated.copy(alpha = 0.85f))
                        .testTag("studio_lyrics_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Lyrics Studio",
                        tint = VantaSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Prominent Export Video Button
                Button(
                    onClick = onOpenExport,
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("studio_export_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VantaPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Export",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }

        // Bottom Customization Control Panel
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .testTag("studio_control_panel"),
            color = VantaSurfaceElevated.copy(alpha = 0.95f),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Toggle expand header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isControlsExpanded = !isControlsExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = VantaSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isControlsExpanded) "Customize Studio" else "Tap to Customize Visuals",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = VantaTextPrimary
                        )
                    }

                    Icon(
                        imageVector = if (isControlsExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = VantaTextSecondary
                    )
                }

                // Layers Manager (Replacing Mode Selection Chips)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active Layers", fontSize = 12.sp, color = VantaTextSecondary, modifier = Modifier.padding(end = 8.dp))
                    
                    // Simple Layer Add Button
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp).background(VantaSurfaceVariant, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Layer", tint = VantaPrimary, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("Spectrum Bars") }, onClick = { 
                                onConfigChange(renderConfig.copy(layers = renderConfig.layers + VisualEffect.SpectrumBars()))
                                showMenu = false 
                            })
                            DropdownMenuItem(text = { Text("Neon Wave") }, onClick = { 
                                onConfigChange(renderConfig.copy(layers = renderConfig.layers + VisualEffect.NeonWave()))
                                showMenu = false 
                            })
                            DropdownMenuItem(text = { Text("Particle Burst") }, onClick = { 
                                onConfigChange(renderConfig.copy(layers = renderConfig.layers + VisualEffect.ParticleBurst()))
                                showMenu = false 
                            })
                        }
                    }
                }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    items(renderConfig.layers.size) { index ->
                        val layer = renderConfig.layers[index]
                        FilterChip(
                            selected = true,
                            onClick = { /* TODO: Select layer for editing */ },
                            label = { Text(layer.name, fontSize = 12.sp) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val newLayers = renderConfig.layers.toMutableList().apply { removeAt(index) }
                                    onConfigChange(renderConfig.copy(layers = newLayers))
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VantaPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Expanded Customization Controls
                AnimatedVisibility(visible = isControlsExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 12.dp)
                    ) {
                        // Loop Visualization Card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    if (isSectionLoopEnabled) VantaPrimary else VantaCardBorder,
                                    RoundedCornerShape(16.dp)
                                ),
                            color = if (isSectionLoopEnabled) VantaGlassSurface else VantaSurfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AllInclusive,
                                            contentDescription = null,
                                            tint = if (isSectionLoopEnabled) VantaSecondary else VantaTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Loop Visualization",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VantaTextPrimary
                                            )
                                            Text(
                                                text = if (isSectionLoopEnabled) "Looping [${formatStudioTime(loopStartMs)} - ${formatStudioTime(loopEndMs)}]"
                                                else "Loop specific section of audio-reactive animation",
                                                fontSize = 11.sp,
                                                color = if (isSectionLoopEnabled) VantaSecondary else VantaTextMuted
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = isSectionLoopEnabled,
                                        onCheckedChange = onToggleSectionLoop,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = VantaSecondary,
                                            checkedTrackColor = VantaPrimary
                                        ),
                                        modifier = Modifier.testTag("studio_loop_switch")
                                    )
                                }

                                if (isSectionLoopEnabled) {
                                    Spacer(modifier = Modifier.height(10.dp))

                                    val maxDuration = if (durationMs > 0) durationMs.toFloat() else 60000f
                                    val safeStart = loopStartMs.toFloat().coerceIn(0f, maxDuration)
                                    val safeEnd = loopEndMs.toFloat().coerceIn(safeStart + 1000f, maxDuration)

                                    // Time Indicator Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "A: ${formatStudioTime(loopStartMs)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = VantaSecondary
                                        )
                                        Text(
                                            text = "Length: ${"%.1f".format((loopEndMs - loopStartMs) / 1000f)}s",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = VantaTextPrimary
                                        )
                                        Text(
                                            text = "B: ${formatStudioTime(loopEndMs)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = VantaTertiary
                                        )
                                    }

                                    // Range Slider
                                    RangeSlider(
                                        value = safeStart..safeEnd,
                                        onValueChange = { range ->
                                            onSetLoopRange(range.start.toLong(), range.endInclusive.toLong())
                                        },
                                        valueRange = 0f..maxDuration,
                                        colors = SliderDefaults.colors(
                                            thumbColor = VantaSecondary,
                                            activeTrackColor = VantaPrimary,
                                            inactiveTrackColor = VantaSurfaceVariant
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    // Quick Duration Presets
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            5000L to "5s Hook",
                                            10000L to "10s Drop",
                                            15000L to "15s Reel",
                                            30000L to "30s Story"
                                        ).forEach { (lengthMs, label) ->
                                            val isCurrentLen = (loopEndMs - loopStartMs) in (lengthMs - 500L)..(lengthMs + 500L)
                                            FilterChip(
                                                selected = isCurrentLen,
                                                onClick = {
                                                    val newEnd = (loopStartMs + lengthMs).coerceAtMost(maxDuration.toLong())
                                                    onSetLoopRange(loopStartMs, newEnd)
                                                },
                                                label = { Text(label, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = VantaPrimary.copy(alpha = 0.35f),
                                                    selectedLabelColor = VantaSecondary,
                                                    containerColor = VantaSurfaceVariant,
                                                    labelColor = VantaTextMuted
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Set In / Set Out Marker Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = onSetLoopStartCurrent,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = VantaSurfaceVariant)
                                        ) {
                                            Text("Set In [A]", fontSize = 11.sp, color = VantaSecondary)
                                        }

                                        Button(
                                            onClick = onSetLoopEndCurrent,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = VantaSurfaceVariant)
                                        ) {
                                            Text("Set Out [B]", fontSize = 11.sp, color = VantaTertiary)
                                        }

                                        Button(
                                            onClick = { onSeek(loopStartMs) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = VantaPrimary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Replay,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Restart", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Presets Row
                        Text("Presets", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VantaTextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(VisualizerPreset.ALL_PRESETS) { p ->
                                val prim = Color(android.graphics.Color.parseColor(p.primaryColorHex))
                                val sec = Color(android.graphics.Color.parseColor(p.secondaryColorHex))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(VantaSurfaceVariant)
                                        .border(1.dp, prim, RoundedCornerShape(8.dp))
                                        .clickable {
                                            onConfigChange(
                                                renderConfig.copy(
                                                    mode = p.mode,
                                                    primaryColor = prim,
                                                    secondaryColor = sec,
                                                    backgroundColor = Color(android.graphics.Color.parseColor(p.backgroundColorHex)),
                                                    glowIntensity = p.glowIntensity,
                                                    sensitivity = p.sensitivity,
                                                    barCount = p.barCount,
                                                    particleCount = p.particleCount
                                                )
                                            )
                                            particleSim.setCount(p.particleCount)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(p.name, fontSize = 11.sp, color = VantaTextPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Sensitivity Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Audio Sensitivity", fontSize = 13.sp, color = VantaTextSecondary)
                            Text("%.1fx".format(renderConfig.sensitivity), fontSize = 13.sp, color = VantaSecondary)
                        }
                        Slider(
                            value = renderConfig.sensitivity,
                            onValueChange = { onConfigChange(renderConfig.copy(sensitivity = it)) },
                            valueRange = 0.4f..2.2f,
                            colors = SliderDefaults.colors(thumbColor = VantaSecondary, activeTrackColor = VantaPrimary)
                        )

                        // Glow Intensity Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Glow Intensity", fontSize = 13.sp, color = VantaTextSecondary)
                            Text("${(renderConfig.glowIntensity * 100).toInt()}%", fontSize = 13.sp, color = VantaSecondary)
                        }
                        Slider(
                            value = renderConfig.glowIntensity,
                            onValueChange = { onConfigChange(renderConfig.copy(glowIntensity = it)) },
                            valueRange = 0.2f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = VantaSecondary, activeTrackColor = VantaPrimary)
                        )

                        // Particle / Bar Count Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (renderConfig.mode == VisualizerMode.PARTICLE_BURST) "Particle Count" else "Bar Count",
                                fontSize = 13.sp,
                                color = VantaTextSecondary
                            )
                            Text(
                                if (renderConfig.mode == VisualizerMode.PARTICLE_BURST) "${renderConfig.particleCount}" else "${renderConfig.barCount}",
                                fontSize = 13.sp,
                                color = VantaSecondary
                            )
                        }
                        if (renderConfig.mode == VisualizerMode.PARTICLE_BURST) {
                            Slider(
                                value = renderConfig.particleCount.toFloat(),
                                onValueChange = {
                                    val count = it.toInt()
                                    onConfigChange(renderConfig.copy(particleCount = count))
                                    particleSim.setCount(count)
                                },
                                valueRange = 40f..240f,
                                colors = SliderDefaults.colors(thumbColor = VantaSecondary, activeTrackColor = VantaPrimary)
                            )
                        } else {
                            Slider(
                                value = renderConfig.barCount.toFloat(),
                                onValueChange = { onConfigChange(renderConfig.copy(barCount = it.toInt())) },
                                valueRange = 16f..96f,
                                colors = SliderDefaults.colors(thumbColor = VantaSecondary, activeTrackColor = VantaPrimary)
                            )
                        }

                        // Colors Palette selection
                        Text("Color Scheme", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VantaTextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        val palettes = listOf(
                            Pair(Color(0xFF8B5CF6), Color(0xFF06B6D4)), // Purple + Cyan
                            Pair(Color(0xFFEC4899), Color(0xFF8B5CF6)), // Pink + Purple
                            Pair(Color(0xFF10B981), Color(0xFF06B6D4)), // Green + Cyan
                            Pair(Color(0xFFF59E0B), Color(0xFFEF4444)), // Amber + Red
                            Pair(Color(0xFF38BDF8), Color(0xFF6366F1)), // Sky + Indigo
                            Pair(Color(0xFFFFFFFF), Color(0xFFA855F7))  // White + Violet
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            palettes.forEach { (c1, c2) ->
                                val isSelected = renderConfig.primaryColor == c1
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(c1)
                                        .border(2.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
                                        .clickable {
                                            onConfigChange(renderConfig.copy(primaryColor = c1, secondaryColor = c2))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Text Overlay Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show Song Title Overlay", fontSize = 13.sp, color = VantaTextPrimary)
                            Switch(
                                checked = renderConfig.showTextOverlay,
                                onCheckedChange = { onConfigChange(renderConfig.copy(showTextOverlay = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = VantaSecondary, checkedTrackColor = VantaPrimary)
                            )
                        }

                        // --- قسم الإبداع: تأثيرات السينما ---
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Cinematic Effects", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VantaTextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dynamic Camera Shake", fontSize = 12.sp, color = VantaTextSecondary)
                            Switch(
                                checked = renderConfig.cameraShakeEnabled,
                                onCheckedChange = { onConfigChange(renderConfig.copy(cameraShakeEnabled = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = VantaSecondary, checkedTrackColor = VantaPrimary)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Glitch Intensity", fontSize = 12.sp, color = VantaTextSecondary)
                            Text("%.1fx".format(renderConfig.glitchIntensity), fontSize = 12.sp, color = VantaSecondary)
                        }
                        Slider(
                            value = renderConfig.glitchIntensity,
                            onValueChange = { onConfigChange(renderConfig.copy(glitchIntensity = it)) },
                            valueRange = 0f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = VantaSecondary, activeTrackColor = VantaPrimary)
                        )


                        // AI Synchronized Lyrics Toggle
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VantaSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI Synchronized Lyrics", fontSize = 13.sp, color = VantaTextPrimary)
                            }
                            Switch(
                                checked = renderConfig.showLyrics,
                                onCheckedChange = { onConfigChange(renderConfig.copy(showLyrics = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = VantaSecondary, checkedTrackColor = VantaPrimary)
                            )
                        }

                        if (renderConfig.showLyrics) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Karaoke Animation Style", fontSize = 11.sp, color = VantaTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(com.example.transcription.KaraokeStyle.values()) { style ->
                                    val isSelected = style == renderConfig.karaokeStyle
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onConfigChange(renderConfig.copy(karaokeStyle = style)) },
                                        label = { Text(style.displayName, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = VantaPrimary.copy(alpha = 0.3f),
                                            selectedLabelColor = VantaSecondary,
                                            containerColor = VantaSurfaceVariant,
                                            labelColor = VantaTextMuted
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onOpenLyricsEditor,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VantaSurfaceVariant)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VantaSecondary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Lyrics Studio", fontSize = 11.sp, color = VantaSecondary)
                                }
                                Button(
                                    onClick = { showLyricGenDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VantaPrimary)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Generate AI", fontSize = 11.sp, color = Color.White)
                                }
                                Button(
                                    onClick = onOpenModelManager,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VantaSurfaceVariant)
                                ) {
                                    Text("AI Engine", fontSize = 11.sp, color = VantaTextPrimary)
                                }
                            }
                        }

                        // Background Manager Card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, VantaCardBorder, RoundedCornerShape(16.dp)),
                            color = VantaSurfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (renderConfig.backgroundConfig.type == com.example.visualizer.BackgroundType.VIDEO) 
                                                Icons.Default.Videocam else Icons.Default.Image, 
                                            contentDescription = null, 
                                            tint = VantaSecondary, 
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Background Reactor", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VantaTextPrimary)
                                    }
                                    
                                    var showTypeMenu by remember { mutableStateOf(false) }
                                    Box {
                                        IconButton(
                                            onClick = { showTypeMenu = true },
                                            modifier = Modifier.size(32.dp).background(VantaSurfaceElevated, RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(Icons.Default.Tune, contentDescription = "Change Type", tint = VantaPrimary, modifier = Modifier.size(18.dp))
                                        }
                                        DropdownMenu(expanded = showTypeMenu, onDismissRequest = { showTypeMenu = false }) {
                                            com.example.visualizer.BackgroundType.values().forEach { type ->
                                                androidx.compose.material3.DropdownMenuItem(
                                                    text = { Text(type.displayName, fontSize = 12.sp) },
                                                    onClick = {
                                                        onConfigChange(renderConfig.copy(
                                                            backgroundConfig = renderConfig.backgroundConfig.copy(type = type)
                                                        ))
                                                        showTypeMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Asset Selection Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { 
                                            if (renderConfig.backgroundConfig.type == com.example.visualizer.BackgroundType.IMAGE) {
                                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                            } else if (renderConfig.backgroundConfig.type == com.example.visualizer.BackgroundType.VIDEO) {
                                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                            } else {
                                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = VantaSurfaceVariant)
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null, tint = VantaSecondary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pick Asset", fontSize = 11.sp, color = VantaSecondary)
                                    }
                                    
                                    if (renderConfig.backgroundConfig.assetPath != null) {
                                        Text("Loaded", fontSize = 11.sp, color = VantaSecondary, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Reaction Settings
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Reaction Type", fontSize = 12.sp, color = VantaTextSecondary)
                                    
                                    var showReactionMenu by remember { mutableStateOf(false) }
                                    Box {
                                        Text(
                                            text = renderConfig.backgroundConfig.reactionType.displayName,
                                            fontSize = 12.sp,
                                            color = VantaSecondary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { showReactionMenu = true }
                                        )
                                        DropdownMenu(expanded = showReactionMenu, onDismissRequest = { showReactionMenu = false }) {
                                            com.example.visualizer.ReactionType.values().forEach { rType ->
                                                androidx.compose.material3.DropdownMenuItem(
                                                    text = { Text(rType.displayName, fontSize = 12.sp) },
                                                    onClick = {
                                                        onConfigChange(renderConfig.copy(
                                                            backgroundConfig = renderConfig.backgroundConfig.copy(reactionType = rType)
                                                        ))
                                                        showReactionMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                if (renderConfig.backgroundConfig.reactionType != com.example.visualizer.ReactionType.NONE) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Strength", fontSize = 12.sp, color = VantaTextSecondary)
                                        Text("%.2f".format(renderConfig.backgroundConfig.reactionStrength), fontSize = 12.sp, color = VantaSecondary)
                                    }
                                    Slider(
                                        value = renderConfig.backgroundConfig.reactionStrength,
                                        onValueChange = { 
                                            onConfigChange(renderConfig.copy(
                                                backgroundConfig = renderConfig.backgroundConfig.copy(reactionStrength = it)
                                            )) 
                                        },
                                        valueRange = 0f..2f,
                                        colors = SliderDefaults.colors(thumbColor = VantaSecondary, activeTrackColor = VantaPrimary)
                                    )
                                }
                            }
                        }


                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        // Lyric Generation Dialog
        if (showLyricGenDialog) {
            var songName by remember { mutableStateOf(currentTrack?.title ?: "") }
            var artistName by remember { mutableStateOf(currentTrack?.artist ?: "") }
            var selectedLang by remember { mutableStateOf("English") }

            AlertDialog(
                onDismissRequest = { showLyricGenDialog = false },
                title = { Text(\"Generate AI Lyrics\", color = VantaTextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = songName,
                            onValueChange = { songName = it },
                            label = { Text(\"Song Name\") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = VantaPrimary,
                                unfocusedBorderColor = VantaTextMuted,
                                textColor = VantaTextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = artistName,
                            onValueChange = { artistName = it },
                            label = { Text(\"Artist\") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = VantaPrimary,
                                unfocusedBorderColor = VantaTextMuted,
                                textColor = VantaTextPrimary
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilterChip(
                                selected = selectedLang == \"English\",
                                onClick = { selectedLang = \"English\" },
                                label = { Text(\"English\") }
                            )
                            FilterChip(
                                selected = selectedLang == \"Arabic\",
                                onClick = { selectedLang = \"Arabic\" },
                                label = { Text(\"العربية\") }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onGenerateLyrics(songName, artistName, selectedLang)
                            showLyricGenDialog = false
                        }
                    ) {
                        Text(\"Generate\", color = VantaSecondary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLyricGenDialog = false }) {
                        Text(\"Cancel\", color = VantaTextSecondary)
                    }
                },
                containerColor = VantaSurfaceElevated,
                titleColor = VantaTextPrimary
            )
        }
    }
}

private fun formatStudioTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = ((ms % 1000) / 100).toInt()
    return "%02d:%02d.%d".format(minutes, seconds, tenths)
}

