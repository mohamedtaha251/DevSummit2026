package com.example.devsummit2026.litert

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.tflite.java.TfLite
import org.tensorflow.lite.InterpreterApi
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Result data class containing the segmentation mask bitmap.
 */
data class SegmentationResult(
    val maskBitmap: Bitmap
)

/**
 * Helper class for real-time On-Device Selfie Segmentation using LiteRT.
 * MediaPipe Selfie Segmentation model (~256x256 input).
 */
class SelfieSegmentationHelper(
    private val context: Context,
    var onInitialized: (() -> Unit)? = null,
    var onError: ((String) -> Unit)? = null
) : AutoCloseable {

    companion object {
        const val MODEL_FILE = "selfie_segmentation.tflite"
        const val INPUT_SIZE = 256
        const val COLOR_CHANNELS = 3 // RGB
    }

    private var interpreter: InterpreterApi? = null

    // Pre-allocated Direct ByteBuffers for input and output
    private val inputByteBuffer: ByteBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * COLOR_CHANNELS * 4).apply {
        order(ByteOrder.nativeOrder())
    }
    private val inputFloatBuffer: FloatBuffer = inputByteBuffer.asFloatBuffer()

    private val outputByteBuffer: ByteBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 1 * 4).apply {
        order(ByteOrder.nativeOrder())
    }
    private val outputFloatBuffer: FloatBuffer = outputByteBuffer.asFloatBuffer()

    // Reusable arrays and mask bitmap to prevent garbage collection pauses
    private val intPixels = IntArray(INPUT_SIZE * INPUT_SIZE)
    private val inputFloats = FloatArray(INPUT_SIZE * INPUT_SIZE * COLOR_CHANNELS)
    private val outputConfidence = FloatArray(INPUT_SIZE * INPUT_SIZE)
    private val maskPixels = IntArray(INPUT_SIZE * INPUT_SIZE)
    private val maskBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)

    var isInitialized: Boolean = false
        private set

    init {
        initialize()
    }

    fun initialize() {
        TfLite.initialize(context)
            .addOnSuccessListener {
                try {
                    val modelBuffer = loadModelFile(context, MODEL_FILE)
                    val options = InterpreterApi.Options()
                        .setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)

                    interpreter = InterpreterApi.create(modelBuffer, options)
                    isInitialized = true
                    onInitialized?.invoke()
                } catch (e: Exception) {
                    onError?.invoke(e.localizedMessage ?: "Failed to initialize LiteRT interpreter")
                }
            }
            .addOnFailureListener { error ->
                onError?.invoke(error.localizedMessage ?: "TfLite initialization failed")
            }
    }

    private fun loadModelFile(context: Context, modelFileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFileName)
        FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
        }
    }

    /**
     * Runs inference on the camera bitmap and produces an ARGB mask bitmap.
     * Applies smoothstep thresholding to stabilize the mask and eliminate flickering.
     */
    @Synchronized
    fun segment(cameraBitmap: Bitmap): SegmentationResult? {
        val activeInterpreter = interpreter ?: return null

        // 1. Scale camera frame to 256x256
        val scaledBitmap = if (cameraBitmap.width == INPUT_SIZE && cameraBitmap.height == INPUT_SIZE) {
            cameraBitmap
        } else {
            Bitmap.createScaledBitmap(cameraBitmap, INPUT_SIZE, INPUT_SIZE, true)
        }

        // 2. Extract RGB pixels and normalize to [0.0f, 1.0f]
        scaledBitmap.getPixels(intPixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        var floatIndex = 0
        for (i in 0 until INPUT_SIZE * INPUT_SIZE) {
            val color = intPixels[i]
            inputFloats[floatIndex++] = ((color shr 16) and 0xFF) / 255.0f
            inputFloats[floatIndex++] = ((color shr 8) and 0xFF) / 255.0f
            inputFloats[floatIndex++] = (color and 0xFF) / 255.0f
        }

        inputFloatBuffer.rewind()
        inputFloatBuffer.put(inputFloats)
        inputByteBuffer.rewind()
        outputByteBuffer.rewind()

        // 3. Execute inference
        activeInterpreter.run(inputByteBuffer, outputByteBuffer)

        // 4. Read confidence values (1 x 256 x 256 x 1)
        outputFloatBuffer.rewind()
        outputFloatBuffer.get(outputConfidence)

        // 5. Convert confidence mask into an alpha mask bitmap with smoothstep thresholding.
        // Clamping noise below 0.35 to 0 completely eliminates flickering in background pixels,
        // while values above 0.65 stay solid foreground.
        val low = 0.35f
        val high = 0.65f
        val range = high - low

        for (i in 0 until INPUT_SIZE * INPUT_SIZE) {
            val conf = outputConfidence[i]
            val alpha = when {
                conf <= low -> 0
                conf >= high -> 255
                else -> {
                    val t = (conf - low) / range
                    ((t * t * (3f - 2f * t)) * 255f).toInt().coerceIn(0, 255)
                }
            }
            maskPixels[i] = (alpha shl 24) or 0x00FFFFFF
        }

        maskBitmap.setPixels(maskPixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        return SegmentationResult(maskBitmap = maskBitmap)
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
    }
}
