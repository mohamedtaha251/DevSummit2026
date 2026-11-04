package com.example.devsummit2026.ui.qwenagent.a2ui

import android.util.Log
import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiActionInterceptor
import androidx.a2ui.model.processor.A2uiMessageParser
import androidx.a2ui.model.processor.A2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiEventAction
import androidx.a2ui.model.protocol.A2uiUserAction
import com.example.devsummit2026.appfunctions.data.NotesRepository
import com.example.devsummit2026.appfunctions.model.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val TAG = "NotesA2uiManager"

/**
 * Manages the A2UI session, processing inbound declarative JSON payloads from the agent,
 * maintaining active surface states, and handling user action dispatches from interactive components.
 */
class NotesA2uiManager(
    private val scope: CoroutineScope,
    private val onNoteSavedCallback: ((surfaceId: String, note: Note) -> Unit)? = null
) {
    private val parser: A2uiMessageParser<String> = A2uiMessageParser()

    private val actionInterceptor = object : A2uiActionInterceptor {
        override suspend fun onInterceptAction(action: A2uiUserAction): A2uiUserAction? {
            if (action is A2uiEventAction && action.eventName == "saveNote") {
                handleSaveNote(
                    surfaceId = action.surfaceId,
                    context = action.context
                )
                return null // Handled locally
            }
            return action
        }
    }

    private val processor: A2uiMessageProcessor = A2uiMessageProcessor(
        listOf(NotesA2uiCatalog.createCatalog()),
        listOf(actionInterceptor)
    )

    val activeSurfaces: StateFlow<List<A2uiSurfaceModel>> = processor.activeSurfaces

    init {
        // Start background message collection loop
        scope.launch {
            try {
                processor.collectMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Error in A2UI message collection loop", e)
            }
        }

        // Outbound event listener as safety fallback
        scope.launch {
            try {
                processor.outboundEvents.collect { event ->
                    if (event is A2uiClientEventMessage && event.type == "saveNote") {
                        handleSaveNote(
                            surfaceId = event.surfaceId,
                            context = event.context
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in A2UI outbound events listener", e)
            }
        }
    }

    /**
     * Finds an active [A2uiSurfaceModel] by surfaceId.
     */
    fun getSurface(surfaceId: String): A2uiSurfaceModel? {
        return processor.activeSurfaces.value.find { it.id == surfaceId }
    }

    /**
     * Creates a new A2UI surface with the NoteEditor component and dispatches the
     * declarative A2UI JSON payload to the engine.
     * Returns the created surfaceId.
     */
    fun createNoteSurface(title: String, content: String): String {
        val surfaceId = "note_surface_${UUID.randomUUID().toString().take(8)}"

        // Envelope 1: createSurface
        val createSurfaceJson = JSONObject().apply {
            put("version", "v0.9")
            put("createSurface", JSONObject().apply {
                put("surfaceId", surfaceId)
                put("catalogId", NotesA2uiCatalog.CATALOG_ID)
            })
        }.toString()

        // Envelope 2: updateComponents
        val updateComponentsJson = JSONObject().apply {
            put("version", "v0.9")
            put("updateComponents", JSONObject().apply {
                put("surfaceId", surfaceId)
                put("components", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "root")
                        put("component", "NoteEditor")
                        put("title", title)
                        put("content", content)
                        put("isSaved", false)
                    })
                })
            })
        }.toString()

        Log.d(TAG, "Dispatching A2UI createSurface: $createSurfaceJson")
        processor.processInput(parser, createSurfaceJson)

        Log.d(TAG, "Dispatching A2UI updateComponents: $updateComponentsJson")
        processor.processInput(parser, updateComponentsJson)

        return surfaceId
    }

    /**
     * Processes any raw A2UI JSON payload string from the agent.
     */
    fun processA2uiJson(jsonString: String) {
        try {
            processor.processInput(parser, jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process raw A2UI json: $jsonString", e)
        }
    }

    /**
     * Handles note saving into [NotesRepository] and sends an A2UI update message to reflect saved state.
     */
    private fun handleSaveNote(surfaceId: String, context: Map<String, Any?>) {
        val title = (context["title"] as? String)?.takeIf { it.isNotBlank() } ?: "New Note"
        val content = (context["content"] as? String)?.takeIf { it.isNotBlank() } ?: title

        Log.i(TAG, "A2UI action dispatched: saving note to NotesRepository: title='$title'")
        val savedNote = NotesRepository.addNote(title, content)

        // Send updateComponents to update A2UI component to saved state
        val updateSavedJson = JSONObject().apply {
            put("version", "v0.9")
            put("updateComponents", JSONObject().apply {
                put("surfaceId", surfaceId)
                put("components", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "root")
                        put("component", "NoteEditor")
                        put("title", title)
                        put("content", content)
                        put("isSaved", true)
                    })
                })
            })
        }.toString()

        try {
            processor.processInput(parser, updateSavedJson)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update A2UI surface with saved state", e)
        }

        onNoteSavedCallback?.invoke(surfaceId, savedNote)
    }
}
