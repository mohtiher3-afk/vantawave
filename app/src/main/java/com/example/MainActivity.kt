package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.performance.PerformanceReporter
import com.example.ui.MainViewModel
import com.example.ui.components.ExportVideoDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MusicLibraryScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.VisualizerStudioScreen
import com.example.ui.theme.VantaBackground
import com.example.ui.theme.VantaPrimary
import com.example.ui.theme.VantaSecondary
import com.example.ui.theme.VantaSurfaceElevated
import com.example.ui.theme.VantaSurfaceVariant
import com.example.ui.theme.VantaTextMuted
import com.example.ui.theme.VantaTextPrimary
import com.example.ui.theme.VantaWaveTheme
import com.example.ui.components.VantaBottomNavigationBar
import com.example.ui.components.NavItem
import com.example.ui.components.AmbientParticleBackground

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var performanceReporter: PerformanceReporter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        performanceReporter = PerformanceReporter(window)
        performanceReporter.setTrackingEnabled(BuildConfig.DEBUG)
        setContent {
            val activeAccent by viewModel.activeAccent.collectAsState()
            VantaWaveTheme(accent = activeAccent) {
                MainContent(viewModel = viewModel, performanceReporter = performanceReporter)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: MainViewModel, performanceReporter: PerformanceReporter) {
    var selectedScreen by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedScreen) {
        val screenName = when (selectedScreen) {
            0 -> "Home"
            1 -> "Library"
            2 -> "Studio"
            3 -> "Projects"
            4 -> "Uploader"
            else -> "Unknown"
        }
    }

    var showNowPlayingSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val tracks by viewModel.tracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()
    val isSectionLoopEnabled by viewModel.isSectionLoopEnabled.collectAsState()
    val loopStartMs by viewModel.loopStartMs.collectAsState()
    val loopEndMs by viewModel.loopEndMs.collectAsState()
    val fftData by viewModel.fftData.collectAsState()
    val waveformPcm by viewModel.waveformPcm.collectAsState()
    val renderConfig by viewModel.renderConfig.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val recentProjects by viewModel.recentProjects.collectAsState()
    val exports by viewModel.exports.collectAsState()
    val recentExports by viewModel.recentExports.collectAsState()
    val globalExportSettings by viewModel.globalExportSettings.collectAsState(initial = null)
    val isExportDialogOpen by viewModel.isExportDialogOpen.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val currentLyricsSegments by viewModel.currentLyricsSegments.collectAsState()
    val isLyricsEditorOpen by viewModel.isLyricsEditorOpen.collectAsState()
    val isModelManagerOpen by viewModel.isModelManagerOpen.collectAsState()
    val transcriptionProgress by viewModel.transcriptionProgress.collectAsState()
    val modelInfo by viewModel.modelInfo.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshTracks()
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = selectedScreen == 0,
                    onClick = {
                        selectedScreen = 0
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Saved Recordings") },
                    selected = selectedScreen == 3,
                    onClick = {
                        selectedScreen = 3
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        showSettingsDialog = true
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = VantaBackground,
        floatingActionButton = {
            if (selectedScreen == 0) {
                FloatingActionButton(
                    onClick = { viewModel.startRecording() },
                    containerColor = VantaPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = "Start Recording")
                }
            }
        },
        bottomBar = {
            Column {
                if (currentTrack != null) {
                    val progress = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
                    MiniPlayer(
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        progress = progress,
                        onPlayPauseClick = { viewModel.playOrPause() },
                        onNextClick = { viewModel.next() },
                        onOpenStudioClick = { selectedScreen = 2 },
                        onExpandPlayerClick = { showNowPlayingSheet = true }
                    )
                }
                VantaBottomNavigationBar(
                    items = listOf(
                        NavItem("Home", Icons.Filled.Home, Icons.Outlined.Home),
                        NavItem("Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
                        NavItem("Studio", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
                        NavItem("Projects", Icons.Filled.Folder, Icons.Outlined.Folder)
                    ),
                    selectedIndex = selectedScreen,
                    onItemSelected = { selectedScreen = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val activeAccent by viewModel.activeAccent.collectAsState()
            if (selectedScreen != 2) {
                AmbientParticleBackground(
                    modifier = Modifier.fillMaxSize(),
                    accent = activeAccent
                )
            }

            when (selectedScreen) {
                0 -> HomeScreen(
                    tracks = tracks,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    recentProjects = recentProjects,
                    recentExports = recentExports,
                    onTrackSelect = { track -> viewModel.playTrack(track, tracks) },
                    onPresetSelect = { preset ->
                        viewModel.applyPreset(preset)
                        selectedScreen = 2
                    },
                    onProjectSelect = { project ->
                        viewModel.restoreProject(project)
                        selectedScreen = 2
                    },
                    onCreateVideoClick = {
                        selectedScreen = 2
                        viewModel.openExportDialog()
                    },
                    onOpenSettings = { showSettingsDialog = true },
                    onOpenLibrary = { selectedScreen = 1 }
                )
                1 -> MusicLibraryScreen(
                    tracks = tracks,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    playlists = playlists,
                    onTrackSelect = { track, queue -> viewModel.playTrack(track, queue) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onCreatePlaylist = { viewModel.createPlaylist(it) },
                    onDeletePlaylist = { viewModel.deletePlaylist(it) },
                    onAddTrackToPlaylist = { plId, trId -> viewModel.addTrackToPlaylist(plId, trId) },
                    onRefreshScan = { viewModel.refreshTracks() },
                    onOpenStudio = { track ->
                        viewModel.playTrack(track)
                        selectedScreen = 2
                    }
                )
                2 -> VisualizerStudioScreen(
                    currentTrack = currentTrack ?: tracks.firstOrNull(),
                    isPlaying = isPlaying,
                    fftData = fftData,
                    waveformPcm = waveformPcm,
                    renderConfig = renderConfig,
                    onConfigChange = { viewModel.updateRenderConfig(it) },
                    onPlayPause = { viewModel.playOrPause() },
                    onOpenExport = { viewModel.openExportDialog() },
                    onSaveProject = { viewModel.saveCurrentProject() },
                    onOpenLyricsEditor = { viewModel.openLyricsEditor() },
                    onOpenModelManager = { viewModel.openModelManager() },
                    isSectionLoopEnabled = isSectionLoopEnabled,
                    loopStartMs = loopStartMs,
                    loopEndMs = loopEndMs,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    onToggleSectionLoop = { viewModel.toggleSectionLoop(it) },
                    onSetLoopRange = { start, end -> viewModel.setLoopRange(start, end) },
                    onSetLoopStartCurrent = { viewModel.setLoopStartToCurrent() },
                    onSetLoopEndCurrent = { viewModel.setLoopEndToCurrent() },
                    onSeek = { viewModel.seekTo(it) }
                )
                3 -> ProjectsScreen(
                    projects = projects,
                    exports = exports,
                    onOpenProject = { project ->
                        viewModel.restoreProject(project)
                        selectedScreen = 2
                    },
                    onCloneProject = { viewModel.cloneProject(it) },
                    onDeleteProject = { viewModel.deleteProject(it) },
                    onDeleteExport = { viewModel.deleteExport(it) }
                )
            }
        }
    }
}

    if (showNowPlayingSheet && currentTrack != null) {
        NowPlayingSheet(
            track = currentTrack,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeatMode,
            isFavorite = currentTrack?.isFavorite ?: false,
            sleepTimerMinutes = sleepTimerMinutes,
            onPlayPause = { viewModel.playOrPause() },
            onNext = { viewModel.next() },
            onPrevious = { viewModel.previous() },
            onSeek = { viewModel.seekTo(it) },
            onToggleShuffle = { viewModel.toggleShuffle() },
            onCycleRepeat = { viewModel.cycleRepeatMode() },
            onToggleFavorite = { currentTrack?.id?.let { viewModel.toggleFavorite(it) } },
            onOpenSleepTimer = { showSettingsDialog = true },
            onOpenStudio = {
                showNowPlayingSheet = false
                selectedScreen = 2
            },
            onDismiss = { showNowPlayingSheet = false }
        )
    }

    if (isExportDialogOpen) {
        val targetTrack = currentTrack ?: tracks.firstOrNull()
        if (targetTrack != null) {
            ExportVideoDialog(
                track = targetTrack,
                renderConfig = renderConfig,
                exportProgress = exportProgress,
                savedExportSettings = globalExportSettings,
                onStartExport = { viewModel.startExport(it) },
                onCancelExport = { viewModel.cancelExport() },
                onDismiss = { viewModel.closeExportDialog() }
            )
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            sleepTimerMinutes = sleepTimerMinutes,
            onSetSleepTimer = { viewModel.setSleepTimer(it) },
            activeAccent = viewModel.activeAccent.collectAsState().value,
            onUpdateAccent = { viewModel.updateAccent(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (isLyricsEditorOpen) {
        val targetTrack = currentTrack ?: tracks.firstOrNull()
        if (targetTrack != null) {
            com.example.editor.LyricsTimelineEditor(
                track = targetTrack,
                segments = currentLyricsSegments,
                currentPositionMs = currentPositionMs,
                isPlaying = isPlaying,
                transcriptionProgress = transcriptionProgress,
                selectedKaraokeStyle = renderConfig.karaokeStyle,
                onKaraokeStyleChange = { viewModel.setKaraokeStyle(it) },
                onSeekTo = { viewModel.seekTo(it) },
                onPlayPause = { viewModel.playOrPause() },
                onTranscribeTrack = { viewModel.transcribeCurrentTrack(targetTrack) },
                onNudgeSegment = { seg, delta -> viewModel.nudgeSegment(seg, delta) },
                onUpdateSegmentText = { seg, text -> viewModel.updateSegmentText(seg, text) },
                onDeleteSegment = { seg -> viewModel.deleteSegment(seg) },
                onAddSegment = { timeMs, text -> viewModel.addSegment(timeMs, text) },
                onSaveToDatabase = { viewModel.saveLyricsToDatabase() },
                onDismiss = { viewModel.closeLyricsEditor() }
            )
        }
    }

    if (isModelManagerOpen) {
        com.example.ui.components.ModelManagerDialog(
            modelInfo = modelInfo,
            onVerifyModel = { viewModel.verifyModel() },
            onDismiss = { viewModel.closeModelManager() }
        )
    }
}