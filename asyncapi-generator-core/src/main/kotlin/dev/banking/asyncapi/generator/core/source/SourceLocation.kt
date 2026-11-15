package dev.banking.asyncapi.generator.core.source

import java.io.File

/**
 * Location of a value in an input document.
 *
 * Lines and columns are one-based for both YAML and JSON sources.
 *
 * @property file source file, used for diagnostics and relative reference resolution
 * @property path position of the value, rendered as a [NodePath] by the reader; later stages may
 *   replace it with a more specific path
 * @property line one-based source line
 * @property column one-based source column
 */
data class SourceLocation(
    val file: File,
    val path: String,
    val line: Int,
    val column: Int,
) {
    companion object {
        internal fun from(
            source: DocumentSource,
            path: NodePath,
            line: Int,
            column: Int,
        ): SourceLocation =
            SourceLocation(
                file = source.file,
                path = path.toString(),
                line = line,
                column = column,
            )
    }
}
