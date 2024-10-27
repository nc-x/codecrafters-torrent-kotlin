import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import java.io.File

suspend fun Torrent.download(outputLocation: String) {
    val peers = query().peers

    val peer = peers.toList().random()
    val (ip, port) = peer.split(':')

    val selectorManager = SelectorManager(Dispatchers.IO)
    aSocket(selectorManager).tcp().connect(ip, port.toInt()).use { socket ->
        val tx = socket.openWriteChannel(autoFlush = false)
        val rx = socket.openReadChannel()
        handshake(tx, rx, "00000000000000000000")
        
        val pieces = MutableList(numPieces()) { ByteArray(0) }
        for (pieceIdx in 0..<numPieces()) {
            if (pieceIdx > 0) send(tx, PeerMessage.Request, pieceIdx, pieceLength(pieceIdx))
            pieces[pieceIdx] = downloadPiece(tx, rx, pieceIdx)
        }
        val fileContents = pieces.reduce { acc, bytes -> acc + bytes }
        File(outputLocation).writeBytes(fileContents)
    }
}