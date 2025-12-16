package com.abdat.clipwhisper

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.abdat.clipwhisper.core.di.ViewModelModules
import com.abdat.clipwhisper.core.di.initKoin
import com.abdat.clipwhisper.core.di.platformModuleDataBase
import com.abdat.clipwhisper.core.di.sharedDBModules
import com.abdat.clipwhisper.network.di.NetworkModule
import com.abdat.clipwhisper.network.di.sharedNetworkModule

fun main() {
    application {
        initKoin {
                modules(
                    platformModuleDataBase,
                    sharedDBModules,
                    ViewModelModules,
                    NetworkModule,
                    sharedNetworkModule
                )

        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "ClipWhisper",
        ) {
           // ClipboardScreen( )
            DesktopTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    //DeviceDiscoveryScreen()
                    App()
                }
            }
        }
    }
}

@Composable
private fun DesktopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}