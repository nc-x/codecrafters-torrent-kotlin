fun decode(bencoded: String): Any {
    val decoded = _decode(bencoded)
    if (decoded.second != bencoded.length) error("Error decoding: Extra content")
    return decoded.first
}

private fun _decode(bencoded: String): Pair<Any, Int> =
    when (bencoded[0]) {
        'i' -> decodeInt(bencoded)
        'l' -> decodeList(bencoded)
        'd' -> decodeDict(bencoded)
        in '0'..'9' -> decodeString(bencoded)
        else -> error("Error decoding '${bencoded[0]}': Invalid value?")
    }

private fun decodeInt(bencoded: String): Pair<Long, Int> {
    val idx = bencoded.indexOfFirst { it == 'e' }
    return Pair(bencoded.substring(1, idx).toLong(), idx + 1)
}

private fun decodeString(bencoded: String): Pair<String, Int> {
    val colonIdx = bencoded.indexOfFirst { it == ':' }
    val length = bencoded.substring(0, colonIdx).toInt()
    val startIdx = colonIdx + 1
    val endIdx = startIdx + length
    val decoded = bencoded.substring(colonIdx + 1, endIdx)
    return Pair(decoded, endIdx)
}

private fun decodeList(bencoded: String): Pair<List<Any>, Int> {
    val result = mutableListOf<Any>()
    var i = 1 // skip 0th: 'l'
    while (i < bencoded.lastIndex && bencoded[i] != 'e') {
        val (elem, len) = _decode(bencoded.substring(i))
        result.add(elem)
        i += len
    }
    if (i > bencoded.lastIndex) error("Error decoding list: End of content")
    return Pair(result, i + 1)
}

private fun decodeDict(bencoded: String): Pair<Map<String, Any>, Int> {
    val result = mutableMapOf<String, Any>()
    var i = 1 // skip 0th: 'd'
    while (i < bencoded.lastIndex && bencoded[i] != 'e') {
        val (key, len1) = decodeString(bencoded.substring(i))
        i += len1
        if (i > bencoded.lastIndex) error("Error decoding dict: End of content")
        val (value, len2) = _decode(bencoded.substring(i))
        i += len2
        result[key] = value
    }
    if (i > bencoded.lastIndex) error("Error decoding dict: End of content")
    return Pair(result, i + 1)
}
