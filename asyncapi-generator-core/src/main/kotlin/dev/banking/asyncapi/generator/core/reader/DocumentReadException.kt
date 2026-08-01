package dev.banking.asyncapi.generator.core.reader

import java.io.File

/**
 * Reader-stage errors raised before AsyncAPI parsing or validation starts.
 *
 * Expected behavior is covered by:
 * - `DocumentReadExceptionTest`
 * - `YamlDocumentReaderTest`
 * - `JsonDocumentReaderTest`
 */
sealed class DocumentReadException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    class EmptyDocument(
        file: File,
    ) : DocumentReadException("Input document is empty: ${file.absolutePath}")

    class UnsupportedFormat(
        file: File,
        format: String,
    ) : DocumentReadException("Unsupported input document format '$format': ${file.absolutePath}")

    class MalformedDocument(
        file: File,
        cause: Throwable,
    ) : DocumentReadException("Malformed input document: ${file.absolutePath}", cause)

    class UnreadableDocument(
        file: File,
        cause: Throwable,
    ) : DocumentReadException("Unable to read input document: ${file.absolutePath}", cause)

    class ResourceLimitExceeded(
        file: File,
        cause: Throwable,
    ) : DocumentReadException("Input document exceeds reader resource limits: ${file.absolutePath}", cause)

    class InvalidMappingKey(
        file: File,
        line: Int,
        column: Int,
    ) : DocumentReadException(
        "Invalid mapping key in ${file.absolutePath} at line $line, column $column: expected string key",
    )

    class DuplicateKey(
        file: File,
        key: String,
        line: Int,
        column: Int,
    ) : DocumentReadException(
        "Duplicate key '$key' in ${file.absolutePath} at line $line, column $column",
    )
}
