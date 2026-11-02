package com.example.devsummit2026.ui.litert

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.devsummit2026.litert.BackgroundCompositor
import com.example.devsummit2026.litert.BackgroundMode
import com.example.devsummit2026.litert.SelfieSegmentationHelper

/**
 * Simplified, rock-solid ViewModel for Google Meet background segmentation.
 * Stripped of unnecessary frame-by-frame stats to ensure maximum stability and zero UI lag.
 */
class MeetBackgroundViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val segmenterHelper = SelfieSegmentationHelper(
        context = application.applicationContext,
        onInitialized = {
            isModelReady = true
        }
    )

    private val compositor = BackgroundCompositor()

    var isModelReady by mutableStateOf(false)
        private set

    var activeMode by mutableStateOf(BackgroundMode.BLUR)
        private set

    var processedBitmap by mutableStateOf<Bitmap?>(null)
        private set

    var isSimulatedMode by mutableStateOf(false)
        private set

    fun selectMode(mode: BackgroundMode) {
        activeMode = mode
        if (isSimulatedMode) {
            runSimulationFrame()
        }
    }

    /**
     * Processes an incoming camera frame with rotation correction, selfie horizontal mirroring,
     * segmentation inference, and double-buffered background compositing.
     */
    @OptIn(ExperimentalGetImage::class)
    fun processCameraFrame(imageProxy: ImageProxy) {
        try {
            val rawBitmap = imageProxy.toBitmap()

            // Rotate and mirror horizontally for front camera selfie perspective
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val matrix = Matrix().apply {
                postRotate(rotationDegrees.toFloat())
                postScale(-1f, 1f)
            }
            val orientedBitmap = Bitmap.createBitmap(
                rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
            )

            processBitmapFrame(orientedBitmap)
        } catch (_: Exception) {
        } finally {
            imageProxy.close()
        }
    }

    private fun processBitmapFrame(orientedBitmap: Bitmap) {
        if (activeMode == BackgroundMode.NONE || !isModelReady) {
            processedBitmap = orientedBitmap
            return
        }

        val segmentationResult = segmenterHelper.segment(orientedBitmap)
        if (segmentationResult == null) {
            processedBitmap = orientedBitmap
            return
        }

        val result = compositor.composite(
            cameraBitmap = orientedBitmap,
            maskBitmap = segmentationResult.maskBitmap,
            mode = activeMode
        )

        processedBitmap = result
    }

    fun toggleSimulationMode() {
        isSimulatedMode = !isSimulatedMode
        if (isSimulatedMode) {
            runSimulationFrame()
        }
    }

    private fun runSimulationFrame() {
        val sampleBitmap = createSamplePortraitBitmap()
        processBitmapFrame(sampleBitmap)
    }

    private fun createSamplePortraitBitmap(): Bitmap {
        val w = 480
        val h = 640
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // Room background
        val bgPaint = Paint().apply { color = Color.rgb(215, 222, 230) }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val windowPaint = Paint().apply { color = Color.rgb(160, 195, 225) }
        c.drawRect(40f, 60f, 180f, 260f, windowPaint)

        // Person torso & shoulders
        val shirtPaint = Paint().apply { color = Color.rgb(40, 60, 100); isAntiAlias = true }
        c.drawOval(w * 0.15f, h * 0.55f, w * 0.85f, h * 1.05f, shirtPaint)

        // Neck & Head
        val skinPaint = Paint().apply { color = Color.rgb(235, 185, 150); isAntiAlias = true }
        c.drawRect(w * 0.42f, h * 0.45f, w * 0.58f, h * 0.60f, skinPaint)
        c.drawOval(w * 0.30f, h * 0.20f, w * 0.70f, h * 0.52f, skinPaint)

        // Hair
        val hairPaint = Paint().apply { color = Color.rgb(45, 30, 20); isAntiAlias = true }
        c.drawOval(w * 0.28f, h * 0.16f, w * 0.72f, h * 0.32f, hairPaint)

        return bmp
    }

    override fun onCleared() {
        super.onCleared()
        segmenterHelper.close()
    }
}
