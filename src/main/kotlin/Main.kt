import bencode.decode
import bencode.toBytes
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import magnet.Magnet
import java.io.File

suspend fun main(args: Array<String>) {
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
            println("Piece Length: ${torrent.info._pieceLength}")
            torrent.info.pieceHashes.forEach {
                println(it)
            }
        }

        "peers" -> {
            val torrent = Torrent.from(args[1])
            val response = runBlocking { torrent.queryTracker() }
            response.peers.forEach {
                println(it)
            }
        }

        "handshake" -> {
            val torrent = Torrent.from(args[1])
            val peer = args[2]
            val (ip, port) = peer.split(':')
            connect(ip, port.toInt()) {
                val peerId = handshake("00000000000000000000", torrent.infoHash)
                println("Peer ID: $peerId")
            }
        }

        "download_piece" -> {
            assert(args[1] == "-o")
            val outputLocation = args[2]
            val torrent = Torrent.from(args[3])
            val pieceIdx = args[4]

            val peers = torrent.queryTracker().peers

            val peer = peers.toList().random()
            val (ip, port) = peer.split(':')

            connect(ip, port.toInt()) {
                handshake("00000000000000000000", torrent.infoHash)
                pingPong(reader, writer)
                val piece = torrent.downloadPiece(pieceIdx.toInt())
                File(outputLocation).writeBytes(piece)
            }
        }

        "download" -> {
            assert(args[1] == "-o")
            val outputLocation = args[2]
            val torrent = Torrent.from(args[3])
            torrent.download(outputLocation)
        }

        "magnet_parse" -> {
            val magnetLink = args[1]
            val magnet = Magnet.parse(magnetLink)
            println("Tracker URL: ${magnet.trackerUrl}")
            println("Info Hash: ${magnet.exactTopic}")
        }

        "magnet_handshake" -> {
        }

        else -> println("Unknown command $command")
    }
}

