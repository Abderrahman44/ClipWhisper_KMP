package com.abdat.clipwhisper.network.data

import android.annotation.SuppressLint
import android.os.Build
import java.util.UUID

actual class DeviceInfoProvider {
    @SuppressLint("HardwareIds")
    actual fun getDeviceInfo(): DeviceInfo {
        val deviceId = "android-${Build.ID}-${UUID.randomUUID().toString().take(8)}"
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

        return DeviceInfo(
            deviceId = deviceId,
            deviceName = deviceName.ifBlank { "Android Device" },
            port = 8080
        )
    }
}