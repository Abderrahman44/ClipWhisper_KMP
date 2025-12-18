package com.abdat.clipwhisper.network.data.tcp

expect class TcpConnection {
    val remoteAddress: String
    suspend fun writeFrame(bytes: ByteArray)
    suspend fun readFrame(): ByteArray? // null => closed
    fun close()
}