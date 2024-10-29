import bencode.toBytes
import io.ktor.utils.io.*

private const val protocol = "BitTorrent protocol"
private const val reservedLen = 8
private val reservedWithoutExtensions = ByteArray(reservedLen)
private val reservedWithExtensions = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00)


context(Connection)
@OptIn(ExperimentalStdlibApi::class)
suspend fun handshake(
    peerId: String,
    infoHash: ByteArray,
    useExtensions: Boolean = false
): Peer {
    with(writer) {
        writeByte(protocol.length.toByte())
        writeByteArray(protocol.toBytes())
        if (useExtensions) {
            writeByteArray(reservedWithExtensions)
        } else {
            writeByteArray(reservedWithoutExtensions)
        }
        writeByteArray(infoHash)
        writeByteArray(peerId.toBytes())
        flush()
    }

    with(reader) {
        assert(readByte() == protocol.length.toByte())
        assert(readByteArray(protocol.length).contentEquals(protocol.toBytes()))
        val reserved = readByteArray(reservedLen)
        assert(readByteArray(infoHash.size).contentEquals(infoHash))
        val responsePeerId = readByteArray(peerId.toBytes().size)
        if (reserved[5] == 0x10.toByte())
            return Peer(responsePeerId.toHexString(), true)
        return Peer(responsePeerId.toHexString(), false)
    }
}
