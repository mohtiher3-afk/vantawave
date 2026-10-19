package com.example.visualizer

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ParticleSimulation(private var targetCount: Int = 120) {
    private val particles = mutableListOf<Particle>()

    fun update(
        width: Float,
        height: Float,
        fftData: FloatArray,
        sensitivity: Float,
        deltaTimeSeconds: Float
    ): List<Particle> {
        val centerX = width / 2f
        val centerY = height / 2f

        val bassEnergy = if (fftData.isNotEmpty()) {
            (fftData.take(6).average().toFloat() * sensitivity).coerceIn(0f, 2.0f)
        } else 0.2f

        // Ensure target count
        while (particles.size < targetCount) {
            particles.add(createParticle(centerX, centerY, bassEnergy))
        }
        while (particles.size > targetCount) {
            particles.removeAt(particles.size - 1)
        }

        // Update each particle
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= deltaTimeSeconds
            if (p.life <= 0f) {
                // Respawn at center with energy burst
                resetParticle(p, centerX, centerY, bassEnergy)
            } else {
                p.x += p.vx * deltaTimeSeconds * 60f
                p.y += p.vy * deltaTimeSeconds * 60f
                p.alpha = (p.life / p.maxLife).coerceIn(0f, 1f)

                // Drag/deceleration
                p.vx *= 0.985f
                p.vy *= 0.985f

                // Re-center if off screen
                if (p.x < 0 || p.x > width || p.y < 0 || p.y > height) {
                    resetParticle(p, centerX, centerY, bassEnergy)
                }
            }
        }

        return particles
    }

    private fun createParticle(centerX: Float, centerY: Float, bassEnergy: Float): Particle {
        val p = Particle()
        resetParticle(p, centerX, centerY, bassEnergy)
        p.life = Random.nextFloat() * p.maxLife
        return p
    }

    private fun resetParticle(p: Particle, centerX: Float, centerY: Float, bassEnergy: Float) {
        val angle = Random.nextDouble(0.0, 2.0 * PI)
        val speed = (Random.nextFloat() * 3.5f + 1.2f) * (1f + bassEnergy * 2.2f)

        p.x = centerX + (Random.nextFloat() * 40f - 20f)
        p.y = centerY + (Random.nextFloat() * 40f - 20f)
        p.vx = (cos(angle) * speed).toFloat()
        p.vy = (sin(angle) * speed).toFloat()
        p.radius = Random.nextFloat() * 4.5f + 2f
        p.maxLife = Random.nextFloat() * 2.5f + 1.2f
        p.life = p.maxLife
        p.alpha = 1f
        p.colorIndex = if (Random.nextBoolean()) 1f else 0f
    }

    fun setCount(newCount: Int) {
        targetCount = newCount.coerceIn(20, 300)
    }
}
