package com.abdat.clipwhisper.network.data

data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val port: Int = 8080
)
