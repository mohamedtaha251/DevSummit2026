package com.example.devsummit2026

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.devsummit2026.ui.theme.DevSummit2026Theme
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Firebase.appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        }
        enableEdgeToEdge()
        setContent {
            DevSummit2026Theme {
                FirebaseAiChatScreen()
            }
        }
    }
}

private data class ChatMessage(val text: String, val fromUser: Boolean)

@Composable
private fun FirebaseAiChatScreen() {
    val chat = remember {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-3.5-flash-lite")
            .startChat()
    }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Firebase AI chat", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Ask Gemini a question. Replies are generated through Firebase AI Logic.",
                style = MaterialTheme.typography.bodyMedium
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = (if (message.fromUser) "You" else "Gemini") + ":  " + message.text,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                if (isLoading) item { CircularProgressIndicator() }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Message") },
                    enabled = !isLoading,
                    maxLines = 4
                )
                Button(
                    enabled = input.isNotBlank() && !isLoading,
                    onClick = {
                        val prompt = input.trim()
                        input = ""
                        messages.add(ChatMessage(prompt, fromUser = true))
                        error = null
                        isLoading = true
                        scope.launch {
                            try {
                                val response = chat.sendMessage(prompt)
                                messages.add(
                                    ChatMessage(
                                        response.text?.takeIf(String::isNotBlank)
                                            ?: "The model returned an empty response.",
                                        fromUser = false
                                    )
                                )
                            } catch (exception: Exception) {
                                error = exception.localizedMessage
                                    ?: "Could not reach Firebase AI. Check your connection and Firebase setup."
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                ) { Text("Send") }
            }
            Spacer(Modifier.padding(bottom = 4.dp))
        }
    }
}
