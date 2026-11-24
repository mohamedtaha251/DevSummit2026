package com.example.devsummit2026.ui.crashlytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashlyticsMemoryScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var memoryStats by remember { mutableStateOf(MemoryDiagnosticsHelper.readMemoryStats()) }
    val lastExitReason = remember { MemoryDiagnosticsHelper.readLatestExitReason(context) }
    var actionStatus by remember { mutableStateOf("Ready") }
    var isExhaustingMemory by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            memoryStats = MemoryDiagnosticsHelper.readMemoryStats()
            delay(1000L)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Crashlytics Memory Lab",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatRow(
                        label = "Managed Heap",
                        value = String.format(
                            Locale.US,
                            "%.1f / %.1f MB",
                            memoryStats.managedUsedMb,
                            memoryStats.managedMaxMb
                        )
                    )
                    StatRow(
                        label = "Native Heap",
                        value = String.format(Locale.US, "%.1f MB", memoryStats.nativeAllocatedMb)
                    )
                    StatRow(
                        label = "Last Exit",
                        value = lastExitReason
                    )
                    StatRow(
                        label = "Status",
                        value = actionStatus
                    )
                }
            }

            Button(
                onClick = {
                    val stats = MemoryDiagnosticsHelper.readMemoryStats()
                    memoryStats = stats
                    MemoryDiagnosticsHelper.setCustomKey("managed_heap_mb", stats.managedUsedMb)
                    MemoryDiagnosticsHelper.setCustomKey("native_heap_mb", stats.nativeAllocatedMb)
                    MemoryDiagnosticsHelper.setCustomKey("last_exit_reason", lastExitReason)
                    MemoryDiagnosticsHelper.logBreadcrumb(
                        String.format(
                            Locale.US,
                            "Memory lab breadcrumb: managed=%.1fMB native=%.1fMB",
                            stats.managedUsedMb,
                            stats.nativeAllocatedMb
                        )
                    )
                    actionStatus = "Logged Crashlytics breadcrumb & keys"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log Crashlytics Breadcrumb")
            }

            Button(
                onClick = {
                    MemoryDiagnosticsHelper.setCustomKey("crash_type", "managed_oom")
                    MemoryDiagnosticsHelper.logBreadcrumb("Triggering Managed OOM crash")
                    val chunks = mutableListOf<ByteArray>()
                    try {
                        while (true) {
                            chunks.add(ByteArray(16 * 1024 * 1024))
                        }
                    } catch (e: OutOfMemoryError) {
                        // Free chunks so Crashlytics can persist the fatal OOM report to disk
                        chunks.clear()
                        System.gc()
                        throw e
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Trigger Managed OOM")
            }

            Button(
                onClick = {
                    if (isExhaustingMemory) return@Button
                    isExhaustingMemory = true
                    actionStatus = "Exhausting native memory..."
                    MemoryDiagnosticsHelper.setCustomKey("crash_type", "memory_limit_kill")
                    MemoryDiagnosticsHelper.logBreadcrumb("Triggering Memory Limit Kill")
                    Thread {
                        val buffers = mutableListOf<ByteBuffer>()
                        try {
                            while (true) {
                                val buffer = ByteBuffer.allocateDirect(32 * 1024 * 1024)
                                var offset = 0
                                while (offset < buffer.capacity()) {
                                    buffer.put(offset, 1)
                                    offset += 4096
                                }
                                buffers.add(buffer)
                            }
                        } catch (t: Throwable) {
                            buffers.clear()
                            System.gc()
                            throw MemoryLimitKillException(
                                "Native memory limit exceeded: ${t.message}",
                                t
                            )
                        }
                    }.start()
                },
                enabled = !isExhaustingMemory,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text("Trigger Memory Limit Kill")
            }

            Button(
                onClick = {
                    MemoryDiagnosticsHelper.setCustomKey("crash_type", "gwp_asan_mte")
                    MemoryDiagnosticsHelper.logBreadcrumb("Triggering GWP-ASan / MTE native crash")
                    NativeCrashLab.triggerGwpAsanOrMteCrash()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Text("Trigger GWP-ASan / MTE Crash")
            }

            Button(
                onClick = {
                    MemoryDiagnosticsHelper.logBreadcrumb("Simulating Main-Thread ANR (10s block)")
                    actionStatus = "Blocking main thread for 10s..."
                    Thread.sleep(10_000L)
                    actionStatus = "Main thread unblocked"
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Text("Simulate Main-Thread ANR")
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
