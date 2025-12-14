package com.abdat.clipwhisper.clipboard.domain

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Clipboard
import kotlinx.coroutines.*

actual class ClipboardManager {
    private val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
    private val scope = CoroutineScope(Dispatchers.Default)

    actual fun getClipboardText(): String? {
        return try {
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                clipboard.getData(DataFlavor.stringFlavor) as? String
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error getting clipboard text: ${e.message}")
            null
        }
    }

    actual fun setClipboardText(text: String): Boolean {
        return try {
            val selection = StringSelection(text)
            clipboard.setContents(selection, null)
            println("Text set to clipboard: ${text.take(50)}...")
            true
        } catch (e: Exception) {
            println("Error setting clipboard text: ${e.message}")
            false
        }
    }

    actual fun registerClipboardListener(onClipboardChanged: (String) -> Unit): ClipboardListener {
        val listener = ClipboardListener()

        listener.job = scope.launch {
            var lastClipboard = getClipboardText()

            while (isActive) {
                delay(500) // Poll every 500ms
                try {
                    val currentClipboard = getClipboardText()
                    if (currentClipboard != null && currentClipboard != lastClipboard) {
                        println("Clipboard changed: ${currentClipboard.take(50)}...")
                        onClipboardChanged(currentClipboard)
                        lastClipboard = currentClipboard
                    }
                } catch (e: Exception) {
                    println("Error in clipboard listener: ${e.message}")
                }
            }
        }

        println("Clipboard listener registered")
        return listener
    }

    actual fun unregisterClipboardListener(listener: ClipboardListener) {
        listener.job?.cancel()
        println("Clipboard listener unregistered")
    }
}

actual class ClipboardListener {
    var job: Job? = null
}