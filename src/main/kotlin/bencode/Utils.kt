package bencode

import java.security.MessageDigest

fun ByteArray.toStringDecoded(): String =
    String(this, Charsets.ISO_8859_1)

fun ByteArray.parseInt(): Int =
    toStringDecoded().toInt()

fun ByteArray.parseLong(): Long =
    toStringDecoded().toLong()

fun String.toBytes(): ByteArray =
    toByteArray(Charsets.ISO_8859_1)

fun ByteArray.getSHA1(): ByteArray =
    MessageDigest.getInstance("SHA-1").digest(this)
