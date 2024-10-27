import bencode.decode
import bencode.toBytes
import com.google.gson.Gson
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File

@OptIn(ExperimentalStdlibApi::class)
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
            val response = runBlocking { torrent.query() }
            response.peers.forEach {
                println(it)
            }
        }

        "handshake" -> {
            val torrent = Torrent.from(args[1])
            val peer = args[2]
            val (ip, port) = peer.split(':')
            val selectorManager = SelectorManager(Dispatchers.IO)
            aSocket(selectorManager).tcp().connect(ip, port.toInt()).use { socket ->
                val tx = socket.openWriteChannel(autoFlush = false)
                val rx = socket.openReadChannel()
                val peerId = torrent.handshake(tx, rx, "00000000000000000000")
                println("Peer ID: $peerId")
            }
        }

        "download_piece" -> {
            assert(args[1] == "-o")
            val outputLocation = args[2]
            val torrent = Torrent.from(args[3])
            val pieceIdx = args[4]

            val peers = torrent.query().peers

            val peer = peers.toList().random()
            val (ip, port) = peer.split(':')

            val selectorManager = SelectorManager(Dispatchers.IO)
            aSocket(selectorManager).tcp().connect(ip, port.toInt()).use { socket ->
                val tx = socket.openWriteChannel(autoFlush = false)
                val rx = socket.openReadChannel()
                val peerId = torrent.handshake(tx, rx, "00000000000000000000")
                val piece = torrent.downloadPiece(tx, rx, pieceIdx.toInt())
                File(outputLocation).writeBytes(piece)
            }
        }

        "download" -> {
            assert(args[1] == "-o")
            val outputLocation = args[2]
            val torrent = Torrent.from(args[3])
            torrent.download(outputLocation)
        }

        else -> println("Unknown command $command")
    }
}