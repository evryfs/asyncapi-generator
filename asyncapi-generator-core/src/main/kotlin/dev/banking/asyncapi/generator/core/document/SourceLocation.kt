package dev.banking.asyncapi.generator.core.document

import java.io.File

/**
 * Location of a value in an input document.
 *
 * Expected behavior is covered by:
 * - `DocumentLocationTest`
 */
data class SourceLocation(
    val sourceId: String,
    val file: File,
    val path: String,
    val line: Int,
    val column: Int,
) {
    companion object {
        internal fun from(
            source: DocumentSource,
            path: String,
            line: Int,
            column: Int,
        ): SourceLocation =
            SourceLocation(
                sourceId = source.id,
                file = source.file,
                path = path,
                line = line,
                column = column,
            )
    }
}
