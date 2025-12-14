package com.abdat.clipwhisper.clipboard.presentation


import androidx.lifecycle.ViewModel
import com.abdat.clipwhisper.clipboard.domain.ClipboardListener
import com.abdat.clipwhisper.clipboard.domain.ClipboardManager
import com.abdat.clipwhisper.clipboard.domain.ClipboardRepository
import com.abdat.clipwhisper.clipboard.domain.models.ClipboardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

import kotlinx.coroutines.launch


class ClipboardViewModel(
    private val clipboardManager: ClipboardManager,
    private val repo: ClipboardRepository,
    private val originDeviceId: String,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
): ViewModel()
{
    private val _state = MutableStateFlow(ClipboardState())
    val state: StateFlow<ClipboardState> = _state.asStateFlow()
    private var listener: ClipboardListener? = null
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    private var pollingJob: Job? = null
    private var lastClipboardContent: String? = null

    init {
        // Keep UI history in sync with DB
        viewModelScope.launch {
            repo.observeRecent(limit = 10).collect { items ->
                _state.value = _state.value.copy(
                    history = items.map { it.payload }
                )
            }
        }
    }

    // Auto-fetch clipboard on initialization
    fun autoFetchClipboard() {
        val text = clipboardManager.getClipboardText()
        if (!text.isNullOrEmpty() && text != lastClipboardContent) {
            lastClipboardContent = text
            _state.value = _state.value.copy(
                currentClipboard = text,
                statusMessage = "📋 Clipboard loaded automatically"
            )
            addToHistory(text)
        }
    }

    fun updateInputText(text: String) {
        _state.value = _state.value.copy(inputText = text)
    }

    fun updateStatusMessage(message: String) {
        _state.value = _state.value.copy(statusMessage = message)
    }

    fun setClipboard() {
        val text = _state.value.inputText
        if (text.isBlank()) {
            _state.value = _state.value.copy(statusMessage = "❌ Text cannot be empty")
            return
        }

        val success = clipboardManager.setClipboardText(text)
        if (success) {
            lastClipboardContent = text
            _state.value = _state.value.copy(
                currentClipboard = text,
                statusMessage = "✅ Clipboard set successfully",
                inputText = ""
            )
            addToHistory(text)
        } else {
            _state.value = _state.value.copy(statusMessage = "❌ Failed to set clipboard")
        }
    }

    fun getClipboard() {
        val text = clipboardManager.getClipboardText()
        if (text != null) {
            lastClipboardContent = text
            _state.value = _state.value.copy(
                currentClipboard = text,
                statusMessage = "✅ Clipboard retrieved successfully"
            )
            addToHistory(text)
        } else {
            _state.value = _state.value.copy(
                statusMessage = "❌ Clipboard is empty or unavailable"
            )
        }
    }

    fun toggleListener() {
        if (_state.value.isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun startListening() {
        // Start clipboard listener
        listener = clipboardManager.registerClipboardListener { text ->
            onClipboardChanged(text)
        }

        // Start polling to check clipboard periodically
        startPolling()

        _state.value = _state.value.copy(
            isListening = true,
            statusMessage = "🎧 Auto-detect started - watching for clipboard changes"
        )
    }

    private fun stopListening() {
        // Stop clipboard listener
        listener?.let {
            clipboardManager.unregisterClipboardListener(it)
            listener = null
        }

        // Stop polling
        stopPolling()

        _state.value = _state.value.copy(
            isListening = false,
            statusMessage = "🔇 Auto-detect stopped"
        )
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val text = clipboardManager.getClipboardText()
                    if (!text.isNullOrEmpty() && text != lastClipboardContent) {
                        lastClipboardContent = text
                        _state.value = _state.value.copy(
                            currentClipboard = text,
                            statusMessage = "🔔 New clipboard content detected"
                        )
                        addToHistory(text)
                    }
                } catch (e: Exception) {
                    // Ignore errors during polling
                }
                delay(500) // Check every 500ms
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun onClipboardChanged(text: String) {
        if (text.isNotEmpty() && text != lastClipboardContent) {
            lastClipboardContent = text
            _state.value = _state.value.copy(
                currentClipboard = text,
                statusMessage = "🔔 Clipboard changed"
            )
            addToHistory(text)
        }
    }

    /*fun clearHistory() {
        _state.value = _state.value.copy(
            history = emptyList(),
            statusMessage = "🗑️ History cleared"
        )
    }*/
    fun clearHistory() {
        viewModelScope.launch {
            repo.clearAll()
            _state.value = _state.value.copy(statusMessage = "🗑️ History cleared")
        }
    }

    fun copyFromHistory(text: String) {
        val success = clipboardManager.setClipboardText(text)
        if (success) {
            lastClipboardContent = text
            _state.value = _state.value.copy(
                currentClipboard = text,
                statusMessage = "✅ Copied from history"
            )
        }
    }

    /*private fun addToHistory(text: String) {
        val currentHistory = _state.value.history.toMutableList()
        // Don't add if it's already the most recent
        if (currentHistory.firstOrNull() == text) {
            return
        }
        // Remove if already exists
        currentHistory.remove(text)
        // Add to front
        currentHistory.add(0, text)
        // Keep only last 10
        if (currentHistory.size > 10) {
            currentHistory.removeAt(currentHistory.lastIndex)
        }
        _state.value = _state.value.copy(history = currentHistory)
    }*/

    private fun addToHistory(text: String) {
        viewModelScope.launch {
            repo.addToHistory(
                text = text,
                originDeviceId = originDeviceId,
                nowMillis = nowMillis(),
                keepMax = 10
            )
        }
    }

    fun clearStatus() {
        _state.value = _state.value.copy(statusMessage = "")
    }

    fun onClear() {
        stopListening()
        stopPolling()
        pollingJob?.cancel()
    }
}