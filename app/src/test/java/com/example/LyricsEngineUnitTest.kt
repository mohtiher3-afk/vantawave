package com.example

import com.example.alignment.WordLevelSynchronizer
import com.example.audio.MusicAnalyzer
import com.example.transcription.LanguageDetector
import com.example.transcription.LyricsSegment
import com.example.transcription.WordTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsEngineUnitTest {

    @Test
    fun testLanguageDetector_arabicRtl() {
        val arabicText = "في سكون الليل يصدح هذا النغم"
        assertTrue(LanguageDetector.isRtl(arabicText))
        assertEquals("ar", LanguageDetector.detectLanguage(arabicText))
    }

    @Test
    fun testLanguageDetector_englishLtr() {
        val englishText = "Neon lights illuminate the digital street"
        assertFalse(LanguageDetector.isRtl(englishText))
        assertEquals("en", LanguageDetector.detectLanguage(englishText))
    }

    @Test
    fun testWordLevelSynchronizer_generatesSequentialTimestamps() {
        val phrase = "Living inside a synthwave dream"
        val startMs = 5000L
        val endMs = 8000L

        val words = WordLevelSynchronizer.synchronizeWords(
            rawText = phrase,
            startTimeMs = startMs,
            endTimeMs = endMs
        )

        assertEquals(5, words.size)
        assertEquals("Living", words.first().word)
        assertEquals("dream", words.last().word)
        assertTrue("First word must start at or after startMs", words.first().startTimeMs >= startMs)
        assertTrue("Last word must end at endMs", words.last().endTimeMs <= endMs + 50L)

        // Verify sequential monotonicity
        for (i in 0 until words.size - 1) {
            assertTrue(
                "Words must have ascending timestamps",
                words[i].startTimeMs <= words[i + 1].startTimeMs
            )
        }
    }

    @Test
    fun testWordLevelSynchronizer_nudgeSegment() {
        val segment = LyricsSegment(
            id = "seg_1",
            trackId = "track_1",
            lineIndex = 0,
            startTimeMs = 2000L,
            endTimeMs = 5000L,
            text = "Test Phrase",
            words = listOf(
                WordTimestamp("Test", 2000L, 3500L),
                WordTimestamp("Phrase", 3500L, 5000L)
            )
        )

        val nudged = WordLevelSynchronizer.nudgeSegment(segment, 500L, 60000L)
        assertEquals(2500L, nudged.startTimeMs)
        assertEquals(5500L, nudged.endTimeMs)
        assertEquals(2500L, nudged.words.first().startTimeMs)
        assertEquals(5500L, nudged.words.last().endTimeMs)
    }

    @Test
    fun testMusicAnalyzer_pcmAnalysis() {
        // Generate a synthetic 44.1kHz 1-second sine wave burst
        val sampleRate = 44100
        val pcm = ShortArray(sampleRate) { i ->
            val sinVal = kotlin.math.sin(2.0 * Math.PI * 440.0 * i / sampleRate)
            (sinVal * 16000).toInt().toShort()
        }

        val result = MusicAnalyzer.analyzePcmAudio(pcm, sampleRate)
        assertTrue(result.averageEnergy > 0f)
        assertTrue(result.estimatedBpm in 60.0..180.0)
    }
}
