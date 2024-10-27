import bencode.decode
import bencode.toBytes
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalStdlibApi::class)
fun main(args: Array<String>) {
    val gson = Gson()

    when (val command = args[0]) {
        "decode" -> {
            val bencoded = args[1]
            val decoded = decode(bencoded.toBytes())
            println(gson.toJson(decoded))
        }

        "info" -> {
            val torrent = Torrent.from(args[1])
            println("Tracker URL: ${torrent.announce}")
            println("Length: ${torrent.info.length}")
            println("Info Hash: ${torrent.infoHashHex}")
            println("Piece Length: ${torrent.info.pieceLength}")
            torrent.info.pieceHashes.forEach {
                println(it)
            }
        }

        "peers" -> {
            val torrent = Torrent.from(args[1])
            val response = runBlocking { torrent.query() }
            response.peers.forEach {
                println(it)
            }
        }

        "handshake" -> {
            val torrent = Torrent.from(args[1])
            val peer = args[2]
            val (ip, port) = peer.split(':')
            val peerId = runBlocking { torrent.handshake("00000000000000000000", ip, port.toInt()) }
            println("Peer ID: $peerId")
        }

        else -> println("Unknown command $command")
    }
}