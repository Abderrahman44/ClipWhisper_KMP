@file:Suppress("DEPRECATION")

package com.abdat.clipwhisper

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.abdat.clipwhisper.core.di.ViewModelModules
import com.abdat.clipwhisper.core.di.initKoin
import com.abdat.clipwhisper.core.di.platformModuleDataBase
import com.abdat.clipwhisper.core.di.sharedDBModules
import com.abdat.clipwhisper.network.di.NetworkModule
import com.abdat.clipwhisper.network.di.sharedNetworkModule
import com.abdat.clipwhisper.settings.AppSettingsStore
import com.abdat.clipwhisper.settings.ThemeMode
import com.materialkolor.dynamicColorScheme
import org.koin.compose.koinInject

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
            icon = painterResource("clipwhisper_pin_stack_icon_1024.png")
        ) {
            val settingsStore: AppSettingsStore = koinInject()

            AppThemed(settingsStore = settingsStore) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App()
                }
            }
        }
    }
}

@Composable
fun AppThemed(
    settingsStore: AppSettingsStore,
    content: @Composable () -> Unit
) {
    val settings by settingsStore.settings.collectAsState()

    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val seedColor = remember(settings.themeColor) {
        Color(settings.themeColor.toInt())
    }

    val colorScheme = dynamicColorScheme(
        seedColor = seedColor,
        isDark = darkTheme,
        isAmoled = false
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}