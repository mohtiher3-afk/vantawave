package com.example.transcription

import kotlin.math.roundToLong

object LyricsGenerator {
    /**
     * Simulates generating lyrics for a song. In a real app, this would call a GenAI API or a Lyrics DB.
     */
    fun generateLyricsText(song: String, artist: String, language: String): String {
        return if (language == "Arabic") {
            """
            (مقدمة موسيقية)
            أهلاً بكم في عالم الفن والجمال
            هذه كلمات أغنية $song للفنان $artist
            نغني معاً في هذه الليلة
            ألحان تنساب كالنهر الجاري
            قلوبنا تخفق على إيقاع الموسيقى
            نحلق عالياً في سماء الخيال
            (جسر موسيقي)
            يا جمال هذه اللحظات الساحرة
            صوتك يملأ الكون ضياءً
            نهاية الرحلة وبداية الحلم
            (خاتمة)
            """.trimIndent()
        } else {
            """
            (Musical Intro)
            Welcome to the world of art and beauty
            These are the lyrics for $song by $artist
            We sing together in this night
            Melodies flowing like a running river
            Our hearts beat to the rhythm of music
            Flying high in the sky of imagination
            (Bridge)
            Oh, the beauty of these magical moments
            Your voice fills the universe with light
            End of the journey, start of the dream
            (Outro)
            """.trimIndent()
        }
    }

    /**
     * Transforms raw text into synchronized segments by distributing lines across the song duration.
     */
    fun createSegments(text: String, trackId: String, durationMs: Long): List<LyricsSegment> {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val segmentDuration = durationMs / lines.size
        
        return lines.mapIndexed { index, line ->
            val start = (index * segmentDuration).toLong()
            val end = ((index + 1) * segmentDuration).toLong().coerceAtMost(durationMs)
            
            LyricsSegment(
                id = "gen_${trackId}_${index}_${System.currentTimeMillis()}",
                trackId = trackId,
                lineIndex = index,
                startTimeMs = start,
                endTimeMs = end,
                text = line,
                words = emptyList(), // Generation doesn't provide word-level timestamps without AI sync
                isRtl = com.example.transcription.LanguageDetector.isRtl(line),
                confidence = 0.9f
            )
        }
    }
}
