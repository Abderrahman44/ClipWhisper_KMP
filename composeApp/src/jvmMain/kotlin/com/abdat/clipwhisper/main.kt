package com.abdat.clipwhisper

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.abdat.clipwhisper.clipboard.domain.ClipboardManager
import com.abdat.clipwhisper.clipboard.presentation.ClipboardScreen
import com.abdat.clipwhisper.core.di.ViewModelModules
import com.abdat.clipwhisper.core.di.initKoin
import com.abdat.clipwhisper.core.di.platformModuleDataBase
import com.abdat.clipwhisper.core.di.sharedDBModules
import org.koin.compose.koinInject

fun main() {
    application {
        initKoin {
                modules(
                    platformModuleDataBase,
                    sharedDBModules,
                    ViewModelModules
                )

        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "ClipWhisper",
        ) {
            ClipboardScreen( )
        }
    }
}
