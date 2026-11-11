package com.example.devsummit2026.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.devsummit2026.model.ChatMessage
import com.example.devsummit2026.voice.stt.SpeechToTextState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val messages = viewModel.messages
    val isLoading = viewModel.isLoading
    val error = viewModel.error
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    val sttState by viewModel.sttState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    LaunchedEffect(sttState) {
        when (val state = sttState) {
            is SpeechToTextState.PartialResult -> {
                input = state.text
            }
            is SpeechToTextState.Success -> {
                if (state.text.isNotBlank()) {
                    input = ""
                    viewModel.sendMessage(state.text)
                }
            }
            else -> Unit
        }
    }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val isListening = sttState is SpeechToTextState.Listening
    val errorMessage = error ?: (sttState as? SpeechToTextState.Error)?.message

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Firebase logic lab",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Firebase AI logic",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Ask Gemini a question with text or voice. Replies are generated through Firebase AI Logic.",
                style = MaterialTheme.typography.bodyMedium
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    ChatMessageItem(message = message)
                }
                if (isLoading) {
                    item {
                        CircularProgressIndicator()
                    }
                }
            }

            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Message") },
                    enabled = !isLoading,
                    maxLines = 4
                )

                VoiceInputButton(
                    isListening = isListening,
                    onStartListening = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            viewModel.startListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStopListening = { viewModel.stopListening() }
                )

                Button(
                    enabled = input.isNotBlank() && !isLoading,
                    onClick = {
                        val prompt = input
                        input = ""
                        viewModel.sendMessage(prompt)
                    }
                ) {
                    Text("Send")
                }
            }
            Spacer(Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = if (message.fromUser) "You" else "Gemini",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun VoiceInputButton(
    isListening: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voicePulseScale"
    )

    IconButton(
        onClick = {
            if (isListening) onStopListening() else onStartListening()
        },
        modifier = modifier.scale(if (isListening) scale else 1f)
    ) {
        Icon(
            imageVector = if (isListening) Icons.Filled.Mic else Icons.Filled.MicNone,
            contentDescription = if (isListening) "Stop voice input" else "Start voice input",
            tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}
