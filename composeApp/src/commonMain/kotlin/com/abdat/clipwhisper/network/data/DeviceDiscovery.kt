package com.abdat.clipwhisper.network.data

import com.abdat.clipwhisper.network.domain.PairedDeviceStore
import com.abdat.clipwhisper.network.domain.model.Device
import kotlinx.coroutines.flow.StateFlow

expect class DeviceDiscovery(
    pairedStore: PairedDeviceStore
) {
    val discoveredDevices: StateFlow<List<Device>>
    val isRunning: StateFlow<Boolean>

    suspend fun start(deviceId: String, deviceName: String, port: Int)
    fun stop()
}