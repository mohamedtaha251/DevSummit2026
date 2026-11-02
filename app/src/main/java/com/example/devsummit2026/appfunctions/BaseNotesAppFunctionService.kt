package com.example.devsummit2026.appfunctions

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import com.example.devsummit2026.appfunctions.data.NotesRepository
import com.example.devsummit2026.appfunctions.model.Note

/**
 * Entry point declaring the Quick Notes App Functions exposed to on-device AI assistants.
 */
@AppFunctionServiceEntryPoint(
    serviceName = "NotesAppFunctionService",
    appFunctionXmlFileName = "notes_app_functions"
)
abstract class BaseNotesAppFunctionService : AppFunctionService() {

    /**
     * Creates and saves a new note.
     *
     * @param title The title or subject of the note.
     * @param content The detailed content or body text of the note.
     * @return The created [Note] with generated id and timestamp.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    fun createNote(title: String, content: String): Note {
        return NotesRepository.addNote(title = title, content = content)
    }

    /**
     * Searches notes by matching text in title or content.
     *
     * @param query The search keyword or phrase to look for.
     * @return A list of [Note] objects that match the query.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    fun searchNotes(query: String): List<Note> {
        return NotesRepository.searchNotes(query = query)
    }
}
