package com.example.devsummit2026.firebase

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirebaseAiChatService(
    modelName: String = "gemini-3.5-flash-lite"
) {
    private val chat = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel(modelName)
        .startChat()

    suspend fun sendMessage(prompt: String): String = withContext(Dispatchers.IO) {
        val response = chat.sendMessage(prompt)
        response.text?.takeIf(String::isNotBlank)
            ?: "The model returned an empty response."
    }
}
