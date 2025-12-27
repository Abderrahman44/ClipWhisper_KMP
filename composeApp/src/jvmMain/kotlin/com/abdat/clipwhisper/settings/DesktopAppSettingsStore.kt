package com.abdat.clipwhisper.settings

import app.cash.sqldelight.db.use
import com.abdat.clipwhisper.network.data.DeviceInfoProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File


class DesktopAppSettingsStore(
    deviceInfoProvider: DeviceInfoProvider
) : AppSettingsStore
{

    private val mutex = Mutex()
    private val dir = File(System.getProperty("user.home"), ".clipwhisper").apply { mkdirs() }
    private val file = File(dir, "settings.properties")

    private val defaults = run {
        val base = deviceInfoProvider.getDeviceInfo()
        AppSettings(
            deviceName = base.deviceName,
            listenPort = base.port,
            maxTextSize = 64_000,
            ignoreEmptyOrWhitespace = true,
            historySizeLimit = 50,
            themeColor = 0xFF6750A4.toInt().toLong(),
            themeMode = ThemeMode.SYSTEM
        )
    }


    private val _settings = MutableStateFlow(load())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load(): AppSettings {
        val p = java.util.Properties()
        runCatching { if (file.exists()) file.inputStream().use(p::load) }

        val name = (p.getProperty("device_name") ?: defaults.deviceName).take(64)
        val port = p.getProperty("listen_port")?.toIntOrNull()?.coerceIn(1, 65535) ?: defaults.listenPort
        val max = p.getProperty("max_text_size")?.toIntOrNull()?.coerceIn(1, 64_000) ?: defaults.maxTextSize
        val ign = p.getProperty("ignore_empty_ws")?.toBooleanStrictOrNull() ?: defaults.ignoreEmptyOrWhitespace
        val hist = p.getProperty("history_limit")?.toIntOrNull()?.coerceIn(0, 10_000) ?: defaults.historySizeLimit
        val theme = p.getProperty("theme_color")?.toLongOrNull() ?: defaults.themeColor

        val mode = p.getProperty("theme_mode")
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: defaults.themeMode

        return AppSettings(name, port, max, ign, hist, theme, mode)   }

    private fun persist(s: AppSettings) {
        val p = java.util.Properties().apply {
            setProperty("device_name", s.deviceName)
            setProperty("listen_port", s.listenPort.toString())
            setProperty("max_text_size", s.maxTextSize.toString())
            setProperty("ignore_empty_ws", s.ignoreEmptyOrWhitespace.toString())
            setProperty("history_limit", s.historySizeLimit.toString())
            setProperty("theme_color", s.themeColor.toString())
            setProperty("theme_mode", s.themeMode.name)

        }

        runCatching {
            file.outputStream().use { out ->
                p.store(out, "ClipWhisper settings")
            }
        }
    }


    override suspend fun setDeviceName(value: String) {
        val v = value.trim().take(64)
        mutex.withLock {
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

    override suspend fun setThemeMode(mode: ThemeMode) {
        mutex.withLock {
            val updated = _settings.value.copy(themeMode = mode)
            persist(updated)
            _settings.value = updated
        }
    }



}
