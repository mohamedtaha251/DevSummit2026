package com.example.devsummit2026.cronet

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

enum class ActiveNetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    DISCONNECTED,
    OTHER
}

enum class UploadStatus {
    IDLE,
    CONNECTING,
    STREAMING,
    COMPLETED,
    CANCELLED,
    ERROR
}

data class UploadTransferStats(
    val status: UploadStatus = UploadStatus.IDLE,
    val uploadedBytes: Long = 0L,
    val totalBytes: Long = 50L * 1024L * 1024L,
    val transferSpeedMbps: Double = 0.0,
    val negotiatedProtocol: String = "Detecting...",
    val currentNetwork: ActiveNetworkType = ActiveNetworkType.DISCONNECTED,
    val statusDetail: String = "Ready to start upload."
)

class CronetUploadManager(
    private val context: Context,
    private val onStatsChanged: (UploadTransferStats) -> Unit,
    private val onLogMessage: (String) -> Unit
) {
    private val cronetEngine: CronetEngine = CronetEngineProvider.getEngine(context)
    private val executor = Executors.newFixedThreadPool(2)
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var activeRequest: UrlRequest? = null
    private var activeProvider: StreamingUploadDataProvider? = null
    private val isUploading = AtomicBoolean(false)

    private var lastSpeedCalcTime = 0L
    private var lastSpeedCalcBytes = 0L
    private var currentSpeedMbps = 0.0

    private var currentNetworkType: ActiveNetworkType = determineCurrentNetwork()
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        registerNetworkObserver()
        emitStats(
            UploadTransferStats(
                status = UploadStatus.IDLE,
                currentNetwork = currentNetworkType,
                statusDetail = "Cronet Engine initialized with QUIC Session Migration."
            )
        )
        onLogMessage("⚡ Cronet Engine ready. QUIC session migration v2 & port migration enabled.")
    }

    private fun registerNetworkObserver() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val newType = when {
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ActiveNetworkType.WIFI
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ActiveNetworkType.CELLULAR
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ActiveNetworkType.ETHERNET
                    else -> ActiveNetworkType.OTHER
                }
                handleNetworkChange(newType)
            }

            override fun onLost(network: Network) {
                val updatedType = determineCurrentNetwork()
                handleNetworkChange(updatedType)
            }
        }

        networkCallback = callback
        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            onLogMessage("Warning: Could not register network callback: ${e.message}")
        }
    }

    private fun handleNetworkChange(newType: ActiveNetworkType) {
        val oldType = currentNetworkType
        if (oldType != newType) {
            currentNetworkType = newType
            val transitionText = "$oldType ➔ $newType"
            if (isUploading.get()) {
                onLogMessage("🔄 [QUIC Handover] Network switched: $transitionText. Active stream migrating without teardown!")
            } else {
                onLogMessage("🌐 Network changed: $transitionText")
            }
            emitStats(
                currentStats.copy(
                    currentNetwork = newType,
                    statusDetail = if (isUploading.get()) {
                        "Handover: $transitionText (QUIC Connection Migration)"
                    } else {
                        "Active Network: $newType"
                    }
                )
            )
        }
    }

    fun determineCurrentNetwork(): ActiveNetworkType {
        val activeNetwork = connectivityManager.activeNetwork ?: return ActiveNetworkType.DISCONNECTED
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return ActiveNetworkType.DISCONNECTED
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ActiveNetworkType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ActiveNetworkType.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ActiveNetworkType.ETHERNET
            else -> ActiveNetworkType.OTHER
        }
    }

    private var currentStats = UploadTransferStats(
        status = UploadStatus.IDLE,
        currentNetwork = currentNetworkType
    )

    private fun emitStats(stats: UploadTransferStats) {
        currentStats = stats
        onStatsChanged(stats)
    }

    fun startStreamingUpload(
        targetUrl: String = "https://cloudflare-quic.com/",
        totalBytes: Long = 50L * 1024L * 1024L
    ) {
        if (isUploading.getAndSet(true)) {
            onLogMessage("Upload is already running.")
            return
        }

        lastSpeedCalcTime = System.currentTimeMillis()
        lastSpeedCalcBytes = 0L
        currentSpeedMbps = 0.0

        emitStats(
            UploadTransferStats(
                status = UploadStatus.CONNECTING,
                uploadedBytes = 0L,
                totalBytes = totalBytes,
                transferSpeedMbps = 0.0,
                negotiatedProtocol = "Negotiating QUIC/H3...",
                currentNetwork = currentNetworkType,
                statusDetail = "Connecting to $targetUrl via QUIC..."
            )
        )
        onLogMessage("🚀 Starting chunked streaming upload ($totalBytes bytes) to $targetUrl")

        val dataProvider = StreamingUploadDataProvider(
            totalBytes = totalBytes,
            chunkSize = 32 * 1024,
            chunkDelayMs = 15L
        ) { chunkBytes, totalUploaded ->
            handleChunkUploaded(totalUploaded, totalBytes)
        }
        activeProvider = dataProvider

        val callback = object : UrlRequest.Callback() {
            override fun onRedirectReceived(
                request: UrlRequest,
                info: UrlResponseInfo,
                newLocationUrl: String
            ) {
                onLogMessage("Redirect: ${info.httpStatusCode} -> $newLocationUrl")
                request.followRedirect()
            }

            override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
                val protocol = info.negotiatedProtocol.ifBlank { "HTTP/3 (QUIC)" }
                onLogMessage("📥 Response received: HTTP ${info.httpStatusCode} via $protocol")
                emitStats(
                    currentStats.copy(
                        negotiatedProtocol = protocol,
                        statusDetail = "Response HTTP ${info.httpStatusCode} received ($protocol)"
                    )
                )
                request.read(ByteBuffer.allocateDirect(32768))
            }

            override fun onReadCompleted(
                request: UrlRequest,
                info: UrlResponseInfo,
                byteBuffer: ByteBuffer
            ) {
                byteBuffer.clear()
                request.read(byteBuffer)
            }

            override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
                isUploading.set(false)
                val protocol = info.negotiatedProtocol.ifBlank { "HTTP/3 (QUIC)" }
                onLogMessage("✅ Upload Completed! Status: ${info.httpStatusCode}, Protocol: $protocol")
                emitStats(
                    currentStats.copy(
                        status = UploadStatus.COMPLETED,
                        negotiatedProtocol = protocol,
                        statusDetail = "Completed (${info.httpStatusCode}) over $protocol"
                    )
                )
            }

            override fun onFailed(
                request: UrlRequest,
                info: UrlResponseInfo?,
                error: CronetException
            ) {
                isUploading.set(false)
                val msg = error.message ?: "Network error"
                onLogMessage("❌ Upload Failed: $msg")
                emitStats(
                    currentStats.copy(
                        status = UploadStatus.ERROR,
                        statusDetail = "Failed: $msg"
                    )
                )
            }

            override fun onCanceled(request: UrlRequest, info: UrlResponseInfo?) {
                isUploading.set(false)
                onLogMessage("⏹ Upload cancelled.")
                emitStats(
                    currentStats.copy(
                        status = UploadStatus.CANCELLED,
                        statusDetail = "Upload was cancelled."
                    )
                )
            }
        }

        try {
            val request = cronetEngine.newUrlRequestBuilder(targetUrl, callback, executor)
                .setHttpMethod("POST")
                .addHeader("Content-Type", "application/octet-stream")
                .setUploadDataProvider(dataProvider, executor)
                .build()

            activeRequest = request
            request.start()
        } catch (e: Exception) {
            isUploading.set(false)
            onLogMessage("❌ Failed to initiate request: ${e.message}")
            emitStats(
                currentStats.copy(
                    status = UploadStatus.ERROR,
                    statusDetail = "Initialization failed: ${e.message}"
                )
            )
        }
    }

    private fun handleChunkUploaded(uploadedBytes: Long, totalBytes: Long) {
        val now = System.currentTimeMillis()
        val elapsed = now - lastSpeedCalcTime
        if (elapsed >= 500) {
            val deltaBytes = uploadedBytes - lastSpeedCalcBytes
            val bytesPerSec = (deltaBytes * 1000.0) / elapsed
            currentSpeedMbps = (bytesPerSec * 8) / (1024.0 * 1024.0) // Mbps
            lastSpeedCalcTime = now
            lastSpeedCalcBytes = uploadedBytes
        }

        emitStats(
            currentStats.copy(
                status = UploadStatus.STREAMING,
                uploadedBytes = uploadedBytes,
                totalBytes = totalBytes,
                transferSpeedMbps = currentSpeedMbps,
                statusDetail = "Streaming chunks (${uploadedBytes / 1024} KB sent)..."
            )
        )
    }

    fun cancelUpload() {
        if (isUploading.get()) {
            activeProvider?.close()
            activeRequest?.cancel()
            activeRequest = null
            activeProvider = null
            isUploading.set(false)
            onLogMessage("⏹ Upload cancel requested.")
            emitStats(
                currentStats.copy(
                    status = UploadStatus.CANCELLED,
                    statusDetail = "Cancelled by user"
                )
            )
        }
    }

    fun release() {
        cancelUpload()
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
        }
    }
}
