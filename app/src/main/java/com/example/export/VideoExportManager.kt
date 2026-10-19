package com.example.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import com.example.data.ExportRecordEntity
import com.example.data.VantaWaveDatabase
import com.example.visualizer.ParticleSimulation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

class VideoExportManager(private val context: Context) {
    private val TAG = "VideoExportManager"
    private val exportRecordDao = VantaWaveDatabase.getInstance(context).exportRecordDao()

    private val _progress = MutableStateFlow(ExportProgress())
    val progress: StateFlow<ExportProgress> = _progress.asStateFlow()

    private var activeJob: Job? = null
    @Volatile
    private var isCancelled = false

    fun cancelExport() {
        isCancelled = true
        _progress.value = _progress.value.copy(
            stage = ExportStage.CANCELLED,
            message = "Export cancelled by user."
        )
    }

    suspend fun exportVideo(settings: VideoExportSettings): Result<File> = withContext(Dispatchers.IO) {
        isCancelled = false
        _progress.value = ExportProgress(
            stage = ExportStage.PREPARING,
            progress = 0f,
            message = "Initializing video export engine..."
        )

        val outputDir = getOutputDir()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val sanitizedTitle = settings.title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(24)
        val outputFile = File(outputDir, "VantaWave_${sanitizedTitle}_$timestamp.mp4")
        val tempVideoFile = File(context.cacheDir, "temp_video_$timestamp.h264")
        val tempAudioFile = File(context.cacheDir, "temp_audio_$timestamp.aac")

        try {
            val width = (settings.width / 16) * 16 // Must be divisible by 16 for AVC encoders
            val height = (settings.height / 16) * 16
            val fps = settings.fps
            val durationSec = settings.effectiveDurationSeconds
            val totalFrames = durationSec * fps

            _progress.value = ExportProgress(
                stage = ExportStage.ANALYZING_AUDIO,
                progress = 0.05f,
                totalFrames = totalFrames,
                message = "Analyzing audio waveform and spectrum..."
            )

            // Step 1: Generate pre-analyzed audio spectrum & waveform snapshots for each frame
            val frameAudioData = generateAudioSnapshots(settings, totalFrames, fps)

            if (isCancelled) {
                return@withContext Result.failure(Exception("Export cancelled"))
            }

            // Step 2: Encode Video Frames to H.264 using MediaCodec
            _progress.value = ExportProgress(
                stage = ExportStage.ENCODING_VIDEO,
                progress = 0.1f,
                totalFrames = totalFrames,
                message = "Rendering visualizer frames..."
            )

            encodeVideoFrames(
                settings = settings,
                width = width,
                height = height,
                fps = fps,
                totalFrames = totalFrames,
                frameAudioData = frameAudioData,
                tempOutputFile = tempVideoFile
            )

            if (isCancelled) {
                tempVideoFile.delete()
                return@withContext Result.failure(Exception("Export cancelled"))
            }

            // Step 3: Extract or Transcode Audio Track
            _progress.value = ExportProgress(
                stage = ExportStage.MUXING_AUDIO,
                progress = 0.85f,
                totalFrames = totalFrames,
                message = "Extracting and synchronizing audio..."
            )

            val audioFormat = prepareAudioTrack(settings, durationSec, tempAudioFile)

            // Step 4: Mux Video and Audio into final MP4 using MediaMuxer
            _progress.value = ExportProgress(
                stage = ExportStage.MUXING_AUDIO,
                progress = 0.92f,
                totalFrames = totalFrames,
                message = "Muxing MP4 container..."
            )

            muxVideoAndAudio(
                tempVideoFile = tempVideoFile,
                tempAudioFile = tempAudioFile,
                outputFile = outputFile,
                width = width,
                height = height,
                fps = fps,
                audioFormat = audioFormat
            )

            // Cleanup temp files
            tempVideoFile.delete()
            tempAudioFile.delete()

            // Step 5: Notify Android MediaStore scanner
            MediaScannerConnection.scanFile(
                context,
                arrayOf(outputFile.absolutePath),
                arrayOf("video/mp4"),
                null
            )

            // Save record in Room database
            val record = ExportRecordEntity(
                id = "exp_${System.currentTimeMillis()}",
                title = settings.title,
                videoUriString = Uri.fromFile(outputFile).toString(),
                filePath = outputFile.absolutePath,
                durationMs = durationSec * 1000L,
                resolution = "${width}x${height}",
                aspectRatio = settings.aspectRatioName,
                fileSizeBytes = outputFile.length()
            )
            exportRecordDao.insertExport(record)

            _progress.value = ExportProgress(
                stage = ExportStage.COMPLETED,
                progress = 1.0f,
                totalFrames = totalFrames,
                message = "Video exported successfully!",
                outputFile = outputFile
            )

            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Video export failed", e)
            tempVideoFile.delete()
            tempAudioFile.delete()
            if (outputFile.exists()) outputFile.delete()

            val errorMsg = e.localizedMessage ?: "Unknown encoding error"
            _progress.value = ExportProgress(
                stage = ExportStage.FAILED,
                progress = 0f,
                error = errorMsg,
                message = "Export failed: $errorMsg"
            )
            Result.failure(e)
        }
    }

    private data class FrameAudioSnapshot(
        val fftData: FloatArray,
        val waveform: FloatArray
    )

    private fun generateAudioSnapshots(
        settings: VideoExportSettings,
        totalFrames: Int,
        fps: Int
    ): List<FrameAudioSnapshot> {
        val snapshots = ArrayList<FrameAudioSnapshot>(totalFrames)
        // If file is accessible as WAV, try to read PCM samples
        var pcmSamples: ShortArray? = null
        try {
            val uri = Uri.parse(settings.track.uriString)
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists() && file.extension.lowercase() == "wav") {
                    pcmSamples = readWavPcm(file)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read PCM directly: ${e.message}")
        }

        val sampleRate = 44100
        val samplesPerFrame = sampleRate / fps

        for (frame in 0 until totalFrames) {
            val fft = FloatArray(64)
            val wave = FloatArray(128)

            if (pcmSamples != null && pcmSamples.isNotEmpty()) {
                val startIdx = frame * samplesPerFrame
                for (w in 0 until 128) {
                    val sIdx = (startIdx + (w * samplesPerFrame / 128)).coerceIn(0, pcmSamples.size - 1)
                    wave[w] = (pcmSamples[sIdx] / 32768f).coerceIn(-1f, 1f)
                }
                // Compute energy bands from wave
                var energy = 0f
                for (w in 0 until 128) energy += Math.abs(wave[w])
                energy = (energy / 128f) * 2.5f

                for (b in 0 until 64) {
                    val freqFactor = 1.0f - (b.toFloat() / 64f)
                    fft[b] = (energy * freqFactor).coerceIn(0.05f, 1.4f)
                }
            } else {
                // Procedural audio responsiveness fallback
                val timeSec = frame.toFloat() / fps
                val beat = (sin(timeSec * 2.0 * PI * 2.1) * 0.5 + 0.5).toFloat()
                for (b in 0 until 64) {
                    val factor = (1.0f - b / 64f)
                    val mod = (sin(timeSec * 4.0 + b * 0.3) * 0.35 + 0.65).toFloat()
                    fft[b] = (beat * 0.7f + mod * 0.3f) * factor
                }
                for (w in 0 until 128) {
                    val t = (w.toFloat() / 128f) * 4.0 * PI
                    wave[w] = (sin(t + timeSec * 6.0) * 0.7f).toFloat() * beat
                }
            }
            snapshots.add(FrameAudioSnapshot(fft, wave))
        }
        return snapshots
    }

    private fun readWavPcm(file: File): ShortArray {
        val fis = FileInputStream(file)
        val header = ByteArray(44)
        fis.read(header)
        val pcmBytes = fis.readBytes()
        fis.close()

        val shorts = ShortArray(pcmBytes.size / 2)
        ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
        return shorts
    }

    private fun encodeVideoFrames(
        settings: VideoExportSettings,
        width: Int,
        height: Int,
        fps: Int,
        totalFrames: Int,
        frameAudioData: List<FrameAudioSnapshot>,
        tempOutputFile: File
    ) {
        val mime = MediaFormat.MIMETYPE_VIDEO_AVC
        val bitrate = if (width >= 1080) 6_000_000 else 3_500_000
        val iFrameInterval = 1 // 1 keyframe per second

        val format = MediaFormat.createVideoFormat(mime, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
        }

        val codec = MediaCodec.createEncoderByType(mime)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val fos = java.io.FileOutputStream(tempOutputFile)
        val bufferInfo = MediaCodec.BufferInfo()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val renderer = OfflineFrameRenderer(width, height, settings.renderConfig)
        val particleSim = ParticleSimulation(settings.renderConfig.particleCount)

        val argbArray = IntArray(width * height)
        val yuvArray = ByteArray(width * height * 3 / 2)

        var frameIndex = 0
        var sawInputEOS = false
        var sawOutputEOS = false

        while (!sawOutputEOS && !isCancelled) {
            // Feed input
            if (!sawInputEOS && frameIndex < totalFrames) {
                val inputBufferIndex = codec.dequeueInputBuffer(10000L)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                    if (inputBuffer != null) {
                        inputBuffer.clear()

                        // Render frame onto bitmap
                        val audioData = frameAudioData[frameIndex]
                        val timeSeconds = frameIndex.toFloat() / fps
                        val particles = particleSim.update(
                            width.toFloat(), height.toFloat(),
                            audioData.fftData, settings.renderConfig.sensitivity, 1f / fps
                        )

                        renderer.renderFrame(
                            canvas = canvas,
                            fftData = audioData.fftData,
                            waveformData = audioData.waveform,
                            particles = particles,
                            timeSeconds = timeSeconds
                        )

                        // Convert ARGB to YUV420
                        bitmap.getPixels(argbArray, 0, width, 0, 0, width, height)
                        argbToYuv420(argbArray, yuvArray, width, height)

                        inputBuffer.put(yuvArray)
                        val presentationTimeUs = frameIndex * 1_000_000L / fps
                        codec.queueInputBuffer(
                            inputBufferIndex, 0, yuvArray.size, presentationTimeUs, 0
                        )

                        frameIndex++
                        val progress = 0.1f + 0.75f * (frameIndex.toFloat() / totalFrames)
                        _progress.value = _progress.value.copy(
                            progress = progress,
                            currentFrame = frameIndex,
                            message = "Rendering frame $frameIndex / $totalFrames"
                        )
                    }
                }
            } else if (!sawInputEOS) {
                val inputBufferIndex = codec.dequeueInputBuffer(10000L)
                if (inputBufferIndex >= 0) {
                    val presentationTimeUs = frameIndex * 1_000_000L / fps
                    codec.queueInputBuffer(
                        inputBufferIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                    sawInputEOS = true
                }
            }

            // Drain output
            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000L)
            if (outputBufferIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    val chunk = ByteArray(bufferInfo.size)
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    outputBuffer.get(chunk)
                    fos.write(chunk)
                }
                codec.releaseOutputBuffer(outputBufferIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true
                }
            }
        }

        fos.flush()
        fos.close()
        codec.stop()
        codec.release()
        bitmap.recycle()
    }

    private fun argbToYuv420(argb: IntArray, yuv: ByteArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uIndex = frameSize
        var vIndex = frameSize + frameSize / 4

        var a: Int
        var r: Int
        var g: Int
        var b: Int
        var y: Int
        var u: Int
        var v: Int
        var index = 0

        for (j in 0 until height) {
            for (i in 0 until width) {
                a = (argb[index] and -0x1000000) shr 24
                r = (argb[index] and 0xff0000) shr 16
                g = (argb[index] and 0xff00) shr 8
                b = (argb[index] and 0xff)

                y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                yuv[yIndex++] = (if (y < 0) 0 else if (y > 255) 255 else y).toByte()
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv[uIndex++] = (if (u < 0) 0 else if (u > 255) 255 else u).toByte()
                    yuv[vIndex++] = (if (v < 0) 0 else if (v > 255) 255 else v).toByte()
                }
                index++
            }
        }
    }

    private fun prepareAudioTrack(
        settings: VideoExportSettings,
        durationSeconds: Int,
        tempAudioFile: File
    ): MediaFormat {
        // Encode PCM WAV or audio input to AAC
        val mime = MediaFormat.MIMETYPE_AUDIO_AAC
        val sampleRate = 44100
        val channels = 2
        val bitrate = 192000

        val format = MediaFormat.createAudioFormat(mime, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        val codec = MediaCodec.createEncoderByType(mime)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val fos = java.io.FileOutputStream(tempAudioFile)
        val bufferInfo = MediaCodec.BufferInfo()

        // Read source audio bytes
        val uri = Uri.parse(settings.track.uriString)
        val inputStream = try {
            if (uri.scheme == "file") FileInputStream(File(uri.path ?: ""))
            else context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            null
        }

        // If WAV, skip 44-byte header
        if (inputStream != null && settings.track.uriString.lowercase().endsWith(".wav")) {
            inputStream.skip(44)
        }

        val chunk = ByteArray(4096)
        var totalBytesRead = 0
        val maxBytes = durationSeconds * sampleRate * channels * 2
        var sawInputEOS = false
        var sawOutputEOS = false

        while (!sawOutputEOS && !isCancelled) {
            if (!sawInputEOS) {
                val inputBufferIndex = codec.dequeueInputBuffer(10000L)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                    if (inputBuffer != null) {
                        inputBuffer.clear()
                        val bytesToRead = minOf(chunk.size, (maxBytes - totalBytesRead).coerceAtLeast(0))
                        val read = if (bytesToRead > 0 && inputStream != null) {
                            inputStream.read(chunk, 0, bytesToRead)
                        } else -1

                        if (read > 0) {
                            inputBuffer.put(chunk, 0, read)
                            val ptsUs = (totalBytesRead.toLong() * 1_000_000L) / (sampleRate * channels * 2)
                            codec.queueInputBuffer(inputBufferIndex, 0, read, ptsUs, 0)
                            totalBytesRead += read
                        } else {
                            codec.queueInputBuffer(
                                inputBufferIndex, 0, 0,
                                (totalBytesRead.toLong() * 1_000_000L) / (sampleRate * channels * 2),
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            sawInputEOS = true
                        }
                    }
                }
            }

            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000L)
            if (outputBufferIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    val aacChunk = ByteArray(bufferInfo.size)
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    outputBuffer.get(aacChunk)
                    fos.write(aacChunk)
                }
                codec.releaseOutputBuffer(outputBufferIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true
                }
            }
        }

        inputStream?.close()
        fos.flush()
        fos.close()
        val outputFormat = codec.outputFormat
        codec.stop()
        codec.release()

        return outputFormat
    }

    private fun muxVideoAndAudio(
        tempVideoFile: File,
        tempAudioFile: File,
        outputFile: File,
        width: Int,
        height: Int,
        fps: Int,
        audioFormat: MediaFormat
    ) {
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        // Setup Video Track
        val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        }

        // Use MediaExtractor on tempVideoFile and tempAudioFile to feed muxer
        val videoExtractor = MediaExtractor().apply { setDataSource(tempVideoFile.absolutePath) }
        val videoTrackIdxInExtractor = (0 until videoExtractor.trackCount).find {
            videoExtractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
        } ?: 0
        videoExtractor.selectTrack(videoTrackIdxInExtractor)
        val extractedVideoFormat = videoExtractor.getTrackFormat(videoTrackIdxInExtractor)
        val videoTrackIndex = muxer.addTrack(extractedVideoFormat)

        // Setup Audio Track
        var audioTrackIndex = -1
        val audioExtractor = MediaExtractor()
        var hasAudio = false
        try {
            audioExtractor.setDataSource(tempAudioFile.absolutePath)
            val audioTrackIdxInExtractor = (0 until audioExtractor.trackCount).find {
                audioExtractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }
            if (audioTrackIdxInExtractor != null) {
                audioExtractor.selectTrack(audioTrackIdxInExtractor)
                val extractedAudioFormat = audioExtractor.getTrackFormat(audioTrackIdxInExtractor)
                audioTrackIndex = muxer.addTrack(extractedAudioFormat)
                hasAudio = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio extractor note: ${e.message}")
        }

        muxer.start()

        // Write Video Samples
        val videoBuffer = ByteBuffer.allocate(1024 * 1024)
        val videoBufferInfo = MediaCodec.BufferInfo()
        while (!isCancelled) {
            val sampleSize = videoExtractor.readSampleData(videoBuffer, 0)
            if (sampleSize < 0) break
            videoBufferInfo.offset = 0
            videoBufferInfo.size = sampleSize
            videoBufferInfo.presentationTimeUs = videoExtractor.sampleTime
            videoBufferInfo.flags = videoExtractor.sampleFlags
            muxer.writeSampleData(videoTrackIndex, videoBuffer, videoBufferInfo)
            videoExtractor.advance()
        }
        videoExtractor.release()

        // Write Audio Samples
        if (hasAudio && audioTrackIndex >= 0) {
            val audioBuffer = ByteBuffer.allocate(256 * 1024)
            val audioBufferInfo = MediaCodec.BufferInfo()
            while (!isCancelled) {
                val sampleSize = audioExtractor.readSampleData(audioBuffer, 0)
                if (sampleSize < 0) break
                audioBufferInfo.offset = 0
                audioBufferInfo.size = sampleSize
                audioBufferInfo.presentationTimeUs = audioExtractor.sampleTime
                audioBufferInfo.flags = audioExtractor.sampleFlags
                muxer.writeSampleData(audioTrackIndex, audioBuffer, audioBufferInfo)
                audioExtractor.advance()
            }
            audioExtractor.release()
        }

        try {
            muxer.stop()
            muxer.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping muxer", e)
        }
    }

    private fun getOutputDir(): File {
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val vantaDir = File(moviesDir, "VantaWave")
        if (!vantaDir.exists()) {
            vantaDir.mkdirs()
        }
        return if (vantaDir.exists() && vantaDir.canWrite()) vantaDir else context.filesDir
    }
}
