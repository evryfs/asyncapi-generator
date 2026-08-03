package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.SourceLocation
import java.io.File

/** Reader resource whose configured maximum was exceeded. */
enum class DocumentResourceLimit(val description: String) {
    DOCUMENT_BYTES("UTF-8 document bytes"),
    DOCUMENT_CHARACTERS("document characters"),
    NESTING_DEPTH("nesting depth"),
    COLLECTION_ALIASES("collection aliases"),
    NUMBER_CHARACTERS("numeric token characters"),
}

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
    val file: File,
    val location: SourceLocation? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    class EmptyDocument(
        file: File,
    ) : DocumentReadException(
        message = "Input document is empty: ${file.absolutePath}",
        file = file,
    )

    class UnsupportedFormat(
        file: File,
        val format: String,
    ) : DocumentReadException(
        message = "Unsupported input document format '$format': ${file.absolutePath}",
        file = file,
    )

    class MalformedDocument(
        file: File,
        cause: Throwable,
        location: SourceLocation? = null,
    ) : DocumentReadException(
        message = "Malformed input document${locationSuffix(location)}: ${file.absolutePath}",
        file = file,
        location = location,
        cause = cause,
    )

    class UnreadableDocument(
        file: File,
        cause: Throwable,
    ) : DocumentReadException(
        message = "Unable to read input document: ${file.absolutePath}",
        file = file,
        cause = cause,
    )

    class ResourceLimitExceeded(
        file: File,
        val limit: DocumentResourceLimit,
        val maximum: Long,
        cause: Throwable,
        location: SourceLocation? = null,
    ) : DocumentReadException(
        message =
            "Input document exceeds ${limit.description} limit of $maximum" +
                "${locationSuffix(location)}: ${file.absolutePath}",
        file = file,
        location = location,
        cause = cause,
    )

    class InvalidMappingKey(
        file: File,
        location: SourceLocation,
    ) : DocumentReadException(
        message =
            "Invalid mapping key in ${file.absolutePath} at line ${location.line}, " +
                "column ${location.column}: expected string key",
        file = file,
        location = location,
    )

    class DuplicateKey(
        file: File,
        val memberName: String,
        location: SourceLocation,
    ) : DocumentReadException(
        message =
            "Duplicate key '$memberName' in ${file.absolutePath} at line ${location.line}, " +
                "column ${location.column}",
        file = file,
        location = location,
    )

    companion object {
        private fun locationSuffix(location: SourceLocation?): String =
            location?.let { " at line ${it.line}, column ${it.column}" }.orEmpty()
    }
}
