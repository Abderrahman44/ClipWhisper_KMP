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
    private fun log(msg: String) = println("ClipWhisper/DiscoveryManager: $msg")

    val discoveredDevices: StateFlow<List<Device>> = discovery.discoveredDevices
    val isRunning: StateFlow<Boolean> = discovery.isRunning
    val approvedDeviceIds: StateFlow<Set<String>> = pairedStore.approvedIds

    suspend fun startDiscovery(deviceId: String, deviceName: String, port: Int) {
        log("startDiscovery() selfId=$deviceId name=$deviceName port=$port")
        discovery.start(deviceId, deviceName, port)
        log("Discovery started. approvedIds=${pairedStore.getApprovedIds()}")
    }

    fun stopDiscovery() {
        log("stopDiscovery()")
        discovery.stop()
    }

    suspend fun pairDevice(deviceId: String) {
        log("pairDevice() requested deviceId=$deviceId")
        pairedStore.approve(deviceId)

        val approvedNow = pairedStore.isApproved(deviceId)
        log("pairDevice() store updated -> isApproved=$approvedNow approvedIds=${pairedStore.getApprovedIds()}")
    }

    suspend fun unpairDevice(deviceId: String) {
        log("unpairDevice() requested deviceId=$deviceId")
        pairedStore.revoke(deviceId)

        val approvedNow = pairedStore.isApproved(deviceId)
        log("unpairDevice() store updated -> isApproved=$approvedNow approvedIds=${pairedStore.getApprovedIds()}")
    }

    suspend fun isPaired(deviceId: String): Boolean {
        val result = pairedStore.isApproved(deviceId)
        log("isPaired($deviceId) -> $result")
        return result
    }

    fun getApprovedDevices(): List<Device> {
        val list = discoveredDevices.value.filter { it.isApproved }
        log("getApprovedDevices() -> ${list.size} approved devices currently visible in discovery list")
        return list
    }
}
