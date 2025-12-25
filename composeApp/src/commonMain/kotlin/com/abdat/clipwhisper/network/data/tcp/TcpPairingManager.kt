package com.abdat.clipwhisper.network.data.tcp


import com.abdat.clipwhisper.core.domain.models.RemoteClipboardEvent
import com.abdat.clipwhisper.network.data.DeviceDiscoveryManager
import com.abdat.clipwhisper.network.data.DeviceInfoProvider
import com.abdat.clipwhisper.network.domain.PairedDeviceStore
import com.abdat.clipwhisper.network.domain.model.Device
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val discoveryManager: DeviceDiscoveryManager,
) {
    private fun log(msg: String) = println("ClipWhisper/TCP: $msg")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _incomingRequests = MutableStateFlow<List<IncomingPairRequest>>(emptyList())
    val incomingRequests: StateFlow<List<IncomingPairRequest>> = _incomingRequests.asStateFlow()

    private val _outgoingRequests = MutableStateFlow<List<OutgoingPairRequest>>(emptyList())
    val outgoingRequests: StateFlow<List<OutgoingPairRequest>> = _outgoingRequests.asStateFlow()

    private val _incomingClipboard = MutableSharedFlow<RemoteClipboardEvent>(extraBufferCapacity = 64)
    val incomingClipboard: SharedFlow<RemoteClipboardEvent> = _incomingClipboard.asSharedFlow()

    private var listener: TcpListener? = null
    private var serverJob: Job? = null

    private var seqCounter = 0L

    // discovery cache for reconnect
    private val known = mutableMapOf<String, Device>()

    // connections to peers
    private data class ConnEntry(
        val peerId: String,
        val peerName: String,
        val conn: TcpConnection,
        val writeMutex: Mutex,
        val readJob: Job,
        val keepAliveJob: Job
    )
    private val conns = mutableMapOf<String, ConnEntry>()

    // pairing sessions
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

    private val pendingUnpairs = mutableMapOf<String, CompletableDeferred<Unit>>()


    init {
        // keep endpoints updated + auto-connect to approved devices when seen
        scope.launch {
            discoveryManager.discoveredDevices.collect { list ->
                mutex.withLock { list.forEach { known[it.deviceId] = it } }
                list.filter { it.isApproved }.forEach { d ->
                    ensureConnectedIfPossible(d.deviceId)
                }
            }
        }
    }

    fun startServer() {
        val self = deviceInfoProvider.getDeviceInfo()
        if (serverJob?.isActive == true) return

        listener = tcpListen(self.port)
        serverJob = scope.launch {
            val l = listener ?: return@launch
            while (isActive) {
                val conn = l.accept()
                launch { handleIncomingConnection(conn) }
            }
        }
        log("TCP server started on port=${self.port}")
    }

    fun stopServer() {
        serverJob?.cancel()
        serverJob = null
        listener?.close()
        listener = null

        scope.launch {
            mutex.withLock {
                conns.values.forEach { it.conn.close(); it.readJob.cancel(); it.keepAliveJob.cancel() }
                conns.clear()
                incomingSessions.values.forEach { it.conn.close() }
                outgoingSessions.values.forEach { it.conn?.close() }
                incomingSessions.clear()
                outgoingSessions.clear()
            }
            _incomingRequests.value = emptyList()
            _outgoingRequests.value = emptyList()
        }
        log("TCP server stopped")
    }


    fun requestPairing(targetDeviceId: String, targetName: String, targetHost: String, targetPort: Int): String {
        val requestId = randomId()
        val address = "$targetHost:$targetPort"

        _outgoingRequests.update {
            it.filterNot { r -> r.requestId == requestId } + OutgoingPairRequest(
                requestId = requestId,
                toDeviceId = targetDeviceId,
                toDeviceName = targetName,
                toAddress = address,
                status = OutgoingPairStatus.CONNECTING
            )
        }

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
                val conn = tcpConnect(targetHost, targetPort)
                session.conn = conn

                // HELLO
                send(conn, PairingPacket(
                    type = PairingPacket.Type.HELLO,
                    deviceId = self.deviceId,
                    deviceName = self.deviceName,
                    port = self.port
                )
                )
                val peerHello = receive(conn, 5_000)
                require(peerHello.type == PairingPacket.Type.HELLO)

                // PAIR_REQUEST
                send(conn, PairingPacket(
                    type = PairingPacket.Type.PAIR_REQUEST,
                    requestId = requestId,
                    deviceId = self.deviceId,
                    deviceName = self.deviceName,
                    port = self.port
                ))
                setOutgoingStatus(requestId, OutgoingPairStatus.WAITING_REMOTE_APPROVAL)

                val decision = receive(conn, 30_000)
                when (decision.type) {
                    PairingPacket.Type.PAIR_ACCEPT -> {
                        setOutgoingStatus(requestId, OutgoingPairStatus.WAITING_LOCAL_CONFIRM)

                        val ok = withTimeoutOrNull(60_000) { session.confirm.await() } ?: false
                        if (!ok) {
                            send(conn, PairingPacket(type = PairingPacket.Type.PAIR_CANCEL, requestId = requestId))
                            setOutgoingFailed(requestId, "Local canceled or timed out")
                            conn.close()
                            return@launch
                        }

                        send(conn, PairingPacket(type = PairingPacket.Type.PAIR_CONFIRM, requestId = requestId))

                        pairedStore.approve(targetDeviceId)
                        send(conn, PairingPacket(type = PairingPacket.Type.PAIR_DONE, requestId = requestId))
                        setOutgoingStatus(requestId, OutgoingPairStatus.PAIRED)

                        // ✅ keep connection alive for clipboard
                        registerConnection(targetDeviceId, targetName, conn)
                    }

                    PairingPacket.Type.PAIR_REJECT -> {
                        setOutgoingRejected(requestId, decision.reason ?: "rejected")
                        conn.close()
                    }

                    else -> {
                        setOutgoingFailed(requestId, "Unexpected: ${decision.type}")
                        conn.close()
                    }
                }

            } catch (e: Exception) {
                setOutgoingFailed(requestId, e.message ?: "error")
                session.conn?.close()
            } finally {
                mutex.withLock { outgoingSessions.remove(requestId) }
            }
        }

        return requestId
    }

    fun requestUnpair(peerId: String) {
        scope.launch {
            pairedStore.revoke(peerId)
            val self = deviceInfoProvider.getDeviceInfo()
            val requestId = randomId()

            val pkt = PairingPacket(
                type = PairingPacket.Type.UNPAIR_REQUEST,
                requestId = requestId,
                deviceId = self.deviceId,
                deviceName = self.deviceName,
                ts = System.currentTimeMillis()
            )

            val entry = mutex.withLock { conns[peerId] }

            val acked: Boolean = if (entry != null) {
                val waiter = CompletableDeferred<Unit>()
                mutex.withLock { pendingUnpairs[requestId] = waiter }

                val sent = sendSafe(entry, pkt)
                val ok = sent && (withTimeoutOrNull(3_000) { waiter.await(); true } ?: false)

                mutex.withLock { pendingUnpairs.remove(requestId) }
                ok
            } else {
                // best effort: connect via discovery cache even if we're about to revoke locally
                sendUnpairOverTempConnection(peerId, pkt)
            }

            cleanup(peerId)

            log("requestUnpair(peerId=$peerId) acked=$acked approvedNow=${pairedStore.isApproved(peerId)}")
        }
    }

    private suspend fun sendUnpairOverTempConnection(peerId: String, pkt: PairingPacket): Boolean {
        val device = mutex.withLock { known[peerId] } ?: return false

        return try {
            val self = deviceInfoProvider.getDeviceInfo()
            val conn = tcpConnect(device.ipAddress, device.port)

            // HELLO exchange (must match handleIncomingConnection)
            send(conn, PairingPacket(
                type = PairingPacket.Type.HELLO,
                deviceId = self.deviceId,
                deviceName = self.deviceName,
                port = self.port
            ))
            val peerHello = receive(conn, 5_000)
            if (peerHello.type != PairingPacket.Type.HELLO) {
                conn.close()
                return false
            }

            // send UNPAIR
            send(conn, pkt)

            // wait for ACK (best effort)
            val ack = runCatching { receive(conn, 5_000) }.getOrNull()
            conn.close()

            ack?.type == PairingPacket.Type.UNPAIR_ACK && ack.requestId == pkt.requestId
        } catch (_: Exception) {
            false
        }
    }

    fun confirmOutgoing(requestId: String) {
        scope.launch { mutex.withLock { outgoingSessions[requestId]?.confirm?.complete(true) } }
    }

    fun cancelOutgoing(requestId: String) {
        scope.launch { mutex.withLock { outgoingSessions[requestId]?.confirm?.complete(false) } }
    }

    fun approveIncoming(requestId: String) {
        scope.launch { mutex.withLock { incomingSessions[requestId]?.decision?.complete(true) } }
    }

    fun rejectIncoming(requestId: String) {
        scope.launch { mutex.withLock { incomingSessions[requestId]?.decision?.complete(false) } }
    }


    fun sendClipboard(text: String) {
        val trimmed = text.take(MAX_CLIPBOARD_CHARS)
        if (trimmed.isBlank()) return

        scope.launch {
            val approved = pairedStore.getApprovedIds()
            if (approved.isEmpty()) {
                log("sendClipboard(): no approved peers")
                return@launch
            }

            // best-effort reconnect
            approved.forEach { ensureConnectedIfPossible(it) }

            val self = deviceInfoProvider.getDeviceInfo()
            val seq = mutex.withLock { ++seqCounter }
            val pkt = PairingPacket(
                type = PairingPacket.Type.CLIP_PUSH,
                deviceId = self.deviceId,
                deviceName = self.deviceName,
                text = trimmed,
                seq = seq,
                ts = System.currentTimeMillis()
            )

            val entries = mutex.withLock { conns.values.toList() }
            var sent = 0
            entries.forEach { entry ->
                if (entry.peerId in approved) {
                    if (sendSafe(entry, pkt)) sent++
                }
            }
            log("sendClipboard(): seq=$seq sent=$sent len=${trimmed.length}")
        }
    }


    private suspend fun handleIncomingConnection(conn: TcpConnection) {
        val self = deviceInfoProvider.getDeviceInfo()

        try {
            // HELLO exchange
            send(conn, PairingPacket(
                type = PairingPacket.Type.HELLO,
                deviceId = self.deviceId,
                deviceName = self.deviceName,
                port = self.port
            ))

            val peerHello = receive(conn, 5_000)
            require(peerHello.type == PairingPacket.Type.HELLO)

            val peerId = peerHello.deviceId ?: "unknown"
            val peerName = (peerHello.deviceName ?: "Unknown").take(64)

            // already paired? register and start read loop
            if (pairedStore.isApproved(peerId)) {
                registerConnection(peerId, peerName, conn)
                return
            }

            // otherwise require PAIR_REQUEST
            val req = receive(conn, 30_000)
            if (req.type != PairingPacket.Type.PAIR_REQUEST || req.requestId == null) {
                conn.close()
                return
            }

            val requestId = req.requestId
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

            _incomingRequests.update {
                it + IncomingPairRequest(
                    requestId = requestId,
                    fromDeviceId = fromId,
                    fromDeviceName = fromName,
                    fromAddress = conn.remoteAddress
                )
            }

            val approved = withTimeoutOrNull(60_000) { session.decision.await() } ?: false
            _incomingRequests.update { it.filterNot { r -> r.requestId == requestId } }

            if (!approved) {
                send(conn, PairingPacket(type = PairingPacket.Type.PAIR_REJECT, requestId = requestId, reason = "User rejected"))
                conn.close()
                return
            }

            send(conn, PairingPacket(type = PairingPacket.Type.PAIR_ACCEPT, requestId = requestId))

            val confirm = receive(conn, 30_000)
            if (confirm.type == PairingPacket.Type.PAIR_CONFIRM) {
                pairedStore.approve(fromId)
                send(conn, PairingPacket(type = PairingPacket.Type.PAIR_DONE, requestId = requestId))

                //  keep connection alive
                registerConnection(fromId, fromName, conn)
            } else {
                conn.close()
            }

        } catch (_: Exception) {
            conn.close()
        }
    }


    private suspend fun registerConnection(peerId: String, peerName: String, conn: TcpConnection) {
        // remove old outside the lock usage pattern
        val old = mutex.withLock { conns.remove(peerId) }
        if (old != null) {
            runCatching { old.conn.close() }
            old.readJob.cancel()
            old.keepAliveJob.cancel()
            log("Replaced existing connection: peerId=$peerId")
        }

        val writeMutex = Mutex()
        val readJob = scope.launch { readLoop(peerId, peerName, conn) }
        val keepAliveJob = scope.launch { keepAliveLoop(peerId, conn, writeMutex) }

        mutex.withLock {
            conns[peerId] = ConnEntry(peerId, peerName, conn, writeMutex, readJob, keepAliveJob)
        }
        log("Connected: peerId=$peerId name=$peerName addr=${conn.remoteAddress}")
    }


    private suspend fun readLoop(peerId: String, peerName: String, conn: TcpConnection) {
        try {
            while (currentCoroutineContext().isActive) {
                val bytes = conn.readFrame() ?: break
                val pkt = decode(bytes) ?: continue

                when (pkt.type) {
                    PairingPacket.Type.CLIP_PUSH -> {
                        if (!pairedStore.isApproved(peerId)) continue
                        val txt = pkt.text ?: continue
                        val seq = pkt.seq ?: 0L
                        val ts = pkt.ts ?: System.currentTimeMillis()

                        _incomingClipboard.tryEmit(
                            RemoteClipboardEvent(peerId, peerName, txt, seq, ts)
                        )
                    }

                    PairingPacket.Type.PING -> {
                        val entry = mutex.withLock { conns[peerId] }
                        if (entry != null) sendSafe(
                            entry,
                            PairingPacket(type = PairingPacket.Type.PONG)
                        )
                    }

                    PairingPacket.Type.UNPAIR_REQUEST -> {
                        val reqId = pkt.requestId
                        log("UNPAIR_REQUEST from peerId=$peerId reqId=$reqId")

                        if (reqId != null) {
                            val entry = mutex.withLock { conns[peerId] }
                            if (entry != null) {
                                sendSafe(
                                    entry,
                                    PairingPacket(
                                        type = PairingPacket.Type.UNPAIR_ACK,
                                        requestId = reqId
                                    )
                                )
                            }
                        }

                        // revoke + close
                        pairedStore.revoke(peerId)
                        return
                    }

                    PairingPacket.Type.UNPAIR_ACK -> {
                        val reqId = pkt.requestId ?: return
                        mutex.withLock {
                            pendingUnpairs.remove(reqId)?.complete(Unit)
                        }
                    }
                    else -> Unit
                }
            }
        } catch (_: Exception) {
        } finally {
            cleanup(peerId)
        }
    }

    private suspend fun keepAliveLoop(peerId: String, conn: TcpConnection, writeMutex: Mutex) {
        try {
            while (currentCoroutineContext().isActive) {
                delay(15_000)
                val bytes = PairingJson.encodeToString(
                    PairingPacket.serializer(),
                    PairingPacket(type = PairingPacket.Type.PING)
                ).encodeToByteArray()
                writeMutex.withLock { conn.writeFrame(bytes) }
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun cleanup(peerId: String) {
        val entry = mutex.withLock { conns.remove(peerId) } ?: return
        runCatching { entry.conn.close() }
        entry.readJob.cancel()
        entry.keepAliveJob.cancel()
        log("Disconnected: peerId=$peerId")
    }

    private suspend fun ensureConnectedIfPossible(peerId: String) {
        val already = mutex.withLock { conns.containsKey(peerId) }
        if (already) return
        if (!pairedStore.isApproved(peerId)) return

        val device = mutex.withLock { known[peerId] } ?: return

        scope.launch {
            try {
                val self = deviceInfoProvider.getDeviceInfo()
                val conn = tcpConnect(device.ipAddress, device.port)

                // HELLO exchange
                send(conn, PairingPacket(
                    type = PairingPacket.Type.HELLO,
                    deviceId = self.deviceId,
                    deviceName = self.deviceName,
                    port = self.port
                ))
                val peerHello = receive(conn, 5_000)
                require(peerHello.type == PairingPacket.Type.HELLO)

                val name = (peerHello.deviceName ?: device.name).take(64)
                registerConnection(peerId, name, conn)
            } catch (_: Exception) {
                // best effort; retry later
            }
        }
    }

    private suspend fun sendSafe(entry: ConnEntry, pkt: PairingPacket): Boolean {
        return try {
            val bytes = PairingJson.encodeToString(PairingPacket.serializer(), pkt).encodeToByteArray()
            entry.writeMutex.withLock { entry.conn.writeFrame(bytes) }
            true
        } catch (_: Exception) {
            cleanup(entry.peerId)
            false
        }
    }

    private suspend fun send(conn: TcpConnection, pkt: PairingPacket) {
        val bytes = PairingJson.encodeToString(PairingPacket.serializer(), pkt).encodeToByteArray()
        conn.writeFrame(bytes)
    }

    private suspend fun receive(conn: TcpConnection, timeoutMs: Long): PairingPacket {
        val bytes = withTimeout(timeoutMs) { conn.readFrame() ?: throw CancellationException("closed") }
        return decode(bytes) ?: throw IllegalStateException("bad json")
    }

    private fun decode(bytes: ByteArray): PairingPacket? {
        return runCatching {
            PairingJson.decodeFromString(PairingPacket.serializer(), bytes.decodeToString())
        }.getOrNull()
    }

    private fun setOutgoingStatus(requestId: String, status: OutgoingPairStatus) {
        _outgoingRequests.update { list ->
            list.map { if (it.requestId == requestId) it.copy(status = status, error = null) else it }
        }
    }

    private fun setOutgoingRejected(requestId: String, reason: String) {
        _outgoingRequests.update { list ->
            list.map { if (it.requestId == requestId) it.copy(status = OutgoingPairStatus.REJECTED, error = reason) else it }
        }
    }

    private fun setOutgoingFailed(requestId: String, error: String) {
        _outgoingRequests.update { list ->
            list.map { if (it.requestId == requestId) it.copy(status = OutgoingPairStatus.FAILED, error = error) else it }
        }
    }

    private fun randomId(): String {
        val bytes = Random.nextBytes(16)
        return bytes.joinToString("") { b -> ((b.toInt() and 0xFF).toString(16)).padStart(2, '0') }
    }

    companion object {
        private const val MAX_CLIPBOARD_CHARS = 64_000
    }
}

