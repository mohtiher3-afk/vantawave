package com.example.effects

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.example.transcription.KaraokeStyle
import com.example.transcription.LyricsSegment
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.abs

/**
 * High-performance karaoke lyrics rendering engine.
 * Renders animated karaoke text with neon glows, outlines, and smooth transitions
 * onto both standard Android Canvas (for hardware MP4 export) and Compose screens.
 * Fully supports Arabic Right-to-Left (RTL), English Left-to-Right (LTR), and mixed phrases.
 */
object KaraokeLyricsRenderer {

    private val textPaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val glowPaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
    }

    private val subTextPaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }

    /**
     * Draws animated karaoke lyrics on the provided Canvas.
     */
    fun drawKaraokeCanvas(
        canvas: Canvas,
        segments: List<LyricsSegment>,
        currentPositionMs: Long,
        width: Float,
        height: Float,
        style: KaraokeStyle = KaraokeStyle.SMOOTH_SLIDE_FADE,
        primaryColor: Int = Color.parseColor("#8B5CF6"),
        secondaryColor: Int = Color.parseColor("#06B6D4"),
        bassEnergy: Float = 0.5f
    ) {
        if (segments.isEmpty()) return

        val activeIndex = segments.indexOfFirst { it.isCurrentlyActive(currentPositionMs) }
        val activeSegment = if (activeIndex >= 0) segments[activeIndex] else null

        val centerY = height * 0.76f
        val baseFontSize = (width * 0.052f).coerceIn(24f, 52f)

        // Bass-driven shake: Subtle random shift based on energy
        val shakeX = (Math.random().toFloat() - 0.5f) * 4f * bassEnergy
        val shakeY = (Math.random().toFloat() - 0.5f) * 4f * bassEnergy
        canvas.translate(shakeX, shakeY)

        if (activeSegment != null) {
            val progressInSegment = ((currentPositionMs - activeSegment.startTimeMs).toFloat() /
                    (activeSegment.endTimeMs - activeSegment.startTimeMs).coerceAtLeast(1L)).coerceIn(0f, 1f)

            when (style) {
                KaraokeStyle.SMOOTH_SLIDE_FADE -> renderSmoothSlideFade(canvas, activeSegment, segments, activeIndex, progressInSegment, width, centerY, baseFontSize, primaryColor, secondaryColor)
                KaraokeStyle.NEON_WORD_HIGHLIGHT -> renderNeonWordHighlight(canvas, activeSegment, currentPositionMs, width, centerY, baseFontSize, primaryColor, secondaryColor, bassEnergy)
                KaraokeStyle.KINETIC_BEAT_PULSE -> renderKineticBeatPulse(canvas, activeSegment, currentPositionMs, width, centerY, baseFontSize, primaryColor, secondaryColor, bassEnergy)
                KaraokeStyle.CINEMATIC_GLOW -> renderCinematicGlow(canvas, activeSegment, currentPositionMs, width, centerY, baseFontSize, primaryColor, secondaryColor, progressInSegment)
                KaraokeStyle.CYBER_NEON -> renderCyberNeon(canvas, activeSegment, currentPositionMs, width, centerY, baseFontSize, primaryColor, secondaryColor, bassEnergy)
                KaraokeStyle.ELEGANT_MINIMAL -> renderElegantMinimal(canvas, activeSegment, currentPositionMs, width, centerY, baseFontSize, primaryColor, secondaryColor)
            }
        } else {
            val nextSegment = segments.firstOrNull { it.startTimeMs > currentPositionMs }
            if (nextSegment != null && (nextSegment.startTimeMs - currentPositionMs) < 3000L) {
                val alpha = (1f - ((nextSegment.startTimeMs - currentPositionMs) / 3000f)).coerceIn(0f, 0.7f)
                subTextPaint.color = Color.WHITE
                subTextPaint.alpha = (alpha * 180).toInt()
                subTextPaint.textSize = baseFontSize * 0.78f
                canvas.drawText(nextSegment.text, width / 2f, centerY, subTextPaint)
            }
        }
        
        canvas.translate(-shakeX, -shakeY)
    }

    private fun renderSmoothSlideFade(
        canvas: Canvas,
        activeSegment: LyricsSegment,
        segments: List<LyricsSegment>,
        activeIndex: Int,
        progress: Float,
        width: Float,
        centerY: Float,
        baseFontSize: Float,
        primaryColor: Int,
        secondaryColor: Int
    ) {
        // Vertical slide offset: gently glides up as the line progresses
        val slideOffset = (1f - progress) * 8f
        val currentY = centerY - slideOffset

        // Previous line fading out above
        if (activeIndex > 0) {
            val prev = segments[activeIndex - 1]
            subTextPaint.textSize = baseFontSize * 0.72f
            subTextPaint.color = Color.LTGRAY
            subTextPaint.alpha = ((1f - progress) * 110).toInt()
            canvas.drawText(prev.text, width / 2f, currentY - baseFontSize * 1.6f, subTextPaint)
        }

        // Draw glowing outer outline
        glowPaint.textSize = baseFontSize
        glowPaint.strokeWidth = 6f
        glowPaint.color = primaryColor
        glowPaint.alpha = (180 + (progress * 75)).toInt().coerceIn(0, 255)
        canvas.drawText(activeSegment.text, width / 2f, currentY, glowPaint)

        // Draw primary text with gradient neon shine
        textPaint.textSize = baseFontSize
        textPaint.color = Color.WHITE
        textPaint.alpha = 255
        canvas.drawText(activeSegment.text, width / 2f, currentY, textPaint)

        // Next line preview below
        if (activeIndex + 1 < segments.size) {
            val next = segments[activeIndex + 1]
            subTextPaint.textSize = baseFontSize * 0.72f
            subTextPaint.color = Color.GRAY
            subTextPaint.alpha = (progress * 130).toInt().coerceIn(40, 130)
            canvas.drawText(next.text, width / 2f, currentY + baseFontSize * 1.6f, subTextPaint)
        }
    }

    private fun renderNeonWordHighlight(
        canvas: Canvas,
        activeSegment: LyricsSegment,
        currentPositionMs: Long,
        width: Float,
        centerY: Float,
        baseFontSize: Float,
        primaryColor: Int,
        secondaryColor: Int,
        bassEnergy: Float
    ) {
        val words = activeSegment.words
        if (words.isEmpty()) {
            textPaint.textSize = baseFontSize
            textPaint.color = Color.WHITE
            canvas.drawText(activeSegment.text, width / 2f, centerY, textPaint)
            return
        }

        val isRtl = activeSegment.isRtl
        textPaint.textSize = baseFontSize

        // Measure total width of line
        val spaceWidth = textPaint.measureText(" ")
        val wordWidths = words.map { textPaint.measureText(it.word) }
        val totalLineWidth = wordWidths.sum() + spaceWidth * (words.size - 1)

        var startX = if (isRtl) {
            (width / 2f) + (totalLineWidth / 2f)
        } else {
            (width / 2f) - (totalLineWidth / 2f)
        }

        words.forEachIndexed { idx, wordTimestamp ->
            val wWidth = wordWidths[idx]
            val isActiveWord = currentPositionMs in wordTimestamp.startTimeMs..wordTimestamp.endTimeMs
            val isPastWord = currentPositionMs > wordTimestamp.endTimeMs

            val wordX = if (isRtl) startX - (wWidth / 2f) else startX + (wWidth / 2f)

            if (isActiveWord) {
                // Active word bounces slightly with beat
                val bounceScale = 1.0f + (bassEnergy * 0.18f)
                val wordProgress = ((currentPositionMs - wordTimestamp.startTimeMs).toFloat() /
                        wordTimestamp.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)

                // Outer neon glow
                glowPaint.textSize = baseFontSize * bounceScale
                glowPaint.strokeWidth = 7f
                glowPaint.color = secondaryColor
                glowPaint.alpha = 240
                canvas.drawText(wordTimestamp.word, wordX, centerY - (bounceScale - 1f) * 10f, glowPaint)

                // Fill bright active color
                textPaint.textSize = baseFontSize * bounceScale
                textPaint.color = Color.WHITE
                textPaint.alpha = 255
                canvas.drawText(wordTimestamp.word, wordX, centerY - (bounceScale - 1f) * 10f, textPaint)
            } else if (isPastWord) {
                // Sung word
                textPaint.textSize = baseFontSize
                textPaint.color = secondaryColor
                textPaint.alpha = 220
                canvas.drawText(wordTimestamp.word, wordX, centerY, textPaint)
            } else {
                // Upcoming word in active line
                textPaint.textSize = baseFontSize
                textPaint.color = Color.WHITE
                textPaint.alpha = 130
                canvas.drawText(wordTimestamp.word, wordX, centerY, textPaint)
            }

            if (isRtl) {
                startX -= (wWidth + spaceWidth)
            } else {
                startX += (wWidth + spaceWidth)
            }
        }
    }

    private fun renderKineticBeatPulse(
        canvas: Canvas,
        activeSegment: LyricsSegment,
        currentPositionMs: Long,
        width: Float,
        centerY: Float,
        baseFontSize: Float,
        primaryColor: Int,
        secondaryColor: Int,
        bassEnergy: Float
    ) {
        val pulseSize = baseFontSize * (1.0f + bassEnergy * 0.22f)

        // Bass drop neon ring flash
        if (bassEnergy > 0.8f) {
            glowPaint.textSize = pulseSize * 1.08f
            glowPaint.strokeWidth = 10f
            glowPaint.color = primaryColor
            glowPaint.alpha = 255
            canvas.drawText(activeSegment.text, width / 2f, centerY, glowPaint)
        }

        // Inner glowing text
        textPaint.textSize = pulseSize
        textPaint.color = Color.WHITE
        textPaint.alpha = 255
        canvas.drawText(activeSegment.text, width / 2f, centerY, textPaint)
    }

    private fun renderCinematicGlow(canvas: Canvas, activeSegment: LyricsSegment, currentPositionMs: Long, width: Float, centerY: Float, baseFontSize: Float, primaryColor: Int, secondaryColor: Int, progress: Float) {
        // Floating motion
        val floatOffset = sin(currentPositionMs / 500.0).toFloat() * 10f
        val currentY = centerY + floatOffset
        textPaint.textSize = baseFontSize
        textPaint.color = Color.WHITE
        textPaint.alpha = (255 * progress).toInt().coerceIn(0, 255)
        textPaint.setShadowLayer(30f, 0f, 0f, primaryColor)
        canvas.drawText(activeSegment.text, width / 2f, currentY, textPaint)
        textPaint.setShadowLayer(0f, 0f, 0f, 0)
    }

    private fun renderCyberNeon(canvas: Canvas, activeSegment: LyricsSegment, currentPositionMs: Long, width: Float, centerY: Float, baseFontSize: Float, primaryColor: Int, secondaryColor: Int, bassEnergy: Float) {
        val words = activeSegment.words
        if (words.isEmpty()) {
            textPaint.textSize = baseFontSize
            textPaint.color = Color.WHITE
            canvas.drawText(activeSegment.text, width / 2f, centerY, textPaint)
            return
        }
        val isRtl = activeSegment.isRtl
        textPaint.textSize = baseFontSize
        val spaceWidth = textPaint.measureText(\" \")
        val wordWidths = words.map { textPaint.measureText(it.word) }
        val totalLineWidth = wordWidths.sum() + spaceWidth * (words.size - 1)
        var startX = if (isRtl) (width / 2f) + (totalLineWidth / 2f) else (width / 2f) - (totalLineWidth / 2f)

        words.forEachIndexed { idx, wordTimestamp ->
            val wWidth = wordWidths[idx]
            val isActive = currentPositionMs in wordTimestamp.startTimeMs..wordTimestamp.endTimeMs
            val wordX = if (isRtl) startX - (wWidth / 2f) else startX + (wWidth / 2f)
            if (isActive) {
                val glitchColor = if (Math.random() > 0.9) secondaryColor else primaryColor
                textPaint.setShadowLayer(15f, 2f, 0f, glitchColor)
                textPaint.color = Color.WHITE
                canvas.drawText(wordTimestamp.word, wordX, centerY, textPaint)
                textPaint.setShadowLayer(0f, 0f, 0f, 0)
            } else {
                textPaint.color = Color.WHITE
                textPaint.alpha = 100
                canvas.drawText(wordTimestamp.word, wordX, centerY, textPaint)
            }
            if (isRtl) startX -= (wWidth + spaceWidth) else startX += (wWidth + spaceWidth)
        }
    }

    private fun renderElegantMinimal(canvas: Canvas, activeSegment: LyricsSegment, currentPositionMs: Long, width: Float, centerY: Float, baseFontSize: Float, primaryColor: Int, secondaryColor: Int) {
        textPaint.textSize = baseFontSize
        textPaint.color = Color.WHITE
        textPaint.alpha = 255
        canvas.drawText(activeSegment.text, width / 2f, centerY, textPaint)
        val textWidth = textPaint.measureText(activeSegment.text)
        val linePaint = Paint().apply {
            style = Style.STROKE
            strokeWidth = 4f
            shader = LinearGradient(
                (width / 2f) - (textWidth / 2f), 0f, 
                (width / 2f) + (textWidth / 2f), 0f, 
                intArrayOf(Color.TRANSPARENT, primaryColor, Color.TRANSPARENT), 
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
            )
        }
        canvas.drawLine((width / 2f) - (textWidth / 2f), centerY + 10f, (width / 2f) + (textWidth / 2f), centerY + 10f, linePaint)
    }
}
