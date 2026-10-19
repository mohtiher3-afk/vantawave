package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-performance on-device audio decoder.
 * Decodes local audio streams via Android MediaCodec into 16-bit PCM samples
 * suitable for local DSP music analysis and on-device speech transcription.
 */
object AudioDecoder {
    private const val TAG = "AudioDecoder"
    private const val TIMEOUT_US = 10_000L

    suspend fun decodeToPcm(
        context: Context,
        audioUri: Uri,
        maxDurationMs: Long = 180_000L
    ): ShortArray = withContext(Dispatchers.IO) {
        val pcmList = mutableListOf<Short>()

        // 1. Check if it's a local file scheme with standard WAV header
        if (audioUri.scheme == "file") {
            val file = File(audioUri.path ?: "")
            if (file.exists() && file.name.endsWith(".wav", ignoreCase = true)) {
                val wavPcm = readWavPcm(file, maxDurationMs)
                if (wavPcm.isNotEmpty()) {
                    return@withContext wavPcm
                }
            }
        }

        // 2. Hardware / Software MediaCodec decoding
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(context, audioUri, null)
            val numTracks = extractor.trackCount
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until numTracks) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex < 0 || audioFormat == null) {
                Log.w(TAG, "No audio track found in $audioUri")
                return@withContext ShortArray(0)
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: ""
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(audioFormat, null, null, 0)
            codec.start()

            val channelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT, 2)
            val bufferInfo = MediaCodec.BufferInfo()
            var isInputEOS = false
            var isOutputEOS = false
            var totalSamplesRead = 0L
            val maxSamples = (maxDurationMs * 44100L / 1000L).toInt()

            while (!isOutputEOS && totalSamplesRead < maxSamples) {
                if (!isInputEOS) {
                    val inputBufIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufIndex >= 0) {
                        val inputBuf = codec.getInputBuffer(inputBufIndex)
                        if (inputBuf != null) {
                            val sampleSize = extractor.readSampleData(inputBuf, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(
                                    inputBufIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                isInputEOS = true
                            } else {
                                codec.queueInputBuffer(
                                    inputBufIndex,
                                    0,
                                    sampleSize,
                                    extractor.sampleTime,
                                    0
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                val outputBufIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputBufIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isOutputEOS = true
                    }

                    val outputBuf = codec.getOutputBuffer(outputBufIndex)
                    if (outputBuf != null && bufferInfo.size > 0) {
                        outputBuf.position(bufferInfo.offset)
                        outputBuf.limit(bufferInfo.offset + bufferInfo.size)
                        val shortBuffer = outputBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()

                        val count = shortBuffer.remaining()
                        if (channelCount > 1) {
                            var j = 0
                            while (j < count - 1) {
                                val left = shortBuffer.get()
                                val right = shortBuffer.get()
                                val mono = ((left.toInt() + right.toInt()) / 2).toShort()
                                pcmList.add(mono)
                                totalSamplesRead++
                                j += 2
                            }
                        } else {
                            while (shortBuffer.hasRemaining()) {
                                pcmList.add(shortBuffer.get())
                                totalSamplesRead++
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outputBufIndex, false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding audio: ${e.message}", e)
        } finally {
            try {
                codec?.stop()
                codec?.release()
                extractor.release()
            } catch (ignored: Exception) {}
        }

        return@withContext pcmList.toShortArray()
    }

    private fun readWavPcm(file: File, maxDurationMs: Long): ShortArray {
        try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(44)
                val read = fis.read(header)
                if (read < 44) return ShortArray(0)

                val sampleRate = ByteBuffer.wrap(header, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int
                val channels = ByteBuffer.wrap(header, 22, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()

                val maxBytes = (maxDurationMs * sampleRate * channels * 2 / 1000).toInt()
                val audioBytes = ByteArray(maxBytes.coerceAtMost(file.length().toInt() - 44))
                val bytesRead = fis.read(audioBytes)
                if (bytesRead <= 0) return ShortArray(0)

                val shortBuf = ByteBuffer.wrap(audioBytes, 0, bytesRead)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()

                val numShorts = shortBuf.remaining()
                if (channels >= 2) {
                    val mono = ShortArray(numShorts / 2)
                    var idx = 0
                    while (shortBuf.hasRemaining() && idx < mono.size) {
                        val left = shortBuf.get()
                        val right = shortBuf.get()
                        mono[idx++] = ((left.toInt() + right.toInt()) / 2).toShort()
                    }
                    return mono
                } else {
                    val pcm = ShortArray(numShorts)
                    shortBuf.get(pcm)
                    return pcm
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading WAV directly: ${e.message}")
            return ShortArray(0)
        }
    }
}
