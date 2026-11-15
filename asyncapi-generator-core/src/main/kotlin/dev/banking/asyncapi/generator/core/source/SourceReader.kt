package dev.banking.asyncapi.generator.core.source

import java.io.File
import java.io.IOException

/**
 * Selects the correct format-specific reader for an input file.
 *
 * This is the reader-stage entry point for file-based inputs. It detects the
 * input format, creates the `DocumentSource`, and delegates parsing to a
 * format-specific reader.
 */
internal object SourceReader {
    fun read(file: File): InputDocument {
        requireInputFile(file)
        val format =
            InputFormat.fromFile(file)
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
                file = file,
                content = content,
                format = format,
            )
        return read(source)
    }

    /** Fails with a reader error when [file] does not exist or is not a regular file. */
    fun requireInputFile(file: File) {
        if (!file.exists()) throw DocumentReadException.MissingDocument(file)
        if (!file.isFile) throw DocumentReadException.NotAFile(file)
    }

    fun read(source: DocumentSource): InputDocument =
        when (source.format) {
            InputFormat.YAML -> YamlDocumentReader().read(source)
            InputFormat.JSON -> JsonDocumentReader().read(source)
        }
}
