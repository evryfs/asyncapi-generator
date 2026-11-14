package dev.banking.asyncapi.generator.core.generator.util

/** Splits the type arguments of a rendered generic type, such as `String, Map<String, Int>`. */
internal object GenericTypeArguments {

    /** Splits [arguments] at the commas that are not nested inside another type's angle brackets. */
    fun split(arguments: String): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var start = 0
        arguments.forEachIndexed { index, character ->
            when (character) {
                '<' -> depth++
                '>' -> depth--
                ',' -> if (depth == 0) {
                    parts += arguments.substring(start, index).trim()
                    start = index + 1
                }
            }
        }
        parts += arguments.substring(start).trim()
        return parts.filter(String::isNotEmpty)
    }
}
