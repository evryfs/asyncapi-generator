package dev.banking.asyncapi.generator.core.source

import java.io.File
import java.io.IOException
import java.nio.charset.CharacterCodingException

/**
 * Reads an input file into an [InputDocument].
 *
 * It detects the input format from the file extension, reads the content within the document limits, and
 * delegates to the reader for that format.
 */
internal object SourceReader {
    /** Reads [file] as YAML or JSON, depending on its extension, within [limits]. */
    fun read(
        file: File,
        limits: SourceLimits = SourceLimits(),
    ): InputDocument {
        requireInputFile(file)
        val format =
            InputFormat.fromFile(file)
                ?: throw DocumentReadException.UnsupportedFormat(file, file.extension.ifBlank { "<none>" })
        val source =
            DocumentSource(
                file = file,
                content = readContent(file, limits),
                format = format,
            )
        return read(source, limits)
    }

    /** Fails with a reader error when [file] does not exist or is not a regular file. */
    fun requireInputFile(file: File) {
        if (!file.exists()) throw DocumentReadException.MissingDocument(file)
        if (!file.isFile) throw DocumentReadException.NotAFile(file)
    }

    /** Reads already loaded [source] content in its format, within [limits]. */
    fun read(
        source: DocumentSource,
        limits: SourceLimits = SourceLimits(),
    ): InputDocument =
        when (source.format) {
            InputFormat.YAML -> YamlDocumentReader(limits).read(source)
            InputFormat.JSON -> JsonDocumentReader(limits).read(source)
        }

    private fun readContent(
        file: File,
        limits: SourceLimits,
    ): String {
        val bytes =
            readBytesAtMost(file, limits.maxDocumentBytes)
                ?: throw limits.documentLimitExceeded(
                    file,
                    DocumentResourceLimit.DOCUMENT_BYTES,
                    limits.maxDocumentBytes,
                )
        return try {
            bytes.decodeUtf8Strictly()
        } catch (exception: CharacterCodingException) {
            throw DocumentReadException.MalformedDocument(file, exception)
        }
    }

    private fun readBytesAtMost(
        file: File,
        maxBytes: Int,
    ): ByteArray? =
        try {
            file.readBytesAtMost(maxBytes)
        } catch (exception: IOException) {
            throw DocumentReadException.UnreadableDocument(file, exception)
        } catch (exception: SecurityException) {
            throw DocumentReadException.UnreadableDocument(file, exception)
        }
}
