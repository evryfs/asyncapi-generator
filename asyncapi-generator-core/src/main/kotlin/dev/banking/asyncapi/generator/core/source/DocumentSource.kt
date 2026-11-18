package dev.banking.asyncapi.generator.core.source

import java.io.File

/**
 * Input content and identity used by the reader stage.
 *
 * @property file file used for diagnostics and relative reference resolution
 * @property content complete YAML or JSON source text
 * @property format reader format selected for [content]
 */
internal data class DocumentSource(
    val file: File,
    val content: String,
    val format: InputFormat,
)
