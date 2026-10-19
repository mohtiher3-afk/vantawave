package com.example.visualizer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.example.data.VisualizerMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object VisualizerDrawers {

    fun drawVisualizer(
        scope: DrawScope,
        config: VisualizerRenderConfig,
        fftData: FloatArray,
        waveformData: FloatArray,
        particles: List<Particle>,
        timeSeconds: Float
    ) {
        val width = scope.size.width
        val height = scope.size.height

        // 1. دمج لون المزاج (Mood Color) مع الألوان الأساسية إذا وجد
        val activePrimary = if (config.currentMoodColor != null) {
            config.primaryColor.lerp(config.currentMoodColor!!, 0.6f)
        } else config.primaryColor

        val activeSecondary = if (config.currentMoodColor != null) {
            config.secondaryColor.lerp(config.currentMoodColor!!, 0.4f)
        } else config.secondaryColor

        // نسخة من التكوين تستخدم الألوان النشطة بدلاً من الأساسية لضمان تناسق كل الطبقات
        val activeConfig = config.copy(
            primaryColor = activePrimary,
            secondaryColor = activeSecondary
        )

        // 2. حساب طاقة البيس (Bass Energy)
        val bass = if (fftData.isNotEmpty()) {
            (fftData.take(6).average().toFloat() * activeConfig.sensitivity).coerceIn(0f, 2.0f)
        } else 0f

        // 3. تنفيذ "الكاميرا الديناميكية" (Dynamic Camera)
        val cameraScale = if (activeConfig.cameraShakeEnabled) {
            1.0f + (bass * activeConfig.cameraShakeStrength) 
        } else 1.0f

        scope.drawContext.canvas.save()
        
        scope.drawContext.canvas.translate(width / 2f, height / 2f)
        scope.drawContext.canvas.scale(cameraScale, cameraScale)
        scope.drawContext.canvas.translate(-width / 2f, -height / 2f)

        // --- بداية عملية الرسم الفعلية باستخدام activeConfig ---

        // أ. الخلفية التفاعلية
        drawBackground(scope, activeConfig, fftData)

        // ب. رسم الطبقات
        for (layer in activeConfig.layers) {
            when (layer) {
                is VisualEffect.SpectrumBars -> drawSpectrumBarsLayer(scope, activeConfig, layer, fftData)
                is VisualEffect.CircularSpectrum -> drawCircularSpectrumLayer(scope, activeConfig, layer, fftData, timeSeconds)
                is VisualEffect.NeonWave -> drawNeonWaveLayer(scope, activeConfig, layer, waveformData, timeSeconds)
                is VisualEffect.RadialRings -> drawRadialRingsLayer(scope, activeConfig, layer, fftData, timeSeconds)
                is VisualEffect.ParticleBurst -> drawParticleBurstLayer(scope, activeConfig, layer, fftData, particles)
                is VisualEffect.CyberGrid -> drawCyberGridLayer(scope, activeConfig, layer, fftData, timeSeconds)
                is VisualEffect.NeonLiquidWave -> drawNeonLiquidWaveLayer(scope, activeConfig, layer, waveformData, timeSeconds)
            }
        }

        // ج. تأثير الـ GLITCH التفاعلي
        if (bass > 1.3f && activeConfig.glitchIntensity > 0f) {
            applyGlitchEffect(scope, activeConfig, bass)
        }

        // د. طبقة الكلمات
        if (activeConfig.showLyrics && activeConfig.lyricsSegments.isNotEmpty()) {
            val primaryInt = android.graphics.Color.argb(
                (activeConfig.primaryColor.alpha * 255).toInt(),
                (activeConfig.primaryColor.red * 255).toInt(),
                (activeConfig.primaryColor.green * 255).toInt(),
                (activeConfig.primaryColor.blue * 255).toInt()
            )
            val secondaryInt = android.graphics.Color.argb(
                (activeConfig.secondaryColor.alpha * 255).toInt(),
                (activeConfig.secondaryColor.red * 255).toInt(),
                (activeConfig.secondaryColor.green * 255).toInt(),
                (activeConfig.secondaryColor.blue * 255).toInt()
            )

            val nativeCanvas = scope.drawContext.canvas.nativeCanvas
            com.example.effects.KaraokeLyricsRenderer.drawKaraokeCanvas(
                canvas = nativeCanvas,
                segments = activeConfig.lyricsSegments,
                currentPositionMs = (timeSeconds * 1000f).toLong(),
                width = width,
                height = height,
                style = activeConfig.karaokeStyle,
                primaryColor = primaryInt,
                secondaryColor = secondaryInt,
                bassEnergy = bass
            )
        }

        scope.drawContext.canvas.restore()
    }

    private fun applyGlitchEffect(scope: DrawScope, config: VisualizerRenderConfig, bass: Float) {
        val width = scope.size.width
        val height = scope.size.height
        
        val offsetX = (Math.random().toFloat() - 0.5f) * 40f * config.glitchIntensity * bass
        val offsetY = (Math.random().toFloat() - 0.5f) * 20f * config.glitchIntensity * bass

        scope.drawContext.canvas.save()
        scope.drawContext.canvas.translate(offsetX, offsetY)
        
        scope.drawRect(
            color = config.primaryColor.copy(alpha = 0.15f * config.glitchIntensity),
            blendMode = androidx.compose.ui.graphics.BlendMode.Difference
        )
        
        scope.drawContext.canvas.restore()
    }

    // 1. Spectrum Bars: Vertical columns with gradient, rounded tops, and floating peak caps
    private fun drawSpectrumBarsLayer(
        scope: DrawScope,
        config: VisualizerRenderConfig,
        fftData: FloatArray
    ) {
        val width = scope.size.width
        val height = scope.size.height
        val barCount = config.barCount.coerceIn(16, 96)
        val spacing = (width * 0.05f) / barCount
        val totalSpacing = spacing * (barCount + 1)
        val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(3f)

        val centerY = height * 0.65f
        val maxBarHeight = height * 0.45f * config.sensitivity

        val gradient = Brush.verticalGradient(
            colors = listOf(
                config.secondaryColor,
                config.primaryColor,
                config.primaryColor.copy(alpha = 0.3f)
            ),
            startY = centerY - maxBarHeight,
            endY = centerY + 10f
        )

        for (i in 0 until barCount) {
            val fftIdx = (i.toFloat() / barCount * fftData.size).toInt().coerceIn(0, fftData.size - 1)
            val magnitude = (fftData[fftIdx] * config.sensitivity).coerceIn(0.02f, 1.2f)
            val barHeight = (magnitude * maxBarHeight).coerceAtLeast(6f)

            val x = spacing + i * (barWidth + spacing)
            val top = centerY - barHeight

            // Draw glowing halo behind bar
            if (config.glowIntensity > 0.3f) {
                scope.drawRoundRect(
                    color = config.primaryColor.copy(alpha = 0.25f * config.glowIntensity),
                    topLeft = Offset(x - 2f, top - 2f),
                    size = Size(barWidth + 4f, barHeight + 4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }

            // Draw primary bar
            scope.drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
            )

            // Draw floating peak indicator
            val peakY = (top - 8f).coerceAtLeast(centerY - maxBarHeight - 14f)
            scope.drawRoundRect(
                color = config.secondaryColor,
                topLeft = Offset(x, peakY),
                size = Size(barWidth, 4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
            )
        }
    }

    // 2. Circular Spectrum: Radial bars radiating outward with pulsing inner orbital core
    private fun drawCircularSpectrumLayer(
        scope: DrawScope,
        config: VisualizerRenderConfig,
        fftData: FloatArray,
        timeSeconds: Float
    ) {
        val center = Offset(scope.size.width / 2f, scope.size.height / 2f)
        val minDim = minOf(scope.size.width, scope.size.height)
        val baseRadius = minDim * 0.22f
        val maxBarLength = minDim * 0.24f * config.sensitivity

        // Bass energy drives center circle pulse
        val bassEnergy = if (fftData.isNotEmpty()) {
            (fftData.take(6).average().toFloat() * config.sensitivity).coerceIn(0f, 1.5f)
        } else 0f

        val pulsedRadius = baseRadius + bassEnergy * (minDim * 0.05f)
        val barCount = config.barCount.coerceIn(24, 96)

        // Draw glowing inner core
        val innerGlowBrush = Brush.radialGradient(
            colors = listOf(
                config.primaryColor.copy(alpha = 0.4f * config.glowIntensity),
                config.secondaryColor.copy(alpha = 0.15f * config.glowIntensity),
                Color.Transparent
            ),
            center = center,
            radius = pulsedRadius * 1.5f
        )
        scope.drawCircle(brush = innerGlowBrush, radius = pulsedRadius * 1.5f, center = center)

        // Draw inner orbit ring
        scope.drawCircle(
            color = config.primaryColor.copy(alpha = 0.8f),
            radius = pulsedRadius,
            center = center,
            style = Stroke(width = 3.5f)
        )

        val angleStep = (2.0 * PI / barCount)
        val rotationOffset = (timeSeconds * 0.2).toDouble()

        for (i in 0 until barCount) {
            val angle = i * angleStep + rotationOffset
            // Mirror FFT across circle
            val normalizedIdx = if (i < barCount / 2) i else (barCount - 1 - i)
            val fftIdx = (normalizedIdx.toFloat() / (barCount / 2f) * fftData.size).toInt().coerceIn(0, fftData.size - 1)
            val magnitude = (fftData[fftIdx] * config.sensitivity).coerceIn(0.04f, 1.4f)
            val barLength = magnitude * maxBarLength

            val startX = (center.x + pulsedRadius * cos(angle)).toFloat()
            val startY = (center.y + pulsedRadius * sin(angle)).toFloat()
            val endX = (center.x + (pulsedRadius + barLength) * cos(angle)).toFloat()
            val endY = (center.y + (pulsedRadius + barLength) * sin(angle)).toFloat()

            val barColor = if (i % 2 == 0) config.primaryColor else config.secondaryColor

            // Outer glow line
            if (config.glowIntensity > 0.4f) {
                scope.drawLine(
                    color = barColor.copy(alpha = 0.35f * config.glowIntensity),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )
            }

            scope.drawLine(
                color = barColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }

    // 3. Waveform: Smooth fluid oscillating Bezier audio wave with multi-pass neon bloom
    private fun drawNeonWaveLayer(
        scope: DrawScope,
        config: VisualizerRenderConfig,
        waveformData: FloatArray,
        timeSeconds: Float
    ) {
        val width = scope.size.width
        val height = scope.size.height
        val centerY = height * 0.5f
        val maxAmplitude = height * 0.25f * config.sensitivity

        if (waveformData.isEmpty()) return

        val path = Path()
        val glowPath = Path()

        val points = mutableListOf<Offset>()
        val count = waveformData.size
        val dx = width / (count - 1).coerceAtLeast(1)

        for (i in 0 until count) {
            val x = i * dx
            val amp = waveformData[i] * maxAmplitude
            val y = centerY + amp
            points.add(Offset(x, y))
        }

        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            glowPath.moveTo(points[0].x, points[0].y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx = (p0.x + p1.x) / 2f
                val cy = (p0.y + p1.y) / 2f
                path.quadraticTo(p0.x, p0.y, cx, cy)
                glowPath.quadraticTo(p0.x, p0.y, cx, cy)
            }
            path.lineTo(points.last().x, points.last().y)
            glowPath.lineTo(points.last().x, points.last().y)
        }

        val brush = Brush.horizontalGradient(
            colors = listOf(
                config.primaryColor,
                config.secondaryColor,
                config.primaryColor
            )
        )

        // Neon Glow pass 1 (broad)
        scope.drawPath(
            path = glowPath,
            color = config.primaryColor.copy(alpha = 0.2f * config.glowIntensity),
            style = Stroke(width = 16f, cap = StrokeCap.Round)
        )

        // Neon Glow pass 2 (medium)
        scope.drawPath(
            path = glowPath,
            color = config.secondaryColor.copy(alpha = 0.45f * config.glowIntensity),
            style = Stroke(width = 8f, cap = StrokeCap.Round)
        )

        // Core bright wave line
        scope.drawPath(
            path = path,
            brush = brush,
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )
    }

    // 4. Radial Rings: Concentric shockwaves expanding with bass energy
    private fun drawRadialRingsLayer(
        scope: DrawScope,
        config: VisualizerRenderConfig,
        fftData: FloatArray,
        timeSeconds: Float
    ) {
        val center = Offset(scope.size.width / 2f, scope.size.height / 2f)
        val maxRadius = maxOf(scope.size.width, scope.size.height) * 0.45f

        val bassEnergy = if (fftData.isNotEmpty()) {
            (fftData.take(5).average().toFloat() * config.sensitivity).coerceIn(0f, 1.8f)
        } else 0.5f

        val ringCount = 6
        for (i in 0 until ringCount) {
            val progress = ((timeSeconds * 0.5f + i.toFloat() / ringCount) % 1.0f)
            val currentRadius = progress * maxRadius * (1f + bassEnergy * 0.2f)
            val alpha = ((1.0f - progress) * (0.2f + bassEnergy * 0.5f) * config.glowIntensity).coerceIn(0f, 1f)
            val strokeW = (4f + (1.0f - progress) * 6f * (1f + bassEnergy)).coerceAtLeast(1.5f)

            val color = if (i % 2 == 0) config.primaryColor else config.secondaryColor

            scope.drawCircle(
                color = color.copy(alpha = alpha),
                radius = currentRadius,
                center = center,
                style = Stroke(width = strokeW)
            )
        }

        // Center pulsating bass orb
        val coreRadius = (30f + bassEnergy * 45f).coerceAtLeast(15f)
        val coreBrush = Brush.radialGradient(
            colors = listOf(
                config.secondaryColor,
                config.primaryColor.copy(alpha = 0.8f),
                Color.Transparent
            ),
            center = center,
            radius = coreRadius * 1.5f
        )
        scope.drawCircle(brush = coreBrush, radius = coreRadius * 1.5f, center = center)
        scope.drawCircle(color = config.primaryColor, radius = coreRadius * 0.6f, center = center)
    }

    // 5. Particle Burst: High-energy kinetic particles exploding with transients
    private fun drawParticleBurstLayer(
        scope: DrawScope,
        config: VisualizerRenderConfig,
        fftData: FloatArray,
        particles: List<Particle>
    ) {
        val bass = if (fftData.isNotEmpty()) {
            (fftData.take(6).average().toFloat() * config.sensitivity).coerceIn(0f, 1.6f)
        } else 0f

        for (p in particles) {
            val alpha = (p.alpha * config.glowIntensity).coerceIn(0f, 1f)
            val radius = p.radius * (1f + bass * 0.4f)
            val color = if (p.colorIndex > 0.5f) config.secondaryColor else config.primaryColor

            // Particle halo
            scope.drawCircle(
                color = color.copy(alpha = alpha * 0.35f),
                radius = radius * 2.2f,
                center = Offset(p.x, p.y)
            )

            // Particle core
            scope.drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = Offset(p.x, p.y)
            )
        }

    private fun drawCyberGridLayer(scope: DrawScope, config: VisualizerRenderConfig, layer: VisualEffect.CyberGrid, fftData: FloatArray, timeSeconds: Float) {
        val width = scope.size.width
        val height = scope.size.height
        val centerX = width / 2f
        val horizonY = height * 0.5f
        val offset = (timeSeconds * 50f * layer.speed) % 100f
        val bass = if (fftData.isNotEmpty()) (fftData.take(5).average().toFloat() * 100f) else 0f

        val gridPath = Path()
        for (x in -width..width step 100) {
            val startX = centerX + x
            val endX = centerX + x * 2f
            gridPath.moveTo(startX, height)
            gridPath.lineTo(endX, horizonY)
        }
        for (y in 0..height step 50) {
            val currentY = horizonY + (y * 0.5f) + offset
            if (currentY < height) {
                gridPath.moveTo(0f, currentY)
                gridPath.lineTo(width, currentY)
            }
        }
        scope.drawPath(path = gridPath, color = config.primaryColor.copy(alpha = 0.4f + bass * 0.001f), style = Stroke(width = 2f))
    }

    private fun drawNeonLiquidWaveLayer(scope: DrawScope, config: VisualizerRenderConfig, layer: VisualEffect.NeonLiquidWave, waveformData: FloatArray, timeSeconds: Float) {
        val width = scope.size.width
        val height = scope.size.height
        val centerY = height * 0.5f
        if (waveformData.isEmpty()) return
        
        val colors = listOf(config.primaryColor, config.secondaryColor)
        for (i in 0..2) {
            val offset = i * 0.3f
            val path = Path()
            val dx = width / (waveformData.size - 1).coerceAtLeast(1)
            for (j in waveformData.indices) {
                val x = j * dx
                val y = centerY + (waveformData[j] * height * 0.2f * layer.intensity) + (sin(timeSeconds + j * 0.1f + offset) * 20f)
                if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            scope.drawPath(path = path, color = colors[i % 2].copy(alpha = 0.4f - i * 0.1f), style = Stroke(width = 8f, cap = StrokeCap.Round))
        }
    }

    private fun drawBackground(
        scope: DrawScope,
        config: VisualizerRenderConfig,
        fftData: FloatArray
    ) {
        val bg = config.backgroundConfig
        val bass = if (fftData.isNotEmpty()) {
            (fftData.take(6).average().toFloat()).coerceIn(0f, 1.5f)
        } else 0f

        when (bg.type) {
            BackgroundType.COLOR -> drawReactiveColor(scope, config, bass)
            BackgroundType.IMAGE -> drawReactiveImage(scope, config, bass)
            BackgroundType.VIDEO -> drawReactiveVideo(scope, config, bass)
            BackgroundType.ANIMATED_IMAGE -> drawReactiveAnimatedImage(scope, config, bass)
        }
    }

    private fun drawReactiveColor(scope: DrawScope, config: VisualizerRenderConfig, bass: Float) {
        // لون يتفاعل بنعومة مع البيس
        val color = if (bass > 0.8f) {
            config.primaryColor.copy(alpha = 0.3f)
        } else {
            config.backgroundColor
        }
        scope.drawRect(color = color)
    }

    private fun drawReactiveImage(scope: DrawScope, config: VisualizerRenderConfig, bass: Float) {
        val bg = config.backgroundConfig
        val reaction = bg.reactionType
        val strength = bg.reactionStrength

        if (reaction == BackgroundConfig.ReactionType.SCALE) {
            val scale = 1.0f + (bass * 0.05f * strength)
            scope.drawRect(
                color = config.backgroundColor, 
                alpha = bg.opacity
            )
        } else if (reaction == BackgroundConfig.ReactionType.BRIGHTNESS) {
            val brightness = 1.0f + (bass * 0.2f * strength)
            scope.drawRect(
                color = config.backgroundColor.copy(alpha = bg.opacity), 
            )
        } else {
            scope.drawRect(color = config.backgroundColor, alpha = bg.opacity)
        }
    }

    private fun drawReactiveVideo(scope: DrawScope, config: VisualizerRenderConfig, bass: Float) {
        val glowAlpha = (bass * 0.2f * config.backgroundConfig.reactionStrength).coerceIn(0f, 0.5f)
        scope.drawRect(
            color = config.primaryColor.copy(alpha = glowAlpha),
            blendMode = androidx.compose.ui.graphics.BlendMode.Screen
        )
    }

    private fun drawReactiveAnimatedImage(scope: DrawScope, config: VisualizerRenderConfig, bass: Float) {
        val pulse = 1.0f + (bass * 0.03f * config.backgroundConfig.reactionStrength)
        scope.drawRect(color = config.backgroundColor, alpha = config.backgroundConfig.opacity)
    }
}
