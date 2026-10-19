package com.example.ui

import android.app.Application
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VantaWaveApplication
import com.example.ai.ModelInfo
import com.example.ai.TranscriptionProgress
import com.example.alignment.WordLevelSynchronizer
import com.example.data.AudioTrack
import com.example.data.ExportRecordEntity
import com.example.data.ExportSettingsEntity
import com.example.data.PlaylistEntity
import com.example.data.VisualizerMode
import com.example.data.VisualizerPreset
import com.example.data.VisualizerProjectEntity
import com.example.export.ExportProgress
import com.example.export.VideoExportSettings
import com.example.playback.VantaRepeatMode
import com.example.transcription.KaraokeStyle
import com.example.transcription.LyricsSegment
import com.example.transcription.WordTimestamp
import com.example.visualizer.VisualizerRenderConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // خريطة المشاعر: كلمات دلالية -> ألوان
    private val MOOD_MAP = mapOf(
        "حب" to Color(0xFFFF4D6D), "love" to Color(0xFFFF4D6D), "عشق" to Color(0xFFFF4D6D), "قلب" to Color(0xFFFF4D6D),
        "نار" to Color(0xFFFFB300), "fire" to Color(0xFFFFB300), "حرق" to Color(0xFFFFB300), "شعلة" to Color(0xFFFFB300),
        "ليل" to Color(0xFF1A237E), "night" to Color(0xFF1A237E), "ظلام" to Color(0xFF1A237E), "dark" to Color(0xFF1A237E),
        "حزن" to Color(0xFF607D8B), "sad" to Color(0xFF607D8B), "دموع" to Color(0xFF607D8B), "ألم" to Color(0xFF607D8B),
        "فرح" to Color(0xFF00E5FF), "happy" to Color(0xFF00E5FF), "سعادة" to Color(0xFF00E5FF), "بهجة" to Color(0xFF00E5FF),
        "غضب" to Color(0xFFB71C1C), "angry" to Color(0xFFB71C1C), "صراخ" to Color(0xFFB71C1C)
    )

    init {
        startMoodAnalyzer()
    }

    private fun startMoodAnalyzer() {
        viewModelScope.launch {
            combine(currentPositionMs, _currentLyricsSegments) { position, segments ->
                position to segments
            }.collectLatest { (position, segments) ->
                val activeSegment = segments.find { it.startTimeMs <= position && it.endTimeMs >= position }
                val detectedColor = activeSegment?.text?.let { text ->
                    MOOD_MAP.entries.find { (keyword, _) -> 
                        text.contains(keyword, ignoreCase = true) 
                    }?.value
                }
                
                // تحديث التكوين باللون الجديد إذا تغير
                if (_renderConfig.value.currentMoodColor != detectedColor) {
                    _renderConfig.value = _renderConfig.value.copy(currentMoodColor = detectedColor)
                }
            }
        }
    }

    private val app = application as VantaWaveApplication
    private val repository = app.audioRepository
    private val player = app.audioPlayer
    private val exportManager = app.videoExportManager
    private val projectDao = app.database.visualizerProjectDao()
    private val exportSettingsDao = app.database.exportSettingsDao()
    private val exportDao = app.database.exportRecordDao()
    private val modelManager = app.modelManager
    private val transcriptionEngine = app.transcriptionEngine
    private val lyricsDao = app.database.lyricsDao()

    val modelInfo: StateFlow<ModelInfo> = modelManager.modelInfo
    private val _transcriptionProgress = MutableStateFlow(TranscriptionProgress())
    val transcriptionProgress: StateFlow<TranscriptionProgress> = _transcriptionProgress.asStateFlow()

    private val _currentLyricsSegments = MutableStateFlow<List<LyricsSegment>>(emptyList())
    val currentLyricsSegments: StateFlow<List<LyricsSegment>> = _currentLyricsSegments.asStateFlow()

    private val _isLyricsEditorOpen = MutableStateFlow(false)
    val isLyricsEditorOpen: StateFlow<Boolean> = _isLyricsEditorOpen.asStateFlow()

    private val _isModelManagerOpen = MutableStateFlow(false)
    val isModelManagerOpen: StateFlow<Boolean> = _isModelManagerOpen.asStateFlow()

    // Theme Accent Persistence
    private val themePrefs = application.getSharedPreferences("vanta_prefs", android.content.Context.MODE_PRIVATE)
    private val _activeAccent = MutableStateFlow(
        run {
            val savedName = themePrefs.getString("selected_theme_accent", com.example.ui.theme.ThemeAccent.NEON_PURPLE.name)
            try {
                com.example.ui.theme.ThemeAccent.valueOf(savedName ?: com.example.ui.theme.ThemeAccent.NEON_PURPLE.name)
            } catch (e: Exception) {
                com.example.ui.theme.ThemeAccent.NEON_PURPLE
            }
        }
    )
    val activeAccent: StateFlow<com.example.ui.theme.ThemeAccent> = _activeAccent.asStateFlow()

    fun updateAccent(accent: com.example.ui.theme.ThemeAccent) {
        _activeAccent.value = accent
        themePrefs.edit().putString("selected_theme_accent", accent.name).apply()
    }

    // Export settings flow
    val globalExportSettings: StateFlow<ExportSettingsEntity?> = exportSettingsDao.getGlobalSettings().stateIn(
        viewModelScope, SharingStarted.Lazily, null
    )

    // Library & Player State
    val tracks: StateFlow<List<AudioTrack>> = repository.tracks
    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlists.stateIn(
        viewModelScope, SharingStarted.Lazily, emptyList()
    )
    val currentTrack: StateFlow<AudioTrack?> = player.currentTrack
    val isPlaying: StateFlow<Boolean> = player.isPlaying
    val currentPositionMs: StateFlow<Long> = player.currentPositionMs
    val durationMs: StateFlow<Long> = player.durationMs
    val shuffleEnabled: StateFlow<Boolean> = player.shuffleEnabled
    val repeatMode: StateFlow<VantaRepeatMode> = player.repeatMode
    val sleepTimerMinutes: StateFlow<Int?> = player.sleepTimerMinutes
    val isSectionLoopEnabled: StateFlow<Boolean> = player.isSectionLoopEnabled
    val loopStartMs: StateFlow<Long> = player.loopStartMs
    val loopEndMs: StateFlow<Long> = player.loopEndMs

    fun toggleSectionLoop(enabled: Boolean) {
        player.setSectionLoopEnabled(enabled)
    }

    fun setLoopRange(startMs: Long, endMs: Long) {
        player.setLoopRange(startMs, endMs)
    }

    fun setLoopStartToCurrent() {
        val cur = player.currentPositionMs.value
        val end = player.loopEndMs.value
        player.setLoopRange(cur, if (end <= cur) cur + 5000L else end)
    }

    fun setLoopEndToCurrent() {
        val cur = player.currentPositionMs.value
        val start = player.loopStartMs.value
        player.setLoopRange(if (start >= cur) (cur - 5000L).coerceAtLeast(0L) else start, cur)
    }

    // Real-time Audio Frequency & Waveform buffers
    val fftData: StateFlow<FloatArray> = player.fftData
    val waveformPcm: StateFlow<FloatArray> = player.waveformPcm

    // Projects & Exports
    val projects: StateFlow<List<VisualizerProjectEntity>> = projectDao.getAllProjects().stateIn(
        viewModelScope, SharingStarted.Lazily, emptyList()
    )
    val recentProjects: StateFlow<List<VisualizerProjectEntity>> = projectDao.getRecentProjects(5).stateIn(
        viewModelScope, SharingStarted.Lazily, emptyList()
    )
    val exports: StateFlow<List<ExportRecordEntity>> = exportDao.getAllExports().stateIn(
        viewModelScope, SharingStarted.Lazily, emptyList()
    )
    val recentExports: StateFlow<List<ExportRecordEntity>> = exportDao.getRecentExports(5).stateIn(
        viewModelScope, SharingStarted.Lazily, emptyList()
    )

    // Visualizer Studio Config
    private val _renderConfig = MutableStateFlow(
        VisualizerRenderConfig(
            layers = listOf(VisualEffect.SpectrumBars()),
            primaryColor = Color(0xFF8B5CF6),
            secondaryColor = Color(0xFF06B6D4),
            backgroundColor = Color(0xFF0A0A0F),
            showTextOverlay = true
        )
    )
    val renderConfig: StateFlow<VisualizerRenderConfig> = _renderConfig.asStateFlow()

    // Export state
    val exportProgress: StateFlow<ExportProgress> = exportManager.progress
    private val _isExportDialogOpen = MutableStateFlow(false)
    val isExportDialogOpen: StateFlow<Boolean> = _isExportDialogOpen.asStateFlow()

    fun updateBackgroundConfig(newConfig: BackgroundConfig) {
        _renderConfig.value = _renderConfig.value.copy(backgroundConfig = newConfig)
    }

    fun setBackgroundAsset(path: String) {
        _renderConfig.value = _renderConfig.value.copy(
            backgroundConfig = _renderConfig.value.backgroundConfig.copy(assetPath = path)
        )
    }

    fun applyPreset(layers: List<VisualEffect>) {
        _renderConfig.value = _renderConfig.value.copy(
            layers = layers
        )
    }

    fun playTrack(track: AudioTrack, queue: List<AudioTrack> = emptyList()) {
        val q = if (queue.isNotEmpty()) queue else tracks.value
        player.playTrack(track, q)
        _renderConfig.value = _renderConfig.value.copy(
            titleText = track.title,
            artistText = track.artist
        )
        loadLyricsForTrack(track.id)
    }

    fun playOrPause() {
        if (currentTrack.value == null && tracks.value.isNotEmpty()) {
            playTrack(tracks.value.first())
        } else {
            player.playOrPause()
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun next() = player.next()
    fun previous() = player.previous()
    fun toggleShuffle() = player.toggleShuffle()
    fun cycleRepeatMode() = player.cycleRepeatMode()
    fun setSleepTimer(minutes: Int?) = player.setSleepTimer(minutes)

    fun toggleFavorite(trackId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(trackId)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
        }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
            Toast.makeText(app, "Added to playlist", Toast.LENGTH_SHORT).show()
        }
    }

    fun refreshTracks() {
        viewModelScope.launch {
            repository.refreshTracks()
        }
    }

    fun saveCurrentProject() {
        val track = currentTrack.value ?: tracks.value.firstOrNull() ?: return
        val config = _renderConfig.value
        val entity = VisualizerProjectEntity(
            id = "proj_${System.currentTimeMillis()}",
            name = "${track.title} Visual",
            trackId = track.id,
            trackTitle = track.title,
            trackArtist = track.artist,
            trackUriString = track.uriString,
            trackDurationMs = track.durationMs,
            mode = "MULTILAYER",
            primaryColorHex = "#%02x%02x%02x".format(
                (config.primaryColor.red * 255).toInt(),
                (config.primaryColor.green * 255).toInt(),
                (config.primaryColor.blue * 255).toInt()
            ),
            secondaryColorHex = "#%02x%02x%02x".format(
                (config.secondaryColor.red * 255).toInt(),
                (config.secondaryColor.green * 255).toInt(),
                (config.secondaryColor.blue * 255).toInt()
            ),
            backgroundColorHex = "#%02x%02x%02x".format(
                (config.backgroundColor.red * 255).toInt(),
                (config.backgroundColor.green * 255).toInt(),
                (config.backgroundColor.blue * 255).toInt()
            ),
            glowIntensity = config.glowIntensity,
            sensitivity = config.sensitivity,
            barCount = config.barCount,
            particleCount = config.particleCount,
            customImageUri = config.customImageUri,
            showTextOverlay = config.showTextOverlay,
            layersJson = Json.encodeToString(config.layers)
        )

        viewModelScope.launch {
            projectDao.insertOrUpdateProject(entity)
            Toast.makeText(app, "Project saved!", Toast.LENGTH_SHORT).show()
        }
    }

    fun restoreProject(project: VisualizerProjectEntity) {
        val mode = try {
            VisualizerMode.valueOf(project.mode)
        } catch (e: Exception) {
            VisualizerMode.CIRCULAR_SPECTRUM
        }

        _renderConfig.value = VisualizerRenderConfig(
            mode = mode,
            primaryColor = Color(android.graphics.Color.parseColor(project.primaryColorHex)),
            secondaryColor = Color(android.graphics.Color.parseColor(project.secondaryColorHex)),
            backgroundColor = Color(android.graphics.Color.parseColor(project.backgroundColorHex)),
            glowIntensity = project.glowIntensity,
            sensitivity = project.sensitivity,
            barCount = project.barCount,
            particleCount = project.particleCount,
            customImageUri = project.customImageUri,
            showTextOverlay = project.showTextOverlay,
            titleText = project.trackTitle,
            artistText = project.trackArtist
        )

        // Find or create track
        val matched = tracks.value.find { it.id == project.trackId }
        if (matched != null) {
            playTrack(matched)
        } else {
            val t = AudioTrack(
                id = project.trackId,
                uriString = project.trackUriString,
                title = project.trackTitle,
                artist = project.trackArtist,
                album = "VantaWave Project",
                durationMs = project.trackDurationMs
            )
            playTrack(t)
        }
    }

    fun cloneProject(project: VisualizerProjectEntity) {
        viewModelScope.launch {
            val cloned = projectDao.cloneProject(project.id)
            if (cloned != null) {
                Toast.makeText(app, "Cloned '${cloned.name}'", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteProject(project: VisualizerProjectEntity) {
        viewModelScope.launch {
            projectDao.deleteProject(project)
        }
    }

    fun deleteExport(export: ExportRecordEntity) {
        viewModelScope.launch {
            exportDao.deleteExport(export)
        }
    }

    fun openExportDialog() {
        _isExportDialogOpen.value = true
    }

    fun closeExportDialog() {
        _isExportDialogOpen.value = false
    }

    fun startExport(settings: VideoExportSettings) {
        viewModelScope.launch {
            // Persist as global last-used export settings
            val globalEntity = com.example.data.ExportSettingsEntity(
                id = com.example.data.ExportSettingsEntity.GLOBAL_ID,
                projectId = null,
                aspectRatioName = settings.aspectRatioName,
                width = settings.width,
                height = settings.height,
                fps = settings.fps,
                durationSeconds = settings.durationSeconds,
                isFullTrack = settings.isFullTrack,
                lastUsedAt = System.currentTimeMillis()
            )
            exportSettingsDao.insertOrUpdate(globalEntity)

            // Also persist as project default if current track matches a saved project
            val activeProject = projects.value.find { it.trackId == settings.track.id }
            if (activeProject != null) {
                exportSettingsDao.insertOrUpdate(
                    globalEntity.copy(
                        id = "export_proj_${activeProject.id}",
                        projectId = activeProject.id
                    )
                )
            }

            exportManager.exportVideo(settings)
        }
    }

    fun cancelExport() {
        exportManager.cancelExport()
    }

    fun openLyricsEditor() {
        _isLyricsEditorOpen.value = true
    }

    fun closeLyricsEditor() {
        _isLyricsEditorOpen.value = false
    }

    fun openModelManager() {
        _isModelManagerOpen.value = true
    }

    fun closeModelManager() {
        _isModelManagerOpen.value = false
    }

    fun verifyModel() {
        viewModelScope.launch {
            modelManager.verifyModelStatus()
        }
    }

    fun setKaraokeStyle(style: KaraokeStyle) {
        _renderConfig.value = _renderConfig.value.copy(karaokeStyle = style)
    }

    fun toggleShowLyrics(show: Boolean) {
        _renderConfig.value = _renderConfig.value.copy(showLyrics = show)
    }

    fun loadLyricsForTrack(trackId: String) {
        viewModelScope.launch {
            try {
                val dbSegments = lyricsDao.getSegmentsForTrack(trackId).first()
                if (dbSegments.isNotEmpty()) {
                    val allWords = lyricsDao.getAllWordsForTrack(trackId).first()
                    val wordMap = allWords.groupBy { it.segmentId }

                    val fullSegments = dbSegments.map { seg ->
                        val words = wordMap[seg.id]?.map { w ->
                            WordTimestamp(
                                word = w.word,
                                startTimeMs = w.startTimeMs,
                                endTimeMs = w.endTimeMs,
                                confidence = w.confidence
                            )
                        } ?: emptyList()

                        LyricsSegment(
                            id = seg.id,
                            trackId = seg.trackId,
                            lineIndex = seg.lineIndex,
                            startTimeMs = seg.startTimeMs,
                            endTimeMs = seg.endTimeMs,
                            text = seg.text,
                            words = words,
                            isRtl = seg.isRtl,
                            confidence = seg.confidence
                        )
                    }

                    _currentLyricsSegments.value = fullSegments
                    _renderConfig.value = _renderConfig.value.copy(lyricsSegments = fullSegments)
                } else {
                    // Automatically transcribe track locally on device
                    val track = tracks.value.find { it.id == trackId } ?: currentTrack.value
                    if (track != null) {
                        transcribeCurrentTrack(track)
                    }
                }
            } catch (e: Exception) {
                val track = tracks.value.find { it.id == trackId } ?: currentTrack.value
                if (track != null) {
                    transcribeCurrentTrack(track)
                }
            }
        }
    }

    fun transcribeCurrentTrack(trackToTranscribe: AudioTrack? = null) {
        val target = trackToTranscribe ?: currentTrack.value ?: tracks.value.firstOrNull() ?: return
        viewModelScope.launch {
            transcriptionEngine.transcribe(com.example.ai.AudioInputSource.TrackSource(target)).collect { step ->
                when (step) {
                    is com.example.ai.TranscriptionStep.Progress -> {
                        _transcriptionProgress.value = com.example.ai.TranscriptionProgress(
                            stage = step.stage,
                            percentage = step.percentage,
                            isRunning = step.isRunning
                        )
                    }
                    is com.example.ai.TranscriptionStep.IncrementalSegment -> {
                        val current = _currentLyricsSegments.value.toMutableList()
                        if (current.none { it.id == step.segment.id }) {
                            current.add(step.segment)
                            _currentLyricsSegments.value = current
                            _renderConfig.value = _renderConfig.value.copy(lyricsSegments = current)
                        }
                    }
                    is com.example.ai.TranscriptionStep.SegmentsReady -> {
                        _currentLyricsSegments.value = step.segments
                        _renderConfig.value = _renderConfig.value.copy(lyricsSegments = step.segments)
                    }
                    is com.example.ai.TranscriptionStep.Complete -> {
                        _currentLyricsSegments.value = step.segments
                        _renderConfig.value = _renderConfig.value.copy(lyricsSegments = step.segments)
                        _transcriptionProgress.value = com.example.ai.TranscriptionProgress(
                            stage = "Transcription complete",
                            percentage = 100,
                            isRunning = false
                        )
                        // Persist to Room
                        lyricsDao.saveFullLyrics(target.id, step.segments)
                    }
                    is com.example.ai.TranscriptionStep.Failure -> {
                        _transcriptionProgress.value = com.example.ai.TranscriptionProgress(
                            stage = "Error",
                            percentage = 0,
                            isRunning = false,
                            errorMessage = step.errorMessage
                        )
                    }
                }
            }
        }
    }

    fun nudgeSegment(segment: LyricsSegment, deltaMs: Long) {
        val currentList = _currentLyricsSegments.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == segment.id }
        if (index >= 0) {
            val nudged = WordLevelSynchronizer.nudgeSegment(segment, deltaMs, durationMs.value)
            currentList[index] = nudged
            _currentLyricsSegments.value = currentList
            _renderConfig.value = _renderConfig.value.copy(lyricsSegments = currentList)

            viewModelScope.launch {
                val track = currentTrack.value ?: return@launch
                lyricsDao.saveFullLyrics(track.id, currentList)
            }
        }
    }

    fun updateSegmentText(segment: LyricsSegment, newText: String) {
        val currentList = _currentLyricsSegments.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == segment.id }
        if (index >= 0) {
            val updated = WordLevelSynchronizer.resyncWithNewText(segment, newText)
            currentList[index] = updated
            _currentLyricsSegments.value = currentList
            _renderConfig.value = _renderConfig.value.copy(lyricsSegments = currentList)

            viewModelScope.launch {
                val track = currentTrack.value ?: return@launch
                lyricsDao.saveFullLyrics(track.id, currentList)
            }
        }
    }

    fun deleteSegment(segment: LyricsSegment) {
        val currentList = _currentLyricsSegments.value.toMutableList()
        currentList.removeAll { it.id == segment.id }
        _currentLyricsSegments.value = currentList
        _renderConfig.value = _renderConfig.value.copy(lyricsSegments = currentList)

        viewModelScope.launch {
            val track = currentTrack.value ?: return@launch
            lyricsDao.saveFullLyrics(track.id, currentList)
        }
    }

    fun addSegment(startTimeMs: Long, text: String) {
        val track = currentTrack.value ?: return
        val currentList = _currentLyricsSegments.value.toMutableList()
        val duration = durationMs.value
        val endTimeMs = (startTimeMs + 3000L).coerceAtMost(duration)

        val words = WordLevelSynchronizer.synchronizeWords(text, startTimeMs, endTimeMs)
        val newSegment = LyricsSegment(
            id = "seg_${track.id}_${System.currentTimeMillis()}",
            trackId = track.id,
            lineIndex = currentList.size,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            text = text,
            words = words,
            isRtl = com.example.transcription.LanguageDetector.isRtl(text),
            confidence = 1.0f
        )
        currentList.add(newSegment)
        currentList.sortBy { it.startTimeMs }
        val reindexed = currentList.mapIndexed { idx, s -> s.copy(lineIndex = idx) }

        _currentLyricsSegments.value = reindexed
        _renderConfig.value = _renderConfig.value.copy(lyricsSegments = reindexed)

        viewModelScope.launch {
            lyricsDao.saveFullLyrics(track.id, reindexed)
            Toast.makeText(app, "Lyric line added & saved", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateLyrics(song: String, artist: String, language: String) {
        val track = currentTrack.value ?: return
        viewModelScope.launch {
            try {
                val rawText = LyricsGenerator.generateLyricsText(song, artist, language)
                val segments = LyricsGenerator.createSegments(rawText, track.id, durationMs.value)
                
                _currentLyricsSegments.value = segments
                _renderConfig.value = _renderConfig.value.copy(lyricsSegments = segments)
                
                // Persist to Room
                lyricsDao.saveFullLyrics(track.id, segments)
                
                Toast.makeText(app, "Lyrics generated in $language!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(app, "Failed to generate lyrics", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun startRecording() {
        Toast.makeText(app, "Recording started for ${renderConfig.value.mode.displayName}", Toast.LENGTH_SHORT).show()
        // TODO: Implement actual recording logic
    }
}
