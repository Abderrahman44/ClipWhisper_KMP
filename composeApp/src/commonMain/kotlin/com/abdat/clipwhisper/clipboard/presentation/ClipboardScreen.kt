package com.abdat.clipwhisper.clipboard.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardScreen(
    viewModel: ClipboardViewModel = koinInject()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show status as snackbar (modern & non-blocking)
    LaunchedEffect(state.statusMessage) {
        if (state.statusMessage.isNotBlank()) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = state.statusMessage,
                    withDismissAction = true,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearStatus()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ClipWhisper") },
                actions = {
                    IconButton(onClick = viewModel::getClipboard) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh clipboard")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomActionBar(
                inputText = state.inputText,
                isListening = state.isListening,
                onSetClipboard = viewModel::setClipboard,
                onToggleListener = viewModel::toggleListener
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                CurrentClipboardCard(
                    clipboardText = state.currentClipboard,
                    isListening = state.isListening,
                    onCopy = {
                        if (state.currentClipboard.isNotBlank()) {
                            viewModel.copyFromHistory(state.currentClipboard)
                        }
                    }
                )
            }

            item {
                InputCard(
                    value = state.inputText,
                    onValueChange = viewModel::updateInputText
                )
            }

            item {
                HistoryHeader(
                    count = state.history.size,
                    max = 10,
                    onClear = viewModel::clearHistory,
                    enabled = state.history.isNotEmpty()
                )
            }

            if (state.history.isEmpty()) {
                item {
                    EmptyHistoryHint()
                }
            } else {
                items(state.history, key = { it }) { text ->
                    HistoryRow(
                        text = text,
                        onCopy = { viewModel.copyFromHistory(text) }
                    )
                }
            }

            item { Spacer(Modifier.height(72.dp)) } // space for bottom bar
        }
    }
}

@Composable
private fun CurrentClipboardCard(
    clipboardText: String,
    isListening: Boolean,
    onCopy: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Current clipboard",
                    style = MaterialTheme.typography.titleMedium
                )
                AssistChip(
                    onClick = { /* read-only */ },
                    label = { Text(if (isListening) "Listening" else "Idle") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    },
                    enabled = false
                )
            }

            Text(
                text = if (clipboardText.isBlank()) "Nothing yet — tap Refresh or start listening."
                else clipboardText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCopy, enabled = clipboardText.isNotBlank()) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Copy")
                }
            }
        }
    }
}

@Composable
private fun InputCard(
    value: String,
    onValueChange: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Set clipboard", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Type something to copy…") },
                minLines = 2,
                maxLines = 5,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
private fun BottomActionBar(
    inputText: String,
    isListening: Boolean,
    onSetClipboard: () -> Unit,
    onToggleListener: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSetClipboard,
                modifier = Modifier.weight(1f),
                enabled = inputText.isNotBlank(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Set")
            }

            OutlinedButton(
                onClick = onToggleListener,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isListening) "Stop" else "Listen")
            }
        }
    }
}

@Composable
private fun HistoryHeader(
    count: Int,
    max: Int,
    onClear: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "History",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$count/$max",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onClear, enabled = enabled) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear history")
        }
    }
}

@Composable
private fun EmptyHistoryHint() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = "No history yet. Copy something and it will appear here.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryRow(
    text: String,
    onCopy: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy item")
            }
        }
    }
}
