package com.abdat.clipwhisper.network.data.tcp

expect class TcpListener(port: Int) {
    suspend fun accept(): TcpConnection
    fun close()
}

expect fun tcpConnect(host: String, port: Int, connectTimeoutMs: Int = 3_000): TcpConnection
expect fun tcpListen(port: Int): TcpListener