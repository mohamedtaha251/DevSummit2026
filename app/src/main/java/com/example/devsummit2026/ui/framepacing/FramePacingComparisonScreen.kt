package com.example.devsummit2026.ui.framepacing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.metrics.performance.JankStats

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Educational Screen orchestrating the UI Frame Pacing & JankStats comparison.
 * - Tracks live frame telemetry via AndroidX JankStats.createAndTrack(window, frameListener).
 * - Switches between Unoptimized (Mode A) and Frame-Paced (Mode B).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FramePacingComparisonScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val window = remember(context) { context.findActivity()?.window }

    var selectedMode by remember { mutableStateOf(PacingComparisonMode.OPTIMIZED) }

    // JankStats Telemetry State
    var totalFrames by remember { mutableLongStateOf(0L) }
    var jankFrames by remember { mutableLongStateOf(0L) }
    var latestDurationMs by remember { mutableFloatStateOf(0f) }
    var latestIsJank by remember { mutableStateOf(false) }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // AndroidX JankStats registration via createAndTrack
    DisposableEffect(window) {
        val currentWindow = window ?: return@DisposableEffect onDispose {}
        val jankStats = JankStats.createAndTrack(currentWindow) { frameData ->
            val durationMs = frameData.frameDurationUiNanos / 1_000_000f
            val isJank = frameData.isJank
            mainHandler.post {
                totalFrames++
                if (isJank) {
                    jankFrames++
                }
                latestDurationMs = durationMs
                latestIsJank = isJank
            }
        }
        jankStats.isTrackingEnabled = true

        onDispose {
            jankStats.isTrackingEnabled = false
        }
    }

    val jankRate = remember(totalFrames, jankFrames) {
        if (totalFrames > 0L) (jankFrames.toFloat() / totalFrames * 100f) else 0f
    }

    val items = remember {
        List(100) { index ->
            TelemetryItem(
                id = index,
                title = "Frame Target #${index + 1}",
                subtitle = "VSYNC Target: 16.6ms (60Hz)",
                category = if (index % 2 == 0) "Draw & Layout" else "Composition Pass"
            )
        }
    }

    val unoptimizedState = rememberLazyListState()
    val optimizedState = rememberLazyListState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Frame Pacing & JankStats",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Missed Frame Elimination & VSYNC Budget",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live Telemetry & Jank HUD (Top of Screen)
            JankTelemetryHud(
                totalFrames = totalFrames,
                jankFrames = jankFrames,
                jankRate = jankRate,
                latestDurationMs = latestDurationMs,
                latestIsJank = latestIsJank,
                onResetMetrics = {
                    totalFrames = 0L
                    jankFrames = 0L
                    latestDurationMs = 0f
                    latestIsJank = false
                }
            )

            // Mode Selector Toggle
            ModeSelectorRow(
                selectedMode = selectedMode,
                onModeSelected = { mode ->
                    if (selectedMode != mode) {
                        selectedMode = mode
                        totalFrames = 0L
                        jankFrames = 0L
                        latestDurationMs = 0f
                        latestIsJank = false
                    }
                }
            )

            // Mode Explanation Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (selectedMode == PacingComparisonMode.UNOPTIMIZED) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (selectedMode == PacingComparisonMode.UNOPTIMIZED) {
                            Icons.Default.Warning
                        } else {
                            Icons.Default.Bolt
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (selectedMode == PacingComparisonMode.UNOPTIMIZED) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    Text(
                        text = if (selectedMode == PacingComparisonMode.UNOPTIMIZED) {
                            "Mode A: ~18ms blocking work & un-keyed items (drops frames)."
                        } else {
                            "Mode B: Stable keys & deferred phase reads (<16.6ms)."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Interactive fast-scrollable list (100 items)
            if (selectedMode == PacingComparisonMode.UNOPTIMIZED) {
                LazyColumn(
                    state = unoptimizedState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Unoptimized: un-keyed items + synchronous heavy work in composition
                    items(items.size) { index ->
                        val item = items[index]
                        UnoptimizedItemCard(
                            item = item,
                            scrollOffset = unoptimizedState.firstVisibleItemScrollOffset
                        )
                    }
                }
            } else {
                // Derived state optimization: only re-evaluates when threshold boundary is crossed
                val isScrolledPastFirst by remember {
                    derivedStateOf { optimizedState.firstVisibleItemIndex > 0 }
                }

                LazyColumn(
                    state = optimizedState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Optimized: stable keys + no blocking calls + deferred phase reads
                    items(items, key = { it.id }) { item ->
                        OptimizedItemCard(
                            item = item,
                            isScrolled = isScrolledPastFirst
                        )
                    }
                }
            }
        }
    }
}
