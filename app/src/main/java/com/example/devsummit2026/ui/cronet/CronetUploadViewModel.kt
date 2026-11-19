package com.example.devsummit2026.ui.cronet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.devsummit2026.cronet.ActiveNetworkType
import com.example.devsummit2026.cronet.CronetUploadManager
import com.example.devsummit2026.cronet.UploadStatus
import com.example.devsummit2026.cronet.UploadTransferStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CronetUploadUiState(
    val stats: UploadTransferStats = UploadTransferStats(),
    val targetUrl: String = "https://cloudflare-quic.com/",
    val eventLogs: List<String> = emptyList()
)

class CronetUploadViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CronetUploadUiState())
    val uiState: StateFlow<CronetUploadUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val uploadManager = CronetUploadManager(
        context = application.applicationContext,
        onStatsChanged = { newStats ->
            _uiState.update { it.copy(stats = newStats) }
        },
        onLogMessage = { message ->
            addLog(message)
        }
    )

    private fun addLog(message: String) {
        val timestamp = timeFormat.format(Date())
        val entry = "[$timestamp] $message"
        _uiState.update { current ->
            current.copy(eventLogs = current.eventLogs + entry)
        }
    }

    fun startUpload() {
        val url = _uiState.value.targetUrl.trim()
        uploadManager.startStreamingUpload(
            targetUrl = if (url.isNotBlank()) url else "https://cloudflare-quic.com/",
            totalBytes = 50L * 1024L * 1024L // 50 MB
        )
    }

    fun cancelUpload() {
        uploadManager.cancelUpload()
    }

    fun onTargetUrlChanged(newUrl: String) {
        _uiState.update { it.copy(targetUrl = newUrl) }
    }

    fun clearLogs() {
        _uiState.update { it.copy(eventLogs = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        uploadManager.release()
    }
}
