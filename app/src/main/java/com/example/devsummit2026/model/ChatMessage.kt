package com.example.devsummit2026.model

import androidx.annotation.keep.UnconditionallyKeep

@UnconditionallyKeep
data class ChatMessage(
    val text: String,
    val fromUser: Boolean
)
