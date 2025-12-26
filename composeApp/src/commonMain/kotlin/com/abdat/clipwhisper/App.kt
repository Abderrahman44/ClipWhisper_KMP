package com.abdat.clipwhisper

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.abdat.clipwhisper.clipboard.presentation.ClipboardScreen
import com.abdat.clipwhisper.core.nav.Routes
import com.abdat.clipwhisper.network.presentation.DeviceDiscoveryScreen
import com.abdat.clipwhisper.settings.SettingsScreen

@Composable
fun App() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = destination?.hasRoute<Routes.ClipboardRoute>() == true,
                    onClick = {
                        navController.navigate(Routes.ClipboardRoute) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    },
                    icon = { Icon(Icons.Outlined.ContentPaste, contentDescription = "Clipboard") },
                    label = { Text("Clipboard") }
                )

                NavigationBarItem(
                    selected = destination?.hasRoute<Routes.DevicesRoute>() == true,
                    onClick = {
                        navController.navigate(Routes.DevicesRoute) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    },
                    icon = { Icon(Icons.Outlined.Devices, contentDescription = "Devices") },
                    label = { Text("Devices") }
                )
                NavigationBarItem(
                    selected = destination?.hasRoute<Routes.SettingsRoute>() == true,
                    onClick = {
                        navController.navigate(Routes.SettingsRoute) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        }
                    },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.ClipboardRoute,
            modifier = Modifier.padding(padding)
        ) {
            composable<Routes.ClipboardRoute> {
                ClipboardScreen()
            }
            composable<Routes.DevicesRoute> {
                DeviceDiscoveryScreen()
            }
            composable<Routes.SettingsRoute> {
                SettingsScreen()
            }

        }
    }
}
