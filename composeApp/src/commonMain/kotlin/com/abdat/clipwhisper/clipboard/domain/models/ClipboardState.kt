package com.abdat.clipwhisper.clipboard.domain.models

import kotlinx.serialization.Serializable


@Serializable
data class ClipboardState(
    val currentClipboard: String = "",
    val inputText: String = "",
    val history: List<String> = emptyList(),
    val isListening: Boolean = false,
    val statusMessage: String = ""
)