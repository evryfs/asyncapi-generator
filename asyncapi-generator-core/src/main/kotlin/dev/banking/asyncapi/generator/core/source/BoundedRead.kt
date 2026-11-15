package dev.banking.asyncapi.generator.core.source

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction.REPORT
import java.nio.charset.StandardCharsets.UTF_8

/** Reads this file, or returns `null` without reading further when it is larger than [maxBytes]. */
internal fun File.readBytesAtMost(maxBytes: Int): ByteArray? {
    val bytes = inputStream().use { input -> input.readNBytes(maxBytes + 1) }
    return bytes.takeIf { it.size <= maxBytes }
}

/** Decodes these bytes as UTF-8, failing with a `CharacterCodingException` on invalid input. */
internal fun ByteArray.decodeUtf8Strictly(): String =
    UTF_8
        .newDecoder()
        .onMalformedInput(REPORT)
        .onUnmappableCharacter(REPORT)
        .decode(ByteBuffer.wrap(this))
        .toString()
