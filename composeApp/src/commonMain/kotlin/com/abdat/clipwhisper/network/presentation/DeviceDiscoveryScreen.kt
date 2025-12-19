package com.abdat.clipwhisper.network.presentation


import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abdat.clipwhisper.network.domain.model.Device
import com.abdat.clipwhisper.network.domain.model.IncomingPairRequest
import com.abdat.clipwhisper.network.domain.model.OutgoingPairRequest
import com.abdat.clipwhisper.network.domain.model.OutgoingPairStatus
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDiscoveryScreen(
    viewModel: DeviceDiscoveryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val devices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val isDiscovering by viewModel.isDiscovering.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
    val unpairedDevices by viewModel.unpairededDevices.collectAsStateWithLifecycle()
    val incomingRequests by viewModel.incomingPairRequests.collectAsStateWithLifecycle()
    val outgoingRequests by viewModel.outgoingPairRequests.collectAsStateWithLifecycle()


    // Handle dialogs
    if (uiState.showPairDialog && uiState.selectedDevice != null) {
        PairDeviceDialog(
            device = uiState.selectedDevice!!,
            onConfirm = { viewModel.requestPairing(uiState.selectedDevice!!) },
            onDismiss = { viewModel.dismissPairDialog() }
        )
    }
    // Receiver side: incoming request dialog (show first pending)
    incomingRequests.firstOrNull()?.let { req ->
        IncomingPairRequestDialog(
            req = req,
            onAccept = { viewModel.acceptIncomingPair(req.requestId) },
            onReject = { viewModel.rejectIncomingPair(req.requestId) }
        )
    }

// Initiator side: two-way confirm after remote accepts
    outgoingRequests.firstOrNull { it.status == OutgoingPairStatus.WAITING_LOCAL_CONFIRM }?.let { req ->
        ConfirmOutgoingPairDialog(
            req = req,
            onConfirm = { viewModel.confirmOutgoingPair(req.requestId) },
            onCancel = { viewModel.cancelOutgoingPair(req.requestId) }
        )
    }


    if (uiState.showUnpairDialog && uiState.selectedDevice != null) {
        UnpairDeviceDialog(
            device = uiState.selectedDevice!!,
            onConfirm = { viewModel.unpairDevice(uiState.selectedDevice!!) },
            onDismiss = { viewModel.dismissUnpairDialog() }
        )
    }

    // Snackbar host
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Device Discovery",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                actions = {
                    // Discovery toggle
                    IconButton(
                        onClick = {
                            if (isDiscovering) {
                                viewModel.stopDiscovery()
                            } else {
                                viewModel.startDiscovery()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isDiscovering) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isDiscovering) "Stop Discovery" else "Start Discovery",
                            tint = if (isDiscovering) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status Card
            DiscoveryStatusCard(
                isDiscovering = isDiscovering,
                deviceCount = devices.size,
                pairedCount = pairedDevices.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Device Lists
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Paired Devices Section
                if (pairedDevices.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Paired Devices",
                            count = pairedDevices.size,
                            icon = Icons.Default.CheckCircle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(
                        items = pairedDevices,
                        key = { it.deviceId }
                    ) { device ->
                        DeviceCard(
                            device = device,
                            isPaired = true,
                            onAction = { viewModel.showUnpairDialog(device) },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Available Devices Section
                if (unpairedDevices.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Available Devices",
                            count = unpairedDevices.size,
                            icon = Icons.Default.Devices,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    items(
                        items = unpairedDevices,
                        key = { it.deviceId }
                    ) { device ->
                        DeviceCard(
                            device = device,
                            isPaired = false,
                            onAction = { viewModel.showPairDialog(device) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                // Empty state
                if (devices.isEmpty() && isDiscovering) {
                    item {
                        EmptyStateCard(
                            message = "Searching for devices...",
                            isLoading = true
                        )
                    }
                }

                if (devices.isEmpty() && !isDiscovering) {
                    item {
                        EmptyStateCard(
                            message = "No devices found. Start discovery to search.",
                            isLoading = false
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

   /* DisposableEffect(Unit) {
        onDispose { viewModel.onCleared() }
    }*/

}

@Composable
private fun DiscoveryStatusCard(
    isDiscovering: Boolean,
    deviceCount: Int,
    pairedCount: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isDiscovering) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(if (isDiscovering) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                if (isDiscovering) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            )
                    )

                    Text(
                        text = if (isDiscovering) "Discovering..." else "Inactive",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatusChip(
                        icon = Icons.Default.Devices,
                        label = "$deviceCount found",
                        color = MaterialTheme.colorScheme.secondary
                    )

                    StatusChip(
                        icon = Icons.Default.CheckCircle,
                        label = "$pairedCount paired",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (isDiscovering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.2f)
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DeviceCard(
    device: Device,
    isPaired: Boolean,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onAction),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaired) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Device Icon
                Surface(
                    shape = CircleShape,
                    color = if (isPaired) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isPaired) Icons.Default.CheckCircle else Icons.Default.Smartphone,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = if (isPaired) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }

                // Device Info
                Column {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${device.ipAddress}:${device.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isPaired) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Connected",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Action Button
            FilledTonalButton(
                onClick = onAction,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isPaired) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                )
            ) {
                Icon(
                    imageVector = if (isPaired) Icons.Default.LinkOff else Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isPaired) "Unpair" else "Pair")
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    message: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PairDeviceDialog(
    device: Device,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("Pair Device")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Do you want to pair with this device?")

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${device.ipAddress}:${device.port}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onConfirm) {
                Text("Pair")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun UnpairDeviceDialog(
    device: Device,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.LinkOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Unpair Device")
        },
        text = {
            Text("Are you sure you want to unpair from ${device.name}?")
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onConfirm,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text("Unpair")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
@Composable
private fun IncomingPairRequestDialog(
    req: IncomingPairRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        icon = { Icon(Icons.Default.Link, contentDescription = null) },
        title = { Text("Pair request") },
        text = {
            Column {
                Text("A device wants to pair with you:")
                Spacer(Modifier.height(8.dp))
                Text("• Name: ${req.fromDeviceName}")
                Text("• Id: ${req.fromDeviceId}")
                Text("• From: ${req.fromAddress}")
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onAccept) { Text("Accept") }
        },
        dismissButton = {
            TextButton(onClick = onReject) { Text("Reject") }
        }
    )
}

@Composable
private fun ConfirmOutgoingPairDialog(
    req: OutgoingPairRequest,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
        title = { Text("Confirm pairing") },
        text = {
            Column {
                Text("${req.toDeviceName} accepted your request.")
                Spacer(Modifier.height(8.dp))
                Text("Confirm on this device to finish (two-way approval).")
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onConfirm) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}
