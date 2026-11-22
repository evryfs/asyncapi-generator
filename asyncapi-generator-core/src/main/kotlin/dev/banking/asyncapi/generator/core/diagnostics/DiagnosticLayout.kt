package dev.banking.asyncapi.generator.core.diagnostics

/**
 * The layout every frontend prints diagnostics in: a title, then per entry the
 * message, an optional source snippet, and a documentation link.
 *
 * Validation findings from the contract and configuration warnings share it, so
 * users read one format whatever the warning is about.
 */
internal object DiagnosticLayout {
    /**
     * @property message What is wrong.
     * @property snippet Source excerpt showing where; null when there is no source document.
     * @property documentation Link to the explanation.
     */
    data class Entry(
        val message: String,
        val snippet: String?,
        val documentation: String,
    )

    fun format(
        title: String,
        entries: List<Entry>,
    ): String =
        buildString {
            appendLine(title)
            appendLine()
            entries.forEach { entry ->
                appendLine(">> ${entry.message}")
                appendLine()
                entry.snippet?.let {
                    appendLine(it)
                    appendLine()
                }
                appendLine("See documentation: ${entry.documentation}")
                appendLine(SEPARATOR)
                appendLine()
            }
        }

    private const val SEPARATOR =
        "---------------------------------------------------------------------------------------------------------------------"
}
