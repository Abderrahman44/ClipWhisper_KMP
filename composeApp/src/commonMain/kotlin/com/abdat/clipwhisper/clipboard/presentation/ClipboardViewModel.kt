package com.abdat.clipwhisper.clipboard.presentation


import androidx.lifecycle.ViewModel
import com.abdat.clipwhisper.clipboard.domain.ClipboardRepository
import com.abdat.clipwhisper.clipboard.domain.models.ClipboardItem
import com.abdat.clipwhisper.clipboard.domain.models.ClipboardState
import com.abdat.clipwhisper.network.data.ClipboardSyncManager
import com.abdat.clipwhisper.settings.AppSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


@OptIn(ExperimentalCoroutinesApi::class)
class ClipboardViewModel(
    private val sync: ClipboardSyncManager,
    private val repo: ClipboardRepository,
    private val settingsStore: AppSettingsStore,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private fun log(msg: String) = println("ClipWhisper/ClipboardVM: $msg")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(ClipboardState())
    val state: StateFlow<ClipboardState> = _state.asStateFlow()

    private val _undoEvents = MutableSharedFlow<ClipboardItem>(extraBufferCapacity = 16)
    val undoEvents: SharedFlow<ClipboardItem> = _undoEvents.asSharedFlow()

    init {
        scope.launch {
            settingsStore.settings
                .map { it.historySizeLimit }
                .distinctUntilChanged()
                .flatMapLatest { limit ->
                    repo.observeRecent(limit = limit.toLong()) // ✅ limit comes from settings
                }
                .collect { items ->
                    val pinned = items.filter { it.pinned }
                    val history = items.filterNot { it.pinned }
                    _state.update { it.copy(pinned = pinned, history = history) }
                }
        }

        scope.launch {
            sync.isRunning.collect { running ->
                _state.update { it.copy(isListening = running) }
            }
        }

        scope.launch {
            sync.currentClipboard.collect { text ->
                _state.update { it.copy(currentClipboard = text) }
            }
        }

        scope.launch {
            sync.statusEvents.collect { msg ->
                _state.update { it.copy(statusMessage = msg) }
            }
        }
    }

    fun autoFetchClipboard() = sync.pullOnce()
    fun getClipboard() = sync.pullOnce()

    fun updateInputText(text: String) = _state.update { it.copy(inputText = text) }

    fun setClipboard() {
        val text = _state.value.inputText
        sync.setLocalClipboard(text, alsoSend = true)
        _state.update { it.copy(inputText = "") }
    }

    fun toggleListener() = sync.toggle()

    fun copyItem(item: ClipboardItem) {
        sync.setLocalClipboard(item.payload, alsoSend = true)
    }

    fun copyFromHistory(text: String) {
        sync.setLocalClipboard(text, alsoSend = true)
    }

    fun togglePinnedExpanded() {
        _state.update { it.copy(pinnedExpanded = !it.pinnedExpanded) }
    }

    fun togglePin(item: ClipboardItem) {
        scope.launch {
            runCatching {
                repo.setPinned(id = item.id, pinned = !item.pinned)
            }.onFailure { e ->
                log("togglePin failed: ${e.message}")
                _state.update { it.copy(statusMessage = "Failed to update pin") }
            }
        }
    }

    fun deleteItem(item: ClipboardItem) {
        scope.launch {
            runCatching {
                repo.deleteById(item.id)
                _undoEvents.tryEmit(item)
            }.onFailure { e ->
                log("deleteItem failed: ${e.message}")
                _state.update { it.copy(statusMessage = "Failed to delete") }
            }
        }
    }

    fun undoDelete(item: ClipboardItem) {
        scope.launch {
            runCatching {
                val keepMax = settingsStore.settings.value.historySizeLimit.toLong()

                // optional: if user set 0 history, avoid re-inserting non-pinned
                if (keepMax == 0L && !item.pinned) {
                    _state.update { it.copy(statusMessage = "History limit is 0 (nothing to restore)") }
                    return@runCatching
                }

                repo.addToHistory(
                    text = item.payload,
                    originDeviceId = item.originDeviceId,
                    nowMillis = nowMillis(),
                    keepMax = keepMax,
                    expiresAtMillis = item.expiresAtMillis,
                    pinned = item.pinned
                )
            }.onFailure { e ->
                log("undoDelete failed: ${e.message}")
                _state.update { it.copy(statusMessage = "Failed to undo") }
            }
        }
    }


    fun clearHistory(keepPinned: Boolean = true) {
        scope.launch {
            runCatching {
                repo.clearAll(keepPinned = keepPinned)
                _state.update {
                    it.copy(
                        statusMessage = if (keepPinned)
                            "🗑️ Cleared history (kept pinned)"
                        else
                            "🗑️ Cleared everything"
                    )
                }
            }.onFailure { e ->
                log("clearHistory failed: ${e.message}")
                _state.update { it.copy(statusMessage = "Failed to clear") }
            }
        }
    }

    fun clearStatus() = _state.update { it.copy(statusMessage = "") }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}

