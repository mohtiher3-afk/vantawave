package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThemeAccent
import com.example.ui.theme.VantaBackground
import com.example.ui.theme.VantaTertiary
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private class AmbientParticle(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var baseRadius: Float = 2f,
    var pulsePhase: Float = 0f,
    var pulseSpeed: Float = 1f,
    var alpha: Float = 0.4f,
    var colorType: Int = 0 // 0: primary, 1: secondary, 2: tertiary
)

/**
 * A subtle, slow-moving futuristic ambient particle system that floats behind
 * the main UI components, creating the signature glowing VantaWave cyber aesthetic.
 */
@Composable
fun AmbientParticleBackground(
    modifier: Modifier = Modifier,
    accent: ThemeAccent = ThemeAccent.NEON_PURPLE,
    particleCount: Int = 38
) {
    var timeSeconds by remember { mutableFloatStateOf(0f) }
    var deltaTime by remember { mutableFloatStateOf(0.016f) }

    val particles = remember {
        List(particleCount) {
            AmbientParticle(
                pulsePhase = Random.nextFloat() * 6.28f,
                pulseSpeed = Random.nextFloat() * 0.8f + 0.6f,
                alpha = Random.nextFloat() * 0.35f + 0.15f,
                baseRadius = Random.nextFloat() * 2.0f + 1.2f,
                colorType = when (Random.nextInt(5)) {
                    0, 1 -> 0 // primary
                    2, 3 -> 1 // secondary
                    else -> 2 // tertiary
                }
            )
        }
    }

    var initialized by remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var lastTime = withFrameMillis { it }
        while (true) {
            val currentTime = withFrameMillis { it }
            val dt = ((currentTime - lastTime) / 1000f).coerceIn(0.001f, 0.05f)
            lastTime = currentTime
            deltaTime = dt
            timeSeconds += dt
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // Initialize particle positions on first frame with valid canvas dimensions
        if (!initialized) {
            particles.forEach { p ->
                p.x = Random.nextFloat() * width
                p.y = Random.nextFloat() * height
                p.vx = (Random.nextFloat() - 0.5f) * 14f
                p.vy = -(Random.nextFloat() * 16f + 6f) // Gentle upward float
            }
            initialized = true
        }

        // Draw ambient cosmic void background gradient
        val bgGradient = Brush.radialGradient(
            colors = listOf(
                accent.primary.copy(alpha = 0.12f),
                accent.secondary.copy(alpha = 0.05f),
                VantaBackground
            ),
            center = Offset(width * 0.5f, height * 0.35f),
            radius = width.coerceAtLeast(height) * 0.85f
        )
        drawRect(brush = bgGradient)

        val connectionDistance = 75.dp.toPx()
        val connectionDistanceSq = connectionDistance * connectionDistance

        // Update physics
        for (i in particles.indices) {
            val p = particles[i]
            // Gentle sinusoidal sway
            val sway = sin(timeSeconds * p.pulseSpeed + p.pulsePhase) * 7f
            p.x += (p.vx + sway) * deltaTime
            p.y += p.vy * deltaTime

            // Seamless boundary wrap
            if (p.y < -20f) {
                p.y = height + 20f
                p.x = Random.nextFloat() * width
            }
            if (p.x < -20f) p.x = width + 20f
            if (p.x > width + 20f) p.x = -20f
        }

        // Draw faint constellation connections between nearby drifting particles
        for (i in 0 until particles.size) {
            val p1 = particles[i]
            for (j in i + 1 until particles.size) {
                val p2 = particles[j]
                val dx = p1.x - p2.x
                val dy = p1.y - p2.y
                val distSq = dx * dx + dy * dy
                if (distSq < connectionDistanceSq) {
                    val dist = sqrt(distSq)
                    val lineAlpha = (1f - dist / connectionDistance) * 0.10f
                    drawLine(
                        color = accent.primary.copy(alpha = lineAlpha),
                        start = Offset(p1.x, p1.y),
                        end = Offset(p2.x, p2.y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }

        // Draw glowing particles
        for (p in particles) {
            val color = when (p.colorType) {
                0 -> accent.primary
                1 -> accent.secondary
                else -> VantaTertiary
            }

            // Subtle breathing pulsation
            val pulse = 1f + sin(timeSeconds * 2f * p.pulseSpeed + p.pulsePhase) * 0.25f
            val currentRadius = p.baseRadius.dp.toPx() * pulse
            val currentAlpha = (p.alpha * pulse).coerceIn(0.08f, 0.75f)

            // Outer soft neon halo
            drawCircle(
                color = color.copy(alpha = currentAlpha * 0.28f),
                radius = currentRadius * 3.2f,
                center = Offset(p.x, p.y)
            )

            // Core bright particle
            drawCircle(
                color = color.copy(alpha = currentAlpha),
                radius = currentRadius,
                center = Offset(p.x, p.y)
            )
        }
    }
}
