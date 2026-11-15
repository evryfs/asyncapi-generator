package dev.banking.asyncapi.generator.core.source

import java.io.File

/**
 * A failure to read an input document, raised before AsyncAPI parsing starts.
 *
 * @param message the complete message shown to the user
 * @property file the document that could not be read
 * @property location where in the document the problem is, when it is known
 * @param cause the underlying failure from the file system or the YAML or JSON parser, when there is one
 */
internal sealed class DocumentReadException(
    message: String,
    val file: File,
    val location: SourceLocation? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    /**
     * The input document does not exist.
     *
     * @param file the missing document
     */
    class MissingDocument(
        file: File,
    ) : DocumentReadException(
            message = "Input document does not exist: ${file.absolutePath}",
            file = file,
        )

    /**
     * The input path exists but is not a regular file.
     *
     * @param file the path that is not a file
     */
    class NotAFile(
        file: File,
    ) : DocumentReadException(
            message = "Input document is not a file: ${file.absolutePath}",
            file = file,
        )

    /**
     * The input document is empty or contains only whitespace.
     *
     * @param file the empty document
     */
    class EmptyDocument(
        file: File,
    ) : DocumentReadException(
            message = "Input document is empty: ${file.absolutePath}",
            file = file,
        )

    /**
     * The file extension is not one of the supported input formats.
     *
     * @param file the document
     * @property format the unsupported extension, or `<none>`
     */
    class UnsupportedFormat(
        file: File,
        val format: String,
    ) : DocumentReadException(
            message = "Unsupported input document format '$format': ${file.absolutePath}",
            file = file,
        )

    /**
     * The content is not valid YAML or JSON, or uses a construct the reader rejects.
     *
     * @param file the document
     * @param cause the parser failure, when there is one
     * @param location where the problem is, when it is known
     * @param reason what is wrong, when the reader knows it better than the parser failure
     */
    class MalformedDocument(
        file: File,
        cause: Throwable? = null,
        location: SourceLocation? = null,
        reason: String? = null,
    ) : DocumentReadException(
            message =
                "Malformed input document${locationSuffix(location)}${reason?.let { " ($it)" }.orEmpty()}: " +
                    file.absolutePath,
            file = file,
            location = location,
            cause = cause,
        ) {
        companion object {
            /** A malformed document at [location], described by [reason]. */
            fun at(
                location: SourceLocation,
                reason: String,
            ): MalformedDocument = MalformedDocument(file = location.file, location = location, reason = reason)
        }
    }

    /**
     * The document exists but cannot be read.
     *
     * @param file the document
     * @param cause the file system failure
     */
    class UnreadableDocument(
        file: File,
        cause: Throwable,
    ) : DocumentReadException(
            message = "Unable to read input document: ${file.absolutePath}",
            file = file,
            cause = cause,
        )

    /**
     * The document exceeds a per-document resource limit.
     *
     * @param file the document
     * @property limit the exceeded limit
     * @property maximum the configured maximum
     * @param cause the parser failure that reported the limit, when there is one
     * @param location where the limit was exceeded, when it is known
     */
    class ResourceLimitExceeded(
        file: File,
        val limit: DocumentResourceLimit,
        val maximum: Long,
        cause: Throwable? = null,
        location: SourceLocation? = null,
    ) : DocumentReadException(
            message =
                "Input document exceeds ${limit.description} limit of $maximum" +
                    "${locationSuffix(location)}: ${file.absolutePath}",
            file = file,
            location = location,
            cause = cause,
        )

    /**
     * A YAML mapping key is not a string.
     *
     * @param file the document
     * @param location location of the key
     */
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

    /**
     * An object declares the same key twice.
     *
     * @param file the document
     * @property memberName the duplicated key
     * @param location location of the second declaration
     */
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
