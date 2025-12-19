package com.abdat.clipwhisper.network.data


import com.abdat.clipwhisper.clipboard.domain.ClipboardListener
import com.abdat.clipwhisper.clipboard.domain.ClipboardManager
import com.abdat.clipwhisper.clipboard.domain.ClipboardRepository
import com.abdat.clipwhisper.network.data.tcp.TcpPairingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ClipboardSyncManager(
    private val clipboardManager: ClipboardManager,
    private val repo: ClipboardRepository,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val tcp: TcpPairingManager,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _running = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _running.asStateFlow()

    private val _current = MutableStateFlow("")
    val currentClipboard: StateFlow<String> = _current.asStateFlow()

    private val _status = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val statusEvents: SharedFlow<String> = _status.asSharedFlow()

    private var listener: ClipboardListener? = null
    private var pollingJob: Job? = null

    private var lastClipboard: String? = null

    // echo suppression
    private var lastRemoteApplied: String? = null
    private var suppressUntil: Long = 0L

    init {
        scope.launch {
            tcp.incomingClipboard.collect { e ->
                applyRemote(e.fromDeviceId, e.fromDeviceName, e.text)
            }
        }
    }

    fun start() {
        if (_running.value) return
        _running.value = true

        listener = clipboardManager.registerClipboardListener { text ->
            onLocalChanged(text)
        }

        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                delay(500)
                val text = runCatching { clipboardManager.getClipboardText() }.getOrNull()
                if (!text.isNullOrBlank() && text != lastClipboard) onLocalChanged(text)
            }
        }

        _status.tryEmit("Clipboard sync started")
    }

    fun stop() {
        if (!_running.value) return
        _running.value = false

        listener?.let { clipboardManager.unregisterClipboardListener(it) }
        listener = null
        pollingJob?.cancel()
        pollingJob = null

        _status.tryEmit("Clipboard sync stopped")
    }

    fun toggle() = if (_running.value) stop() else start()

    fun pullOnce() {
        val text = clipboardManager.getClipboardText().orEmpty()
        if (text.isNotBlank()) {
            lastClipboard = text
            _current.value = text
            _status.tryEmit("Clipboard refreshed")
        } else {
            _status.tryEmit("Clipboard empty")
        }
    }

    fun setLocalClipboard(text: String, alsoSend: Boolean) {
        if (text.isBlank()) {
            _status.tryEmit("Text cannot be empty")
            return
        }
        val ok = clipboardManager.setClipboardText(text)
        if (!ok) {
            _status.tryEmit("Failed to set clipboard")
            return
        }
        lastClipboard = text
        _current.value = text
        _status.tryEmit("Clipboard set")
        addToHistory(text, deviceInfoProvider.getDeviceInfo().deviceId)
        if (alsoSend) tcp.sendClipboard(text)
    }

    private fun onLocalChanged(text: String) {
        val now = nowMillis()
        if (now < suppressUntil && text == lastRemoteApplied) return
        if (text.isBlank() || text == lastClipboard) return

        lastClipboard = text
        _current.value = text
        _status.tryEmit("Clipboard changed")

        addToHistory(text, deviceInfoProvider.getDeviceInfo().deviceId)
        tcp.sendClipboard(text)
    }

    private fun applyRemote(fromId: String, fromName: String, text: String) {
        lastRemoteApplied = text
        suppressUntil = nowMillis() + 1200

        val ok = clipboardManager.setClipboardText(text)
        if (!ok) {
            _status.tryEmit("Failed to apply from $fromName")
            return
        }

        lastClipboard = text
        _current.value = text
        _status.tryEmit("Received from $fromName")
        addToHistory(text, fromId)
    }

    private fun addToHistory(text: String, originId: String) {
        scope.launch {
            runCatching {
                repo.addToHistory(text, originId, nowMillis(), keepMax = 10)
            }
        }
    }
}

