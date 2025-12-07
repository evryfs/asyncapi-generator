package com.tietoevry.banking.asyncapi.generator.core.generator.util

object DocumentationUtils {

    private const val WRAP_WIDTH = 120

    fun toKDocLines(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()

        return cleanAndUnescape(raw)
            .lines()
            .map { stripMarkdownPrefixes(it) }
            .flatMap { logicalLine ->
                val trimmedStart = logicalLine.trimStart()
                // KDoc specific: preserve bullets/preformatted blocks without wrapping
                val isBulletOrPreformatted =
                    trimmedStart.startsWith("* ") ||
                        trimmedStart.startsWith("- ") ||
                        trimmedStart.startsWith("•") ||
                        logicalLine.startsWith("  ")

                if (isBulletOrPreformatted) {
                    listOf(logicalLine.trimEnd())
                } else {
                    wrapLine(logicalLine)
                }
            }
            .filter { it.isNotBlank() }
    }

    fun toJavaDocLines(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()

        // JavaDoc logic: clean -> strip prefixes -> wrap everything (JavaDoc handles <p> etc)
        return cleanAndUnescape(raw)
            .lines()
            .flatMap { logicalLine ->
                wrapLine(logicalLine)
            }
            .filter { it.isNotBlank() }
            .map { stripMarkdownPrefixes(it) }
    }


    fun toAvroDocLines(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return raw
            .replace("\n", " ")
            .replace("\r", "")
            .trimStart('"', '\'', '|', '>')
            .trim()
    }

    private fun cleanAndUnescape(raw: String): String {
        return unescapeBasicHtml(raw)
            .replace("\r\n", "\n")
            .trim()
            .removePrefix("\"")
            .removeSuffix("\"")
    }

    private fun stripMarkdownPrefixes(line: String): String {
        val trimmedRight = line.trimEnd()
        return when {
            trimmedRight.trimStart().startsWith("|") ->
                trimmedRight.trimStart().removePrefix("|").trimStart()
            trimmedRight.trimStart().startsWith(">") ->
                trimmedRight.trimStart().removePrefix(">").trimStart()
            else -> trimmedRight
        }
    }

    private fun wrapLine(line: String): List<String> {
        val trimmed = line.trim()
        if (trimmed.length <= WRAP_WIDTH) return listOf(trimmed)

        val words = trimmed.split(Regex("\\s+"))
        val result = mutableListOf<String>()
        var current = StringBuilder()

        for (word in words) {
            if (current.isEmpty()) {
                current.append(word)
                continue
            }

            if (current.length + 1 + word.length > WRAP_WIDTH) {
                result += current.toString()
                current = StringBuilder(word)
            } else {
                current.append(' ').append(word)
            }
        }

        if (current.isNotEmpty()) {
            result += current.toString()
        }

        return result
    }

    fun wrapForParamDoc(paramName: String, text: String): List<String> {
        val cleaned = text.trim()
        if (cleaned.length <= WRAP_WIDTH) return listOf(cleaned)

        val prefixLenFirst = "* @param [$paramName] - ".length
        val words = cleaned.split(Regex("\\s+"))

        val result = mutableListOf<String>()
        var current = StringBuilder()
        var currentWidth = WRAP_WIDTH - prefixLenFirst
        var firstLine = true

        for (word in words) {
            if (current.isEmpty()) {
                current.append(word)
                continue
            }

            if (current.length + 1 + word.length > currentWidth) {
                result += current.toString()
                current = StringBuilder(word)

                if (firstLine) {
                    firstLine = false
                    currentWidth = WRAP_WIDTH
                }
            } else {
                current.append(' ').append(word)
            }
        }

        if (current.isNotEmpty()) {
            result += current.toString()
        }

        return result
    }

    private fun unescapeBasicHtml(input: String): String {
        return input
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
    }
}
