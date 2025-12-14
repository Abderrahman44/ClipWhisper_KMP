package com.abdat.clipwhisper.clipboard.domain.models

data class ClipboardItem(
    val id: Long,
    val type: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long?,
    val pinned: Boolean,
    val originDeviceId: String,
    val payload: String
)