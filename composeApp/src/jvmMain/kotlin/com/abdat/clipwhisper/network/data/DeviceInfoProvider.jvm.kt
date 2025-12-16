package com.abdat.clipwhisper.network.data

import java.net.InetAddress
import java.util.UUID

actual class DeviceInfoProvider {
    actual fun getDeviceInfo(): DeviceInfo {
        val hostname = try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            "desktop"
        }

        val deviceId = "jvm-$hostname-${UUID.randomUUID().toString().take(8)}"
        val deviceName = hostname.replaceFirstChar { it.uppercase() }

        return DeviceInfo(
            deviceId = deviceId,
            deviceName = deviceName.ifBlank { "Desktop Device" },
            port = 8080
        )
    }
}