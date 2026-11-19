package com.example.devsummit2026.ui.cronet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.devsummit2026.cronet.ActiveNetworkType
import com.example.devsummit2026.cronet.UploadStatus
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CronetUploadScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CronetUploadViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val logListState = rememberLazyListState()

    // Auto-scroll to bottom of logs when new entries arrive
    LaunchedEffect(state.eventLogs.size) {
        if (state.eventLogs.isNotEmpty()) {
            logListState.animateScrollToItem(state.eventLogs.size - 1)
        }
    }

    val isTransferActive = state.stats.status == UploadStatus.STREAMING ||
            state.stats.status == UploadStatus.CONNECTING

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cronet & QUIC Migration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Seamless Wi-Fi ➔ Cellular Handover",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Active Network Indicator Card
            NetworkStateCard(
                networkType = state.stats.currentNetwork,
                isTransferActive = isTransferActive
            )

            // 2. Live Transfer Progress & Speed Card
            TransferMetricsCard(
                stats = state.stats
            )

            // 3. Endpoint Configuration
            OutlinedTextField(
                value = state.targetUrl,
                onValueChange = { viewModel.onTargetUrlChanged(it) },
                label = { Text("HTTP/3 QUIC Endpoint") },
                singleLine = true,
                enabled = !isTransferActive,
                modifier = Modifier.fillMaxWidth()
            )

            // 4. Action Buttons (Start / Cancel)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.startUpload() },
                    enabled = !isTransferActive,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Streaming Upload")
                }

                OutlinedButton(
                    onClick = { viewModel.cancelUpload() },
                    enabled = isTransferActive,
                    modifier = Modifier.weight(0.7f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }
            }

            // Educational hint
            Text(
                text = "💡 Tip: Start upload, then toggle Wi-Fi in Quick Settings to observe QUIC connection migration to Cellular without interruption.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 5. Connection & Migration Event Log
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Event & Handover Log",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = { viewModel.clearLogs() },
                            enabled = state.eventLogs.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }

                    LazyColumn(
                        state = logListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (state.eventLogs.isEmpty()) {
                            item {
                                Text(
                                    text = "Ready. Tap 'Start Streaming Upload' to begin.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            items(state.eventLogs) { logEntry ->
                                val isHandover = logEntry.contains("QUIC Handover") ||
                                        logEntry.contains("Network switched")
                                val isSuccess = logEntry.contains("Completed") || logEntry.contains("✅")
                                val isError = logEntry.contains("Failed") || logEntry.contains("❌")

                                val textColor = when {
                                    isHandover -> MaterialTheme.colorScheme.primary
                                    isSuccess -> Color(0xFF2E7D32)
                                    isError -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Text(
                                    text = logEntry,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    fontWeight = if (isHandover) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkStateCard(
    networkType: ActiveNetworkType,
    isTransferActive: Boolean,
    modifier: Modifier = Modifier
) {
    val (icon: ImageVector, title: String, subtitle: String, tintColor: Color) = when (networkType) {
        ActiveNetworkType.WIFI -> Quad(
            Icons.Default.Wifi,
            "Wi-Fi (Active)",
            if (isTransferActive) "Primary interface streaming" else "Connected",
            Color(0xFF1B5E20)
        )
        ActiveNetworkType.CELLULAR -> Quad(
            Icons.Default.SignalCellularAlt,
            "Cellular (Active)",
            if (isTransferActive) "Handover destination active" else "Connected",
            Color(0xFFE65100)
        )
        ActiveNetworkType.ETHERNET -> Quad(
            Icons.Default.NetworkCheck,
            "Ethernet (Active)",
            "Connected",
            Color(0xFF0D47A1)
        )
        ActiveNetworkType.DISCONNECTED -> Quad(
            Icons.Default.WifiOff,
            "Disconnected",
            "No active network",
            MaterialTheme.colorScheme.error
        )
        ActiveNetworkType.OTHER -> Quad(
            Icons.Default.CellTower,
            "Network Active",
            "Connected",
            MaterialTheme.colorScheme.primary
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tintColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = if (isTransferActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isTransferActive) "QUIC Live" else "QUIC Idle",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = if (isTransferActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TransferMetricsCard(
    stats: com.example.devsummit2026.cronet.UploadTransferStats,
    modifier: Modifier = Modifier
) {
    val uploadedMb = stats.uploadedBytes / (1024.0 * 1024.0)
    val totalMb = stats.totalBytes / (1024.0 * 1024.0)
    val progress = if (stats.totalBytes > 0) {
        (stats.uploadedBytes.toFloat() / stats.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upload Progress",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format(Locale.US, "%.1f / %.1f MB", uploadedMb, totalMb),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Transfer Speed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f Mbps", stats.transferSpeedMbps),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Protocol",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stats.negotiatedProtocol,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = stats.statusDetail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
