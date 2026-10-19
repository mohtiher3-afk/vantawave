package com.example.playback

import android.content.Context
import android.content.Intent
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class VantaRepeatMode {
    OFF, ALL, ONE
}

class VantaAudioPlayer(private val context: Context) {
    private val TAG = "VantaAudioPlayer"
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .build()

    private var visualizer: Visualizer? = null

    private val _currentTrack = MutableStateFlow<AudioTrack?>(null)
    val currentTrack: StateFlow<AudioTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<AudioTrack>>(emptyList())
    val queue: StateFlow<List<AudioTrack>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(VantaRepeatMode.ALL)
    val repeatMode: StateFlow<VantaRepeatMode> = _repeatMode.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _sleepTimerRemainingSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Int?> = _sleepTimerRemainingSeconds.asStateFlow()

    // Section Loop Visualization Support
    private val _isSectionLoopEnabled = MutableStateFlow(false)
    val isSectionLoopEnabled: StateFlow<Boolean> = _isSectionLoopEnabled.asStateFlow()

    private val _loopStartMs = MutableStateFlow(0L)
    val loopStartMs: StateFlow<Long> = _loopStartMs.asStateFlow()

    private val _loopEndMs = MutableStateFlow(15000L)
    val loopEndMs: StateFlow<Long> = _loopEndMs.asStateFlow()

    fun setSectionLoopEnabled(enabled: Boolean) {
        _isSectionLoopEnabled.value = enabled
        if (enabled) {
            val cur = exoPlayer.currentPosition
            val start = _loopStartMs.value
            val end = _loopEndMs.value
            if (cur < start || cur >= end) {
                seekTo(start)
            }
        }
    }

    fun setLoopRange(startMs: Long, endMs: Long) {
        val validStart = startMs.coerceAtLeast(0L)
        val validEnd = endMs.coerceAtLeast(validStart + 1000L)
        _loopStartMs.value = validStart
        _loopEndMs.value = validEnd
        if (_isSectionLoopEnabled.value) {
            val cur = exoPlayer.currentPosition
            if (cur < validStart || cur >= validEnd) {
                seekTo(validStart)
            }
        }
    }

    // Real-time audio frequency and waveform buffers
    private val _fftData = MutableStateFlow(FloatArray(64))
    val fftData: StateFlow<FloatArray> = _fftData.asStateFlow()

    private val _waveformPcm = MutableStateFlow(FloatArray(128))
    val waveformPcm: StateFlow<FloatArray> = _waveformPcm.asStateFlow()

    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    private var positionUpdateJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var fallbackAnalysisJob: Job? = null

    private var originalQueue = listOf<AudioTrack>()

    init {
        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startPlaybackService()
                    startPositionUpdates()
                    attachVisualizer()
                } else {
                    stopPositionUpdates()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                } else if (playbackState == Player.STATE_ENDED) {
                    handleTrackEnded()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentIdx = _queue.value.indexOfFirst { it.uriString == mediaItem?.mediaId }
                if (currentIdx != -1) {
                    _currentIndex.value = currentIdx
                    _currentTrack.value = _queue.value[currentIdx]
                }
            }
        })
    }

    private fun startPlaybackService() {
        try {
            val intent = Intent(context, PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start PlaybackService", e)
        }
    }

    private fun buildMediaItem(track: AudioTrack): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setDisplayTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album.ifEmpty { "VantaWave" })
            .apply {
                if (!track.albumArtUri.isNullOrEmpty()) {
                    setArtworkUri(Uri.parse(track.albumArtUri))
                }
            }
            .build()

        return MediaItem.Builder()
            .setUri(Uri.parse(track.uriString))
            .setMediaId(track.uriString)
            .setMediaMetadata(metadata)
            .build()
    }

    fun playTrack(track: AudioTrack, queueList: List<AudioTrack> = listOf(track)) {
        originalQueue = queueList
        val currentQueue = if (_shuffleEnabled.value) {
            val shuffled = queueList.filter { it.id != track.id }.shuffled()
            listOf(track) + shuffled
        } else {
            queueList
        }
        _queue.value = currentQueue
        val targetIdx = currentQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        _currentIndex.value = targetIdx
        _currentTrack.value = track

        val mediaItems = currentQueue.map { buildMediaItem(it) }
        exoPlayer.setMediaItems(mediaItems, targetIdx, 0L)
        exoPlayer.prepare()
        exoPlayer.play()
        startPlaybackService()
    }

    private fun loadTrackIntoExoPlayer(track: AudioTrack, autoPlay: Boolean) {
        val mediaItem = buildMediaItem(track)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (autoPlay) {
            exoPlayer.play()
            startPlaybackService()
        }
    }

    fun playOrPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.mediaItemCount == 0) {
                _currentTrack.value?.let { playTrack(it, _queue.value.ifEmpty { listOf(it) }) }
            } else {
                exoPlayer.play()
                startPlaybackService()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
        _currentPositionMs.value = positionMs
    }

    fun next() {
        val q = _queue.value
        if (q.isEmpty()) return
        val nextIdx = (_currentIndex.value + 1) % q.size
        _currentIndex.value = nextIdx
        val track = q[nextIdx]
        _currentTrack.value = track
        loadTrackIntoExoPlayer(track, autoPlay = true)
    }

    fun previous() {
        val q = _queue.value
        if (q.isEmpty()) return
        if (exoPlayer.currentPosition > 3000) {
            exoPlayer.seekTo(0)
            return
        }
        val prevIdx = if (_currentIndex.value - 1 < 0) q.size - 1 else _currentIndex.value - 1
        _currentIndex.value = prevIdx
        val track = q[prevIdx]
        _currentTrack.value = track
        loadTrackIntoExoPlayer(track, autoPlay = true)
    }

    fun toggleShuffle() {
        val newShuffle = !_shuffleEnabled.value
        _shuffleEnabled.value = newShuffle
        val current = _currentTrack.value
        if (newShuffle && current != null) {
            val remaining = originalQueue.filter { it.id != current.id }.shuffled()
            _queue.value = listOf(current) + remaining
            _currentIndex.value = 0
        } else {
            _queue.value = originalQueue
            _currentIndex.value = originalQueue.indexOfFirst { it.id == current?.id }.coerceAtLeast(0)
        }
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            VantaRepeatMode.OFF -> VantaRepeatMode.ALL
            VantaRepeatMode.ALL -> VantaRepeatMode.ONE
            VantaRepeatMode.ONE -> VantaRepeatMode.OFF
        }
    }

    fun setSleepTimer(minutes: Int?) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _sleepTimerRemainingSeconds.value = null
            return
        }

        var remaining = minutes * 60
        _sleepTimerRemainingSeconds.value = remaining

        sleepTimerJob = scope.launch {
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining
            }
            if (isActive) {
                exoPlayer.pause()
                _sleepTimerMinutes.value = null
                _sleepTimerRemainingSeconds.value = null
            }
        }
    }

    private fun handleTrackEnded() {
        when (_repeatMode.value) {
            VantaRepeatMode.ONE -> {
                exoPlayer.seekTo(0)
                exoPlayer.play()
            }
            VantaRepeatMode.ALL -> {
                next()
            }
            VantaRepeatMode.OFF -> {
                if (_currentIndex.value < _queue.value.size - 1) {
                    next()
                } else {
                    exoPlayer.pause()
                    exoPlayer.seekTo(0)
                }
            }
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (isActive && exoPlayer.isPlaying) {
                val pos = exoPlayer.currentPosition
                _currentPositionMs.value = pos
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                _durationMs.value = dur

                // Loop section check
                if (_isSectionLoopEnabled.value) {
                    val end = _loopEndMs.value
                    val start = _loopStartMs.value
                    if (pos >= end) {
                        exoPlayer.seekTo(start)
                        _currentPositionMs.value = start
                    }
                }
                delay(60)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
    }

    // --- Real-time Visualizer and Audio Frequency Analysis ---
    private fun attachVisualizer() {
        try {
            val sessionId = exoPlayer.audioSessionId
            _audioSessionId.value = sessionId
            if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0) {
                startFallbackAnalysis()
                return
            }

            visualizer?.release()
            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(512)
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(vis: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                            waveform?.let { processWaveform(it) }
                        }

                        override fun onFftDataCapture(vis: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            fft?.let { processFft(it) }
                        }
                    },
                    Visualizer.getMaxCaptureRate(),
                    true,
                    true
                )
                enabled = true
            }
            fallbackAnalysisJob?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Hardware Visualizer init fallback: ${e.message}")
            startFallbackAnalysis()
        }
    }

    private fun processWaveform(waveform: ByteArray) {
        val out = FloatArray(128)
        val step = (waveform.size.toFloat() / 128f).coerceAtLeast(1f)
        for (i in 0 until 128) {
            val idx = (i * step).toInt().coerceIn(0, waveform.size - 1)
            // PCM 8-bit unsigned is 0..255 with 128 as center
            val sample = (waveform[idx].toInt() and 0xFF) - 128
            out[i] = (sample / 128f).coerceIn(-1f, 1f)
        }
        _waveformPcm.value = out
    }

    private fun processFft(fft: ByteArray) {
        val bands = 64
        val magnitudes = FloatArray(bands)
        val n = (fft.size / 2).coerceAtMost(bands)
        for (i in 0 until n) {
            val re = fft[2 * i].toFloat()
            val im = fft[2 * i + 1].toFloat()
            val mag = Math.hypot(re.toDouble(), im.toDouble()).toFloat()
            magnitudes[i] = (mag / 128f).coerceIn(0f, 1.5f)
        }
        _fftData.value = magnitudes
    }

    private fun startFallbackAnalysis() {
        fallbackAnalysisJob?.cancel()
        fallbackAnalysisJob = scope.launch(Dispatchers.Default) {
            var phase = 0.0
            while (isActive && _isPlaying.value) {
                phase += 0.15
                val bands = FloatArray(64)
                val wave = FloatArray(128)

                // Synthesize active frequency bands reacting to beat
                val beatAmp = (sin(phase * 2.0) * 0.5 + 0.5).toFloat()
                for (i in bands.indices) {
                    val freqFactor = 1.0f - (i.toFloat() / 64f)
                    val modulation = (sin(phase + i * 0.4) * 0.4 + 0.6).toFloat()
                    bands[i] = (beatAmp * 0.7f + modulation * 0.3f) * freqFactor
                }

                for (i in wave.indices) {
                    val t = (i.toFloat() / 128f) * 4.0 * PI
                    wave[i] = (sin(t + phase) * 0.6f + cos(t * 2.0 + phase) * 0.3f).toFloat() * beatAmp
                }

                _fftData.value = bands
                _waveformPcm.value = wave
                delay(33) // ~30 fps update
            }
        }
    }

    fun release() {
        stopPositionUpdates()
        sleepTimerJob?.cancel()
        fallbackAnalysisJob?.cancel()
        try {
            visualizer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        exoPlayer.release()
    }
}
