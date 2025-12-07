package com.tietoevry.banking.asyncapi.generator.core.repository

import java.io.File
import kotlin.math.max
import kotlin.math.min

class SourceRepository {

    data class Source(
        val file: File,
        val id: String,
        val lines: List<String>
    )

    // Map of file absolute path → Source
    private val sources = mutableMapOf<String, Source>()

    // Map of node path → line number
    internal val lineMap = mutableMapOf<String, Int>()

    private lateinit var current: Source

    fun registerSource(file: File, content: String) {
        val id = file.nameWithoutExtension.replace(Regex("[^A-Za-z0-9_]"), "_")
        val src = Source(file = file, id = id, lines = content.lines())
        sources[file.absolutePath] = src
        current = src
    }

    fun registerLine(path: String, line: Int) {
        lineMap[path] = line
    }

    fun getLine(path: String): Int? = lineMap[path]

    fun getCurrentFile(): File = current.file
    fun getAllSources(): Collection<Source> = sources.values
    fun getAllLines(): Map<String, Int> = lineMap.toMap()

    fun fileIdForName(name: String): String? {
        val clean = name.trim().trimStart('\'', '"', '|', '>')
        // exact match on filename
        sources.values.firstOrNull { it.file.name == clean }?.let { return it.id }
        // fallback: basename
        val base = File(clean).name
        sources.values.firstOrNull { it.file.name == base }?.let { return it.id }
        return null
    }

    fun findNearestLine(path: String): Int? {
        val normalized = path.replace(Regex("""\[\d+]"""), "") // e.g. tags[0] → tags
        var key = normalized
        while (true) {
            lineMap[key]?.let { return it }
            key = key.substringBeforeLast(".", missingDelimiterValue = "")
            if (key.isEmpty()) return null
        }
    }

    fun pathSnippet(path: String, contextLines: Int = 3): String {
        val source = current
        val lines = source.lines
        val line = findNearestLine(path) ?: return "(no line found for $path)"
        return buildSnippet(lines, source.file.name, line, contextLines, path)
    }

    fun lineSnippet(line: Int, contextLines: Int = 3): String {
        val source = current
        val lines = source.lines
        return buildSnippet(lines, source.file.name, line, contextLines, "line $line")
    }

    private fun buildSnippet(lines: List<String>, fileName: String, center: Int, context: Int, label: String): String {
        val start = max(0, center - context - 1)
        val end = min(lines.size, center + context)
        return buildString {
            appendLine("$fileName ($label)")
            for (i in start until end) {
                val mark = if (i + 1 == center) "→" else " "
                appendLine("$mark ${(i + 1).toString().padStart(4)} | ${lines[i]}")
            }
        }.trimEnd()
    }
}
