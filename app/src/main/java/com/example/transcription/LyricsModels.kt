package com.example.transcription

enum class KaraokeStyle(val displayName: String, val description: String) {
    SMOOTH_SLIDE_FADE("Smooth Slide & Fade", "Progressive line-by-line smooth vertical slide with ambient fade"),
    NEON_WORD_HIGHLIGHT("Neon Word Highlight", "Active word-by-word glowing neon swipe with gentle scale bounce"),
    KINETIC_BEAT_PULSE("Kinetic Beat Pulse", "High-energy typography pulsing with musical transients and bass drops"),
    CINEMATIC_GLOW("Cinematic Glow", "Soft external glow with floating animation and elegant fade-ins"),
    CYBER_NEON("Cyber Neon", "High-contrast neon outlines with digital glitch-style active words"),
    ELEGANT_MINIMAL("Elegant Minimal", "Clean typography with subtle under-glow and smooth transitions")
}

data class WordTimestamp(
    val word: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val confidence: Float = 1.0f
) {
    val durationMs: Long
        get() = (endTimeMs - startTimeMs).coerceAtLeast(0)
}

data class LyricsSegment(
    val id: String,
    val trackId: String,
    val lineIndex: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val words: List<WordTimestamp> = emptyList(),
    val isRtl: Boolean = false,
    val confidence: Float = 0.95f
) {
    val formattedTimestamp: String
        get() {
            val totalSec = startTimeMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return "%02d:%02d".format(min, sec)
        }

    fun isCurrentlyActive(currentPositionMs: Long): Boolean {
        return currentPositionMs in startTimeMs..endTimeMs
    }

    fun getActiveWordIndex(currentPositionMs: Long): Int {
        if (words.isEmpty()) return -1
        return words.indexOfFirst { currentPositionMs in it.startTimeMs..it.endTimeMs }
    }
}
