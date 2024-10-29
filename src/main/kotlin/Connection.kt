import PeerMessage.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers

data class Connection(val reader: ByteReadChannel, val writer: ByteWriteChannel) {
    suspend fun readPayloadLen(reader: ByteReadChannel): Int {
        var payloadLen: Int
        do {
            // ignore keepalives
            payloadLen = reader.readInt()
        } while (payloadLen <= 0)
        return payloadLen
    }

    suspend fun pingPong(reader: ByteReadChannel, writer: ByteWriteChannel) {
        ignoreBitfield(reader)
        registerInterest(writer)
    }

    suspend fun ignoreBitfield(reader: ByteReadChannel) {
        val payloadLen = reader.readInt()
        assert(reader.readByte() == Bitfield.id)
        // ignore the payload as it is not important
        reader.readByteArray(payloadLen - 1)
    }

    private suspend fun registerInterest(writer: ByteWriteChannel) {
        writer.writeInt(1) // len 1
        writer.writeByte(Interested.id)
        writer.flush()
    }

    suspend fun requestBlocks(
        pieceLength: Long,
        writer: ByteWriteChannel,
        pieceIdx: Int
    ) {
        var offset = 0L
        while (offset < pieceLength) {
            val size = minOf(Torrent.BLOCK_SIZE, (pieceLength - offset))
            writer.writeInt(13) // Length of the below items
            writer.writeByte(Request.id)
            writer.writeInt(pieceIdx)
            writer.writeInt(offset.toInt())
            writer.writeInt(size.toInt())
            writer.flush()
            offset += size
        }
    }
}

suspend fun <T> connect(ip: String, port: Int, f: suspend Connection.() -> T): T {
    val selectorManager = SelectorManager(Dispatchers.IO)
    aSocket(selectorManager).tcp().connect(ip, port).use { socket ->
        val reader = socket.openReadChannel()
        val writer = socket.openWriteChannel(autoFlush = false)
        return f(Connection(reader, writer))
    }
}
