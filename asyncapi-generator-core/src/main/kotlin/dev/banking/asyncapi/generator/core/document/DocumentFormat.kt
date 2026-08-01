package dev.banking.asyncapi.generator.core.document

import java.io.File

/**
 * Supported input document formats for the reader stage.
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
        fun fromFile(file: File): DocumentFormat? {
            val extension = file.extension.lowercase()
            return entries.firstOrNull { extension in it.extensions }
        }
    }
}
