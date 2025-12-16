package com.abdat.clipwhisper.network.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val DISCOVERY_PORT = 6997
internal const val DISCOVER_INTERVAL_MS = 1500L
internal const val STALE_AFTER_MS = 10_000L
internal const val SOCKET_TIMEOUT_MS = 5000

internal val LanJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

@Serializable
internal data class LanPacket(
    val type: PacketType,
    val deviceId: String,
    val name: String,
    val port: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class PacketType {
        DISCOVER, ANNOUNCE
    }
}

@Serializable
data class Device(
    val deviceId: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val isApproved: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
) {
    val isStale: Boolean
        get() = System.currentTimeMillis() - lastSeen > STALE_AFTER_MS

    fun withApprovalStatus(approved: Boolean): Device =
        copy(isApproved = approved)

    fun updateLastSeen(): Device =
        copy(lastSeen = System.currentTimeMillis())
}