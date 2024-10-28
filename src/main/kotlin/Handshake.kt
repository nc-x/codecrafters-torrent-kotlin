import bencode.toBytes
import io.ktor.utils.io.*
import java.util.*

private const val protocol = "BitTorrent protocol"
private const val numReservedBytes = 8
private val reserved = BitSet(numReservedBytes * 8)

context(Connection)
@OptIn(ExperimentalStdlibApi::class)
suspend fun handshake(
    peerId: String,
    infoHash: ByteArray,
    useExtensions: Boolean = false
): String {
    if (useExtensions) reserved.set(20)

    with(writer) {
        writeByte(protocol.length.toByte())
        writeByteArray(protocol.toBytes())
        if (useExtensions) {
            writeByteArray(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00))
        } else {
            writeByteArray(ByteArray(8))
        }
        writeByteArray(infoHash)
        writeByteArray(peerId.toBytes())
        flush()
    }

    with(reader) {
        assert(readByte() == protocol.length.toByte())
        assert(readByteArray(protocol.length).contentEquals(protocol.toBytes()))
        assert(readByteArray(numReservedBytes).contentEquals(reserved.toByteArray()))
        assert(readByteArray(infoHash.size).contentEquals(infoHash))
        val responsePeerId = readByteArray(peerId.toBytes().size)
        return responsePeerId.toHexString()
    }
}