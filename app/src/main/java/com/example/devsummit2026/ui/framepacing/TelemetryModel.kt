package com.example.devsummit2026.ui.framepacing

import androidx.annotation.keep.UnconditionallyKeep

enum class PacingComparisonMode {
    UNOPTIMIZED,
    OPTIMIZED
}

@UnconditionallyKeep
data class TelemetryItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val category: String
)
