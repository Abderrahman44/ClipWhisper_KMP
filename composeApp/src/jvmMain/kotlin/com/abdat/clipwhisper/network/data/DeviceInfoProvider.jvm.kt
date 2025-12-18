package com.abdat.clipwhisper.network.data

import java.net.InetAddress
import java.util.UUID

// jvmMain
actual class DeviceInfoProvider {

    private val cached: DeviceInfo by lazy {
        val hostname = runCatching { InetAddress.getLocalHost().hostName }
            .getOrElse { "desktop" }

        val instanceSuffix = UUID.randomUUID().toString().take(8)

        val deviceId = "jvm-$hostname-$instanceSuffix"
        val deviceName = hostname.replaceFirstChar { it.uppercase() }.ifBlank { "Desktop Device" }

        DeviceInfo(
            deviceId = deviceId,
            deviceName = deviceName,
            port = 8080
        )
    }

    actual fun getDeviceInfo(): DeviceInfo = cached
}
