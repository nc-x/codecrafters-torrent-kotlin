import MetadataMessage.Extended
import MetadataMessage.Request
import bencode.decode
import bencode.encode
import bencode.getSHA1
import bencode.toBytes
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import magnet.Magnet
import java.io.File
import kotlin.math.ceil

data class Torrent(
    @SerializedName("announce")
    val trackerUrl: String,
    val info: Info,
) {
    private lateinit var metadata: Map<*, *>

    val infoHash: ByteArray
        get() {
            val info = encode(metadata).toBytes()
            return info.getSHA1()
        }

    @OptIn(ExperimentalStdlibApi::class)
    val infoHashHex: String
        get() = infoHash.toHexString()


    fun numPieces(): Int =
        ceil(info.length.toDouble() / info._pieceLength).toInt()

    fun pieceLength(pieceIdx: Int): Long =
        if (pieceIdx == numPieces() - 1) info.length % info._pieceLength else info._pieceLength

    fun numBlocks(pieceIdx: Int): Int =
        ceil(pieceLength(pieceIdx) / BLOCK_SIZE.toDouble()).toInt()

    companion object {
        private val gson = Gson()
        const val BLOCK_SIZE = 16 * 1024L

        fun from(file: File): Torrent {
            val decoded = decode(file.readBytes()) as Map<*, *>
            val json = gson.toJson(decoded)
            val torrent = gson.fromJson(json, Torrent::class.java) ?: error("invalid torrent")
            torrent.metadata = (decoded["info"] as Map<*, *>?)!!
            return torrent
        }

        suspend fun from(magnet: Magnet): Torrent {
            val trackerRequest = TrackerRequest(
                magnet.trackerUrl ?: error("the given magnetLink is missing the trackerURL"),
                magnet.infoHash,
                1
            )
            val response = Tracker.query(trackerRequest)
            val peerIp = response.peers.toList().random()
            val (ip, port) = peerIp.split(':')
            return connect(ip, port.toInt()) {
                handshake("00000000000000000000", magnet.infoHash, useExtensions = true)
                val extensionMetadata = sendMetadataMessage(Extended.id, 0)

                @Suppress("UNCHECKED_CAST")
                val m = extensionMetadata["m"]!! as Map<String, Any>
                val extendedMessageId = m["ut_metadata"].toString().toUByte().toByte()
                val infoMap = sendMetadataMessage(Request.id, extendedMessageId)
                val info = gson.fromJson(gson.toJson(infoMap), Info::class.java)
                val torrent = Torrent(magnet.trackerUrl, info)
                torrent.metadata = infoMap
                torrent
            }
        }
    }
}

data class Info(
    val name: String,
    @SerializedName("piece length")
    val _pieceLength: Long,
    val pieces: String,
    val length: Long,
) {
    @OptIn(ExperimentalStdlibApi::class)
    val pieceHashes: Sequence<String>
        get() {
            val nPieces = pieces.length / 20
            return sequence {
                for (i in 0..<nPieces) {
                    val piece = pieces.substring(i * 20..<(i + 1) * 20)
                    yield(piece.toBytes().toHexString())
                }
            }
        }
}
