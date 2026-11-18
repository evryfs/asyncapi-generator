package dev.banking.asyncapi.generator.core.parser.tracking

import dev.banking.asyncapi.generator.core.source.SourceId
import dev.banking.asyncapi.generator.core.source.SourceLocation
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * The source documents of one load.
 *
 * Each document gets a [SourceId] that is unique within the load, and diagnostics render snippets of
 * its content.
 */
internal class SourceTracking {
    /**
     * One registered document.
     *
     * @property id the document's id within the load
     * @property file the document's canonical file
     * @property lines the document's content, line by line
     */
    private class Source(
        val id: SourceId,
        val file: File,
        val lines: List<String>,
    )

    private val sourcesByPath = mutableMapOf<String, Source>()
    private val sourcesById = mutableMapOf<SourceId, Source>()

    /** Registers [file] with its [content] and returns its id; registering a file again returns the same id. */
    fun registerSource(
        file: File,
        content: String,
    ): SourceId {
        val canonicalFile = file.canonicalFile
        val source =
            sourcesByPath.getOrPut(canonicalFile.absolutePath) {
                Source(uniqueId(canonicalFile), canonicalFile, content.lines())
                    .also { sourcesById[it.id] = it }
            }
        return source.id
    }

    /** The file registered under [id], or `null` when there is none. */
    fun file(id: SourceId): File? = sourcesById[id]?.file

    /** The id of [file], or `null` when it was not registered. */
    fun sourceId(file: File): SourceId? = sourcesByPath[file.canonicalFile.absolutePath]?.id

    /** A snippet of the source around [location], labelled with [label]. */
    fun snippet(
        location: SourceLocation,
        label: String = location.path,
        contextLines: Int = DEFAULT_CONTEXT_LINES,
    ): String {
        val source = sourcesByPath[location.file.canonicalFile.absolutePath] ?: return "${location.file.name} ($label)"
        val center = location.line
        val start = maxOf(0, center - contextLines - 1)
        val end = minOf(source.lines.size, center + contextLines)
        return buildString {
            appendLine("${source.file.name} ($label)")
            for (index in start until end) {
                val mark = if (index + 1 == center) "→" else " "
                appendLine("$mark ${(index + 1).toString().padStart(LINE_NUMBER_WIDTH)} | ${source.lines[index]}")
            }
        }.trimEnd()
    }

    private fun uniqueId(file: File): SourceId {
        val preferred = SourceId(file.nameWithoutExtension.replace(NON_IDENTIFIER, "_"))
        if (preferred !in sourcesById) return preferred
        val suffix =
            UUID
                .nameUUIDFromBytes(file.absolutePath.toByteArray(StandardCharsets.UTF_8))
                .toString()
                .substringBefore('-')
        val base = "${preferred.value}_$suffix"
        val candidates =
            sequenceOf(base) + generateSequence(2) { it + 1 }.map { discriminator -> "${base}_$discriminator" }
        return candidates.map(::SourceId).first { it !in sourcesById }
    }

    private companion object {
        const val DEFAULT_CONTEXT_LINES = 3
        const val LINE_NUMBER_WIDTH = 4
        val NON_IDENTIFIER = Regex("[^A-Za-z0-9_]")
    }
}
