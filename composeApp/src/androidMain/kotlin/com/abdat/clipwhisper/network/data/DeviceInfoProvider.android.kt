package com.abdat.clipwhisper.network.data

import android.content.Context
import android.os.Build
import java.util.UUID


actual class DeviceInfoProvider(
    private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences("clipwhisper_device", Context.MODE_PRIVATE)
    }

    private val cached: DeviceInfo by lazy {
        val storedId = prefs.getString(KEY_DEVICE_ID, null)
        val deviceId = storedId ?: run {
            // Generate once, persist forever (unless user clears app data)
            val newId = "android-${Build.FINGERPRINT.take(40)}-${UUID.randomUUID().toString().take(12)}"
            prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
            newId
        }

        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
            .trim()
            .ifBlank { "Android Device" }

        DeviceInfo(
            deviceId = deviceId,
            deviceName = deviceName,
            port = 8080
        )
    }

    actual fun getDeviceInfo(): DeviceInfo = cached

    private companion object {
        const val KEY_DEVICE_ID = "device_id"
    }
}

