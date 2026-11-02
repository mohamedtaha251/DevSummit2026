package com.example.devsummit2026.appfunctions.model

import androidx.appfunctions.AppFunctionSerializable

/**
 * Represents a quick note with title and content.
 *
 * @property id The unique identifier of the note.
 * @property title The title or subject of the note.
 * @property content The main body text of the note.
 * @property createdAt Timestamp in milliseconds when the note was created.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
