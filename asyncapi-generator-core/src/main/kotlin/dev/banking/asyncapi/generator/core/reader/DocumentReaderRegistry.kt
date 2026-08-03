package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.document.InputDocument
import java.io.File
import java.io.IOException

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
internal object DocumentReaderRegistry {
    fun read(file: File): InputDocument {
        val format = DocumentFormat.fromFile(file)
            ?: throw DocumentReadException.UnsupportedFormat(file, file.extension.ifBlank { "<none>" })
        val content =
            try {
                DocumentReaderLimits.DEFAULT.readContent(file)
            } catch (exception: IOException) {
                throw DocumentReadException.UnreadableDocument(file, exception)
            } catch (exception: SecurityException) {
                throw DocumentReadException.UnreadableDocument(file, exception)
            }
        val source =
            DocumentSource(
                id = file.nameWithoutExtension,
                file = file,
                content = content,
                format = format,
            )
        return read(source)
    }

    fun read(source: DocumentSource): InputDocument =
        when (source.format) {
            DocumentFormat.YAML -> YamlDocumentReader().read(source)
            DocumentFormat.JSON -> JsonDocumentReader().read(source)
        }
}
