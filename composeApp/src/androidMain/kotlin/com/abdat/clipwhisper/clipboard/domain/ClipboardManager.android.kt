package com.abdat.clipwhisper.clipboard.domain


import android.content.ClipData
import android.content.ClipboardManager as AndroidClipboardManager
import android.content.Context
import android.util.Log



actual class ClipboardManager(private val context: Context) {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as AndroidClipboardManager
    private val TAG = "ClipboardManager"

    actual fun getClipboardText(): String? {
        return try {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                if (text != null) {
                    Log.d(TAG, "Clipboard read: ${text.take(50)}...")
                }
                text
            } else {
                Log.d(TAG, "Clipboard is empty")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting clipboard text: ${e.message}")
            null
        }
    }

    actual fun setClipboardText(text: String): Boolean {
        return try {
            val clipData = ClipData.newPlainText("ClipboardManager", text)
            clipboardManager.setPrimaryClip(clipData)
            Log.d(TAG, "Text set to clipboard: ${text.take(50)}...")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting clipboard text: ${e.message}")
            false
        }
    }

    actual fun registerClipboardListener(onClipboardChanged: (String) -> Unit): ClipboardListener {
        val listener = AndroidClipboardManager.OnPrimaryClipChangedListener {
            try {
                val clipText = getClipboardText()
                if (!clipText.isNullOrEmpty()) {
                    Log.d(TAG, "Clipboard changed: ${clipText.take(50)}...")
                    onClipboardChanged(clipText)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in clipboard listener: ${e.message}")
            }
        }
        clipboardManager.addPrimaryClipChangedListener(listener)
        Log.d(TAG, "Clipboard listener registered")
        return ClipboardListener(listener)
    }

    actual fun unregisterClipboardListener(listener: ClipboardListener) {
        try {
            clipboardManager.removePrimaryClipChangedListener(listener.androidListener)
            Log.d(TAG, "Clipboard listener unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering clipboard listener: ${e.message}")
        }
    }
}

actual class ClipboardListener(val androidListener: AndroidClipboardManager.OnPrimaryClipChangedListener)
const val CLIPBOARD_CHANGED_ACTION = "com.abdat.clipwhisper.CLIPBOARD_CHANGED"
const val CLIPBOARD_SET_ACTION = "com.abdat.clipwhisper.CLIPBOARD_SET"
const val CLIPBOARD_TEXT_EXTRA = "clipboard_text"