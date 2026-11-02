package com.example.devsummit2026.ui.litert

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.devsummit2026.litert.ClassificationResult
import com.example.devsummit2026.litert.ImageClassifierHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel managing state for the LiteRT Image Classification screen.
 */
class ImageClassificationViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val classifierHelper = ImageClassifierHelper(
        context = application.applicationContext,
        onInitialized = {
            isRuntimeReady = true
            statusMessage = "LiteRT runtime ready (Google Play services)"
        },
        onError = { error ->
            errorMessage = error
            statusMessage = "LiteRT initialization failed"
        }
    )

    var selectedBitmap by mutableStateOf<Bitmap?>(null)
        private set

    var classifications by mutableStateOf<List<ClassificationResult>>(emptyList())
        private set

    var isProcessing by mutableStateOf(false)
        private set

    var isRuntimeReady by mutableStateOf(false)
        private set

    var statusMessage by mutableStateOf("Initializing LiteRT via Google Play services...")
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var inferenceLatencyMs by mutableStateOf<Long?>(null)
        private set

    fun onImageSelected(uri: Uri) {
        val bitmap = classifierHelper.loadBitmapFromUri(getApplication(), uri)
        if (bitmap != null) {
            selectedBitmap = bitmap
            classifications = emptyList()
            errorMessage = null
            inferenceLatencyMs = null
        } else {
            errorMessage = "Failed to load selected image"
        }
    }

    fun onSampleSelected() {
        selectedBitmap = classifierHelper.createSampleBitmap()
        classifications = emptyList()
        errorMessage = null
        inferenceLatencyMs = null
    }

    fun classifyCurrentImage() {
        val bitmap = selectedBitmap ?: return
        if (!isRuntimeReady) {
            errorMessage = "LiteRT runtime is still initializing. Please wait a moment."
            return
        }

        isProcessing = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val results = withContext(Dispatchers.Default) {
                    classifierHelper.classify(bitmap)
                }
                classifications = results
                inferenceLatencyMs = classifierHelper.lastInferenceTimeMs
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Classification failed"
            } finally {
                isProcessing = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        classifierHelper.close()
    }
}
