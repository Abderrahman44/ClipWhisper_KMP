package com.abdat.clipwhisper.network.data

import android.os.Build
import java.util.UUID

actual class DeviceInfoProvider {

    private val cached: DeviceInfo by lazy {
        // Still not "perfectly stable" across app restarts, but stable for this instance.
        val instanceSuffix = UUID.randomUUID().toString().take(8)

        val deviceId = "android-${Build.FINGERPRINT.take(40)}-$instanceSuffix"
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Android Device" }

        DeviceInfo(
            deviceId = deviceId,
            deviceName = deviceName,
            port = 8080
        )
    }

    actual fun getDeviceInfo(): DeviceInfo = cached
}
