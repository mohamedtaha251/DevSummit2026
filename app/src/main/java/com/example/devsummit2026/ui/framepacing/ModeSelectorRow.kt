package com.example.devsummit2026.ui.framepacing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Compact toggle buttons allowing instant switching between:
 * - Mode A: Violator (~18ms)
 * - Mode B: Paced (< 4ms)
 */
@Composable
fun ModeSelectorRow(
    selectedMode: PacingComparisonMode,
    onModeSelected: (PacingComparisonMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selectedMode == PacingComparisonMode.UNOPTIMIZED) {
            FilledTonalButton(
                onClick = {},
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(
                    text = "Mode A: Violator",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        } else {
            OutlinedButton(
                onClick = { onModeSelected(PacingComparisonMode.UNOPTIMIZED) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Mode A: Violator",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        }

        if (selectedMode == PacingComparisonMode.OPTIMIZED) {
            FilledTonalButton(
                onClick = {},
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(
                    text = "Mode B: Paced",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        } else {
            OutlinedButton(
                onClick = { onModeSelected(PacingComparisonMode.OPTIMIZED) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Mode B: Paced",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        }
    }
}
