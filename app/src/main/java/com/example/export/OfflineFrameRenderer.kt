package com.example.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.data.VisualizerMode
import com.example.visualizer.Particle
import com.example.visualizer.VisualizerRenderConfig
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class OfflineFrameRenderer(
    private val width: Int,
    private val height: Int,
    private val config: VisualizerRenderConfig
) {
    private val bgPaint = Paint().apply { isAntiAlias = true }
    private val visualizerPaint = Paint().apply { isAntiAlias = true }
    private val textPaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val primaryIntColor = android.graphics.Color.parseColor(
        "#%02x%02x%02x".format(
            (config.primaryColor.red * 255).toInt(),
            (config.primaryColor.green * 255).toInt(),
            (config.primaryColor.blue * 255).toInt()
        )
    )

    private val secondaryIntColor = android.graphics.Color.parseColor(
        "#%02x%02x%02x".format(
            (config.secondaryColor.red * 255).toInt(),
            (config.secondaryColor.green * 255).toInt(),
            (config.secondaryColor.blue * 255).toInt()
        )
    )

    private val backgroundIntColor = android.graphics.Color.parseColor(
        "#%02x%02x%02x".format(
            (config.backgroundColor.red * 255).toInt(),
            (config.backgroundColor.green * 255).toInt(),
            (config.backgroundColor.blue * 255).toInt()
        )
    )

    fun renderFrame(
        canvas: Canvas,
        fftData: FloatArray,
        waveformData: FloatArray,
        particles: List<Particle>,
        timeSeconds: Float
    ) {
        // 1. Draw solid dark background
        canvas.drawColor(backgroundIntColor)

        // 2. Draw subtle background radial ambient glow
        val center = Pair(width / 2f, height / 2f)
        val ambientShader = RadialGradient(
            center.first, center.second, maxOf(width, height) * 0.6f,
            intArrayOf(
                adjustAlpha(primaryIntColor, (0.15f * config.glowIntensity)),
                adjustAlpha(backgroundIntColor, 0f)
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        bgPaint.shader = ambientShader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        bgPaint.shader = null

        // 3. Render visualizer mode
        when (config.mode) {
            VisualizerMode.SPECTRUM_BARS -> renderSpectrumBars(canvas, fftData)
            VisualizerMode.CIRCULAR_SPECTRUM -> renderCircularSpectrum(canvas, fftData, timeSeconds)
            VisualizerMode.WAVEFORM -> renderWaveform(canvas, waveformData)
            VisualizerMode.RADIAL_RINGS -> renderRadialRings(canvas, fftData, timeSeconds)
            VisualizerMode.PARTICLE_BURST -> renderParticleBurst(canvas, fftData, particles)
        }

        // 4. Render synchronized karaoke lyrics layer
        if (config.showLyrics && config.lyricsSegments.isNotEmpty()) {
            val bassEnergy = if (fftData.isNotEmpty()) {
                (fftData.take(6).average().toFloat() * config.sensitivity).coerceIn(0f, 1.5f)
            } else 0.5f
            com.example.effects.KaraokeLyricsRenderer.drawKaraokeCanvas(
                canvas = canvas,
                segments = config.lyricsSegments,
                currentPositionMs = (timeSeconds * 1000f).toLong(),
                width = width.toFloat(),
                height = height.toFloat(),
                style = config.karaokeStyle,
                primaryColor = primaryIntColor,
                secondaryColor = secondaryIntColor,
                bassEnergy = bassEnergy
            )
        }

        // 5. Render optional song title and artist text overlay
        if (config.showTextOverlay && config.titleText.isNotEmpty()) {
            renderTextOverlay(canvas)
        }
    }

    private fun renderSpectrumBars(canvas: Canvas, fftData: FloatArray) {
        val barCount = config.barCount.coerceIn(16, 96)
        val spacing = (width * 0.05f) / barCount
        val totalSpacing = spacing * (barCount + 1)
        val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(3f)

        val centerY = height * 0.65f
        val maxBarHeight = height * 0.42f * config.sensitivity

        val shader = LinearGradient(
            0f, centerY - maxBarHeight, 0f, centerY,
            intArrayOf(secondaryIntColor, primaryIntColor),
            null,
            Shader.TileMode.CLAMP
        )
        visualizerPaint.shader = shader
        visualizerPaint.style = Paint.Style.FILL

        for (i in 0 until barCount) {
            val fftIdx = (i.toFloat() / barCount * fftData.size).toInt().coerceIn(0, fftData.size - 1)
            val magnitude = (fftData[fftIdx] * config.sensitivity).coerceIn(0.02f, 1.2f)
            val barHeight = (magnitude * maxBarHeight).coerceAtLeast(6f)

            val x = spacing + i * (barWidth + spacing)
            val top = centerY - barHeight

            canvas.drawRoundRect(
                RectF(x, top, x + barWidth, centerY),
                barWidth / 2f, barWidth / 2f, visualizerPaint
            )

            // Draw floating peak indicator
            val peakPaint = Paint().apply {
                color = secondaryIntColor
                style = Paint.Style.FILL
            }
            val peakY = (top - 8f).coerceAtLeast(centerY - maxBarHeight - 12f)
            canvas.drawRoundRect(
                RectF(x, peakY, x + barWidth, peakY + 4f),
                2f, 2f, peakPaint
            )
        }
        visualizerPaint.shader = null
    }

    private fun renderCircularSpectrum(canvas: Canvas, fftData: FloatArray, timeSeconds: Float) {
        val centerX = width / 2f
        val centerY = height / 2f
        val minDim = minOf(width, height)
        val baseRadius = minDim * 0.22f
        val maxBarLength = minDim * 0.24f * config.sensitivity

        val bassEnergy = if (fftData.isNotEmpty()) {
            (fftData.take(6).average().toFloat() * config.sensitivity).coerceIn(0f, 1.5f)
        } else 0.5f

        val pulsedRadius = baseRadius + bassEnergy * (minDim * 0.05f)
        val barCount = config.barCount.coerceIn(24, 96)

        // Draw inner orbit ring
        visualizerPaint.shader = null
        visualizerPaint.color = primaryIntColor
        visualizerPaint.style = Paint.Style.STROKE
        visualizerPaint.strokeWidth = 4f
        canvas.drawCircle(centerX, centerY, pulsedRadius, visualizerPaint)

        val angleStep = (2.0 * PI / barCount)
        val rotationOffset = (timeSeconds * 0.2).toDouble()

        for (i in 0 until barCount) {
            val angle = i * angleStep + rotationOffset
            val normalizedIdx = if (i < barCount / 2) i else (barCount - 1 - i)
            val fftIdx = (normalizedIdx.toFloat() / (barCount / 2f) * fftData.size).toInt().coerceIn(0, fftData.size - 1)
            val magnitude = (fftData[fftIdx] * config.sensitivity).coerceIn(0.04f, 1.4f)
            val barLength = magnitude * maxBarLength

            val startX = (centerX + pulsedRadius * cos(angle)).toFloat()
            val startY = (centerY + pulsedRadius * sin(angle)).toFloat()
            val endX = (centerX + (pulsedRadius + barLength) * cos(angle)).toFloat()
            val endY = (centerY + (pulsedRadius + barLength) * sin(angle)).toFloat()

            visualizerPaint.color = if (i % 2 == 0) primaryIntColor else secondaryIntColor
            visualizerPaint.strokeWidth = 4f
            visualizerPaint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(startX, startY, endX, endY, visualizerPaint)
        }
    }

    private fun renderWaveform(canvas: Canvas, waveformData: FloatArray) {
        val centerY = height * 0.5f
        val maxAmplitude = height * 0.25f * config.sensitivity
        if (waveformData.isEmpty()) return

        val path = Path()
        val count = waveformData.size
        val dx = width.toFloat() / (count - 1).coerceAtLeast(1)

        val firstY = centerY + waveformData[0] * maxAmplitude
        path.moveTo(0f, firstY)

        for (i in 0 until count - 1) {
            val x0 = i * dx
            val y0 = centerY + waveformData[i] * maxAmplitude
            val x1 = (i + 1) * dx
            val y1 = centerY + waveformData[i + 1] * maxAmplitude
            val cx = (x0 + x1) / 2f
            val cy = (y0 + y1) / 2f
            path.quadTo(x0, y0, cx, cy)
        }

        visualizerPaint.style = Paint.Style.STROKE
        visualizerPaint.strokeCap = Paint.Cap.ROUND
        visualizerPaint.shader = LinearGradient(
            0f, 0f, width.toFloat(), 0f,
            intArrayOf(primaryIntColor, secondaryIntColor, primaryIntColor),
            null,
            Shader.TileMode.CLAMP
        )

        // Outer glow pass
        visualizerPaint.strokeWidth = 12f
        visualizerPaint.alpha = (70 * config.glowIntensity).toInt()
        canvas.drawPath(path, visualizerPaint)

        // Core bright line
        visualizerPaint.strokeWidth = 4f
        visualizerPaint.alpha = 255
        canvas.drawPath(path, visualizerPaint)
        visualizerPaint.shader = null
    }

    private fun renderRadialRings(canvas: Canvas, fftData: FloatArray, timeSeconds: Float) {
        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = maxOf(width, height) * 0.45f

        val bassEnergy = if (fftData.isNotEmpty()) {
            (fftData.take(5).average().toFloat() * config.sensitivity).coerceIn(0f, 1.8f)
        } else 0.5f

        visualizerPaint.style = Paint.Style.STROKE
        val ringCount = 6
        for (i in 0 until ringCount) {
            val progress = ((timeSeconds * 0.5f + i.toFloat() / ringCount) % 1.0f)
            val currentRadius = progress * maxRadius * (1f + bassEnergy * 0.2f)
            val alphaFactor = ((1.0f - progress) * (0.2f + bassEnergy * 0.5f) * config.glowIntensity).coerceIn(0f, 1f)
            val strokeW = (4f + (1.0f - progress) * 6f * (1f + bassEnergy)).coerceAtLeast(1.5f)

            visualizerPaint.strokeWidth = strokeW
            val baseColor = if (i % 2 == 0) primaryIntColor else secondaryIntColor
            visualizerPaint.color = adjustAlpha(baseColor, alphaFactor)
            canvas.drawCircle(centerX, centerY, currentRadius, visualizerPaint)
        }

        // Center orb
        val coreRadius = (30f + bassEnergy * 45f).coerceAtLeast(15f)
        visualizerPaint.style = Paint.Style.FILL
        visualizerPaint.color = primaryIntColor
        canvas.drawCircle(centerX, centerY, coreRadius * 0.6f, visualizerPaint)
    }

    private fun renderParticleBurst(canvas: Canvas, fftData: FloatArray, particles: List<Particle>) {
        val bass = if (fftData.isNotEmpty()) {
            (fftData.take(6).average().toFloat() * config.sensitivity).coerceIn(0f, 1.6f)
        } else 0f

        visualizerPaint.style = Paint.Style.FILL
        for (p in particles) {
            val alpha = (p.alpha * config.glowIntensity).coerceIn(0f, 1f)
            val radius = p.radius * (1f + bass * 0.4f)
            val baseColor = if (p.colorIndex > 0.5f) secondaryIntColor else primaryIntColor

            visualizerPaint.color = adjustAlpha(baseColor, alpha)
            canvas.drawCircle(p.x, p.y, radius, visualizerPaint)
        }
    }

    private fun renderTextOverlay(canvas: Canvas) {
        val margin = width * 0.08f
        val textY = height * 0.88f

        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = (width * 0.055f).coerceIn(24f, 54f)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(config.titleText, margin, textY, textPaint)

        if (config.artistText.isNotEmpty()) {
            textPaint.color = android.graphics.Color.LTGRAY
            textPaint.textSize = (width * 0.038f).coerceIn(18f, 36f)
            canvas.drawText(config.artistText, margin, textY + textPaint.textSize * 1.4f, textPaint)
        }
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (android.graphics.Color.alpha(color) * factor.coerceIn(0f, 1f)).toInt()
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        return android.graphics.Color.argb(alpha, red, green, blue)
    }
}
