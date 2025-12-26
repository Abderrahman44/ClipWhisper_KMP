package com.abdat.clipwhisper.settings

import android.content.Context
import androidx.core.content.edit
import com.abdat.clipwhisper.network.data.DeviceInfoProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AndroidAppSettingsStore(
    context: Context,
    deviceInfoProvider: DeviceInfoProvider
) : AppSettingsStore
{

    private val mutex = Mutex()
    private val prefs = context.getSharedPreferences("clipwhisper_settings", Context.MODE_PRIVATE)

    private val defaults = run {
        val base = deviceInfoProvider.getDeviceInfo()
        AppSettings(
            deviceName = base.deviceName,
            listenPort = base.port,
            maxTextSize = 64_000,
            ignoreEmptyOrWhitespace = true,
            historySizeLimit = 50,
            themeColor = 0xFF6750A4.toInt().toLong()
        )
    }

    private val _settings = MutableStateFlow(load())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()


    private fun load(): AppSettings {
        val name = prefs.getString("device_name", null) ?: defaults.deviceName
        val port = prefs.getInt("listen_port", defaults.listenPort).coerceIn(1, 65535)
        val max = prefs.getInt("max_text_size", defaults.maxTextSize).coerceIn(1, 64_000)
        val ign = prefs.getBoolean("ignore_empty_ws", defaults.ignoreEmptyOrWhitespace)
        val hist = prefs.getInt("history_limit", defaults.historySizeLimit).coerceIn(0, 10_000)
        val theme = prefs.getLong("theme_color", defaults.themeColor)
        return AppSettings(name, port, max, ign, hist, theme)    }

    private fun persist(s: AppSettings) {
        prefs.edit {
            putString("device_name", s.deviceName)
                .putInt("listen_port", s.listenPort)
                .putInt("max_text_size", s.maxTextSize)
                .putBoolean("ignore_empty_ws", s.ignoreEmptyOrWhitespace)
                .putInt("history_limit", s.historySizeLimit)
                .putLong("theme_color", s.themeColor)
        }
    }

    override suspend fun setDeviceName(value: String) {
        val v = value.trim().take(64)
        mutex.withLock  {
            val next = _settings.value.copy(deviceName = v)
            _settings.value = next
            persist(next)
        }
    }

    override suspend fun setListenPort(value: Int) {
        val v = value.coerceIn(1, 65535)
        mutex.withLock {
            val next = _settings.value.copy(listenPort = v)
            _settings.value = next
            persist(next)
        }
    }

    override suspend fun resetListenPort() {
        mutex.withLock {
            val next = _settings.value.copy(listenPort = defaults.listenPort)
            _settings.value = next
            persist(next)
        }
    }

    override suspend fun setMaxTextSize(value: Int) {
        val v = value.coerceIn(1, 64_000)
        mutex.withLock {
            val next = _settings.value.copy(maxTextSize = v)
            _settings.value = next
            persist(next)
        }
    }

    override suspend fun setIgnoreEmptyOrWhitespace(value: Boolean) {
        mutex.withLock {
            val next = _settings.value.copy(ignoreEmptyOrWhitespace = value)
            _settings.value = next
            persist(next)
        }
    }

    override suspend fun setHistorySizeLimit(value: Int) {
        val v = value.coerceIn(0, 10_000)
        mutex.withLock {
            val next = _settings.value.copy(historySizeLimit = v)
            _settings.value = next
            persist(next)
        }
    }

    override suspend fun resetAll() {
        mutex.withLock {
            _settings.value = defaults
            persist(defaults)
        }
    }

    override suspend fun setThemeColor(color: Long) {
        mutex.withLock {
            val next = _settings.value.copy(themeColor = color)
            _settings.value = next
            persist(next)
        }
    }

}
