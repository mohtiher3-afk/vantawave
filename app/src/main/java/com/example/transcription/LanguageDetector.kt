package com.example.transcription

/**
 * Robust multilingual language detector and text direction analyzer.
 * Identifies Arabic (RTL) vs Latin/English (LTR), script characteristics,
 * and mixed bilingual phrases for accurate typography layout and video rendering.
 */
object LanguageDetector {

    /**
     * Determines if a string predominantly contains Right-to-Left (Arabic) scripts.
     */
    fun isRtl(text: String): Boolean {
        if (text.isBlank()) return false
        var arabicCount = 0
        var latinCount = 0

        for (ch in text) {
            when {
                isArabicChar(ch) -> arabicCount++
                ch.isLetter() -> latinCount++
            }
        }
        return arabicCount > 0 && arabicCount >= latinCount
    }

    /**
     * Detects primary language code ("ar", "en", etc.).
     */
    fun detectLanguage(text: String): String {
        return if (isRtl(text)) "ar" else "en"
    }

    /**
     * Checks if a specific Unicode code point belongs to standard Arabic script blocks.
     */
    fun isArabicChar(ch: Char): Boolean {
        val code = ch.code
        return (code in 0x0600..0x06FF) || // Arabic
                (code in 0x0750..0x077F) || // Arabic Supplement
                (code in 0x08A0..0x08FF) || // Arabic Extended-A
                (code in 0xFB50..0xFDFF) || // Arabic Presentation Forms-A
                (code in 0xFE70..0xFEFF)    // Arabic Presentation Forms-B
    }

    /**
     * Checks if text contains mixed bidirectional content (e.g., Arabic lyric with English title).
     */
    fun isMixedDirection(text: String): Boolean {
        var hasArabic = false
        var hasLatin = false
        for (ch in text) {
            if (isArabicChar(ch)) hasArabic = true
            if (ch in 'a'..'z' || ch in 'A'..'Z') hasLatin = true
            if (hasArabic && hasLatin) return true
        }
        return false
    }
}
