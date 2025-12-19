package com.abdat.clipwhisper.clipboard.domain.models


data class ClipboardState(
    val currentClipboard: String = "",
    val inputText: String = "",
    val pinnedExpanded: Boolean = true,
    val pinned: List<ClipboardItem> = emptyList(),
    val history: List<ClipboardItem> = emptyList(),
    val isListening: Boolean = false,
    val statusMessage: String = ""
)