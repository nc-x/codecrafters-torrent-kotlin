package bencode

fun encode(value: Any): String =
    when (value) {
        is Long -> "i${value}e"
        is String -> "${value.length}:${value}"
        is List<*> -> buildString {
            append("l")
            for (elem in value) {
                append(encode(value))
            }
            append("e")
        }

        is Map<*, *> -> buildString {
            @Suppress("UNCHECKED_CAST")
            value as Map<String, *>
            val map = value.toSortedMap()
            append("d")
            for ((k, v) in map) {
                append(encode(k!!))
                append(encode(v!!))
            }
            append("e")
        }

        else -> error("cannot encode '${value::class.simpleName}': must be long/string/list/map")
    }
