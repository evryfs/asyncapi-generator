package dev.banking.asyncapi.generator.core.document

import java.io.File

/**
 * Input content and identity used by the reader stage.
 *
 * Expected behavior is covered by:
 * - `DocumentReaderRegistryTest`
 * - `YamlDocumentReaderTest`
 */
data class DocumentSource(
    val id: String,
    val file: File,
    val content: String,
    val format: DocumentFormat,
)
