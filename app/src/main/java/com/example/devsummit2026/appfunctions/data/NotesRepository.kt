package com.example.devsummit2026.appfunctions.data

import com.example.devsummit2026.appfunctions.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Simple in-memory repository for storing and retrieving notes.
 */
object NotesRepository {
    private val _notes = MutableStateFlow<List<Note>>(
        listOf(
            Note(
                id = "note_1",
                title = "Grocery List",
                content = "Buy milk, eggs, bread, and apples.",
                createdAt = System.currentTimeMillis() - 3600_000
            ),
            Note(
                id = "note_2",
                title = "DevSummit Keynote",
                content = "Explore Android App Functions API for on-device AI agent tool execution.",
                createdAt = System.currentTimeMillis() - 1800_000
            )
        )
    )

    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    /**
     * Creates and stores a new note.
     */
    fun addNote(title: String, content: String): Note {
        val newNote = Note(
            id = UUID.randomUUID().toString().take(8),
            title = title,
            content = content,
            createdAt = System.currentTimeMillis()
        )
        _notes.value = _notes.value + newNote
        return newNote
    }

    /**
     * Searches notes whose title or content contains the query.
     */
    fun searchNotes(query: String): List<Note> {
        val lowerQuery = query.lowercase().trim()
        if (lowerQuery.isEmpty()) {
            return _notes.value
        }
        return _notes.value.filter { note ->
            note.title.lowercase().contains(lowerQuery) ||
                note.content.lowercase().contains(lowerQuery)
        }
    }
}
