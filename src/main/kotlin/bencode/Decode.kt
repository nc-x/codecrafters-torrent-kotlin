package bencode

fun decode(bencoded: ByteArray): Any {
    val decoded = decodeStream(bencoded)
    if (decoded.second != bencoded.size) error("Error decoding: Extra content")
    return decoded.first
}

fun decodeStream(bencoded: ByteArray): Pair<Any, Int> =
    when (bencoded[0]) {
        'i'.code.toByte() -> decodeInt(bencoded)
        'l'.code.toByte() -> decodeList(bencoded)
        'd'.code.toByte() -> decodeDict(bencoded)
        in '0'.code.toByte()..'9'.code.toByte() -> decodeString(bencoded)
        else -> error("Error decoding '${bencoded[0]}': Invalid value?")
    }

private fun decodeInt(bencoded: ByteArray): Pair<Long, Int> {
    val idx = bencoded.indexOfFirst { it == 'e'.code.toByte() }
    return Pair(bencoded.sliceArray(1..<idx).parseLong(), idx + 1)
}

private fun decodeString(bencoded: ByteArray): Pair<String, Int> {
    val colonIdx = bencoded.indexOfFirst { it == ':'.code.toByte() }
    val length = bencoded.sliceArray(0..<colonIdx).parseInt()
    val startIdx = colonIdx + 1
    val endIdx = startIdx + length
    val decoded = bencoded.sliceArray(colonIdx + 1..<endIdx)
    return Pair(decoded.toStringDecoded(), endIdx)
}

private fun decodeList(bencoded: ByteArray): Pair<List<Any>, Int> {
    val result = mutableListOf<Any>()
    var i = 1 // skip 0th: 'l'
    while (i < bencoded.lastIndex && bencoded[i] != 'e'.code.toByte()) {
        val (elem, len) = decodeStream(bencoded.sliceArray(i..bencoded.lastIndex))
        result.add(elem)
        i += len
    }
    if (i > bencoded.lastIndex) error("Error decoding list: End of content")
    return Pair(result, i + 1)
}

private fun decodeDict(bencoded: ByteArray): Pair<Map<String, Any>, Int> {
    val result = mutableMapOf<String, Any>()
    var i = 1 // skip 0th: 'd'
    while (i < bencoded.lastIndex && bencoded[i] != 'e'.code.toByte()) {
        val (key, len1) = decodeString(bencoded.sliceArray(i..bencoded.lastIndex))
        i += len1
        if (i > bencoded.lastIndex) error("Error decoding dict: End of content")
        val (value, len2) = decodeStream(bencoded.sliceArray(i..bencoded.lastIndex))
        i += len2
        result[key] = value
    }
    if (i > bencoded.lastIndex) error("Error decoding dict: End of content")
    return Pair(result, i + 1)
}
