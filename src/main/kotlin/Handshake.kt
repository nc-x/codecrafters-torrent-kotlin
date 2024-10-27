import bencode.toBytes
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers

private const val protocol = "BitTorrent protocol"
private val reserved = ByteArray(8) { 0 }

@OptIn(ExperimentalStdlibApi::class)
suspend fun Torrent.handshake(peerId: String, ip: String, port: Int): String {
    val selectorManager = SelectorManager(Dispatchers.IO)
    aSocket(selectorManager).tcp().connect(ip, port).use {
        val sendChannel = it.openWriteChannel(autoFlush = false)
        val receiveChannel = it.openReadChannel()

        with(sendChannel) {
            writeByte(protocol.length.toByte())
            writeByteArray(protocol.toBytes())
            writeByteArray(reserved)
            writeByteArray(infoHash)
            writeByteArray(peerId.toBytes())
            flush()
        }

        with(receiveChannel) {
            assert(readByte() == protocol.length.toByte())
            assert(readByteArray(protocol.length).contentEquals(protocol.toBytes()))
            assert(readByteArray(reserved.size).contentEquals(reserved))
            assert(readByteArray(infoHash.size).contentEquals(infoHash))
            val responsePeerId = readByteArray(peerId.toBytes().size)
            return responsePeerId.toHexString()
        }
    }
}