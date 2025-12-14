package com.abdat.clipwhisper.clipboard.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardScreen(
    viewModel: ClipboardViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()

    CurrentClipboardDisplay(
        clipboardText = state.currentClipboard,
        isListening = state.isListening
    )

    // Auto-clear status message after 3 seconds
    LaunchedEffect(state.statusMessage) {
        if (state.statusMessage.isNotEmpty()) {
            delay(3000)
            viewModel.clearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Clipboard Manager",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info Card about auto-fetch
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💡", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Clipboard content is automatically loaded when you open or return to the app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Status Message
            if (state.statusMessage.isNotEmpty()) {
                StatusBanner(message = state.statusMessage)
            }

            // Input Section
            InputSection(
                inputText = state.inputText,
                onInputChange = viewModel::updateInputText,
                onSetClipboard = viewModel::setClipboard
            )

            // Action Buttons
            ActionButtons(
                isListening = state.isListening,
                onGetClipboard = viewModel::getClipboard,
                onToggleListener = viewModel::toggleListener
            )

            // Current Clipboard Display
            CurrentClipboardDisplay(clipboardText = state.currentClipboard)

            // History Section
            HistorySection(
                history = state.history,
                onCopyFromHistory = viewModel::copyFromHistory,
                onClearHistory = viewModel::clearHistory
            )
        }
    }
}

@Composable
fun StatusBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = when {
            message.contains("✅") || message.contains("📋") -> Color(0xFF4CAF50).copy(alpha = 0.2f)
            message.contains("❌") -> Color(0xFFF44336).copy(alpha = 0.2f)
            else -> MaterialTheme.colorScheme.secondaryContainer
        }
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun InputSection(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSetClipboard: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Set Clipboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enter text to copy") },
                placeholder = { Text("Type something...") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(8.dp)
            )

            Button(
                onClick = onSetClipboard,
                modifier = Modifier.fillMaxWidth(),
                enabled = inputText.isNotBlank()
            ) {
                Text("📋 Set to Clipboard", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun ActionButtons(
    isListening: Boolean,
    onGetClipboard: () -> Unit,
    onToggleListener: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onGetClipboard,
            modifier = Modifier.weight(1f)
        ) {
            Text("📥 Refresh")
        }

        Button(
            onClick = onToggleListener,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isListening)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(
                text = if (isListening) "🔇 Stop Auto-detect" else "🎧 Auto-detect Changes",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CurrentClipboardDisplay(
    clipboardText: String,
    isListening: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isListening)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.tertiaryContainer
        ),
        border = if (isListening)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Clipboard Content",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isListening)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onTertiaryContainer
                )

                if (isListening) {
                    // Animated pulse indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }

            if (clipboardText.isEmpty()) {
                Text(
                    text = if (isListening)
                        "Waiting for clipboard changes..."
                    else
                        "No content yet...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isListening)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = clipboardText,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun HistorySection(
    history: List<String>,
    onCopyFromHistory: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "History (${history.size}/10)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Text("🗑️ Clear")
                    }
                }
            }

            if (history.isEmpty()) {
                Text(
                    text = "No history yet. Copy something to see it here!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history) { item ->
                        HistoryItem(
                            text = item,
                            onClick = { onCopyFromHistory(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📄",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "→",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}