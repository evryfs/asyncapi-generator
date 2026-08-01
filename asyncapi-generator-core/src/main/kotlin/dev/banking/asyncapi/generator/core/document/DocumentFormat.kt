package dev.banking.asyncapi.generator.core.document

import java.io.File

/**
 * Supported input document formats for the reader stage.
 *
 * @property extensions recognized lowercase file extensions without a leading dot
 *
 * Expected behavior is covered by:
 * - `DocumentReaderRegistryTest`
 */
enum class DocumentFormat(
    val extensions: Set<String>,
) {
    YAML(setOf("yaml", "yml")),
    JSON(setOf("json"));

    companion object {
        /** Returns the format identified by [file]'s extension, if supported. */
        fun fromFile(file: File): DocumentFormat? {
            val extension = file.extension.lowercase()
            return entries.firstOrNull { extension in it.extensions }
        }
    }
}
