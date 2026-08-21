package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.document.SourceLocation
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction.REPORT
import java.nio.charset.CharacterCodingException
import java.nio.charset.StandardCharsets.UTF_8

/**
 * Resource limits shared by the YAML and JSON reader implementations.
 *
 * @property maxDocumentBytes maximum file size in bytes
 * @property maxDocumentCharacters maximum content length in characters
 * @property maxNestingDepth maximum object/array nesting depth
 * @property maxAliasesForCollections maximum YAML alias references per collection
 * @property maxNumberCharacters maximum length of a numeric token
 */
internal data class DocumentReaderLimits(
    val maxDocumentBytes: Int,
    val maxDocumentCharacters: Int,
    val maxNestingDepth: Int,
    val maxAliasesForCollections: Int,
    val maxNumberCharacters: Int,
) {
    fun readContent(file: File): String {
        val bytes = file.inputStream().use { input ->
            input.readNBytes(maxDocumentBytes + 1)
        }
        if (bytes.size > maxDocumentBytes) {
            throw limitExceeded(
                file = file,
                limit = DocumentResourceLimit.DOCUMENT_BYTES,
                maximum = maxDocumentBytes,
            )
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
        val encodedBytes = source.content.toByteArray(UTF_8).size
        if (encodedBytes > maxDocumentBytes) {
            throw limitExceeded(
                file = source.file,
                limit = DocumentResourceLimit.DOCUMENT_BYTES,
                maximum = maxDocumentBytes,
            )
        }
        if (source.content.length > maxDocumentCharacters) {
            throw limitExceeded(
                file = source.file,
                limit = DocumentResourceLimit.DOCUMENT_CHARACTERS,
                maximum = maxDocumentCharacters,
            )
        }
    }

    fun requireNumberLength(
        value: String,
        location: SourceLocation,
    ) {
        if (value.length > maxNumberCharacters) {
            throw DocumentReadException.ResourceLimitExceeded(
                file = location.file,
                limit = DocumentResourceLimit.NUMBER_CHARACTERS,
                maximum = maxNumberCharacters.toLong(),
                cause = IllegalArgumentException(
                    "Maximum numeric token length is $maxNumberCharacters characters",
                ),
                location = location,
            )
        }
    }

    private fun limitExceeded(
        file: File,
        limit: DocumentResourceLimit,
        maximum: Int,
    ): DocumentReadException.ResourceLimitExceeded =
        DocumentReadException.ResourceLimitExceeded(
            file = file,
            limit = limit,
            maximum = maximum.toLong(),
            cause = IllegalArgumentException(
                "Maximum ${limit.description} is $maximum",
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
                maxNumberCharacters = 1000,
            )
    }
}
