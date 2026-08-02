package dev.banking.asyncapi.generator.core.document

import java.io.File

/**
 * Location of a value in an input document.
 *
 * Paths start at `root`, use dot-separated object member names, and use
 * bracketed zero-based array indexes, for example `root.channels[0]`. Lines
 * and columns are one-based for both YAML and JSON sources.
 *
 * @property sourceId stable identity of the input source that owns the value
 * @property file source file used for diagnostics and relative reference resolution
 * @property path reader/parser path of the value within the source
 * @property line one-based source line
 * @property column one-based source column
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
