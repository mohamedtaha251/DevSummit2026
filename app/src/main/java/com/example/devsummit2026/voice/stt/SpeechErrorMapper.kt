package com.example.devsummit2026.voice.stt

import android.speech.SpeechRecognizer

object SpeechErrorMapper {
    fun map(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client-side error occurred"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio permission not granted"
        SpeechRecognizer.ERROR_NETWORK -> "Network communication error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network operation timed out"
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech could be recognized"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
        SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Recognition service disconnected. Tap to retry"
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many speech requests"
        12 -> "Language not supported"
        13 -> "Language unavailable"
        14 -> "Cannot check language support"
        else -> "Speech recognition error ($errorCode)"
    }
}
