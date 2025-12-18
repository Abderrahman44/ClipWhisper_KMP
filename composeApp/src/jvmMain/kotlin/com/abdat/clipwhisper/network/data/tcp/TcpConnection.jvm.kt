package com.abdat.clipwhisper.network.data.tcp


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.Socket

private const val MAX_FRAME_SIZE = 256 * 1024

actual class TcpConnection internal constructor(
    private val socket: Socket
) {
    private val input = BufferedInputStream(socket.getInputStream())
    private val output = BufferedOutputStream(socket.getOutputStream())

    actual val remoteAddress: String =
        socket.inetAddress?.hostAddress ?: socket.remoteSocketAddress.toString()

    actual suspend fun writeFrame(bytes: ByteArray) = withContext(Dispatchers.IO) {
        require(bytes.size in 1..MAX_FRAME_SIZE) { "Bad frame size: ${bytes.size}" }
        writeInt(bytes.size)
        output.write(bytes)
        output.flush()
    }

    actual suspend fun readFrame(): ByteArray? = withContext(Dispatchers.IO) {
        val len = readInt() ?: return@withContext null
        if (len !in 1..MAX_FRAME_SIZE) {
            close()
            throw IllegalStateException("Bad frame length: $len")
        }
        val buf = ByteArray(len)
        readFully(buf)
        buf
    }

    actual fun close() {
        runCatching { socket.close() }
    }

    private fun writeInt(v: Int) {
        output.write((v ushr 24) and 0xFF)
        output.write((v ushr 16) and 0xFF)
        output.write((v ushr 8) and 0xFF)
        output.write(v and 0xFF)
    }

    private fun readInt(): Int? {
        val b1 = input.read()
        if (b1 == -1) return null
        val b2 = input.read(); val b3 = input.read(); val b4 = input.read()
        if (b2 == -1 || b3 == -1 || b4 == -1) return null
        return (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
    }

    private fun readFully(dst: ByteArray) {
        var off = 0
        while (off < dst.size) {
            val r = input.read(dst, off, dst.size - off)
            if (r <= 0) throw IllegalStateException("Stream closed while reading")
            off += r
        }
    }
}

