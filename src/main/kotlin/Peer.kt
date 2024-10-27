import PeerMessage.*
import bencode.getSHA1
import io.ktor.utils.io.*
import kotlin.math.ceil

const val blockSize = 16 * 1024L

enum class PeerMessage(val id: Byte) {
    Choke(0),
    Unchoke(1),
    Interested(2),
    NotInterested(3),
    Have(4),
    Bitfield(5),
    Request(6),
    Piece(7),
    Cancel(8),
}

@OptIn(ExperimentalStdlibApi::class)
suspend fun Torrent.downloadPiece(tx: ByteWriteChannel, rx: ByteReadChannel, pieceIdx: Int): ByteArray {
    val numPieces = ceil(info.length.toDouble() / info.pieceLength).toInt()
    val isLastPiece = pieceIdx == numPieces - 1 // 0 indexed
    val pieceLength = if (isLastPiece) info.length % info.pieceLength else info.pieceLength

    var bitfieldReceived = false
    val blocks = MutableList(ceil(pieceLength / blockSize.toDouble()).toInt()) { ByteArray(0) }
    while (true) {
        val payloadLen = rx.readInt()
        if (payloadLen <= 0) continue
        when (rx.readByte()) {
            Bitfield.id -> {
                // ignore the payload as it is not important
                rx.readByteArray(payloadLen - 1)
                send(tx, Interested, pieceIdx, pieceLength)
                bitfieldReceived = true
            }

            Unchoke.id -> {
                if (!bitfieldReceived) continue
                send(tx, Request, pieceIdx, pieceLength)
            }

            Piece.id -> {
                val index = rx.readInt()
                val offset = rx.readInt()
                val block = rx.readByteArray(payloadLen - 9) // 4 + 4 bytes used above
                blocks[(offset / blockSize).toInt()] = block

                if (blocks.sumOf { it.size.toLong() } >= pieceLength) {
                    val piece = blocks.reduce { acc, bytes -> acc + bytes }
                    assert(piece.getSHA1().toHexString() == info.pieceHashes.elementAt(pieceIdx))
                    return piece
                }
            }

            else -> {
                // ignore payloads if anything else and do nothing
                rx.readByteArray(payloadLen - 1)
            }
        }
    }
}

suspend fun send(tx: ByteWriteChannel, message: PeerMessage, pieceIdx: Int, pieceLength: Long) {
    when (message) {
        Interested -> {
            tx.writeInt(1) // len 1
            tx.writeByte(Interested.id)
            tx.flush()
        }

        Request -> {
            var offset = 0L
            while (offset < pieceLength) {
                val size = minOf(blockSize, (pieceLength - offset))
                tx.writeInt(13) // Length of the below items
                tx.writeByte(Request.id)
                tx.writeInt(pieceIdx)
                tx.writeInt(offset.toInt())
                tx.writeInt(size.toInt())
                tx.flush()
                offset += size
            }
        }

        else -> error("Invalid message type '$message' to send")
    }
}
