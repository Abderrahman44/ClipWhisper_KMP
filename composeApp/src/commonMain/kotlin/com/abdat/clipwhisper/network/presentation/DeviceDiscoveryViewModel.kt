package com.abdat.clipwhisper.network.presentation

/**
 * Shared ViewModel for device discovery across all platforms
 */
import androidx.lifecycle.ViewModel
import com.abdat.clipwhisper.network.data.DeviceDiscoveryManager
import com.abdat.clipwhisper.network.data.DeviceInfoProvider
import com.abdat.clipwhisper.network.data.tcp.TcpPairingManager
import com.abdat.clipwhisper.network.domain.model.Device
import com.abdat.clipwhisper.network.domain.model.IncomingPairRequest
import com.abdat.clipwhisper.network.domain.model.OutgoingPairRequest
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


class DeviceDiscoveryViewModel(
    private val discoveryManager: DeviceDiscoveryManager,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val pairingManager: TcpPairingManager,
) : ViewModel()
{

    private fun log(msg: String) = println("ClipWhisper/ViewModel: $msg")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(DeviceDiscoveryUiState())
    val uiState: StateFlow<DeviceDiscoveryUiState> = _uiState.asStateFlow()

    val discoveredDevices: StateFlow<List<Device>> = discoveryManager.discoveredDevices
    val isDiscovering: StateFlow<Boolean> = discoveryManager.isRunning
    val approvedDeviceIds: StateFlow<Set<String>> = discoveryManager.approvedDeviceIds

    // Pairing flows
    val incomingPairRequests: StateFlow<List<IncomingPairRequest>> = pairingManager.incomingRequests
    val outgoingPairRequests: StateFlow<List<OutgoingPairRequest>> = pairingManager.outgoingRequests

    val pairedDevices: StateFlow<List<Device>> = discoveredDevices
        .map { list -> list.filter { it.isApproved } }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unpairededDevices: StateFlow<List<Device>> = discoveredDevices
        .map { list -> list.filter { !it.isApproved } }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        pairingManager.startServer()

        if (_uiState.value.autoStartEnabled) {
            startDiscovery()
        }

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
                log("startDiscovery() selfId=${info.deviceId} name=${info.deviceName} port=${info.port}")
                discoveryManager.startDiscovery(info.deviceId, info.deviceName, info.port)
            }.onFailure { e ->
                log("startDiscovery() failed: ${e.message}")
                _uiState.update { it.copy(error = "Failed to start discovery: ${e.message}") }
            }
        }
    }

    fun stopDiscovery() {
        discoveryManager.stopDiscovery()
    }

    // UI dialog open/close for "Pair" button
    fun showPairDialog(device: Device) {
        _uiState.update { it.copy(selectedDevice = device, showPairDialog = true) }
    }

    fun dismissPairDialog() {
        _uiState.update { it.copy(selectedDevice = null, showPairDialog = false) }
    }

    // When user confirms "Pair" => start TCP pairing request
    fun requestPairing(device: Device) {
        log("requestPairing() -> ${device.name} ${device.ipAddress}:${device.port} id=${device.deviceId}")

        pairingManager.requestPairing(
            targetDeviceId = device.deviceId,
            targetName = device.name,
            targetHost = device.ipAddress,
            targetPort = device.port
        )

        _uiState.update {
            it.copy(selectedDevice = null, showPairDialog = false, message = "Pair request sent to ${device.name}")
        }
    }

    // Two-way approval: initiator confirms after remote accept
    fun confirmOutgoingPair(requestId: String) {
        log("confirmOutgoingPair($requestId)")
        pairingManager.confirmOutgoing(requestId)
    }

    fun cancelOutgoingPair(requestId: String) {
        log("cancelOutgoingPair($requestId)")
        pairingManager.cancelOutgoing(requestId)
    }

    // Receiver side: accept/reject incoming request
    fun acceptIncomingPair(requestId: String) {
        log("acceptIncomingPair($requestId)")
        pairingManager.approveIncoming(requestId)
        _uiState.update { it.copy(message = "Accepted pairing request") }
    }

    fun rejectIncomingPair(requestId: String) {
        log("rejectIncomingPair($requestId)")
        pairingManager.rejectIncoming(requestId)
        _uiState.update { it.copy(message = "Rejected pairing request") }
    }

    // Unpair = local only for now (simple)
    fun showUnpairDialog(device: Device) {
        _uiState.update { it.copy(selectedDevice = device, showUnpairDialog = true) }
    }

    fun dismissUnpairDialog() {
        _uiState.update { it.copy(selectedDevice = null, showUnpairDialog = false) }
    }

    fun unpairDevice(device: Device) {
        scope.launch {
            runCatching {
                // ✅ sends UNPAIR_REQUEST to the peer + revokes locally
                pairingManager.requestUnpair(device.deviceId)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        selectedDevice = null,
                        showUnpairDialog = false,
                        message = "Unpaired from ${device.name}"
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "Failed to unpair: ${e.message}") }
            }
        }
    }


    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    public override fun onCleared() {
        scope.cancel()
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