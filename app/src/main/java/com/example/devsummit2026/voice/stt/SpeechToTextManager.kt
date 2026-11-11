package com.example.devsummit2026.voice.stt

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechToTextManager(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<SpeechToTextState>(SpeechToTextState.Idle)
    val state: StateFlow<SpeechToTextState> = _state.asStateFlow()

    fun startListening(languageCode: String? = null) = runOnMain {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _state.value = SpeechToTextState.Error(
                message = "Speech recognition is not available on this device",
                code = -1
            )
            return@runOnMain
        }

        try {
            val recognizer = getOrCreateRecognizer()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
                if (!languageCode.isNullOrBlank()) {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                }
            }

            recognizer.startListening(intent)
            _state.value = SpeechToTextState.Listening
        } catch (e: Exception) {
            _state.value = SpeechToTextState.Error(
                message = e.localizedMessage ?: "Failed to start speech recognition",
                code = -1
            )
        }
    }

    fun stopListening() = runOnMain {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
    }

    fun cancel() = runOnMain {
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        _state.value = SpeechToTextState.Idle
    }

    fun destroy() = runOnMain {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _state.value = SpeechToTextState.Idle
    }

    private fun getOrCreateRecognizer(): SpeechRecognizer {
        speechRecognizer?.let { return it }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
        recognizer.setRecognitionListener(createRecognitionListener())
        speechRecognizer = recognizer
        return recognizer
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    private fun createRecognitionListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = SpeechToTextState.Listening
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            if (error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED ||
                error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
            ) {
                try {
                    speechRecognizer?.destroy()
                } catch (_: Exception) {}
                speechRecognizer = null
            }
            val message = SpeechErrorMapper.map(error)
            _state.value = SpeechToTextState.Error(message = message, code = error)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull().orEmpty()
            _state.value = SpeechToTextState.Success(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull().orEmpty()
            if (text.isNotBlank()) {
                _state.value = SpeechToTextState.PartialResult(text)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
