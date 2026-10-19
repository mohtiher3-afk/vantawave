package com.example

import android.app.Application
import com.example.data.AudioRepository
import com.example.data.VantaWaveDatabase
import com.example.export.VideoExportManager
import com.example.playback.VantaAudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VantaWaveApplication : Application() {
    val database by lazy { VantaWaveDatabase.getInstance(this) }
    val audioRepository by lazy { AudioRepository(this) }
    val audioPlayer by lazy { VantaAudioPlayer(this) }
    val videoExportManager by lazy { VideoExportManager(this) }
    val modelManager by lazy { com.example.ai.ModelManager(this) }
    val transcriptionEngine: com.example.ai.TranscriptionEngine by lazy { com.example.ai.OnnxQuantizedTranscriptionEngine(this, modelManager) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Pre-scan audio and ensure demo tracks exist
        appScope.launch {
            audioRepository.refreshTracks()
        }
    }
}
