package com.example.devsummit2026.ui.qwenagent

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.devsummit2026.appfunctions.data.NotesRepository
import com.example.devsummit2026.appfunctions.model.Note
import com.example.devsummit2026.ui.qwen.ModelState
import com.example.devsummit2026.ui.qwenagent.a2ui.NotesA2uiManager
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private const val TAG = "QwenAgentViewModel"
private const val MODEL_FILENAME = "qwen2.5-0.5b-instruct.bin"
private const val HUGGING_FACE_DOWNLOAD_URL =
    "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task"

sealed interface AgentStep {
    data class Thinking(val message: String = "Analyzing request...") : AgentStep
    data class ExecutingAction(val actionName: String, val actionDetails: String) : AgentStep
    data class Success(val resultSummary: String) : AgentStep
    data class Error(val message: String) : AgentStep
}

sealed interface ToolCallResult {
    data class CreateNote(val title: String, val content: String) : ToolCallResult
    data class SearchNotes(val query: String, val isQuestion: Boolean = false) : ToolCallResult
    data class GetNotesCount(val message: String? = null) : ToolCallResult
    data class Chat(val reply: String) : ToolCallResult
}

data class AgentMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val fromUser: Boolean,
    val step: AgentStep? = null,
    val actionType: String? = null,
    val createdNote: Note? = null,
    val searchResults: List<Note>? = null,
    val a2uiSurfaceId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class QwenAgentUiState(
    val modelState: ModelState = ModelState.Checking,
    val activeBackend: String = "Unknown",
    val messages: List<AgentMessage> = emptyList(),
    val inputText: String = "",
    val isBusy: Boolean = false,
    val currentStep: AgentStep? = null,
    val modelFilePath: String = "",
    val adbPushCommand: String = ""
)

class QwenAgentViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(QwenAgentUiState())
    val uiState: StateFlow<QwenAgentUiState> = _uiState.asStateFlow()

    val repositoryNotes: StateFlow<List<Note>> = NotesRepository.notes

    val a2uiManager: NotesA2uiManager = NotesA2uiManager(
        scope = viewModelScope,
        onNoteSavedCallback = { surfaceId, savedNote ->
            _uiState.update { state ->
                val updated = state.messages.map { msg ->
                    if (msg.a2uiSurfaceId == surfaceId) {
                        msg.copy(
                            createdNote = savedNote,
                            step = AgentStep.Success("Saved note: \"${savedNote.title}\"")
                        )
                    } else msg
                }
                state.copy(messages = updated)
            }
        }
    )

    private var llmInference: LlmInference? = null
    private var downloadJob: Job? = null

    val targetModelFile: File
        get() = File(getApplication<Application>().filesDir, MODEL_FILENAME)

    init {
        val adbCmd = "adb push Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task /data/local/tmp/$MODEL_FILENAME"
        _uiState.update {
            it.copy(
                modelFilePath = targetModelFile.absolutePath,
                adbPushCommand = adbCmd
            )
        }
        checkAndPrepareModel()
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun checkAndPrepareModel() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(modelState = ModelState.Checking) }

            // 1. Check if model exists in internal storage (filesDir)
            if (targetModelFile.exists() && targetModelFile.length() > 10_000_000L) {
                Log.i(TAG, "Found valid model in internal storage: ${targetModelFile.absolutePath}")
                initializeEngine(targetModelFile)
                return@launch
            }

            // 2. Check /data/local/tmp/
            val candidatePaths = listOf(
                File("/data/local/tmp/$MODEL_FILENAME"),
                File("/data/local/tmp/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task")
            )
            for (candidate in candidatePaths) {
                if (candidate.exists() && candidate.canRead() && candidate.length() > 10_000_000L) {
                    Log.i(TAG, "Found candidate model at: ${candidate.absolutePath}. Copying...")
                    _uiState.update {
                        it.copy(modelState = ModelState.Copying("Copying model from /data/local/tmp/..."))
                    }
                    try {
                        candidate.inputStream().use { input ->
                            targetModelFile.outputStream().use { output ->
                                input.copyTo(output, bufferSize = 64 * 1024)
                            }
                        }
                        initializeEngine(targetModelFile)
                        return@launch
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not copy, trying direct use", e)
                        initializeEngine(candidate)
                        return@launch
                    }
                }
            }

            Log.i(TAG, "Model file not found. Prompting download.")
            _uiState.update { it.copy(modelState = ModelState.Missing) }
        }
    }

    private fun initializeEngine(modelFile: File) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(modelState = ModelState.Initializing("CPU")) }
            val effectiveModelFile = prepareEffectiveModelFile(modelFile)

            try {
                llmInference?.close()
            } catch (_: Exception) { }
            llmInference = null

            // Primary attempt: CPU backend (XNNPACK)
            try {
                Log.i(TAG, "Initializing MediaPipe GenAI with CPU backend: ${effectiveModelFile.absolutePath}")
                val cpuOptions = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(effectiveModelFile.absolutePath)
                    .setMaxTokens(1024)
                    .setPreferredBackend(LlmInference.Backend.CPU)
                    .build()

                val engine = LlmInference.createFromOptions(getApplication(), cpuOptions)
                llmInference = engine
                _uiState.update {
                    it.copy(
                        modelState = ModelState.Ready,
                        activeBackend = "CPU (XNNPACK)"
                    )
                }
                Log.i(TAG, "MediaPipe GenAI successfully initialized on CPU (XNNPACK)!")
                return@launch
            } catch (e: Throwable) {
                Log.w(TAG, "CPU initialization failed. Trying GPU backend...", e)
            }

            // Fallback attempt: GPU backend
            try {
                _uiState.update { it.copy(modelState = ModelState.Initializing("GPU (Fallback)")) }
                val gpuOptions = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(effectiveModelFile.absolutePath)
                    .setMaxTokens(1024)
                    .setPreferredBackend(LlmInference.Backend.GPU)
                    .build()

                val engine = LlmInference.createFromOptions(getApplication(), gpuOptions)
                llmInference = engine
                _uiState.update {
                    it.copy(
                        modelState = ModelState.Ready,
                        activeBackend = "GPU"
                    )
                }
                Log.i(TAG, "MediaPipe GenAI successfully initialized on GPU!")
            } catch (e: Throwable) {
                Log.e(TAG, "Engine initialization failed on both CPU and GPU", e)
                _uiState.update {
                    it.copy(
                        modelState = ModelState.Error(
                            "Failed to initialize MediaPipe engine: ${e.localizedMessage ?: e.message}"
                        )
                    )
                }
            }
        }
    }

    private fun prepareEffectiveModelFile(sourceFile: File): File {
        if (sourceFile.name.endsWith(".task")) return sourceFile
        val taskFile = File(sourceFile.parentFile, sourceFile.nameWithoutExtension + ".task")
        if (!taskFile.exists()) {
            try {
                android.system.Os.symlink(sourceFile.absolutePath, taskFile.absolutePath)
                Log.i(TAG, "Created symlink to .task: ${taskFile.absolutePath}")
            } catch (e: Exception) {
                Log.w(TAG, "Symlink creation failed, using source directly", e)
                return sourceFile
            }
        }
        return taskFile
    }

    fun startDownload() {
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            val tmpFile = File(getApplication<Application>().filesDir, "$MODEL_FILENAME.tmp")
            try {
                _uiState.update {
                    it.copy(modelState = ModelState.Downloading(0f, 0L, 0L))
                }
                downloadFileWithResume(HUGGING_FACE_DOWNLOAD_URL, tmpFile) { downloaded, total ->
                    val fraction = if (total > 0) downloaded.toFloat() / total.toFloat() else 0f
                    _uiState.update {
                        it.copy(modelState = ModelState.Downloading(fraction, downloaded, total))
                    }
                }
                if (targetModelFile.exists()) targetModelFile.delete()
                val renamed = tmpFile.renameTo(targetModelFile)
                if (!renamed) {
                    tmpFile.copyTo(targetModelFile, overwrite = true)
                    tmpFile.delete()
                }
                initializeEngine(targetModelFile)
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                _uiState.update {
                    it.copy(modelState = ModelState.Error("Download failed: ${e.localizedMessage ?: e.message}"))
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        val tmpFile = File(getApplication<Application>().filesDir, "$MODEL_FILENAME.tmp")
        if (tmpFile.exists()) tmpFile.delete()
        _uiState.update { it.copy(modelState = ModelState.Missing) }
    }

    private suspend fun downloadFileWithResume(
        urlString: String,
        destinationFile: File,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        var downloadedBytes = if (destinationFile.exists()) destinationFile.length() else 0L
        var totalBytes = -1L
        var remainingRetries = 20

        while (remainingRetries > 0) {
            if (!coroutineContext.isActive) throw InterruptedException("Download cancelled")
            try {
                var currentUrl = urlString
                var connection: HttpURLConnection? = null
                var redirects = 0
                while (redirects < 6) {
                    val url = URL(currentUrl)
                    connection = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 30_000
                        readTimeout = 30_000
                        instanceFollowRedirects = true
                        if (downloadedBytes > 0) {
                            setRequestProperty("Range", "bytes=$downloadedBytes-")
                        }
                        setRequestProperty("User-Agent", "Mozilla/5.0 Android LiteRT DevSummit2026")
                    }
                    connection.connect()
                    val code = connection.responseCode
                    if (code in 300..399) {
                        val newUrl = connection.getHeaderField("Location") ?: break
                        connection.disconnect()
                        currentUrl = newUrl
                        redirects++
                        continue
                    }
                    break
                }

                val conn = connection ?: throw IllegalStateException("Failed to open connection")
                try {
                    val code = conn.responseCode
                    val isPartial = code == 206
                    if (code !in 200..299) {
                        throw IllegalStateException("HTTP error $code: ${conn.responseMessage}")
                    }

                    if (isPartial) {
                        val cr = conn.getHeaderField("Content-Range")
                        if (cr != null && cr.contains("/")) {
                            totalBytes = cr.substringAfter("/").trim().toLongOrNull() ?: totalBytes
                        }
                    } else {
                        totalBytes = conn.contentLengthLong
                        downloadedBytes = 0L
                    }

                    if (totalBytes > 0 && downloadedBytes >= totalBytes) break

                    val append = isPartial && downloadedBytes > 0
                    conn.inputStream.use { input ->
                        FileOutputStream(destinationFile, append).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var bytesRead: Int
                            var lastUpdate = System.currentTimeMillis()
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                if (!coroutineContext.isActive) throw InterruptedException("Download cancelled")
                                output.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead
                                val now = System.currentTimeMillis()
                                if (now - lastUpdate > 150) {
                                    lastUpdate = now
                                    onProgress(downloadedBytes, totalBytes)
                                }
                            }
                            output.flush()
                        }
                    }

                    if (totalBytes <= 0 || downloadedBytes >= totalBytes) {
                        onProgress(downloadedBytes, totalBytes)
                        break
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                if (e is InterruptedException) throw e
                remainingRetries--
                if (remainingRetries <= 0) throw e
                delay(1000)
            }
        }
    }

    /**
     * Executes user prompt with intelligent, robust intent understanding:
     * - Greetings ("hello", "hi") -> responds with friendly normal text (no JSON, no stepper)
     * - Count queries ("how many notes I have?") -> answers immediately with current count
     * - Existence/search queries ("have you note contain Taha?", "do you have notes about...") -> answers Yes/No + search results
     * - Explicit note creation ("create note...", "add note...") -> creates note in NotesRepository with action stepper
     * - Model fallback: runs Qwen 2.5 ChatML inference and parses tool response cleanly
     */
    fun sendPrompt(userPrompt: String = _uiState.value.inputText) {
        val trimmed = userPrompt.trim()
        val engine = llmInference
        if (trimmed.isBlank() || _uiState.value.isBusy) return

        val userMessage = AgentMessage(
            text = trimmed,
            fromUser = true
        )

        // 1. GREETING INTENT PRE-CHECK: ("hello", "hi", "hey", etc.)
        val greetingPattern = Regex("^(hello|hi|hey|good\\s+(morning|afternoon|evening)|howdy)\\b.*", RegexOption.IGNORE_CASE)
        if (greetingPattern.matches(trimmed)) {
            val assistantReply = AgentMessage(
                text = "Hello! How can I help you with your notes today?",
                fromUser = false,
                actionType = "chat",
                step = null // Pure natural text response, no stepper badge
            )
            _uiState.update {
                it.copy(
                    messages = it.messages + userMessage + assistantReply,
                    inputText = ""
                )
            }
            return
        }

        // 2. NOTES COUNT QUERY PRE-CHECK: ("how many notes I have??", "count notes", etc.)
        val countPattern = Regex(".*(how\\s+many\\s+notes|count\\s+(my\\s+)?notes|number\\s+of\\s+notes|total\\s+notes).*", RegexOption.IGNORE_CASE)
        if (countPattern.matches(trimmed)) {
            val totalNotes = NotesRepository.notes.value.size
            val countReplyText = if (totalNotes == 0) {
                "You don't have any notes stored yet. Ask me to create one!"
            } else {
                "You currently have $totalNotes note${if (totalNotes == 1) "" else "s"} in your repository."
            }
            val assistantReply = AgentMessage(
                text = countReplyText,
                fromUser = false,
                actionType = "getNotesCount",
                step = AgentStep.Success("Count: $totalNotes note(s)")
            )
            _uiState.update {
                it.copy(
                    messages = it.messages + userMessage + assistantReply,
                    inputText = ""
                )
            }
            return
        }

        // 3. EXISTENCE / SEARCH QUESTION INTENT PRE-CHECK:
        // E.g. "have u note can contain taha", "do you have a note about meeting", "is there any note with X"
        val questionSearchPattern = Regex(".*(have\\s+(you|u)|do\\s+(we|you|i)\\s+have|is\\s+there|any)\\s+(a\\s+)?note.*", RegexOption.IGNORE_CASE)
        val extractedKeyword = extractKeywordFromQuery(trimmed)
        if (questionSearchPattern.matches(trimmed) && extractedKeyword.isNotBlank()) {
            executeSearchAction(userMessage, extractedKeyword, isYesNoQuestion = true)
            return
        }

        // 4. NOTE CREATION INTENT PRE-CHECK: ("create note...", "add note...", etc.)
        val createPattern = Regex("^(create|add|new)\\s+note\\b.*", RegexOption.IGNORE_CASE)
        if (createPattern.matches(trimmed)) {
            val contentPart = if (trimmed.contains(":")) {
                trimmed.substringAfter(":").trim()
            } else {
                trimmed.replace(Regex("^(create|add|new)\\s+note\\s*(to|about|for)?\\s*", RegexOption.IGNORE_CASE), "").trim()
            }
            val title = contentPart.take(30).ifBlank { "New Note" }
            val content = contentPart.ifBlank { title }

            val assistantMessageId = UUID.randomUUID().toString()
            val surfaceId = a2uiManager.createNoteSurface(title, content)
            val successStep = AgentStep.Success("A2UI Component Ready")
            val assistantReply = AgentMessage(
                id = assistantMessageId,
                text = "I've generated an interactive A2UI component. Review or edit before saving:",
                fromUser = false,
                actionType = "a2uiNoteForm",
                step = successStep,
                a2uiSurfaceId = surfaceId
            )
            _uiState.update {
                it.copy(
                    messages = it.messages + userMessage + assistantReply,
                    inputText = ""
                )
            }
            return
        }

        // If engine not ready yet, return
        if (engine == null) return

        // 4. GENERAL LLM INFERENCE FOR ALL OTHER QUERIES (create note, specific queries, etc.)
        val assistantMessageId = UUID.randomUUID().toString()
        val initialThinkingStep = AgentStep.Thinking("Analyzing request...")
        val assistantPlaceholder = AgentMessage(
            id = assistantMessageId,
            text = "",
            fromUser = false,
            step = initialThinkingStep
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + assistantPlaceholder,
                inputText = "",
                isBusy = true,
                currentStep = initialThinkingStep
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val prompt = buildAgentChatMlPrompt(trimmed, NotesRepository.notes.value.size)
                Log.d(TAG, "Calling LlmInference with ChatML prompt:\n$prompt")

                val rawResponse = engine.generateResponse(prompt)
                Log.d(TAG, "LlmInference raw response: $rawResponse")

                val parsedTool = parseToolCall(rawResponse, trimmed)
                Log.d(TAG, "Parsed tool result: $parsedTool")

                when (parsedTool) {
                    is ToolCallResult.CreateNote -> {
                        val executingStep = AgentStep.ExecutingAction(
                            actionName = "A2UI Form",
                            actionDetails = "Rendering component: \"${parsedTool.title}\""
                        )
                        updateAssistantMessage(assistantMessageId) {
                            it.copy(
                                step = executingStep,
                                actionType = "a2uiNoteForm"
                            )
                        }
                        _uiState.update { it.copy(currentStep = executingStep) }

                        delay(400)

                        val surfaceId = a2uiManager.createNoteSurface(
                            title = parsedTool.title,
                            content = parsedTool.content
                        )

                        val successStep = AgentStep.Success("A2UI Component Ready")
                        updateAssistantMessage(assistantMessageId) {
                            it.copy(
                                step = successStep,
                                text = "I've generated an interactive A2UI component. Review or edit before saving:",
                                a2uiSurfaceId = surfaceId
                            )
                        }
                        _uiState.update { it.copy(currentStep = successStep) }
                    }

                    is ToolCallResult.SearchNotes -> {
                        val executingStep = AgentStep.ExecutingAction(
                            actionName = "searchNotes",
                            actionDetails = "Query: \"${parsedTool.query}\""
                        )
                        updateAssistantMessage(assistantMessageId) {
                            it.copy(
                                step = executingStep,
                                actionType = "searchNotes"
                            )
                        }
                        _uiState.update { it.copy(currentStep = executingStep) }

                        delay(500)

                        val results = NotesRepository.searchNotes(parsedTool.query)

                        val responseText = if (parsedTool.isQuestion) {
                            if (results.isNotEmpty()) {
                                "Yes, you have ${results.size} note${if (results.size == 1) "" else "s"} containing \"${parsedTool.query}\":"
                            } else {
                                "No, you don't have any notes containing \"${parsedTool.query}\"."
                            }
                        } else {
                            if (results.isNotEmpty()) {
                                "Found ${results.size} note(s) matching \"${parsedTool.query}\":"
                            } else {
                                "No notes found matching \"${parsedTool.query}\"."
                            }
                        }

                        val successStep = AgentStep.Success(
                            if (results.isNotEmpty()) {
                                "Found ${results.size} note${if (results.size == 1) "" else "s"} for \"${parsedTool.query}\""
                            } else {
                                "No notes matching \"${parsedTool.query}\""
                            }
                        )
                        updateAssistantMessage(assistantMessageId) {
                            it.copy(
                                step = successStep,
                                text = responseText,
                                searchResults = results
                            )
                        }
                        _uiState.update { it.copy(currentStep = successStep) }
                    }

                    is ToolCallResult.GetNotesCount -> {
                        val totalNotes = NotesRepository.notes.value.size
                        val countText = if (totalNotes == 0) {
                            "You don't have any notes stored yet. Ask me to create one!"
                        } else {
                            "You currently have $totalNotes note${if (totalNotes == 1) "" else "s"} in your repository."
                        }
                        val successStep = AgentStep.Success("Count: $totalNotes note(s)")
                        updateAssistantMessage(assistantMessageId) {
                            it.copy(
                                step = successStep,
                                actionType = "getNotesCount",
                                text = countText
                            )
                        }
                        _uiState.update { it.copy(currentStep = successStep) }
                    }

                    is ToolCallResult.Chat -> {
                        val cleanReply = if (parsedTool.reply.isBlank() || parsedTool.reply == "{}") {
                            "Hello! How can I help you with your notes today?"
                        } else {
                            parsedTool.reply
                        }
                        updateAssistantMessage(assistantMessageId) {
                            it.copy(
                                step = null, // No stepper badge for pure chat
                                actionType = "chat",
                                text = cleanReply
                            )
                        }
                        _uiState.update { it.copy(currentStep = null) }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Agent execution error", e)
                val errorStep = AgentStep.Error("Error: ${e.localizedMessage ?: e.message}")
                updateAssistantMessage(assistantMessageId) {
                    it.copy(
                        step = errorStep,
                        text = "I encountered an error executing your request: ${e.localizedMessage ?: e.message}"
                    )
                }
                _uiState.update { it.copy(currentStep = errorStep) }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun executeSearchAction(userMessage: AgentMessage, query: String, isYesNoQuestion: Boolean) {
        val assistantMessageId = UUID.randomUUID().toString()
        val executingStep = AgentStep.ExecutingAction("searchNotes", "Query: \"$query\"")
        val placeholder = AgentMessage(
            id = assistantMessageId,
            text = "",
            fromUser = false,
            step = executingStep,
            actionType = "searchNotes"
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + placeholder,
                inputText = "",
                isBusy = true,
                currentStep = executingStep
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            delay(400)
            val results = NotesRepository.searchNotes(query)
            val responseText = if (isYesNoQuestion) {
                if (results.isNotEmpty()) {
                    "Yes, you have ${results.size} note${if (results.size == 1) "" else "s"} containing \"$query\":"
                } else {
                    "No, you don't have any notes containing \"$query\"."
                }
            } else {
                if (results.isNotEmpty()) {
                    "Found ${results.size} note(s) matching \"$query\":"
                } else {
                    "No notes found matching \"$query\"."
                }
            }

            val successStep = AgentStep.Success(
                if (results.isNotEmpty()) {
                    "Found ${results.size} note(s) for \"$query\""
                } else {
                    "No notes matching \"$query\""
                }
            )

            updateAssistantMessage(assistantMessageId) {
                it.copy(
                    step = successStep,
                    text = responseText,
                    searchResults = results
                )
            }
            _uiState.update {
                it.copy(
                    currentStep = successStep,
                    isBusy = false
                )
            }
        }
    }

    private fun extractKeywordFromQuery(prompt: String): String {
        val lower = prompt.lowercase().trim().removeSuffix("?").removeSuffix(".").trim()
        val patterns = listOf(
            Regex(".*(?:contain(?:s|ing)?|can\\s+contain)\\s+['\"]?([a-zA-Z0-9_-]+)['\"]?.*"),
            Regex(".*(?:about|named|titled|with|for)\\s+['\"]?([a-zA-Z0-9_-]+)['\"]?.*")
        )
        for (pattern in patterns) {
            val match = pattern.find(lower)
            if (match != null && match.groupValues.size > 1) {
                val kw = match.groupValues[1].trim()
                if (kw.isNotEmpty() && kw !in listOf("note", "notes", "a", "an", "the", "me", "you", "u")) {
                    return kw
                }
            }
        }
        return ""
    }

    private fun updateAssistantMessage(id: String, transform: (AgentMessage) -> AgentMessage) {
        _uiState.update { state ->
            val updated = state.messages.map { msg ->
                if (msg.id == id) transform(msg) else msg
            }
            state.copy(messages = updated)
        }
    }

    private fun buildAgentChatMlPrompt(userPrompt: String, totalNotes: Int): String {
        return buildString {
            append("<|im_start|>system\n")
            append("You are an in-app Notes Agent.\n")
            append("Total notes in repository: $totalNotes.\n\n")
            append("Reply with a single JSON object matching one of these tools:\n")
            append("1. {\"action\": \"createNote\", \"title\": \"<short title>\", \"content\": \"<details>\"}\n")
            append("   (Use ONLY when user explicitly asks to CREATE, ADD, or SAVE a note)\n")
            append("2. {\"action\": \"searchNotes\", \"query\": \"<search keyword>\", \"isQuestion\": true}\n")
            append("   (Use when user asks if a note exists or searches for notes, e.g. \"have you note contain X?\", \"search notes\")\n")
            append("3. {\"action\": \"getNotesCount\"}\n")
            append("   (Use when user asks how many notes they have)\n")
            append("4. {\"action\": \"chat\", \"reply\": \"<friendly message>\"}\n")
            append("   (Use for greetings and general questions)\n\n")
            append("CRITICAL: Questions like \"Have you note contain X?\" are search queries, NEVER createNote!\n")
            append("Output ONLY raw JSON. No markdown.\n")
            append("<|im_end|>\n")
            append("<|im_start|>user\n")
            append(userPrompt).append("\n")
            append("<|im_end|>\n")
            append("<|im_start|>assistant\n")
        }
    }

    private fun parseToolCall(response: String, userPrompt: String): ToolCallResult {
        val trimmed = response.trim()
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')

        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            val jsonCandidate = trimmed.substring(firstBrace, lastBrace + 1)
            try {
                val json = JSONObject(jsonCandidate)
                val action = json.optString("action").trim()
                val isQuestion = json.optBoolean("isQuestion", false) || userPrompt.contains("?") ||
                        userPrompt.startsWith("have", ignoreCase = true) ||
                        userPrompt.startsWith("do you", ignoreCase = true)

                when (action) {
                    "createNote" -> {
                        // Guard against false createNote on questions
                        if (userPrompt.startsWith("have", ignoreCase = true) ||
                            userPrompt.contains("contain", ignoreCase = true) && userPrompt.contains("?")
                        ) {
                            val kw = extractKeywordFromQuery(userPrompt)
                            if (kw.isNotBlank()) {
                                return ToolCallResult.SearchNotes(kw, isQuestion = true)
                            }
                        }
                        val title = json.optString("title").ifBlank { "New Note" }
                        val content = json.optString("content").ifBlank { title }
                        return ToolCallResult.CreateNote(title, content)
                    }
                    "searchNotes" -> {
                        val query = json.optString("query").ifBlank { extractKeywordFromQuery(userPrompt) }
                        return ToolCallResult.SearchNotes(query, isQuestion)
                    }
                    "getNotesCount" -> {
                        return ToolCallResult.GetNotesCount()
                    }
                    "chat" -> {
                        val reply = json.optString("reply").ifBlank { "Hello! How can I help you with your notes today?" }
                        return ToolCallResult.Chat(reply)
                    }
                    else -> {
                        if (json.has("title") || json.has("content")) {
                            val title = json.optString("title", "New Note")
                            val content = json.optString("content", title)
                            return ToolCallResult.CreateNote(title, content)
                        }
                        if (json.has("query")) {
                            return ToolCallResult.SearchNotes(json.optString("query"), isQuestion)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed parsing JSON substring: $jsonCandidate", e)
            }
        }

        // Check if user is asking how many notes
        if (userPrompt.contains("how many", ignoreCase = true)) {
            return ToolCallResult.GetNotesCount()
        }

        // Clean plain text fallback
        val cleanFallback = trimmed
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        if (cleanFallback == "{}" || cleanFallback.isBlank()) {
            return ToolCallResult.Chat("Hello! How can I help you with your notes today?")
        }
        return ToolCallResult.Chat(cleanFallback)
    }

    fun clearHistory() {
        _uiState.update {
            it.copy(
                messages = emptyList(),
                currentStep = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing LlmInference", e)
        }
        llmInference = null
    }
}
