package magnet

import io.ktor.http.*

// magnet:
// ?xt=urn:btih:ad42ce8109f54c99613ce38f9b4d87e70f24a165
// &dn=magnet1.gif
// &tr=http%3A%2F%2Fbittorrent-test-tracker.codecrafters.io%2Fannounce
data class Magnet(
    val trackerUrl: String?,
    val infoHash: ByteArray,
    val displayName: String?,
) {
    @OptIn(ExperimentalStdlibApi::class)
    val infoHashHex: String
        get() = infoHash.toHexString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Magnet

        if (trackerUrl != other.trackerUrl) return false
        if (!infoHash.contentEquals(other.infoHash)) return false
        if (displayName != other.displayName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = trackerUrl?.hashCode() ?: 0
        result = 31 * result + infoHash.contentHashCode()
        result = 31 * result + (displayName?.hashCode() ?: 0)
        return result
    }

    companion object {
        private const val MAGNET_PREFIX = "magnet:?"
        private const val XT_PREFIX = "urn:btih:"

        @OptIn(ExperimentalStdlibApi::class)
        fun parse(magnetLink: String): Magnet {
            if (!magnetLink.startsWith(MAGNET_PREFIX)) error("invalid magnet uri")
            return magnetLink
                .substring(MAGNET_PREFIX.length)
                .split("&")
                .associate { it.split("=").let { it[0] to it[1] } }
                .let { params ->
                    val xt = params["xt"]?.takeIf { it.startsWith(XT_PREFIX) }
                        ?.removePrefix(XT_PREFIX)
                        ?: error("invalid magnet uri: missing 'xt'")

                    Magnet(
                        trackerUrl = params["tr"]?.decodeURLPart(),
                        infoHash = xt.hexToByteArray(),
                        displayName = params["dn"],
                    )
                }
        }
    }
}
