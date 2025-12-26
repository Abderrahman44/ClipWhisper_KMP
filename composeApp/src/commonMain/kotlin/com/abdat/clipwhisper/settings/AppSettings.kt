package com.abdat.clipwhisper.settings

data class AppSettings(
    val deviceName: String,
    val listenPort: Int,
    val maxTextSize: Int,
    val ignoreEmptyOrWhitespace: Boolean,
    val historySizeLimit: Int,
    val themeColor: Long = 0xFF6750A4.toInt().toLong() // Default Material Purple

)