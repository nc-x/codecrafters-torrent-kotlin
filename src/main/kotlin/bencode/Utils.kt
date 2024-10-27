package bencode

fun ByteArray.toStringDecoded(): String =
    String(this, Charsets.ISO_8859_1)

fun ByteArray.toInt(): Int =
    toStringDecoded().toInt()

fun ByteArray.toLong(): Long =
    toStringDecoded().toLong()

fun String.toBytes(): ByteArray =
    toByteArray(Charsets.ISO_8859_1)

