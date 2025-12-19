package com.abdat.clipwhisper.core.domain.models

data class RemoteClipboardEvent(
    val fromDeviceId: String,
    val fromDeviceName: String,
    val text: String,
    val seq: Long,
    val timestamp: Long
)