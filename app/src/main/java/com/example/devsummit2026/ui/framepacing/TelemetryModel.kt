package com.example.devsummit2026.ui.framepacing

enum class PacingComparisonMode {
    UNOPTIMIZED,
    OPTIMIZED
}

data class TelemetryItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val category: String
)
