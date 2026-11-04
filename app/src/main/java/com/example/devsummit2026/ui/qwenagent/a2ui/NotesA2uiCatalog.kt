package com.example.devsummit2026.ui.qwenagent.a2ui

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Component Catalog definition for Agent-to-UI (A2UI).
 * Provides custom interactive components for note creation and editing
 * mapped through [A2uiComponentScope].
 */
object NotesA2uiCatalog {

    const val CATALOG_ID = "https://a2ui.org/specification/v0_9/catalogs/notes/catalog.json"

    val TitleProperty = A2uiProperty.dynamicString(
        "title",
        false,
        "The note title"
    )

    val ContentProperty = A2uiProperty.dynamicString(
        "content",
        false,
        "The note content body"
    )

    val IsSavedProperty = A2uiProperty.dynamicBoolean(
        "isSaved",
        false,
        "Whether this note has been saved to the repository"
    )

    /**
     * Interactive Note Editor A2UI component.
     */
    val NoteEditorComponent = object : A2uiComponent {
        override val name: String = "NoteEditor"
        override val description: String = "Interactive Note form with title, content and save action"
        override val properties: List<A2uiProperty<*>> = listOf(
            TitleProperty,
            ContentProperty,
            IsSavedProperty
        )

        @Composable
        override fun A2uiComponentScope.Content(
            properties: A2uiComponentProperties,
            modifier: Modifier
        ) {
            val propTitle = properties.bind(TitleProperty) ?: ""
            val propContent = properties.bind(ContentProperty) ?: ""
            val propIsSaved = properties.bind(IsSavedProperty) ?: false

            var title by remember(propTitle) { mutableStateOf(propTitle) }
            var content by remember(propContent) { mutableStateOf(propContent) }
            var isSaved by remember(propIsSaved) { mutableStateOf(propIsSaved) }

            Card(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header with A2UI Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.CheckCircle else Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (isSaved) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isSaved) "Note Saved" else "Draft Note (A2UI)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSaved) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "A2UI COMPONENT",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Field 1: Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (!isSaved) title = it },
                        label = { Text("Title") },
                        placeholder = { Text("Enter note title...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSaved,
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Field 2: Content
                    OutlinedTextField(
                        value = content,
                        onValueChange = { if (!isSaved) content = it },
                        label = { Text("Content") },
                        placeholder = { Text("Enter note details...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        enabled = !isSaved,
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Action Button or Saved Feedback
                    if (!isSaved) {
                        Button(
                            onClick = {
                                isSaved = true
                                // Dispatch A2UI action adhering to A2UI specification
                                dispatchAction(
                                    mapOf(
                                        "event" to mapOf(
                                            "name" to "saveNote",
                                            "context" to mapOf(
                                                "title" to title,
                                                "content" to content
                                            )
                                        )
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = title.isNotBlank() || content.isNotBlank(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Note")
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Successfully saved in NotesRepository",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Builds and returns the [A2uiCatalog] including the custom Note Editor component.
     */
    fun createCatalog(): A2uiCatalog {
        return A2uiCatalog(
            catalogId = CATALOG_ID,
            components = listOf(NoteEditorComponent)
        )
    }
}
