import bencode.decode
import bencode.encode
import bencode.getSHA1
import bencode.toBytes
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
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
            val info = encode(metadata["info"]!!).toBytes()
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

        fun from(file: String): Torrent {
            return from(File(file))
        }

        fun from(file: File): Torrent {
            val decoded = decode(file.readBytes()) as Map<*, *>
            val json = gson.toJson(decoded)
            val torrent = gson.fromJson(json, Torrent::class.java) ?: error("invalid torrent")
            torrent.metadata = decoded
            return torrent
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
