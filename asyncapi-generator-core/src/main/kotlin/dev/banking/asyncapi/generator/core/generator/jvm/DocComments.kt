package dev.banking.asyncapi.generator.core.generator.jvm

internal object DocComments {
    private const val WRAP_WIDTH = 120

    fun toKDocLines(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()

        return cleanAndUnescape(raw)
            .lines()
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
            }.filter { it.isNotBlank() }
            .map(::escapeKotlinComment)
    }

    fun toJavaDocLines(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()

        return cleanAndUnescape(raw)
            .lines()
            .flatMap { logicalLine ->
                wrapLine(logicalLine)
            }.filter { it.isNotBlank() }
            .map(::escapeJavaComment)
    }

    /** Keeps text inside a Javadoc comment: `*` + `/` would end the comment early. */
    private fun escapeJavaComment(line: String): String = line.replace("*/", "*&#47;")

    /** Kotlin block comments nest, so an opening `/` + `*` in a KDoc line must not start a new one either. */
    private fun escapeKotlinComment(line: String): String = escapeJavaComment(line).replace("/*", "&#47;*")

    private fun cleanAndUnescape(raw: String): String =
        unescapeBasicHtml(raw)
            .replace("\r\n", "\n")
            .trim()

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

    private fun unescapeBasicHtml(input: String): String =
        input
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
}
