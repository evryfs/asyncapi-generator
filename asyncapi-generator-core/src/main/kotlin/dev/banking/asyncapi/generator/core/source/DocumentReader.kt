package dev.banking.asyncapi.generator.core.source

/**
 * Reads the content of one document format into a document tree.
 *
 * The checks that every format shares (document size, byte-order mark, blank content) happen in [read];
 * a format only implements [readRoot].
 */
internal interface DocumentReader {
    /** Limits enforced while reading. */
    val limits: SourceLimits

    /** Reads [source] into an [InputDocument]. */
    fun read(source: DocumentSource): InputDocument {
        limits.requireDocumentSize(source)
        val content = source.content.removePrefix(UTF_8_BOM)
        if (content.isBlank()) {
            throw DocumentReadException.EmptyDocument(source.file)
        }
        return InputDocument(source = source, root = readRoot(source, content))
    }

    /** Parses [content], the text of [source] without its byte-order mark, into the root node. */
    fun readRoot(
        source: DocumentSource,
        content: String,
    ): DocumentNode

    private companion object {
        const val UTF_8_BOM = "﻿"
    }
}
