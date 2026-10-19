package com.example.audio

import kotlin.math.abs
import kotlin.math.sqrt

data class AudioAnalysisResult(
    val estimatedBpm: Double,
    val beatIntervalMs: Long,
    val averageEnergy: Float,
    val vocalRegions: List<Pair<Long, Long>>, // (startMs, endMs)
    val transientBeatsMs: List<Long>,
    val normalizationFactor: Float // Added normalization factor
)

object MusicAnalyzer {

    fun analyzePcmAudio(pcmShorts: ShortArray, sampleRate: Int = 44100): AudioAnalysisResult {
        if (pcmShorts.isEmpty()) {
            return AudioAnalysisResult(120.0, 500L, 0.5f, emptyList(), emptyList(), 1.0f)
        }

        // 1. Compute frame-based energy envelopes (10ms windows = 441 samples at 44.1kHz)
        val windowSize = sampleRate / 100 // 10ms
        val frameCount = pcmShorts.size / windowSize
        val energyEnvelope = FloatArray(frameCount)
        var totalEnergy = 0.0
        
        // Track peak amplitude for normalization
        var peakAmplitude = 0.0f
        
        for (i in 0 until frameCount) {
            var sumSquare = 0.0
            val start = i * windowSize
            
            // Track local peak for this frame
            var framePeak = 0.0f
            
            for (j in 0 until windowSize) {
                val sampleValue = pcmShorts[start + j].toFloat()
                val normalizedSample = sampleValue / 32768.0f
                sumSquare += (normalizedSample * normalizedSample).toDouble()
                
                val absSample = abs(normalizedSample)
                if (absSample > framePeak) framePeak = absSample
            }
            
            if (framePeak > peakAmplitude) peakAmplitude = framePeak
            
            val rms = sqrt(sumSquare / windowSize).toFloat()
            energyEnvelope[i] = rms
            totalEnergy += rms
        }

        // Normalization Factor: Aim for 0.8 peak amplitude
        val normalizationFactor = if (peakAmplitude > 0.01f) (0.8f / peakAmplitude) else 1.0f

        val avgEnergy = (totalEnergy / frameCount.coerceAtLeast(1)).toFloat()

        // 2. Transient Beat Detection (Derivative / onset peaks above local threshold)
        val beatTimestamps = mutableListOf<Long>()
        val threshold = (avgEnergy * 1.35f).coerceAtLeast(0.08f)

        for (i in 1 until frameCount - 1) {
            val prev = energyEnvelope[i - 1]
            val curr = energyEnvelope[i]
            val next = energyEnvelope[i + 1]

            if (curr > threshold && curr > prev && curr > next) {
                val timeMs = (i.toLong() * 10L)
                if (beatTimestamps.isEmpty() || (timeMs - beatTimestamps.last() > 220)) { // Max ~270 BPM
                    beatTimestamps.add(timeMs)
                }
            }
        }

        // 3. Estimate BPM from beat intervals
        var estimatedBpm = 120.0
        if (beatTimestamps.size >= 4) {
            val intervals = mutableListOf<Long>()
            for (i in 1 until beatTimestamps.size) {
                intervals.add(beatTimestamps[i] - beatTimestamps[i - 1])
            }
            val medianIntervalMs = intervals.sorted()[intervals.size / 2].coerceIn(300, 1000)
            estimatedBpm = (60000.0 / medianIntervalMs).coerceIn(60.0, 180.0)
        }
        val beatIntervalMs = (60000.0 / estimatedBpm).toLong()

        // 4. Voice Activity Detection (VAD) / Vocal regions estimation
        val vocalRegions = mutableListOf<Pair<Long, Long>>()
        var inVocal = false
        var vocalStartMs = 0L
        val vocalThreshold = (avgEnergy * 0.75f).coerceAtLeast(0.04f)

        for (i in 0 until frameCount) {
            val curr = energyEnvelope[i]
            val timeMs = i.toLong() * 10L

            if (curr >= vocalThreshold && !inVocal) {
                inVocal = true
                vocalStartMs = timeMs
            } else if (curr < vocalThreshold && inVocal) {
                // Must be at least 400ms to be considered a vocal region
                if (timeMs - vocalStartMs >= 400) {
                    vocalRegions.add(Pair(vocalStartMs, timeMs))
                }
                inVocal = false
            }
        }
        if (inVocal) {
            vocalRegions.add(Pair(vocalStartMs, frameCount.toLong() * 10L))
        }

        return AudioAnalysisResult(
            estimatedBpm = estimatedBpm,
            beatIntervalMs = beatIntervalMs,
            averageEnergy = avgEnergy,
            vocalRegions = vocalRegions,
            transientBeatsMs = beatTimestamps,
            normalizationFactor = normalizationFactor
        )
    }
}
