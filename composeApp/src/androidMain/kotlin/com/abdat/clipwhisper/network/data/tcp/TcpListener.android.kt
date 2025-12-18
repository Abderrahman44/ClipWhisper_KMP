package com.abdat.clipwhisper.network.data.tcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

actual class TcpListener actual constructor(port: Int) {
    private val server = ServerSocket(port).apply { reuseAddress = true }

    actual suspend fun accept(): TcpConnection = withContext(Dispatchers.IO) {
        TcpConnection(server.accept().apply { tcpNoDelay = true })
    }
    actual fun close() {
        runCatching { server.close() }
    }
}

actual fun tcpConnect(host: String, port: Int, connectTimeoutMs: Int): TcpConnection {
    val socket = Socket()
    socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
    socket.tcpNoDelay = true
    return TcpConnection(socket)
}

actual fun tcpListen(port: Int): TcpListener = TcpListener(port)
