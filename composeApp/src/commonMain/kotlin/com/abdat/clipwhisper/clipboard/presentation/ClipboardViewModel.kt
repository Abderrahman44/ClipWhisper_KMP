package com.abdat.clipwhisper.clipboard.presentation


import androidx.lifecycle.ViewModel
import com.abdat.clipwhisper.clipboard.domain.ClipboardRepository
import com.abdat.clipwhisper.clipboard.domain.models.ClipboardState
import com.abdat.clipwhisper.network.data.ClipboardSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class ClipboardViewModel(
    private val sync: ClipboardSyncManager,
    private val repo: ClipboardRepository
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(ClipboardState())
    val state: StateFlow<ClipboardState> = _state.asStateFlow()

    init {
        scope.launch {
            repo.observeRecent(limit = 10).collect { items ->
                _state.update { it.copy(history = items.map { row -> row.payload }) }
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
    fun updateInputText(text: String) = _state.update { it.copy(inputText = text) }

    fun setClipboard() {
        val text = _state.value.inputText
        sync.setLocalClipboard(text, alsoSend = true)
        _state.update { it.copy(inputText = "") }
    }

    fun getClipboard() = sync.pullOnce()
    fun toggleListener() = sync.toggle()
    fun copyFromHistory(text: String) = sync.setLocalClipboard(text, alsoSend = true)

    fun clearHistory() {
        scope.launch {
            repo.clearAll()
            _state.update { it.copy(statusMessage = "History cleared") }
        }
    }

    fun clearStatus() = _state.update { it.copy(statusMessage = "") }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}

