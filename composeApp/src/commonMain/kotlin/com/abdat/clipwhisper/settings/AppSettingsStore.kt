package com.abdat.clipwhisper.settings

import kotlinx.coroutines.flow.StateFlow

interface AppSettingsStore {
    val settings: StateFlow<AppSettings>

    suspend fun setDeviceName(value: String)
    suspend fun setListenPort(value: Int)
    suspend fun resetListenPort()
    suspend fun setMaxTextSize(value: Int)
    suspend fun setIgnoreEmptyOrWhitespace(value: Boolean)
    suspend fun setHistorySizeLimit(value: Int)
    suspend fun resetAll()
    suspend fun setThemeColor(color: Long)

}