package com.example.devsummit2026.ui.framepacing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Mode A: Unoptimized (Budget Violator ~18ms)
 *
 * Flaws demonstrated:
 * 1. Synchronous Blocking on UI Thread: Thread.sleep(18) or heavy synchronous
 *    computation halts Choreographer, directly exceeding the 16.6ms VSYNC budget.
 * 2. Composition Phase State Read: Reading scrollOffset directly in the composable
 *    invalidates this item on EVERY single pixel scrolled, forcing constant recompositions.
 * 3. Un-keyed item parent: When used in LazyColumn without keys, item identity is lost.
 */
@Composable
fun UnoptimizedItemCard(
    item: TelemetryItem,
    scrollOffset: Int,
    modifier: Modifier = Modifier
) {
    // 1. Simulates blocking CPU/IO work directly on Android Main (UI) thread
    Thread.sleep(18)

    // 2. Direct state read during Composition phase forces recomposition on every pixel delta
    val dynamicAlpha = ((scrollOffset % 100) / 100f).coerceIn(0.85f, 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = dynamicAlpha },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
        )
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
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.error, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${item.id + 1}",
                    color = MaterialTheme.colorScheme.onError,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.error
            ) {
                Text(
                    text = "~18ms CPU",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}
