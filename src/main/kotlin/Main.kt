import MetadataMessage.Extended
import bencode.decode
import bencode.toBytes
import com.google.gson.Gson
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
            val torrent = Torrent.from(File(args[1]))
            println("Tracker URL: ${torrent.trackerUrl}")
            println("Length: ${torrent.info.length}")
            println("Info Hash: ${torrent.infoHashHex}")
            println("Piece Length: ${torrent.info._pieceLength}")
            torrent.info.pieceHashes.forEach {
                println(it)
            }
        }

        "peers" -> {
            val torrent = Torrent.from(File(args[1]))
            val trackerRequest = TrackerRequest(torrent.trackerUrl, torrent.infoHash, torrent.info.length)
            val response = Tracker.query(trackerRequest)
            response.peers.forEach {
                println(it)
            }
        }

        "handshake" -> {
            val torrent = Torrent.from(File(args[1]))
            val peerIp = args[2]
            val (ip, port) = peerIp.split(':')
            connect(ip, port.toInt()) {
                val peer = handshake("00000000000000000000", torrent.infoHash)
                println("Peer ID: ${peer.id}")
            }
        }

        "download_piece" -> {
            assert(args[1] == "-o")
            val outputLocation = args[2]
            val torrent = Torrent.from(File(args[3]))
            val pieceIdx = args[4]

            val trackerRequest = TrackerRequest(torrent.trackerUrl, torrent.infoHash, torrent.info.length)
            val response = Tracker.query(trackerRequest)
            val peerIp = response.peers.toList().random()
            val (ip, port) = peerIp.split(':')

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
            val torrent = Torrent.from(File(args[3]))
            torrent.download(outputLocation)
        }

        "magnet_parse" -> {
            val magnetLink = args[1]
            val magnet = Magnet.parse(magnetLink)
            println("Tracker URL: ${magnet.trackerUrl}")
            println("Info Hash: ${magnet.infoHashHex}")
        }

        "magnet_handshake" -> {
            val magnetLink = args[1]
            val magnet = Magnet.parse(magnetLink)
            val trackerRequest = TrackerRequest(
                magnet.trackerUrl ?: error("the given magnetLink is missing the trackerURL"),
                magnet.infoHash,
                1
            )
            val response = Tracker.query(trackerRequest)
            val peerIp = response.peers.toList().random()
            val (ip, port) = peerIp.split(':')
            connect(ip, port.toInt()) {
                val peer = handshake("00000000000000000000", magnet.infoHash, useExtensions = true)
                println("Peer ID: ${peer.id}")
                val extensionMetadata = sendMetadataMessage(Extended.id, 0)

                @Suppress("UNCHECKED_CAST")
                val m = extensionMetadata["m"]!! as Map<String, Any>
                println("Peer Metadata Extension ID: ${m["ut_metadata"]}")
            }
        }

        "magnet_info" -> {
            val magnetLink = args[1]
            val magnet = Magnet.parse(magnetLink)
            val torrent = Torrent.from(magnet)
            println("Tracker URL: ${torrent.trackerUrl}")
            println("Length: ${torrent.info.length}")
            println("Info Hash: ${torrent.infoHashHex}")
            println("Piece Length: ${torrent.pieceLength(0)}")
            println("Piece Hashses:")
            torrent.info.pieceHashes.forEach {
                println(it)
            }
        }

        "magnet_download_piece" -> {
            assert(args[1] == "-o")
            val outputLocation = args[2]
            val pieceIdx = args[4]

            val magnetLink = args[3]
            val magnet = Magnet.parse(magnetLink)
            val torrent = Torrent.from(magnet)

            val trackerRequest = TrackerRequest(torrent.trackerUrl, torrent.infoHash, torrent.info.length)
            val response = Tracker.query(trackerRequest)
            val peerIp = response.peers.toList().random()
            val (ip, port) = peerIp.split(':')

            connect(ip, port.toInt()) {
                handshake("00000000000000000000", torrent.infoHash)
                pingPong(reader, writer)
                val piece = torrent.downloadPiece(pieceIdx.toInt())
                File(outputLocation).writeBytes(piece)
            }
        }

        "magnet_download" -> {
            assert(args[1] == "-o")
            val outputLocation = args[2]

            val magnetLink = args[3]
            val magnet = Magnet.parse(magnetLink)
            val torrent = Torrent.from(magnet)
            torrent.download(outputLocation)
        }

        else -> println("Unknown command $command")
    }
}
