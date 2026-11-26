package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.output.GeneratedOutputManifest
import java.io.File

/**
 * Where generated output is written.
 *
 * @param outputDirectory the directory generated sources and schemas are written to.
 * @param document the bundled document to write, or `null` for none.
 * @param manifest the record of generated files used to remove stale ones, or `null` to keep every file.
 */
data class GeneratorOutputConfiguration(
    val outputDirectory: File,
    val document: DocumentOutput? = null,
    val manifest: GeneratedOutputManifest? = null,
)

/**
 * Bundled AsyncAPI document output selected by the generator profile.
 *
 * @param file the file the document is written to.
 * @param format the serialization of the document.
 */
data class DocumentOutput(
    val file: File,
    val format: DocumentFormat,
)
