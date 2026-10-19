package com.example.export

import com.example.data.AudioTrack
import com.example.visualizer.VisualizerRenderConfig
import java.io.File

enum class ExportStage {
    IDLE,
    PREPARING,
    ANALYZING_AUDIO,
    ENCODING_VIDEO,
    MUXING_AUDIO,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class VideoExportSettings(
    val track: AudioTrack,
    val title: String = track.title,
    val aspectRatioName: String = "9:16",
    val width: Int = 720,
    val height: Int = 1280,
    val fps: Int = 30,
    val durationSeconds: Int = 15, // Default 15s clip for social media, or full track
    val isFullTrack: Boolean = false,
    val renderConfig: VisualizerRenderConfig
) {
    val effectiveDurationSeconds: Int
        get() = if (isFullTrack) (track.durationMs / 1000).toInt().coerceAtLeast(5) else durationSeconds.coerceAtLeast(5)

    val totalFrames: Int
        get() = effectiveDurationSeconds * fps

    companion object {
        val RATIO_9_16_HD = AspectRatioPreset("9:16 HD", 720, 1280, "TikTok, Instagram Reels, Shorts (Fast Export)")
        val RATIO_9_16_FHD = AspectRatioPreset("9:16 FHD", 1080, 1920, "High Definition Reels/Shorts")
        val RATIO_1_1 = AspectRatioPreset("1:1 Square", 720, 720, "Instagram Feed & Posts")
        val RATIO_16_9 = AspectRatioPreset("16:9 Landscape", 1280, 720, "YouTube Standard Video")
        val RATIO_4_5 = AspectRatioPreset("4:5 Portrait", 720, 900, "Instagram Portrait Feed")

        val ALL_RATIOS = listOf(RATIO_9_16_HD, RATIO_9_16_FHD, RATIO_1_1, RATIO_16_9, RATIO_4_5)
    }
}

data class AspectRatioPreset(
    val name: String,
    val width: Int,
    val height: Int,
    val description: String
)

data class ExportProgress(
    val stage: ExportStage = ExportStage.IDLE,
    val progress: Float = 0f,
    val currentFrame: Int = 0,
    val totalFrames: Int = 0,
    val message: String = "",
    val outputFile: File? = null,
    val error: String? = null
)
