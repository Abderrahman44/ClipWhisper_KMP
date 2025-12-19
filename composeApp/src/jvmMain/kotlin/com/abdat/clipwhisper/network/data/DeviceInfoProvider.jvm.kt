package com.abdat.clipwhisper.network.data


import java.io.File
import java.net.InetAddress
import java.util.UUID

actual class DeviceInfoProvider {

    private val cached: DeviceInfo by lazy {
        val hostname = runCatching { InetAddress.getLocalHost().hostName }
            .getOrElse { "desktop" }

        val deviceId = loadOrCreateId(hostname)

        val deviceName = hostname
            .replaceFirstChar { it.uppercase() }
            .ifBlank { "Desktop Device" }

        DeviceInfo(
            deviceId = deviceId,
            deviceName = deviceName,
            port = 8080
        )
    }

    actual fun getDeviceInfo(): DeviceInfo = cached

    private fun loadOrCreateId(hostname: String): String {
        val dir = File(System.getProperty("user.home"), ".clipwhisper")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "device_id.txt")

        val existing = runCatching { file.readText().trim() }.getOrNull()
        if (!existing.isNullOrBlank()) return existing

        val newId = "jvm-$hostname-${UUID.randomUUID().toString().take(12)}"
        runCatching { file.writeText(newId) }
        return newId
    }
}
