package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.document.InputDocument
import java.io.File

/**
 * Selects the correct `DocumentReader` for an input file.
 *
 * This is the reader-stage entry point for file-based inputs. It detects the
 * input format, creates the `DocumentSource`, and delegates parsing to a
 * format-specific reader.
 *
 * Expected behavior is covered by:
 * - `DocumentReaderRegistryTest`
 */
object DocumentReaderRegistry {
    private val yamlReader = YamlDocumentReader()
    private val jsonReader = JsonDocumentReader()

    fun read(file: File): InputDocument {
        val format = DocumentFormat.fromFile(file)
            ?: throw DocumentReadException.UnsupportedFormat(file, file.extension.ifBlank { "<none>" })
        val source =
            DocumentSource(
                id = file.nameWithoutExtension,
                file = file,
                content = file.readText(Charsets.UTF_8),
                format = format,
            )
        return read(source)
    }

    fun read(source: DocumentSource): InputDocument =
        when (source.format) {
            DocumentFormat.YAML -> yamlReader.read(source)
            DocumentFormat.JSON -> jsonReader.read(source)
        }
}
