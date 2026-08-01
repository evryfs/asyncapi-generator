package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentSource
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction.REPORT
import java.nio.charset.CharacterCodingException
import java.nio.charset.StandardCharsets.UTF_8

/** Resource limits shared by the YAML and JSON reader implementations. */
internal data class DocumentReaderLimits(
    val maxDocumentBytes: Int,
    val maxDocumentCharacters: Int,
    val maxNestingDepth: Int,
    val maxAliasesForCollections: Int,
) {
    fun readContent(file: File): String {
        val bytes = file.inputStream().use { input ->
            input.readNBytes(maxDocumentBytes + 1)
        }
        if (bytes.size > maxDocumentBytes) {
            throw limitExceeded(file)
        }
        return try {
            UTF_8.newDecoder()
                .onMalformedInput(REPORT)
                .onUnmappableCharacter(REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (exception: CharacterCodingException) {
            throw DocumentReadException.MalformedDocument(file, exception)
        }
    }

    fun requireDocumentSize(source: DocumentSource) {
        if (
            source.content.length > maxDocumentCharacters ||
            source.content.toByteArray(UTF_8).size > maxDocumentBytes
        ) {
            throw limitExceeded(source.file)
        }
    }

    private fun limitExceeded(file: File): DocumentReadException.ResourceLimitExceeded =
        DocumentReadException.ResourceLimitExceeded(
            file = file,
            cause = IllegalArgumentException(
                "Maximum document size is $maxDocumentBytes UTF-8 bytes and " +
                    "$maxDocumentCharacters characters",
            ),
        )

    companion object {
        private const val MEBIBYTE = 1024 * 1024

        val DEFAULT =
            DocumentReaderLimits(
                maxDocumentBytes = 20 * MEBIBYTE,
                maxDocumentCharacters = 20 * MEBIBYTE,
                maxNestingDepth = 100,
                maxAliasesForCollections = 50,
            )
    }
}
