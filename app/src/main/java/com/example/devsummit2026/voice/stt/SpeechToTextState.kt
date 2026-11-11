package com.example.devsummit2026.voice.stt

sealed interface SpeechToTextState {
    data object Idle : SpeechToTextState
    data object Listening : SpeechToTextState
    data class PartialResult(val text: String) : SpeechToTextState
    data class Success(val text: String) : SpeechToTextState
    data class Error(val message: String, val code: Int) : SpeechToTextState
}
