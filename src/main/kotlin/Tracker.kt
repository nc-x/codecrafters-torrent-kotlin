import Tracker.toInt
import bencode.decode
import bencode.toBytes
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import java.net.URLEncoder

data class TrackerRequest(
    val trackerUrl: String,
    val infoHash: ByteArray,
    val downloadSize: Long,
    val peerId: String = "00000000000000000000",
    val port: Int = 6881,
    val uploaded: Int = 0,
    val downloaded: Int = 0,
    val compact: Int = 1
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrackerRequest

        if (downloadSize != other.downloadSize) return false
        if (port != other.port) return false
        if (uploaded != other.uploaded) return false
        if (downloaded != other.downloaded) return false
        if (compact != other.compact) return false
        if (trackerUrl != other.trackerUrl) return false
        if (!infoHash.contentEquals(other.infoHash)) return false
        if (peerId != other.peerId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = downloadSize.hashCode()
        result = 31 * result + port
        result = 31 * result + uploaded
        result = 31 * result + downloaded
        result = 31 * result + compact
        result = 31 * result + trackerUrl.hashCode()
        result = 31 * result + infoHash.contentHashCode()
        result = 31 * result + peerId.hashCode()
        return result
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
                    val port = it.subList(4, 6).toByteArray().toInt()
                    "$ip:$port"
                }
}

object Tracker {
    private val gson = Gson()
    private val http = HttpClient(CIO)

    suspend fun query(
        request: TrackerRequest
    ): TrackerResponse {
        val response = http.get(request.trackerUrl) {
            url {
                encodedParameters.append(
                    "info_hash", URLEncoder.encode(String(request.infoHash, Charsets.ISO_8859_1), "ISO-8859-1")
                )
                parameters.append("peer_id", request.peerId)
                parameters.append("port", "${request.port}")
                parameters.append("uploaded", "${request.uploaded}")
                parameters.append("downloaded", "${request.downloaded}")
                parameters.append("left", "${request.downloadSize}")
                parameters.append("compact", "${request.compact}")
            }
        }

        val body: ByteArray = response.body()
        val json = gson.toJson(decode(body))
        if ("failure reason" in json) throw Exception(json)
        return gson.fromJson(json, TrackerResponse::class.java)
    }

    // Different from the one in Utils.kt as that is based on the number being represented in ascii
    // Whereas this one interprets the bits as a number itself
    fun ByteArray.toInt(): Int =
        fold(0) { acc, elem -> (acc shl 8) + elem.toUByte().toInt() }

}
