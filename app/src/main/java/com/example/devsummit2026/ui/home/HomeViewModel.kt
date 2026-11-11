package com.example.devsummit2026.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.devsummit2026.firebase.FirebaseAiChatService
import com.example.devsummit2026.model.ChatMessage
import com.example.devsummit2026.voice.stt.SpeechToTextManager
import com.example.devsummit2026.voice.stt.SpeechToTextState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel @JvmOverloads constructor(
    application: Application,
    private val chatService: FirebaseAiChatService = FirebaseAiChatService(),
    private val sttManager: SpeechToTextManager = SpeechToTextManager(application)
) : AndroidViewModel(application) {

    val messages = mutableStateListOf<ChatMessage>()
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val sttState: StateFlow<SpeechToTextState> = sttManager.state

    fun sendMessage(text: String) {
        val prompt = text.trim()
        if (prompt.isBlank() || isLoading) return

        messages.add(ChatMessage(prompt, fromUser = true))
        error = null
        isLoading = true

        viewModelScope.launch {
            try {
                val reply = chatService.sendMessage(prompt)
                messages.add(ChatMessage(reply, fromUser = false))
            } catch (exception: Exception) {
                error = exception.localizedMessage
                    ?: "Could not reach Firebase AI. Check your connection and Firebase setup."
            } finally {
                isLoading = false
            }
        }
    }

    fun startListening() {
        sttManager.startListening()
    }

    fun stopListening() {
        sttManager.stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        sttManager.destroy()
    }
}
