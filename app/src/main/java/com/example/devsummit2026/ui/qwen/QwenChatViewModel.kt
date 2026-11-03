package com.example.devsummit2026.ui.qwen

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.devsummit2026.model.ChatMessage
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "QwenChatViewModel"
private const val MODEL_FILENAME = "qwen2.5-0.5b-instruct.bin"
private const val HUGGING_FACE_DOWNLOAD_URL =
    "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task"

sealed interface ModelState {
    data object Checking : ModelState
    data object Missing : ModelState
    data class Copying(val progressText: String) : ModelState
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : ModelState
    data class Initializing(val backend: String) : ModelState
    data object Ready : ModelState
    data class Error(val message: String) : ModelState
}

data class QwenChatUiState(
    val modelState: ModelState = ModelState.Checking,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val activeBackend: String = "Unknown",
    val modelFilePath: String = "",
    val adbPushCommand: String = ""
)

class QwenChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(QwenChatUiState())
    val uiState: StateFlow<QwenChatUiState> = _uiState.asStateFlow()

    private var llmInference: LlmInference? = null
    private var downloadJob: Job? = null

    val targetModelFile: File
        get() = File(getApplication<Application>().filesDir, MODEL_FILENAME)

    init {
        val app = getApplication<Application>()
        val adbCmd = "adb push Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task /data/local/tmp/$MODEL_FILENAME"
        _uiState.update {
            it.copy(
                modelFilePath = targetModelFile.absolutePath,
                adbPushCommand = adbCmd
            )
        }
        Log.i(TAG, "Recommended ADB push command:\n$adbCmd")

        checkAndPrepareModel()
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun checkAndPrepareModel() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(modelState = ModelState.Checking) }

            // 1. Check if model already exists in app internal storage (filesDir)
            if (targetModelFile.exists() && targetModelFile.length() > 10_000_000L) {
                Log.i(TAG, "Found valid model in internal storage: ${targetModelFile.absolutePath}")
                initializeEngine(targetModelFile)
                return@launch
            }

            // 2. Check if pushed manually into /data/local/tmp/
            val candidatePaths = listOf(
                File("/data/local/tmp/$MODEL_FILENAME"),
                File("/data/local/tmp/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task")
            )
            for (candidate in candidatePaths) {
                if (candidate.exists() && candidate.canRead() && candidate.length() > 10_000_000L) {
                    Log.i(TAG, "Found candidate model at: ${candidate.absolutePath}. Copying to internal storage...")
                    _uiState.update {
                        it.copy(modelState = ModelState.Copying("Copying model from /data/local/tmp/..."))
                    }
                    try {
                        candidate.inputStream().use { input ->
                            targetModelFile.outputStream().use { output ->
                                input.copyTo(output, bufferSize = 64 * 1024)
                            }
                        }
                        Log.i(TAG, "Model successfully copied to ${targetModelFile.absolutePath}")
                        initializeEngine(targetModelFile)
                        return@launch
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not copy from ${candidate.absolutePath}, attempting direct use", e)
                        initializeEngine(candidate)
                        return@launch
                    }
                }
            }

            // 3. Model is missing -> user can trigger download or push via adb
            Log.i(TAG, "Model file not found. Showing download prompt.")
            _uiState.update { it.copy(modelState = ModelState.Missing) }
        }
    }

    fun startDownload() {
        if (downloadJob?.isActive == true) return

        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            val tmpFile = File(getApplication<Application>().filesDir, "$MODEL_FILENAME.tmp")
            try {
                _uiState.update {
                    it.copy(
                        modelState = ModelState.Downloading(
                            progress = 0f,
                            downloadedBytes = 0L,
                            totalBytes = 0L
                        )
                    )
                }

                downloadFileWithResume(
                    urlString = HUGGING_FACE_DOWNLOAD_URL,
                    destinationFile = tmpFile
                ) { downloaded, total ->
                    val progressFraction = if (total > 0) downloaded.toFloat() / total.toFloat() else 0f
                    _uiState.update {
                        it.copy(
                            modelState = ModelState.Downloading(
                                progress = progressFraction,
                                downloadedBytes = downloaded,
                                totalBytes = total
                            )
                        )
                    }
                }

                // Download complete: atomically rename to final file
                if (targetModelFile.exists()) {
                    targetModelFile.delete()
                }
                val renamed = tmpFile.renameTo(targetModelFile)
                if (!renamed) {
                    tmpFile.copyTo(targetModelFile, overwrite = true)
                    tmpFile.delete()
                }

                Log.i(TAG, "Download finished successfully. File size: ${targetModelFile.length()} bytes")
                initializeEngine(targetModelFile)

            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                _uiState.update {
                    it.copy(
                        modelState = ModelState.Error("Download interrupted: ${e.localizedMessage ?: e.message}")
                    )
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        val tmpFile = File(getApplication<Application>().filesDir, "$MODEL_FILENAME.tmp")
        if (tmpFile.exists()) {
            tmpFile.delete()
        }
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

                    if (totalBytes > 0 && downloadedBytes >= totalBytes) {
                        break
                    }

                    val append = isPartial && downloadedBytes > 0
                    conn.inputStream.use { input ->
                        FileOutputStream(destinationFile, append).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var bytesRead: Int
                            var lastUpdate = System.currentTimeMillis()

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                if (!coroutineContext.isActive) {
                                    throw InterruptedException("Download cancelled")
                                }
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
                Log.w(TAG, "Connection interrupted at $downloadedBytes bytes, retrying...", e)
                remainingRetries--
                if (remainingRetries <= 0) throw e
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun initializeEngine(modelFile: File) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(modelState = ModelState.Initializing("CPU")) }

            // Ensure the file path passed to MediaPipe ends with .task if it's a task bundle
            val effectiveModelFile = prepareEffectiveModelFile(modelFile)

            // Clean up any previously opened engine
            try {
                llmInference?.close()
            } catch (_: Exception) { }
            llmInference = null

            // Primary attempt: CPU backend (optimized with XNNPACK for Qwen dynamic_int8 as recommended by LiteRT)
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
                Log.w(TAG, "CPU initialization failed (${e.message}). Trying GPU backend...", e)
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
        // MediaPipe requires .task extension to trigger LiteRT task bundle loader
        if (sourceFile.name.endsWith(".task")) {
            return sourceFile
        }

        val taskFile = File(sourceFile.parentFile, sourceFile.nameWithoutExtension + ".task")
        if (!taskFile.exists()) {
            try {
                android.system.Os.symlink(sourceFile.absolutePath, taskFile.absolutePath)
                Log.i(TAG, "Created symlink to .task: ${taskFile.absolutePath}")
            } catch (e: Exception) {
                Log.w(TAG, "Symlink creation failed, using source file directly", e)
                return sourceFile
            }
        }
        return taskFile
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        val engine = llmInference
        if (text.isBlank() || _uiState.value.isGenerating || engine == null) return

        val userMessage = ChatMessage(text = text, fromUser = true)
        val assistantPlaceholder = ChatMessage(text = "", fromUser = false)

        val updatedMessages = _uiState.value.messages + userMessage + assistantPlaceholder
        _uiState.update {
            it.copy(
                messages = updatedMessages,
                inputText = "",
                isGenerating = true
            )
        }

        val prompt = buildQwenPrompt(updatedMessages.dropLast(1))
        Log.d(TAG, "Sending prompt to Qwen:\n$prompt")

        viewModelScope.launch(Dispatchers.Default) {
            try {
                Log.i(TAG, "Calling generateResponse on CPU...")
                val responseText = engine.generateResponse(prompt)
                Log.i(TAG, "generateResponse returned: $responseText")

                // Stream response tokens/words smoothly to UI
                val words = responseText.split(Regex("(?<=\\s)"))
                val accumulated = StringBuilder()
                for (word in words) {
                    accumulated.append(word)
                    _uiState.update { state ->
                        val currentList = state.messages.toMutableList()
                        if (currentList.isNotEmpty() && !currentList.last().fromUser) {
                            currentList[currentList.lastIndex] = ChatMessage(
                                text = accumulated.toString(),
                                fromUser = false
                            )
                        }
                        state.copy(messages = currentList)
                    }
                    kotlinx.coroutines.delay(16)
                }

                _uiState.update { it.copy(isGenerating = false) }
            } catch (e: Throwable) {
                Log.e(TAG, "Error generating response", e)
                _uiState.update { state ->
                    val currentList = state.messages.toMutableList()
                    if (currentList.isNotEmpty() && !currentList.last().fromUser) {
                        currentList[currentList.lastIndex] = ChatMessage(
                            text = "Error generating response: ${e.localizedMessage ?: e.message}",
                            fromUser = false
                        )
                    }
                    state.copy(
                        messages = currentList,
                        isGenerating = false
                    )
                }
            }
        }
    }

    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList()) }
    }

    private fun buildQwenPrompt(messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        sb.append("<|im_start|>system\nYou are a helpful and concise AI assistant running completely on-device.<|im_end|>\n")
        for (msg in messages) {
            if (msg.fromUser) {
                sb.append("<|im_start|>user\n").append(msg.text).append("<|im_end|>\n")
            } else if (msg.text.isNotBlank()) {
                sb.append("<|im_start|>assistant\n").append(msg.text).append("<|im_end|>\n")
            }
        }
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing LlmInference engine", e)
        }
        llmInference = null
    }
}
