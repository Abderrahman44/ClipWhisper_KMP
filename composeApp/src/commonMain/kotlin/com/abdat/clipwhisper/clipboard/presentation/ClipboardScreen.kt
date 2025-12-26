package com.abdat.clipwhisper.clipboard.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abdat.clipwhisper.clipboard.domain.models.ClipboardItem
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardScreen(
    viewModel: ClipboardViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.autoFetchClipboard() }

    LaunchedEffect(state.statusMessage) {
        if (state.statusMessage.isNotBlank()) {
            snackbarHostState.showSnackbar(
                message = state.statusMessage,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
            viewModel.clearStatus()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.undoEvents.collect { deleted ->
            val res = snackbarHostState.showSnackbar(
                message = "Deleted",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (res == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete(deleted)
            }
        }
    }

    var showClearDialog by remember { mutableStateOf(false) }
    var keepPinned by remember { mutableStateOf(true) }

    // NEW: composer sheet state
    var showComposer by rememberSaveable { mutableStateOf(false) }

    if (showComposer) {
        ClipboardComposerBottomSheet(
            value = state.inputText,
            onValueChange = viewModel::updateInputText,
            onSet = {
                viewModel.setClipboard()
                showComposer = false
            },
            onDismiss = { showComposer = false }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
            title = { Text("Delete history?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This will remove clipboard history.")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = keepPinned, onCheckedChange = { keepPinned = it })
                        Text("Keep pinned items")
                    }
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearHistory(keepPinned = keepPinned)
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("ClipWhisper") },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = viewModel::getClipboard) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh clipboard")
                    }
                    IconButton(onClick = viewModel::toggleListener) {
                        Icon(
                            imageVector = if (state.isListening) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (state.isListening) "Stop listening" else "Start listening"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },

        // NEW: FAB replaces InputCard (+ also replaces BottomActionBar if you accept this layout)
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showComposer = true },
                icon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                text = { Text("Set clipboard") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CurrentClipboardCardModern(
                    clipboardText = state.currentClipboard,
                    isListening = state.isListening,
                    onCopy = {
                        if (state.currentClipboard.isNotBlank()) {
                            viewModel.setClipboard()
                        }
                    }
                )
            }

            if (state.pinned.isNotEmpty()) {
                item {
                    PinnedHeader(
                        count = state.pinned.size,
                        expanded = state.pinnedExpanded,
                        onToggle = viewModel::togglePinnedExpanded
                    )
                }

                if (state.pinnedExpanded) {
                    items(state.pinned, key = { it.id }) { item ->
                        ClipboardRow(
                            item = item,
                            onCopy = { viewModel.copyItem(item) },
                            onPinToggle = { viewModel.togglePin(item) },
                            onDelete = { viewModel.deleteItem(item) },
                            modifier = Modifier.animateItem() //animateItemPlacement
                        )
                    }
                }
            }

            item {
                HistoryHeader(
                    count = state.history.size,
                    //max = 10,
                    onClear = { showClearDialog = true },
                    enabled = state.history.isNotEmpty()
                )
            }

            if (state.history.isEmpty()) {
                item { EmptyHistoryHint() }
            } else {
                items(state.history, key = { it.id }) { item ->
                    ClipboardRow(
                        item = item,
                        onCopy = { viewModel.copyItem(item) },
                        onPinToggle = { viewModel.togglePin(item) },
                        onDelete = { viewModel.deleteItem(item) },
                        modifier = Modifier.animateItem() //Placement()
                    )
                }
            }
        }
    }
}



@Composable
private fun CurrentClipboardCardModern(
    clipboardText: String,
    isListening: Boolean,
    onCopy: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ListeningDot(isListening = isListening)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Current clipboard", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (isListening) "Listening…" else "Idle",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalIconButton(
                    onClick = onCopy,
                    enabled = clipboardText.isNotBlank()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                }
            }

            AnimatedContent(
                targetState = clipboardText,
                label = "clipboardText"
            ) { text ->
                val shown = text.ifBlank { "Nothing yet — tap Refresh or start listening." }

                // Nice on desktop: allow selection/copying
                SelectionContainer {
                    Text(
                        text = shown,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) 20 else 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { expanded = !expanded },
                                onLongClick = { expanded = true }
                            )
                    )
                }
            }

            if (clipboardText.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Show less" else "Show more")
                    }
                }
            }
        }
    }
}

@Composable
private fun ListeningDot(isListening: Boolean) {
    val transition = rememberInfiniteTransition(label = "listeningDot")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val baseColor =
        if (isListening) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(baseColor.copy(alpha = if (isListening) pulse else 0.6f))
    )
}


@ExperimentalMaterial3Api
@Composable
private fun ClipboardComposerBottomSheet(
    value: String,
    onValueChange: (String) -> Unit,
    onSet: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Set clipboard", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Type something to copy…") },
                minLines = 3,
                maxLines = 8,
                shape = RoundedCornerShape(16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = onSet,
                    enabled = value.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Set")
                }
            }
        }
    }
}


@Composable
private fun HistoryHeader(
    count: Int,
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
            text = "$count",
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
private fun PinnedHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggle, onLongClick = onToggle),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Pinned", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Text("$count", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClipboardRow(
    item: ClipboardItem,
    onCopy: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)

    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        positionalThreshold = { it * 0.35f }
    )

    LaunchedEffect(dismissState.currentValue) {
        val dismissed =
            dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd ||
                    dismissState.currentValue == SwipeToDismissBoxValue.EndToStart

        if (dismissed) {
            onDelete()
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        ElevatedCard(
            onClick = onCopy,
            modifier = modifier
                .fillMaxWidth()
                .then(
                    if (item.pinned) Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        shape = shape
                    ) else Modifier
                ),
            shape = shape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.payload,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )

                IconToggleButton(
                    checked = item.pinned,
                    onCheckedChange = { onPinToggle() }
                ) {
                    Icon(
                        imageVector = if (item.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (item.pinned) "Unpin" else "Pin",
                        tint = if (item.pinned) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


