package com.abdat.clipwhisper.network.data

import com.abdat.clipwhisper.network.domain.PairedDeviceStore
import com.abdat.clipwhisper.network.domain.model.DISCOVERY_PORT
import com.abdat.clipwhisper.network.domain.model.DISCOVER_INTERVAL_MS
import com.abdat.clipwhisper.network.domain.model.Device
import com.abdat.clipwhisper.network.domain.model.LanJson
import com.abdat.clipwhisper.network.domain.model.LanPacket
import com.abdat.clipwhisper.network.domain.model.SOCKET_TIMEOUT_MS
import com.abdat.clipwhisper.network.domain.model.STALE_AFTER_MS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap

// androidMain
actual class DeviceDiscovery actual constructor(
    private val pairedStore: PairedDeviceStore
) {
    private val startStopMutex = Mutex()

    private var runJob: Job? = null
    private var socket: DatagramSocket? = null

    private val devices = ConcurrentHashMap<String, Device>()

    private val _discoveredDevices = MutableStateFlow<List<Device>>(emptyList())
    actual val discoveredDevices: StateFlow<List<Device>> = _discoveredDevices.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    actual val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var selfId: String = ""
    private var selfName: String = ""
    private var selfPort: Int = 0

    private var broadcastAddresses: List<InetAddress> = emptyList()

    actual suspend fun start(deviceId: String, deviceName: String, port: Int) {
        startStopMutex.withLock {
            if (_isRunning.value) stopInternal()

            selfId = deviceId
            selfName = deviceName
            selfPort = port

            try {
                socket = DatagramSocket(DISCOVERY_PORT).apply {
                    broadcast = true
                    reuseAddress = true
                    soTimeout = SOCKET_TIMEOUT_MS
                }

                broadcastAddresses = getBroadcastAddresses()
                _isRunning.value = true

                // One parent job controls all discovery work
                runJob = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    launch {
                        pairedStore.approvedIds.collect { approved ->
                            updateApprovalStatus(approved)
                        }
                    }
                    launch { broadcastLoop() }
                    launch { listenLoop() }
                    launch { cleanupLoop() }
                }
            } catch (e: Exception) {
                handleError("Failed to start discovery", e)
                stopInternal()
            }
        }
    }

    actual fun stop() {
        // Non-suspending API: do best effort without blocking callers.
        // If you want strictly serialized stop/start, you can make stop() suspend.
        stopInternal()
    }

    private fun stopInternal() {
        _isRunning.value = false

        runJob?.cancel()
        runJob = null

        socket?.close()
        socket = null

        devices.clear()
        _discoveredDevices.value = emptyList()
    }

    private fun getBroadcastAddresses(): List<InetAddress> {
        val addresses = mutableListOf<InetAddress>()

        runCatching {
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { nif ->
                if (nif.isUp && !nif.isLoopback) {
                    nif.interfaceAddresses.forEach { ia ->
                        val b = ia.broadcast
                        if (b is Inet4Address) addresses += b
                    }
                }
            }
        }.onFailure { handleError("Failed to get broadcast addresses", it as Exception) }

        if (addresses.isEmpty()) {
            runCatching { addresses += InetAddress.getByName("255.255.255.255") }
                .onFailure { handleError("Failed to add fallback broadcast", it as Exception) }
        }

        return addresses.distinct()
    }

    private suspend fun broadcastLoop() {
        val s = socket ?: return

        val packet = LanPacket(
            type = LanPacket.PacketType.DISCOVER,
            deviceId = selfId,
            name = selfName,
            port = selfPort
        )

        while (currentCoroutineContext().isActive && _isRunning.value) {
            val payload = runCatching {
                LanJson.encodeToString(LanPacket.serializer(), packet).encodeToByteArray()
            }.getOrElse {
                handleError("Broadcast encode error", it as Exception)
                delay(DISCOVER_INTERVAL_MS)
                continue
            }

            for (addr in broadcastAddresses) {
                runCatching {
                    s.send(DatagramPacket(payload, payload.size, addr, DISCOVERY_PORT))
                }
                // ignore per-address failures
            }

            delay(DISCOVER_INTERVAL_MS)
        }
    }

    private suspend fun listenLoop() {
        val s = socket ?: return
        val buffer = ByteArray(2048)

        while (currentCoroutineContext().isActive && _isRunning.value) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                s.receive(packet)

                if (packet.length <= 0 || packet.length > buffer.size) continue

                val json = packet.data.decodeToString(0, packet.length)
                val message = runCatching {
                    LanJson.decodeFromString(LanPacket.serializer(), json)
                }.getOrElse {
                    // Ignore invalid/malicious packets
                    continue
                }

                if (message.deviceId == selfId) continue
                handleReceivedPacket(message, packet)

            } catch (e: SocketTimeoutException) {
                // Expected, allows loop to check cancellation
            } catch (e: Exception) {
                if (_isRunning.value) handleError("Listen error", e)
            }
        }
    }

    private suspend fun handleReceivedPacket(message: LanPacket, packet: DatagramPacket) {
        when (message.type) {
            LanPacket.PacketType.DISCOVER -> {
                sendAnnouncement(packet.address, packet.port)
            }
            LanPacket.PacketType.ANNOUNCE -> {
                val ip = packet.address.hostAddress ?: return
                addOrUpdateDevice(message, ip)
            }
        }
    }

    private suspend fun sendAnnouncement(address: InetAddress, port: Int) {
        val s = socket ?: return

        val reply = LanPacket(
            type = LanPacket.PacketType.ANNOUNCE,
            deviceId = selfId,
            name = selfName,
            port = selfPort
        )

        runCatching {
            val bytes = LanJson.encodeToString(LanPacket.serializer(), reply).encodeToByteArray()
            s.send(DatagramPacket(bytes, bytes.size, address, port))
        }.onFailure { handleError("Failed to send announcement", it as Exception) }
    }

    private fun addOrUpdateDevice(message: LanPacket, ip: String) {
        val approved = message.deviceId in pairedStore.approvedIds.value

        val safeName = message.name.take(64).ifBlank { "Unknown" }

        val device = Device(
            deviceId = message.deviceId,
            name = safeName,
            ipAddress = ip,
            port = message.port,
            isApproved = approved,
            lastSeen = System.currentTimeMillis()
        )

        devices[message.deviceId] = device
        publishDevices()
    }

    private suspend fun cleanupLoop() {
        while (currentCoroutineContext().isActive && _isRunning.value) {
            delay(1000)

            val now = System.currentTimeMillis()
            for ((id, d) in devices.entries) {
                if (now - d.lastSeen > STALE_AFTER_MS) {
                    devices.remove(id)
                }
            }

            publishDevices()
        }
    }

    private fun updateApprovalStatus(approvedIds: Set<String>) {
        for ((id, d) in devices.entries) {
            val approved = id in approvedIds
            if (d.isApproved != approved) {
                devices[id] = d.withApprovalStatus(approved)
            }
        }
        publishDevices()
    }

    private fun publishDevices() {
        val newList = devices.values.sortedWith(
            compareByDescending<Device> { it.isApproved }
                .thenByDescending { it.lastSeen }
        )

        if (_discoveredDevices.value != newList) {
            _discoveredDevices.value = newList
        }
    }

    private fun handleError(message: String, error: Exception) {
        println("DeviceDiscovery error: $message - ${error.message}")
    }
}
