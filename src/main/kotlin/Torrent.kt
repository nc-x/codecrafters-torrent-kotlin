import bencode.decode
import bencode.encode
import bencode.getSHA1
import bencode.toBytes
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import java.io.File
import java.net.URLEncoder
import kotlin.math.ceil

data class Torrent(
    val announce: String,
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

    suspend fun query(
        peerId: String = "00000000000000000000",
        port: Int = 6881,
        uploaded: Int = 0,
        downloaded: Int = 0,
        compact: Int = 1
    ): TrackerResponse {
        val response = http.get(announce) {
            url {
                encodedParameters.append(
                    "info_hash", URLEncoder.encode(String(infoHash, Charsets.ISO_8859_1), "ISO-8859-1")
                )
                parameters.append("peer_id", peerId)
                parameters.append("port", "$port")
                parameters.append("uploaded", "$uploaded")
                parameters.append("downloaded", "$downloaded")
                parameters.append("left", "${info.length}")
                parameters.append("compact", "$compact")
            }
        }

        val body: ByteArray = response.body()
        val json = gson.toJson(decode(body))
        return gson.fromJson(json, TrackerResponse::class.java)
    }

    fun numPieces(): Int =
        ceil(info.length.toDouble() / info._pieceLength).toInt()

    fun pieceLength(pieceIdx: Int): Long =
        if (pieceIdx == numPieces() - 1) info.length % info._pieceLength else info._pieceLength

    fun numBlocks(pieceIdx: Int): Int =
        ceil(pieceLength(pieceIdx) / blockSize.toDouble()).toInt()

    companion object {
        private val gson = Gson()
        private val http = HttpClient(CIO)

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

data class TrackerResponse(
    @SerializedName("min interval")
    val minInterval: Int,
    val complete: Int,
    val incomplete: Int,
    val interval: Int,
    @SerializedName("peers")
    val _peers: String,
) {
    val peers: Sequence<String>
        get() =
            _peers.toBytes().asSequence()
                .chunked(6)
                .map {
                    val ip = it.subList(0, 4).joinToString(".") { it.toUByte().toInt().toString() }
                    val port = toInt(it.subList(4, 6).toByteArray())
                    "$ip:$port"
                }
}

// Different from the one in Utils.kt as that is based on the number being represented in ascii
// Whereas this one interprets the bits as a number itself
fun toInt(bytes: ByteArray): Int =
    bytes.fold(0) { acc, elem -> (acc shl 8) + elem.toUByte().toInt() }
