import MetadataMessage.Extended
import MetadataMessage.Request
import bencode.decode
import bencode.decodeStream
import bencode.encode
import bencode.toBytes
import io.ktor.utils.io.*

enum class MetadataMessage(val id: Byte) {
    Request(0),
    Data(1),
    Reject(2),
    Extended(20),
}

context(Connection)
suspend fun sendMetadataMessage(
    msgType: Byte,
    extendedMessageId: Byte,
    pieceIdx: Long = 0
): Map<String, Any> {
    when (msgType) {
        Extended.id -> {
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
                writeByte(extendedMessageId)
                writeByteArray(payload)
                flush()
            }

            with(reader) {
                val payloadLen = readPayloadLen(reader)
                assert(readByte() == msgType)
                assert(readByte() == 0.toByte()) // extension message id
                val payload = readByteArray(payloadLen - 2)
                @Suppress("UNCHECKED_CAST")
                return decode(payload) as Map<String, Any>
            }
        }

        Request.id -> {
            with(writer) {
                val payload = encode(
                    mapOf(
                        "msg_type" to msgType.toLong(),
                        "piece" to pieceIdx
                    )
                ).toBytes()
                writeInt(1 + 1 + payload.size) // length
                writeByte(Extended.id)
                writeByte(extendedMessageId)
                writeByteArray(payload)
                flush()
            }

            with(reader) {
                val payloadLen = readPayloadLen(reader)
                assert(readByte() == msgType)
                assert(readByte() == 0.toByte()) // extension message id
                val payload = readByteArray(payloadLen - 2)
                val meta = decodeStream(payload)
                @Suppress("UNCHECKED_CAST")
                return decode(payload.sliceArray(meta.second..<payload.size)) as Map<String, Any>
            }
        }

        else -> {
        }
    }
    return mapOf()
}
