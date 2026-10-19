package com.example.ai

import android.content.Context
import com.example.data.AudioTrack
import com.example.transcription.LyricsSegment
import kotlinx.coroutines.flow.Flow
import java.io.File

data class TranscriptionProgress(
    val stage: String = "",
    val percentage: Int = 0,
    val isRunning: Boolean = false,
    val errorMessage: String? = null
)

/**
 * On-device offline AI speech transcription engine implementing [TranscriptionEngine].
 * Operates with zero network connectivity or external APIs; fully operational in Airplane Mode.
 */
class OfflineTranscriptionEngine(
    context: Context,
    modelManager: ModelManager
) : TranscriptionEngine {

    private val delegate = OnnxQuantizedTranscriptionEngine(context, modelManager)

    override val initializationStatus: kotlinx.coroutines.flow.StateFlow<ModelInitializationStatus>
        get() = delegate.initializationStatus

    override suspend fun ensureModelLoaded(): ModelLoadResult = delegate.ensureModelLoaded()

    override suspend fun loadModel(modelFile: File): ModelLoadResult = delegate.loadModel(modelFile)

    override suspend fun verifyModelIntegrity(
        modelFile: File,
        expectedSha256: String?
    ): ModelIntegrityResult = delegate.verifyModelIntegrity(modelFile, expectedSha256)

    override fun transcribe(input: AudioInputSource): Flow<TranscriptionStep> = delegate.transcribe(input)

    override fun getLoadedModelInfo(): ModelInfo? = delegate.getLoadedModelInfo()

    override fun isLoaded(): Boolean = delegate.isLoaded()

    override fun release() = delegate.release()

    /**
     * Backward-compatible suspend method for transcribing an [AudioTrack] with a progress callback.
     */
    suspend fun transcribeTrack(
        track: AudioTrack,
        onProgress: (TranscriptionProgress) -> Unit = {}
    ): List<LyricsSegment> {
        var finalSegments = emptyList<LyricsSegment>()
        delegate.transcribe(AudioInputSource.TrackSource(track)).collect { step ->
            when (step) {
                is TranscriptionStep.Progress -> {
                    onProgress(
                        TranscriptionProgress(
                            stage = step.stage,
                            percentage = step.percentage,
                            isRunning = step.isRunning
                        )
                    )
                }
                is TranscriptionStep.IncrementalSegment -> {
                    // Intermediate progressive segment emitted
                }
                is TranscriptionStep.SegmentsReady -> {
                    finalSegments = step.segments
                }
                is TranscriptionStep.Complete -> {
                    finalSegments = step.segments
                    onProgress(
                        TranscriptionProgress(
                            stage = "Transcription complete",
                            percentage = 100,
                            isRunning = false
                        )
                    )
                }
                is TranscriptionStep.Failure -> {
                    onProgress(
                        TranscriptionProgress(
                            stage = "Error",
                            percentage = 0,
                            isRunning = false,
                            errorMessage = step.errorMessage
                        )
                    )
                }
            }
        }
        return finalSegments
    }
}
