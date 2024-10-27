import bencode.decode
import bencode.toBytes
import com.google.gson.Gson

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
            println("Info Hash: ${torrent.infoHash}")
            println("Piece Length: ${torrent.info.pieceLength}")
            torrent.info.pieceHashes.forEach {
                println(it)
            }
        }

        else -> println("Unknown command $command")
    }
}