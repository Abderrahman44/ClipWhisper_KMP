package com.abdat.clipwhisper.core.nav

import kotlinx.serialization.Serializable

sealed class Routes() {
    @Serializable
     object ClipboardRoute : Routes()
    @Serializable
     object DevicesRoute : Routes()
    @Serializable
    object SettingsRoute : Routes()
}