package com.example.devsummit2026.ui.mlkit

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class TextRecognitionViewModel(
    private val helper: TextRecognitionHelper = TextRecognitionHelper()
) : ViewModel() {

    var selectedBitmap by mutableStateOf<Bitmap?>(null)
        private set

    var recognizedText by mutableStateOf("")
        private set

    var detectedBlocksCount by mutableIntStateOf(0)
        private set

    var isProcessing by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun onImageSelected(context: Context, uri: Uri) {
        val bitmap = helper.loadBitmapFromUri(context, uri)
        if (bitmap != null) {
            processBitmap(bitmap)
        } else {
            errorMessage = "Failed to load image"
        }
    }

    fun onSampleSelected() {
        val sample = helper.createSampleBitmap()
        processBitmap(sample)
    }

    fun copyToClipboard(context: Context) {
        if (recognizedText.isNotEmpty()) {
            helper.copyToClipboard(context, recognizedText)
        }
    }

    private fun processBitmap(bitmap: Bitmap) {
        selectedBitmap = bitmap
        recognizedText = ""
        errorMessage = null
        isProcessing = true

        helper.runTextRecognition(bitmap) { text, blockCount, error ->
            isProcessing = false
            recognizedText = text
            detectedBlocksCount = blockCount
            errorMessage = error
        }
    }
}
