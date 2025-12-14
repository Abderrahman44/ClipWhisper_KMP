package com.abdat.clipwhisper.clipboard.domain

expect class ClipboardManager {
    fun getClipboardText(): String?
    fun setClipboardText(text: String): Boolean
    fun registerClipboardListener(onClipboardChanged: (String) -> Unit): ClipboardListener
    fun unregisterClipboardListener(listener: ClipboardListener)
}

expect class ClipboardListener