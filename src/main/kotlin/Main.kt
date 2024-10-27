import bencode.decode
import com.google.gson.Gson

// import com.dampcake.bencode.Bencode; - available if you need it!


fun main(args: Array<String>) {
    val gson = Gson()

    when (val command = args[0]) {
        "decode" -> {
            val bencoded = args[1]
            val decoded = decode(bencoded.toByteArray())
            println(gson.toJson(decoded))
        }

        "info" -> {
            val torrent = Torrent.from(args[1])
            println("Tracker URL: ${torrent.announce}")
            println("Length: ${torrent.info.length}")
            println("Info Hash: ${torrent.infoHash}")
        }

        else -> println("Unknown command $command")
    }
}