import com.google.gson.Gson;

// import com.dampcake.bencode.Bencode; - available if you need it!

val gson = Gson()

fun main(args: Array<String>) {
    when (val command = args[0]) {
        "decode" -> {
            val bencoded = args[1]
            val decoded = decode(bencoded)
            println(gson.toJson(decoded))
            return
        }

        else -> println("Unknown command $command")
    }
}