package com.abdat.clipwhisper.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abdat.clipwhisper.network.data.DeviceInfoProvider
import com.abdat.clipwhisper.network.data.tcp.TcpPairingManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsStore: AppSettingsStore = koinInject(),
    deviceInfoProvider: DeviceInfoProvider = koinInject(),
    pairingManager: TcpPairingManager = koinInject(),
    appVersion: String = "1.0.0",
    githubUrl: String = "https://github.com/Abderrahman44",
    linkedInUrl: String = "https://www.linkedin.com/in/abderrahmane-abdat-3b2374284"
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    val settings by settingsStore.settings.collectAsState()
    val deviceInfo = remember(deviceInfoProvider) { deviceInfoProvider.getDeviceInfo() }
    val deviceId = deviceInfo.deviceId

    var deviceNameInput by remember { mutableStateOf("") }
    var portInput by remember { mutableStateOf("") }
    var maxTextSizeInput by remember { mutableStateOf("") }
    var historyLimitInput by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }

    LaunchedEffect(settings.deviceName) { deviceNameInput = settings.deviceName }
    LaunchedEffect(settings.listenPort) { portInput = settings.listenPort.toString() }
    LaunchedEffect(settings.maxTextSize) { maxTextSizeInput = settings.maxTextSize.toString() }
    LaunchedEffect(settings.historySizeLimit) {
        historyLimitInput = settings.historySizeLimit.toString()
    }

    fun showSnack(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    fun parsePortOrNull(s: String): Int? = s.toIntOrNull()?.takeIf { it in 1..65535 }
    fun parseMaxTextOrNull(s: String): Int? = s.toIntOrNull()?.takeIf { it in 1..64_000 }
    fun parseHistoryOrNull(s: String): Int? = s.toIntOrNull()?.takeIf { it in 0..10_000 }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset all settings?") },
            text = { Text("This will restore device identity, sync filters, and history limit to defaults.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    scope.launch {
                        settingsStore.resetAll()
                        showSnack("Settings reset")
                    }
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showThemePicker) {
        ThemePickerDialogCompact(
            currentThemeColor = settings.themeColor,
            onDismiss = { showThemePicker = false },
            onThemeSelected = { color ->
                scope.launch {
                    settingsStore.setThemeColor(color)
                    showSnack("Theme updated")
                }
                showThemePicker = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BrandTopAppBar(
                themeColor = settings.themeColor,
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = "Reset all settings")
                    }
                }
            )
        }

    ) { padding ->
        JbScreenBackground {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {

                item {
                    JbSectionCard(title = "Appearance", icon = Icons.Outlined.Palette) {
                        // keep your ThemeColorSelector, but make it look branded (below)
                        ThemeColorSelector(
                            currentColor = settings.themeColor,
                            onClick = { showThemePicker = true }
                        )
                        ThemeModeSelector(
                            current = settings.themeMode,
                            onSelect = { mode ->
                                scope.launch {
                                    settingsStore.setThemeMode(mode)
                                    showSnack("Theme mode updated")
                                }
                            }
                        )

                    }
                }

                item {
                    JbSectionCard(title = "Device Identity", icon = Icons.Outlined.DevicesOther) {
                        // keep your fields, but consider: Device ID looks more "dev" if it’s in tonal surface
                        OutlinedTextField(
                            value = deviceNameInput,
                            onValueChange = { deviceNameInput = it.take(64) },
                            label = { Text("Device name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )

                        OutlinedTextField(
                            value = deviceId,
                            onValueChange = {},
                            label = { Text("Device ID") },
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it.filter(Char::isDigit).take(5) },
                            label = { Text("Listening port") },
                            supportingText = { Text("1024–65535") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val name = deviceNameInput.trim()
                                    val port = parsePortOrNull(portInput)

                                    if (name.isBlank()) {
                                        showSnack("Device name cannot be empty")
                                        return@Button
                                    }
                                    if (port == null) {
                                        showSnack("Please input a valid port")
                                        return@Button
                                    }

                                    val err = portValidationError(port)
                                    if (err != null) {
                                        showSnack(err)
                                        return@Button
                                    }

                                    scope.launch {
                                        settingsStore.setDeviceName(name)
                                        val currentPort = settingsStore.settings.value.listenPort

                                        if (port == currentPort) {
                                            showSnack("Saved device identity")
                                            return@launch
                                        }

                                        val available = pairingManager.canListenOn(port)
                                        if (available) {
                                            settingsStore.setListenPort(port)
                                            showSnack("Saved device identity")
                                        } else {
                                            val defaultPort =
                                                deviceInfoProvider.getDeviceInfo().port
                                            val defaultOk = pairingManager.canListenOn(defaultPort)

                                            if (defaultOk) {
                                                settingsStore.setListenPort(defaultPort)
                                                showSnack("Port $port in use. Reverted to $defaultPort")
                                            } else {
                                                showSnack("Port unavailable. Choose another.")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Spacer(Modifier.width(8.dp))
                                Text("Save")
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    scope.launch {
                                        settingsStore.resetListenPort()
                                        showSnack("Port reset")
                                    }
                                }
                            ) { Icon(Icons.Outlined.RestartAlt, null) }
                        }
                    }
                }

                item {
                    JbSectionCard(title = "Sync Filters", icon = Icons.Outlined.FilterAlt) {
                        OutlinedTextField(
                            value = maxTextSizeInput,
                            onValueChange = { maxTextSizeInput = it.filter(Char::isDigit).take(6) },
                            label = { Text("Max text size") },
                            supportingText = { Text("1–64,000 characters") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )

                        JbPreferenceRow(
                            title = "Ignore empty/whitespace",
                            subtitle = "Skips blank content during sync",
                            trailing = {
                                Switch(
                                    checked = settings.ignoreEmptyOrWhitespace,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            settingsStore.setIgnoreEmptyOrWhitespace(
                                                checked
                                            )
                                        }
                                    }
                                )
                            }
                        )

                        Button(
                            onClick = {
                                val v = parseMaxTextOrNull(maxTextSizeInput)
                                if (v == null) {
                                    showSnack("Max text size must be 1-64,000")
                                    return@Button
                                }
                                scope.launch {
                                    settingsStore.setMaxTextSize(v)
                                    showSnack("Sync filters saved")
                                }

                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Spacer(Modifier.width(8.dp))
                            Text("Save filters")
                        }
                    }
                }

                item {
                    JbSectionCard(title = "Data & Maintenance", icon = Icons.Outlined.Storage) {
                        OutlinedTextField(
                            value = historyLimitInput,
                            onValueChange = {
                                historyLimitInput = it.filter(Char::isDigit).take(5)
                            },
                            label = { Text("History size limit") },
                            supportingText = { Text("0–10,000 items") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Button(
                            onClick = {
                                val v = parseHistoryOrNull(historyLimitInput)
                                if (v == null) {
                                    showSnack("History size must be 0-10,000")
                                    return@Button
                                }
                                scope.launch {
                                    settingsStore.setHistorySizeLimit(v)
                                    showSnack("History limit saved")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Spacer(Modifier.width(8.dp))
                            Text("Save limit")
                        }
                    }
                }

                item {
                    JbSectionCard(title = "About", icon = Icons.Outlined.Info) {
                        InfoRow("Version", appVersion)

                        Text(
                            "Open-source under Apache-2.0, MIT, and BSD-3-Clause licenses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(
                                onClick = { uriHandler.openUri(githubUrl) },
                                modifier = Modifier.weight(1f),
                                enabled = githubUrl.isNotBlank(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Outlined.Code, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("GitHub")
                            }

                            FilledTonalButton(
                                onClick = { uriHandler.openUri(linkedInUrl) },
                                modifier = Modifier.weight(1f),
                                enabled = linkedInUrl.isNotBlank(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Outlined.Person, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("LinkedIn")
                            }
                        }
                    }
                }
            }
        }
    }

}


@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ThemeColorSelector(
    currentColor: Long,
    onClick: () -> Unit
) {
    val fallbackColor = remember(currentColor) { Color(currentColor.toInt()) }
    val preset = remember(currentColor) { BrandThemePresets.findByStoredLong(currentColor) }
    val previewBrush = preset?.previewBrush

    val shape = RoundedCornerShape(18.dp)
    val stroke = remember { JbBrand.strokeBrush(alpha = 0.40f) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, stroke, shape),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        onClick = onClick
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Theme color",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    preset?.name ?: "Choose a KotlinConf-like accent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            JbColorDot(brush = previewBrush, fallback = fallbackColor)
        }
    }
}

@Composable
fun ThemePickerDialogCompact(
    currentThemeColor: Long,
    onDismiss: () -> Unit,
    onThemeSelected: (Long) -> Unit
) {
    val presets = remember { BrandThemePresets.presets }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(presets) { preset ->
                    val storedLong = remember(preset) { preset.seed.toArgb().toLong() }
                    ThemePresetItem(
                        brush = preset.previewBrush,
                        color = preset.seed,
                        isSelected = storedLong == currentThemeColor,
                        onClick = { onThemeSelected(storedLong) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun ThemePresetItem(
    brush: Brush?,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(brush ?: SolidColor(color))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeModeSelector(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Theme mode",
            style = MaterialTheme.typography.titleSmall
        )

        val items = listOf(
            Triple("System", Icons.Outlined.AutoMode, ThemeMode.SYSTEM),
            Triple("Light", Icons.Outlined.LightMode, ThemeMode.LIGHT),
            Triple("Dark", Icons.Outlined.DarkMode, ThemeMode.DARK),
        )

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, (label, icon, mode) ->
                SegmentedButton(
                    selected = current == mode,
                    onClick = { onSelect(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, items.size),
                    icon = {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                ) {
                    Text(label)
                }
            }
        }
    }
}


private fun portValidationError(port: Int): String? {
    if (port !in 1024..65535) return "Port must be between 1024 and 65535"

    val forbidden = setOf(1900, 5353, 53, 67, 68, 137, 138, 139, 445)
    if (port in forbidden) return "Port $port is reserved. Choose another"

    return null
}