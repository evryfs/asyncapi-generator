package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.output.GeneratedOutputManifest
import java.io.File

/**
 * Output directories used by generated source and resource artifacts.
 *
 * @param sourceOutputDirectory the directory for generated sources.
 * @param resourceOutputDirectory the directory for generated schemas.
 * @param javaSourceOutputDirectory the directory for generated Java sources.
 * @param document the bundled document to write, or `null` for none.
 * @param manifest the record of generated files used to remove stale ones, or `null` to keep every file.
 */
data class GeneratorOutputConfiguration(
    val sourceOutputDirectory: File,
    val resourceOutputDirectory: File,
    val javaSourceOutputDirectory: File = sourceOutputDirectory,
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
