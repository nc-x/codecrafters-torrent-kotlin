import PeerMessage.Piece
import Torrent.Companion.BLOCK_SIZE
import bencode.getSHA1
import io.ktor.utils.io.*


context(Connection)
@OptIn(ExperimentalStdlibApi::class)
suspend fun Torrent.downloadPiece(pieceIdx: Int): ByteArray {
    val blocks = MutableList(numBlocks(pieceIdx)) { ByteArray(0) }
    requestBlocks(pieceLength(pieceIdx), writer, pieceIdx)
    while (true) {
        val payloadLen = readPayloadLen(reader)
        when (val b = reader.readByte()) {
            Piece.id -> {
                val index = reader.readInt()
                val offset = reader.readInt()
                val block = reader.readByteArray(payloadLen - 9) // 4 + 4 bytes used above
                blocks[(offset / BLOCK_SIZE).toInt()] = block

                if (blocks.sumOf { it.size.toLong() } >= pieceLength(pieceIdx)) {
                    val piece = blocks.reduce { acc, bytes -> acc + bytes }
                    assert(piece.getSHA1().toHexString() == info.pieceHashes.elementAt(pieceIdx))
                    return piece
                }
            }

            else -> {
                // ignore payloads if anything else and do nothing
                reader.readByteArray(payloadLen - 1)
            }
        }
    }
}
