import java.io.File

suspend fun Torrent.download(outputLocation: String) {
    val trackerRequest = TrackerRequest(trackerUrl, infoHash, info.length)
    val response = Tracker.query(trackerRequest)
    val peers = response.peers

    val peer = peers.toList().random()
    val (ip, port) = peer.split(':')

    connect(ip, port.toInt()) {
        handshake("00000000000000000000", infoHash)
        pingPong(reader, writer)

        val pieces = MutableList(numPieces()) { ByteArray(0) }
        for (pieceIdx in 0..<numPieces()) {
            pieces[pieceIdx] = downloadPiece(pieceIdx)
        }
        val fileContents = pieces.reduce { acc, bytes -> acc + bytes }
        File(outputLocation).writeBytes(fileContents)
    }
}
