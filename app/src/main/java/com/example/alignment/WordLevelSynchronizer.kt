package com.example.alignment

import com.example.audio.AudioAnalysisResult
import com.example.transcription.LanguageDetector
import com.example.transcription.LyricsSegment
import com.example.transcription.WordTimestamp
import kotlin.math.max

/**
 * Word-level audio synchronization engine.
 * Maps recognized lyric words to precise millisecond timestamps by combining
 * vocal energy envelopes, syllable weighting, and transient speech cues.
 */
object WordLevelSynchronizer {

    /**
     * Synchronizes a text line across a detected vocal segment using intelligent
     * phoneme/syllable duration weighting and local audio transient alignments.
     */
    fun synchronizeWords(
        rawText: String,
        startTimeMs: Long,
        endTimeMs: Long,
        audioAnalysis: AudioAnalysisResult? = null,
        overallConfidence: Float = 0.95f
    ): List<WordTimestamp> {
        val words = rawText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()

        val totalDurationMs = (endTimeMs - startTimeMs).coerceAtLeast(300L)

        // 1. Calculate relative weight based on character count and vowels/syllables
        val weights = words.map { word ->
            computeWordWeight(word)
        }
        val totalWeight = weights.sum().coerceAtLeast(1.0)

        // 2. Initial word boundary partition based on phonetic weights
        val wordTimestamps = mutableListOf<WordTimestamp>()
        var currentStart = startTimeMs

        for (i in words.indices) {
            val word = words[i]
            val durationForWord = ((weights[i] / totalWeight) * totalDurationMs).toLong()
            var currentEnd = (currentStart + durationForWord).coerceAtMost(endTimeMs)

            if (i == words.size - 1) {
                currentEnd = endTimeMs
            }

            // 3. Audio transient refinement: Nudge to nearby onset transient if within +/- 60ms
            if (audioAnalysis != null && audioAnalysis.transientBeatsMs.isNotEmpty()) {
                val nearestTransient = audioAnalysis.transientBeatsMs.minByOrNull {
                    kotlin.math.abs(it - currentStart)
                }
                if (nearestTransient != null && kotlin.math.abs(nearestTransient - currentStart) <= 60) {
                    currentStart = nearestTransient
                }
            }

            val confidence = calculateWordConfidence(word, overallConfidence)

            wordTimestamps.add(
                WordTimestamp(
                    word = word,
                    startTimeMs = currentStart,
                    endTimeMs = max(currentStart + 120L, currentEnd),
                    confidence = confidence
                )
            )
            currentStart = currentEnd
        }

        return wordTimestamps
    }

    /**
     * Estimates proportional duration weight based on word length and language.
     * Arabic vocalization and elongated syllables are accounted for.
     */
    private fun computeWordWeight(word: String): Double {
        val clean = word.filter { it.isLetterOrDigit() }
        if (clean.isEmpty()) return 1.0

        val isArabic = LanguageDetector.isRtl(word)
        return if (isArabic) {
            // Arabic words often have joined consonants and diacritics
            clean.length * 1.3
        } else {
            // English: Vowel count + length
            val vowels = clean.count { it.lowercaseChar() in "aeiouy" }
            clean.length * 1.0 + vowels * 0.4
        }
    }

    private fun calculateWordConfidence(word: String, baseConfidence: Float): Float {
        val length = word.length
        return when {
            length <= 2 -> (baseConfidence * 0.88f).coerceIn(0.6f, 1.0f)
            length > 7 -> (baseConfidence * 0.98f).coerceIn(0.7f, 1.0f)
            else -> baseConfidence
        }
    }

    /**
     * Nudges the timing of a segment and proportionally offsets all child words.
     */
    fun nudgeSegment(segment: LyricsSegment, deltaMs: Long, trackDurationMs: Long): LyricsSegment {
        val newStart = (segment.startTimeMs + deltaMs).coerceIn(0L, trackDurationMs)
        val newEnd = (segment.endTimeMs + deltaMs).coerceIn(newStart + 200L, trackDurationMs)

        val updatedWords = segment.words.map { w ->
            w.copy(
                startTimeMs = (w.startTimeMs + deltaMs).coerceIn(newStart, newEnd),
                endTimeMs = (w.endTimeMs + deltaMs).coerceIn(newStart, newEnd)
            )
        }

        return segment.copy(
            startTimeMs = newStart,
            endTimeMs = newEnd,
            words = updatedWords
        )
    }

    /**
     * Resynchronizes word timestamps when user modifies the line text.
     */
    fun resyncWithNewText(segment: LyricsSegment, newText: String): LyricsSegment {
        val isRtl = LanguageDetector.isRtl(newText)
        val newWords = synchronizeWords(
            rawText = newText,
            startTimeMs = segment.startTimeMs,
            endTimeMs = segment.endTimeMs,
            overallConfidence = 1.0f // User edited text has full confidence
        )
        return segment.copy(
            text = newText,
            isRtl = isRtl,
            words = newWords,
            confidence = 1.0f
        )
    }
}
