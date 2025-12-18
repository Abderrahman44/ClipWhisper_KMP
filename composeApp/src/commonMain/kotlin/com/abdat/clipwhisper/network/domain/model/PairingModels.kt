package com.abdat.clipwhisper.network.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PairingPacket(
    val type: Type,
    val requestId: String? = null,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val port: Int? = null,
    val reason: String? = null
) {
    @Serializable
    enum class Type {
        HELLO,
        PAIR_REQUEST,
        PAIR_ACCEPT,
        PAIR_REJECT,
        PAIR_CONFIRM,
        PAIR_CANCEL,
        PAIR_DONE,
        ERROR
    }
}

internal object PairingJson {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
data class IncomingPairRequest(
    val requestId: String,
    val fromDeviceId: String,
    val fromDeviceName: String,
    val fromAddress: String
)

enum class OutgoingPairStatus {
    CONNECTING,
    WAITING_REMOTE_APPROVAL,
    WAITING_LOCAL_CONFIRM,
    PAIRED,
    REJECTED,
    FAILED
}

data class OutgoingPairRequest(
    val requestId: String,
    val toDeviceId: String,
    val toDeviceName: String,
    val toAddress: String,
    val status: OutgoingPairStatus,
    val error: String? = null
)
