package com.example.devsummit2026.ui.mlkit

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Handles ML Kit on-device text recognition operations,
 * bitmap decoding from URIs, sample bitmap creation, and clipboard utilities.
 */
class TextRecognitionHelper {

    /**
     * Executes ML Kit on-device Text Recognition on the provided Bitmap.
     */
    fun runTextRecognition(
        bitmap: Bitmap,
        onResult: (text: String, blockCount: Int, error: String?) -> Unit
    ) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                onResult(visionText.text, visionText.textBlocks.size, null)
            }
            .addOnFailureListener { exception ->
                onResult("", 0, exception.localizedMessage ?: "Failed to process text")
            }
    }

    /**
     * Generates an in-memory sample bitmap containing readable text for quick testing.
     */
    fun createSampleBitmap(): Bitmap {
        val width = 640
        val height = 360
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint().apply {
            color = Color.rgb(25, 118, 210) // Blue
            textSize = 36f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }

        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 28f
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }

        canvas.drawText("ML Kit On-Device OCR", 40f, 70f, titlePaint)
        canvas.drawText("DevSummit 2026 Android Lab", 40f, 130f, bodyPaint)
        canvas.drawText("Kotlin + Jetpack Compose", 40f, 185f, bodyPaint)
        canvas.drawText("100% On-Device & Private!", 40f, 240f, bodyPaint)
        canvas.drawText("Status: Ready for Inference", 40f, 295f, bodyPaint)

        return bitmap
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
     * Copies text into the Android clipboard.
     */
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Recognized Text", text)
        clipboard.setPrimaryClip(clip)
    }
}
