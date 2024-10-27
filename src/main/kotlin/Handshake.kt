import bencode.toBytes
import io.ktor.utils.io.*

private const val protocol = "BitTorrent protocol"
private val reserved = ByteArray(8) { 0 }

@OptIn(ExperimentalStdlibApi::class)
suspend fun Torrent.handshake(tx: ByteWriteChannel, rx: ByteReadChannel, peerId: String): String {
    with(tx) {
        writeByte(protocol.length.toByte())
        writeByteArray(protocol.toBytes())
        writeByteArray(reserved)
        writeByteArray(infoHash)
        writeByteArray(peerId.toBytes())
        flush()
    }

    with(rx) {
        assert(readByte() == protocol.length.toByte())
        assert(readByteArray(protocol.length).contentEquals(protocol.toBytes()))
        assert(readByteArray(reserved.size).contentEquals(reserved))
        assert(readByteArray(infoHash.size).contentEquals(infoHash))
        val responsePeerId = readByteArray(peerId.toBytes().size)
        return responsePeerId.toHexString()
    }
}