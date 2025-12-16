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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap

actual class DeviceDiscovery actual constructor(
    private val pairedStore: PairedDeviceStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _discoveredDevices = MutableStateFlow<List<Device>>(emptyList())
    actual val discoveredDevices: StateFlow<List<Device>> = _discoveredDevices.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    actual val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val devices = ConcurrentHashMap<String, Device>()

    private var socket: DatagramSocket? = null
    private var jobs = mutableListOf<Job>()

    private var selfId: String = ""
    private var selfName: String = ""
    private var selfPort: Int = 0

    actual suspend fun start(deviceId: String, deviceName: String, port: Int) {
        if (_isRunning.value) {
            stop()
        }

        selfId = deviceId
        selfName = deviceName
        selfPort = port

        try {
            socket = DatagramSocket(DISCOVERY_PORT).apply {
                broadcast = true
                reuseAddress = true
                soTimeout = SOCKET_TIMEOUT_MS
            }

            _isRunning.value = true

            // Monitor pairing changes
            jobs += scope.launch {
                pairedStore.approvedIds.collect { approved ->
                    updateApprovalStatus(approved)
                }
            }

            jobs += scope.launch { broadcastLoop() }
            jobs += scope.launch { listenLoop() }
            jobs += scope.launch { cleanupLoop() }

        } catch (e: Exception) {
            handleError("Failed to start discovery", e)
            stop()
        }
    }

    actual fun stop() {
        _isRunning.value = false

        jobs.forEach { it.cancel() }
        jobs.clear()

        socket?.close()
        socket = null

        devices.clear()
        _discoveredDevices.value = emptyList()
    }

    private fun getBroadcastAddresses(): List<InetAddress> {
        val addresses = mutableListOf<InetAddress>()

        try {
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { nif ->
                if (nif.isUp && !nif.isLoopback) {
                    nif.interfaceAddresses.forEach { ia ->
                        ia.broadcast?.let { broadcast ->
                            if (broadcast is Inet4Address) {
                                addresses.add(broadcast)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            handleError("Failed to get broadcast addresses", e)
        }

        // Fallback to general broadcast
        if (addresses.isEmpty()) {
            try {
                addresses.add(InetAddress.getByName("255.255.255.255"))
            } catch (e: Exception) {
                handleError("Failed to add fallback broadcast", e)
            }
        }

        return addresses.distinct()
    }

    private suspend fun broadcastLoop() = coroutineScope {
        val s = socket ?: return@coroutineScope

        val packet = LanPacket(
            type = LanPacket.PacketType.DISCOVER,
            deviceId = selfId,
            name = selfName,
            port = selfPort
        )

        while (isActive && _isRunning.value) {
            try {
                val packetBytes = LanJson.encodeToString(
                    LanPacket.serializer(),
                    packet
                ).encodeToByteArray()

                getBroadcastAddresses().forEach { addr ->
                    try {
                        val datagram = DatagramPacket(
                            packetBytes,
                            packetBytes.size,
                            addr,
                            DISCOVERY_PORT
                        )
                        s.send(datagram)
                    } catch (e: Exception) {
                        // Ignore individual send failures
                    }
                }
            } catch (e: Exception) {
                handleError("Broadcast error", e)
            }

            delay(DISCOVER_INTERVAL_MS)
        }
    }

    private suspend fun listenLoop() = coroutineScope {
        val s = socket ?: return@coroutineScope
        val buffer = ByteArray(2048)

        while (isActive && _isRunning.value) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)

                withContext(Dispatchers.IO) {
                    s.receive(packet)
                }

                val json = packet.data.decodeToString(0, packet.length)
                val message = LanJson.decodeFromString(LanPacket.serializer(), json)

                if (message.deviceId == selfId) continue

                handleReceivedPacket(message, packet)

            } catch (e: SocketTimeoutException) {
                // Normal timeout, continue
            } catch (e: Exception) {
                if (_isRunning.value) {
                    handleError("Listen error", e)
                }
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

        try {
            val reply = LanPacket(
                type = LanPacket.PacketType.ANNOUNCE,
                deviceId = selfId,
                name = selfName,
                port = selfPort
            )

            val replyBytes = LanJson.encodeToString(
                LanPacket.serializer(),
                reply
            ).encodeToByteArray()

            val datagram = DatagramPacket(replyBytes, replyBytes.size, address, port)

            withContext(Dispatchers.IO) {
                s.send(datagram)
            }
        } catch (e: Exception) {
            handleError("Failed to send announcement", e)
        }
    }

    private fun addOrUpdateDevice(message: LanPacket, ip: String) {
        val approved = pairedStore.approvedIds.value.contains(message.deviceId)

        val device = Device(
            deviceId = message.deviceId,
            name = message.name,
            ipAddress = ip,
            port = message.port,
            isApproved = approved,
            lastSeen = System.currentTimeMillis()
        )

        devices[message.deviceId] = device
        publishDevices()
    }

    private suspend fun cleanupLoop() = coroutineScope {
        while (isActive && _isRunning.value) {
            delay(1000)

            val now = System.currentTimeMillis()
            val iterator = devices.entries.iterator()

            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (now - entry.value.lastSeen > STALE_AFTER_MS) {
                    iterator.remove()
                }
            }

            publishDevices()
        }
    }

    private fun updateApprovalStatus(approvedIds: Set<String>) {
        devices.replaceAll { deviceId, device ->
            device.withApprovalStatus(approvedIds.contains(deviceId))
        }
        publishDevices()
    }

    private fun publishDevices() {
        _discoveredDevices.value = devices.values
            .sortedWith(
                compareByDescending<Device> { it.isApproved }
                    .thenByDescending { it.lastSeen }
            )
    }

    private fun handleError(message: String, error: Exception) {
        // Log error (use your logging framework)
        println("DeviceDiscovery error: $message - ${error.message}")
    }
}