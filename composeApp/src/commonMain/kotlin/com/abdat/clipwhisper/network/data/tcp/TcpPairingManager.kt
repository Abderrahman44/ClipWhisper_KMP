package com.abdat.clipwhisper.network.data.tcp


import com.abdat.clipwhisper.network.data.DeviceInfoProvider
import com.abdat.clipwhisper.network.domain.PairedDeviceStore
import com.abdat.clipwhisper.network.domain.model.IncomingPairRequest
import com.abdat.clipwhisper.network.domain.model.OutgoingPairRequest
import com.abdat.clipwhisper.network.domain.model.OutgoingPairStatus
import com.abdat.clipwhisper.network.domain.model.PairingJson
import com.abdat.clipwhisper.network.domain.model.PairingPacket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

class TcpPairingManager(
    private val pairedStore: PairedDeviceStore,
    private val deviceInfoProvider: DeviceInfoProvider,
) {
    private fun log(msg: String) = println("ClipWhisper/Pairing: $msg")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _incoming = MutableStateFlow<List<IncomingPairRequest>>(emptyList())
    val incomingRequests: StateFlow<List<IncomingPairRequest>> = _incoming.asStateFlow()

    private val _outgoing = MutableStateFlow<List<OutgoingPairRequest>>(emptyList())
    val outgoingRequests: StateFlow<List<OutgoingPairRequest>> = _outgoing.asStateFlow()

    private val mutex = Mutex()

    private var listener: TcpListener? = null
    private var serverJob: Job? = null

    private data class IncomingSession(
        val requestId: String,
        val peerId: String,
        val peerName: String,
        val conn: TcpConnection,
        val decision: CompletableDeferred<Boolean>
    )

    private data class OutgoingSession(
        val requestId: String,
        val peerId: String,
        val peerName: String,
        val address: String,
        val confirm: CompletableDeferred<Boolean>,
        var conn: TcpConnection? = null
    )

    private val incomingSessions = mutableMapOf<String, IncomingSession>()
    private val outgoingSessions = mutableMapOf<String, OutgoingSession>()

    fun startServer() {
        val self = deviceInfoProvider.getDeviceInfo()

        if (serverJob?.isActive == true) {
            log("TCP server already running on port=${self.port}")
            return
        }

        log("Starting TCP server on port=${self.port} (selfId=${self.deviceId}, name=${self.deviceName})")

        try {
            listener = tcpListen(self.port)
        } catch (e: Exception) {
            log("Failed to listen on port=${self.port}: ${e.message}")
            return
        }

        serverJob = scope.launch {
            val l = listener ?: return@launch
            try {
                while (isActive) {
                    val conn = l.accept()
                    log("Accepted connection from ${conn.remoteAddress}")
                    launch { handleIncomingConnection(conn) }
                }
            } catch (e: Exception) {
                if (isActive) log("Server loop error: ${e.message}")
            }
        }
    }

    fun stopServer() {
        log("Stopping TCP server")
        serverJob?.cancel()
        serverJob = null

        listener?.close()
        listener = null

        scope.launch {
            mutex.withLock {
                incomingSessions.values.forEach { it.conn.close() }
                outgoingSessions.values.forEach { it.conn?.close() }
                incomingSessions.clear()
                outgoingSessions.clear()
            }
            _incoming.value = emptyList()
            _outgoing.value = emptyList()
        }
    }

    /**
     * Initiator: user taps a discovered device to pair.
     * Returns requestId immediately (UI can track).
     */
    fun requestPairing(
        targetDeviceId: String,
        targetName: String,
        targetHost: String,
        targetPort: Int
    ): String {
        val requestId = randomId()
        val address = "$targetHost:$targetPort"

        updateOutgoing(
            OutgoingPairRequest(
                requestId = requestId,
                toDeviceId = targetDeviceId,
                toDeviceName = targetName,
                toAddress = address,
                status = OutgoingPairStatus.CONNECTING
            )
        )

        val session = OutgoingSession(
            requestId = requestId,
            peerId = targetDeviceId,
            peerName = targetName,
            address = address,
            confirm = CompletableDeferred()
        )

        scope.launch {
            mutex.withLock { outgoingSessions[requestId] = session }

            try {
                val self = deviceInfoProvider.getDeviceInfo()

                log("Connecting to $address (requestId=$requestId)")
                val conn = tcpConnect(targetHost, targetPort)
                session.conn = conn

                // HELLO exchange
                send(conn, PairingPacket(
                    type = PairingPacket.Type.HELLO,
                    deviceId = self.deviceId,
                    deviceName = self.deviceName,
                    port = self.port
                )
                )

                val peerHello = receive(conn, timeoutMs = 5_000)
                require(peerHello.type == PairingPacket.Type.HELLO) { "Expected HELLO, got ${peerHello.type}" }
                log("HELLO from ${conn.remoteAddress}: id=${peerHello.deviceId} name=${peerHello.deviceName}")

                // PAIR_REQUEST
                send(conn, PairingPacket(
                    type = PairingPacket.Type.PAIR_REQUEST,
                    requestId = requestId,
                    deviceId = self.deviceId,
                    deviceName = self.deviceName,
                    port = self.port
                ))
                updateOutgoingStatus(requestId, OutgoingPairStatus.WAITING_REMOTE_APPROVAL)

                // Wait accept/reject
                val decision = receive(conn, timeoutMs = 30_000)
                when (decision.type) {
                    PairingPacket.Type.PAIR_ACCEPT -> {
                        log("Remote accepted (requestId=$requestId). Waiting LOCAL confirm…")
                        updateOutgoingStatus(requestId, OutgoingPairStatus.WAITING_LOCAL_CONFIRM)

                        val confirmed = withTimeoutOrNull(60_000) { session.confirm.await() } ?: false
                        if (!confirmed) {
                            send(conn, PairingPacket(type = PairingPacket.Type.PAIR_CANCEL, requestId = requestId))
                            updateOutgoingFailed(requestId, "Local canceled or timed out")
                            conn.close()
                            return@launch
                        }

                        // Two-way approval complete: confirm
                        send(conn, PairingPacket(type = PairingPacket.Type.PAIR_CONFIRM, requestId = requestId))

                        // Store approval locally
                        pairedStore.approve(targetDeviceId)
                        log("PAIRED (initiator). approvedIds=${pairedStore.getApprovedIds()}")

                        send(conn, PairingPacket(type = PairingPacket.Type.PAIR_DONE, requestId = requestId))
                        updateOutgoingStatus(requestId, OutgoingPairStatus.PAIRED)
                        conn.close()
                    }

                    PairingPacket.Type.PAIR_REJECT -> {
                        val reason = decision.reason ?: "rejected"
                        log("Remote rejected (requestId=$requestId) reason=$reason")
                        updateOutgoingRejected(requestId, reason)
                        conn.close()
                    }

                    else -> {
                        updateOutgoingFailed(requestId, "Unexpected response: ${decision.type}")
                        conn.close()
                    }
                }
            } catch (e: Exception) {
                log("Outgoing pairing failed (requestId=$requestId): ${e.message}")
                updateOutgoingFailed(requestId, e.message ?: "error")
                session.conn?.close()
            } finally {
                mutex.withLock { outgoingSessions.remove(requestId) }
            }
        }

        return requestId
    }

    // Initiator confirms after remote accept
    fun confirmOutgoing(requestId: String) {
        scope.launch {
            mutex.withLock { outgoingSessions[requestId]?.confirm?.complete(true) }
        }
    }

    fun cancelOutgoing(requestId: String) {
        scope.launch {
            mutex.withLock { outgoingSessions[requestId]?.confirm?.complete(false) }
        }
    }

    // Receiver decides on incoming request
    fun approveIncoming(requestId: String) {
        scope.launch {
            mutex.withLock { incomingSessions[requestId]?.decision?.complete(true) }
        }
    }

    fun rejectIncoming(requestId: String) {
        scope.launch {
            mutex.withLock { incomingSessions[requestId]?.decision?.complete(false) }
        }
    }

    // -------------------- Incoming handler --------------------

    private suspend fun handleIncomingConnection(conn: TcpConnection) {
        val self = deviceInfoProvider.getDeviceInfo()

        var requestId: String? = null

        try {
            // HELLO exchange
            send(conn, PairingPacket(
                type = PairingPacket.Type.HELLO,
                deviceId = self.deviceId,
                deviceName = self.deviceName,
                port = self.port
            ))

            val peerHello = receive(conn, timeoutMs = 5_000)
            require(peerHello.type == PairingPacket.Type.HELLO) { "Expected HELLO, got ${peerHello.type}" }

            val peerId = peerHello.deviceId ?: "unknown"
            val peerName = peerHello.deviceName ?: "Unknown"
            log("HELLO from ${conn.remoteAddress}: id=$peerId name=$peerName")

            // Expect PAIR_REQUEST
            val req = receive(conn, timeoutMs = 30_000)
            if (req.type != PairingPacket.Type.PAIR_REQUEST || req.requestId == null) {
                log("Expected PAIR_REQUEST, got ${req.type}. Closing.")
                conn.close()
                return
            }

            requestId = req.requestId
            val fromId = req.deviceId ?: peerId
            val fromName = (req.deviceName ?: peerName).take(64)

            val session = IncomingSession(
                requestId = requestId,
                peerId = fromId,
                peerName = fromName,
                conn = conn,
                decision = CompletableDeferred()
            )

            mutex.withLock { incomingSessions[requestId] = session }

            _incoming.update {
                it + IncomingPairRequest(
                    requestId = requestId,
                    fromDeviceId = fromId,
                    fromDeviceName = fromName,
                    fromAddress = conn.remoteAddress
                )
            }

            log("Incoming PAIR_REQUEST requestId=$requestId fromId=$fromId name=$fromName")

            // Wait user decision (receiver approval)
            val approved = withTimeoutOrNull(60_000) { session.decision.await() } ?: false

            _incoming.update { list -> list.filterNot { it.requestId == requestId } }

            if (!approved) {
                send(conn, PairingPacket(type = PairingPacket.Type.PAIR_REJECT, requestId = requestId, reason = "User rejected or timed out"))
                log("Receiver rejected/timed out requestId=$requestId")
                conn.close()
                return
            }

            // Receiver accepts
            send(conn, PairingPacket(type = PairingPacket.Type.PAIR_ACCEPT, requestId = requestId))
            log("Receiver accepted requestId=$requestId. Waiting initiator confirm…")

            // Wait initiator confirm
            val confirm = receive(conn, timeoutMs = 30_000)
            when (confirm.type) {
                PairingPacket.Type.PAIR_CONFIRM -> {
                    pairedStore.approve(fromId)
                    log("PAIRED (receiver). approvedIds=${pairedStore.getApprovedIds()}")
                    send(conn, PairingPacket(type = PairingPacket.Type.PAIR_DONE, requestId = requestId))
                }
                PairingPacket.Type.PAIR_CANCEL -> log("Initiator canceled requestId=$requestId")
                else -> log("Unexpected while waiting confirm: ${confirm.type}")
            }

            conn.close()
        } catch (e: Exception) {
            log("Incoming pairing error: ${e.message}")
            conn.close()
        } finally {
            if (requestId != null) {
                mutex.withLock { incomingSessions.remove(requestId) }
                _incoming.update { list -> list.filterNot { it.requestId == requestId } }
            }
        }
    }

    // -------------------- IO helpers --------------------

    private suspend fun send(conn: TcpConnection, packet: PairingPacket) {
        val bytes = PairingJson.json.encodeToString(PairingPacket.serializer(), packet).encodeToByteArray()
        conn.writeFrame(bytes)
        log("-> ${packet.type} to ${conn.remoteAddress} req=${packet.requestId}")
    }

    private suspend fun receive(conn: TcpConnection, timeoutMs: Long): PairingPacket {
        val bytes = withTimeout(timeoutMs) {
            conn.readFrame() ?: throw CancellationException("Connection closed")
        }
        val jsonStr = bytes.decodeToString()
        val pkt = runCatching {
            PairingJson.json.decodeFromString(PairingPacket.serializer(), jsonStr)
        }.getOrElse {
            conn.close()
            throw IllegalStateException("Invalid packet JSON")
        }
        log("<- ${pkt.type} from ${conn.remoteAddress} req=${pkt.requestId}")
        return pkt
    }

    private fun updateOutgoing(req: OutgoingPairRequest) {
        _outgoing.update { list -> list.filterNot { it.requestId == req.requestId } + req }
    }

    private fun updateOutgoingStatus(requestId: String, status: OutgoingPairStatus) {
        _outgoing.update { list ->
            list.map { if (it.requestId == requestId) it.copy(status = status, error = null) else it }
        }
    }

    private fun updateOutgoingRejected(requestId: String, reason: String) {
        _outgoing.update { list ->
            list.map { if (it.requestId == requestId) it.copy(status = OutgoingPairStatus.REJECTED, error = reason) else it }
        }
    }

    private fun updateOutgoingFailed(requestId: String, error: String) {
        _outgoing.update { list ->
            list.map { if (it.requestId == requestId) it.copy(status = OutgoingPairStatus.FAILED, error = error) else it }
        }
    }

    private fun randomId(): String {
        val bytes = Random.nextBytes(16)
        return bytes.joinToString("") { b -> ((b.toInt() and 0xFF).toString(16)).padStart(2, '0') }
    }
}
