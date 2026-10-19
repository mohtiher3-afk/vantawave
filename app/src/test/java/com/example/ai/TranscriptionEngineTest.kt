package com.example.ai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class TranscriptionEngineTest {

    private lateinit var context: Context
    private lateinit var modelManager: ModelManager
    private lateinit var engine: OnnxQuantizedTranscriptionEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        modelManager = ModelManager(context)
        engine = OnnxQuantizedTranscriptionEngine(context, modelManager)
    }

    @Test
    fun testVerifyModelIntegrity_fileNotFound() = runBlocking {
        val nonExistentFile = File(context.cacheDir, "does_not_exist.onnx")
        val result = engine.verifyModelIntegrity(nonExistentFile)
        assertTrue(result is ModelIntegrityResult.FileNotFound)
    }

    @Test
    fun testVerifyModelIntegrity_tooSmallOrInvalidHeader() = runBlocking {
        val badFile = File(context.cacheDir, "bad_model.onnx")
        FileOutputStream(badFile).use { it.write("BAD".toByteArray()) }

        val result = engine.verifyModelIntegrity(badFile)
        assertTrue(result is ModelIntegrityResult.InvalidHeader)
    }

    @Test
    fun testVerifyModelIntegrity_customQuantizedModel() = runBlocking {
        val customFile = File(context.cacheDir, "test_custom_quant.onnx")
        val validContent = "VANTAWAVE_QUANTIZED_MODEL_WEIGHTS_1234567890".toByteArray()
        FileOutputStream(customFile).use { it.write(validContent) }

        val digest = MessageDigest.getInstance("SHA-256")
        val expectedSha256 = digest.digest(validContent).joinToString("") { "%02x".format(it) }

        val result = engine.verifyModelIntegrity(customFile, expectedSha256 = expectedSha256)
        assertTrue(result is ModelIntegrityResult.Valid)
        val valid = result as ModelIntegrityResult.Valid
        assertEquals(expectedSha256, valid.sha256Hex)
        assertEquals("Custom Quantized INT8 Model", valid.modelFormat)
    }

    @Test
    fun testVerifyModelIntegrity_standardOnnxProtobufModel() = runBlocking {
        val onnxFile = File(context.cacheDir, "test_standard.onnx")
        // Standard ONNX protobuf files start with 0x08 (ir_version) followed by version varint
        val onnxBytes = byteArrayOf(0x08, 0x08, 0x12, 0x04, 0x6F, 0x6E, 0x6E, 0x78, 0x1A, 0x04, 0x74, 0x65, 0x73, 0x74, 0x20, 0x01)
        FileOutputStream(onnxFile).use { it.write(onnxBytes) }

        val result = engine.verifyModelIntegrity(onnxFile)
        assertTrue(result is ModelIntegrityResult.Valid)
        val valid = result as ModelIntegrityResult.Valid
        assertEquals("Standard ONNX Protobuf Model", valid.modelFormat)
        assertEquals(onnxBytes.size.toLong(), valid.sizeBytes)
    }

    @Test
    fun testLoadModel_lifecycle() = runBlocking {
        val validFile = File(context.cacheDir, "lifecycle_model.onnx")
        val validContent = "VANTAWAVE_QUANTIZED_ONNX_MODEL_TEST_BUFFER".toByteArray()
        FileOutputStream(validFile).use { it.write(validContent) }

        assertFalse(engine.isLoaded())

        val loadResult = engine.loadModel(validFile)
        assertTrue(loadResult is ModelLoadResult.Success)
        assertTrue(engine.isLoaded())
        assertNotNull(engine.getLoadedModelInfo())
        assertTrue(engine.initializationStatus.value is ModelInitializationStatus.Ready)

        engine.release()
        assertFalse(engine.isLoaded())
        assertEquals(ModelInitializationStatus.NotInitialized, engine.initializationStatus.value)
    }

    @Test
    fun testEnsureModelLoaded_verifiesCacheAndChecksum() = runBlocking {
        // Ensure initial state
        assertEquals(ModelInitializationStatus.NotInitialized, engine.initializationStatus.value)

        // Calling ensureModelLoaded should locate/initialize cache file, verify checksum, and mark Ready
        val result = engine.ensureModelLoaded()
        assertTrue(result is ModelLoadResult.Success)
        assertTrue(engine.isLoaded())

        val status = engine.initializationStatus.value
        assertTrue("InitializationStatus must be Ready", status is ModelInitializationStatus.Ready)
        val ready = status as ModelInitializationStatus.Ready
        assertTrue("Checksum must be non-empty", ready.checksumSha256.isNotBlank())
        assertNotNull(ready.modelInfo)
    }

    @Test
    fun testTranscribePcm_flowIncrementalEmissions() = runBlocking {
        val sampleRate = 16000
        val pcm = ShortArray(sampleRate) { i ->
            (kotlin.math.sin(2.0 * Math.PI * 440.0 * i / sampleRate) * 12000).toInt().toShort()
        }

        val input = AudioInputSource.PcmSource(
            pcmShorts = pcm,
            sampleRate = sampleRate,
            trackTitle = "Cyber Neon Journey"
        )

        val steps = engine.transcribe(input).toList()
        assertTrue("Must emit multiple flow steps", steps.size >= 5)

        // Verify Progress steps are emitted
        val progressSteps = steps.filterIsInstance<TranscriptionStep.Progress>()
        assertTrue("Must emit Progress steps", progressSteps.isNotEmpty())
        assertTrue("Final progress must reach near 100", progressSteps.last().percentage >= 90)

        // Verify IncrementalSegment steps are emitted progressively
        val incrementalSteps = steps.filterIsInstance<TranscriptionStep.IncrementalSegment>()
        assertTrue("Must emit IncrementalSegment steps progressively", incrementalSteps.isNotEmpty())
        assertEquals(1, incrementalSteps.first().currentSegmentsCount)

        // Verify Complete step is emitted
        val completeStep = steps.filterIsInstance<TranscriptionStep.Complete>().firstOrNull()
        assertNotNull("Must emit Complete step", completeStep)
        assertTrue("Segments must not be empty", completeStep!!.segments.isNotEmpty())
    }
}
