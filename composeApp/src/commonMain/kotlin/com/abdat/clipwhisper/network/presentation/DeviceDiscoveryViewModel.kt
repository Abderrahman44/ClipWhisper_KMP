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
): ViewModel() {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // UI State
    private val _uiState = MutableStateFlow(DeviceDiscoveryUiState())
    val uiState: StateFlow<DeviceDiscoveryUiState> = _uiState.asStateFlow()

    // Discovered devices
    val discoveredDevices: StateFlow<List<Device>> = discoveryManager.discoveredDevices
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Is discovery running
    val isDiscovering: StateFlow<Boolean> = discoveryManager.isRunning
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Approved device IDs
    val approvedDeviceIds: StateFlow<Set<String>> = discoveryManager.approvedDeviceIds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    // Derived states
    val pairedDevices: StateFlow<List<Device>> = discoveredDevices
        .map { devices -> devices.filter { it.isApproved } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unpairededDevices: StateFlow<List<Device>> = discoveredDevices
        .map { devices -> devices.filter { !it.isApproved } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Auto-start discovery if enabled
        viewModelScope.launch {
            if (_uiState.value.autoStartEnabled) {
                startDiscovery()
            }
        }
    }

    fun startDiscovery() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(error = null) }

                val deviceInfo = deviceInfoProvider.getDeviceInfo()
                discoveryManager.startDiscovery(
                    deviceId = deviceInfo.deviceId,
                    deviceName = deviceInfo.deviceName,
                    port = deviceInfo.port
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to start discovery: ${e.message}")
                }
            }
        }
    }

    fun stopDiscovery() {
        discoveryManager.stopDiscovery()
    }

    fun pairDevice(device: Device) {
        viewModelScope.launch {
            try {
                discoveryManager.pairDevice(device.deviceId)
                _uiState.update {
                    it.copy(
                        selectedDevice = null,
                        showPairDialog = false,
                        message = "Paired with ${device.name}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to pair: ${e.message}")
                }
            }
        }
    }

    fun unpairDevice(device: Device) {
        viewModelScope.launch {
            try {
                discoveryManager.unpairDevice(device.deviceId)
                _uiState.update {
                    it.copy(
                        selectedDevice = null,
                        showUnpairDialog = false,
                        message = "Unpaired from ${device.name}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to unpair: ${e.message}")
                }
            }
        }
    }

    fun showPairDialog(device: Device) {
        _uiState.update {
            it.copy(selectedDevice = device, showPairDialog = true)
        }
    }

    fun dismissPairDialog() {
        _uiState.update {
            it.copy(selectedDevice = null, showPairDialog = false)
        }
    }

    fun showUnpairDialog(device: Device) {
        _uiState.update {
            it.copy(selectedDevice = device, showUnpairDialog = true)
        }
    }

    fun dismissUnpairDialog() {
        _uiState.update {
            it.copy(selectedDevice = null, showUnpairDialog = false)
        }
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
        viewModelScope.cancel()
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