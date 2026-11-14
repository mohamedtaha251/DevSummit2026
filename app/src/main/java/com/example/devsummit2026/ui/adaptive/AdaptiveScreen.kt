package com.example.devsummit2026.ui.adaptive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.LocalUiMediaScope
import androidx.compose.ui.UiMediaScope
import androidx.compose.ui.derivedMediaQuery

@OptIn(ExperimentalMediaQueryApi::class)
private class SafeUiMediaScope(
    override val windowWidth: Dp,
    override val windowHeight: Dp,
    override val windowPosture: UiMediaScope.Posture = UiMediaScope.Posture.Flat,
    override val pointerPrecision: UiMediaScope.PointerPrecision = UiMediaScope.PointerPrecision.Coarse,
    override val keyboardKind: UiMediaScope.KeyboardKind = UiMediaScope.KeyboardKind.Virtual,
    override val hasMicrophone: Boolean = true,
    override val hasCamera: Boolean = true,
    override val viewingDistance: UiMediaScope.ViewingDistance = UiMediaScope.ViewingDistance.Near
) : UiMediaScope

@OptIn(ExperimentalMediaQueryApi::class)
@Composable
fun SafeMediaQueryContainer(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val fallbackScope = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        SafeUiMediaScope(
            windowWidth = configuration.screenWidthDp.dp,
            windowHeight = configuration.screenHeightDp.dp
        )
    }

    val isScopeAvailable = runCatching { LocalUiMediaScope.current }.isSuccess

    if (!isScopeAvailable) {
        CompositionLocalProvider(LocalUiMediaScope provides fallbackScope) {
            content()
        }
    } else {
        content()
    }
}

private data class DemoCard(
    val id: String,
    val title: String,
    val category: String,
    val description: String
)

/**
 * Educational Adaptive Screen:
 * 1. Tracks live windowWidth and windowHeight using derivedMediaQuery.
 * 2. In Portrait mode (< 600dp), renders items as a single-column List.
 * 3. On screen rotation to Landscape (>= 600dp), transforms automatically into a multi-column Grid.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMediaQueryApi::class)
@Composable
fun AdaptiveScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SafeMediaQueryContainer {
        // Read live width & height efficiently using derivedMediaQuery
        val windowWidth by derivedMediaQuery { windowWidth }
        val windowHeight by derivedMediaQuery { windowHeight }

        // Adaptive column calculation:
        // Portrait mobile (< 600dp): 1 column (List layout)
        // Landscape mobile / small tablet (600dp - 900dp): 2 columns (Grid layout)
        // Large screen (>= 900dp): 3 columns (Grid layout)
        val columns = when {
            windowWidth < 600.dp -> 1
            windowWidth < 900.dp -> 2
            else -> 3
        }

        val modeLabel = if (columns == 1) "Portrait (1-Column List)" else "Landscape ($columns-Column Grid)"

        val sampleItems = remember {
            listOf(
                DemoCard("1", "Scale For All Screens", "Compose 2026", "Jetpack Compose responsive layout using the new MediaQuery API."),
                DemoCard("2", "derivedMediaQuery", "Live Metrics", "Evaluated efficiently in derivedStateOf to avoid recompositions on every single pixel delta."),
                DemoCard("3", "List to Grid Transformation", "Adaptive Layout", "Automatically transforms from list to grid on orientation change without Activity recreation."),
                DemoCard("4", "On-Device AI with LiteRT", "LiteRT Labs", "Running localized models directly on modern Android hardware."),
                DemoCard("5", "App Functions Integration", "System Architecture", "Registering actions with Android system for Gemini agent orchestration."),
                DemoCard("6", "Edge-to-Edge Experience", "Android 15 & 16", "Rendering underneath system bars with modern WindowInsets.")
            )
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Adaptive List-to-Grid",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Compose MediaQuery Demo",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live Dimension Card (Shows width & height updating on rotation)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Straighten,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Window Size",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "${windowWidth.value.toInt()}dp × ${windowHeight.value.toInt()}dp",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Layout Mode",
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(
                                text = modeLabel,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Adaptive Grid / List
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sampleItems, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = item.category,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
