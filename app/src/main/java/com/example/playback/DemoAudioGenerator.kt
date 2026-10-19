package com.example.playback

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.sin

/**
 * Generates synthetic electronic / synthwave WAV audio files for demo and instant offline playback.
 * Produces authentic 16-bit 44.1kHz stereo audio with melodic basslines, chords, and rhythmic beats.
 */
object DemoAudioGenerator {
    private const val TAG = "DemoAudioGenerator"
    private const val SAMPLE_RATE = 44100
    private const val CHANNELS = 2
    private const val BITS_PER_SAMPLE = 16

    data class DemoTrackInfo(
        val filename: String,
        val title: String,
        val artist: String,
        val bpm: Double,
        val baseFreq: Double,
        val durationSeconds: Int = 30
    )

    val DEMO_TRACKS = listOf(
        DemoTrackInfo("vantawave_neon_horizon.wav", "Neon Horizon", "VantaWave Original", 120.0, 110.0, 30),
        DemoTrackInfo("vantawave_cyber_pulse.wav", "Cyber Pulse", "Synthetix", 128.0, 130.81, 30),
        DemoTrackInfo("vantawave_midnight_echo.wav", "Midnight Echo", "VantaWave Studio", 115.0, 98.0, 30)
    )

    fun ensureDemoTracksExist(context: Context): List<File> {
        val files = mutableListOf<File>()
        val dir = File(context.filesDir, "demo_audio").apply { if (!exists()) mkdirs() }

        for (track in DEMO_TRACKS) {
            val file = File(dir, track.filename)
            if (!file.exists() || file.length() < 1000) {
                try {
                    generateSynthwaveWav(file, track)
                    Log.d(TAG, "Generated demo track: ${file.name} (${file.length()} bytes)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed generating demo track: ${track.filename}", e)
                }
            }
            if (file.exists() && file.length() > 0) {
                files.add(file)
            }
        }
        return files
    }

    private fun generateSynthwaveWav(outFile: File, track: DemoTrackInfo) {
        val totalSamples = SAMPLE_RATE * track.durationSeconds
        val bytesPerSample = CHANNELS * (BITS_PER_SAMPLE / 8)
        val dataSize = totalSamples * bytesPerSample

        val fos = FileOutputStream(outFile)
        // Write placeholder WAV header (44 bytes)
        fos.write(ByteArray(44))

        val buffer = ByteArray(4096)
        var bufferIdx = 0

        val secondsPerBeat = 60.0 / track.bpm
        val samplesPerBeat = (SAMPLE_RATE * secondsPerBeat).toInt()

        // Pentatonic scale multipliers
        val scale = doubleArrayOf(1.0, 1.1892, 1.3348, 1.4983, 1.7818, 2.0)

        for (sample in 0 until totalSamples) {
            val t = sample.toDouble() / SAMPLE_RATE
            val beatPos = (sample % samplesPerBeat).toDouble() / samplesPerBeat
            val currentBeat = (sample / samplesPerBeat) % 16

            // 1. Kick drum: sharp low pitch envelope on beats 0, 4, 8, 12 (four on the floor)
            var kick = 0.0
            if (currentBeat % 4 == 0) {
                val kickEnv = Math.exp(-beatPos * 14.0)
                val kickFreq = 150.0 * Math.exp(-beatPos * 25.0) + 45.0
                kick = sin(2.0 * PI * kickFreq * beatPos * secondsPerBeat) * kickEnv * 0.7
            }

            // 2. Snare / Clap: noise burst on beats 2, 6, 10, 14
            var snare = 0.0
            if (currentBeat % 4 == 2) {
                val snareEnv = Math.exp(-beatPos * 10.0)
                val noise = (Math.random() * 2.0 - 1.0)
                snare = noise * snareEnv * 0.35
            }

            // 3. Synth Bass: 16th-note rhythmic bassline
            val sixteenthPos = (sample % (samplesPerBeat / 4)).toDouble() / (samplesPerBeat / 4)
            val bassEnv = Math.exp(-sixteenthPos * 6.0)
            val noteIdx = (currentBeat + (sample / (samplesPerBeat / 4))) % scale.size
            val bassFreq = track.baseFreq * scale[noteIdx]
            val bass = sin(2.0 * PI * bassFreq * t) * bassEnv * 0.45 +
                    0.2 * sin(4.0 * PI * bassFreq * t) * bassEnv

            // 4. Arpeggiator synth lead (higher octave)
            val arpFreq = track.baseFreq * 4.0 * scale[(currentBeat * 2 + (sample / (samplesPerBeat / 2))) % scale.size]
            val arp = sin(2.0 * PI * arpFreq * t) * 0.25 * (0.6 + 0.4 * sin(2.0 * PI * 4.0 * t))

            // Mix channels
            var left = kick + snare * 0.9 + bass * 0.8 + arp * 0.7
            var right = kick + snare * 0.9 + bass * 0.8 + arp * 0.7

            // Soft clamp between -1.0 and 1.0
            left = left.coerceIn(-1.0, 1.0)
            right = right.coerceIn(-1.0, 1.0)

            val shortLeft = (left * 32767).toInt().toShort()
            val shortRight = (right * 32767).toInt().toShort()

            buffer[bufferIdx++] = (shortLeft.toInt() and 0xFF).toByte()
            buffer[bufferIdx++] = ((shortLeft.toInt() shr 8) and 0xFF).toByte()
            buffer[bufferIdx++] = (shortRight.toInt() and 0xFF).toByte()
            buffer[bufferIdx++] = ((shortRight.toInt() shr 8) and 0xFF).toByte()

            if (bufferIdx >= buffer.size) {
                fos.write(buffer, 0, bufferIdx)
                bufferIdx = 0
            }
        }

        if (bufferIdx > 0) {
            fos.write(buffer, 0, bufferIdx)
        }
        fos.flush()
        fos.close()

        // Overwrite header with true sizes
        val raf = RandomAccessFile(outFile, "rw")
        raf.seek(0)
        writeWavHeader(raf, totalSamples, dataSize)
        raf.close()
    }

    private fun writeWavHeader(raf: RandomAccessFile, totalSamples: Int, dataSize: Int) {
        val totalDataLen = dataSize + 36
        val byteRate = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8)

        raf.writeBytes("RIFF")
        raf.writeInt(Integer.reverseBytes(totalDataLen))
        raf.writeBytes("WAVE")
        raf.writeBytes("fmt ")
        raf.writeInt(Integer.reverseBytes(16)) // PCM subchunk size
        raf.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // AudioFormat 1 = PCM
        raf.writeShort(java.lang.Short.reverseBytes(CHANNELS.toShort()).toInt())
        raf.writeInt(Integer.reverseBytes(SAMPLE_RATE))
        raf.writeInt(Integer.reverseBytes(byteRate))
        raf.writeShort(java.lang.Short.reverseBytes((CHANNELS * (BITS_PER_SAMPLE / 8)).toShort()).toInt())
        raf.writeShort(java.lang.Short.reverseBytes(BITS_PER_SAMPLE.toShort()).toInt())
        raf.writeBytes("data")
        raf.writeInt(Integer.reverseBytes(dataSize))
    }
}
