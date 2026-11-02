package com.example.devsummit2026.litert

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import com.google.android.gms.tflite.java.TfLite
import org.tensorflow.lite.InterpreterApi
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Result data class for top predictions.
 */
data class ClassificationResult(
    val label: String,
    val score: Float,
    val inferenceTimeMs: Long = 0L
)

/**
 * Helper class for On-Device Image Classification using LiteRT via Google Play services runtime.
 */
class ImageClassifierHelper(
    private val context: Context,
    var onInitialized: (() -> Unit)? = null,
    var onError: ((String) -> Unit)? = null
) : AutoCloseable {

    companion object {
        const val MODEL_FILE = "mobilenet_v1_1.0_224_quant.tflite"
        const val LABELS_FILE = "labels.txt"
        const val IMAGE_SIZE = 224
        const val COLOR_CHANNELS = 3 // RGB
    }

    private var interpreter: InterpreterApi? = null
    private var labels: List<String> = emptyList()

    var isInitialized: Boolean = false
        private set

    var lastInferenceTimeMs: Long = 0L
        private set

    init {
        initialize()
    }

    /**
     * Initializes Google Play services TfLite runtime and creates the InterpreterApi instance.
     */
    fun initialize() {
        TfLite.initialize(context)
            .addOnSuccessListener {
                try {
                    val modelBuffer = loadModelFile(context, MODEL_FILE)
                    labels = loadLabels(context, LABELS_FILE)

                    val options = InterpreterApi.Options()
                        .setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)

                    interpreter = InterpreterApi.create(modelBuffer, options)
                    isInitialized = true
                    onInitialized?.invoke()
                } catch (e: Exception) {
                    val message = e.localizedMessage ?: "Failed to initialize LiteRT interpreter"
                    onError?.invoke(message)
                }
            }
            .addOnFailureListener { error ->
                val message = error.localizedMessage ?: "Google Play services TfLite runtime initialization failed"
                onError?.invoke(message)
            }
    }

    /**
     * Loads the model file from assets into a MappedByteBuffer.
     */
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
     * Reads label file from assets.
     */
    private fun loadLabels(context: Context, labelFileName: String): List<String> {
        return context.assets.open(labelFileName).bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() }.toList()
        }
    }

    /**
     * Resizes the image to 224x224, converts pixels to direct ByteBuffer, runs inference,
     * and returns the top 3 predicted labels with confidence scores and latency in milliseconds.
     */
    fun classify(bitmap: Bitmap): List<ClassificationResult> {
        val activeInterpreter = interpreter
            ?: throw IllegalStateException("LiteRT Interpreter is not ready. Google Play services TfLite may still be initializing.")

        if (labels.isEmpty()) {
            throw IllegalStateException("Labels list is empty. Check assets/labels.txt.")
        }

        // 1. Resize image to 224x224 required by MobileNet v1
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true)

        // 2. Convert pixels to direct ByteBuffer (UINT8 values: 1 x 224 x 224 x 3)
        val inputBuffer = ByteBuffer.allocateDirect(1 * IMAGE_SIZE * IMAGE_SIZE * COLOR_CHANNELS).apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }

        val pixels = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        resizedBitmap.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)

        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF).toByte()
            val g = (pixel shr 8 and 0xFF).toByte()
            val b = (pixel and 0xFF).toByte()
            inputBuffer.put(r)
            inputBuffer.put(g)
            inputBuffer.put(b)
        }

        // 3. Prepare output container (1 x labels.size UINT8 bytes)
        val output = Array(1) { ByteArray(labels.size) }

        // 4. Run inference and record latency
        val startTime = SystemClock.uptimeMillis()
        activeInterpreter.run(inputBuffer, output)
        val endTime = SystemClock.uptimeMillis()
        val inferenceTimeMs = endTime - startTime
        lastInferenceTimeMs = inferenceTimeMs

        // 5. Parse scores and return top 3 predictions
        val rawScores = output[0]
        val results = labels.indices.map { index ->
            val score = (rawScores[index].toInt() and 0xFF) / 255.0f
            ClassificationResult(
                label = labels[index],
                score = score,
                inferenceTimeMs = inferenceTimeMs
            )
        }

        return results
            .filter { !it.label.equals("background", ignoreCase = true) }
            .sortedByDescending { it.score }
            .take(3)
    }

    /**
     * Decodes an image URI into an Android Bitmap.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        }.getOrNull()
    }

    /**
     * Generates an in-memory sample bitmap (goldfish on light background) for quick testing.
     */
    fun createSampleBitmap(): Bitmap {
        val size = 224
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(240, 248, 255))

        val fishBodyPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(255, 120, 0)
        }
        canvas.drawOval(40f, 75f, 170f, 145f, fishBodyPaint)

        val tailPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(255, 140, 20)
        }
        val tailPath = android.graphics.Path().apply {
            moveTo(155f, 110f)
            lineTo(205f, 75f)
            lineTo(205f, 145f)
            close()
        }
        canvas.drawPath(tailPath, tailPaint)

        val eyePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }
        canvas.drawCircle(75f, 100f, 7f, eyePaint)

        val whiteEyePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
        }
        canvas.drawCircle(73f, 98f, 2.5f, whiteEyePaint)

        return bitmap
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
    }
}
