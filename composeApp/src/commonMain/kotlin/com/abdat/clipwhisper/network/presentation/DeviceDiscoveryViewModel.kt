package com.abdat.clipwhisper.network.presentation

import androidx.lifecycle.ViewModel
import com.abdat.clipwhisper.network.data.DeviceDiscoveryManager
import com.abdat.clipwhisper.network.data.DeviceInfoProvider
import com.abdat.clipwhisper.network.domain.model.Device
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for device discovery across all platforms
 */
class DeviceDiscoveryViewModel(
    private val discoveryManager: DeviceDiscoveryManager,
    private val deviceInfoProvider: DeviceInfoProvider
) : ViewModel() {

    private fun log(msg: String) = println("ClipWhisper/ViewModel: $msg")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(DeviceDiscoveryUiState())
    val uiState: StateFlow<DeviceDiscoveryUiState> = _uiState.asStateFlow()

    val discoveredDevices: StateFlow<List<Device>> = discoveryManager.discoveredDevices
    val isDiscovering: StateFlow<Boolean> = discoveryManager.isRunning
    val approvedDeviceIds: StateFlow<Set<String>> = discoveryManager.approvedDeviceIds

    val pairedDevices: StateFlow<List<Device>> = discoveredDevices
        .map { list -> list.filter { it.isApproved } }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ✅ you asked for this exact property name back
    val unpairededDevices: StateFlow<List<Device>> = discoveredDevices
        .map { list -> list.filter { !it.isApproved } }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (_uiState.value.autoStartEnabled) {
            startDiscovery()
        }

        // Optional but VERY helpful: logs whenever approved IDs change
        scope.launch {
            approvedDeviceIds.collect { ids ->
                log("approvedDeviceIds changed -> count=${ids.size} ids=$ids")
            }
        }
    }

    fun startDiscovery() {
        scope.launch {
            _uiState.update { it.copy(error = null, message = null) }

            runCatching {
                val info = deviceInfoProvider.getDeviceInfo()
                log("startDiscovery() using selfId=${info.deviceId} name=${info.deviceName} port=${info.port}")
                discoveryManager.startDiscovery(info.deviceId, info.deviceName, info.port)
            }.onFailure { e ->
                log("startDiscovery() failed: ${e.message}")
                _uiState.update { it.copy(error = "Failed to start discovery: ${e.message}") }
            }
        }
    }

    fun stopDiscovery() {
        log("stopDiscovery()")
        discoveryManager.stopDiscovery()
    }

    fun pairDevice(device: Device) {
        scope.launch {
            log("pairDevice() clicked deviceId=${device.deviceId} name=${device.name}")

            runCatching {
                discoveryManager.pairDevice(device.deviceId)

                // ✅ actually use isPaired()
                val pairedInStore = discoveryManager.isPaired(device.deviceId)

                // check if discovery list reflects it (may lag by a tiny moment)
                val pairedInList = discoveredDevices.value
                    .firstOrNull { it.deviceId == device.deviceId }
                    ?.isApproved

                val approvedCount = discoveryManager.getApprovedDevices().size

                log("pairDevice() result -> store=$pairedInStore, discoveryList=$pairedInList, approvedVisible=$approvedCount")

                _uiState.update {
                    it.copy(
                        selectedDevice = null,
                        showPairDialog = false,
                        message = buildString {
                            append("Pair request saved. store=$pairedInStore")
                            if (pairedInList != null) append(", discoveryList=$pairedInList")
                            append(" (approvedVisible=$approvedCount)")
                        }
                    )
                }
            }.onFailure { e ->
                log("pairDevice() failed: ${e.message}")
                _uiState.update { it.copy(error = "Failed to pair: ${e.message}") }
            }
        }
    }

    fun unpairDevice(device: Device) {
        scope.launch {
            log("unpairDevice() clicked deviceId=${device.deviceId} name=${device.name}")

            runCatching {
                discoveryManager.unpairDevice(device.deviceId)

                val pairedInStore = discoveryManager.isPaired(device.deviceId)
                val pairedInList = discoveredDevices.value
                    .firstOrNull { it.deviceId == device.deviceId }
                    ?.isApproved

                val approvedCount = discoveryManager.getApprovedDevices().size

                log("unpairDevice() result -> store=$pairedInStore, discoveryList=$pairedInList, approvedVisible=$approvedCount")

                _uiState.update {
                    it.copy(
                        selectedDevice = null,
                        showUnpairDialog = false,
                        message = buildString {
                            append("Unpair request saved. store=$pairedInStore")
                            if (pairedInList != null) append(", discoveryList=$pairedInList")
                            append(" (approvedVisible=$approvedCount)")
                        }
                    )
                }
            }.onFailure { e ->
                log("unpairDevice() failed: ${e.message}")
                _uiState.update { it.copy(error = "Failed to unpair: ${e.message}") }
            }
        }
    }

    fun showPairDialog(device: Device) {
        _uiState.update { it.copy(selectedDevice = device, showPairDialog = true) }
    }

    fun dismissPairDialog() {
        _uiState.update { it.copy(selectedDevice = null, showPairDialog = false) }
    }

    fun showUnpairDialog(device: Device) {
        _uiState.update { it.copy(selectedDevice = device, showUnpairDialog = true) }
    }

    fun dismissUnpairDialog() {
        _uiState.update { it.copy(selectedDevice = null, showUnpairDialog = false) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun setAutoStart(enabled: Boolean) {
        _uiState.update { it.copy(autoStartEnabled = enabled) }
    }

    fun onDispose() {
        stopDiscovery()
        scope.cancel()
    }

    override fun onCleared() {
        onDispose()
        super.onCleared()
    }
}



/**
 * UI State for device discovery screen
 */
data class DeviceDiscoveryUiState(
    val selectedDevice: Device? = null,
    val showPairDialog: Boolean = false,
    val showUnpairDialog: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val autoStartEnabled: Boolean = true
)