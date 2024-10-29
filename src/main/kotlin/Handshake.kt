import PeerMessage.Extended
import bencode.decode
import bencode.encode
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

context(Connection)
@Suppress("UNCHECKED_CAST")
suspend fun extensionHandshake(): Map<String, Any> {
    ignoreBitfield(reader)

    with(writer) {
        val payload = encode(
            mapOf(
                "m" to mapOf(
                    "ut_metadata" to 16L
                )
            )
        ).toBytes()
        writeInt(2 + payload.size) // length of below two
        writeByte(Extended.id)
        writeByte(0) // extension message id
        writeByteArray(payload)
        flush()
    }

    with(reader) {
        val payloadLen = readPayloadLen(reader)
        assert(readByte() == Extended.id)
        assert(readByte() == 0.toByte()) // extension message id
        val payload = readByteArray(payloadLen - 2)
        return decode(payload) as Map<String, Any>
    }
}
