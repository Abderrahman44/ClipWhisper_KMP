package com.abdat.clipwhisper.network.data

import com.abdat.clipwhisper.network.domain.PairedDeviceStore
import com.abdat.clipwhisper.network.domain.model.Device
import kotlinx.coroutines.flow.StateFlow

/**
 * Cross-platform manager for device discovery operations
 */
class DeviceDiscoveryManager(
    private val discovery: DeviceDiscovery,
    private val pairedStore: PairedDeviceStore
) {
    val discoveredDevices: StateFlow<List<Device>> = discovery.discoveredDevices
    val isRunning: StateFlow<Boolean> = discovery.isRunning
    val approvedDeviceIds: StateFlow<Set<String>> = pairedStore.approvedIds

    suspend fun startDiscovery(deviceId: String, deviceName: String, port: Int) {
        discovery.start(deviceId, deviceName, port)
    }

    fun stopDiscovery() {
        discovery.stop()
    }

    suspend fun pairDevice(deviceId: String) {
        pairedStore.approve(deviceId)
    }

    suspend fun unpairDevice(deviceId: String) {
        pairedStore.revoke(deviceId)
    }

    suspend fun isPaired(deviceId: String): Boolean =
        pairedStore.isApproved(deviceId)

    fun getApprovedDevices(): List<Device> =
        discoveredDevices.value.filter { it.isApproved }
}