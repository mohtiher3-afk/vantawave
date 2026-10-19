package com.example.visualizer

import androidx.compose.ui.graphics.Color
import com.example.transcription.KaraokeStyle
import com.example.transcription.LyricsSegment

// نوع الخلفية المدعومة
enum class BackgroundType {
    COLOR, IMAGE, VIDEO, ANIMATED_IMAGE
}

// إعدادات الخلفية وتفاعلها مع الصوت
data class BackgroundConfig(
    val type: BackgroundType = BackgroundType.COLOR,
    val assetPath: String? = null,
    val reactionStrength: Float = 0.5f, // مدى قوة التأثر بالبيس (0.0 to 1.0)
    val reactionType: ReactionType = ReactionType.SCALE,
    val opacity: Float = 1.0f
) {
    enum class ReactionType {
        SCALE, // تضخم وتتقلص مع البيس
        BRIGHTNESS, // تزداد إضاءتها مع البيس
        SATURATION, // تزداد تشبع ألوانها مع البيس
        NONE // ثابتة
    }
}

// الهيكل الجديد للتأثيرات: كل تأثير هو "طبقة" قابلة للتعديل
sealed class VisualEffect {
    abstract val id: String
    abstract val name: String

    data class SpectrumBars(
        override val id: String = "spectrum_bars",
        override val name: String = "Spectrum Bars",
        val barCount: Int = 48,
        val sensitivity: Float = 1.0f,
        val glowIntensity: Float = 0.85f
    ) : VisualEffect()

    data class NeonWave(
        override val id: String = "neon_wave",
        override val name: String = "Neon Wave",
        val amplitude: Float = 1.0f,
        val glowIntensity: Float = 0.9f
    ) : VisualEffect()

    data class ParticleBurst(
        override val id: String = "particle_burst",
        override val name: String = "Particle Burst",
        val particleCount: Int = 120,
        val speed: Float = 1.0f
    ) : VisualEffect()

    data class CircularSpectrum(
        override val id: String = "circular_spectrum",
        override val name: String = "Circular Spectrum",
        val barCount: Int = 48,
        val sensitivity: Float = 1.0f
    ) : VisualEffect()

    data class RadialRings(
        override val id: String = "radial_rings",
        override val name: String = "Radial Rings",
        val ringCount: Int = 6
    ) : VisualEffect()

    data class CyberGrid(
        override val id: String = "cyber_grid",
        override val name: String = "Cyber Grid",
        val speed: Float = 1.0f
    ) : VisualEffect()

    data class NeonLiquidWave(
        override val id: String = "neon_liquid",
        override val name: String = "Neon Liquid",
        val intensity: Float = 1.0f
    ) : VisualEffect()
}

data class VisualizerRenderConfig(
    // نظام الطبقات بدلاً من نمط واحد
    val layers: List<VisualEffect> = listOf(VisualEffect.SpectrumBars()),
    
    val primaryColor: Color = Color(0xFF8B5CF6),
    val secondaryColor: Color = Color(0xFF06B6D4),
    val backgroundColor: Color = Color(0xFF0A0A0F),
    
    // إعدادات عامة للتأثيرات (للتحكم السريع)
    val sensitivity: Float = 1.0f,
    val glowIntensity: Float = 0.85f,
    val barCount: Int = 48,
    val particleCount: Int = 120,
    
    // إعدادات الخلفية التفاعلية
    val backgroundConfig: BackgroundConfig = BackgroundConfig(),
    val customImageUri: String? = null,
    
    val showTextOverlay: Boolean = true,
    val titleText: String = "",
    val artistText: String = "",
    val showLyrics: Boolean = true,
    val karaokeStyle: KaraokeStyle = KaraokeStyle.SMOOTH_SLIDE_FADE,
    val lyricsSegments: List<LyricsSegment> = emptyList(),

    // --- إضافات الإبداع السينمائي ---
    val glitchIntensity: Float = 0.5f,
    val cameraShakeEnabled: Boolean = true,
    val cameraShakeStrength: Float = 0.03f
)

data class Particle(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var radius: Float = 4f,
    var alpha: Float = 1f,
    var life: Float = 1f,
    var maxLife: Float = 1f,
    var colorIndex: Float = 0f // 0 = primary, 1 = secondary
)
