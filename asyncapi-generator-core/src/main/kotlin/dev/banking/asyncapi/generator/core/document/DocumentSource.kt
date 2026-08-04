package dev.banking.asyncapi.generator.core.document

import java.io.File

/**
 * Input content and identity used by the reader stage.
 *
 * @property id stable identity used to distinguish this source from other root or external sources
 * @property file file used for diagnostics and relative reference resolution
 * @property content complete YAML or JSON source text
 * @property format reader format selected for [content]
 */
internal data class DocumentSource(
    val id: String,
    val file: File,
    val content: String,
    val format: DocumentFormat,
)
