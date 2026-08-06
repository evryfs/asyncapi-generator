package dev.banking.asyncapi.generator.core.generator.output

import java.io.File

/**
 * Rendered bundled-document output targeting an explicitly configured file.
 */
data class GeneratedDocumentArtifact(
    val file: File,
    val content: String,
)
